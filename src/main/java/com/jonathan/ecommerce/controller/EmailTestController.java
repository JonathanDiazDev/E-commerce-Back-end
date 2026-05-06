package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.kafka.producer.EmailKafkaProducer;
import com.jonathan.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;
    private final EmailKafkaProducer emailKafkaProducer;

    @GetMapping("/api/test/email")
    public String testEmail(@RequestParam String email, @RequestParam String product, @RequestParam Integer stock) throws Exception {

//        Map<String, Object> properties = new HashMap<>();
//        properties.put("productName", product);
//        EmailRequest request = new EmailRequest(
//                email, product, "STOCK_AVAILABILITY", properties
//        );
//        emailKafkaProducer.sendEmailEvent(request);
        emailService.sendStockAvailabilityEmail(email, product, stock);
        return "Evento de stock enviado a Kafka para el producto:. " + product;
    }
}