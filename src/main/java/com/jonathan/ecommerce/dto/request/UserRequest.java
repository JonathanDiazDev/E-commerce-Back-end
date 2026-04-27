package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Email is required") @Email(message = "It must be a valid email address.")
        String email,
    @NotBlank(message = "A password is required.")
        @Size(
            min = 12,
            max = 20,
            message = "The password must be between 12 and 20 characters long.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{12,20}$",
            message =
                "The password must contain at least one letter, one number, and one special character.")
        String password) {}
