package com.carry;

import com.carry.dto.CreateProductRequest;
import com.carry.dto.UpdateProductRequest;
import com.carry.entity.LocationArea;
import com.carry.entity.ProductRequest;
import com.carry.entity.RequestStatus;
import com.carry.entity.User;
import com.carry.repository.ProductRequestRepository;
import com.carry.repository.UserRepository;
import com.carry.security.JwtService;
import com.carry.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRequestRepository productRequestRepository;

    @Autowired private com.carry.repository.TripRepository tripRepository;
    @Autowired private com.carry.repository.StatusUpdateRepository statusUpdateRepository;
    @Autowired private com.carry.repository.PaymentRepository paymentRepository;
    @Autowired private com.carry.repository.RatingRepository ratingRepository;
    @Autowired private com.carry.repository.ComplaintRepository complaintRepository;

    @Autowired
    private JwtService jwtService;

    private User customerUser;
    private User otherUser;
    private String customerToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        statusUpdateRepository.deleteAllInBatch();
        productRequestRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        
        customerUser = User.builder()
                .fullName("Customer User")
                .studentId("1807050")
                .email("customer@stud.kuet.ac.bd")
                .passwordHash("hashed")
                .build();
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateToken(UserDetailsImpl.build(customerUser));

        otherUser = User.builder()
                .fullName("Other Customer")
                .studentId("1807051")
                .email("othercust@stud.kuet.ac.bd")
                .passwordHash("hashed")
                .build();
        otherUser = userRepository.save(otherUser);
        otherUserToken = jwtService.generateToken(UserDetailsImpl.build(otherUser));
    }

    @Test
    void testCreateProductRequestSuccess() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .productName("Mechanical Keyboard Switch")
                .category("Electronics")
                .quantity(10)
                .preferredShop("Tech Land Khulna")
                .pickupArea(LocationArea.ELECTRONICS_MARKET)
                .budget(new BigDecimal("1200.00"))
                .instructions("Outemu Blue switches preferred")
                .build();

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customerUser.getId()))
                .andExpect(jsonPath("$.productName").value("Mechanical Keyboard Switch"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void testGetMyRequests() throws Exception {
        ProductRequest req1 = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Notebook")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(2)
                .status(RequestStatus.REQUESTED)
                .build());

        ProductRequest req2 = productRequestRepository.save(ProductRequest.builder()
                .customer(otherUser)
                .productName("Pen")
                .pickupArea(LocationArea.SONADANGA)
                .quantity(5)
                .status(RequestStatus.REQUESTED)
                .build());

        mockMvc.perform(get("/api/requests/mine")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productName").value("Notebook"))
                .andExpect(jsonPath("$[0].customerId").value(customerUser.getId()));
    }

    @Test
    void testEditProductRequestSuccess() throws Exception {
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Original Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(1)
                .status(RequestStatus.REQUESTED)
                .build());

        UpdateProductRequest update = UpdateProductRequest.builder()
                .productName("Updated Item Name")
                .category("Stationery")
                .quantity(3)
                .preferredShop("Book Shop")
                .pickupArea(LocationArea.GOLLAMARI)
                .budget(new BigDecimal("250.00"))
                .instructions("Blue color")
                .build();

        mockMvc.perform(put("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Updated Item Name"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.pickupArea").value("GOLLAMARI"));

        ProductRequest updated = productRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals("Updated Item Name", updated.getProductName());
    }

    @Test
    void testEditByNonOwnerReturns403() throws Exception {
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Customer A Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(1)
                .status(RequestStatus.REQUESTED)
                .build());

        UpdateProductRequest update = UpdateProductRequest.builder()
                .productName("Malicious Update")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(1)
                .build();

        mockMvc.perform(put("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owning customer can edit this request"));
    }

    @Test
    void testEditWhenNotRequestedReturns400() throws Exception {
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Accepted Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(1)
                .status(RequestStatus.ACCEPTED) // not REQUESTED
                .build());

        UpdateProductRequest update = UpdateProductRequest.builder()
                .productName("Attempted Edit")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(1)
                .build();

        mockMvc.perform(put("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request can only be edited while in REQUESTED status. Current status is ACCEPTED"));
    }

    @Test
    void testCancelAllowedBeforeCollected() throws Exception {
        // 1. Cancel while in REQUESTED status
        ProductRequest req1 = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Item 1")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.REQUESTED)
                .build());

        mockMvc.perform(put("/api/requests/" + req1.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 2. Cancel while in ACCEPTED status
        ProductRequest req2 = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Item 2")
                .pickupArea(LocationArea.DAULATPUR)
                .status(RequestStatus.ACCEPTED)
                .build());

        mockMvc.perform(put("/api/requests/" + req2.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testCancelFromCollectedOnwardReturns400() throws Exception {
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Collected Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.COLLECTED)
                .build());

        mockMvc.perform(put("/api/requests/" + req.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel request: partner has already spent money and collected the item. Please file a complaint instead."));
    }

    @Test
    void testCancelByNonOwnerReturns403() throws Exception {
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Customer A Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.REQUESTED)
                .build());

        mockMvc.perform(put("/api/requests/" + req.getId() + "/cancel")
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owning customer can cancel this request"));
    }

    @Test
    void testConcurrentEditUsingVersionReturns409() throws Exception {
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Original Name")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(1)
                .status(RequestStatus.REQUESTED)
                .version(0)
                .build());

        // First edit succeeds and increments version to 1
        UpdateProductRequest firstUpdate = UpdateProductRequest.builder()
                .productName("First Update Name")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(2)
                .version(0)
                .build();

        mockMvc.perform(put("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpdate)))
                .andExpect(status().isOk());

        // Concurrent/second edit using the old version 0 is rejected with 409 Conflict
        UpdateProductRequest staleUpdate = UpdateProductRequest.builder()
                .productName("Stale Concurrent Update Name")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(5)
                .version(0) // stale version
                .build();

        mockMvc.perform(put("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetMetaZonesReturnsFixedEnumValues() throws Exception {
        mockMvc.perform(get("/api/meta/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0]").value("NEW_MARKET"))
                .andExpect(jsonPath("$[4]").value("KUET_AREA"));
    }

    @Test
    void testGetRequestByIdAuthorizedAndForbidden() throws Exception {
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Detail Test Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .quantity(2)
                .status(RequestStatus.REQUESTED)
                .build());

        // Owning customer can view
        mockMvc.perform(get("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(req.getId()))
                .andExpect(jsonPath("$.productName").value("Detail Test Item"));

        // Other non-owning user receives 403 Forbidden
        mockMvc.perform(get("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to view this request"));
    }
}
