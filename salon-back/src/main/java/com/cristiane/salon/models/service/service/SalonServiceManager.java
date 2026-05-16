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
    public List<SalonServiceResponse> findAll() {
        return salonServiceRepository.findAll().stream()
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
        SalonService service = new SalonService();
        service.setName(request.name());
        service.setDescription(request.description());
        if (request.price() != null && request.price().signum() < 0) {
            throw new BadRequestException("O preço não pode ser negativo");
        }
        service.setPrice(request.price());
        service.setDurationMin(request.durationMin());
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
        if (request.durationMin() != null) service.setDurationMin(request.durationMin());
        if (request.active() != null) service.setActive(request.active());

        return SalonServiceResponse.fromEntity(salonServiceRepository.save(service));
    }

    @Transactional
    public void delete(Long id) {
        SalonService service = salonServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
        // Soft delete (desativar)
        service.setActive(false);
        salonServiceRepository.save(service);
    }
}
