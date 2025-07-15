package com.harsh.covilink;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminFormActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private TextView welcomeText;
    private TextView totalUsersText;
    private TextView vaccinatedUsersText;
    private TextView pendingAppointmentsText;
    private Button manageSlotsButton;
    private Button userRecordsButton;
    private Button appointmentManagementButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_form);

        // Initialize views
        welcomeText = findViewById(R.id.get_started);
        totalUsersText = findViewById(R.id.total_users);
        vaccinatedUsersText = findViewById(R.id.vaccinated_users);
        pendingAppointmentsText = findViewById(R.id.pending_appointments);
        manageSlotsButton = findViewById(R.id.manage_slots_button);
        userRecordsButton = findViewById(R.id.user_records_button);
        appointmentManagementButton = findViewById(R.id.appointment_management_button);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Set up navigation drawer
        setupNavigationDrawer();
        
        // Set up dashboard buttons
        setupDashboardButtons();
        
        // Load admin dashboard statistics
        loadDashboardStatistics();
        
        // Update welcome message
        updateWelcomeMessage();
    }
    
    private void setupNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.main);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ImageButton menuButton = findViewById(R.id.menu_button);

        menuButton.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END);
            }
        });

        // Set up navigation item selected listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END);
            
            if (id == R.id.nav_admin_dashboard) {
                // Already on dashboard
                return true;
            } else if (id == R.id.nav_manage_slots) {
                // Navigate to manage slots
                Toast.makeText(this, "Manage Vaccination Slots - Coming Soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_user_records) {
                // Navigate to user records
                Toast.makeText(this, "User Records - Coming Soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_appointment_management) {
                // Navigate to appointment management
                Toast.makeText(this, "Appointment Management - Coming Soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_admin_profile) {
                // Navigate to admin profile
                Toast.makeText(this, "Admin Profile - Coming Soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_logout) {
                // Logout item clicked
                showLogoutDialog();
                return true;
            }
            return false;
        });
    }
    
    private void setupDashboardButtons() {
        manageSlotsButton.setOnClickListener(v -> {
            // Vibrate on button click
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
            // Open ManageSlotsActivity
            Intent intent = new Intent(AdminFormActivity.this, ManageSlotsActivity.class);
            startActivity(intent);
        });
        
        userRecordsButton.setOnClickListener(v -> {
            // Vibrate on button click
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
            // Open UserRecordsActivity
            Intent intent = new Intent(AdminFormActivity.this, UserRecordsActivity.class);
            startActivity(intent);
        });
        
        appointmentManagementButton.setOnClickListener(v -> {
            // Vibrate on button click
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
            // Open AppointmentManagementActivity
            Intent intent = new Intent(AdminFormActivity.this, AppointmentManagementActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadDashboardStatistics() {
        // Load total users count
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalUsers = (int) dataSnapshot.getChildrenCount();
                int vaccinatedUsers = 0;
                
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    if (userSnapshot.hasChild("vaccination")) {
                        DataSnapshot vaccinationSnapshot = userSnapshot.child("vaccination");
                        if (vaccinationSnapshot.hasChild("isFullyVaccinated")) {
                            Boolean isFullyVaccinated = vaccinationSnapshot.child("isFullyVaccinated").getValue(Boolean.class);
                            if (isFullyVaccinated != null && isFullyVaccinated) {
                                vaccinatedUsers++;
                            }
                        }
                    }
                }
                
                updateStatistics(totalUsers, vaccinatedUsers, 0); // 0 for pending appointments for now
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminFormActivity.this, "Failed to load statistics", Toast.LENGTH_SHORT).show();
                updateStatistics(0, 0, 0);
            }
        });
    }
    
    private void updateStatistics(int totalUsers, int vaccinatedUsers, int pendingAppointments) {
        if (totalUsersText != null) {
            totalUsersText.setText("Total Users: " + totalUsers);
        }
        
        if (vaccinatedUsersText != null) {
            vaccinatedUsersText.setText("Fully Vaccinated: " + vaccinatedUsers);
        }
        
        if (pendingAppointmentsText != null) {
            pendingAppointmentsText.setText("Pending Appointments: " + pendingAppointments);
        }
    }
    
    private void updateWelcomeMessage() {
        if (welcomeText != null) {
            welcomeText.setText("Welcome back!\nAdmin");
        }
    }

    private void showLogoutDialog() {
        // Shows a confirmation dialog to the user before logging out.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    logout();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void logout() {
        // Navigate back to MainActivity
        Intent intent = new Intent(AdminFormActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}