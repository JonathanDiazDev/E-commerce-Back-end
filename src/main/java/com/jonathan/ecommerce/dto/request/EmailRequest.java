package com.jonathan.ecommerce.dto.request;

import java.util.Map;

public record EmailRequest(
        String to, String productName, String templateType, Map<String, Object> properties
) {}
