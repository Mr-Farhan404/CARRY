package com.carry;

import com.carry.entity.*;
import com.carry.repository.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EntityRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private ProductRequestRepository productRequestRepository;
    @Autowired private StatusUpdateRepository statusUpdateRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private PaymentRepository paymentRepository;

    @org.junit.jupiter.api.BeforeEach
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
    void testSaveAndRetrieveEntities() {
        // 1. Users
        User customer = User.builder()
                .fullName("John Doe")
                .studentId("1807001")
                .email("john@stud.kuet.ac.bd")
                .passwordHash("hashedpassword123")
                .phone("01700000000")
                .build();
        customer = userRepository.save(customer);

        User partner = User.builder()
                .fullName("Jane Smith")
                .studentId("1807002")
                .email("jane@stud.kuet.ac.bd")
                .passwordHash("hashedpassword456")
                .build();
        partner = userRepository.save(partner);

        // 2. Trip
        Trip trip = Trip.builder()
                .partner(partner)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(5))
                .destinationArea(LocationArea.NEW_MARKET)
                .status(TripStatus.PLANNED)
                .build();
        trip = tripRepository.save(trip);

        // 3. Product Request
        ProductRequest request = ProductRequest.builder()
                .customer(customer)
                .productName("Arduino Uno")
                .pickupArea(LocationArea.ELECTRONICS_MARKET)
                .budget(new BigDecimal("1500.00"))
                .status(RequestStatus.REQUESTED)
                .matchedTrip(trip)
                .build();
        request = productRequestRepository.save(request);

        // 4. Status Update
        StatusUpdate update = StatusUpdate.builder()
                .request(request)
                .status("ACCEPTED")
                .note("I will buy it today")
                .build();
        statusUpdateRepository.save(update);

        // 5. Complaint
        Complaint complaint = Complaint.builder()
                .request(request)
                .raisedBy(customer)
                .againstUser(partner)
                .description("Did not reply to my calls.")
                .status(ComplaintStatus.OPEN)
                .build();
        complaintRepository.save(complaint);

        // 6. Payment
        Payment payment = Payment.builder()
                .request(request)
                .productCost(new BigDecimal("1500.00"))
                .deliveryFee(new BigDecimal("50.00"))
                .total(new BigDecimal("1550.00"))
                .status(PaymentStatus.SIMULATED_PAID)
                .build();
        paymentRepository.save(payment);

        // 7. Rating
        Rating rating = Rating.builder()
                .request(request)
                .ratedBy(customer)
                .ratedUser(partner)
                .score(5)
                .comment("Great delivery!")
                .build();
        ratingRepository.save(rating);

        // Verification
        assertEquals(2, userRepository.count());
        assertEquals(1, tripRepository.count());
        assertEquals(1, productRequestRepository.count());
        assertEquals(1, statusUpdateRepository.count());
        assertEquals(1, complaintRepository.count());
        assertEquals(1, paymentRepository.count());
        assertEquals(1, ratingRepository.count());
    }

    @Test
    void testRatingConstraintViolation() {
        User customer = User.builder()
                .fullName("C")
                .studentId("111")
                .email("c@c.c")
                .passwordHash("x")
                .build();
        customer = userRepository.save(customer);

        ProductRequest request = ProductRequest.builder()
                .customer(customer)
                .productName("Item")
                .pickupArea(LocationArea.OTHER)
                .status(RequestStatus.REQUESTED)
                .build();
        request = productRequestRepository.save(request);

        Rating invalidRating = Rating.builder()
                .request(request)
                .ratedBy(customer)
                .ratedUser(customer)
                .score(6) // INVALID score (>5)
                .build();

        assertThrows(ConstraintViolationException.class, () -> {
            ratingRepository.saveAndFlush(invalidRating);
        });
    }
}
