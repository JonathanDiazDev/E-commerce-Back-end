package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.StockNotification;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.producer.EmailKafkaProducer;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.repository.StockNotificationRepository;
import com.jonathan.ecommerce.service.AuthService;
import com.jonathan.ecommerce.service.StockNotificationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jonathan.ecommerce.service.helper.NotificationPersistenceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockNotificationServiceImpl implements StockNotificationService {

  private final StockNotificationRepository stockNotificationRepository;
  private final ProductRepository productRepository;
  private final AuthService authService;
  private final EmailKafkaProducer emailKafkaProducer;
  private final NotificationPersistenceHelper helper;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createNotification(Long productId) {
    User user = authService.getAuthenticatedUser();
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

    if (stockNotificationRepository.existsByUserIdAndProductIdAndIsNotifiedFalse(
        user.getId(), productId)) {
      throw new ResourceNotFoundException("You are already subscribed to this product");
    }

    StockNotification notification =
        StockNotification.builder()
            .user(user)
            .product(product)
            .createdAt(Instant.now())
            .isNotified(false)
            .build();
    stockNotificationRepository.save(notification);
  }

    @Override
// Eliminamos @Transactional de aquí para que el Helper pueda manejar sus propias micro-transacciones
    public void processNotification(Long productId, Integer currentStock) {
        List<StockNotification> pending =
                stockNotificationRepository.findByProductIdAndIsNotifiedFalse(productId);

        List<StockNotification> successfulNotifications = new ArrayList<>();
        int batchSize = 50;

        for (StockNotification notification : pending) {
            // Bloque 1: Intentar la acción externa (Kafka)
            try {
                Map<String, Object> props = new HashMap<>();
                props.put("productName", notification.getProduct().getName());
                props.put("totalStock", currentStock);

                EmailRequest emailRequest = new EmailRequest(
                        notification.getUser().getEmail(),
                        notification.getProduct().getName(),
                        "STOCK_AVAILABILITY",
                        props);
                emailKafkaProducer.sendEmailEvent(emailRequest);

                notification.setNotified(true);
                successfulNotifications.add(notification);
            } catch (Exception e) {
                log.error("Error individual (Kafka) para el usuario: {}", notification.getUser().getEmail(), e);
                continue;
            }

            if (successfulNotifications.size() >= batchSize) {
                persistirLote(successfulNotifications);
            }
        }

        if (!successfulNotifications.isEmpty()) {
            persistirLote(successfulNotifications);
        }

        log.info("Proceso de notificación terminado con éxito para el producto {}", productId);
    }

    private void persistirLote(List<StockNotification> batch) {
        try {
            helper.saveBatchAtomic(batch);
            batch.clear();
        } catch (Exception e) {
            log.error("Fallo crítico al guardar lote en DB. Se reintentará en la próxima ejecución.", e);
            batch.clear();
        }
    }
}
