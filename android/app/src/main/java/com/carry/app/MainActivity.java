package com.carry.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carry.app.adapters.CartAdapter;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.CartItem;
import com.carry.app.models.CheckoutLineDto;
import com.carry.app.models.LocationArea;
import com.carry.app.models.PaymentMethod;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.User;
import com.carry.app.ui.auth.LoginActivity;
import com.carry.app.ui.profile.ProfileFragment;
import com.carry.app.ui.requests.MyRequestsFragment;
import com.carry.app.ui.requests.NewRequestFragment;
import com.carry.app.ui.shop.ShopFragment;
import com.carry.app.ui.trips.DeliveriesFragment;
import com.carry.app.utils.CartManager;
import com.carry.app.utils.DeliveryChargeCalculator;
import com.carry.app.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements CartManager.CartChangeListener {

    private Toolbar toolbar;
    private FrameLayout btnCartHeader;
    private TextView tvCartBadge;
    private BottomNavigationView bottomNavigation;

    private SessionManager sessionManager;
    private CartManager cartManager;

    private final ShopFragment shopFragment = new ShopFragment();
    private final NewRequestFragment newRequestFragment = new NewRequestFragment();
    private final MyRequestsFragment myRequestsFragment = new MyRequestsFragment();
    private final DeliveriesFragment deliveriesFragment = new DeliveriesFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        cartManager = CartManager.getInstance();

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        cartManager.addListener(this);

        initViews();
        setupNavigation();
        updateCartBadge();

        if (savedInstanceState == null) {
            switchFragment(shopFragment);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnCartHeader = findViewById(R.id.btnCartHeader);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        btnCartHeader.setOnClickListener(v -> showCartBottomSheet());
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_shop) {
                switchFragment(shopFragment);
                return true;
            } else if (id == R.id.nav_new_request) {
                switchFragment(newRequestFragment);
                return true;
            } else if (id == R.id.nav_my_requests) {
                switchFragment(myRequestsFragment);
                return true;
            } else if (id == R.id.nav_deliveries) {
                switchFragment(deliveriesFragment);
                return true;
            } else if (id == R.id.nav_profile) {
                switchFragment(profileFragment);
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void navigateToTab(int navItemId) {
        bottomNavigation.setSelectedItemId(navItemId);
    }

    @Override
    public void onCartChanged() {
        runOnUiThread(this::updateCartBadge);
    }

    private void updateCartBadge() {
        int count = cartManager.getTotalItemCount();
        if (count > 0) {
            tvCartBadge.setVisibility(View.VISIBLE);
            tvCartBadge.setText(String.valueOf(count));
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }

    private void showCartBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cart_bottom_sheet, null);
        sheet.setContentView(dialogView);

        // Views
        TextView tvCartSheetTitle = dialogView.findViewById(R.id.tvCartSheetTitle);
        TextView btnClearCart = dialogView.findViewById(R.id.btnClearCart);
        ImageView btnCloseCartSheet = dialogView.findViewById(R.id.btnCloseCartSheet);
        RecyclerView rvCartItems = dialogView.findViewById(R.id.rvCartItems);
        View layoutCartEmpty = dialogView.findViewById(R.id.layoutCartEmpty);
        View layoutCheckoutSection = dialogView.findViewById(R.id.layoutCheckoutSection);

        Spinner spinnerCartPickupZone = dialogView.findViewById(R.id.spinnerCartPickupZone);
        TextInputEditText etCartInstructions = dialogView.findViewById(R.id.etCartInstructions);
        TextView tvCartProductTotal = dialogView.findViewById(R.id.tvCartProductTotal);
        TextView tvCartDeliveryTotal = dialogView.findViewById(R.id.tvCartDeliveryTotal);
        TextView tvCartMfsFee = dialogView.findViewById(R.id.tvCartMfsFee);
        TextView tvCartGrandTotal = dialogView.findViewById(R.id.tvCartGrandTotal);

        RadioGroup rgCartPaymentMethod = dialogView.findViewById(R.id.rgCartPaymentMethod);
        RadioButton rbCartBkash = dialogView.findViewById(R.id.rbCartBkash);
        RadioButton rbCartNagad = dialogView.findViewById(R.id.rbCartNagad);
        TextInputEditText etCartSenderPhone = dialogView.findViewById(R.id.etCartSenderPhone);
        TextInputEditText etCartTrxId = dialogView.findViewById(R.id.etCartTrxId);
        MaterialButton btnCartCheckout = dialogView.findViewById(R.id.btnCartCheckout);
        ProgressBar progressBarCartCheckout = dialogView.findViewById(R.id.progressBarCartCheckout);

        // Pre-fill phone
        User user = sessionManager.getUser();
        if (user != null && user.getPhone() != null) {
            etCartSenderPhone.setText(user.getPhone());
        }

        // Setup pickup spinner
        LocationArea[] areas = LocationArea.values();
        String[] areaNames = new String[areas.length];
        for (int i = 0; i < areas.length; i++) {
            areaNames[i] = areas[i].getDisplayName();
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, areaNames);
        spinnerCartPickupZone.setAdapter(spinnerAdapter);

        // Setup Cart adapter
        final CartAdapter[] cartAdapterHolder = new CartAdapter[1];
        CartAdapter cartAdapter = new CartAdapter(this, new CartAdapter.OnCartItemInteractionListener() {
            @Override
            public void onQuantityChanged(Long productId, int newQuantity) {
                cartManager.updateQuantity(productId, newQuantity);
                if (cartAdapterHolder[0] != null) {
                    updateCartSheetUI(dialogView, cartAdapterHolder[0]);
                }
            }

            @Override
            public void onItemRemoved(Long productId) {
                cartManager.removeItem(productId);
                if (cartAdapterHolder[0] != null) {
                    updateCartSheetUI(dialogView, cartAdapterHolder[0]);
                }
            }
        });
        cartAdapterHolder[0] = cartAdapter;

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(cartAdapter);

        updateCartSheetUI(dialogView, cartAdapter);

        btnClearCart.setOnClickListener(v -> {
            cartManager.clear();
            updateCartSheetUI(dialogView, cartAdapter);
        });

        btnCloseCartSheet.setOnClickListener(v -> sheet.dismiss());

        // Confirm Checkout
        btnCartCheckout.setOnClickListener(v -> {
            List<CartItem> items = cartManager.getItems();
            if (items.isEmpty()) return;

            String senderPhone = etCartSenderPhone.getText() != null ? etCartSenderPhone.getText().toString().trim() : "";
            String trxId = etCartTrxId.getText() != null ? etCartTrxId.getText().toString().trim() : "";
            String instructions = etCartInstructions.getText() != null ? etCartInstructions.getText().toString().trim() : "";

            if (senderPhone.isEmpty()) {
                etCartSenderPhone.setError("Sender phone is required");
                return;
            }
            if (trxId.isEmpty()) {
                etCartTrxId.setError("TrxID is required");
                return;
            }

            btnCartCheckout.setEnabled(false);
            progressBarCartCheckout.setVisibility(View.VISIBLE);

            LocationArea selectedArea = areas[spinnerCartPickupZone.getSelectedItemPosition()];
            PaymentMethod method = rbCartNagad.isChecked() ? PaymentMethod.NAGAD : PaymentMethod.BKASH;

            List<CheckoutLineDto> checkoutLines = new ArrayList<>();
            for (CartItem item : items) {
                if (item.getProduct() == null) continue;
                CheckoutLineDto line = new CheckoutLineDto();
                line.setProductId(item.getProduct().getId());
                line.setQuantity(item.getQuantity());
                line.setPickupArea(selectedArea);
                line.setPreferredShop(instructions);
                line.setInstructions(instructions);
                line.setPaymentMethod(method);
                line.setSenderPhone(senderPhone);
                line.setTrxId(trxId);
                checkoutLines.add(line);
            }

            ApiService api = ApiClient.getService(this);
            api.checkout(checkoutLines).enqueue(new Callback<List<ProductRequest>>() {
                @Override
                public void onResponse(Call<List<ProductRequest>> call, Response<List<ProductRequest>> response) {
                    btnCartCheckout.setEnabled(true);
                    progressBarCartCheckout.setVisibility(View.GONE);

                    if (response.isSuccessful()) {
                        String firebaseUid = sessionManager.getFirebaseUid();
                        if (response.body() != null && !response.body().isEmpty()) {
                            com.carry.app.data.firebase.LiveStatusStore liveStore = new com.carry.app.data.firebase.LiveStatusStoreImpl();
                            for (ProductRequest pr : response.body()) {
                                if (pr.getId() != null) {
                                    liveStore.emitOrderEvent(pr.getId(), "REQUESTED", firebaseUid, null);
                                }
                            }
                        }
                        cartManager.clear();
                        sheet.dismiss();
                        Toast.makeText(MainActivity.this, "Cart checkout complete! Orders submitted.", Toast.LENGTH_LONG).show();
                        navigateToTab(R.id.nav_my_requests);
                    } else {
                        String errMsg = parseCheckoutError(response);
                        Toast.makeText(MainActivity.this, "Checkout failed: " + errMsg, Toast.LENGTH_LONG).show();
                        if (response.code() == 401 || response.code() == 403) {
                            sessionManager.logout();
                            startActivity(new Intent(MainActivity.this, com.carry.app.ui.auth.LoginActivity.class));
                            finish();
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<ProductRequest>> call, Throwable t) {
                    btnCartCheckout.setEnabled(true);
                    progressBarCartCheckout.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        sheet.show();
    }

    private String parseCheckoutError(Response<?> response) {
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

    private void updateCartSheetUI(View dialogView, CartAdapter adapter) {
        List<CartItem> items = cartManager.getItems();
        adapter.setItems(items);

        View layoutCartEmpty = dialogView.findViewById(R.id.layoutCartEmpty);
        View layoutCheckoutSection = dialogView.findViewById(R.id.layoutCheckoutSection);
        TextView tvCartSheetTitle = dialogView.findViewById(R.id.tvCartSheetTitle);
        TextView tvCartProductTotal = dialogView.findViewById(R.id.tvCartProductTotal);
        TextView tvCartDeliveryTotal = dialogView.findViewById(R.id.tvCartDeliveryTotal);
        TextView tvCartMfsFee = dialogView.findViewById(R.id.tvCartMfsFee);
        TextView tvCartGrandTotal = dialogView.findViewById(R.id.tvCartGrandTotal);
        MaterialButton btnCartCheckout = dialogView.findViewById(R.id.btnCartCheckout);

        if (items.isEmpty()) {
            layoutCartEmpty.setVisibility(View.VISIBLE);
            layoutCheckoutSection.setVisibility(View.GONE);
            tvCartSheetTitle.setText("🛒 Campus Cart (0)");
        } else {
            layoutCartEmpty.setVisibility(View.GONE);
            layoutCheckoutSection.setVisibility(View.VISIBLE);
            tvCartSheetTitle.setText(String.format("🛒 Campus Cart (%d items)", cartManager.getTotalItemCount()));

            BigDecimal productCost = cartManager.getTotalProductCost();
            BigDecimal deliveryFee = cartManager.getTotalDeliveryCharge();
            BigDecimal subtotal = productCost.add(deliveryFee);
            BigDecimal mfsFee = DeliveryChargeCalculator.calculateGatewayFee(subtotal);
            BigDecimal total = subtotal.add(mfsFee);

            tvCartProductTotal.setText(String.format("৳%.2f", productCost));
            tvCartDeliveryTotal.setText(String.format("৳%.2f", deliveryFee));
            tvCartMfsFee.setText(String.format("৳%.2f", mfsFee));
            tvCartGrandTotal.setText(String.format("৳%.2f", total));
            btnCartCheckout.setText(String.format("Confirm Cart Checkout (৳%.0f)", total));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartManager != null) {
            cartManager.removeListener(this);
        }
    }
}
