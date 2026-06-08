package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.StockNotification;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.producer.EmailKafkaProducer;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.repository.StockNotificationRepository;
import com.jonathan.ecommerce.service.StockNotificationService;
import com.jonathan.ecommerce.service.helper.NotificationPersistenceHelper;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.time.Instant;
import java.util.*;
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
  private final SecurityHelper securityHelper;
  private final EmailKafkaProducer emailKafkaProducer;
  private final NotificationPersistenceHelper helper;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createNotification(Long productId) {
    User user = securityHelper.getCurrentUser();
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
  public void processNotification(Long productId, Integer currentStock) {
    List<StockNotification> pending =
        stockNotificationRepository.findByProductIdAndIsNotifiedFalse(productId);

    List<StockNotification> successfulNotifications = new ArrayList<>();
    int batchSize = 50;

    for (StockNotification notification : pending) {
      try {
        Map<String, Object> props = new HashMap<>();
        props.put("productName", notification.getProduct().getName());
        props.put("totalStock", currentStock);

        String messageId = UUID.randomUUID().toString();

        EmailRequest emailRequest =
            new EmailRequest(
                notification.getUser().getEmail(),
                notification.getUser().getName(),
                notification.getProduct().getName(),
                "STOCK_AVAILABILITY",
                messageId,
                props);
        emailKafkaProducer.sendEmailEvent(emailRequest);

        notification.setNotified(true);
        successfulNotifications.add(notification);
      } catch (Exception e) {
        log.error(
            "Individual error (Kafka) for the user: {}", notification.getUser().getEmail(), e);
        continue;
      }

      if (successfulNotifications.size() >= batchSize) {
        persistirLote(successfulNotifications);
      }
    }

    if (!successfulNotifications.isEmpty()) {
      persistirLote(successfulNotifications);
    }

    log.info("Notification process successfully completed for the product {}", productId);
  }

  private void persistirLote(List<StockNotification> batch) {
    try {
      helper.saveBatchAtomic(batch);
      batch.clear();
    } catch (Exception e) {
      log.error(
          "Critical failure while saving batch to database. Will be retried on the next run.", e);
      batch.clear();
    }
  }
}
