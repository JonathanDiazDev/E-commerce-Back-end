package com.jonathan.ecommerce.stock.event;

import com.jonathan.ecommerce.service.StockNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StockNotificationListener {
  private final StockNotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleStockNotification(StockRestockEventDTO stockRestockEventDTO) {
    notificationService.processNotification(
        stockRestockEventDTO.productId(), stockRestockEventDTO.totalStock());
  }
}
