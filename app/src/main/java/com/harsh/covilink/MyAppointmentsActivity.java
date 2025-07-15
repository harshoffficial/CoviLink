package com.harsh.covilink;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyAppointmentsActivity extends AppCompatActivity {
    private LinearLayout appointmentsListLayout;
    private DatabaseReference appointmentsRef, slotsRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);

        appointmentsListLayout = findViewById(R.id.appointmentsListLayout);
        appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        slotsRef = FirebaseDatabase.getInstance().getReference("vaccination_slots");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loadAppointments();
    }

    private void loadAppointments() {
        appointmentsListLayout.removeAllViews();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        appointmentsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.hasChildren()) {
                            TextView emptyView = new TextView(MyAppointmentsActivity.this);
                            emptyView.setText("No appointments found.");
                            emptyView.setTextColor(getResources().getColor(android.R.color.white));
                            emptyView.setPadding(16, 32, 16, 32);
                            appointmentsListLayout.addView(emptyView);
                            return;
                        }
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String appointmentId = appSnap.getKey();
                            String slotId = appSnap.child("slotId").getValue(String.class);
                            String status = appSnap.child("status").getValue(String.class);
                            loadSlotDetails(slotId, status, appointmentId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyAppointmentsActivity.this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSlotDetails(String slotId, String status, String appointmentId) {
        slotsRef.child(slotId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot slotSnap) {
                String date = slotSnap.child("date").getValue(String.class);
                String time = slotSnap.child("time").getValue(String.class);
                String location = slotSnap.child("location").getValue(String.class);
                View appView = LayoutInflater.from(MyAppointmentsActivity.this).inflate(R.layout.item_appointment, appointmentsListLayout, false);
                TextView appInfo = appView.findViewById(R.id.textViewAppointmentInfo);
                TextView appStatus = appView.findViewById(R.id.textViewAppointmentStatus);
                Button cancelButton = appView.findViewById(R.id.buttonCancelAppointment);
                appInfo.setText(date + " | " + time + " | " + location);
                appStatus.setText("Status: " + status);
                cancelButton.setOnClickListener(v -> cancelAppointment(appointmentId, slotId, cancelButton));
                appointmentsListLayout.addView(appView);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void cancelAppointment(String appointmentId, String slotId, Button cancelButton) {
        // Remove appointment
        appointmentsRef.child(appointmentId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Increase slot seats by 1
                slotsRef.child(slotId).child("seats").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int seats = 0;
                        Object seatsObj = snapshot.getValue();
                        if (seatsObj != null) {
                            try {
                                if (seatsObj instanceof Long) {
                                    seats = ((Long) seatsObj).intValue();
                                } else if (seatsObj instanceof Integer) {
                                    seats = (Integer) seatsObj;
                                } else if (seatsObj instanceof Double) {
                                    seats = ((Double) seatsObj).intValue();
                                } else {
                                    seats = Integer.parseInt(seatsObj.toString());
                                }
                            } catch (Exception e) {
                                seats = 0;
                            }
                        }
                        slotsRef.child(slotId).child("seats").setValue(seats + 1);
                        Toast.makeText(MyAppointmentsActivity.this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                        cancelButton.setEnabled(false);
                        cancelButton.setText("Cancelled");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            } else {
                Toast.makeText(MyAppointmentsActivity.this, "Failed to cancel", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 