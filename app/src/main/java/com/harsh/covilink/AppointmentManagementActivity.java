package com.harsh.covilink;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

public class AppointmentManagementActivity extends AppCompatActivity {
    private LinearLayout appointmentsListLayout;
    private DatabaseReference appointmentsRef, slotsRef, usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_management);

        appointmentsListLayout = findViewById(R.id.appointmentsListLayout);
        appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        slotsRef = FirebaseDatabase.getInstance().getReference("vaccination_slots");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadAllAppointments();
    }

    private void loadAllAppointments() {
        appointmentsListLayout.removeAllViews();
        appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.hasChildren()) {
                    TextView emptyView = new TextView(AppointmentManagementActivity.this);
                    emptyView.setText("No appointments found.");
                    emptyView.setTextColor(getResources().getColor(android.R.color.white));
                    emptyView.setPadding(16, 32, 16, 32);
                    appointmentsListLayout.addView(emptyView);
                    return;
                }
                for (DataSnapshot appSnap : snapshot.getChildren()) {
                    String appointmentId = appSnap.getKey();
                    String userId = appSnap.child("userId").getValue(String.class);
                    String slotId = appSnap.child("slotId").getValue(String.class);
                    String status = appSnap.child("status").getValue(String.class);
                    loadUserAndSlotDetails(appointmentId, userId, slotId, status);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AppointmentManagementActivity.this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserAndSlotDetails(String appointmentId, String userId, String slotId, String status) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                String username = userSnap.child("username").getValue(String.class);
                String userEmail = userSnap.child("email").getValue(String.class);
                slotsRef.child(slotId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot slotSnap) {
                        String date = slotSnap.child("date").getValue(String.class);
                        String time = slotSnap.child("time").getValue(String.class);
                        String location = slotSnap.child("location").getValue(String.class);
                        View appView = LayoutInflater.from(AppointmentManagementActivity.this).inflate(R.layout.item_admin_appointment, appointmentsListLayout, false);
                        TextView userInfo = appView.findViewById(R.id.textViewAdminAppointmentUser);
                        TextView slotInfo = appView.findViewById(R.id.textViewAdminAppointmentSlot);
                        TextView appStatus = appView.findViewById(R.id.textViewAdminAppointmentStatus);
                        Button completedButton = appView.findViewById(R.id.buttonMarkCompleted);
                        Button missedButton = appView.findViewById(R.id.buttonMarkMissed);
                        userInfo.setText("User: " + username + " (" + userEmail + ")");
                        slotInfo.setText("Slot: " + date + " | " + time + " | " + location);
                        appStatus.setText("Status: " + status);
                        // Disable buttons if already marked
                        if ("completed".equalsIgnoreCase(status) || "missed".equalsIgnoreCase(status)) {
                            completedButton.setEnabled(false);
                            missedButton.setEnabled(false);
                        }
                        completedButton.setOnClickListener(v -> markAppointmentStatus(appointmentId, "completed", appStatus, completedButton, missedButton));
                        missedButton.setOnClickListener(v -> markAppointmentStatus(appointmentId, "missed", appStatus, completedButton, missedButton));
                        appointmentsListLayout.addView(appView);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void markAppointmentStatus(String appointmentId, String newStatus, TextView appStatus, Button completedButton, Button missedButton) {
        appointmentsRef.child(appointmentId).child("status").setValue(newStatus).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                appStatus.setText("Status: " + newStatus);
                completedButton.setEnabled(false);
                missedButton.setEnabled(false);
                Toast.makeText(this, "Marked as " + newStatus, Toast.LENGTH_SHORT).show();
                // If completed, increment user's doses
                if ("completed".equalsIgnoreCase(newStatus)) {
                    appointmentsRef.child(appointmentId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot appSnap) {
                            String userId = appSnap.child("userId").getValue(String.class);
                            if (userId != null) {
                                DatabaseReference userVaccRef = usersRef.child(userId).child("vaccination");
                                userVaccRef.child("dosesTaken").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot doseSnap) {
                                        int doses = 0;
                                        Object dosesObj = doseSnap.getValue();
                                        if (dosesObj != null) {
                                            try {
                                                if (dosesObj instanceof Long) {
                                                    doses = ((Long) dosesObj).intValue();
                                                } else if (dosesObj instanceof Integer) {
                                                    doses = (Integer) dosesObj;
                                                } else {
                                                    doses = Integer.parseInt(dosesObj.toString());
                                                }
                                            } catch (Exception e) { doses = 0; }
                                        }
                                        doses++;
                                        userVaccRef.child("dosesTaken").setValue(doses);
                                        if (doses >= 2) {
                                            userVaccRef.child("isFullyVaccinated").setValue(true);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) { }
                                });
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });
                }
            } else {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 