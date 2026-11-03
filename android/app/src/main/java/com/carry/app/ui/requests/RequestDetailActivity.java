package com.carry.app.ui.requests;

import android.os.Bundle;
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
import com.carry.app.models.AdditionalPaymentStatus;
import com.carry.app.models.Payment;
import com.carry.app.models.PaymentMethod;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.RequestStatus;
import com.carry.app.models.StatusUpdate;
import com.carry.app.models.SubmitAdditionalPaymentDto;
import com.carry.app.models.User;
import com.carry.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.List;

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
    private Button btnCancelOrder;

    private TimelineAdapter timelineAdapter;
    private Long requestId;
    private ProductRequest currentRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        requestId = getIntent().getLongExtra("request_id", -1L);
        currentRequest = (ProductRequest) getIntent().getSerializableExtra("request_data");

        initViews();
        setupToolbar();

        if (currentRequest != null) {
            bindRequestData(currentRequest);
        }

        if (requestId != -1L) {
            loadRequestDetails();
            loadTimeline();
        }
    }

    private void initViews() {
        detailToolbar = findViewById(R.id.detailToolbar);
        tvDetailOrderId = findViewById(R.id.tvDetailOrderId);
        tvDetailStatusPill = findViewById(R.id.tvDetailStatusPill);
        tvDetailItemName = findViewById(R.id.tvDetailItemName);
        tvDetailPickupAndShop = findViewById(R.id.tvDetailPickupAndShop);
        tvDetailInstructions = findViewById(R.id.tvDetailInstructions);

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
        btnCancelOrder = findViewById(R.id.btnCancelOrder);

        timelineAdapter = new TimelineAdapter(this);
        rvDetailTimeline.setLayoutManager(new LinearLayoutManager(this));
        rvDetailTimeline.setAdapter(timelineAdapter);

        btnPayAdditional.setOnClickListener(v -> showAdditionalPaymentDialog());
        btnCancelOrder.setOnClickListener(v -> cancelCurrentOrder());
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
        tvDetailOrderId.setText(String.format("#REQ-%d", req.getId()));

        RequestStatus status = req.getStatus() != null ? req.getStatus() : RequestStatus.REQUESTED;
        tvDetailStatusPill.setText(status.getLabel());

        int qty = req.getQuantity() != null ? req.getQuantity() : 1;
        tvDetailItemName.setText(String.format("%s (Quantity: %d)", req.getProductName(), qty));

        String area = req.getPickupArea() != null ? req.getPickupArea().getDisplayName() : "Khulna City";
        String shop = req.getPreferredShop() != null && !req.getPreferredShop().isEmpty() ? " • Shop: " + req.getPreferredShop() : "";
        tvDetailPickupAndShop.setText(String.format("Pickup: %s%s", area, shop));

        tvDetailInstructions.setText(req.getInstructions() != null && !req.getInstructions().isEmpty() ?
                "Instructions: " + req.getInstructions() : "No special instructions");

        // Pricing breakdown
        Payment payment = req.getPayment();
        if (payment != null) {
            tvDetailProductCost.setText(String.format("৳%.2f", payment.getProductCost()));
            tvDetailDeliveryFee.setText(String.format("৳%.2f", payment.getDeliveryFee()));
            tvDetailMfsFee.setText(String.format("৳%.2f", payment.getGatewayFee()));
            tvDetailTotalPaid.setText(String.format("৳%.2f", payment.getTotal()));

            String method = payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDisplayName() : "MFS";
            String trx = payment.getTrxId() != null ? payment.getTrxId() : "N/A";
            tvDetailTrxInfo.setText(String.format("Paid via %s • TrxID: %s (%s)", method, trx, payment.getStatus().getLabel()));

            // Additional payment requested by carrier
            if (payment.getAdditionalPaymentStatus() == AdditionalPaymentStatus.REQUESTED) {
                layoutAdditionalPaymentCard.setVisibility(View.VISIBLE);
                tvAdditionalAmountNotice.setText(String.format("⚠️ Additional Payment Requested: ৳%.2f", payment.getAdditionalAmount()));
                tvAdditionalReason.setText(payment.getNeedMoreReason() != null ? "Reason: " + payment.getNeedMoreReason() : "Actual purchase price is higher");
            } else if (payment.getAdditionalPaymentStatus() == AdditionalPaymentStatus.SUBMITTED) {
                layoutAdditionalPaymentCard.setVisibility(View.VISIBLE);
                tvAdditionalAmountNotice.setText(String.format("✓ Additional ৳%.2f Submitted", payment.getAdditionalAmount()));
                tvAdditionalReason.setText("Verification in progress by carrier/admin");
                btnPayAdditional.setVisibility(View.GONE);
            } else {
                layoutAdditionalPaymentCard.setVisibility(View.GONE);
            }
        } else {
            BigDecimal budget = req.getBudget() != null ? req.getBudget() : BigDecimal.ZERO;
            BigDecimal delivery = req.getDeliveryCharge() != null ? req.getDeliveryCharge() : BigDecimal.ZERO;
            tvDetailProductCost.setText(String.format("৳%.2f", budget));
            tvDetailDeliveryFee.setText(String.format("৳%.2f", delivery));
            tvDetailMfsFee.setText("৳0.00");
            tvDetailTotalPaid.setText(String.format("৳%.2f", budget.add(delivery)));
            tvDetailTrxInfo.setText("Deposit: Pending");
            layoutAdditionalPaymentCard.setVisibility(View.GONE);
        }

        // Cancel button visible only when still REQUESTED
        btnCancelOrder.setVisibility(status == RequestStatus.REQUESTED ? View.VISIBLE : View.GONE);
    }

    private void loadRequestDetails() {
        ApiService api = ApiClient.getService(this);
        api.getRequestById(requestId).enqueue(new Callback<ProductRequest>() {
            @Override
            public void onResponse(Call<ProductRequest> call, Response<ProductRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindRequestData(response.body());
                }
            }

            @Override
            public void onFailure(Call<ProductRequest> call, Throwable t) {
                // Ignore silent failure if cached request is already visible
            }
        });
    }

    private void loadTimeline() {
        ApiService api = ApiClient.getService(this);
        api.getRequestTimeline(requestId).enqueue(new Callback<List<StatusUpdate>>() {
            @Override
            public void onResponse(Call<List<StatusUpdate>> call, Response<List<StatusUpdate>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    timelineAdapter.setUpdates(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<StatusUpdate>> call, Throwable t) {
                // Ignore silent failure
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
                        Toast.makeText(RequestDetailActivity.this, "Additional payment submitted!", Toast.LENGTH_SHORT).show();
                        loadRequestDetails();
                        loadTimeline();
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
                                Toast.makeText(RequestDetailActivity.this, "Order cancelled", Toast.LENGTH_SHORT).show();
                                loadRequestDetails();
                                loadTimeline();
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
