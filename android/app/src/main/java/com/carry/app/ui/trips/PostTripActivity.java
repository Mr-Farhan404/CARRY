package com.carry.app.ui.trips;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.carry.app.R;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.CreateTripRequest;
import com.carry.app.models.LocationArea;
import com.carry.app.models.Trip;
import com.carry.app.models.User;
import com.carry.app.models.UserRole;
import com.carry.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostTripActivity extends AppCompatActivity {

    private Toolbar postTripToolbar;
    private Spinner spinnerTripDestination;
    private TextInputEditText etDepartureTime;
    private TextInputEditText etReturnTime;
    private TextInputEditText etCapacityNotes;
    private MaterialButton btnPostTripSubmit;
    private ProgressBar progressBarPostTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_trip);

        postTripToolbar = findViewById(R.id.postTripToolbar);
        spinnerTripDestination = findViewById(R.id.spinnerTripDestination);
        etDepartureTime = findViewById(R.id.etDepartureTime);
        etReturnTime = findViewById(R.id.etReturnTime);
        etCapacityNotes = findViewById(R.id.etCapacityNotes);
        btnPostTripSubmit = findViewById(R.id.btnPostTripSubmit);
        progressBarPostTrip = findViewById(R.id.progressBarPostTrip);

        setupToolbar();
        setupSpinners();
        prefillTimestamps();

        btnPostTripSubmit.setOnClickListener(v -> submitTrip());
    }

    private void setupToolbar() {
        setSupportActionBar(postTripToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        postTripToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        LocationArea[] areas = LocationArea.values();
        String[] areaNames = new String[areas.length];
        for (int i = 0; i < areas.length; i++) {
            areaNames[i] = areas[i].getDisplayName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, areaNames);
        spinnerTripDestination.setAdapter(adapter);
    }

    private void prefillTimestamps() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        Calendar now = Calendar.getInstance();
        etDepartureTime.setText(sdf.format(now.getTime()));

        now.add(Calendar.HOUR_OF_DAY, 4);
        etReturnTime.setText(sdf.format(now.getTime()));
    }

    private void submitTrip() {
        User user = SessionManager.getInstance(this).getUser();
        UserRole role = user != null ? user.getRole() : UserRole.STUDENT;
        if (role != UserRole.DELIVERY_PARTNER && role != UserRole.ADMIN) {
            Toast.makeText(this, "Only approved Delivery Partners can post trips.", Toast.LENGTH_LONG).show();
            return;
        }

        String departure = etDepartureTime.getText() != null ? etDepartureTime.getText().toString().trim() : "";
        String returnTime = etReturnTime.getText() != null ? etReturnTime.getText().toString().trim() : "";
        String notes = etCapacityNotes.getText() != null ? etCapacityNotes.getText().toString().trim() : "";

        if (departure.isEmpty()) {
            etDepartureTime.setError("Departure time is required");
            return;
        }

        if (returnTime.isEmpty()) {
            etReturnTime.setError("Return time is required");
            return;
        }

        btnPostTripSubmit.setEnabled(false);
        progressBarPostTrip.setVisibility(View.VISIBLE);

        LocationArea selectedArea = LocationArea.values()[spinnerTripDestination.getSelectedItemPosition()];
        CreateTripRequest request = new CreateTripRequest(departure, returnTime, selectedArea, notes);

        ApiService api = ApiClient.getService(this);
        api.createTrip(request).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                btnPostTripSubmit.setEnabled(true);
                progressBarPostTrip.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Trip trip = response.body();
                    SessionManager.getInstance(PostTripActivity.this).setActiveTripId(trip.getId());
                    Toast.makeText(PostTripActivity.this, "Trip posted! You can now accept deliveries.", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(PostTripActivity.this, "Failed to post trip", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Trip> call, Throwable t) {
                btnPostTripSubmit.setEnabled(true);
                progressBarPostTrip.setVisibility(View.GONE);
                Toast.makeText(PostTripActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
