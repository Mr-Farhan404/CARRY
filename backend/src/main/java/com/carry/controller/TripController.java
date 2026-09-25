package com.carry.controller;

import com.carry.dto.CreateTripRequest;
import com.carry.dto.TripResponseDto;
import com.carry.dto.UpdateTripStatusRequest;
import com.carry.security.UserDetailsImpl;
import com.carry.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponseDto> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        TripResponseDto response = tripService.createTrip(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<TripResponseDto>> getMyTrips(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(tripService.getMyTrips(currentUser));
    }

    @GetMapping("/active")
    public ResponseEntity<List<TripResponseDto>> getActiveTrips() {
        return ResponseEntity.ok(tripService.getActiveTrips());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TripResponseDto> updateTripStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTripStatusRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(tripService.updateTripStatus(id, request, currentUser));
    }
}
