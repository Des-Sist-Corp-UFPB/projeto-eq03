package com.cristiane.salon.models.user.dto;

public record ClientFilter(
    String name,
    String email,
    String phone,
    String cpf,
    Boolean active
) {}
