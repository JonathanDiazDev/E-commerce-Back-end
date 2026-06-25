package com.jonathan.ecommerce.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.StockNotification;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.producer.EmailKafkaProducer;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.repository.StockNotificationRepository;
import com.jonathan.ecommerce.service.helper.NotificationPersistenceHelper;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockNotificationServiceImplTest {

  @Mock private StockNotificationRepository stockNotificationRepository;
  @Mock private ProductRepository productRepository;
  @Mock private SecurityHelper securityHelper;
  @Mock private EmailKafkaProducer emailKafkaProducer;
  @Mock private NotificationPersistenceHelper helper;

  @InjectMocks private StockNotificationServiceImpl stockNotificationService;

  private User user;
  private Product product;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(1L);
    user.setEmail("test@test.com");
    user.setName("Test User");

    product = new Product();
    product.setId(1L);
    product.setName("Test Product");
  }

  @Test
  void createNotification_Success() {
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(stockNotificationRepository.existsByUserIdAndProductIdAndIsNotifiedFalse(1L, 1L))
        .thenReturn(false);

    stockNotificationService.createNotification(1L);

    verify(stockNotificationRepository).save(any(StockNotification.class));
  }

  @Test
  void createNotification_ProductNotFound() {
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(productRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> stockNotificationService.createNotification(1L));
  }

  @Test
  void createNotification_AlreadySubscribed() {
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(stockNotificationRepository.existsByUserIdAndProductIdAndIsNotifiedFalse(1L, 1L))
        .thenReturn(true);

    assertThrows(
        ResourceNotFoundException.class, () -> stockNotificationService.createNotification(1L));
  }

  @Test
  void processNotification_Success() throws Exception {
    StockNotification notification = new StockNotification();
    notification.setUser(user);
    notification.setProduct(product);
    notification.setNotified(false);

    when(stockNotificationRepository.findByProductIdAndIsNotifiedFalse(1L))
        .thenReturn(List.of(notification));

    stockNotificationService.processNotification(1L, 50);

    verify(emailKafkaProducer).sendEmailEvent(any(EmailRequest.class));
    verify(helper).saveBatchAtomic(anyList());
  }

  @Test
  void processNotification_NoPendingNotifications() throws Exception {
    when(stockNotificationRepository.findByProductIdAndIsNotifiedFalse(1L)).thenReturn(List.of());

    stockNotificationService.processNotification(1L, 50);

    verify(emailKafkaProducer, never()).sendEmailEvent(any());
  }

  @Test
  void processNotification_KafkaErrorContinues() throws Exception {
    StockNotification n1 = new StockNotification();
    n1.setUser(user);
    n1.setProduct(product);
    n1.setNotified(false);

    StockNotification n2 = new StockNotification();
    n2.setUser(user);
    n2.setProduct(product);
    n2.setNotified(false);

    when(stockNotificationRepository.findByProductIdAndIsNotifiedFalse(1L))
        .thenReturn(List.of(n1, n2));

    doThrow(new RuntimeException("Kafka down"))
        .when(emailKafkaProducer)
        .sendEmailEvent(any(EmailRequest.class));

    stockNotificationService.processNotification(1L, 50);

    verify(helper, never()).saveBatchAtomic(anyList());
    verify(emailKafkaProducer, times(2)).sendEmailEvent(any(EmailRequest.class));
  }
}
