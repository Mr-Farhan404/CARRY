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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setStudentId("1907000");
        request.setEmail("test@stud.kuet.ac.bd");
        request.setPassword("securepassword");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User saved = userRepository.findByEmail("test@stud.kuet.ac.bd").orElseThrow();
        assertFalse(saved.getPasswordHash().equals("securepassword")); // hashed
    }

    @Test
    void testDuplicateEmailRegistration() throws Exception {
        User existing = User.builder()
                .fullName("Existing")
                .studentId("1111111")
                .email("duplicate@stud.kuet.ac.bd")
                .passwordHash("hash")
                .build();
        userRepository.save(existing);

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setStudentId("2222222");
        request.setEmail("duplicate@stud.kuet.ac.bd");
        request.setPassword("securepassword");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    void testValidationFailureOnRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(""); // blank
        request.setStudentId("1907000");
        request.setEmail("invalid-email");
        request.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fullName").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void testSuccessfulLoginAndProtectedEndpoint() throws Exception {
        User existing = User.builder()
                .fullName("Login Test")
                .studentId("3333333")
                .email("login@stud.kuet.ac.bd")
                .passwordHash(passwordEncoder.encode("mypassword"))
                .build();
        userRepository.save(existing);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@stud.kuet.ac.bd");
        loginRequest.setPassword("mypassword");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Test protected endpoint
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@stud.kuet.ac.bd"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void testWrongPasswordLogin() throws Exception {
        User existing = User.builder()
                .fullName("Login Test")
                .studentId("4444444")
                .email("wrongpass@stud.kuet.ac.bd")
                .passwordHash(passwordEncoder.encode("correctpass"))
                .build();
        userRepository.save(existing);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongpass@stud.kuet.ac.bd");
        loginRequest.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden()); // or isUnauthorized depending on entry point
    }

    @Test
    void testLoginRateLimiting() throws Exception {
        LoginRequest badLogin = new LoginRequest();
        badLogin.setEmail("nonexistent@stud.kuet.ac.bd");
        badLogin.setPassword("wrongpassword");

        // First 5 failed attempts from the same IP should fail with 401 Unauthorized
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "203.0.113.42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt from the same IP must be rate-limited with 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many login attempts. Please try again later."));
    }
}
