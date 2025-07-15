package com.harsh.covilink;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SimpleSignupActivity extends AppCompatActivity {
    
    private EditText userNameEditText, emailEditText, passwordEditText;
    private Button signupButton;
    private ProgressBar progressBar;
    private TextView backButton;
    private ImageView profileImageView;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Cloudinary cloudinary;
    
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private Uri selectedImageUri;
    private String profileImageUrl = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_activity_signup);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize Cloudinary
        cloudinary = CloudinaryConfig.getCloudinary();

        // Initialize views
        userNameEditText = findViewById(R.id.UserName);
        emailEditText = findViewById(R.id.EmailAddress);
        passwordEditText = findViewById(R.id.Password);
        signupButton = findViewById(R.id.signUp);
        progressBar = findViewById(R.id.progressBarSignUp);
        backButton = findViewById(R.id.back1);
        profileImageView = findViewById(R.id.profile);

        View.OnClickListener vibrateListener = v -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        };

        signupButton.setOnClickListener(v -> {
            vibrateListener.onClick(v);
            performSignup();
        });

        backButton.setOnClickListener(v -> {
            vibrateListener.onClick(v);
            Intent intent = new Intent(SimpleSignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
        
        // Set up profile image click listener
        profileImageView.setOnClickListener(v -> {
            vibrateListener.onClick(v);
            // Try simple permission check first
            if (checkBasicPermissions()) {
                showImagePickerDialog();
            } else {
                checkPermissionsAndShowImagePicker();
            }
        });
    }
    
    private void performSignup() {
        String username = userNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Validate input
        if (username.isEmpty()) {
            userNameEditText.setError("Username is required");
            userNameEditText.requestFocus();
            return;
        }
        
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }
        
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }
        
        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);
        
        // If profile image is selected, upload it first
        if (selectedImageUri != null) {
            uploadImageToCloudinary(selectedImageUri, new OnImageUploadListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    profileImageUrl = imageUrl;
                    createUserAccount(username, email, password);
                }
                
                @Override
                public void onError(String error) {
                    progressBar.setVisibility(View.GONE);
                    signupButton.setEnabled(true);
                    Toast.makeText(SimpleSignupActivity.this, 
                            "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // No image selected, create account directly
            createUserAccount(username, email, password);
        }
    }
    
    private void createUserAccount(String username, String email, String password) {
        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success, save user data to database
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserDataToDatabase(user.getUid(), username, email);
                            }
                        } else {
                            // Sign up failed
                            progressBar.setVisibility(View.GONE);
                            signupButton.setEnabled(true);
                            String errorMessage = task.getException() != null ? 
                                    task.getException().getMessage() : "Signup failed";
                            

                            
                            Toast.makeText(SimpleSignupActivity.this, errorMessage, 
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    
    private void saveUserDataToDatabase(String userId, String username, String email) {
        User user = new User(userId, username, email);
        user.setProfileImageUrl(profileImageUrl); // Add profile image URL
        
        mDatabase.child("users").child(userId).setValue(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        signupButton.setEnabled(true);
                        
                        if (task.isSuccessful()) {
                            Toast.makeText(SimpleSignupActivity.this, 
                                    "Account created successfully!", Toast.LENGTH_SHORT).show();
                            // Navigate to FormActivity
                            Intent intent = new Intent(SimpleSignupActivity.this, FormActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SimpleSignupActivity.this, 
                                    "Failed to save user data", Toast.LENGTH_SHORT).show();
                                                 }
                     }
                 });
     }
     
     // Simple permission check method
     private boolean checkBasicPermissions() {
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
         } else {
             return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
         }
     }
     
     // Image handling methods
     private void checkPermissionsAndShowImagePicker() {
         // For Android 13+ (API 33+), we only need READ_MEDIA_IMAGES
         // For older versions, we need READ_EXTERNAL_STORAGE
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             // Android 13+ - use READ_MEDIA_IMAGES
             Dexter.withContext(this)
                     .withPermissions(
                             Manifest.permission.READ_MEDIA_IMAGES,
                             Manifest.permission.CAMERA
                     )
                     .withListener(new MultiplePermissionsListener() {
                         @Override
                         public void onPermissionsChecked(MultiplePermissionsReport report) {
                             if (report.areAllPermissionsGranted()) {
                                 showImagePickerDialog();
                             } else {
                                 Toast.makeText(SimpleSignupActivity.this, 
                                         "Permissions are required to select image", Toast.LENGTH_SHORT).show();
                             }
                         }

                         @Override
                         public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, 
                                                                       PermissionToken token) {
                             token.continuePermissionRequest();
                         }
                     }).check();
         } else {
             // Android 12 and below - use READ_EXTERNAL_STORAGE
             Dexter.withContext(this)
                     .withPermissions(
                             Manifest.permission.READ_EXTERNAL_STORAGE,
                             Manifest.permission.CAMERA
                     )
                     .withListener(new MultiplePermissionsListener() {
                         @Override
                         public void onPermissionsChecked(MultiplePermissionsReport report) {
                             if (report.areAllPermissionsGranted()) {
                                 showImagePickerDialog();
                             } else {
                                 Toast.makeText(SimpleSignupActivity.this, 
                                         "Permissions are required to select image", Toast.LENGTH_SHORT).show();
                             }
                         }

                         @Override
                         public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, 
                                                                       PermissionToken token) {
                             token.continuePermissionRequest();
                         }
                     }).check();
         }
     }
     
     private void showImagePickerDialog() {
         String[] options = {"Camera", "Gallery"};
         androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
         builder.setTitle("Choose Image Source");
         builder.setItems(options, (dialog, which) -> {
             if (which == 0) {
                 if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                     openCamera();
                 } else {
                     ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
                 }
             } else {
                 openGallery();
             }
         });
         builder.show();
     }
     
     private void openCamera() {
         Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
         startActivityForResult(intent, CAMERA_REQUEST);
     }
     
     private void openGallery() {
         Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
         startActivityForResult(intent, PICK_IMAGE_REQUEST);
     }
     
     @Override
     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         if (requestCode == 100) {
             if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 openCamera();
             } else {
                 Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
             }
         }
     }
     
     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         
         if (resultCode == RESULT_OK) {
             if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                 selectedImageUri = data.getData();
                 loadImageIntoView(selectedImageUri);
             } else if (requestCode == CAMERA_REQUEST && data != null) {
                 Bitmap photo = (Bitmap) data.getExtras().get("data");
                 selectedImageUri = getImageUriFromBitmap(photo);
                 loadImageIntoView(selectedImageUri);
             }
         }
     }
     

     
     private Uri getImageUriFromBitmap(Bitmap bitmap) {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
         String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
         return Uri.parse(path);
     }
     
     private void loadImageIntoView(Uri imageUri) {
         Picasso.get()
                 .load(imageUri)
                 .placeholder(R.drawable.profile_pic)
                 .error(R.drawable.profile_pic)
                 .into(profileImageView);
     }
     
     private void uploadImageToCloudinary(Uri imageUri, OnImageUploadListener listener) {
         new Thread(() -> {
             try {
                 Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                 byte[] imageBytes = baos.toByteArray();
                 
                 Map<String, Object> uploadOptions = CloudinaryConfig.getUploadOptions();
                 Map result = cloudinary.uploader().upload(imageBytes, uploadOptions);
                 
                 String imageUrl = (String) result.get("secure_url");
                 
                 runOnUiThread(() -> {
                     if (listener != null) {
                         listener.onSuccess(imageUrl);
                     }
                 });
                 
             } catch (IOException e) {
                 e.printStackTrace();
                 runOnUiThread(() -> {
                     if (listener != null) {
                         listener.onError(e.getMessage());
                     }
                 });
             }
         }).start();
     }
     
     private interface OnImageUploadListener {
         void onSuccess(String imageUrl);
         void onError(String error);
    }
} 