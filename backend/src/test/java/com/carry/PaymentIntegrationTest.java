package com.carry;

import com.carry.dto.AcceptRequestDto;
import com.carry.dto.AdminVerifyPaymentDto;
import com.carry.dto.CreateProductRequest;
import com.carry.dto.NeedMorePaymentDto;
import com.carry.dto.SubmitAdditionalPaymentDto;
import com.carry.dto.UpdateRequestStatusDto;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRequestRepository productRequestRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StatusUpdateRepository statusUpdateRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private JwtService jwtService;

    private User customerUser;
    private User partnerUser;
    private User adminUser;
    private String customerToken;
    private String partnerToken;
    private String adminToken;
    private Trip partnerTrip;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        statusUpdateRepository.deleteAllInBatch();
        productRequestRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        customerUser = userRepository.save(User.builder()
                .fullName("Test Customer")
                .studentId("1807001")
                .email("customer@kuet.ac.bd")
                .passwordHash("hashed")
                .isAdmin(false)
                .build());
        customerToken = jwtService.generateToken(UserDetailsImpl.build(customerUser));

        partnerUser = userRepository.save(User.builder()
                .fullName("Test Partner")
                .studentId("1807002")
                .email("partner@kuet.ac.bd")
                .passwordHash("hashed")
                .isAdmin(false)
                .build());
        partnerToken = jwtService.generateToken(UserDetailsImpl.build(partnerUser));

        adminUser = userRepository.save(User.builder()
                .fullName("Admin Farhan")
                .studentId("1807000")
                .email("admin@kuet.ac.bd")
                .passwordHash("hashed")
                .isAdmin(true)
                .build());
        adminToken = jwtService.generateToken(UserDetailsImpl.build(adminUser));

        partnerTrip = tripRepository.save(Trip.builder()
                .partner(partnerUser)
                .destinationArea(LocationArea.NEW_MARKET)
                .departureTime(LocalDateTime.now().plusHours(2))
                .expectedReturnTime(LocalDateTime.now().plusHours(4))
                .status(TripStatus.PLANNED)
                .build());
    }

    @Test
    void testUpfrontPaymentCreationAndCalculation() throws Exception {
        // Customer creates an order with budget 350, paying via bKash
        CreateProductRequest request = CreateProductRequest.builder()
                .productName("Calculus Textbook")
                .category("Books")
                .quantity(1)
                .preferredShop("Book Zone")
                .pickupArea(LocationArea.NEW_MARKET)
                .budget(new BigDecimal("350.00"))
                .instructions("Latest edition please")
                .paymentMethod(PaymentMethod.BKASH)
                .senderPhone("01712345678")
                .trxId("TRX123456789")
                .build();

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.payment.productCost").value(350.00))
                .andExpect(jsonPath("$.payment.deliveryFee").value(30.00))
                // 350 * 0.0139 = 4.865 -> 4.87
                .andExpect(jsonPath("$.payment.gatewayFee").value(4.87))
                // 350 + 30 + 4.87 = 384.87
                .andExpect(jsonPath("$.payment.total").value(384.87))
                .andExpect(jsonPath("$.payment.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.payment.paymentMethod").value("BKASH"))
                .andExpect(jsonPath("$.payment.senderPhone").value("01712345678"))
                .andExpect(jsonPath("$.payment.trxId").value("TRX123456789"))
                .andExpect(jsonPath("$.payment.additionalPaymentStatus").value("NONE"));
    }

    @Test
    void testNeedMorePriceAdjustmentWorkflowAndCollectionBlocking() throws Exception {
        // 1. Create order
        ProductRequest productRequest = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Engineering Drawing Kit")
                .pickupArea(LocationArea.NEW_MARKET)
                .budget(new BigDecimal("350.00"))
                .quantity(1)
                .status(RequestStatus.REQUESTED)
                .build());

        Payment initialPayment = paymentRepository.save(Payment.builder()
                .request(productRequest)
                .productCost(new BigDecimal("350.00"))
                .deliveryFee(new BigDecimal("30.00"))
                .gatewayFee(new BigDecimal("4.87"))
                .total(new BigDecimal("384.87"))
                .status(PaymentStatus.SUBMITTED)
                .paymentMethod(PaymentMethod.BKASH)
                .senderPhone("01711112222")
                .trxId("TRXINIT")
                .additionalPaymentStatus(AdditionalPaymentStatus.NONE)
                .build());
        productRequest.setPayment(initialPayment);

        // 2. Partner accepts request
        AcceptRequestDto acceptDto = new AcceptRequestDto();
        acceptDto.setTripId(partnerTrip.getId());
        acceptDto.setVersion(productRequest.getVersion());

        mockMvc.perform(put("/api/requests/" + productRequest.getId() + "/accept")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 3. Partner requests "Need More": 40 taka extra needed
        NeedMorePaymentDto needMoreDto = new NeedMorePaymentDto();
        needMoreDto.setAdditionalAmount(new BigDecimal("40.00"));
        needMoreDto.setReason("Shopkeeper said retail price increased to 390 taka");

        mockMvc.perform(post("/api/requests/" + productRequest.getId() + "/need-more")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(needMoreDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalAmount").value(40.00))
                // 40 * 0.0139 = 0.556 -> 0.56
                .andExpect(jsonPath("$.additionalFee").value(0.56))
                // 40 + 0.56 = 40.56
                .andExpect(jsonPath("$.additionalTotal").value(40.56))
                .andExpect(jsonPath("$.additionalPaymentStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.needMoreReason").value("Shopkeeper said retail price increased to 390 taka"));

        // 4. Partner attempts to mark as COLLECTED before customer pays -> MUST BE BLOCKED (400)
        UpdateRequestStatusDto collectDto = new UpdateRequestStatusDto();
        collectDto.setStatus(RequestStatus.COLLECTED);
        collectDto.setNote("Bought items from shop");

        mockMvc.perform(put("/api/requests/" + productRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collectDto)))
                .andExpect(status().isBadRequest());

        // 5. Customer submits the remaining payment via Nagad
        SubmitAdditionalPaymentDto submitDto = new SubmitAdditionalPaymentDto();
        submitDto.setPaymentMethod(PaymentMethod.NAGAD);
        submitDto.setSenderPhone("01999998888");
        submitDto.setTrxId("NAGAD40EXTRA");

        mockMvc.perform(post("/api/requests/" + productRequest.getId() + "/additional-payment")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalPaymentStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.additionalPaymentMethod").value("NAGAD"))
                .andExpect(jsonPath("$.additionalTrxId").value("NAGAD40EXTRA"));

        // 6. Partner can now advance to COLLECTED without being blocked!
        mockMvc.perform(put("/api/requests/" + productRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collectDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTED"));
    }

    @Test
    void testAdminPaymentInspectionAndVerification() throws Exception {
        // Setup request and payment
        ProductRequest req = productRequestRepository.save(ProductRequest.builder()
                .customer(customerUser)
                .productName("Wireless Mouse")
                .pickupArea(LocationArea.NEW_MARKET)
                .budget(new BigDecimal("500.00"))
                .quantity(1)
                .status(RequestStatus.REQUESTED)
                .build());

        Payment payment = paymentRepository.save(Payment.builder()
                .request(req)
                .productCost(new BigDecimal("500.00"))
                .deliveryFee(new BigDecimal("30.00"))
                .gatewayFee(new BigDecimal("6.95"))
                .total(new BigDecimal("536.95"))
                .status(PaymentStatus.SUBMITTED)
                .paymentMethod(PaymentMethod.BKASH)
                .senderPhone("01700000000")
                .trxId("TRXADMINTEST")
                .additionalPaymentStatus(AdditionalPaymentStatus.NONE)
                .build());
        req.setPayment(payment);

        // 1. Admin gets all payments
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trxId").value("TRXADMINTEST"))
                .andExpect(jsonPath("$[0].customerEmail").value("customer@kuet.ac.bd"))
                .andExpect(jsonPath("$[0].productName").value("Wireless Mouse"));

        // 2. Admin verifies the payment
        AdminVerifyPaymentDto verifyDto = AdminVerifyPaymentDto.builder()
                .status(PaymentStatus.VERIFIED)
                .additionalPaymentStatus(AdditionalPaymentStatus.VERIFIED)
                .adminNotes("Verified via bKash statement")
                .build();

        mockMvc.perform(put("/api/admin/payments/" + payment.getId() + "/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.additionalPaymentStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.adminNotes").value("Verified via bKash statement"));
    }
}
