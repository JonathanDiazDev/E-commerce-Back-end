package com.jonathan.ecommerce;

import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@RequiredArgsConstructor
public class CartServiceConcurrencyTest {

    private final CartService cartService;


    private final InventoryRepository inventoryRepository;

    @Test
    public void testConcurrentAddItemToCart() throws InterruptedException {
        Long productId = 1L; // Asume que existe
        int stockInitial = 10;
        int numThreads = 5;
        int quantityPerThread = 3; // Total: 15 (más que stock disponible)

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    cartService.addItemToCart(productId, quantityPerThread);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    // Esperado: algunos van a fallar
                    System.out.println("Expected: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verificar: solo 3 de 5 threads pudieron agregar stock
        // (10 / 3 = 3 exitosos, 2 fallan)
        assertEquals(3, successCount.get(),
                "Exactly 3 threads should succeed with stock of 10");

        // Verificar stock final
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow();
        assertEquals(1, inventory.getQuantity(),
                "Final stock should be 1 (10 - 3*3)");
    }
}
