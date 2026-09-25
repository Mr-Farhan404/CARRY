package com.carry.service;

import com.carry.dto.CreateRatingDto;
import com.carry.dto.RatingResponseDto;
import com.carry.entity.ProductRequest;
import com.carry.entity.Rating;
import com.carry.entity.RequestStatus;
import com.carry.entity.User;
import com.carry.exception.BadRequestException;
import com.carry.exception.ConflictException;
import com.carry.exception.ForbiddenException;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.ProductRequestRepository;
import com.carry.repository.RatingRepository;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ProductRequestRepository productRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public RatingResponseDto createRating(Long requestId, CreateRatingDto dto, UserDetailsImpl currentUser) {
        ProductRequest request = productRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (!request.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the customer who created this request can rate it");
        }

        if (request.getStatus() != RequestStatus.DELIVERED) {
            throw new BadRequestException("Request can only be rated when status is DELIVERED. Current status is " + request.getStatus());
        }

        if (ratingRepository.existsByRequestId(requestId)) {
            throw new ConflictException("This request has already been rated");
        }

        if (request.getMatchedTrip() == null || request.getMatchedTrip().getPartner() == null) {
            throw new BadRequestException("Request does not have an assigned delivery partner to rate");
        }

        User ratedBy = request.getCustomer();
        Long partnerId = request.getMatchedTrip().getPartner().getId();
        User ratedUser = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner user not found with id: " + partnerId));

        Rating rating = Rating.builder()
                .request(request)
                .ratedBy(ratedBy)
                .ratedUser(ratedUser)
                .score(dto.getScore())
                .comment(dto.getComment())
                .build();

        Rating savedRating = ratingRepository.saveAndFlush(rating);

        Double avgScore = ratingRepository.findAverageScoreByRatedUserId(ratedUser.getId());
        if (avgScore != null) {
            BigDecimal roundedAvg = BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP);
            ratedUser.setAvgRating(roundedAvg);
            userRepository.save(ratedUser);
        }

        return RatingResponseDto.fromEntity(savedRating);
    }

    @Transactional(readOnly = true)
    public RatingResponseDto getRatingByRequestId(Long requestId, UserDetailsImpl currentUser) {
        ProductRequest request = productRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        boolean isCustomer = request.getCustomer().getId().equals(currentUser.getId());
        boolean isPartner = request.getMatchedTrip() != null && request.getMatchedTrip().getPartner().getId().equals(currentUser.getId());
        boolean isAdmin = Boolean.TRUE.equals(currentUser.getIsAdmin());

        if (!isCustomer && !isPartner && !isAdmin) {
            throw new ForbiddenException("You do not have permission to view this rating");
        }

        Rating rating = ratingRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("No rating found for request id: " + requestId));

        return RatingResponseDto.fromEntity(rating);
    }
}
