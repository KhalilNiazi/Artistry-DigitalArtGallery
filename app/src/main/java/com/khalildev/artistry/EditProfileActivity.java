package com.khalildev.artistry;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView imgProfilePic;
    private EditText etDisplayName, etUsername, etBio;
    private MaterialButton btnSaveProfile;

    private Uri selectedImageUri = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imgProfilePic = findViewById(R.id.imgProfilePic);
        etDisplayName = findViewById(R.id.etDisplayName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        // Load existing profile
        loadUserProfile();

        // Register launcher for image picking
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).into(imgProfilePic);
                        }
                    }
                });

        // Pick image on image click
        imgProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Save profile
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        etDisplayName.setText(snapshot.getString("name"));
                        etUsername.setText(snapshot.getString("username"));
                        etBio.setText(snapshot.getString("bio"));
                        String photoUrl = snapshot.getString("photo");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this).load(photoUrl).into(imgProfilePic);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String displayName = etDisplayName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (displayName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Display name and username are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", displayName);
        userMap.put("username", username);
        userMap.put("bio", bio);

        if (selectedImageUri != null) {
            // Convert image to Base64 and add to map
            Bitmap bitmap = getBitmapFromUri(selectedImageUri);
            if (bitmap != null) {
                String encodedImage = encodeImageToBase64(bitmap);
                if (encodedImage != null) {
                    userMap.put("photo", encodedImage);
                } else {
                    Toast.makeText(this, "Failed to encode image", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Update Firestore user document
        db.collection("users").document(uid)
                .update(userMap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
