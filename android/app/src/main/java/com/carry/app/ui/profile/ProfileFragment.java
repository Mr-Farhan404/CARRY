package com.carry.app.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.carry.app.R;
import com.carry.app.MainActivity;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.*;
import com.carry.app.ui.auth.LoginActivity;
import com.carry.app.utils.CartManager;
import com.carry.app.data.firebase.FirebaseAuthGateway;
import com.carry.app.data.firebase.FirebaseAuthGatewayImpl;
import com.carry.app.data.firebase.PhotoStore;
import com.carry.app.data.firebase.PhotoStoreImpl;
import com.carry.app.data.firebase.ProfileStore;
import com.carry.app.data.firebase.ProfileStoreImpl;
import com.carry.app.utils.ImageResizer;
import com.carry.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.bumptech.glide.Glide;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.math.BigDecimal;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private ImageView ivProfileAvatar;
    private ProgressBar pbPhotoUpload;
    private MaterialButton btnChangePhoto;
    private ImageButton btnEditDisplayName;

    private ProfileViewModel profileViewModel;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && getContext() != null) {
                    try (java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                        if (is != null) {
                            byte[] compressed = ImageResizer.processAndCompress(
                                    is,
                                    ImageResizer.FALLBACK_MAX_DIMENSION,
                                    ImageResizer.FALLBACK_MAX_BYTES,
                                    ImageResizer.DEFAULT_JPEG_QUALITY
                            );
                            if (compressed != null && profileViewModel != null) {
                                profileViewModel.uploadProfilePhoto(compressed);
                            } else if (getContext() != null) {
                                Toast.makeText(getContext(), "Failed to compress profile photo.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Unable to read selected photo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private TextView tvProfileName;
    private TextView tvProfileStudentId;
    private TextView tvProfileRoleBadge;
    private TextView tvProfileEmail;
    private TextView tvProfilePhone;

    private TextView tvRoleDescription;
    private LinearLayout llStudentActions;
    private MaterialButton btnApplyPartner;
    private MaterialButton btnApplyModerator;

    private LinearLayout llModeratorActions;
    private MaterialButton btnModManageProducts;
    private MaterialButton btnModViewAllOrders;
    private MaterialButton btnReviewPartnerApps;

    private LinearLayout llAdminActions;
    private MaterialButton btnManageAccounts;
    private MaterialButton btnAdminManageProducts;
    private MaterialButton btnAdminViewAllOrders;
    private MaterialButton btnAdminReviewPartnerApps;
    private MaterialButton btnViewModReports;
    private MaterialButton btnReviewModApps;

    private TextView tvApplicationsTitle;
    private LinearLayout llApplicationsList;

    private RadioGroup rgServerEnv;
    private RadioButton rbLocalServer;
    private RadioButton rbCloudServer;
    private MaterialButton btnLogout;

    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = SessionManager.getInstance(requireContext());

        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        pbPhotoUpload = view.findViewById(R.id.pbPhotoUpload);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnEditDisplayName = view.findViewById(R.id.btnEditDisplayName);

        ProfileStore profileStore = new ProfileStoreImpl();
        PhotoStore photoStore = new PhotoStoreImpl(profileStore);
        profileViewModel = new ProfileViewModel(profileStore, photoStore);
        observeProfileViewModel();

        btnChangePhoto.setOnClickListener(v -> {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnEditDisplayName.setOnClickListener(v -> showEditDisplayNameDialog());

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileStudentId = view.findViewById(R.id.tvProfileStudentId);
        tvProfileRoleBadge = view.findViewById(R.id.tvProfileRoleBadge);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);

        tvRoleDescription = view.findViewById(R.id.tvRoleDescription);
        llStudentActions = view.findViewById(R.id.llStudentActions);
        btnApplyPartner = view.findViewById(R.id.btnApplyPartner);
        btnApplyModerator = view.findViewById(R.id.btnApplyModerator);

        llModeratorActions = view.findViewById(R.id.llModeratorActions);
        btnModManageProducts = view.findViewById(R.id.btnModManageProducts);
        btnModViewAllOrders = view.findViewById(R.id.btnModViewAllOrders);
        btnReviewPartnerApps = view.findViewById(R.id.btnReviewPartnerApps);

        llAdminActions = view.findViewById(R.id.llAdminActions);
        btnManageAccounts = view.findViewById(R.id.btnManageAccounts);
        btnAdminManageProducts = view.findViewById(R.id.btnAdminManageProducts);
        btnAdminViewAllOrders = view.findViewById(R.id.btnAdminViewAllOrders);
        btnAdminReviewPartnerApps = view.findViewById(R.id.btnAdminReviewPartnerApps);
        btnReviewModApps = view.findViewById(R.id.btnReviewModApps);
        btnViewModReports = view.findViewById(R.id.btnViewModReports);

        tvApplicationsTitle = view.findViewById(R.id.tvApplicationsTitle);
        llApplicationsList = view.findViewById(R.id.llApplicationsList);

        rgServerEnv = view.findViewById(R.id.rgServerEnv);
        rbLocalServer = view.findViewById(R.id.rbLocalServer);
        rbCloudServer = view.findViewById(R.id.rbCloudServer);
        btnLogout = view.findViewById(R.id.btnLogout);

        populateUserData();
        setupServerEnvironmentToggle();

        btnApplyPartner.setOnClickListener(v -> showApplyDeliveryPartnerDialog());
        btnApplyModerator.setOnClickListener(v -> showApplyModeratorDialog());

        // Moderator handlers
        btnModManageProducts.setOnClickListener(v -> showManageProductsDialog());
        btnModViewAllOrders.setOnClickListener(v -> showAllOrdersDialog());
        btnReviewPartnerApps.setOnClickListener(v -> showPartnerApplicationsDialog());

        // Admin handlers
        btnManageAccounts.setOnClickListener(v -> showManageAccountsDialog());
        btnAdminManageProducts.setOnClickListener(v -> showManageProductsDialog());
        btnAdminViewAllOrders.setOnClickListener(v -> showAllOrdersDialog());
        btnAdminReviewPartnerApps.setOnClickListener(v -> showPartnerApplicationsDialog());
        btnReviewModApps.setOnClickListener(v -> showModeratorApplicationsDialog());
        btnViewModReports.setOnClickListener(v -> showModeratorReportsDialog());

        btnLogout.setOnClickListener(v -> confirmLogout());

        fetchFreshUserProfile();

        return view;
    }

    private void fetchFreshUserProfile() {
        ApiService api = ApiClient.getService(requireContext());
        api.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveUser(response.body());
                    if (isAdded()) {
                        populateUserData();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void populateUserData() {
        User user = sessionManager.getUser();
        if (user == null) return;

        tvProfileName.setText(user.getFullName() != null ? user.getFullName() : "KUET Member");
        String sid = user.getStudentId() != null ? user.getStudentId() : "N/A";
        String dept = user.getDepartment() != null ? user.getDepartment() : "General";
        tvProfileStudentId.setText(String.format("ID: %s • Dept: %s", sid, dept));

        tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        tvProfilePhone.setText(user.getPhone() != null ? user.getPhone() : "N/A");

        String firebaseUid = sessionManager.getFirebaseUid();
        if (firebaseUid == null) {
            firebaseUid = new FirebaseAuthGatewayImpl().getCurrentFirebaseUid();
            if (firebaseUid != null) {
                sessionManager.saveFirebaseUid(firebaseUid);
            }
        }
        if (firebaseUid != null && profileViewModel != null) {
            profileViewModel.loadOrSyncProfile(firebaseUid, user);
        }

        UserRole role = user.getRole();
        configureRoleUi(role);
    }

    private void configureRoleUi(UserRole role) {
        if (role == null) role = UserRole.STUDENT;

        llStudentActions.setVisibility(View.GONE);
        llModeratorActions.setVisibility(View.GONE);
        llAdminActions.setVisibility(View.GONE);

        switch (role) {
            case ADMIN:
                tvProfileRoleBadge.setText("👑 Admin (Full Access)");
                tvProfileRoleBadge.setBackgroundResource(R.drawable.bg_badge_danger);
                tvRoleDescription.setText("You have complete administrative authority: product catalog, account deletion, moderator report audits, and partner approvals.");
                llAdminActions.setVisibility(View.VISIBLE);
                break;

            case MODERATOR:
                tvProfileRoleBadge.setText("🛡️ Moderator");
                tvProfileRoleBadge.setBackgroundResource(R.drawable.bg_badge_primary);
                tvRoleDescription.setText("You can manage catalog products, update prices/pictures, and review student delivery partner requests. All actions are reported to Admin.");
                llModeratorActions.setVisibility(View.VISIBLE);
                break;

            case DELIVERY_PARTNER:
                tvProfileRoleBadge.setText("🚗 Delivery Partner");
                tvProfileRoleBadge.setBackgroundResource(R.drawable.bg_badge_primary);
                tvRoleDescription.setText("You are authorized to post planned trips to Khulna and accept student order deliveries.");
                llStudentActions.setVisibility(View.VISIBLE);
                btnApplyPartner.setVisibility(View.GONE);
                loadMyApplications();
                break;

            case TEACHER:
                tvProfileRoleBadge.setText("👨‍🏫 Faculty Member");
                tvProfileRoleBadge.setBackgroundResource(R.drawable.bg_badge_success);
                tvRoleDescription.setText("Direct order placement enabled. Faculty accounts are restricted from delivery partner and moderator operations.");
                break;

            case STUDENT:
            default:
                tvProfileRoleBadge.setText("🎓 Student Member");
                tvProfileRoleBadge.setBackgroundResource(R.drawable.bg_badge_success);
                tvRoleDescription.setText("You can place catalog and custom requests. You may apply below to become a Delivery Partner or Moderator.");
                llStudentActions.setVisibility(View.VISIBLE);
                btnApplyPartner.setVisibility(View.VISIBLE);
                btnApplyModerator.setVisibility(View.VISIBLE);
                loadMyApplications();
                break;
        }
    }

    private void showApplyDeliveryPartnerDialog() {
        User currentUser = sessionManager.getUser();
        if (currentUser != null && (currentUser.getRole() == UserRole.DELIVERY_PARTNER || currentUser.getRole() == UserRole.ADMIN)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Already Authorized")
                    .setMessage("You already have Delivery Partner privileges as " + currentUser.getRole().name() + "!\nYou can go directly to the Deliveries tab to post trips and accept delivery orders.")
                    .setPositiveButton("Go to Deliveries", (d, w) -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToTab(R.id.nav_deliveries);
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
            return;
        }

        if (currentUser != null && currentUser.getRole() == UserRole.TEACHER) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Not Eligible")
                    .setMessage("Teacher accounts are not permitted to apply for Delivery Partner roles.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_apply_delivery_partner, null);
        EditText etAltPhone = dialogView.findViewById(R.id.etAltPhone);
        EditText etBkashNumber = dialogView.findViewById(R.id.etBkashNumber);
        EditText etReturnTime = dialogView.findViewById(R.id.etReturnTime);
        EditText etReason = dialogView.findViewById(R.id.etReason);
        CheckBox cbAgreeRules = dialogView.findViewById(R.id.cbAgreeRules);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Submit Application", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitBtn.setOnClickListener(v -> {
                String altPhone = etAltPhone.getText().toString().trim();
                String bkashNumber = etBkashNumber.getText().toString().trim();
                String returnTime = etReturnTime.getText().toString().trim();
                String reason = etReason.getText().toString().trim();

                if (altPhone.isEmpty()) {
                    etAltPhone.setError("Alternative phone number is required");
                    etAltPhone.requestFocus();
                    return;
                }
                if (bkashNumber.isEmpty()) {
                    etBkashNumber.setError("bKash number is required");
                    etBkashNumber.requestFocus();
                    return;
                }
                if (returnTime.isEmpty()) {
                    etReturnTime.setError("Estimated return time is required");
                    etReturnTime.requestFocus();
                    return;
                }
                if (reason.isEmpty()) {
                    etReason.setError("Please explain why you want to be a delivery partner");
                    etReason.requestFocus();
                    return;
                }
                if (!cbAgreeRules.isChecked()) {
                    Toast.makeText(requireContext(), "You must agree to all rules to apply", Toast.LENGTH_SHORT).show();
                    return;
                }

                ApplyRoleDto dto = new ApplyRoleDto(
                        UserRole.DELIVERY_PARTNER,
                        altPhone,
                        bkashNumber,
                        returnTime,
                        null,
                        true,
                        reason
                );

                ApiService api = ApiClient.getService(requireContext());
                api.applyRole(dto).enqueue(new Callback<RoleApplication>() {
                    @Override
                    public void onResponse(Call<RoleApplication> call, Response<RoleApplication> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Delivery Partner application submitted!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            loadMyApplications();
                        } else {
                            String err = parseErrorMessage(response);
                            Toast.makeText(requireContext(), "Submission Failed: " + err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RoleApplication> call, Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(requireContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private String parseErrorMessage(Response<?> response) {
        if (response.code() == 401 || response.code() == 403) {
            return "Role application is not available on this cloud backend version. To test partner features, please use the admin account (farhan@kuet.ac.bd)!";
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

    private void showApplyModeratorDialog() {
        User currentUser = sessionManager.getUser();
        if (currentUser != null && (currentUser.getRole() == UserRole.MODERATOR || currentUser.getRole() == UserRole.ADMIN)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Already Authorized")
                    .setMessage("You already have Moderator privileges as " + currentUser.getRole().name() + "!")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (currentUser != null && currentUser.getRole() == UserRole.TEACHER) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Not Eligible")
                    .setMessage("Teacher accounts are not permitted to apply for Moderator roles.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_apply_moderator, null);
        EditText etAltPhone = dialogView.findViewById(R.id.etModAltPhone);
        EditText etBkashNumber = dialogView.findViewById(R.id.etModBkashNumber);
        RadioButton rbCommitmentYes = dialogView.findViewById(R.id.rbCommitmentYes);
        EditText etReason = dialogView.findViewById(R.id.etModReason);
        CheckBox cbAgreeRules = dialogView.findViewById(R.id.cbModAgreeRules);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Submit Application", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitBtn.setOnClickListener(v -> {
                String altPhone = etAltPhone.getText().toString().trim();
                String bkashNumber = etBkashNumber.getText().toString().trim();
                boolean commitment = rbCommitmentYes != null && rbCommitmentYes.isChecked();
                String reason = etReason.getText().toString().trim();

                if (altPhone.isEmpty()) {
                    etAltPhone.setError("Alternative phone number is required");
                    etAltPhone.requestFocus();
                    return;
                }
                if (bkashNumber.isEmpty()) {
                    etBkashNumber.setError("bKash number is required");
                    etBkashNumber.requestFocus();
                    return;
                }
                if (!commitment) {
                    Toast.makeText(requireContext(), "You must commit to working actively and honestly", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (reason.isEmpty()) {
                    etReason.setError("Please explain why you want to be a moderator");
                    etReason.requestFocus();
                    return;
                }
                if (!cbAgreeRules.isChecked()) {
                    Toast.makeText(requireContext(), "You must agree to all rules to apply", Toast.LENGTH_SHORT).show();
                    return;
                }

                ApplyRoleDto dto = new ApplyRoleDto(
                        UserRole.MODERATOR,
                        altPhone,
                        bkashNumber,
                        null,
                        commitment,
                        true,
                        reason
                );

                ApiService api = ApiClient.getService(requireContext());
                api.applyRole(dto).enqueue(new Callback<RoleApplication>() {
                    @Override
                    public void onResponse(Call<RoleApplication> call, Response<RoleApplication> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Moderator application submitted!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            loadMyApplications();
                        } else {
                            String err = parseErrorMessage(response);
                            Toast.makeText(requireContext(), "Submission Failed: " + err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RoleApplication> call, Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(requireContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void loadMyApplications() {
        ApiService api = ApiClient.getService(requireContext());
        api.getMyApplications().enqueue(new Callback<List<RoleApplication>>() {
            @Override
            public void onResponse(Call<List<RoleApplication>> call, Response<List<RoleApplication>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                List<RoleApplication> apps = response.body();
                llApplicationsList.removeAllViews();

                if (apps.isEmpty()) {
                    tvApplicationsTitle.setVisibility(View.GONE);
                    return;
                }

                tvApplicationsTitle.setVisibility(View.VISIBLE);
                for (RoleApplication app : apps) {
                    TextView row = new TextView(requireContext());
                    String roleName = app.getRequestedRole() == UserRole.DELIVERY_PARTNER ? "🚗 Delivery Partner" : "🛡️ Moderator";
                    String statusText = app.getStatus() != null ? app.getStatus().name() : "PENDING";
                    row.setText(String.format("%s: %s\nReason: %s", roleName, statusText, app.getReason() != null ? app.getReason() : ""));
                    row.setTextSize(12);
                    row.setPadding(0, 8, 0, 8);
                    llApplicationsList.addView(row);
                }
            }

            @Override
            public void onFailure(Call<List<RoleApplication>> call, Throwable t) {}
        });
    }

    // Moderator: Review Delivery Partner Applications
    private void showPartnerApplicationsDialog() {
        ApiService api = ApiClient.getService(requireContext());
        api.getPendingPartnerApplications().enqueue(new Callback<List<RoleApplication>>() {
            @Override
            public void onResponse(Call<List<RoleApplication>> call, Response<List<RoleApplication>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Partner Applications")
                            .setMessage("No pending delivery partner applications right now.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                List<RoleApplication> apps = response.body();
                String[] items = new String[apps.size()];
                for (int i = 0; i < apps.size(); i++) {
                    RoleApplication a = apps.get(i);
                    items[i] = String.format("#%d %s (%s)\n%s", a.getId(), a.getUserName(), a.getStudentId(), a.getReason() != null ? a.getReason() : "");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Pending Delivery Partner Applications")
                        .setItems(items, (dialog, which) -> {
                            RoleApplication selected = apps.get(which);
                            showReviewApplicationActionDialog(selected, false);
                        })
                        .setNegativeButton("Close", null)
                        .show();
            }

            @Override
            public void onFailure(Call<List<RoleApplication>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Admin: Review Moderator Applications
    private void showModeratorApplicationsDialog() {
        ApiService api = ApiClient.getService(requireContext());
        api.getModeratorApplications().enqueue(new Callback<List<RoleApplication>>() {
            @Override
            public void onResponse(Call<List<RoleApplication>> call, Response<List<RoleApplication>> response) {
                if (!isAdded() || getContext() == null) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Moderator Applications")
                            .setMessage("No pending moderator applications.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                List<RoleApplication> apps = response.body();
                String[] items = new String[apps.size()];
                for (int i = 0; i < apps.size(); i++) {
                    RoleApplication a = apps.get(i);
                    items[i] = String.format("#%d %s (%s) [%s]\n%s", a.getId(), a.getUserName(), a.getStudentId(), a.getStatus(), a.getReason() != null ? a.getReason() : "");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Moderator Applications")
                        .setItems(items, (dialog, which) -> {
                            RoleApplication selected = apps.get(which);
                            if (selected.getStatus() == ApplicationStatus.PENDING) {
                                showReviewApplicationActionDialog(selected, true);
                            } else {
                                Toast.makeText(requireContext(), "Application is already " + selected.getStatus(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Close", null)
                        .show();
            }

            @Override
            public void onFailure(Call<List<RoleApplication>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReviewApplicationActionDialog(RoleApplication app, boolean isModeratorApp) {
        StringBuilder sb = new StringBuilder();
        sb.append("Applicant: ").append(app.getUserName() != null ? app.getUserName() : "User")
          .append(" (").append(app.getUserEmail()).append(")\n");
        if (app.getAlternativePhone() != null && !app.getAlternativePhone().trim().isEmpty()) {
            sb.append("📞 Alt Phone: ").append(app.getAlternativePhone()).append("\n");
        }
        if (app.getBkashNumber() != null && !app.getBkashNumber().trim().isEmpty()) {
            sb.append("💳 bKash No: ").append(app.getBkashNumber()).append("\n");
        }
        if (!isModeratorApp && app.getEstimatedReturnTime() != null && !app.getEstimatedReturnTime().trim().isEmpty()) {
            sb.append("⏰ Return Time: ").append(app.getEstimatedReturnTime()).append("\n");
        }
        if (isModeratorApp && app.getActiveHonestCommitment() != null) {
            sb.append("🤝 Work Actively & Honestly: ").append(app.getActiveHonestCommitment() ? "Yes" : "No").append("\n");
        }
        sb.append("📝 Reason: ").append(app.getReason() != null ? app.getReason() : "").append("\n");
        sb.append("📜 Agreed to Rules: ").append(Boolean.TRUE.equals(app.getAgreedToRules()) ? "Yes" : "No");

        new AlertDialog.Builder(requireContext())
                .setTitle("Review Application #" + app.getId())
                .setMessage(sb.toString())
                .setPositiveButton("✓ Approve", (dialog, which) -> {
                    executeApplicationReview(app.getId(), true, isModeratorApp);
                })
                .setNegativeButton("✕ Reject", (dialog, which) -> {
                    executeApplicationReview(app.getId(), false, isModeratorApp);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void executeApplicationReview(Long appId, boolean approve, boolean isModeratorApp) {
        ApiService api = ApiClient.getService(requireContext());
        Call<RoleApplication> call = isModeratorApp ?
                (approve ? api.approveModeratorApplication(appId, "Approved via Android Admin Panel") : api.rejectModeratorApplication(appId, "Rejected via Android Admin Panel")) :
                (approve ? api.approvePartnerApplication(appId, "Approved via Android Moderator Panel") : api.rejectPartnerApplication(appId, "Rejected via Android Moderator Panel"));

        call.enqueue(new Callback<RoleApplication>() {
            @Override
            public void onResponse(Call<RoleApplication> call, Response<RoleApplication> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Application #" + appId + (approve ? " APPROVED!" : " REJECTED!"), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RoleApplication> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Admin: View Moderator Reports
    private void showModeratorReportsDialog() {
        ApiService api = ApiClient.getService(requireContext());
        api.getModeratorReports().enqueue(new Callback<List<ModeratorReport>>() {
            @Override
            public void onResponse(Call<List<ModeratorReport>> call, Response<List<ModeratorReport>> response) {
                if (!isAdded() || getContext() == null) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Moderator Reports")
                            .setMessage("No moderator activity reports logged yet.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                List<ModeratorReport> reports = response.body();
                StringBuilder sb = new StringBuilder();
                for (ModeratorReport r : reports) {
                    sb.append("• [").append(r.getActionType()).append("] by ")
                      .append(r.getModeratorName()).append("\n")
                      .append("  ").append(r.getDescription()).append("\n\n");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("📜 Moderator Activity Reports")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("Close", null)
                        .show();
            }

            @Override
            public void onFailure(Call<List<ModeratorReport>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Admin Only: Account Management & Deletion
    private void showManageAccountsDialog() {
        ApiService api = ApiClient.getService(requireContext());
        api.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (!isAdded() || getContext() == null) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    Toast.makeText(requireContext(), "No users found or failed to load users.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<User> users = response.body();
                String[] userItems = new String[users.size()];
                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    userItems[i] = String.format("#%d %s [%s]\nID: %s • %s",
                            u.getId(),
                            u.getFullName() != null ? u.getFullName() : "N/A",
                            u.getRole() != null ? u.getRole() : "STUDENT",
                            u.getStudentId() != null ? u.getStudentId() : "N/A",
                            u.getEmail() != null ? u.getEmail() : "N/A");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("👥 User Account Management (" + users.size() + ")")
                        .setItems(userItems, (dialog, which) -> {
                            User selectedUser = users.get(which);
                            showUserAccountDetailsDialog(selectedUser);
                        })
                        .setNegativeButton("Close", null)
                        .show();
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Error loading users: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserAccountDetailsDialog(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("User ID: #").append(user.getId()).append("\n");
        sb.append("Full Name: ").append(user.getFullName()).append("\n");
        sb.append("Student ID: ").append(user.getStudentId()).append("\n");
        sb.append("Role: ").append(user.getRole()).append("\n");
        sb.append("Email: ").append(user.getEmail()).append("\n");
        sb.append("Phone: ").append(user.getPhone() != null ? user.getPhone() : "N/A").append("\n");
        sb.append("Department: ").append(user.getDepartment() != null ? user.getDepartment() : "N/A");

        User myProfile = sessionManager.getUser();
        boolean isSelf = myProfile != null && myProfile.getId() != null && myProfile.getId().equals(user.getId());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Account: " + user.getFullName())
                .setMessage(sb.toString())
                .setPositiveButton("Close", null);

        if (!isSelf) {
            builder.setNeutralButton("🗑️ DELETE ACCOUNT", (dialog, which) -> {
                confirmDeleteAccount(user);
            });
        }

        builder.show();
    }

    private void confirmDeleteAccount(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Account Deletion")
                .setMessage(String.format("Are you sure you want to PERMANENTLY delete account:\n\n%s (%s)\nRole: %s\n\nAll orders, trips, and data for this user will be removed.",
                        user.getFullName(), user.getEmail(), user.getRole()))
                .setPositiveButton("Yes, Permanently Delete", (dialog, which) -> {
                    ApiService api = ApiClient.getService(requireContext());
                    api.deleteUser(user.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!isAdded() || getContext() == null) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "User account deleted successfully!", Toast.LENGTH_LONG).show();
                                showManageAccountsDialog();
                            } else {
                                Toast.makeText(requireContext(), "Failed to delete: " + response.message(), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            if (!isAdded() || getContext() == null) return;
                            Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Product Catalog Management (Admin & Moderator)
    private void showManageProductsDialog() {
        String[] options = {
                "➕ Add New Product to Catalog",
                "✏️ Edit Existing Products (Change Price / Display Picture / Status)"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("🛍️ Product Catalog Management")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddNewProductDialog();
                    } else {
                        showEditProductsListDialog();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddNewProductDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null);
        EditText etName = dialogView.findViewById(R.id.etProductName);
        RadioButton rbElectronics = dialogView.findViewById(R.id.rbCatElectronics);
        RadioButton rbStationery = dialogView.findViewById(R.id.rbCatStationery);
        EditText etPrice = dialogView.findViewById(R.id.etProductPrice);
        EditText etImageUrl = dialogView.findViewById(R.id.etProductImageUrl);
        EditText etDesc = dialogView.findViewById(R.id.etProductDesc);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Add Product", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitBtn.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String imgUrl = etImageUrl.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();

                if (name.isEmpty()) {
                    etName.setError("Product name is required");
                    etName.requestFocus();
                    return;
                }
                if (priceStr.isEmpty()) {
                    etPrice.setError("Price is required");
                    etPrice.requestFocus();
                    return;
                }

                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                } catch (Exception e) {
                    etPrice.setError("Invalid price format");
                    etPrice.requestFocus();
                    return;
                }

                ProductCategory category = ProductCategory.FOOD;
                if (rbElectronics.isChecked()) {
                    category = ProductCategory.ELECTRONICS;
                } else if (rbStationery.isChecked()) {
                    category = ProductCategory.OTHERS;
                }

                CreateProductDto dto = new CreateProductDto(
                        name,
                        category,
                        desc.isEmpty() ? null : desc,
                        imgUrl.isEmpty() ? null : imgUrl,
                        price
                );

                ApiService api = ApiClient.getService(requireContext());
                api.createModeratorProduct(dto).enqueue(new Callback<Product>() {
                    @Override
                    public void onResponse(Call<Product> call, Response<Product> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Product '" + name + "' added to catalog!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(requireContext(), "Failed: " + response.message(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Product> call, Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void showEditProductsListDialog() {
        ApiService api = ApiClient.getService(requireContext());
        api.getModeratorProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    Toast.makeText(requireContext(), "No products available in catalog.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Product> products = response.body();
                String[] items = new String[products.size()];
                for (int i = 0; i < products.size(); i++) {
                    Product p = products.get(i);
                    boolean active = p.getIsActive() != null && p.getIsActive();
                    items[i] = String.format("%s - ৳%s [%s]%s",
                            p.getName(),
                            p.getEstimatedPrice(),
                            p.getCategory(),
                            active ? "" : " (Out of Stock)");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Select Product to Edit")
                        .setItems(items, (dialog, which) -> {
                            Product selected = products.get(which);
                            showEditSingleProductDialog(selected);
                        })
                        .setNegativeButton("Close", null)
                        .show();
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditSingleProductDialog(Product product) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_product, null);
        EditText etName = dialogView.findViewById(R.id.etEditProductName);
        EditText etPrice = dialogView.findViewById(R.id.etEditProductPrice);
        EditText etImageUrl = dialogView.findViewById(R.id.etEditProductImageUrl);
        EditText etDesc = dialogView.findViewById(R.id.etEditProductDesc);
        CheckBox cbActive = dialogView.findViewById(R.id.cbEditProductActive);

        etName.setText(product.getName());
        etPrice.setText(String.valueOf(product.getEstimatedPrice()));
        if (product.getImageUrl() != null) etImageUrl.setText(product.getImageUrl());
        if (product.getDescription() != null) etDesc.setText(product.getDescription());
        cbActive.setChecked(product.getIsActive() != null && product.getIsActive());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save Changes", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String imgUrl = etImageUrl.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                boolean active = cbActive.isChecked();

                if (name.isEmpty()) {
                    etName.setError("Product name cannot be empty");
                    etName.requestFocus();
                    return;
                }
                if (priceStr.isEmpty()) {
                    etPrice.setError("Price cannot be empty");
                    etPrice.requestFocus();
                    return;
                }

                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                } catch (Exception e) {
                    etPrice.setError("Invalid price format");
                    etPrice.requestFocus();
                    return;
                }

                UpdateProductDto dto = new UpdateProductDto();
                dto.setName(name);
                dto.setEstimatedPrice(price);
                dto.setImageUrl(imgUrl.isEmpty() ? null : imgUrl);
                dto.setDescription(desc.isEmpty() ? null : desc);
                dto.setIsActive(active);

                ApiService api = ApiClient.getService(requireContext());
                api.updateModeratorProduct(product.getId(), dto).enqueue(new Callback<Product>() {
                    @Override
                    public void onResponse(Call<Product> call, Response<Product> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Product updated successfully!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            showEditProductsListDialog();
                        } else {
                            Toast.makeText(requireContext(), "Failed: " + response.message(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Product> call, Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    // Campus Orders Management (Admin & Moderator)
    private void showAllOrdersDialog() {
        ApiService api = ApiClient.getService(requireContext());
        api.getAllCampusOrders().enqueue(new Callback<List<ProductRequest>>() {
            @Override
            public void onResponse(Call<List<ProductRequest>> call, Response<List<ProductRequest>> response) {
                if (!isAdded() || getContext() == null) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    Toast.makeText(requireContext(), "No orders found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<ProductRequest> orders = response.body();
                String[] items = new String[orders.size()];
                for (int i = 0; i < orders.size(); i++) {
                    ProductRequest req = orders.get(i);
                    String buyer = req.getCustomerName() != null ? req.getCustomerName() : ("Customer #" + req.getCustomerId());
                    BigDecimal total = req.getBudget() != null ? req.getBudget().add(req.getDeliveryCharge()) : BigDecimal.ZERO;
                    items[i] = String.format("#%d [%s] ৳%s\n%s • Buyer: %s",
                            req.getId(),
                            req.getStatus(),
                            total,
                            req.getProductName() != null ? req.getProductName() : "Item",
                            buyer);
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("📦 All Campus Orders (" + orders.size() + ")")
                        .setItems(items, (dialog, which) -> {
                            ProductRequest selected = orders.get(which);
                            showSingleOrderDetailsDialog(selected);
                        })
                        .setNegativeButton("Close", null)
                        .show();
            }

            @Override
            public void onFailure(Call<List<ProductRequest>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Error loading orders: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSingleOrderDetailsDialog(ProductRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(req.getId()).append(" - ").append(req.getStatus()).append("\n\n");
        sb.append("🛍️ Item: ").append(req.getProductName() != null ? req.getProductName() : "Custom item")
          .append(" (Qty: ").append(req.getQuantity() != null ? req.getQuantity() : 1).append(")\n");
        sb.append("👤 Buyer: ").append(req.getCustomerName() != null ? req.getCustomerName() : ("Customer #" + req.getCustomerId())).append("\n");
        if (req.getMatchedTripId() != null) {
            sb.append("🚗 Matched Trip: #").append(req.getMatchedTripId()).append("\n");
        }
        if (req.getPickupArea() != null) {
            sb.append("📍 Pickup: ").append(req.getPickupArea()).append("\n");
        }
        if (req.getPreferredShop() != null) {
            sb.append("🏪 Shop: ").append(req.getPreferredShop()).append("\n");
        }
        sb.append("💵 Budget/Price: ৳").append(req.getBudget()).append("\n");
        sb.append("🛵 Delivery Charge: ৳").append(req.getDeliveryCharge()).append("\n");
        BigDecimal total = req.getBudget() != null ? req.getBudget().add(req.getDeliveryCharge()) : BigDecimal.ZERO;
        sb.append("💰 Total: ৳").append(total).append("\n\n");
        if (req.getInstructions() != null && !req.getInstructions().trim().isEmpty()) {
            sb.append("📝 Instructions: ").append(req.getInstructions()).append("\n");
        }
        if (req.getCreatedAt() != null) {
            sb.append("⏰ Placed At: ").append(req.getCreatedAt());
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Order Details #" + req.getId())
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void setupServerEnvironmentToggle() {
        String currentUrl = sessionManager.getBaseUrl();
        if (currentUrl.contains("onrender.com")) {
            rbCloudServer.setChecked(true);
        } else {
            rbLocalServer.setChecked(true);
        }

        rgServerEnv.setOnCheckedChangeListener((group, checkedId) -> {
            String newUrl = checkedId == R.id.rbCloudServer ?
                    SessionManager.URL_RENDER_CLOUD :
                    SessionManager.URL_LOCAL_EMULATOR;

            sessionManager.setBaseUrl(newUrl);
            ApiClient.reset();
            Toast.makeText(requireContext(), "Backend switched to: " + newUrl, Toast.LENGTH_SHORT).show();
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of your CARRY account?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    new com.carry.app.data.firebase.FirebaseAuthGatewayImpl().signOut();
                    sessionManager.logout();
                    CartManager.getInstance().clear();
                    ApiClient.reset();

                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void observeProfileViewModel() {
        if (profileViewModel == null) return;
        profileViewModel.setStateListener(state -> {
            if (!isAdded() || getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (state.profile != null) {
                    if (state.profile.getDisplayName() != null && !state.profile.getDisplayName().trim().isEmpty()) {
                        tvProfileName.setText(state.profile.getDisplayName());
                    }
                    if (state.profile.getPhotoBase64() != null && !state.profile.getPhotoBase64().isEmpty()) {
                        try {
                            byte[] decoded = Base64.decode(state.profile.getPhotoBase64(), Base64.DEFAULT);
                            Glide.with(ProfileFragment.this)
                                    .load(decoded)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_person)
                                    .into(ivProfileAvatar);
                        } catch (Exception ignored) {}
                    }
                }

                if (state.isUploadingPhoto) {
                    pbPhotoUpload.setVisibility(View.VISIBLE);
                    btnChangePhoto.setEnabled(false);
                } else {
                    pbPhotoUpload.setVisibility(View.GONE);
                    btnChangePhoto.setEnabled(true);
                }

                if (state.statusMessage != null) {
                    Toast.makeText(requireContext(), state.statusMessage, Toast.LENGTH_SHORT).show();
                    state.statusMessage = null;
                }
                if (state.errorMessage != null) {
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show();
                    state.errorMessage = null;
                }
            });
        });
    }

    private void showEditDisplayNameDialog() {
        if (getContext() == null || profileViewModel == null) return;
        EditText etName = new EditText(requireContext());
        etName.setText(tvProfileName.getText());
        etName.setSelection(etName.getText().length());
        etName.setHint("Enter display name");

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Display Name")
                .setView(etName)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        profileViewModel.updateDisplayName(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (profileViewModel != null) {
            profileViewModel.removeStateListener();
        }
    }
}
