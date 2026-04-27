package com.jonathan.ecommerce.dto.response;

import com.jonathan.ecommerce.entity.enums.MovementType;
import java.time.Instant;

public record MovementResponse(
    Long id,
    String productName,
    MovementType movementType,
    Integer quantity,
    String reason,
    Instant date,
    String formattedDate) {}
