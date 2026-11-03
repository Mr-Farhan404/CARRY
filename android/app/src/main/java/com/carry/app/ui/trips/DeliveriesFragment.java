package com.carry.app.ui.trips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.carry.app.R;
import com.carry.app.adapters.AvailableRequestAdapter;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.AcceptRequestDto;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.Trip;
import com.carry.app.models.TripStatus;
import com.carry.app.models.UpdateTripStatusRequest;
import com.carry.app.models.User;
import com.carry.app.models.UserRole;
import com.carry.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveriesFragment extends Fragment implements AvailableRequestAdapter.OnAcceptDeliveryListener {

    private SwipeRefreshLayout swipeRefreshDeliveries;
    private LinearLayout layoutActiveTrip;
    private TextView tvTripStatusBadge;
    private TextView tvTripDestination;
    private TextView tvTripTimes;
    private Button btnTripInCity;
    private Button btnTripReturning;
    private Button btnTripCompleted;

    private LinearLayout layoutNoActiveTrip;
    private Button btnPostTrip;

    private RecyclerView rvAvailableRequests;
    private View layoutAvailableEmpty;
    private AvailableRequestAdapter adapter;

    private Trip currentActiveTrip;
    private ActivityResultLauncher<Intent> postTripLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postTripLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadActiveTripAndRequests();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deliveries, container, false);

        swipeRefreshDeliveries = view.findViewById(R.id.swipeRefreshDeliveries);
        layoutActiveTrip = view.findViewById(R.id.layoutActiveTrip);
        tvTripStatusBadge = view.findViewById(R.id.tvTripStatusBadge);
        tvTripDestination = view.findViewById(R.id.tvTripDestination);
        tvTripTimes = view.findViewById(R.id.tvTripTimes);
        btnTripInCity = view.findViewById(R.id.btnTripInCity);
        btnTripReturning = view.findViewById(R.id.btnTripReturning);
        btnTripCompleted = view.findViewById(R.id.btnTripCompleted);

        layoutNoActiveTrip = view.findViewById(R.id.layoutNoActiveTrip);
        btnPostTrip = view.findViewById(R.id.btnPostTrip);

        rvAvailableRequests = view.findViewById(R.id.rvAvailableRequests);
        layoutAvailableEmpty = view.findViewById(R.id.layoutAvailableEmpty);

        setupRecyclerView();
        setupClickListeners();

        swipeRefreshDeliveries.setOnRefreshListener(this::loadActiveTripAndRequests);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadActiveTripAndRequests();
    }

    private void setupRecyclerView() {
        adapter = new AvailableRequestAdapter(requireContext(), this);
        rvAvailableRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAvailableRequests.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnPostTrip.setOnClickListener(v -> {
            User user = SessionManager.getInstance(requireContext()).getUser();
            UserRole role = user != null ? user.getRole() : UserRole.STUDENT;
            if (role != UserRole.DELIVERY_PARTNER && role != UserRole.ADMIN) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delivery Partner Access Required")
                        .setMessage(role == UserRole.TEACHER ?
                                "Faculty accounts are not authorized to post delivery trips." :
                                "Only approved Delivery Partners can post trips and accept orders. You can apply to become a Delivery Partner in the Profile tab!")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            Intent intent = new Intent(requireContext(), PostTripActivity.class);
            postTripLauncher.launch(intent);
        });

        btnTripInCity.setOnClickListener(v -> updateTripStatus(TripStatus.IN_CITY));
        btnTripReturning.setOnClickListener(v -> updateTripStatus(TripStatus.RETURNING));
        btnTripCompleted.setOnClickListener(v -> updateTripStatus(TripStatus.COMPLETED));
    }

    private void loadActiveTripAndRequests() {
        if (!isAdded()) return;
        swipeRefreshDeliveries.setRefreshing(true);

        ApiService api = ApiClient.getService(requireContext());

        // 1. Fetch my trips to see if there is an active trip
        api.getMyTrips().enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentActiveTrip = null;
                    for (Trip trip : response.body()) {
                        if (trip.getStatus() != null && trip.getStatus().isActive()) {
                            currentActiveTrip = trip;
                            break;
                        }
                    }
                    updateActiveTripUI(currentActiveTrip);
                } else {
                    updateActiveTripUI(null);
                }
                loadAvailableRequests();
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                updateActiveTripUI(null);
                loadAvailableRequests();
            }
        });
    }

    private void updateActiveTripUI(Trip trip) {
        if (!isAdded()) return;

        if (trip != null) {
            layoutActiveTrip.setVisibility(View.VISIBLE);
            layoutNoActiveTrip.setVisibility(View.GONE);

            tvTripStatusBadge.setText(trip.getStatus() != null ? trip.getStatus().getLabel() : "ACTIVE");
            String dest = trip.getDestinationArea() != null ? trip.getDestinationArea().getDisplayName() : "Khulna City";
            tvTripDestination.setText(String.format("Destination: %s", dest));

            String dep = trip.getDepartureTime() != null ? trip.getDepartureTime().replace("T", " ") : "";
            if (dep.length() > 16) dep = dep.substring(0, 16);
            tvTripTimes.setText(String.format("Departure: %s", dep));

            SessionManager.getInstance(requireContext()).setActiveTripId(trip.getId());
        } else {
            layoutActiveTrip.setVisibility(View.GONE);
            layoutNoActiveTrip.setVisibility(View.VISIBLE);
            SessionManager.getInstance(requireContext()).setActiveTripId(null);
        }
    }

    private void loadAvailableRequests() {
        if (!isAdded()) return;

        ApiService api = ApiClient.getService(requireContext());
        api.getAvailableRequests().enqueue(new Callback<List<ProductRequest>>() {
            @Override
            public void onResponse(Call<List<ProductRequest>> call, Response<List<ProductRequest>> response) {
                if (!isAdded()) return;
                swipeRefreshDeliveries.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<ProductRequest> list = response.body();
                    adapter.setRequests(list);
                    layoutAvailableEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    layoutAvailableEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<ProductRequest>> call, Throwable t) {
                if (!isAdded()) return;
                swipeRefreshDeliveries.setRefreshing(false);
                layoutAvailableEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateTripStatus(TripStatus newStatus) {
        if (currentActiveTrip == null || currentActiveTrip.getId() == null) return;

        ApiService api = ApiClient.getService(requireContext());
        api.updateTripStatus(currentActiveTrip.getId(), new UpdateTripStatusRequest(newStatus)).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(requireContext(), "Trip status updated to " + newStatus.getLabel(), Toast.LENGTH_SHORT).show();
                    loadActiveTripAndRequests();
                } else {
                    Toast.makeText(requireContext(), "Failed to update trip status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Trip> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAccept(ProductRequest request) {
        User user = SessionManager.getInstance(requireContext()).getUser();
        UserRole role = user != null ? user.getRole() : UserRole.STUDENT;
        if (role != UserRole.DELIVERY_PARTNER && role != UserRole.ADMIN) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delivery Partner Access Required")
                    .setMessage(role == UserRole.TEACHER ?
                            "Faculty accounts cannot accept deliveries." :
                            "Only approved Delivery Partners can accept order deliveries. Please apply for Delivery Partner status in your Profile tab.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (currentActiveTrip == null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Active Trip Required")
                    .setMessage("You must post an active trip to Khulna before accepting deliveries.")
                    .setPositiveButton("Post Trip Now", (dialog, which) -> {
                        Intent intent = new Intent(requireContext(), PostTripActivity.class);
                        postTripLauncher.launch(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Accept Delivery Order")
                .setMessage(String.format("Do you want to accept delivery for %s?\nYou will earn ৳%.0f upon delivery.",
                        request.getProductName(), request.getDeliveryCharge()))
                .setPositiveButton("Yes, Accept", (dialog, which) -> {
                    AcceptRequestDto dto = new AcceptRequestDto(currentActiveTrip.getId(), request.getVersion());
                    ApiService api = ApiClient.getService(requireContext());
                    api.acceptRequest(request.getId(), dto).enqueue(new Callback<ProductRequest>() {
                        @Override
                        public void onResponse(Call<ProductRequest> call, Response<ProductRequest> response) {
                            if (!isAdded() || getContext() == null) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Order accepted! Pick it up during your trip.", Toast.LENGTH_LONG).show();
                                loadActiveTripAndRequests();
                            } else {
                                Toast.makeText(requireContext(), "Could not accept order (may have been claimed by another carrier)", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ProductRequest> call, Throwable t) {
                            if (!isAdded() || getContext() == null) return;
                            Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
