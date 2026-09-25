package com.carry;

import com.carry.dto.CreateComplaintDto;
import com.carry.dto.CreateRatingDto;
import com.carry.dto.UpdateComplaintStatusDto;
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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RatingAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ProductRequestRepository productRequestRepository;

    @Autowired
    private StatusUpdateRepository statusUpdateRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private JwtService jwtService;

    private User customer;
    private User partner;
    private User admin;
    private User otherCustomer;

    private String customerToken;
    private String partnerToken;
    private String adminToken;
    private String otherCustomerToken;

    private Trip trip;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        statusUpdateRepository.deleteAllInBatch();
        productRequestRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        customer = userRepository.save(User.builder()
                .studentId("2000001")
                .email("customer@kuet.ac.bd")
                .fullName("Customer Student")
                .passwordHash("hash")
                .isAdmin(false)
                .build());

        partner = userRepository.save(User.builder()
                .studentId("2000002")
                .email("partner@kuet.ac.bd")
                .fullName("Partner Student")
                .passwordHash("hash")
                .isAdmin(false)
                .build());

        admin = userRepository.save(User.builder()
                .studentId("2000003")
                .email("admin@kuet.ac.bd")
                .fullName("Admin User")
                .passwordHash("hash")
                .isAdmin(true)
                .build());

        otherCustomer = userRepository.save(User.builder()
                .studentId("2000004")
                .email("other@kuet.ac.bd")
                .fullName("Other Student")
                .passwordHash("hash")
                .isAdmin(false)
                .build());

        customerToken = jwtService.generateToken(UserDetailsImpl.build(customer));
        partnerToken = jwtService.generateToken(UserDetailsImpl.build(partner));
        adminToken = jwtService.generateToken(UserDetailsImpl.build(admin));
        otherCustomerToken = jwtService.generateToken(UserDetailsImpl.build(otherCustomer));

        trip = tripRepository.save(Trip.builder()
                .partner(partner)
                .destinationArea(LocationArea.NEW_MARKET)
                .departureTime(LocalDateTime.now().plusHours(1))
                .expectedReturnTime(LocalDateTime.now().plusHours(4))
                .status(TripStatus.PLANNED)
                .build());
    }

    @Test
    void testThreeRatingsRecomputedAverage() throws Exception {
        // Create 3 delivered requests
        ProductRequest req1 = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .matchedTrip(trip)
                .productName("Item 1")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.DELIVERED)
                .build());

        ProductRequest req2 = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .matchedTrip(trip)
                .productName("Item 2")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.DELIVERED)
                .build());

        ProductRequest req3 = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .matchedTrip(trip)
                .productName("Item 3")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.DELIVERED)
                .build());

        // 1st Rating: score 5 -> avg: 5.00
        CreateRatingDto rating1 = CreateRatingDto.builder().score(5).comment("Excellent").build();
        mockMvc.perform(post("/api/requests/" + req1.getId() + "/rating")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rating1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(5));

        User updatedPartner1 = userRepository.findById(partner.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("5.00").compareTo(updatedPartner1.getAvgRating()));

        // 2nd Rating: score 4 -> avg: (5 + 4) / 2 = 4.50
        CreateRatingDto rating2 = CreateRatingDto.builder().score(4).comment("Good").build();
        mockMvc.perform(post("/api/requests/" + req2.getId() + "/rating")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rating2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(4));

        User updatedPartner2 = userRepository.findById(partner.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("4.50").compareTo(updatedPartner2.getAvgRating()));

        // 3rd Rating: score 4 -> avg: (5 + 4 + 4) / 3 = 4.333... -> 4.33
        CreateRatingDto rating3 = CreateRatingDto.builder().score(4).comment("Solid").build();
        mockMvc.perform(post("/api/requests/" + req3.getId() + "/rating")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rating3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(4));

        User updatedPartner3 = userRepository.findById(partner.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("4.33").compareTo(updatedPartner3.getAvgRating()));
    }

    @Test
    void testRatingTwiceReturns409() throws Exception {
        ProductRequest req = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .matchedTrip(trip)
                .productName("Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.DELIVERED)
                .build());

        CreateRatingDto ratingDto = CreateRatingDto.builder().score(5).build();

        // First rating succeeds
        mockMvc.perform(post("/api/requests/" + req.getId() + "/rating")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingDto)))
                .andExpect(status().isCreated());

        // Second rating fails with 409 Conflict
        mockMvc.perform(post("/api/requests/" + req.getId() + "/rating")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This request has already been rated"));
    }

    @Test
    void testRatingBeforeDeliveredReturns400() throws Exception {
        ProductRequest req = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .matchedTrip(trip)
                .productName("Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.ACCEPTED) // Not DELIVERED
                .build());

        CreateRatingDto ratingDto = CreateRatingDto.builder().score(5).build();

        mockMvc.perform(post("/api/requests/" + req.getId() + "/rating")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request can only be rated when status is DELIVERED. Current status is ACCEPTED"));
    }

    @Test
    void testRatingByNonCustomerReturns403() throws Exception {
        ProductRequest req = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .matchedTrip(trip)
                .productName("Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.DELIVERED)
                .build());

        CreateRatingDto ratingDto = CreateRatingDto.builder().score(5).build();

        mockMvc.perform(post("/api/requests/" + req.getId() + "/rating")
                        .header("Authorization", "Bearer " + otherCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the customer who created this request can rate it"));
    }

    @Test
    void testNonAdminGets403OnEveryAdminRoute() throws Exception {
        // 1. GET /api/admin/complaints
        mockMvc.perform(get("/api/admin/complaints")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 2. PUT /api/admin/complaints/{id}
        UpdateComplaintStatusDto updateDto = UpdateComplaintStatusDto.builder()
                .status(ComplaintStatus.IN_REVIEW)
                .adminNotes("Reviewing")
                .build();
        mockMvc.perform(put("/api/admin/complaints/1")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());

        // 3. GET /api/admin/users
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 4. GET /api/admin/requests
        mockMvc.perform(get("/api/admin/requests")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminAccessAndComplaintLifecycle() throws Exception {
        // 1. Create a complaint as customer
        CreateComplaintDto complaintDto = CreateComplaintDto.builder()
                .description("Partner arrived late and item damaged")
                .againstUserId(partner.getId())
                .build();

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaintDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.description").value("Partner arrived late and item damaged"));

        Complaint complaint = complaintRepository.findAll().get(0);

        // 2. Admin GET /api/admin/complaints
        mockMvc.perform(get("/api/admin/complaints")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Partner arrived late and item damaged"));

        // 3. Admin PUT /api/admin/complaints/{id} (resolve complaint)
        UpdateComplaintStatusDto updateDto = UpdateComplaintStatusDto.builder()
                .status(ComplaintStatus.RESOLVED)
                .adminNotes("Resolved with partner reprimand")
                .build();

        mockMvc.perform(put("/api/admin/complaints/" + complaint.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminNotes").value("Resolved with partner reprimand"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        Complaint updatedComplaint = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertNotNull(updatedComplaint.getResolvedAt());

        // 4. Admin GET /api/admin/users
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // 5. Admin GET /api/admin/requests (filterable by status)
        productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .productName("Admin View Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.DELIVERED)
                .build());

        mockMvc.perform(get("/api/admin/requests?status=DELIVERED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("DELIVERED"));

        mockMvc.perform(get("/api/admin/requests?status=REQUESTED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
