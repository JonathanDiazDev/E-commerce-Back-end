package com.jonathan.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@RequiredArgsConstructor
public class CartServiceConcurrencyTest {

  //    private final CartService cartService;
  //
  //
  //    private final InventoryRepository inventoryRepository;
  //
  ////    @Test
  ////    public void testConcurrentAddItemToCart() throws InterruptedException {
  ////        Long productId = 1L; // Asume que existe
  ////        int stockInitial = 10;
  ////        int numThreads = 5;
  ////        int quantityPerThread = 3; // Total: 15 (más que stock disponible)
  ////
  ////        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
  ////        CountDownLatch latch = new CountDownLatch(numThreads);
  ////        AtomicInteger successCount = new AtomicInteger(0);
  ////
  ////        for (int i = 0; i < numThreads; i++) {
  ////            executor.submit(() -> {
  ////                try {
  ////                    cartService.addItemToCart(quantityPerThread);
  ////                    successCount.incrementAndGet();
  ////                } catch (InsufficientStockException e) {
  ////                    // Esperado: algunos van a fallar
  ////                    System.out.println("Expected: " + e.getMessage());
  ////                } finally {
  ////                    latch.countDown();
  ////                }
  ////            });
  ////        }
  //
  //        latch.await(30, TimeUnit.SECONDS);
  //        executor.shutdown();
  //
  //        // Verificar: solo 3 de 5 threads pudieron agregar stock
  //        // (10 / 3 = 3 exitosos, 2 fallan)
  //        assertEquals(3, successCount.get(),
  //                "Exactly 3 threads should succeed with stock of 10");
  //
  //        // Verificar stock final
  //        Inventory inventory = inventoryRepository.findByProductId(productId)
  //                .orElseThrow();
  //        assertEquals(1, inventory.getQuantity(),
  //                "Final stock should be 1 (10 - 3*3)");
  //    }
}
