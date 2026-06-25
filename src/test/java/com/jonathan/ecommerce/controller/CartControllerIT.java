package com.jonathan.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.config.BaseIntegrationTest;
import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.entity.Cart;
import com.jonathan.ecommerce.entity.CartItem;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.Status;
import com.jonathan.ecommerce.repository.CartItemsRepository;
import com.jonathan.ecommerce.repository.CartRepository;
import com.jonathan.ecommerce.repository.CategoryRepository;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class CartControllerIT extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ProductRepository productRepository;

  @Autowired private CartRepository cartRepository;

  @Autowired private CartItemsRepository cartItemRepository;

  @Autowired private InventoryRepository inventoryRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    // Clear repositories before each test to ensure a clean state
    cartItemRepository.deleteAll();
    cartRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @WithMockUser(username = "test@example.com")
  void crearPedido_CuandoStockDisponible_DebeRetornar201YGuardarEnBD() throws Exception {
    // Crear usuario de prueba
    User user = new User();
    user.setName("Test User");
    user.setEmail("test@example.com");
    user.setPassword(passwordEncoder.encode("password"));

    // Crear categoría de prueba
    Category category = new Category();
    category.setName("Test Category");
    category.setActive(true);
    category = categoryRepository.save(category);

    // Preparar un producto con stock suficiente
    Product product = new Product();
    product.setName("Test Product");
    product.setDescription("Description for test product");
    product.setPrice(BigDecimal.valueOf(100.00));
    product.setStatus(Status.ACTIVE);
    product.setActive(true);
    product.setCategory(category);
    product = productRepository.save(product);

    // Crear inventory con stock suficiente
    Inventory inventory = new Inventory();
    inventory.setProduct(product);
    inventory.setQuantity(10);
    inventory.setManualDisabled(false);

    // Crear la solicitud para añadir al carrito
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), 5);

    // Enviar la petición POST
    mockMvc
        .perform(
            post("/api/v1/carts/item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addToCartRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items[0].productName").value("Test Product"))
        .andExpect(jsonPath("$.items[0].quantity").value(5));

    // Verificar que el carrito y el item se guardaron en la base de datos
    List<Cart> carts = cartRepository.findAll();
    assertFalse(carts.isEmpty(), "Debe existir al menos un carrito");
    Cart cart = carts.getFirst();

    List<CartItem> cartItems = cart.getItems();
    assertFalse(cartItems.isEmpty(), "Debe existir al menos un item en el carrito");
    assertEquals(1, cartItems.size());

    CartItem cartItem = cartItems.getFirst();
    assertEquals(product.getId(), cartItem.getProduct().getId());
    assertEquals(5, cartItem.getQuantity());

    // Verificar que el stock del producto disminuyó
    Inventory updatedInventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
    assertEquals(5, updatedInventory.getQuantity());
  }

  @Test
  @WithMockUser(username = "test@example.com")
  void crearPedido_CuandoSinStock_DebeRetornar400BadRequest() throws Exception {
    // Crear usuario de prueba
    User user = new User();
    user.setName("Test User");
    user.setEmail("test@example.com");
    user.setPassword(passwordEncoder.encode("password"));

    // Crear categoría de prueba
    Category category = new Category();
    category.setName("Test Category");
    category.setActive(true);
    category = categoryRepository.save(category);

    // Preparar un producto con stock igual a 0
    Product product = new Product();
    product.setName("Out of Stock Product");
    product.setDescription("Description for out of stock product");
    product.setPrice(BigDecimal.valueOf(50.00));
    product.setStatus(Status.ACTIVE);
    product.setActive(true);
    product.setCategory(category);
    product = productRepository.save(product);

    // Crear inventory con stock 0
    Inventory inventory = new Inventory();
    inventory.setProduct(product);
    inventory.setQuantity(0);
    inventory.setManualDisabled(false);

    // Crear la solicitud para añadir al carrito
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), 1);

    // Enviar la petición POST
    mockMvc
        .perform(
            post("/api/v1/carts/item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addToCartRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").exists()); // Asumiendo que el error tiene un campo 'message'

    // Verificar que no se creó ningún carrito ni item en la base de datos
    assertTrue(cartRepository.findAll().isEmpty(), "No debe existir ningún carrito");
    assertTrue(cartItemRepository.findAll().isEmpty(), "No debe existir ningún item en el carrito");

    // Verificar que el stock del producto sigue siendo 0
    Inventory originalInventory =
        inventoryRepository.findByProductId(product.getId()).orElseThrow();
    assertEquals(0, originalInventory.getQuantity());
  }
}
