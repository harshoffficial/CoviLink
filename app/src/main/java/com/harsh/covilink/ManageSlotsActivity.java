package com.harsh.covilink;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.HashMap;
import java.util.Map;

public class ManageSlotsActivity extends AppCompatActivity {
    private EditText dateEditText, timeEditText, locationEditText, seatsEditText;
    private Button addSlotButton;
    private LinearLayout slotsListLayout;
    private DatabaseReference slotsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_slots);

        dateEditText = findViewById(R.id.editTextDate);
        timeEditText = findViewById(R.id.editTextTime);
        locationEditText = findViewById(R.id.editTextLocation);
        seatsEditText = findViewById(R.id.editTextSeats);
        addSlotButton = findViewById(R.id.buttonAddSlot);
        slotsListLayout = findViewById(R.id.slotsListLayout);

        slotsRef = FirebaseDatabase.getInstance().getReference("vaccination_slots");

        addSlotButton.setOnClickListener(v -> addSlot());

        loadSlots();
    }

    private void addSlot() {
        String date = dateEditText.getText().toString().trim();
        String time = timeEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String seatsStr = seatsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time) || TextUtils.isEmpty(location) || TextUtils.isEmpty(seatsStr)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }
        int seats;
        try {
            seats = Integer.parseInt(seatsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Seats must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        String slotId = slotsRef.push().getKey();
        Map<String, Object> slot = new HashMap<>();
        slot.put("date", date);
        slot.put("time", time);
        slot.put("location", location);
        slot.put("seats", seats);
        slotsRef.child(slotId).setValue(slot).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Slot added", Toast.LENGTH_SHORT).show();
                dateEditText.setText("");
                timeEditText.setText("");
                locationEditText.setText("");
                seatsEditText.setText("");
                loadSlots();
            } else {
                Toast.makeText(this, "Failed to add slot", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSlots() {
        slotsListLayout.removeAllViews();
        slotsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot slotSnap : snapshot.getChildren()) {
                    String slotId = slotSnap.getKey();
                    String date = slotSnap.child("date").getValue(String.class);
                    String time = slotSnap.child("time").getValue(String.class);
                    String location = slotSnap.child("location").getValue(String.class);
                    int seats = slotSnap.child("seats").getValue(Integer.class);

                    View slotView = LayoutInflater.from(ManageSlotsActivity.this).inflate(R.layout.item_slot, slotsListLayout, false);
                    TextView slotInfo = slotView.findViewById(R.id.textViewSlotInfo);
                    Button deleteButton = slotView.findViewById(R.id.buttonDeleteSlot);
                    slotInfo.setText(date + " | " + time + " | " + location + " | Seats: " + seats);
                    deleteButton.setOnClickListener(v -> deleteSlot(slotId));
                    slotsListLayout.addView(slotView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageSlotsActivity.this, "Failed to load slots", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSlot(String slotId) {
        slotsRef.child(slotId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Slot deleted", Toast.LENGTH_SHORT).show();
                loadSlots();
            } else {
                Toast.makeText(this, "Failed to delete slot", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 