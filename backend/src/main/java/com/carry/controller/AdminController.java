package com.carry.controller;

import com.carry.dto.*;
import com.carry.entity.RequestStatus;
import com.carry.security.UserDetailsImpl;
import com.carry.service.AdminService;
import com.carry.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ProductService productService;

    @GetMapping("/complaints")
    public ResponseEntity<List<ComplaintResponseDto>> getComplaints(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.getAllComplaints(currentUser));
    }

    @PutMapping("/complaints/{id}")
    public ResponseEntity<ComplaintResponseDto> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComplaintStatusDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.updateComplaint(id, dto, currentUser));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.getAllUsers(currentUser));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<ProductRequestResponseDto>> getRequests(
            @RequestParam(required = false) RequestStatus status,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.getAllRequests(status, currentUser));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponseDto>> getPayments(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.getAllPayments(currentUser));
    }

    @PutMapping("/payments/{id}/verify")
    public ResponseEntity<PaymentResponseDto> verifyPayment(
            @PathVariable Long id,
            @RequestBody AdminVerifyPaymentDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.verifyPayment(id, dto, currentUser));
    }

    // Admin Product Catalog Management
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

    @PutMapping("/products/{id}/deactivate")
    public ResponseEntity<ProductResponseDto> deactivateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productService.toggleProductActive(id, false, currentUser));
    }
}
