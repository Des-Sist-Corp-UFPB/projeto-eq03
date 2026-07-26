package com.cristiane.salon.models.salonprofile.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.models.salonprofile.dto.BusinessHourDto;
import com.cristiane.salon.models.salonprofile.dto.SalonProfileResponse;
import com.cristiane.salon.models.salonprofile.dto.SalonProfileUpdateRequest;
import com.cristiane.salon.models.salonprofile.entity.BusinessHour;
import com.cristiane.salon.models.salonprofile.entity.SalonProfile;
import com.cristiane.salon.models.salonprofile.repository.BusinessHourRepository;
import com.cristiane.salon.models.salonprofile.repository.SalonProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perfil público do salão (issue #117) + horário de funcionamento (issue #116), unificados: o
 * horário é exibido junto do resto do perfil na página pública e editado na mesma tela de admin
 * — são a mesma "ficha" do salão, não duas telas desconectadas.
 */
@Service
@RequiredArgsConstructor
public class SalonProfileService {

    private final SalonProfileRepository profileRepository;
    private final BusinessHourRepository hoursRepository;

    @Transactional(readOnly = true)
    public SalonProfileResponse getProfile() {
        SalonProfile profile = profileRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Perfil do salão não configurado — migration V40 deveria ter semeado um registro."));
        return SalonProfileResponse.fromEntity(profile, sortedBusinessHours());
    }

    @Transactional
    public SalonProfileResponse updateProfile(SalonProfileUpdateRequest request) {
        validateBusinessHours(request.businessHours());

        SalonProfile profile = profileRepository.findFirstByOrderByIdAsc().orElseGet(SalonProfile::new);
        profile.setName(request.name());
        profile.setDescription(request.description());
        profile.setAddress(request.address());
        profile.setPhone(request.phone());
        profile.setInstagram(request.instagram());
        profile.setWhatsapp(request.whatsapp());
        profile.setLogoUrl(request.logoUrl());
        profileRepository.save(profile);

        for (BusinessHourDto dto : request.businessHours()) {
            BusinessHour entity = hoursRepository.findByDayOfWeek(dto.dayOfWeek())
                    .orElseThrow(() -> new IllegalStateException("Dia da semana sem linha semeada: " + dto.dayOfWeek()));
            entity.setOpen(dto.open());
            entity.setOpenTime(dto.open() ? dto.openTime() : null);
            entity.setCloseTime(dto.open() ? dto.closeTime() : null);
            hoursRepository.save(entity);
        }

        return getProfile();
    }

    /**
     * Único ponto de leitura de "esse dia está aberto?" — usado por
     * {@code AppointmentService} para bloquear a data de preferência do CLIENTE (nunca as ações
     * da equipe, que continuam livres para encaixar alguém fora do horário).
     */
    @Transactional(readOnly = true)
    public boolean isDayOpen(DayOfWeek dayOfWeek) {
        return hoursRepository.findByDayOfWeek(dayOfWeek)
                .map(BusinessHour::isOpen)
                // Fail-open: as 7 linhas são sempre semeadas pela migration, então isso nunca
                // deveria disparar de verdade — mas se disparar, não é motivo pra travar o
                // agendamento do cliente por um problema de configuração nosso.
                .orElse(true);
    }

    private List<BusinessHourDto> sortedBusinessHours() {
        return hoursRepository.findAll().stream()
                .sorted(Comparator.comparingInt(bh -> bh.getDayOfWeek().getValue()))
                .map(BusinessHourDto::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateBusinessHours(List<BusinessHourDto> hours) {
        if (hours.size() != 7) {
            throw new BadRequestException("É necessário informar o horário para os 7 dias da semana.");
        }

        Set<DayOfWeek> daysSeen = EnumSet.noneOf(DayOfWeek.class);
        for (BusinessHourDto dto : hours) {
            if (!daysSeen.add(dto.dayOfWeek())) {
                throw new BadRequestException("Dia da semana duplicado: " + dto.dayOfWeek());
            }
            if (dto.open()) {
                if (dto.openTime() == null || dto.closeTime() == null) {
                    throw new BadRequestException(
                            "Informe o horário de abertura e fechamento para " + dto.dayOfWeek());
                }
                if (!dto.openTime().isBefore(dto.closeTime())) {
                    throw new BadRequestException(
                            "O horário de abertura deve ser antes do fechamento em " + dto.dayOfWeek());
                }
            }
        }
    }
}
