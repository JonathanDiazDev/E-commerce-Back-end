package com.jonathan.ecommerce.dto.request;

import com.jonathan.ecommerce.entity.enums.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MovementFilterRequest(
    @Min(0) int page,
    @Min(10) int size,
    @NotBlank String sortBy,
    @NotBlank String direction,
    MovementType type) {
  public MovementFilterRequest {
    if (sortBy == null || sortBy.isBlank()) sortBy = "date";
    if (direction == null || direction.isBlank()) direction = "desc";
  }
}
