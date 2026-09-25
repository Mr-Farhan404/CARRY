package com.carry;

import com.carry.dto.LoginRequest;
import com.carry.dto.RegisterRequest;
import com.carry.entity.User;
import com.carry.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserSerializationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired private com.carry.repository.TripRepository tripRepository;
    @Autowired private com.carry.repository.ProductRequestRepository productRequestRepository;
    @Autowired private com.carry.repository.StatusUpdateRepository statusUpdateRepository;
    @Autowired private com.carry.repository.PaymentRepository paymentRepository;
    @Autowired private com.carry.repository.RatingRepository ratingRepository;
    @Autowired private com.carry.repository.ComplaintRepository complaintRepository;

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
    void testPasswordHashNotSerialized() throws Exception {
        RegisterRequest customerReg = new RegisterRequest();
        customerReg.setFullName("Security Test");
        customerReg.setStudentId("S12345");
        customerReg.setEmail("security@test.com");
        customerReg.setPhone("01700000001");
        customerReg.setDepartment("CSE");
        customerReg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerReg)))
                .andExpect(status().isOk());

        LoginRequest customerLog = new LoginRequest();
        customerLog.setEmail("security@test.com");
        customerLog.setPassword("password123");
        
        MvcResult customerRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerLog)))
                .andExpect(status().isOk()).andReturn();
        String customerToken = "Bearer " + objectMapper.readTree(customerRes.getResponse().getContentAsString()).get("token").asText();

        MvcResult meRes = mockMvc.perform(get("/api/users/me")
                .header("Authorization", customerToken))
                .andExpect(status().isOk()).andReturn();

        String responseJson = meRes.getResponse().getContentAsString();
        
        // Assert passwordHash is NOT present in the JSON response
        assertFalse(responseJson.contains("passwordHash"), "passwordHash should never be serialized in JSON responses");
        assertFalse(responseJson.contains("password"), "password should not be serialized");
    }
}
