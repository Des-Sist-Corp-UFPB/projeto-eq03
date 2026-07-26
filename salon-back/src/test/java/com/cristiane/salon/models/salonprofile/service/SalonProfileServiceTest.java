package com.cristiane.salon.models.salonprofile.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.models.salonprofile.dto.BusinessHourDto;
import com.cristiane.salon.models.salonprofile.dto.SalonProfileResponse;
import com.cristiane.salon.models.salonprofile.dto.SalonProfileUpdateRequest;
import com.cristiane.salon.models.salonprofile.entity.BusinessHour;
import com.cristiane.salon.models.salonprofile.entity.SalonProfile;
import com.cristiane.salon.models.salonprofile.repository.BusinessHourRepository;
import com.cristiane.salon.models.salonprofile.repository.SalonProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonProfileServiceTest {

    @Mock
    private SalonProfileRepository profileRepository;

    @Mock
    private BusinessHourRepository hoursRepository;

    @InjectMocks
    private SalonProfileService service;

    private SalonProfile profile;
    private List<BusinessHour> allDaysOpen;

    @BeforeEach
    void setUp() {
        profile = new SalonProfile();
        profile.setId(1L);
        profile.setName("Espaço Cristiane Moura");

        allDaysOpen = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            BusinessHour bh = new BusinessHour();
            bh.setId((long) day.getValue());
            bh.setDayOfWeek(day);
            bh.setOpen(day != DayOfWeek.SUNDAY);
            if (bh.isOpen()) {
                bh.setOpenTime(LocalTime.of(8, 0));
                bh.setCloseTime(LocalTime.of(18, 0));
            }
            allDaysOpen.add(bh);
        }
    }

    private List<BusinessHourDto> validRequestHours() {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> new BusinessHourDto(day, day != DayOfWeek.SUNDAY,
                        day != DayOfWeek.SUNDAY ? LocalTime.of(8, 0) : null,
                        day != DayOfWeek.SUNDAY ? LocalTime.of(18, 0) : null))
                .toList();
    }

    @Test
    void getProfile_returnsProfileWithHoursSortedByWeekOrder() {
        when(profileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(hoursRepository.findAll()).thenReturn(List.of(allDaysOpen.get(6), allDaysOpen.get(0), allDaysOpen.get(3)));

        SalonProfileResponse response = service.getProfile();

        assertThat(response.businessHours()).extracting(BusinessHourDto::dayOfWeek)
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void isDayOpen_whenDayIsOpen_returnsTrue() {
        when(hoursRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(allDaysOpen.get(0)));

        assertThat(service.isDayOpen(DayOfWeek.MONDAY)).isTrue();
    }

    @Test
    void isDayOpen_whenDayIsClosed_returnsFalse() {
        when(hoursRepository.findByDayOfWeek(DayOfWeek.SUNDAY))
                .thenReturn(Optional.of(allDaysOpen.get(6)));

        assertThat(service.isDayOpen(DayOfWeek.SUNDAY)).isFalse();
    }

    @Test
    void isDayOpen_whenDayNotSeeded_failsOpenAndReturnsTrue() {
        when(hoursRepository.findByDayOfWeek(any())).thenReturn(Optional.empty());

        assertThat(service.isDayOpen(DayOfWeek.MONDAY)).isTrue();
    }

    @Test
    void updateProfile_whenValid_savesProfileAndAllSevenDays() {
        when(profileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenReturn(profile);
        for (BusinessHour bh : allDaysOpen) {
            when(hoursRepository.findByDayOfWeek(bh.getDayOfWeek())).thenReturn(Optional.of(bh));
        }
        when(hoursRepository.findAll()).thenReturn(allDaysOpen);

        SalonProfileUpdateRequest request = new SalonProfileUpdateRequest(
                "Novo Nome", "Descrição", "Rua Teste, 123", "83999999999",
                "@salao", "83999999999", null, validRequestHours());

        service.updateProfile(request);

        verify(profileRepository).save(argThat(p -> p.getName().equals("Novo Nome")));
        verify(hoursRepository, times(7)).save(any());
    }

    @Test
    void updateProfile_whenNotAllSevenDaysProvided_throwsBadRequestException() {
        List<BusinessHourDto> incomplete = validRequestHours().subList(0, 6);
        SalonProfileUpdateRequest request = new SalonProfileUpdateRequest(
                "Nome", null, null, null, null, null, null, incomplete);

        assertThatThrownBy(() -> service.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("É necessário informar o horário para os 7 dias da semana.");

        verifyNoInteractions(profileRepository);
    }

    @Test
    void updateProfile_whenDayDuplicated_throwsBadRequestException() {
        List<BusinessHourDto> withDuplicate = new ArrayList<>(validRequestHours().subList(0, 6));
        withDuplicate.add(withDuplicate.get(0));
        SalonProfileUpdateRequest request = new SalonProfileUpdateRequest(
                "Nome", null, null, null, null, null, null, withDuplicate);

        assertThatThrownBy(() -> service.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("duplicado");
    }

    @Test
    void updateProfile_whenOpenDayMissingTimes_throwsBadRequestException() {
        List<BusinessHourDto> hours = new ArrayList<>(validRequestHours());
        hours.set(0, new BusinessHourDto(DayOfWeek.MONDAY, true, null, null));
        SalonProfileUpdateRequest request = new SalonProfileUpdateRequest(
                "Nome", null, null, null, null, null, null, hours);

        assertThatThrownBy(() -> service.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MONDAY");
    }

    @Test
    void updateProfile_whenOpenTimeAfterCloseTime_throwsBadRequestException() {
        List<BusinessHourDto> hours = new ArrayList<>(validRequestHours());
        hours.set(0, new BusinessHourDto(DayOfWeek.MONDAY, true, LocalTime.of(18, 0), LocalTime.of(8, 0)));
        SalonProfileUpdateRequest request = new SalonProfileUpdateRequest(
                "Nome", null, null, null, null, null, null, hours);

        assertThatThrownBy(() -> service.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("abertura deve ser antes do fechamento");
    }
}
