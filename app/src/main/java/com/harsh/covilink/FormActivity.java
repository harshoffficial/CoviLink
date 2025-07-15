package com.harsh.covilink;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class FormActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView welcomeText;
    private TextView vaccinationStatusText;
    private TextView dosesTakenText;
    private Button viewSlotsButton;
    private Button myAppointmentsButton;
    private Button certificatesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Initialize views
        welcomeText = findViewById(R.id.get_started);
        vaccinationStatusText = findViewById(R.id.vaccination_status);
        dosesTakenText = findViewById(R.id.doses_taken);
        viewSlotsButton = findViewById(R.id.view_slots_button);
        myAppointmentsButton = findViewById(R.id.my_appointments_button);
        certificatesButton = findViewById(R.id.certificates_button);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to login
            Intent intent = new Intent(FormActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // User is logged in, load user data from Firebase Database
        String userEmail = currentUser.getEmail();
        String userId = currentUser.getUid();
        
        // Load user data and update drawer header
        loadUserDataAndUpdateHeader(userId);
        
        // Set up navigation drawer
        setupNavigationDrawer();
        
        // Set up dashboard buttons
        setupDashboardButtons();
        
        // Load user vaccination data
        loadUserVaccinationData(userId);
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
            
            if (id == R.id.nav_home) {
                // Already on dashboard
                return true;
            } else if (id == R.id.nav_available_slots) {
                // Navigate to available slots
                Toast.makeText(this, "Available Slots - Coming Soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_my_appointments) {
                // Navigate to my appointments
                Toast.makeText(this, "My Appointments - Coming Soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_certificates) {
                // Navigate to certificates
                Toast.makeText(this, "Vaccination Certificates - Coming Soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_gallery) {
                // Navigate to update profile
                Toast.makeText(this, "Update Profile - Coming Soon!", Toast.LENGTH_SHORT).show();
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
        viewSlotsButton.setOnClickListener(v -> {
            // Open AvailableSlotsActivity
            Intent intent = new Intent(FormActivity.this, AvailableSlotsActivity.class);
            startActivity(intent);
        });
        
        myAppointmentsButton.setOnClickListener(v -> {
            // Open MyAppointmentsActivity
            Intent intent = new Intent(FormActivity.this, MyAppointmentsActivity.class);
            startActivity(intent);
        });
        
        certificatesButton.setOnClickListener(v -> {
            Toast.makeText(this, "Vaccination Certificates - Coming Soon!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadUserVaccinationData(String userId) {
        mDatabase.child("users").child(userId).child("vaccination").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User has vaccination data
                    int dosesTaken = 0;
                    boolean isFullyVaccinated = false;
                    Object dosesObj = dataSnapshot.child("dosesTaken").getValue();
                    if (dosesObj != null) {
                        try {
                            if (dosesObj instanceof Long) {
                                dosesTaken = ((Long) dosesObj).intValue();
                            } else if (dosesObj instanceof Integer) {
                                dosesTaken = (Integer) dosesObj;
                            } else {
                                dosesTaken = Integer.parseInt(dosesObj.toString());
                            }
                        } catch (Exception e) { dosesTaken = 0; }
                    }
                    if (dataSnapshot.hasChild("isFullyVaccinated")) {
                        isFullyVaccinated = dataSnapshot.child("isFullyVaccinated").getValue(Boolean.class);
                    }
                    updateVaccinationStatus(dosesTaken, isFullyVaccinated);
                    // Show/hide certificate button
                    if (certificatesButton != null) {
                        certificatesButton.setVisibility(isFullyVaccinated ? View.VISIBLE : View.GONE);
                        certificatesButton.setOnClickListener(v -> {
                            Intent intent = new Intent(FormActivity.this, CertificateActivity.class);
                            startActivity(intent);
                        });
                    }
                } else {
                    // User has no vaccination data
                    updateVaccinationStatus(0, false);
                    if (certificatesButton != null) {
                        certificatesButton.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(FormActivity.this, "Failed to load vaccination data", Toast.LENGTH_SHORT).show();
                updateVaccinationStatus(0, false);
            }
        });
    }
    
    private void updateVaccinationStatus(int dosesTaken, boolean isFullyVaccinated) {
        if (vaccinationStatusText != null) {
            if (isFullyVaccinated) {
                vaccinationStatusText.setText("✅ Fully Vaccinated");
                vaccinationStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else if (dosesTaken > 0) {
                vaccinationStatusText.setText("🔄 Partially Vaccinated");
                vaccinationStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                vaccinationStatusText.setText("❌ Not Vaccinated");
                vaccinationStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        }
        
        if (dosesTakenText != null) {
            dosesTakenText.setText("Doses Taken: " + dosesTaken + "/2");
        }
    }
    
    private void loadUserDataAndUpdateHeader(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        updateDrawerHeader(user);
                        updateWelcomeMessage(user.getUsername());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
                Toast.makeText(FormActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateWelcomeMessage(String username) {
        if (welcomeText != null) {
            welcomeText.setText("Welcome!!!! " + username);
        }
    }
    
    private void updateDrawerHeader(User user) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        
        // Update profile image
        ImageView profileImage = headerView.findViewById(R.id.profile_image);
        TextView userName = headerView.findViewById(R.id.user_name);
        TextView userEmail = headerView.findViewById(R.id.user_email);
        
        // Set user name and email
        userName.setText(user.getUsername());
        userEmail.setText(user.getEmail());
        
        // Load profile image if available
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Picasso.get()
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.profile_pic)
                    .error(R.drawable.profile_pic)
                    .into(profileImage);
        } else {
            // Set default profile image
            profileImage.setImageResource(R.drawable.profile_pic);
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
        // Signs out the user from Firebase and navigates back to the LoginActivity.
        // Sign out from Firebase
        mAuth.signOut();

        // Navigate back to LoginActivity
        Intent intent = new Intent(FormActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}