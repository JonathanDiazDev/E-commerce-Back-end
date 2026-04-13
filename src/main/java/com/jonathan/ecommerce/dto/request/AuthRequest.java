package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Ingrese su email")
        @Email(message = "Debe ser un email válido")
        String email,

        @NotBlank(message = "Ingrese su contraseña")
        String password
) {
}
