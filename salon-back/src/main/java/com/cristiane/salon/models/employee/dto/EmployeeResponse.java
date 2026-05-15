package com.cristiane.salon.models.employee.dto;

import com.cristiane.salon.models.employee.entity.Employee;

public record EmployeeResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String bio
) {
    public static EmployeeResponse fromEntity(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUser().getId(),
                employee.getUser().getName(),
                employee.getUser().getEmail(),
                employee.getBio()
        );
    }
}
