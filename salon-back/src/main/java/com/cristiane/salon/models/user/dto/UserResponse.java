package com.cristiane.salon.models.user.dto;

import com.cristiane.salon.models.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String cpf,
        String role,
        Boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getCpf(),
                user.getRoleName(),
                user.getActive(),
                user.getCreatedAt()
        );
    }
}

