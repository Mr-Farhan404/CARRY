package com.carry.app.api;

import com.carry.app.models.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Auth
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("api/users/me")
    Call<User> getMe();

    // Products
    @GET("api/products")
    Call<List<Product>> getProducts(
            @Query("category") String category,
            @Query("search") String search
    );

    @GET("api/products/{id}")
    Call<Product> getProductById(@Path("id") Long id);

    // Requests / Orders
    @POST("api/requests")
    Call<ProductRequest> createRequest(@Body CreateProductRequest request);

    @POST("api/requests/checkout")
    Call<List<ProductRequest>> checkout(@Body List<CheckoutLineDto> cartLines);

    @GET("api/requests/mine")
    Call<List<ProductRequest>> getMyRequests();

    @GET("api/requests/{id}")
    Call<ProductRequest> getRequestById(@Path("id") Long id);

    @GET("api/requests/{id}/timeline")
    Call<List<StatusUpdate>> getRequestTimeline(@Path("id") Long id);

    @PUT("api/requests/{id}/cancel")
    Call<ProductRequest> cancelRequest(@Path("id") Long id);

    @GET("api/requests/available")
    Call<List<ProductRequest>> getAvailableRequests();

    @GET("api/requests/assigned")
    Call<List<ProductRequest>> getPartnerDeliveries();

    @PUT("api/requests/{id}/accept")
    Call<ProductRequest> acceptRequest(@Path("id") Long id, @Body AcceptRequestDto acceptDto);

    @PUT("api/requests/{id}/status")
    Call<ProductRequest> updateRequestStatus(@Path("id") Long id, @Body UpdateRequestStatusDto updateDto);

    @POST("api/requests/{id}/need-more")
    Call<Payment> requestNeedMore(@Path("id") Long id, @Body NeedMorePaymentDto dto);

    @POST("api/requests/{id}/additional-payment")
    Call<Payment> submitAdditionalPayment(@Path("id") Long id, @Body SubmitAdditionalPaymentDto dto);

    @GET("api/requests/{id}/payment")
    Call<Payment> getPayment(@Path("id") Long id);

    // Trips / Deliveries
    @POST("api/trips")
    Call<Trip> createTrip(@Body CreateTripRequest request);

    @GET("api/trips/mine")
    Call<List<Trip>> getMyTrips();

    @GET("api/trips/active")
    Call<List<Trip>> getActiveTrips();

    @PUT("api/trips/{id}/status")
    Call<Trip> updateTripStatus(@Path("id") Long id, @Body UpdateTripStatusRequest request);

    // Role Applications
    @POST("api/users/apply-role")
    Call<RoleApplication> applyRole(@Body ApplyRoleDto dto);

    @GET("api/users/my-applications")
    Call<List<RoleApplication>> getMyApplications();

    // Moderator Endpoints
    @GET("api/moderator/applications")
    Call<List<RoleApplication>> getPendingPartnerApplications();

    @GET("api/moderator/applications/all")
    Call<List<RoleApplication>> getAllPartnerApplications();

    @PUT("api/moderator/applications/{id}/approve")
    Call<RoleApplication> approvePartnerApplication(@Path("id") Long id, @Query("notes") String notes);

    @PUT("api/moderator/applications/{id}/reject")
    Call<RoleApplication> rejectPartnerApplication(@Path("id") Long id, @Query("notes") String notes);

    // Products Management (Admin & Moderator)
    @GET("api/moderator/products")
    Call<List<Product>> getModeratorProducts();

    @POST("api/moderator/products")
    Call<Product> createModeratorProduct(@Body CreateProductDto dto);

    @PUT("api/moderator/products/{id}")
    Call<Product> updateModeratorProduct(@Path("id") Long id, @Body UpdateProductDto dto);

    @PATCH("api/moderator/products/{id}/status")
    Call<Product> toggleProductStatus(@Path("id") Long id, @Query("isActive") Boolean isActive);

    // Campus Orders Management (Admin & Moderator)
    @GET("api/moderator/requests")
    Call<List<ProductRequest>> getAllCampusOrders();

    // Admin Endpoints
    @GET("api/admin/users")
    Call<List<User>> getAllUsers();

    @DELETE("api/admin/users/{id}")
    Call<Void> deleteUser(@Path("id") Long id);

    @GET("api/admin/moderator-reports")
    Call<List<ModeratorReport>> getModeratorReports();

    @GET("api/admin/moderator-applications")
    Call<List<RoleApplication>> getModeratorApplications();

    @PUT("api/admin/moderator-applications/{id}/approve")
    Call<RoleApplication> approveModeratorApplication(@Path("id") Long id, @Query("notes") String notes);

    @PUT("api/admin/moderator-applications/{id}/reject")
    Call<RoleApplication> rejectModeratorApplication(@Path("id") Long id, @Query("notes") String notes);
}
