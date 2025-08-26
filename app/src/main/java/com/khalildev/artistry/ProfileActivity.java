package com.khalildev.artistry;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        imgProfilePic.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        imgProfilePic = findViewById(R.id.imgProfilePic);
        etDisplayName = findViewById(R.id.etDisplayName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        tvSkip = findViewById(R.id.tvSkip);

        handleIncomingProfileImage();

        imgProfilePic.setOnClickListener(v -> checkPermissionsAndOpenGallery());

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
        }
    }

    private void checkPermissionsAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void saveProfileToFirebase() {
        String displayName = etDisplayName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (displayName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Display name and username are required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        if (selectedImageUri != null) {
            String extension = getFileExtension(selectedImageUri);
            if (extension == null) {
                Toast.makeText(this, "Invalid image file", Toast.LENGTH_SHORT).show();
                return;
            }

            StorageReference fileRef = storageRef.child(uid + "." + extension);
            fileRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                uploadUserData(uid, displayName, username, bio, imageUrl);
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                .addOnFailureListener(e ->
                        Toast.makeText(ProfileActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show());
    }

    private String getFileExtension(Uri uri) {
        if (uri == null) return null;
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Permission denied to access media", Toast.LENGTH_SHORT).show();
        }
    }
}
