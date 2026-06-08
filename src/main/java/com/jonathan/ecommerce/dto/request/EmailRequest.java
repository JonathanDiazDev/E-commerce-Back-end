package com.jonathan.ecommerce.dto.request;

import java.util.Map;

public record EmailRequest(
    String to,
    String userName,
    String productName,
    String templateType,
    String messageId,
    Map<String, Object> properties) {}
