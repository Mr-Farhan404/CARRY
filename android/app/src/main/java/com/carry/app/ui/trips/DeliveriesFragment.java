package com.carry.app.ui.trips;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.carry.app.R;
import com.carry.app.adapters.AvailableRequestAdapter;
import com.carry.app.adapters.RequestAdapter;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.data.firebase.LiveStatusStore;
import com.carry.app.data.firebase.LiveStatusStoreImpl;
import com.carry.app.models.AcceptRequestDto;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.Trip;
import com.carry.app.models.TripStatus;
import com.carry.app.models.UpdateTripStatusRequest;
import com.carry.app.models.User;
import com.carry.app.models.UserRole;
import com.carry.app.ui.requests.RequestDetailActivity;
import com.carry.app.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveriesFragment extends Fragment implements AvailableRequestAdapter.OnAcceptDeliveryListener, RequestAdapter.OnRequestClickListener {

    private SwipeRefreshLayout swipeRefreshDeliveries;
    private LinearLayout layoutActiveTrip;
    private TextView tvTripStatusBadge;
    private TextView tvTripDestination;
    private TextView tvTripTimes;
    private Button btnTripInCity;
    private Button btnTripReturning;
    private Button btnTripCompleted;

    // Location sharing views
    private Button btnShareLocationNow;
    private Button btnStopSharingLocation;
    private Space spaceStopSharing;
    private TextView tvLocationShareStatus;

    private LinearLayout layoutNoActiveTrip;
    private Button btnPostTrip;

    // Assigned requests
    private LinearLayout layoutAssignedSection;
    private RecyclerView rvAssignedRequests;
    private RequestAdapter assignedAdapter;

    // Available requests
    private RecyclerView rvAvailableRequests;
    private View layoutAvailableEmpty;
    private AvailableRequestAdapter adapter;

    private Trip currentActiveTrip;
    private ActivityResultLauncher<Intent> postTripLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    private final LiveStatusStore liveStatusStore = new LiveStatusStoreImpl();

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

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchAndShareLocation();
                    } else {
                        Toast.makeText(requireContext(), "Location permission is required to share your live location.", Toast.LENGTH_SHORT).show();
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

        btnShareLocationNow = view.findViewById(R.id.btnShareLocationNow);
        btnStopSharingLocation = view.findViewById(R.id.btnStopSharingLocation);
        spaceStopSharing = view.findViewById(R.id.spaceStopSharing);
        tvLocationShareStatus = view.findViewById(R.id.tvLocationShareStatus);

        layoutNoActiveTrip = view.findViewById(R.id.layoutNoActiveTrip);
        btnPostTrip = view.findViewById(R.id.btnPostTrip);

        layoutAssignedSection = view.findViewById(R.id.layoutAssignedSection);
        rvAssignedRequests = view.findViewById(R.id.rvAssignedRequests);

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

        assignedAdapter = new RequestAdapter(requireContext(), this);
        rvAssignedRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAssignedRequests.setAdapter(assignedAdapter);
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

        btnShareLocationNow.setOnClickListener(v -> handleShareLocationClick());
        btnStopSharingLocation.setOnClickListener(v -> handleStopSharingClick());
    }

    private void handleShareLocationClick() {
        if (currentActiveTrip == null || currentActiveTrip.getId() == null) {
            Toast.makeText(requireContext(), "You need an active trip to share location.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchAndShareLocation();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Location Permission Needed")
                    .setMessage("CARRY uses your location to share your live delivery position with customers waiting for their orders.")
                    .setPositiveButton("Allow", (d, w) -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                    .setNegativeButton("Not Now", null)
                    .show();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchAndShareLocation() {
        if (!isAdded() || getContext() == null) return;
        if (currentActiveTrip == null || currentActiveTrip.getId() == null) return;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        btnShareLocationNow.setEnabled(false);
        tvLocationShareStatus.setText("Status: Reading GPS fix...");

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(requireActivity());
        CancellationTokenSource cts = new CancellationTokenSource();
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    btnShareLocationNow.setEnabled(true);
                    if (location != null) {
                        doShareLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        client.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                doShareLocation(lastLoc.getLatitude(), lastLoc.getLongitude());
                            } else {
                                tvLocationShareStatus.setText("Status: Unable to acquire GPS signal");
                                Toast.makeText(requireContext(), "Could not retrieve GPS location. Please check device location settings.", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> {
                            tvLocationShareStatus.setText("Status: Location unavailable");
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    btnShareLocationNow.setEnabled(true);
                    tvLocationShareStatus.setText("Status: Location error");
                });
    }

    private void doShareLocation(double lat, double lng) {
        if (!isAdded() || currentActiveTrip == null || currentActiveTrip.getId() == null) return;

        String uid = SessionManager.getInstance(requireContext()).getFirebaseUid();
        liveStatusStore.shareTripLocation(currentActiveTrip.getId(), lat, lng, uid, new LiveStatusStore.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                tvLocationShareStatus.setText(String.format(Locale.US, "Status: Shared at %.4f, %.4f", lat, lng));
                btnStopSharingLocation.setVisibility(View.VISIBLE);
                spaceStopSharing.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Live location shared with customers!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                tvLocationShareStatus.setText("Status: Failed to share location");
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleStopSharingClick() {
        if (currentActiveTrip == null || currentActiveTrip.getId() == null) return;

        liveStatusStore.stopSharingTripLocation(currentActiveTrip.getId(), new LiveStatusStore.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                btnStopSharingLocation.setVisibility(View.GONE);
                spaceStopSharing.setVisibility(View.GONE);
                tvLocationShareStatus.setText("Status: Location sharing stopped");
                Toast.makeText(requireContext(), "Location sharing stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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
                loadAssignedDeliveries();
                loadAvailableRequests();
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                updateActiveTripUI(null);
                loadAssignedDeliveries();
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

    private void loadAssignedDeliveries() {
        if (!isAdded()) return;

        ApiService api = ApiClient.getService(requireContext());
        api.getPartnerDeliveries().enqueue(new Callback<List<ProductRequest>>() {
            @Override
            public void onResponse(Call<List<ProductRequest>> call, Response<List<ProductRequest>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    layoutAssignedSection.setVisibility(View.VISIBLE);
                    assignedAdapter.setRequests(response.body());
                } else {
                    layoutAssignedSection.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<ProductRequest>> call, Throwable t) {
                if (!isAdded()) return;
                layoutAssignedSection.setVisibility(View.GONE);
            }
        });
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

        Long tripId = currentActiveTrip.getId();
        ApiService api = ApiClient.getService(requireContext());
        api.updateTripStatus(tripId, new UpdateTripStatusRequest(newStatus)).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(requireContext(), "Trip status updated to " + newStatus.getLabel(), Toast.LENGTH_SHORT).show();
                    if (newStatus == TripStatus.COMPLETED || newStatus == TripStatus.CANCELLED) {
                        liveStatusStore.stopSharingTripLocation(tripId, null);
                        btnStopSharingLocation.setVisibility(View.GONE);
                        spaceStopSharing.setVisibility(View.GONE);
                        tvLocationShareStatus.setText("Status: Not shared");
                    }
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
                                String firebaseUid = SessionManager.getInstance(requireContext()).getFirebaseUid();
                                liveStatusStore.emitOrderEvent(request.getId(), "ACCEPTED", firebaseUid, null);

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

    @Override
    public void onRequestClick(ProductRequest request) {
        Intent intent = new Intent(requireContext(), RequestDetailActivity.class);
        intent.putExtra("request_id", request.getId());
        intent.putExtra("request_data", request);
        startActivity(intent);
    }
}
