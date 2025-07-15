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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AvailableSlotsActivity extends AppCompatActivity {
    private LinearLayout slotsListLayout;
    private DatabaseReference slotsRef, appointmentsRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_slots);

        slotsListLayout = findViewById(R.id.slotsListLayout);
        slotsRef = FirebaseDatabase.getInstance().getReference("vaccination_slots");
        appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loadSlots();
    }

    private void loadSlots() {
        slotsListLayout.removeAllViews();
        slotsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.hasChildren()) {
                    TextView emptyView = new TextView(AvailableSlotsActivity.this);
                    emptyView.setText("No slots available.");
                    emptyView.setTextColor(getResources().getColor(android.R.color.white));
                    emptyView.setPadding(16, 32, 16, 32);
                    slotsListLayout.addView(emptyView);
                    return;
                }
                for (DataSnapshot slotSnap : snapshot.getChildren()) {
                    String slotId = slotSnap.getKey();
                    String date = slotSnap.child("date").getValue(String.class);
                    String time = slotSnap.child("time").getValue(String.class);
                    String location = slotSnap.child("location").getValue(String.class);
                    int seats = 0;
                    Object seatsObj = slotSnap.child("seats").getValue();
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

                    View slotView = LayoutInflater.from(AvailableSlotsActivity.this).inflate(R.layout.item_available_slot, slotsListLayout, false);
                    TextView slotInfo = slotView.findViewById(R.id.textViewSlotInfo);
                    Button bookButton = slotView.findViewById(R.id.buttonBookSlot);
                    slotInfo.setText(date + " | " + time + " | " + location + " | Seats: " + seats);

                    // Disable button if no seats left
                    if (seats <= 0) {
                        bookButton.setEnabled(false);
                        bookButton.setText("Full");
                    } else {
                        // Check if user already booked this slot
                        checkIfAlreadyBooked(slotId, bookButton);
                        final int finalSeats = seats;
                        bookButton.setOnClickListener(v -> bookSlot(slotId, finalSeats, bookButton));
                    }
                    slotsListLayout.addView(slotView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AvailableSlotsActivity.this, "Failed to load slots", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfAlreadyBooked(String slotId, Button bookButton) {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        appointmentsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String bookedSlotId = appSnap.child("slotId").getValue(String.class);
                            if (slotId.equals(bookedSlotId)) {
                                bookButton.setEnabled(false);
                                bookButton.setText("Booked");
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void bookSlot(String slotId, int seats, Button bookButton) {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        // Prevent double booking
        appointmentsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String bookedSlotId = appSnap.child("slotId").getValue(String.class);
                            if (slotId.equals(bookedSlotId)) {
                                Toast.makeText(AvailableSlotsActivity.this, "Already booked!", Toast.LENGTH_SHORT).show();
                                bookButton.setEnabled(false);
                                bookButton.setText("Booked");
                                return;
                            }
                        }
                        // Book the slot
                        String appointmentId = appointmentsRef.push().getKey();
                        Map<String, Object> appointment = new HashMap<>();
                        appointment.put("userId", userId);
                        appointment.put("slotId", slotId);
                        appointment.put("status", "booked");
                        appointmentsRef.child(appointmentId).setValue(appointment).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Decrease seats
                                slotsRef.child(slotId).child("seats").setValue(seats - 1);
                                Toast.makeText(AvailableSlotsActivity.this, "Appointment booked!", Toast.LENGTH_SHORT).show();
                                bookButton.setEnabled(false);
                                bookButton.setText("Booked");
                            } else {
                                Toast.makeText(AvailableSlotsActivity.this, "Booking failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }
} 