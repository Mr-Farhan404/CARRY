package com.carry.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.carry.app.MainActivity;
import com.carry.app.R;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.AuthResponse;
import com.carry.app.models.RegisterRequest;
import com.carry.app.models.User;
import com.carry.app.models.UserRole;
import com.carry.app.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etStudentId;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etDepartment;
    private TextInputEditText etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvGoToLogin;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = SessionManager.getInstance(this);

        etFullName = findViewById(R.id.etFullName);
        etStudentId = findViewById(R.id.etStudentId);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> performRegister());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void performRegister() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String studentId = etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String dept = etDepartment.getText() != null ? etDepartment.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (studentId.isEmpty()) {
            etStudentId.setError("Student ID is required");
            etStudentId.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        RadioGroup rgAccountType = findViewById(R.id.rgAccountType);
        UserRole selectedRole = (rgAccountType != null && rgAccountType.getCheckedRadioButtonId() == R.id.rbTeacher)
                ? UserRole.TEACHER : UserRole.STUDENT;

        RegisterRequest request = new RegisterRequest(name, studentId, email, phone, dept, password, selectedRole);

        AuthCoordinator coordinator = new AuthCoordinator(
                ApiClient.getService(this),
                new com.carry.app.data.firebase.FirebaseAuthGatewayImpl(),
                sessionManager
        );

        coordinator.performRegister(request, new AuthCoordinator.AuthResultCallback() {
            @Override
            public void onSuccess(User user, String firebaseUid, String message) {
                btnRegister.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                btnRegister.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
