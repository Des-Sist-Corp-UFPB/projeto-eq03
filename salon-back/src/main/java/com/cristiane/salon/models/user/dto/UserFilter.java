package com.cristiane.salon.models.user.dto;

public record UserFilter(
    String name,
    String email,
    String phone,
    Boolean active,
    Long roleId
) {}
