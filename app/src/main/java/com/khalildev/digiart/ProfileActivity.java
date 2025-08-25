package com.khalildev.digiart;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfilePic;
    private EditText etDisplayName, etUsername, etBio;
    private MaterialButton btnSaveProfile;
    private TextView tvSkip;

    private Uri selectedImageUri = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private static final int GALLERY_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // Your layout XML

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        imgProfilePic = findViewById(R.id.imgProfilePic);
        etDisplayName = findViewById(R.id.etDisplayName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        tvSkip = findViewById(R.id.tvSkip);

        // Load image from intent or set up gallery picker
        handleIncomingProfileImage();

        imgProfilePic.setOnClickListener(v -> selectImageFromGallery());

        btnSaveProfile.setOnClickListener(v -> saveProfileToFirebase());

        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });
    }

    private void handleIncomingProfileImage() {
        String imageUrl = getIntent().getStringExtra("profileImageUrl");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(imgProfilePic);
        } else {
            selectImageFromGallery();
        }
    }

    private void selectImageFromGallery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imgProfilePic.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveProfileToFirebase() {
        String displayName = etDisplayName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (displayName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Display name and username are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        if (selectedImageUri != null) {
            // Upload profile picture to Firebase Storage
            StorageReference fileRef = storageRef.child(uid + "." + getFileExtension(selectedImageUri));
            fileRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        uploadUserData(uid, displayName, username, bio, imageUrl);
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            uploadUserData(uid, displayName, username, bio, null);
        }
    }

    private void uploadUserData(String uid, String name, String username, String bio, String imageUrl) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("name", name);
        userMap.put("username", username);
        userMap.put("bio", bio);
        if (imageUrl != null) {
            userMap.put("photo", imageUrl);
        }

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }
}
