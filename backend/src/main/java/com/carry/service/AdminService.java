package com.carry.service;

import com.carry.dto.AdminVerifyPaymentDto;
import com.carry.dto.ComplaintResponseDto;
import com.carry.dto.ModeratorReportDto;
import com.carry.dto.PaymentResponseDto;
import com.carry.dto.ProductRequestResponseDto;
import com.carry.dto.RoleApplicationResponseDto;
import com.carry.dto.UpdateComplaintStatusDto;
import com.carry.dto.UserDto;
import com.carry.entity.Complaint;
import com.carry.entity.ComplaintStatus;
import com.carry.entity.Payment;
import com.carry.entity.ProductRequest;
import com.carry.entity.RequestStatus;
import com.carry.entity.RoleApplication;
import com.carry.entity.Trip;
import com.carry.entity.User;
import com.carry.exception.ForbiddenException;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.ComplaintRepository;
import com.carry.repository.ModeratorReportRepository;
import com.carry.repository.PaymentRepository;
import com.carry.repository.ProductRequestRepository;
import com.carry.repository.RatingRepository;
import com.carry.repository.RoleApplicationRepository;
import com.carry.repository.StatusUpdateRepository;
import com.carry.repository.TripRepository;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ProductRequestRepository productRequestRepository;
    private final PaymentRepository paymentRepository;
    private final RatingRepository ratingRepository;
    private final RoleApplicationRepository roleApplicationRepository;
    private final ModeratorReportRepository moderatorReportRepository;
    private final StatusUpdateRepository statusUpdateRepository;
    private final TripRepository tripRepository;
    private final ModeratorReportService moderatorReportService;
    private final RoleApplicationService roleApplicationService;

    /**
     * Reusable admin verification check used across all admin operations.
     */
    public void verifyAdmin(UserDetailsImpl currentUser) {
        if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
            throw new ForbiddenException("Access denied: Admin privileges required");
        }
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDto> getAllComplaints(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ComplaintResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public ComplaintResponseDto updateComplaint(Long id, UpdateComplaintStatusDto dto, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));

        complaint.setStatus(dto.getStatus());
        complaint.setAdminNotes(dto.getAdminNotes());

        if (dto.getStatus() == ComplaintStatus.RESOLVED && complaint.getResolvedAt() == null) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        Complaint updated = complaintRepository.save(complaint);
        return ComplaintResponseDto.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return userRepository.findAll()
                .stream()
                .map(UserDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductRequestResponseDto> getAllRequests(RequestStatus status, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        List<ProductRequest> requests;
        if (status != null) {
            requests = productRequestRepository.findByStatus(status);
        } else {
            requests = productRequestRepository.findAll();
        }

        return requests.stream()
                .map(ProductRequestResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PaymentResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public PaymentResponseDto verifyPayment(Long id, AdminVerifyPaymentDto dto, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (dto.getStatus() != null) {
            payment.setStatus(dto.getStatus());
        }
        if (dto.getAdditionalPaymentStatus() != null) {
            payment.setAdditionalPaymentStatus(dto.getAdditionalPaymentStatus());
        }
        if (dto.getAdminNotes() != null) {
            payment.setAdminNotes(dto.getAdminNotes());
        }

        Payment updated = paymentRepository.save(payment);
        return PaymentResponseDto.fromEntity(updated);
    }

    @Transactional
    public void deleteUser(Long userId, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Admin cannot delete their own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        log.warn("ADMIN [{}] is deleting user [{}] (role: {})", currentUser.getEmail(), user.getEmail(), user.getRole());

        // 1. Delete user's product requests and associated status updates, ratings
        List<ProductRequest> requests = productRequestRepository.findByCustomerId(userId);
        for (ProductRequest req : requests) {
            ratingRepository.findByRequestId(req.getId()).ifPresent(ratingRepository::delete);
            statusUpdateRepository.deleteByRequestId(req.getId());
            productRequestRepository.delete(req);
        }

        // 2. Unassign any product requests where this user was the trip partner
        List<Trip> trips = tripRepository.findByPartnerId(userId);
        for (Trip trip : trips) {
            List<ProductRequest> tripRequests = productRequestRepository.findByMatchedTripId(trip.getId());
            for (ProductRequest req : tripRequests) {
                req.setMatchedTrip(null);
                productRequestRepository.save(req);
            }
            tripRepository.delete(trip);
        }

        // 3. Clean ratings and complaints
        ratingRepository.deleteByRatedByIdOrRatedUserId(userId, userId);
        complaintRepository.deleteByRaisedByIdOrAgainstUserId(userId, userId);

        // 4. Clean role applications
        roleApplicationRepository.deleteByUserId(userId);
        List<RoleApplication> reviewedApps = roleApplicationRepository.findByReviewedById(userId);
        for (RoleApplication ra : reviewedApps) {
            ra.setReviewedBy(null);
            roleApplicationRepository.save(ra);
        }

        // 5. Clean moderator reports
        moderatorReportRepository.deleteByModeratorId(userId);

        // 6. Delete the user
        userRepository.delete(user);
        log.info("User [{}] deleted successfully by ADMIN [{}]", user.getEmail(), currentUser.getEmail());
    }

    @Transactional(readOnly = true)
    public List<ModeratorReportDto> getModeratorReports(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return moderatorReportService.getAllReports();
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationResponseDto> getPendingModeratorApplications(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return roleApplicationService.getPendingModeratorApplications();
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationResponseDto> getAllModeratorApplications(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return roleApplicationService.getAllModeratorApplications();
    }

    @Transactional
    public RoleApplicationResponseDto reviewModeratorApplication(Long id, boolean approve, String notes, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return roleApplicationService.reviewModeratorApplication(id, approve, notes, currentUser.getId());
    }
}
