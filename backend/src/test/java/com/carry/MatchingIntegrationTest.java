package com.carry;

import com.carry.dto.AcceptRequestDto;
import com.carry.dto.UpdateRequestStatusDto;
import com.carry.entity.*;
import com.carry.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Assuming tests run in 'test' profile
@Transactional
public class MatchingIntegrationTest {

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
    private com.carry.security.JwtService jwtService;

    private String customerToken;
    private String partnerToken;
    private String otherPartnerToken;

    private User customer;
    private User partner;
    private User otherPartner;

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
                .studentId("1000100")
                .email("customer@kuet.ac.bd")
                .fullName("Customer User")
                .passwordHash("hash")
                .build());

        partner = userRepository.save(User.builder()
                .studentId("1000200")
                .email("partner@kuet.ac.bd")
                .fullName("Partner User")
                .passwordHash("hash")
                .build());

        otherPartner = userRepository.save(User.builder()
                .studentId("1000300")
                .email("otherpartner@kuet.ac.bd")
                .fullName("Other Partner User")
                .passwordHash("hash")
                .build());

        customerToken = jwtService.generateToken(com.carry.security.UserDetailsImpl.build(customer));
        partnerToken = jwtService.generateToken(com.carry.security.UserDetailsImpl.build(partner));
        otherPartnerToken = jwtService.generateToken(com.carry.security.UserDetailsImpl.build(otherPartner));
    }

    @Test
    void testGetAvailableRequests() throws Exception {
        // Partner has a trip to NEW_MARKET
        Trip trip = tripRepository.save(Trip.builder()
                .partner(partner)
                .destinationArea(LocationArea.NEW_MARKET)
                .departureTime(LocalDateTime.now().plusHours(1))
                .expectedReturnTime(LocalDateTime.now().plusHours(3))
                .status(TripStatus.PLANNED)
                .build());

        // Request 1: Match (NEW_MARKET, REQUESTED, by customer)
        ProductRequest req1 = productRequestRepository.save(ProductRequest.builder()
                .customer(customer)
                .productName("Item 1")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.REQUESTED)
                .build());

        // Request 2: No match (DAULATPUR)
        ProductRequest req2 = productRequestRepository.save(ProductRequest.builder()
                .customer(customer)
                .productName("Item 2")
                .pickupArea(LocationArea.DAULATPUR)
                .status(RequestStatus.REQUESTED)
                .build());

        // Request 3: No match (Partner's own request)
        ProductRequest req3 = productRequestRepository.save(ProductRequest.builder()
                .customer(partner)
                .productName("Item 3")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.REQUESTED)
                .build());

        mockMvc.perform(get("/api/requests/available")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Item 1"));
    }

    @Test
    void testAcceptRequestSuccessAndTimeline() throws Exception {
        Trip trip = tripRepository.save(Trip.builder()
                .partner(partner)
                .destinationArea(LocationArea.NEW_MARKET)
                .departureTime(LocalDateTime.now().plusHours(1))
                .expectedReturnTime(LocalDateTime.now().plusHours(3))
                .status(TripStatus.PLANNED)
                .build());

        ProductRequest req = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .productName("Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.REQUESTED)
                .version(0)
                .build());

        AcceptRequestDto acceptDto = new AcceptRequestDto();
        acceptDto.setTripId(trip.getId());
        acceptDto.setVersion(0);

        mockMvc.perform(put("/api/requests/" + req.getId() + "/accept")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.matchedTripId").value(trip.getId()));

        // Verify request is no longer available
        mockMvc.perform(get("/api/requests/available")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Check timeline
        mockMvc.perform(get("/api/requests/" + req.getId() + "/timeline")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }

    @Test
    void testConcurrentAcceptRaceCondition() throws Exception {
        Trip trip1 = tripRepository.save(Trip.builder()
                .partner(partner)
                .destinationArea(LocationArea.NEW_MARKET)
                .departureTime(LocalDateTime.now().plusHours(1))
                .expectedReturnTime(LocalDateTime.now().plusHours(3))
                .status(TripStatus.PLANNED)
                .build());

        Trip trip2 = tripRepository.save(Trip.builder()
                .partner(otherPartner)
                .destinationArea(LocationArea.NEW_MARKET)
                .departureTime(LocalDateTime.now().plusHours(1))
                .expectedReturnTime(LocalDateTime.now().plusHours(3))
                .status(TripStatus.PLANNED)
                .build());

        ProductRequest req = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .productName("Highly Demanded Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.REQUESTED)
                .version(0)
                .build());

        // Partner 1 accepts (success, increments version to 1)
        AcceptRequestDto accept1 = new AcceptRequestDto();
        accept1.setTripId(trip1.getId());
        accept1.setVersion(0);

        mockMvc.perform(put("/api/requests/" + req.getId() + "/accept")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accept1)))
                .andExpect(status().isOk());

        // Partner 2 attempts to accept concurrently (using version 0)
        AcceptRequestDto accept2 = new AcceptRequestDto();
        accept2.setTripId(trip2.getId());
        accept2.setVersion(0);

        mockMvc.perform(put("/api/requests/" + req.getId() + "/accept")
                        .header("Authorization", "Bearer " + otherPartnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accept2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testStatusUpdateWorkflowAndPaymentGeneration() throws Exception {
        Trip trip = tripRepository.save(Trip.builder()
                .partner(partner)
                .destinationArea(LocationArea.NEW_MARKET)
                .departureTime(LocalDateTime.now().plusHours(1))
                .expectedReturnTime(LocalDateTime.now().plusHours(3))
                .status(TripStatus.PLANNED)
                .build());

        ProductRequest req = productRequestRepository.saveAndFlush(ProductRequest.builder()
                .customer(customer)
                .matchedTrip(trip)
                .productName("Item")
                .pickupArea(LocationArea.NEW_MARKET)
                .status(RequestStatus.ACCEPTED)
                .build());

        // Invalid Jump: ACCEPTED -> DELIVERED
        UpdateRequestStatusDto invalidJump = new UpdateRequestStatusDto();
        invalidJump.setStatus(RequestStatus.DELIVERED);

        mockMvc.perform(put("/api/requests/" + req.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidJump)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request status transition from ACCEPTED to DELIVERED"));

        // Valid Sequence
        RequestStatus[] sequence = {
                RequestStatus.COLLECTED,
                RequestStatus.RETURNING,
                RequestStatus.READY_FOR_DELIVERY,
                RequestStatus.DELIVERED
        };

        for (RequestStatus s : sequence) {
            UpdateRequestStatusDto dto = new UpdateRequestStatusDto();
            dto.setStatus(s);
            dto.setNote("Status is now " + s.name());

            mockMvc.perform(put("/api/requests/" + req.getId() + "/status")
                            .header("Authorization", "Bearer " + partnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(s.name()));
        }

        // Verify Payment was created
        List<Payment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        Payment payment = payments.get(0);
        assertEquals(PaymentStatus.SIMULATED_PAID, payment.getStatus());
        assertEquals(req.getId(), payment.getRequest().getId());
    }
}
