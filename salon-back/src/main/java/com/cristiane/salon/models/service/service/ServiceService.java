package com.cristiane.salon.models.service.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.service.dto.ServiceRequest;
import com.cristiane.salon.models.service.dto.ServiceResponse;
import com.cristiane.salon.models.service.entity.Service;
import com.cristiane.salon.models.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<ServiceResponse> findAll() {
        return serviceRepository.findAll().stream()
                .map(ServiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceResponse findById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
        return ServiceResponse.fromEntity(service);
    }

    @Transactional
    public ServiceResponse create(ServiceRequest request) {
        Service service = new Service();
        service.setName(request.name());
        service.setDescription(request.description());
        service.setPrice(request.price());
        service.setDurationMin(request.durationMin());
        service.setActive(request.active() != null ? request.active() : true);

        return ServiceResponse.fromEntity(serviceRepository.save(service));
    }

    @Transactional
    public ServiceResponse update(Long id, ServiceRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        if (request.name() != null) service.setName(request.name());
        if (request.description() != null) service.setDescription(request.description());
        if (request.price() != null) service.setPrice(request.price());
        if (request.durationMin() != null) service.setDurationMin(request.durationMin());
        if (request.active() != null) service.setActive(request.active());

        return ServiceResponse.fromEntity(serviceRepository.save(service));
    }

    @Transactional
    public void delete(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Serviço não encontrado");
        }
        serviceRepository.deleteById(id);
    }
}
