package com.harsh.covilink;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminLoginActivity extends AppCompatActivity {

    // Admin credentials - you can change these as needed
    private static final String ADMIN_EMAIL = "admin@covilink.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_login);

        // Initialize views
        emailEditText = findViewById(R.id.EmailAddress);
        passwordEditText = findViewById(R.id.Password);
        loginButton = findViewById(R.id.login);
        progressBar = findViewById(R.id.progressBarLogin);
        TextView backButton = findViewById(R.id.back1);

        // Back button functionality
        backButton.setOnClickListener(v -> {
            //vibrate on back button click
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ((android.os.Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                ((android.os.Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(50);
            }
            //finish the activity
            finish();
        });

        // Login button functionality
        loginButton.setOnClickListener(v -> {
            // Vibrate on login button click
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
            
            performAdminLogin();
        });
    }

    private void performAdminLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        // Simulate network delay (you can remove this in production)
        loginButton.postDelayed(() -> {
            // Check admin credentials
            if (ADMIN_EMAIL.equals(email) && ADMIN_PASSWORD.equals(password)) {
                // Login successful
                Toast.makeText(AdminLoginActivity.this, "Admin login successful!", Toast.LENGTH_SHORT).show();
                
                // Navigate to AdminFormActivity
                Intent intent = new Intent(AdminLoginActivity.this, AdminFormActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // Login failed
                Toast.makeText(AdminLoginActivity.this, "Invalid admin credentials!", Toast.LENGTH_LONG).show();
                passwordEditText.setText("");
                passwordEditText.requestFocus();
            }

            // Hide progress
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        }, 1000); // 1 second delay
    }
}