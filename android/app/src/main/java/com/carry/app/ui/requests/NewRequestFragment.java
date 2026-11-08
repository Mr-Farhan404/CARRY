package com.carry.app.ui.requests;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.carry.app.MainActivity;
import com.carry.app.R;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.CreateProductRequest;
import com.carry.app.models.LocationArea;
import com.carry.app.models.OrderType;
import com.carry.app.models.PaymentMethod;
import com.carry.app.models.ProductCategory;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.User;
import com.carry.app.utils.DeliveryChargeCalculator;
import com.carry.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewRequestFragment extends Fragment {

    private TextInputEditText etReqProductName;
    private Spinner spinnerReqCategory;
    private TextInputEditText etReqQuantity;
    private TextInputEditText etReqBudget;
    private Spinner spinnerReqPickupZone;
    private TextInputEditText etReqShop;
    private TextInputEditText etReqInstructions;

    private TextView tvManualBudget;
    private TextView tvManualDeliveryCharge;
    private TextView tvManualMfsFee;
    private TextView tvManualTotal;

    private RadioGroup rgManualMethod;
    private RadioButton rbManualBkash;
    private RadioButton rbManualNagad;
    private View layoutMfsBanner;
    private TextView tvMfsInstructions;

    private TextInputEditText etManualSenderPhone;
    private TextInputEditText etManualTrxId;
    private MaterialButton btnSubmitManualRequest;
    private ProgressBar progressBarManual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_request, container, false);

        etReqProductName = view.findViewById(R.id.etReqProductName);
        spinnerReqCategory = view.findViewById(R.id.spinnerReqCategory);
        etReqQuantity = view.findViewById(R.id.etReqQuantity);
        etReqBudget = view.findViewById(R.id.etReqBudget);
        spinnerReqPickupZone = view.findViewById(R.id.spinnerReqPickupZone);
        etReqShop = view.findViewById(R.id.etReqShop);
        etReqInstructions = view.findViewById(R.id.etReqInstructions);

        tvManualBudget = view.findViewById(R.id.tvManualBudget);
        tvManualDeliveryCharge = view.findViewById(R.id.tvManualDeliveryCharge);
        tvManualMfsFee = view.findViewById(R.id.tvManualMfsFee);
        tvManualTotal = view.findViewById(R.id.tvManualTotal);

        rgManualMethod = view.findViewById(R.id.rgManualMethod);
        rbManualBkash = view.findViewById(R.id.rbManualBkash);
        rbManualNagad = view.findViewById(R.id.rbManualNagad);
        layoutMfsBanner = view.findViewById(R.id.layoutMfsBanner);
        tvMfsInstructions = view.findViewById(R.id.tvMfsInstructions);

        etManualSenderPhone = view.findViewById(R.id.etManualSenderPhone);
        etManualTrxId = view.findViewById(R.id.etManualTrxId);
        btnSubmitManualRequest = view.findViewById(R.id.btnSubmitManualRequest);
        progressBarManual = view.findViewById(R.id.progressBarManual);

        setupSpinners();
        setupCalculations();
        setupPaymentInfo();

        btnSubmitManualRequest.setOnClickListener(v -> submitManualRequest());

        return view;
    }

    private void setupSpinners() {
        // Category spinner
        ProductCategory[] categories = ProductCategory.values();
        String[] catNames = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            catNames[i] = categories[i].getDisplayName();
        }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, catNames);
        spinnerReqCategory.setAdapter(catAdapter);

        // Location pickup zone spinner
        LocationArea[] areas = LocationArea.values();
        String[] areaNames = new String[areas.length];
        for (int i = 0; i < areas.length; i++) {
            areaNames[i] = areas[i].getDisplayName();
        }
        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, areaNames);
        spinnerReqPickupZone.setAdapter(areaAdapter);
    }

    private void setupCalculations() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculate();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etReqQuantity.addTextChangedListener(watcher);
        etReqBudget.addTextChangedListener(watcher);
        recalculate();
    }

    private void recalculate() {
        String budgetStr = etReqBudget.getText() != null ? etReqBudget.getText().toString().trim() : "0";
        String qtyStr = etReqQuantity.getText() != null ? etReqQuantity.getText().toString().trim() : "1";

        BigDecimal budget;
        try {
            budget = new BigDecimal(budgetStr);
        } catch (Exception e) {
            budget = BigDecimal.ZERO;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
        } catch (Exception e) {
            qty = 1;
        }

        BigDecimal deliveryCharge = DeliveryChargeCalculator.calculateDeliveryCharge(null, null, false, qty);
        BigDecimal subtotal = budget.add(deliveryCharge);
        BigDecimal mfsFee = DeliveryChargeCalculator.calculateGatewayFee(subtotal);
        BigDecimal total = subtotal.add(mfsFee);

        tvManualBudget.setText(String.format("৳%.2f", budget));
        tvManualDeliveryCharge.setText(String.format("৳%.2f", deliveryCharge));
        tvManualMfsFee.setText(String.format("৳%.2f", mfsFee));
        tvManualTotal.setText(String.format("৳%.2f", total));
        btnSubmitManualRequest.setText(String.format("Submit Request & Pay ৳%.0f", total));
    }

    private void setupPaymentInfo() {
        SessionManager session = SessionManager.getInstance(requireContext());
        User user = session.getUser();
        if (user != null && user.getPhone() != null) {
            etManualSenderPhone.setText(user.getPhone());
        }

        rgManualMethod.setOnCheckedChangeListener((group, checkedId) -> {
            String method = checkedId == R.id.rbManualNagad ? "Nagad" : "bKash";
            if (tvMfsInstructions != null) {
                tvMfsInstructions.setText(String.format("Send Money to Carry Campus %s Account:\n01700000000 (Tap to copy)", method));
            }
        });

        if (layoutMfsBanner != null) {
            layoutMfsBanner.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Campus Account", "01700000000"));
                    Toast.makeText(requireContext(), "Copied 01700000000 to clipboard", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void submitManualRequest() {
        String name = etReqProductName.getText() != null ? etReqProductName.getText().toString().trim() : "";
        String budgetStr = etReqBudget.getText() != null ? etReqBudget.getText().toString().trim() : "";
        String qtyStr = etReqQuantity.getText() != null ? etReqQuantity.getText().toString().trim() : "1";
        String shop = etReqShop.getText() != null ? etReqShop.getText().toString().trim() : "";
        String instructions = etReqInstructions.getText() != null ? etReqInstructions.getText().toString().trim() : "";
        String senderPhone = etManualSenderPhone.getText() != null ? etManualSenderPhone.getText().toString().trim() : "";
        String trxId = etManualTrxId.getText() != null ? etManualTrxId.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etReqProductName.setError("Item name is required");
            etReqProductName.requestFocus();
            return;
        }

        BigDecimal budget;
        try {
            budget = new BigDecimal(budgetStr);
            if (budget.compareTo(BigDecimal.ZERO) <= 0) {
                etReqBudget.setError("Budget must be greater than 0");
                etReqBudget.requestFocus();
                return;
            }
        } catch (Exception e) {
            etReqBudget.setError("Valid budget required");
            etReqBudget.requestFocus();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) quantity = 1;
        } catch (Exception e) {
            quantity = 1;
        }

        if (senderPhone.isEmpty()) {
            etManualSenderPhone.setError("Sender phone is required");
            etManualSenderPhone.requestFocus();
            return;
        }

        if (trxId.isEmpty()) {
            etManualTrxId.setError("Transaction ID is required");
            etManualTrxId.requestFocus();
            return;
        }

        btnSubmitManualRequest.setEnabled(false);
        progressBarManual.setVisibility(View.VISIBLE);

        ProductCategory category = ProductCategory.values()[spinnerReqCategory.getSelectedItemPosition()];
        LocationArea area = LocationArea.values()[spinnerReqPickupZone.getSelectedItemPosition()];
        PaymentMethod method = rbManualNagad.isChecked() ? PaymentMethod.NAGAD : PaymentMethod.BKASH;

        CreateProductRequest request = new CreateProductRequest();
        request.setOrderType(OrderType.MANUAL);
        request.setProductName(name);
        request.setCategory(category.name());
        request.setQuantity(quantity);
        request.setBudget(budget);
        request.setPickupArea(area);
        request.setPreferredShop(shop);
        request.setInstructions(instructions);
        request.setPaymentMethod(method);
        request.setSenderPhone(senderPhone);
        request.setTrxId(trxId);

        ApiService api = ApiClient.getService(requireContext());
        api.createRequest(request).enqueue(new Callback<ProductRequest>() {
            @Override
            public void onResponse(Call<ProductRequest> call, Response<ProductRequest> response) {
                btnSubmitManualRequest.setEnabled(true);
                progressBarManual.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    ProductRequest created = response.body();
                    if (created != null && created.getId() != null) {
                        String firebaseUid = SessionManager.getInstance(requireContext()).getFirebaseUid();
                        new com.carry.app.data.firebase.LiveStatusStoreImpl().emitOrderEvent(created.getId(), "REQUESTED", firebaseUid, null);
                    }
                    Toast.makeText(requireContext(), "Custom request posted successfully!", Toast.LENGTH_LONG).show();

                    // Clear fields
                    etReqProductName.setText("");
                    etReqShop.setText("");
                    etReqInstructions.setText("");
                    etManualTrxId.setText("");

                    // Navigate to My Requests tab in MainActivity
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToTab(R.id.nav_my_requests);
                    }
                } else {
                    String err = parseRequestError(response);
                    Toast.makeText(requireContext(), "Failed to post request: " + err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ProductRequest> call, Throwable t) {
                btnSubmitManualRequest.setEnabled(true);
                progressBarManual.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String parseRequestError(Response<?> response) {
        if (response.code() == 401 || response.code() == 403) {
            return "Session expired. Please log out and log in again.";
        }
        if (response.errorBody() != null) {
            try {
                String body = response.errorBody().string();
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                if (obj.has("message")) {
                    return obj.get("message").getAsString();
                }
            } catch (Exception ignored) {}
        }
        return response.message() != null && !response.message().isEmpty() ? response.message() : "Error " + response.code();
    }
}
