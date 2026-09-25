package com.carry;

import com.carry.dto.*;
import com.carry.entity.*;
import com.carry.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
public class EndToEndJourneyIntegrationTest {

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
    private RatingRepository ratingRepository;
    
    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        statusUpdateRepository.deleteAllInBatch();
        productRequestRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void completeJourney_Success() throws Exception {
        // 1. Register Customer
        RegisterRequest customerReg = new RegisterRequest();
        customerReg.setFullName("Customer Test");
        customerReg.setStudentId("C12345");
        customerReg.setEmail("customer@test.com");
        customerReg.setPhone("01700000001");
        customerReg.setDepartment("CSE");
        customerReg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerReg)))
                .andExpect(status().isOk());

        // Login Customer
        LoginRequest customerLog = new LoginRequest(); customerLog.setEmail("customer@test.com"); customerLog.setPassword("password123");
        MvcResult customerRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerLog)))
                .andExpect(status().isOk()).andReturn();
        String customerToken = "Bearer " + objectMapper.readTree(customerRes.getResponse().getContentAsString()).get("token").asText();

        // 2. Register Partner
        RegisterRequest partnerReg = new RegisterRequest();
        partnerReg.setFullName("Partner Test");
        partnerReg.setStudentId("P12345");
        partnerReg.setEmail("partner@test.com");
        partnerReg.setPhone("01700000002");
        partnerReg.setDepartment("EEE");
        partnerReg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partnerReg)))
                .andExpect(status().isOk());

        // Login Partner
        LoginRequest partnerLog = new LoginRequest(); partnerLog.setEmail("partner@test.com"); partnerLog.setPassword("password123");
        MvcResult partnerRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partnerLog)))
                .andExpect(status().isOk()).andReturn();
        String partnerToken = "Bearer " + objectMapper.readTree(partnerRes.getResponse().getContentAsString()).get("token").asText();

        // 3. Partner posts a trip
        CreateTripRequest tripReq = new CreateTripRequest();
        tripReq.setDepartureTime(LocalDateTime.now().plusDays(1));
        tripReq.setExpectedReturnTime(LocalDateTime.now().plusDays(2));
        tripReq.setDestinationArea(LocationArea.NEW_MARKET);
        tripReq.setCapacityNotes("Can carry up to 5kg");

        MvcResult tripRes = mockMvc.perform(post("/api/trips")
                .header("Authorization", partnerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tripReq)))
                .andExpect(status().isCreated()).andReturn();
        Long tripId = objectMapper.readTree(tripRes.getResponse().getContentAsString()).get("id").asLong();

        // 4. Customer creates a matching request
        CreateProductRequest prodReq = new CreateProductRequest();
        prodReq.setProductName("Test Item");
        prodReq.setCategory("Electronics");
        prodReq.setQuantity(1);
        prodReq.setPickupArea(LocationArea.NEW_MARKET);
        prodReq.setBudget(new BigDecimal("1000.00"));

        MvcResult reqRes = mockMvc.perform(post("/api/requests")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated()).andReturn();
        Long requestId = objectMapper.readTree(reqRes.getResponse().getContentAsString()).get("id").asLong();
        Integer requestVersion = objectMapper.readTree(reqRes.getResponse().getContentAsString()).get("version").asInt();

        // 5. Partner sees and accepts it
        MvcResult availRes = mockMvc.perform(get("/api/requests/available")
                .header("Authorization", partnerToken))
                .andExpect(status().isOk()).andReturn();
        
        String availJson = availRes.getResponse().getContentAsString();
        assertTrue(availJson.contains("Test Item"));

        AcceptRequestDto acceptDto = new AcceptRequestDto();
        acceptDto.setTripId(tripId);
        acceptDto.setVersion(requestVersion);

        mockMvc.perform(put("/api/requests/" + requestId + "/accept")
                .header("Authorization", partnerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptDto)))
                .andExpect(status().isOk());

        // 6. Partner advances status: Collected -> Returning -> Ready -> Delivered
        String[] statuses = {"COLLECTED", "RETURNING", "READY_FOR_DELIVERY", "DELIVERED"};
        for (String st : statuses) {
            UpdateRequestStatusDto update = new UpdateRequestStatusDto();
            update.setStatus(RequestStatus.valueOf(st));
            update.setNote("Moving to " + st);

            mockMvc.perform(put("/api/requests/" + requestId + "/status")
                .header("Authorization", partnerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());
        }

        // 7. Customer rates the partner
        CreateRatingDto rating = new CreateRatingDto();
        rating.setScore(5);
        rating.setComment("Great delivery!");

        mockMvc.perform(post("/api/requests/" + requestId + "/rating")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rating)))
                .andExpect(status().isCreated());

        // Verify partner's rating was updated
        User partner = userRepository.findByEmail("partner@test.com").get();
        assertEquals(0, new BigDecimal("5.00").compareTo(partner.getAvgRating()));
        
    }
}
