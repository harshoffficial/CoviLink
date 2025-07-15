package com.harsh.covilink;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DetailedFirebaseTestActivity extends AppCompatActivity {

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_test);

        statusText = findViewById(R.id.statusText);
        statusText.setText("Detailed Firebase Test\n\n");

        runDetailedTests();
    }

    private void runDetailedTests() {
        try {
            // Test 1: Check Firebase App
            statusText.append("=== Test 1: Firebase App ===\n");
            FirebaseApp app = FirebaseApp.getInstance();
            statusText.append("✓ Firebase App: " + app.getName() + "\n");
            statusText.append("✓ Project ID: " + app.getOptions().getProjectId() + "\n");
            statusText.append("✓ API Key: " + app.getOptions().getApiKey() + "\n");
            statusText.append("✓ Database URL: " + app.getOptions().getDatabaseUrl() + "\n\n");

            // Test 2: Check Firebase Auth
            statusText.append("=== Test 2: Firebase Auth ===\n");
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                statusText.append("✓ Firebase Auth instance created\n");
                
                // Check current user
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                    statusText.append("✓ Current user: " + currentUser.getEmail() + "\n");
                } else {
                    statusText.append("✓ No current user (expected)\n");
                }
                
                statusText.append("✓ Auth app: " + auth.getApp().getName() + "\n");
                statusText.append("✓ Auth config: " + auth.getApp().getOptions().getProjectId() + "\n\n");
                
            } catch (Exception e) {
                statusText.append("✗ Firebase Auth failed: " + e.getMessage() + "\n");
                Log.e("FirebaseTest", "Auth test failed", e);
                statusText.append("Stack trace: " + Log.getStackTraceString(e) + "\n\n");
            }

            // Test 3: Check Firebase Database
            statusText.append("=== Test 3: Firebase Database ===\n");
            try {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference ref = database.getReference();
                statusText.append("✓ Database instance created\n");
                statusText.append("✓ Database URL: " + database.getReference().toString() + "\n");
                statusText.append("✓ Database app: " + database.getApp().getName() + "\n\n");
                
            } catch (Exception e) {
                statusText.append("✗ Firebase Database failed: " + e.getMessage() + "\n");
                Log.e("FirebaseTest", "Database test failed", e);
                statusText.append("Stack trace: " + Log.getStackTraceString(e) + "\n\n");
            }

            // Test 4: Try Auth Operation
            statusText.append("=== Test 4: Auth Operation Test ===\n");
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                
                // Try to sign in with invalid credentials (this should fail but not crash)
                auth.signInWithEmailAndPassword("test@invalid.com", "wrongpassword")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            statusText.append("✗ Unexpected: Sign in succeeded with invalid credentials\n");
                        } else {
                            statusText.append("✓ Expected: Sign in failed with invalid credentials\n");
                            statusText.append("✓ Error: " + task.getException().getMessage() + "\n");
                            statusText.append("✓ Auth operation test completed successfully\n\n");
                        }
                    })
                    .addOnFailureListener(e -> {
                        statusText.append("✗ Auth operation failed: " + e.getMessage() + "\n");
                        Log.e("FirebaseTest", "Auth operation failed", e);
                    });
                    
            } catch (Exception e) {
                statusText.append("✗ Auth operation test failed: " + e.getMessage() + "\n");
                Log.e("FirebaseTest", "Auth operation test failed", e);
                statusText.append("Stack trace: " + Log.getStackTraceString(e) + "\n\n");
            }

        } catch (Exception e) {
            statusText.append("✗ Overall test failed: " + e.getMessage() + "\n");
            Log.e("FirebaseTest", "Overall test failed", e);
            statusText.append("Stack trace: " + Log.getStackTraceString(e) + "\n");
        }
    }
} 