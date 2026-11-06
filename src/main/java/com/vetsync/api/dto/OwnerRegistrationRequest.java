package com.vetsync.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerRegistrationRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(min = 12, max = 128) String password) {
}
