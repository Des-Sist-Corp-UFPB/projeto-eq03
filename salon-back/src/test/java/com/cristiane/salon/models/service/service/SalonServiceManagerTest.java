package com.cristiane.salon.models.service.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.service.dto.SalonServiceFilter;
import com.cristiane.salon.models.service.dto.SalonServiceRequest;
import com.cristiane.salon.models.service.dto.SalonServiceResponse;
import com.cristiane.salon.models.service.entity.SalonService;
import com.cristiane.salon.models.service.repository.SalonServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonServiceManagerTest {

    @InjectMocks
    private SalonServiceManager salonServiceManager;

    @Mock
    private SalonServiceRepository salonServiceRepository;

    @Test
    void findAll_shouldReturnPageFromRepository() {
        // Arrange
        SalonService s1 = new SalonService(1L, "Corte", "Desc", new BigDecimal("50.0"), 30, "30 min", true);
        SalonService s2 = new SalonService(2L, "Barba", "Desc", new BigDecimal("30.0"), 20, "20 min", false);
        Pageable pageable = PageRequest.of(0, 10);
        Page<SalonService> page = new PageImpl<>(Arrays.asList(s1, s2));
        when(salonServiceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<SalonServiceResponse> result = salonServiceManager.findAll(new SalonServiceFilter(null, null), pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).name()).isEqualTo("Corte");
        assertThat(result.getContent().get(1).name()).isEqualTo("Barba");
        verify(salonServiceRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void findById_shouldReturnService_whenServiceExists() {
        // Arrange
        Long id = 1L;
        SalonService s = new SalonService(id, "Corte", "Desc", new BigDecimal("50.0"), 30, "30 min", true);
        when(salonServiceRepository.findById(id)).thenReturn(Optional.of(s));

        // Act
        SalonServiceResponse result = salonServiceManager.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
    }

    @Test
    void findById_shouldThrowResourceNotFoundException_whenServiceDoesNotExist() {
        // Arrange
        Long id = 1L;
        when(salonServiceRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> salonServiceManager.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Serviço não encontrado");
    }

    @Test
    void create_shouldThrowBadRequestException_whenPriceIsNegative() {
        // Arrange
        SalonServiceRequest request = new SalonServiceRequest("Corte", "Desc", new BigDecimal("-10.0"), "30 min", 30, true);

        // Act & Assert
        assertThatThrownBy(() -> salonServiceManager.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O preço não pode ser negativo");
    }

    @Test
    void create_shouldThrowBadRequestException_whenBothDurationMinAndDurationEstimateAreInvalid() {
        // Arrange
        SalonServiceRequest requestNulls = new SalonServiceRequest("Corte", "Desc", new BigDecimal("50.0"), null, null, true);
        SalonServiceRequest requestInvalids = new SalonServiceRequest("Corte", "Desc", new BigDecimal("50.0"), "   ", 0, true);

        // Act & Assert
        assertThatThrownBy(() -> salonServiceManager.create(requestNulls))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Informe o tempo estimado");

        assertThatThrownBy(() -> salonServiceManager.create(requestInvalids))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Informe o tempo estimado");
    }

    @Test
    void create_shouldSaveService_whenDurationMinIsProvided() {
        // Arrange
        SalonServiceRequest request = new SalonServiceRequest("Corte", "Desc", new BigDecimal("50.0"), "  ", 30, null);
        SalonService saved = new SalonService(1L, "Corte", "Desc", new BigDecimal("50.0"), 30, null, true);
        when(salonServiceRepository.save(any(SalonService.class))).thenReturn(saved);

        // Act
        SalonServiceResponse result = salonServiceManager.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.durationEstimate()).isNull(); // blankToNull test
        assertThat(result.active()).isTrue(); // default active is true

        verify(salonServiceRepository).save(argThat(service -> 
                service.getName().equals("Corte") &&
                service.getDurationMin() == 30 &&
                service.getDurationEstimate() == null &&
                service.getActive()
        ));
    }

    @Test
    void create_shouldSaveService_whenDurationEstimateIsProvided() {
        // Arrange
        SalonServiceRequest request = new SalonServiceRequest("Corte", "Desc", new BigDecimal("50.0"), "cerca de 40 min", null, false);
        SalonService saved = new SalonService(1L, "Corte", "Desc", new BigDecimal("50.0"), null, "cerca de 40 min", false);
        when(salonServiceRepository.save(any(SalonService.class))).thenReturn(saved);

        // Act
        SalonServiceResponse result = salonServiceManager.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.durationMin()).isNull();
        assertThat(result.durationEstimate()).isEqualTo("cerca de 40 min");
        assertThat(result.active()).isFalse();
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenServiceDoesNotExist() {
        // Arrange
        Long id = 1L;
        SalonServiceRequest request = new SalonServiceRequest("Corte", "Desc", new BigDecimal("50.0"), "30 min", 30, true);
        when(salonServiceRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> salonServiceManager.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldThrowBadRequestException_whenNewPriceIsNegative() {
        // Arrange
        Long id = 1L;
        SalonService service = new SalonService(id, "Corte", "Desc", new BigDecimal("50.0"), 30, "30 min", true);
        SalonServiceRequest request = new SalonServiceRequest("Corte", "Desc", new BigDecimal("-5.0"), "30 min", 30, true);

        when(salonServiceRepository.findById(id)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> salonServiceManager.update(id, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O preço não pode ser negativo");
    }

    @Test
    void update_shouldUpdateFieldsAndSave_whenValid() {
        // Arrange
        Long id = 1L;
        SalonService service = new SalonService(id, "Old Corte", "Old Desc", new BigDecimal("40.0"), 20, "20 min", true);
        SalonServiceRequest request = new SalonServiceRequest("New Corte", "New Desc", new BigDecimal("50.0"), "30 min", 30, false);

        SalonService saved = new SalonService(id, "New Corte", "New Desc", new BigDecimal("50.0"), 30, "30 min", false);
        when(salonServiceRepository.findById(id)).thenReturn(Optional.of(service));
        when(salonServiceRepository.save(any(SalonService.class))).thenReturn(saved);

        // Act
        SalonServiceResponse result = salonServiceManager.update(id, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Corte");
        assertThat(result.description()).isEqualTo("New Desc");
        assertThat(result.price()).isEqualTo(new BigDecimal("50.0"));
        assertThat(result.active()).isFalse();

        verify(salonServiceRepository).save(argThat(s -> 
                s.getName().equals("New Corte") &&
                s.getDescription().equals("New Desc") &&
                s.getPrice().equals(new BigDecimal("50.0")) &&
                s.getDurationMin() == 30 &&
                s.getDurationEstimate().equals("30 min") &&
                !s.getActive()
        ));
    }

    @Test
    void delete_shouldMarkServiceAsInactive() {
        // Arrange
        Long id = 1L;
        SalonService service = new SalonService(id, "Corte", "Desc", new BigDecimal("50.0"), 30, "30 min", true);
        when(salonServiceRepository.findById(id)).thenReturn(Optional.of(service));

        // Act
        salonServiceManager.delete(id);

        // Assert
        assertThat(service.getActive()).isFalse();
        verify(salonServiceRepository).save(service);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenServiceDoesNotExist() {
        // Arrange
        Long id = 1L;
        when(salonServiceRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> salonServiceManager.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reactivate_shouldMarkServiceAsActive() {
        // Arrange
        Long id = 1L;
        SalonService service = new SalonService(id, "Corte", "Desc", new BigDecimal("50.0"), 30, "30 min", false);
        SalonService saved = new SalonService(id, "Corte", "Desc", new BigDecimal("50.0"), 30, "30 min", true);

        when(salonServiceRepository.findById(id)).thenReturn(Optional.of(service));
        when(salonServiceRepository.save(service)).thenReturn(saved);

        // Act
        SalonServiceResponse result = salonServiceManager.reactivate(id);

        // Assert
        assertThat(result.active()).isTrue();
        assertThat(service.getActive()).isTrue();
        verify(salonServiceRepository).save(service);
    }

    @Test
    void reactivate_shouldThrowResourceNotFoundException_whenServiceDoesNotExist() {
        // Arrange
        Long id = 1L;
        when(salonServiceRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> salonServiceManager.reactivate(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
