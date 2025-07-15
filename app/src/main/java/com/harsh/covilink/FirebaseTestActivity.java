package com.harsh.covilink;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseTestActivity extends AppCompatActivity {

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_test);

        statusText = findViewById(R.id.statusText);

        testFirebaseConnection();
    }

    private void testFirebaseConnection() {
        try {
            // Test 1: Check if Firebase is initialized
            statusText.append("Testing Firebase initialization...\n");
            FirebaseApp app = FirebaseApp.getInstance();
            statusText.append("✓ Firebase App initialized: " + app.getName() + "\n");

            // Test 2: Check Firebase Auth
            statusText.append("Testing Firebase Auth...\n");
            FirebaseAuth auth = FirebaseAuth.getInstance();
            statusText.append("✓ Firebase Auth initialized\n");

            // Test 3: Check Firebase Database
            statusText.append("Testing Firebase Database...\n");
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference();
            statusText.append("✓ Firebase Database initialized\n");
            statusText.append("Database URL: " + database.getReference().toString() + "\n");

            // Test 4: Try to write a test value
            statusText.append("Testing database write...\n");
            ref.child("test").child("connection").setValue("success")
                    .addOnSuccessListener(aVoid -> {
                        statusText.append("✓ Database write successful\n");
                        Toast.makeText(this, "Firebase connection successful!", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        statusText.append("✗ Database write failed: " + e.getMessage() + "\n");
                        statusText.append("This usually means the Realtime Database hasn't been created yet.\n");
                        statusText.append("Please create the database in Firebase Console.\n");
                        Toast.makeText(this, "Database not created yet. Please set up in Firebase Console.", Toast.LENGTH_LONG).show();
                    });

            // Test 5: Test Authentication (this should work even without database)
            statusText.append("\nTesting Firebase Auth...\n");
            try {
                // Just check if we can get the auth instance
                FirebaseAuth testAuth = FirebaseAuth.getInstance();
                statusText.append("✓ Firebase Auth is working\n");
                statusText.append("Auth instance: " + testAuth.toString() + "\n");
            } catch (Exception e) {
                statusText.append("✗ Firebase Auth failed: " + e.getMessage() + "\n");
            }

        } catch (Exception e) {
            statusText.append("✗ Firebase test failed: " + e.getMessage() + "\n");
            Log.e("FirebaseTest", "Firebase test failed", e);
            Toast.makeText(this, "Firebase test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 