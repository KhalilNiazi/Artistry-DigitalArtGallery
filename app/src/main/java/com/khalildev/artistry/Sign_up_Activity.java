package com.khalildev.artistry;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Sign_up_Activity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvLoginRedirect;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_pref";
    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHOTO = "photo";
    private static final String KEY_FIRST_LOGIN = "isFirstLogin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up); // Make sure your layout file is named correctly

        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email_signup);
        etPassword = findViewById(R.id.et_password_signup);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);
        tvLoginRedirect = findViewById(R.id.tv_login_redirect);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        btnSignup.setOnClickListener(v -> registerUser());

        tvLoginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(Sign_up_Activity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnSignup.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnSignup.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user, fullName);
                        }
                    } else {
                        Toast.makeText(Sign_up_Activity.this, "Signup failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String fullName) {
        String uid = user.getUid();
        String email = user.getEmail();
        Uri photoUri = user.getPhotoUrl();
        String photo = photoUri != null ? photoUri.toString() : "";

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", fullName);
        userData.put("email", email);
        userData.put("photo", photo);

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Save to SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_UID, uid);
                    editor.putString(KEY_NAME, fullName);
                    editor.putString(KEY_EMAIL, email);
                    editor.putString(KEY_PHOTO, photo);
                    editor.putBoolean(KEY_FIRST_LOGIN, true);
                    editor.apply();

                    Toast.makeText(Sign_up_Activity.this, "Signup successful!", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(Sign_up_Activity.this, ProfileActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Sign_up_Activity.this, "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("Sign_up_Activity", "Firestore error", e);
                });
    }
}
