package com.harsh.covilink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import android.content.ContentValues;
import android.os.Build;
import android.provider.MediaStore;
import java.io.OutputStream;

public class CertificateActivity extends AppCompatActivity {
    private TextView nameText, dosesText, statusText;
    private Button downloadPDFButton;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private String certName = "", certDoses = "", certStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate);

        nameText = findViewById(R.id.textViewCertName);
        dosesText = findViewById(R.id.textViewCertDoses);
        statusText = findViewById(R.id.textViewCertStatus);
        downloadPDFButton = findViewById(R.id.buttonDownloadPDF);
        downloadPDFButton.setOnClickListener(v -> downloadCertificatePDF());

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCertificateData();
    }

    private void loadCertificateData() {
        String userId = currentUser.getUid();
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                String username = userSnap.child("username").getValue(String.class);
                int doses = 0;
                boolean fullyVaccinated = false;
                Object dosesObj = userSnap.child("vaccination/dosesTaken").getValue();
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
                if (userSnap.child("vaccination/isFullyVaccinated").getValue() != null) {
                    fullyVaccinated = Boolean.TRUE.equals(userSnap.child("vaccination/isFullyVaccinated").getValue(Boolean.class));
                }
                nameText.setText("Name: " + username);
                dosesText.setText("Doses Taken: " + doses + "/2");
                if (fullyVaccinated) {
                    statusText.setText("✅ Fully Vaccinated");
                } else {
                    statusText.setText("❌ Not Fully Vaccinated");
                }
                // Save for PDF
                certName = "Name: " + username;
                certDoses = "Doses Taken: " + doses + "/2";
                certStatus = fullyVaccinated ? "Fully Vaccinated" : "Not Fully Vaccinated";
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CertificateActivity.this, "Failed to load certificate", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadCertificatePDF() {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(350, 500, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(18);
        canvas.drawText("Vaccination Certificate", 40, 60, paint);
        paint.setTextSize(14);
        canvas.drawText(certName, 40, 120, paint);
        canvas.drawText(certDoses, 40, 160, paint);
        paint.setTextSize(16);
        paint.setColor(certStatus.equals("Fully Vaccinated") ? Color.parseColor("#4CAF50") : Color.RED);
        canvas.drawText(certStatus, 40, 210, paint);
        pdfDocument.finishPage(page);
        String fileName = "Vaccination_Certificate_" + System.currentTimeMillis() + ".pdf";
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.Downloads.RELATIVE_PATH, "Download/");
                android.net.Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri == null) throw new Exception("Failed to create file");
                fos = getContentResolver().openOutputStream(uri);
            } else {
                String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                java.io.File file = new java.io.File(dirPath, fileName);
                fos = new java.io.FileOutputStream(file);
            }
            pdfDocument.writeTo(fos);
            if (fos != null) fos.close();
            Toast.makeText(this, "PDF saved to Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        pdfDocument.close();
    }
} 