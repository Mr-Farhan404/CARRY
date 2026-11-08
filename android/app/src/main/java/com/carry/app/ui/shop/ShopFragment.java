package com.carry.app.ui.shop;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.carry.app.MainActivity;
import com.carry.app.R;
import com.carry.app.adapters.ProductAdapter;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.CheckoutLineDto;
import com.carry.app.models.LocationArea;
import com.carry.app.models.PaymentMethod;
import com.carry.app.models.Product;
import com.carry.app.models.ProductCategory;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.User;
import com.carry.app.utils.CartManager;
import com.carry.app.utils.DeliveryChargeCalculator;
import com.carry.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopFragment extends Fragment implements ProductAdapter.OnProductActionListener {

    private EditText etSearch;
    private TextView chipAll, chipFood, chipElectronics, chipOthers;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvProducts;
    private View layoutEmpty;

    private ProductAdapter adapter;
    private String selectedCategory = null;
    private String currentSearch = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        chipAll = view.findViewById(R.id.chipAll);
        chipFood = view.findViewById(R.id.chipFood);
        chipElectronics = view.findViewById(R.id.chipElectronics);
        chipOthers = view.findViewById(R.id.chipOthers);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rvProducts = view.findViewById(R.id.rvProducts);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        setupRecyclerView();
        setupCategoryChips();
        setupSearch();

        swipeRefresh.setOnRefreshListener(this::loadProducts);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadProducts();
    }

    private void setupRecyclerView() {
        adapter = new ProductAdapter(requireContext(), this);
        rvProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProducts.setAdapter(adapter);
    }

    private void setupCategoryChips() {
        chipAll.setOnClickListener(v -> selectCategoryChip(null, chipAll));
        chipFood.setOnClickListener(v -> selectCategoryChip(ProductCategory.FOOD.name(), chipFood));
        chipElectronics.setOnClickListener(v -> selectCategoryChip(ProductCategory.ELECTRONICS.name(), chipElectronics));
        chipOthers.setOnClickListener(v -> selectCategoryChip(ProductCategory.OTHERS.name(), chipOthers));
    }

    private void selectCategoryChip(String category, TextView selectedView) {
        this.selectedCategory = category;

        // Reset all chips style
        TextView[] chips = {chipAll, chipFood, chipElectronics, chipOthers};
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }

        // Highlight selected
        selectedView.setBackgroundResource(R.drawable.bg_chip_selected);
        selectedView.setTextColor(getResources().getColor(R.color.white, null));

        loadProducts();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim();
                loadProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void loadProducts() {
        if (!isAdded()) return;
        swipeRefresh.setRefreshing(true);

        ApiService api = ApiClient.getService(requireContext());
        api.getProducts(selectedCategory, currentSearch).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Product> list = response.body();
                    adapter.setProducts(list);
                    layoutEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Failed to load products: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAddToCart(Product product, int quantity) {
        CartManager.getInstance().addItem(product, quantity);
        Toast.makeText(requireContext(), String.format("Added %dx %s to Cart!", quantity, product.getName()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onOrderNow(Product product, int quantity) {
        showDirectCheckoutDialog(product, quantity);
    }

    private void showDirectCheckoutDialog(Product product, int quantity) {
        if (!isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_direct_checkout, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Bind Views
        ImageView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        ImageView ivCheckoutProductImg = dialogView.findViewById(R.id.ivCheckoutProductImg);
        TextView tvCheckoutProductName = dialogView.findViewById(R.id.tvCheckoutProductName);
        TextView tvCheckoutQuantityPrice = dialogView.findViewById(R.id.tvCheckoutQuantityPrice);
        Spinner spinnerPickupZone = dialogView.findViewById(R.id.spinnerPickupZone);
        TextInputEditText etPreferredShop = dialogView.findViewById(R.id.etPreferredShop);

        TextView tvBreakdownProductCost = dialogView.findViewById(R.id.tvBreakdownProductCost);
        TextView tvBreakdownDeliveryFee = dialogView.findViewById(R.id.tvBreakdownDeliveryFee);
        TextView tvBreakdownMfsFee = dialogView.findViewById(R.id.tvBreakdownMfsFee);
        TextView tvBreakdownTotal = dialogView.findViewById(R.id.tvBreakdownTotal);

        RadioGroup rgPaymentMethod = dialogView.findViewById(R.id.rgPaymentMethod);
        RadioButton rbBkash = dialogView.findViewById(R.id.rbBkash);
        RadioButton rbNagad = dialogView.findViewById(R.id.rbNagad);
        View layoutMfsBanner = dialogView.findViewById(R.id.layoutMfsBanner);
        TextView tvMfsInstructions = dialogView.findViewById(R.id.tvMfsInstructions);

        TextInputEditText etSenderPhone = dialogView.findViewById(R.id.etSenderPhone);
        TextInputEditText etTrxId = dialogView.findViewById(R.id.etTrxId);
        MaterialButton btnConfirmOrder = dialogView.findViewById(R.id.btnConfirmOrder);
        ProgressBar progressBarCheckout = dialogView.findViewById(R.id.progressBarCheckout);

        // Populate Product Details
        tvCheckoutProductName.setText(product.getName());
        BigDecimal unitPrice = product.getEstimatedPrice();
        BigDecimal productCost = unitPrice.multiply(BigDecimal.valueOf(quantity));
        tvCheckoutQuantityPrice.setText(String.format("%dx ৳%.0f = ৳%.0f", quantity, unitPrice, productCost));

        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            Glide.with(requireContext()).load(product.getImageUrl()).centerCrop().into(ivCheckoutProductImg);
        }

        // Calculations
        BigDecimal deliveryFee = DeliveryChargeCalculator.calculateDeliveryCharge(product, quantity);
        BigDecimal subtotal = productCost.add(deliveryFee);
        BigDecimal mfsFee = DeliveryChargeCalculator.calculateGatewayFee(subtotal);
        BigDecimal grandTotal = subtotal.add(mfsFee);

        tvBreakdownProductCost.setText(String.format("৳%.2f", productCost));
        tvBreakdownDeliveryFee.setText(String.format("৳%.2f", deliveryFee));
        tvBreakdownMfsFee.setText(String.format("৳%.2f", mfsFee));
        tvBreakdownTotal.setText(String.format("৳%.2f", grandTotal));
        btnConfirmOrder.setText(String.format("Confirm & Pay ৳%.0f", grandTotal));

        // Populate Pickup Zone Spinner
        LocationArea[] areas = LocationArea.values();
        String[] areaNames = new String[areas.length];
        for (int i = 0; i < areas.length; i++) {
            areaNames[i] = areas[i].getDisplayName();
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, areaNames);
        spinnerPickupZone.setAdapter(spinnerAdapter);

        // Pre-fill phone if available in session
        SessionManager session = SessionManager.getInstance(requireContext());
        User currentUser = session.getUser();
        if (currentUser != null && currentUser.getPhone() != null) {
            etSenderPhone.setText(currentUser.getPhone());
        }

        // MFS toggle
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            String method = checkedId == R.id.rbNagad ? "Nagad" : "bKash";
            tvMfsInstructions.setText(String.format("Send Money to Carry Campus %s Account:\n01700000000 (Tap to copy)", method));
        });

        // Tap to copy account number
        layoutMfsBanner.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Campus Account", "01700000000"));
                Toast.makeText(requireContext(), "Copied 01700000000 to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        // Place Order
        btnConfirmOrder.setOnClickListener(v -> {
            String senderPhone = etSenderPhone.getText() != null ? etSenderPhone.getText().toString().trim() : "";
            String trxId = etTrxId.getText() != null ? etTrxId.getText().toString().trim() : "";
            String shopNotes = etPreferredShop.getText() != null ? etPreferredShop.getText().toString().trim() : "";

            if (senderPhone.isEmpty()) {
                etSenderPhone.setError("Sender phone is required");
                etSenderPhone.requestFocus();
                return;
            }

            if (trxId.isEmpty()) {
                etTrxId.setError("Transaction ID is required");
                etTrxId.requestFocus();
                return;
            }

            btnConfirmOrder.setEnabled(false);
            progressBarCheckout.setVisibility(View.VISIBLE);

            LocationArea selectedArea = areas[spinnerPickupZone.getSelectedItemPosition()];
            PaymentMethod method = rbNagad.isChecked() ? PaymentMethod.NAGAD : PaymentMethod.BKASH;

            CheckoutLineDto line = new CheckoutLineDto();
            line.setProductId(product.getId());
            line.setQuantity(quantity);
            line.setPickupArea(selectedArea);
            line.setPreferredShop(shopNotes);
            line.setInstructions(shopNotes);
            line.setPaymentMethod(method);
            line.setSenderPhone(senderPhone);
            line.setTrxId(trxId);

            ApiService api = ApiClient.getService(requireContext());
            api.checkout(Collections.singletonList(line)).enqueue(new Callback<List<ProductRequest>>() {
                @Override
                public void onResponse(Call<List<ProductRequest>> call, Response<List<ProductRequest>> response) {
                    btnConfirmOrder.setEnabled(true);
                    progressBarCheckout.setVisibility(View.GONE);

                    if (response.isSuccessful()) {
                        String firebaseUid = SessionManager.getInstance(requireContext()).getFirebaseUid();
                        if (response.body() != null && !response.body().isEmpty()) {
                            com.carry.app.data.firebase.LiveStatusStore liveStore = new com.carry.app.data.firebase.LiveStatusStoreImpl();
                            for (ProductRequest pr : response.body()) {
                                if (pr.getId() != null) {
                                    liveStore.emitOrderEvent(pr.getId(), "REQUESTED", firebaseUid, null);
                                }
                            }
                        }
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "⚡ Order placed successfully!", Toast.LENGTH_LONG).show();

                        // Switch to My Requests tab in MainActivity
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToTab(R.id.nav_my_requests);
                        }
                    } else {
                        String err = parseCheckoutError(response);
                        Toast.makeText(requireContext(), "Order checkout failed: " + err, Toast.LENGTH_LONG).show();
                        if (response.code() == 401 || response.code() == 403) {
                            SessionManager.getInstance(requireContext()).logout();
                            if (getActivity() != null) {
                                getActivity().startActivity(new Intent(getActivity(), com.carry.app.ui.auth.LoginActivity.class));
                                getActivity().finish();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<ProductRequest>> call, Throwable t) {
                    btnConfirmOrder.setEnabled(true);
                    progressBarCheckout.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private String parseCheckoutError(Response<?> response) {
        if (response.code() == 401 || response.code() == 403) {
            return "Session expired. Please log in again.";
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
