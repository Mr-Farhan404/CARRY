package com.carry.app.ui.requests;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carry.app.R;
import com.carry.app.adapters.TimelineAdapter;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.data.firebase.LiveStatusStore;
import com.carry.app.data.firebase.LiveStatusStoreImpl;
import com.carry.app.models.AdditionalPaymentStatus;
import com.carry.app.models.LiveLocation;
import com.carry.app.models.Payment;
import com.carry.app.models.PaymentMethod;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.RequestStatus;
import com.carry.app.models.StatusUpdate;
import com.carry.app.ui.chat.ChatActivity;
import com.carry.app.models.SubmitAdditionalPaymentDto;
import com.carry.app.models.UpdateRequestStatusDto;
import com.carry.app.models.User;
import com.carry.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestDetailActivity extends AppCompatActivity {

    private Toolbar detailToolbar;
    private TextView tvDetailOrderId;
    private TextView tvDetailStatusPill;
    private TextView tvDetailItemName;
    private TextView tvDetailPickupAndShop;
    private TextView tvDetailInstructions;

    // Live location banner
    private LinearLayout layoutPartnerLocationBanner;
    private TextView tvPartnerLocationStatus;
    private Button btnOpenInMaps;

    private TextView tvDetailProductCost;
    private TextView tvDetailDeliveryFee;
    private TextView tvDetailMfsFee;
    private TextView tvDetailTotalPaid;
    private TextView tvDetailTrxInfo;

    private LinearLayout layoutAdditionalPaymentCard;
    private TextView tvAdditionalAmountNotice;
    private TextView tvAdditionalReason;
    private Button btnPayAdditional;

    private RecyclerView rvDetailTimeline;

    // Action buttons
    private Button btnOpenChat;
    private Button btnUpdateDeliveryStatus;
    private Button btnCancelOrder;

    private TimelineAdapter timelineAdapter;
    private Long requestId;
    private ProductRequest currentRequest;

    private RequestDetailViewModel viewModel;
    private final LiveStatusStore liveStatusStore = new LiveStatusStoreImpl();

    private final Handler safetyNetHandler = new Handler(Looper.getMainLooper());
    private final Handler fallbackPollingHandler = new Handler(Looper.getMainLooper());
    private static final long SAFETY_NET_INTERVAL_MS = 60000; // 60 seconds slow safety net
    private static final long FALLBACK_POLL_INTERVAL_MS = 15000; // 15 seconds if RTDB fails

    private final Runnable safetyNetRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null) {
                viewModel.refreshFromBackend();
            }
            safetyNetHandler.postDelayed(this, SAFETY_NET_INTERVAL_MS);
        }
    };

    private final Runnable fallbackPollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null && viewModel.getCurrentState().isFallbackPollingActive) {
                viewModel.refreshFromBackend();
                fallbackPollingHandler.postDelayed(this, FALLBACK_POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        requestId = getIntent().getLongExtra("request_id", -1L);
        currentRequest = (ProductRequest) getIntent().getSerializableExtra("request_data");

        initViews();
        setupToolbar();
        setupViewModel();

        if (currentRequest != null) {
            bindRequestData(currentRequest);
            viewModel.setInitialRequest(currentRequest);
        }
    }

    private void setupViewModel() {
        RequestDetailViewModel.BackendDataSource dataSource = new RequestDetailViewModel.BackendDataSource() {
            @Override
            public void loadRequest(Long reqId, RequestDetailViewModel.BackendCallback<ProductRequest> callback) {
                ApiService api = ApiClient.getService(RequestDetailActivity.this);
                api.getRequestById(reqId).enqueue(new Callback<ProductRequest>() {
                    @Override
                    public void onResponse(Call<ProductRequest> call, Response<ProductRequest> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to fetch order details.");
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductRequest> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
            }

            @Override
            public void loadTimeline(Long reqId, RequestDetailViewModel.BackendCallback<List<StatusUpdate>> callback) {
                ApiService api = ApiClient.getService(RequestDetailActivity.this);
                api.getRequestTimeline(reqId).enqueue(new Callback<List<StatusUpdate>>() {
                    @Override
                    public void onResponse(Call<List<StatusUpdate>> call, Response<List<StatusUpdate>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to fetch timeline.");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<StatusUpdate>> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
            }
        };

        viewModel = new RequestDetailViewModel(liveStatusStore, dataSource);
        viewModel.setStateListener(state -> runOnUiThread(() -> handleViewState(state)));
    }

    private void handleViewState(RequestDetailViewModel.ViewState state) {
        if (state.request != null) {
            bindRequestData(state.request);
        }

        if (state.timeline != null) {
            timelineAdapter.setUpdates(state.timeline);
        }

        // Live Location Banner
        if (state.liveLocation != null && state.liveLocation.getLat() != null && state.liveLocation.getLng() != null) {
            layoutPartnerLocationBanner.setVisibility(View.VISIBLE);
            String timeAgo = state.locationTimeAgo != null ? state.locationTimeAgo : "just now";
            tvPartnerLocationStatus.setText(String.format("📍 Partner location updated %s", timeAgo));

            LiveLocation loc = state.liveLocation;
            btnOpenInMaps.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(Delivery Partner)",
                        loc.getLat(), loc.getLng(), loc.getLat(), loc.getLng()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Toast.makeText(RequestDetailActivity.this, "No map application found.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            layoutPartnerLocationBanner.setVisibility(View.GONE);
        }

        // Fallback polling management
        if (state.isFallbackPollingActive) {
            fallbackPollingHandler.removeCallbacks(fallbackPollingRunnable);
            fallbackPollingHandler.postDelayed(fallbackPollingRunnable, FALLBACK_POLL_INTERVAL_MS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (requestId != -1L) {
            Long tripId = currentRequest != null ? currentRequest.getMatchedTripId() : null;
            viewModel.start(requestId, tripId);

            // Start 60s slow safety net
            safetyNetHandler.removeCallbacks(safetyNetRunnable);
            safetyNetHandler.postDelayed(safetyNetRunnable, SAFETY_NET_INTERVAL_MS);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (viewModel != null) {
            viewModel.stop();
        }
        safetyNetHandler.removeCallbacks(safetyNetRunnable);
        fallbackPollingHandler.removeCallbacks(fallbackPollingRunnable);
    }

    private void initViews() {
        detailToolbar = findViewById(R.id.detailToolbar);
        tvDetailOrderId = findViewById(R.id.tvDetailOrderId);
        tvDetailStatusPill = findViewById(R.id.tvDetailStatusPill);
        tvDetailItemName = findViewById(R.id.tvDetailItemName);
        tvDetailPickupAndShop = findViewById(R.id.tvDetailPickupAndShop);
        tvDetailInstructions = findViewById(R.id.tvDetailInstructions);

        layoutPartnerLocationBanner = findViewById(R.id.layoutPartnerLocationBanner);
        tvPartnerLocationStatus = findViewById(R.id.tvPartnerLocationStatus);
        btnOpenInMaps = findViewById(R.id.btnOpenInMaps);

        tvDetailProductCost = findViewById(R.id.tvDetailProductCost);
        tvDetailDeliveryFee = findViewById(R.id.tvDetailDeliveryFee);
        tvDetailMfsFee = findViewById(R.id.tvDetailMfsFee);
        tvDetailTotalPaid = findViewById(R.id.tvDetailTotalPaid);
        tvDetailTrxInfo = findViewById(R.id.tvDetailTrxInfo);

        layoutAdditionalPaymentCard = findViewById(R.id.layoutAdditionalPaymentCard);
        tvAdditionalAmountNotice = findViewById(R.id.tvAdditionalAmountNotice);
        tvAdditionalReason = findViewById(R.id.tvAdditionalReason);
        btnPayAdditional = findViewById(R.id.btnPayAdditional);

        rvDetailTimeline = findViewById(R.id.rvDetailTimeline);

        btnOpenChat = findViewById(R.id.btnOpenChat);
        btnUpdateDeliveryStatus = findViewById(R.id.btnUpdateDeliveryStatus);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);

        timelineAdapter = new TimelineAdapter(this);
        rvDetailTimeline.setLayoutManager(new LinearLayoutManager(this));
        rvDetailTimeline.setAdapter(timelineAdapter);

        btnPayAdditional.setOnClickListener(v -> showAdditionalPaymentDialog());
        btnCancelOrder.setOnClickListener(v -> cancelCurrentOrder());

        btnOpenChat.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtra(ChatActivity.EXTRA_REQUEST_ID, String.valueOf(requestId));
            if (currentRequest != null) {
                String title = currentRequest.getProductName() != null ? currentRequest.getProductName() : "Order #" + requestId;
                chatIntent.putExtra(ChatActivity.EXTRA_ORDER_NAME, title);

                User currentUser = SessionManager.getInstance(this).getUser();
                boolean isCustomer = currentUser != null && currentRequest.getCustomerId() != null
                        && currentRequest.getCustomerId().equals(currentUser.getId());
                if (isCustomer) {
                    chatIntent.putExtra(ChatActivity.EXTRA_OTHER_PARTY_NAME, "Delivery Partner");
                } else {
                    String customerName = currentRequest.getCustomerName() != null ? currentRequest.getCustomerName() : "Customer";
                    chatIntent.putExtra(ChatActivity.EXTRA_OTHER_PARTY_NAME, customerName);
                    if (currentRequest.getCustomerId() != null) {
                        chatIntent.putExtra(ChatActivity.EXTRA_OTHER_PARTY_BACKEND_ID, currentRequest.getCustomerId());
                    }
                }
            } else {
                chatIntent.putExtra(ChatActivity.EXTRA_ORDER_NAME, "Order #" + requestId);
            }
            startActivity(chatIntent);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(detailToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        detailToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindRequestData(ProductRequest req) {
        currentRequest = req;
        tvDetailOrderId.setText(String.format(Locale.US, "#REQ-%d", req.getId()));

        RequestStatus status = req.getStatus() != null ? req.getStatus() : RequestStatus.REQUESTED;
        tvDetailStatusPill.setText(status.getLabel());

        int qty = req.getQuantity() != null ? req.getQuantity() : 1;
        tvDetailItemName.setText(String.format(Locale.US, "%s (Quantity: %d)", req.getProductName(), qty));

        String area = req.getPickupArea() != null ? req.getPickupArea().getDisplayName() : "Khulna City";
        String shop = req.getPreferredShop() != null && !req.getPreferredShop().isEmpty() ? " • Shop: " + req.getPreferredShop() : "";
        tvDetailPickupAndShop.setText(String.format("Pickup: %s%s", area, shop));

        tvDetailInstructions.setText(req.getInstructions() != null && !req.getInstructions().isEmpty() ?
                "Instructions: " + req.getInstructions() : "No special instructions");

        // Pricing breakdown
        Payment payment = req.getPayment();
        if (payment != null) {
            tvDetailProductCost.setText(String.format(Locale.US, "৳%.2f", payment.getProductCost()));
            tvDetailDeliveryFee.setText(String.format(Locale.US, "৳%.2f", payment.getDeliveryFee()));
            tvDetailMfsFee.setText(String.format(Locale.US, "৳%.2f", payment.getGatewayFee()));
            tvDetailTotalPaid.setText(String.format(Locale.US, "৳%.2f", payment.getTotal()));

            String method = payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDisplayName() : "MFS";
            String trx = payment.getTrxId() != null ? payment.getTrxId() : "N/A";
            tvDetailTrxInfo.setText(String.format("Paid via %s • TrxID: %s (%s)", method, trx, payment.getStatus().getLabel()));

            // Additional payment requested by carrier
            if (payment.getAdditionalPaymentStatus() == AdditionalPaymentStatus.REQUESTED) {
                layoutAdditionalPaymentCard.setVisibility(View.VISIBLE);
                tvAdditionalAmountNotice.setText(String.format(Locale.US, "⚠️ Additional Payment Requested: ৳%.2f", payment.getAdditionalAmount()));
                tvAdditionalReason.setText(payment.getNeedMoreReason() != null ? "Reason: " + payment.getNeedMoreReason() : "Actual purchase price is higher");
            } else if (payment.getAdditionalPaymentStatus() == AdditionalPaymentStatus.SUBMITTED) {
                layoutAdditionalPaymentCard.setVisibility(View.VISIBLE);
                tvAdditionalAmountNotice.setText(String.format(Locale.US, "✓ Additional ৳%.2f Submitted", payment.getAdditionalAmount()));
                tvAdditionalReason.setText("Verification in progress by carrier/admin");
                btnPayAdditional.setVisibility(View.GONE);
            } else {
                layoutAdditionalPaymentCard.setVisibility(View.GONE);
            }
        } else {
            BigDecimal budget = req.getBudget() != null ? req.getBudget() : BigDecimal.ZERO;
            BigDecimal delivery = req.getDeliveryCharge() != null ? req.getDeliveryCharge() : BigDecimal.ZERO;
            tvDetailProductCost.setText(String.format(Locale.US, "৳%.2f", budget));
            tvDetailDeliveryFee.setText(String.format(Locale.US, "৳%.2f", delivery));
            tvDetailMfsFee.setText("৳0.00");
            tvDetailTotalPaid.setText(String.format(Locale.US, "৳%.2f", budget.add(delivery)));
            tvDetailTrxInfo.setText("Deposit: Pending");
            layoutAdditionalPaymentCard.setVisibility(View.GONE);
        }

        // Role & Action Buttons
        User currentUser = SessionManager.getInstance(this).getUser();
        boolean isCustomer = currentUser != null && req.getCustomerId() != null && req.getCustomerId().equals(currentUser.getId());

        // Cancel button: only customer while status is REQUESTED
        btnCancelOrder.setVisibility(isCustomer && status == RequestStatus.REQUESTED ? View.VISIBLE : View.GONE);

        // Chat button: visible once order is ACCEPTED or active
        btnOpenChat.setVisibility(status != RequestStatus.REQUESTED && status != RequestStatus.CANCELLED ? View.VISIBLE : View.GONE);

        // Carrier Progression button
        setupCarrierProgressionButton(isCustomer, status);
    }

    private void setupCarrierProgressionButton(boolean isCustomer, RequestStatus currentStatus) {
        if (isCustomer) {
            btnUpdateDeliveryStatus.setVisibility(View.GONE);
            return;
        }

        RequestStatus nextStatus = null;
        String buttonText = null;

        if (currentStatus == RequestStatus.ACCEPTED) {
            nextStatus = RequestStatus.COLLECTED;
            buttonText = "Mark Items Collected (In City)";
        } else if (currentStatus == RequestStatus.COLLECTED) {
            nextStatus = RequestStatus.RETURNING;
            buttonText = "Mark Returning to Campus";
        } else if (currentStatus == RequestStatus.RETURNING) {
            nextStatus = RequestStatus.READY_FOR_DELIVERY;
            buttonText = "Mark Ready for Pickup";
        } else if (currentStatus == RequestStatus.READY_FOR_DELIVERY) {
            nextStatus = RequestStatus.DELIVERED;
            buttonText = "Mark Order Delivered";
        }

        if (nextStatus != null && buttonText != null) {
            btnUpdateDeliveryStatus.setVisibility(View.VISIBLE);
            btnUpdateDeliveryStatus.setText(buttonText);

            final RequestStatus targetStatus = nextStatus;
            btnUpdateDeliveryStatus.setOnClickListener(v -> advanceDeliveryStatus(targetStatus));
        } else {
            btnUpdateDeliveryStatus.setVisibility(View.GONE);
        }
    }

    private void advanceDeliveryStatus(RequestStatus nextStatus) {
        btnUpdateDeliveryStatus.setEnabled(false);

        ApiService api = ApiClient.getService(this);
        api.updateRequestStatus(requestId, new UpdateRequestStatusDto(nextStatus, "Status updated by delivery partner"))
                .enqueue(new Callback<ProductRequest>() {
                    @Override
                    public void onResponse(Call<ProductRequest> call, Response<ProductRequest> response) {
                        btnUpdateDeliveryStatus.setEnabled(true);
                        if (response.isSuccessful()) {
                            // Emit RTDB nudge best-effort
                            String firebaseUid = SessionManager.getInstance(RequestDetailActivity.this).getFirebaseUid();
                            liveStatusStore.emitOrderEvent(requestId, nextStatus.name(), firebaseUid, null);

                            Toast.makeText(RequestDetailActivity.this, "Status updated to " + nextStatus.getLabel(), Toast.LENGTH_SHORT).show();
                            if (viewModel != null) {
                                viewModel.refreshFromBackend();
                            }
                        } else {
                            Toast.makeText(RequestDetailActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductRequest> call, Throwable t) {
                        btnUpdateDeliveryStatus.setEnabled(true);
                        Toast.makeText(RequestDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAdditionalPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_need_more, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RadioGroup rgMethod = dialogView.findViewById(R.id.rgNeedMoreMethod);
        RadioButton rbBkash = dialogView.findViewById(R.id.rbNeedMoreBkash);
        RadioButton rbNagad = dialogView.findViewById(R.id.rbNeedMoreNagad);
        TextInputEditText etSenderPhone = dialogView.findViewById(R.id.etNeedMoreSenderPhone);
        TextInputEditText etTrxId = dialogView.findViewById(R.id.etNeedMoreTrxId);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitAdditionalPayment);

        SessionManager session = SessionManager.getInstance(this);
        User user = session.getUser();
        if (user != null && user.getPhone() != null) {
            etSenderPhone.setText(user.getPhone());
        }

        btnSubmit.setOnClickListener(v -> {
            String phone = etSenderPhone.getText() != null ? etSenderPhone.getText().toString().trim() : "";
            String trxId = etTrxId.getText() != null ? etTrxId.getText().toString().trim() : "";

            if (phone.isEmpty()) {
                etSenderPhone.setError("Phone is required");
                return;
            }
            if (trxId.isEmpty()) {
                etTrxId.setError("TrxID is required");
                return;
            }

            btnSubmit.setEnabled(false);
            PaymentMethod method = rbNagad.isChecked() ? PaymentMethod.NAGAD : PaymentMethod.BKASH;
            SubmitAdditionalPaymentDto dto = new SubmitAdditionalPaymentDto(method, phone, trxId);

            ApiService api = ApiClient.getService(this);
            api.submitAdditionalPayment(requestId, dto).enqueue(new Callback<Payment>() {
                @Override
                public void onResponse(Call<Payment> call, Response<Payment> response) {
                    btnSubmit.setEnabled(true);
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        // Emit live status nudge
                        String firebaseUid = SessionManager.getInstance(RequestDetailActivity.this).getFirebaseUid();
                        liveStatusStore.emitOrderEvent(requestId, "PAYMENT_SUBMITTED", firebaseUid, null);

                        Toast.makeText(RequestDetailActivity.this, "Additional payment submitted!", Toast.LENGTH_SHORT).show();
                        if (viewModel != null) {
                            viewModel.refreshFromBackend();
                        }
                    } else {
                        Toast.makeText(RequestDetailActivity.this, "Failed to submit additional payment", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Payment> call, Throwable t) {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(RequestDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void cancelCurrentOrder() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Request")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes, Cancel", (d, which) -> {
                    ApiService api = ApiClient.getService(this);
                    api.cancelRequest(requestId).enqueue(new Callback<ProductRequest>() {
                        @Override
                        public void onResponse(Call<ProductRequest> call, Response<ProductRequest> response) {
                            if (response.isSuccessful()) {
                                // Emit RTDB nudge
                                String firebaseUid = SessionManager.getInstance(RequestDetailActivity.this).getFirebaseUid();
                                liveStatusStore.emitOrderEvent(requestId, "CANCELLED", firebaseUid, null);

                                Toast.makeText(RequestDetailActivity.this, "Order cancelled", Toast.LENGTH_SHORT).show();
                                if (viewModel != null) {
                                    viewModel.refreshFromBackend();
                                }
                            } else {
                                Toast.makeText(RequestDetailActivity.this, "Failed to cancel order", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ProductRequest> call, Throwable t) {
                            Toast.makeText(RequestDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }
}
