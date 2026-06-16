package com.cristiane.salon.models.service.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.service.dto.SalonServiceRequest;
import com.cristiane.salon.models.service.dto.SalonServiceResponse;
import com.cristiane.salon.models.service.entity.SalonService;
import com.cristiane.salon.models.service.repository.SalonServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalonServiceManager {

    private final SalonServiceRepository salonServiceRepository;

    @Transactional(readOnly = true)
    public List<SalonServiceResponse> findAll(Boolean active) {
        return salonServiceRepository.findAll().stream()
                .filter(service -> active == null || service.getActive().equals(active))
                .map(SalonServiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalonServiceResponse findById(Long id) {
        SalonService service = salonServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
        return SalonServiceResponse.fromEntity(service);
    }

    @Transactional
    public SalonServiceResponse create(SalonServiceRequest request) {
        validateDuration(request.durationMin(), request.durationEstimate());

        SalonService service = new SalonService();
        service.setName(request.name());
        service.setDescription(request.description());
        if (request.price() != null && request.price().signum() < 0) {
            throw new BadRequestException("O preço não pode ser negativo");
        }
        service.setPrice(request.price());
        service.setDurationMin(request.durationMin());
        service.setDurationEstimate(blankToNull(request.durationEstimate()));
        service.setActive(request.active() != null ? request.active() : true);

        return SalonServiceResponse.fromEntity(salonServiceRepository.save(service));
    }

    @Transactional
    public SalonServiceResponse update(Long id, SalonServiceRequest request) {
        SalonService service = salonServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        if (request.price() != null && request.price().signum() < 0) {
            throw new BadRequestException("O preço não pode ser negativo");
        }
        if (request.name() != null) service.setName(request.name());
        if (request.description() != null) service.setDescription(request.description());
        service.setPrice(request.price());
        service.setDurationMin(request.durationMin());
        service.setDurationEstimate(blankToNull(request.durationEstimate()));
        if (request.active() != null) service.setActive(request.active());

        validateDuration(service.getDurationMin(), service.getDurationEstimate());

        return SalonServiceResponse.fromEntity(salonServiceRepository.save(service));
    }

    @Transactional
    public void delete(Long id) {
        SalonService service = salonServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
        service.setActive(false);
        salonServiceRepository.save(service);
    }

    @Transactional
    public SalonServiceResponse reactivate(Long id) {
        SalonService service = salonServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
        service.setActive(true);
        return SalonServiceResponse.fromEntity(salonServiceRepository.save(service));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }


    private static void validateDuration(Integer durationMin, String durationEstimate) {
        boolean hasMin = durationMin != null && durationMin > 0;
        boolean hasEst = durationEstimate != null && !durationEstimate.isBlank();
        if (!hasMin && !hasEst) {
            throw new BadRequestException(
                    "Informe o tempo estimado (ex.: em média 50 min) ou duração em minutos para uso interno na agenda.");
        }
    }
}
