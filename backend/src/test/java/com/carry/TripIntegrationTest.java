package com.carry;

import com.carry.dto.CreateTripRequest;
import com.carry.dto.UpdateTripStatusRequest;
import com.carry.entity.LocationArea;
import com.carry.entity.Trip;
import com.carry.entity.TripStatus;
import com.carry.entity.User;
import com.carry.repository.TripRepository;
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

import java.time.LocalDateTime;

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
class TripIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;
    
    @Autowired private com.carry.repository.ProductRequestRepository productRequestRepository;
    @Autowired private com.carry.repository.StatusUpdateRepository statusUpdateRepository;
    @Autowired private com.carry.repository.PaymentRepository paymentRepository;
    @Autowired private com.carry.repository.RatingRepository ratingRepository;
    @Autowired private com.carry.repository.ComplaintRepository complaintRepository;

    @Autowired
    private JwtService jwtService;

    private User partnerUser;
    private User otherUser;
    private String partnerToken;
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
        
        partnerUser = User.builder()
                .fullName("Partner User")
                .studentId("1707001")
                .email("partner@stud.kuet.ac.bd")
                .passwordHash("hashed")
                .build();
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateToken(UserDetailsImpl.build(partnerUser));

        otherUser = User.builder()
                .fullName("Other User")
                .studentId("1707002")
                .email("other@stud.kuet.ac.bd")
                .passwordHash("hashed")
                .build();
        otherUser = userRepository.save(otherUser);
        otherUserToken = jwtService.generateToken(UserDetailsImpl.build(otherUser));
    }

    @Test
    void testCreateTripSuccess() throws Exception {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        LocalDateTime expectedReturn = departure.plusHours(4);

        CreateTripRequest request = CreateTripRequest.builder()
                .departureTime(departure)
                .expectedReturnTime(expectedReturn)
                .destinationArea(LocationArea.NEW_MARKET)
                .capacityNotes("Can carry up to 3 small packages")
                .build();

        mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.partnerId").value(partnerUser.getId()))
                .andExpect(jsonPath("$.destinationArea").value("NEW_MARKET"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void testCreateTripInvalidDates() throws Exception {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        LocalDateTime expectedReturn = departure.minusHours(1); // Invalid: return before departure

        CreateTripRequest request = CreateTripRequest.builder()
                .departureTime(departure)
                .expectedReturnTime(expectedReturn)
                .destinationArea(LocationArea.SONADANGA)
                .build();

        mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Expected return time must be after departure time"));
    }

    @Test
    void testGetMyTrips() throws Exception {
        Trip trip1 = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .destinationArea(LocationArea.GOLLAMARI)
                .status(TripStatus.PLANNED)
                .build();
        tripRepository.save(trip1);

        Trip trip2 = Trip.builder()
                .partner(otherUser)
                .departureTime(LocalDateTime.now().plusDays(2))
                .expectedReturnTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .destinationArea(LocationArea.DAULATPUR)
                .status(TripStatus.PLANNED)
                .build();
        tripRepository.save(trip2);

        mockMvc.perform(get("/api/trips/mine")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].partnerId").value(partnerUser.getId()));
    }

    @Test
    void testGetActiveTrips() throws Exception {
        Trip planned = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .destinationArea(LocationArea.KUET_AREA)
                .status(TripStatus.PLANNED)
                .build();
        Trip inCity = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusHours(1))
                .expectedReturnTime(LocalDateTime.now().plusHours(4))
                .destinationArea(LocationArea.SHIB_BARI)
                .status(TripStatus.IN_CITY)
                .build();
        Trip completed = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().minusHours(5))
                .expectedReturnTime(LocalDateTime.now().minusHours(1))
                .destinationArea(LocationArea.ELECTRONICS_MARKET)
                .status(TripStatus.COMPLETED)
                .build();
        Trip cancelled = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(3))
                .expectedReturnTime(LocalDateTime.now().plusDays(3).plusHours(2))
                .destinationArea(LocationArea.OTHER)
                .status(TripStatus.CANCELLED)
                .build();

        tripRepository.save(planned);
        tripRepository.save(inCity);
        tripRepository.save(completed);
        tripRepository.save(cancelled);

        mockMvc.perform(get("/api/trips/active")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testNonOwnerGets403OnStatusUpdate() throws Exception {
        Trip trip = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .destinationArea(LocationArea.NEW_MARKET)
                .status(TripStatus.PLANNED)
                .build();
        trip = tripRepository.save(trip);

        UpdateTripStatusRequest updateRequest = new UpdateTripStatusRequest(TripStatus.IN_CITY);

        // otherUser attempts to update partnerUser's trip
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owning partner can update this trip's status"));
    }

    @Test
    void testInvalidStatusTransitionGets400() throws Exception {
        Trip trip = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .destinationArea(LocationArea.NEW_MARKET)
                .status(TripStatus.PLANNED)
                .build();
        trip = tripRepository.save(trip);

        // 1. Invalid: PLANNED -> COMPLETED (skipping stages)
        UpdateTripStatusRequest invalidSkip = new UpdateTripStatusRequest(TripStatus.COMPLETED);
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSkip)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from PLANNED to COMPLETED"));

        // 2. Set trip to COMPLETED directly in DB, then try COMPLETED -> PLANNED
        trip.setStatus(TripStatus.COMPLETED);
        trip = tripRepository.save(trip);

        UpdateTripStatusRequest invalidFromCompleted = new UpdateTripStatusRequest(TripStatus.PLANNED);
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFromCompleted)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from COMPLETED to PLANNED"));

        // 3. Set trip to RETURNING, then try RETURNING -> CANCELLED (cancelled only allowed from PLANNED or IN_CITY)
        trip.setStatus(TripStatus.RETURNING);
        trip = tripRepository.save(trip);

        UpdateTripStatusRequest invalidCancel = new UpdateTripStatusRequest(TripStatus.CANCELLED);
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCancel)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from RETURNING to CANCELLED"));
    }

    @Test
    void testValidStatusLifecycleTransitions() throws Exception {
        Trip trip = Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .destinationArea(LocationArea.NEW_MARKET)
                .status(TripStatus.PLANNED)
                .build();
        trip = tripRepository.save(trip);

        // PLANNED -> IN_CITY
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTripStatusRequest(TripStatus.IN_CITY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_CITY"));

        // IN_CITY -> RETURNING
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTripStatusRequest(TripStatus.RETURNING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNING"));

        // RETURNING -> COMPLETED
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTripStatusRequest(TripStatus.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualReturnTime").exists());

        Trip finalTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertEquals(TripStatus.COMPLETED, finalTrip.getStatus());
    }

    @Test
    void testCancellationTransitions() throws Exception {
        // PLANNED -> CANCELLED
        Trip trip1 = tripRepository.save(Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .destinationArea(LocationArea.NEW_MARKET)
                .status(TripStatus.PLANNED)
                .build());

        mockMvc.perform(put("/api/trips/" + trip1.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTripStatusRequest(TripStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // IN_CITY -> CANCELLED
        Trip trip2 = tripRepository.save(Trip.builder()
                .partner(partnerUser)
                .departureTime(LocalDateTime.now().plusDays(1))
                .expectedReturnTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .destinationArea(LocationArea.NEW_MARKET)
                .status(TripStatus.IN_CITY)
                .build());

        mockMvc.perform(put("/api/trips/" + trip2.getId() + "/status")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTripStatusRequest(TripStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
