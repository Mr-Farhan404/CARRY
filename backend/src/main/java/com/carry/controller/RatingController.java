package com.carry.controller;

import com.carry.dto.CreateRatingDto;
import com.carry.dto.RatingResponseDto;
import com.carry.security.UserDetailsImpl;
import com.carry.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/{id}/rating")
    public ResponseEntity<RatingResponseDto> createRating(
            @PathVariable Long id,
            @Valid @RequestBody CreateRatingDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        RatingResponseDto response = ratingService.createRating(id, dto, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/rating")
    public ResponseEntity<RatingResponseDto> getRating(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ratingService.getRatingByRequestId(id, currentUser));
    }
}
