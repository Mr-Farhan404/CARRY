package com.carry;

import com.carry.dto.CreateProductDto;
import com.carry.dto.CreateProductRequest;
import com.carry.dto.ProductRequestResponseDto;
import com.carry.dto.ProductResponseDto;
import com.carry.dto.UpdateProductDto;
import com.carry.entity.*;
import com.carry.repository.*;
import com.carry.security.JwtService;
import com.carry.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductRequestRepository productRequestRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private StatusUpdateRepository statusUpdateRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private JwtService jwtService;

    private User customer;
    private User admin;
    private String customerToken;
    private String adminToken;

    private Product activeBiryani;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        statusUpdateRepository.deleteAllInBatch();
        productRequestRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        customer = userRepository.save(User.builder()
                .studentId("2000101")
                .email("student@kuet.ac.bd")
                .fullName("Catalog Customer")
                .passwordHash("hash")
                .isAdmin(false)
                .build());

        admin = userRepository.save(User.builder()
                .studentId("2000102")
                .email("catalogadmin@kuet.ac.bd")
                .fullName("Catalog Admin")
                .passwordHash("hash")
                .isAdmin(true)
                .build());

        customerToken = jwtService.generateToken(UserDetailsImpl.build(customer));
        adminToken = jwtService.generateToken(UserDetailsImpl.build(admin));

        activeBiryani = productRepository.save(Product.builder()
                .name("Hyderabadi Kacchi Biryani")
                .category(ProductCategory.FOOD)
                .description("Authentic mutton kacchi")
                .imageUrl("https://picsum.photos/seed/kacchi/400/300")
                .estimatedPrice(new BigDecimal("320.00"))
                .weightClass(WeightClass.MEDIUM)
                .sizeClass(SizeClass.MEDIUM)
                .isSensitive(false)
                .isActive(true)
                .build());

        inactiveProduct = productRepository.save(Product.builder()
                .name("Seasonal Mango Juice")
                .category(ProductCategory.FOOD)
                .description("Out of stock")
                .imageUrl("https://picsum.photos/seed/mango/400/300")
                .estimatedPrice(new BigDecimal("80.00"))
                .weightClass(WeightClass.LIGHT)
                .sizeClass(SizeClass.SMALL)
                .isSensitive(false)
                .isActive(false)
                .build());
    }

    @Test
    void testCreateCatalogOrderSuccessAndPriceImmutability() throws Exception {
        // Base 30 + Medium Weight 15 + Medium Size 10 = 55. Qty 2 -> 55 + 10 = 65.00 delivery fee.
        // Cost: 320 * 2 = 640.00. Gateway fee: 640 * 0.0139 = 8.90. Total = 713.90
        CreateProductRequest request = CreateProductRequest.builder()
                .orderType(OrderType.CATALOG)
                .productId(activeBiryani.getId())
                .quantity(2)
                .pickupArea(LocationArea.NEW_MARKET)
                .instructions("Extra salad please")
                .build();

        String responseContent = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderType").value("CATALOG"))
                .andExpect(jsonPath("$.productId").value(activeBiryani.getId()))
                .andExpect(jsonPath("$.productName").value("Hyderabadi Kacchi Biryani"))
                .andExpect(jsonPath("$.unitPriceSnapshot").value(320.00))
                .andExpect(jsonPath("$.deliveryCharge").value(65.00))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.payment.productCost").value(640.00))
                .andExpect(jsonPath("$.payment.deliveryFee").value(65.00))
                .andExpect(jsonPath("$.payment.gatewayFee").value(8.90))
                .andExpect(jsonPath("$.payment.total").value(713.90))
                .andReturn().getResponse().getContentAsString();

        ProductRequestResponseDto created = objectMapper.readValue(responseContent, ProductRequestResponseDto.class);

        // Now Admin updates the product's catalog price to 450.00
        activeBiryani.setEstimatedPrice(new BigDecimal("450.00"));
        productRepository.saveAndFlush(activeBiryani);

        // Fetch the existing order; its unitPriceSnapshot must NOT have mutated!
        mockMvc.perform(get("/api/requests/" + created.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPriceSnapshot").value(320.00))
                .andExpect(jsonPath("$.deliveryCharge").value(65.00))
                .andExpect(jsonPath("$.payment.productCost").value(640.00))
                .andExpect(jsonPath("$.payment.total").value(713.90));
    }

    @Test
    void testCreateCatalogOrderWithInactiveProductFails() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .orderType(OrderType.CATALOG)
                .productId(inactiveProduct.getId())
                .quantity(1)
                .pickupArea(LocationArea.NEW_MARKET)
                .build();

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product is currently unavailable for order"));
    }

    @Test
    void testCreateCatalogOrderWithoutProductIdFails() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .orderType(OrderType.CATALOG)
                .productId(null)
                .quantity(1)
                .pickupArea(LocationArea.NEW_MARKET)
                .build();

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product ID is required for catalog orders"));
    }

    @Test
    void testManualOrderRemainsUnaffected() throws Exception {
        CreateProductRequest manualRequest = CreateProductRequest.builder()
                .productName("Custom Notebook & Geometry Box")
                .category("Stationery")
                .quantity(1)
                .pickupArea(LocationArea.GOLLAMARI)
                .budget(new BigDecimal("350.00"))
                .instructions("Classmate brand")
                .build();

        // 350 + 30 (base delivery) + (350 * 0.0139 = 4.87 gateway fee) = 384.87
        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderType").value("MANUAL"))
                .andExpect(jsonPath("$.productId").doesNotExist())
                .andExpect(jsonPath("$.unitPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.deliveryCharge").value(30.00))
                .andExpect(jsonPath("$.productName").value("Custom Notebook & Geometry Box"))
                .andExpect(jsonPath("$.payment.productCost").value(350.00))
                .andExpect(jsonPath("$.payment.deliveryFee").value(30.00))
                .andExpect(jsonPath("$.payment.gatewayFee").value(4.87))
                .andExpect(jsonPath("$.payment.total").value(384.87));
    }

    @Test
    void testPublicCatalogBrowsingEndpoints() throws Exception {
        // Public browsing requires no Authorization header
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // only activeBiryani is active
                .andExpect(jsonPath("$[0].name").value("Hyderabadi Kacchi Biryani"));

        mockMvc.perform(get("/api/products/" + activeBiryani.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hyderabadi Kacchi Biryani"))
                .andExpect(jsonPath("$.estimatedPrice").value(320.00));

        // Inactive product cannot be retrieved via public single item endpoint
        mockMvc.perform(get("/api/products/" + inactiveProduct.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAdminProductCrudAndDeactivation() throws Exception {
        // 1. Non-admin accessing admin product endpoints returns 403
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 2. Admin can view all products (including inactive)
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 3. Admin creates new product
        CreateProductDto createDto = CreateProductDto.builder()
                .name("Wireless Optical Mouse")
                .category(ProductCategory.ELECTRONICS)
                .description("2.4GHz ergonomic wireless mouse")
                .imageUrl("https://picsum.photos/seed/mouse/400/300")
                .estimatedPrice(new BigDecimal("450.00"))
                .weightClass(WeightClass.LIGHT)
                .sizeClass(SizeClass.SMALL)
                .isSensitive(true)
                .isActive(true)
                .build();

        String createRes = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wireless Optical Mouse"))
                .andExpect(jsonPath("$.isSensitive").value(true))
                .andReturn().getResponse().getContentAsString();

        ProductResponseDto createdProduct = objectMapper.readValue(createRes, ProductResponseDto.class);

        // 4. Admin updates product
        UpdateProductDto updateDto = UpdateProductDto.builder()
                .name("Wireless Optical Mouse (Silent Click)")
                .estimatedPrice(new BigDecimal("490.00"))
                .build();

        mockMvc.perform(put("/api/admin/products/" + createdProduct.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wireless Optical Mouse (Silent Click)"))
                .andExpect(jsonPath("$.estimatedPrice").value(490.00));

        // 5. Admin deactivates product
        mockMvc.perform(put("/api/admin/products/" + createdProduct.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        Product inDb = productRepository.findById(createdProduct.getId()).orElseThrow();
        assertEquals(false, inDb.getIsActive());
    }
}
