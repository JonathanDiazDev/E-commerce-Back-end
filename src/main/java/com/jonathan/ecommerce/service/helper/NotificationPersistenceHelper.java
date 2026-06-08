package com.jonathan.ecommerce.service.helper;

import com.jonathan.ecommerce.entity.StockNotification;
import com.jonathan.ecommerce.repository.StockNotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceHelper {

  private final StockNotificationRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveBatchAtomic(List<StockNotification> batch) {
    repository.saveAll(batch);
  }
}
