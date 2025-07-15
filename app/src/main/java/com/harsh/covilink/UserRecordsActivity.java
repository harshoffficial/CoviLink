package com.harsh.covilink;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserRecordsActivity extends AppCompatActivity {
    private EditText emailEditText;
    private Button searchButton;
    private LinearLayout userInfoLayout;
    private LinearLayout userAppointmentsLayout;
    private DatabaseReference usersRef, appointmentsRef, slotsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_records);

        emailEditText = findViewById(R.id.editTextUserEmail);
        searchButton = findViewById(R.id.buttonSearchUser);
        userInfoLayout = findViewById(R.id.userInfoLayout);
        userAppointmentsLayout = findViewById(R.id.userAppointmentsLayout);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        slotsRef = FirebaseDatabase.getInstance().getReference("vaccination_slots");

        searchButton.setOnClickListener(v -> searchUser());
    }

    private void searchUser() {
        userInfoLayout.removeAllViews();
        userAppointmentsLayout.removeAllViews();
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter user email", Toast.LENGTH_SHORT).show();
            return;
        }
        usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.hasChildren()) {
                            Toast.makeText(UserRecordsActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String userId = userSnap.getKey();
                            String username = userSnap.child("username").getValue(String.class);
                            String userEmail = userSnap.child("email").getValue(String.class);
                            int dosesTaken = 0;
                            boolean isFullyVaccinated = false;
                            if (userSnap.child("vaccination/dosesTaken").getValue() != null) {
                                try {
                                    Object dosesObj = userSnap.child("vaccination/dosesTaken").getValue();
                                    if (dosesObj instanceof Long) {
                                        dosesTaken = ((Long) dosesObj).intValue();
                                    } else if (dosesObj instanceof Integer) {
                                        dosesTaken = (Integer) dosesObj;
                                    } else {
                                        dosesTaken = Integer.parseInt(dosesObj.toString());
                                    }
                                } catch (Exception e) { dosesTaken = 0; }
                            }
                            if (userSnap.child("vaccination/isFullyVaccinated").getValue() != null) {
                                isFullyVaccinated = Boolean.TRUE.equals(userSnap.child("vaccination/isFullyVaccinated").getValue(Boolean.class));
                            }
                            showUserInfo(username, userEmail, dosesTaken, isFullyVaccinated);
                            loadUserAppointments(userId);
                            break;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserRecordsActivity.this, "Failed to search user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showUserInfo(String username, String email, int doses, boolean fullyVaccinated) {
        View infoView = LayoutInflater.from(this).inflate(R.layout.item_user_info, userInfoLayout, false);
        TextView nameView = infoView.findViewById(R.id.textViewUserName);
        TextView emailView = infoView.findViewById(R.id.textViewUserEmail);
        TextView statusView = infoView.findViewById(R.id.textViewVaccinationStatus);
        nameView.setText("Name: " + username);
        emailView.setText("Email: " + email);
        statusView.setText("Doses: " + doses + (fullyVaccinated ? " (Fully Vaccinated)" : " (Not Fully Vaccinated)"));
        userInfoLayout.addView(infoView);
    }

    private void loadUserAppointments(String userId) {
        appointmentsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.hasChildren()) {
                            TextView emptyView = new TextView(UserRecordsActivity.this);
                            emptyView.setText("No appointments found.");
                            emptyView.setTextColor(getResources().getColor(android.R.color.white));
                            emptyView.setPadding(16, 32, 16, 32);
                            userAppointmentsLayout.addView(emptyView);
                            return;
                        }
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String slotId = appSnap.child("slotId").getValue(String.class);
                            String status = appSnap.child("status").getValue(String.class);
                            loadSlotDetails(slotId, status);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void loadSlotDetails(String slotId, String status) {
        slotsRef.child(slotId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot slotSnap) {
                String date = slotSnap.child("date").getValue(String.class);
                String time = slotSnap.child("time").getValue(String.class);
                String location = slotSnap.child("location").getValue(String.class);
                View appView = LayoutInflater.from(UserRecordsActivity.this).inflate(R.layout.item_appointment, userAppointmentsLayout, false);
                TextView appInfo = appView.findViewById(R.id.textViewAppointmentInfo);
                TextView appStatus = appView.findViewById(R.id.textViewAppointmentStatus);
                appInfo.setText(date + " | " + time + " | " + location);
                appStatus.setText("Status: " + status);
                userAppointmentsLayout.addView(appView);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
} 