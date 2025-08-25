package com.khalildev.digiart;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khalildev.digiart.Fragment.FavoritesFragment;
import com.khalildev.digiart.Fragment.Home_Fragment;
import com.khalildev.digiart.Fragment.ProfileFragment;
import com.khalildev.digiart.Fragment.SearchFragment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import me.ibrahimsn.lib.SmoothBottomBar;

public class MainActivity extends AppCompatActivity {

    private Uri selectedImageUri;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        SmoothBottomBar bottomBar = findViewById(R.id.bottomBar);
        FloatingActionButton floatingActionButton = findViewById(R.id.fabadd);

        // Show Home_Fragment by default on app start
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new Home_Fragment())
                .commit();

        // Register image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();

                        if (selectedImageUri != null) {
                            UploadDialog uploadDialog = new UploadDialog(MainActivity.this, selectedImageUri);
                            uploadDialog.show();
                        }
                    }
                });

        floatingActionButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        bottomBar.setOnItemSelectedListener(position -> {
            Fragment selectedFragment = null;
            if (position == 0) {
                selectedFragment = new Home_Fragment();
            } else if (position == 1) {
                selectedFragment = new SearchFragment();
            }else if (position == 2) {
                selectedFragment = new FavoritesFragment();
            }else if (position == 3) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    public class UploadDialog extends Dialog {

        private Uri imageUri;
        private ImageView imageView;
        private EditText edtTitle, edtDescription, edtCategory;
        private Button btnUpload, btnCancel;
        private FirebaseFirestore firestore;
        private FirebaseAuth auth;
        private Bitmap editedBitmap;

        private AlertDialog loadingDialog; // 🔹 Progress dialog

        public UploadDialog(@NonNull Context context, Uri imageUri) {
            super(context);
            this.imageUri = imageUri;
            firestore = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.fragment_upload_post);

            // Fullscreen
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            getWindow().setAttributes(params);

            imageView = findViewById(R.id.imgPreview);
            edtTitle = findViewById(R.id.etTitle);
            edtDescription = findViewById(R.id.etDescription);
            edtCategory = findViewById(R.id.etCatigory); // 🔹 new category field
            btnUpload = findViewById(R.id.btnUpload);
            btnCancel = findViewById(R.id.btnCancel);
            FloatingActionButton btnEditImage = findViewById(R.id.btnEditImage);

            // Load preview
            loadImageIntoView(imageUri);

            btnUpload.setOnClickListener(v -> uploadImageToFirestore());
            btnCancel.setOnClickListener(v -> dismiss());
        }

        private void loadImageIntoView(Uri uri) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        // 🔹 Show loading spinner dialog
        private void showLoading() {
            if (loadingDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loading, null);
                builder.setView(view);
                builder.setCancelable(false);
                loadingDialog = builder.create();
            }
            loadingDialog.show();
        }

        // 🔹 Hide loading spinner dialog
        private void hideLoading() {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        }

        private void uploadImageToFirestore() {
            String title = edtTitle.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();
            String category = edtCategory.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading();

            Bitmap imageToUpload = (editedBitmap != null) ? editedBitmap : getBitmapFromUri(imageUri);
            String encodedImage = encodeImageToBase64(imageToUpload);

            String userId = auth.getCurrentUser().getUid(); // ✅ Get current user ID

            Map<String, Object> imageData = new HashMap<>();
            imageData.put("title", title);
            imageData.put("description", description);
            imageData.put("category", category);
            imageData.put("image", encodedImage);
            imageData.put("timestamp", com.google.firebase.Timestamp.now());

            // ✅ Save inside arts/{userId}/userArts/{docId}
            firestore.collection("arts")
                    .document(userId)
                    .collection("userArts")
                    .add(imageData)
                    .addOnSuccessListener(docRef -> {
                        String docId = docRef.getId();
                        docRef.update("docId", docId) // optional
                                .addOnSuccessListener(aVoid -> {
                                    hideLoading();
                                    Toast.makeText(getContext(), "Uploaded successfully!", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    hideLoading();
                                    Toast.makeText(getContext(), "Upload ok, but failed to set ID", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                });
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        private String encodeImageToBase64(Bitmap bitmap) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Encoding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        private Bitmap getBitmapFromUri(Uri uri) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                return BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error decoding image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        }
    }

    }
