package com.cristiane.salon.models.user.dto;

public record UserCpfInfoResponse(
    boolean hasSavedCpf,
    String cpfMasked
) {}
