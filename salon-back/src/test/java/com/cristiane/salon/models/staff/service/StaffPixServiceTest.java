package com.cristiane.salon.models.staff.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.staff.dto.GenerateStaffPixRequest;
import com.cristiane.salon.models.staff.dto.StaffPixQrCodeResponse;
import com.cristiane.salon.models.staff.entity.StaffProfile;
import com.cristiane.salon.models.staff.enums.PixKeyType;
import com.cristiane.salon.models.staff.repository.StaffProfileRepository;
import com.cristiane.salon.models.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffPixServiceTest {

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @InjectMocks
    private StaffPixService staffPixService;

    private StaffProfile profileWithPix() {
        User user = new User();
        user.setId(1L);

        StaffProfile profile = new StaffProfile();
        profile.setId(1L);
        profile.setUser(user);
        profile.setFullName("Maria Silva");
        profile.setCity("Recife");
        profile.setPixKeyType(PixKeyType.EMAIL);
        profile.setPixKey("maria@example.com");
        profile.setPixKeyMasked("mar•••••om");
        return profile;
    }

    @Test
    void generateQrCode_whenProfileHasPixKey_shouldReturnPayloadWithoutExposingTheKeyAsASeparateField() {
        when(staffProfileRepository.findById(1L)).thenReturn(Optional.of(profileWithPix()));

        StaffPixQrCodeResponse response = staffPixService.generateQrCode(
                1L, new GenerateStaffPixRequest(new BigDecimal("70.00"), "Salário"));

        assertThat(response.brCodePayload()).isNotBlank();
        assertThat(response.amount()).isEqualByComparingTo("70.00");
        assertThat(response.recipientName()).isEqualTo("Maria Silva");
    }

    @Test
    void generateQrCode_whenProfileNotFound_shouldThrowResourceNotFoundException() {
        when(staffProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                staffPixService.generateQrCode(99L, new GenerateStaffPixRequest(BigDecimal.TEN, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateQrCode_whenProfileHasNoPixKey_shouldThrowBadRequestException() {
        StaffProfile profile = profileWithPix();
        profile.setPixKeyType(null);
        profile.setPixKey(null);
        when(staffProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() ->
                staffPixService.generateQrCode(1L, new GenerateStaffPixRequest(BigDecimal.TEN, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("não tem chave PIX");
    }
}
