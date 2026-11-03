package com.carry.controller;

import com.carry.dto.*;
import com.carry.entity.RequestStatus;
import com.carry.security.UserDetailsImpl;
import com.carry.service.ProductRequestService;
import com.carry.service.ProductService;
import com.carry.service.RoleApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderator")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class ModeratorController {

    private final RoleApplicationService roleApplicationService;
    private final ProductService productService;
    private final ProductRequestService productRequestService;

    // Delivery Partner Applications Management
    @GetMapping("/applications")
    public ResponseEntity<List<RoleApplicationResponseDto>> getPendingPartnerApplications() {
        return ResponseEntity.ok(roleApplicationService.getPendingDeliveryPartnerApplications());
    }

    @GetMapping("/applications/all")
    public ResponseEntity<List<RoleApplicationResponseDto>> getAllPartnerApplications() {
        return ResponseEntity.ok(roleApplicationService.getAllDeliveryPartnerApplications());
    }

    @PutMapping("/applications/{id}/approve")
    public ResponseEntity<RoleApplicationResponseDto> approvePartnerApplication(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(roleApplicationService.reviewDeliveryPartnerApplication(id, true, notes, currentUser.getId()));
    }

    @PutMapping("/applications/{id}/reject")
    public ResponseEntity<RoleApplicationResponseDto> rejectPartnerApplication(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(roleApplicationService.reviewDeliveryPartnerApplication(id, false, notes, currentUser.getId()));
    }

    // Product Catalog Management (All changes by moderator are logged to moderator_reports)
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productService.getAllProductsForAdmin(currentUser));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody CreateProductDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return new ResponseEntity<>(productService.createProduct(dto, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @RequestBody UpdateProductDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productService.updateProduct(id, dto, currentUser));
    }

    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ProductResponseDto> toggleProductStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean isActive,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productService.toggleProductActive(id, isActive, currentUser));
    }

    // View Order Details
    @GetMapping("/requests")
    public ResponseEntity<List<ProductRequestResponseDto>> getOrders(
            @RequestParam(required = false) RequestStatus status,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        // Moderator can view all orders
        return ResponseEntity.ok(productRequestService.getAllRequests(status));
    }
}
