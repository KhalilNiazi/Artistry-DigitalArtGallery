package com.khalildev.digiart;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.khalildev.digiart.Adapter.CommentAdapter;
import com.khalildev.digiart.Model_Class.CommentItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArtPreviewActivity extends AppCompatActivity {

    private ImageView imgFullArt, imgUserProfile;
    private TextView tvUserName, tvTitle, tvCategory, tvDescription;
    private RecyclerView recyclerComments;
    private EditText etComment;
    private TextView btnSendComment;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private List<CommentItem> commentList;
    private CommentAdapter commentAdapter;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_art_preview);

        imgFullArt = findViewById(R.id.imgFullArt);
        imgUserProfile = findViewById(R.id.imgUserProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvTitle = findViewById(R.id.tvFullTitle);
        tvCategory = findViewById(R.id.tvFullCategory);
        tvDescription = findViewById(R.id.tvFullDescription);

        recyclerComments = findViewById(R.id.recyclerComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerComments.setAdapter(commentAdapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading comment...");
        progressDialog.setCancelable(false);

        // Load artwork details from Intent extras
        String imageUri = getIntent().getStringExtra("image_uri");
        if (imageUri != null && !imageUri.isEmpty()) {
            Glide.with(this).load(Uri.parse(imageUri)).into(imgFullArt);
        }

        String title = getIntent().getStringExtra("title");
        String category = getIntent().getStringExtra("category");
        String description = getIntent().getStringExtra("description");

        tvTitle.setText(title != null ? title : "");
        tvCategory.setText(category != null ? category : "");
        tvDescription.setText(description != null ? description : "");

        // ----------- IMPORTANT: Load author profile based on passed author UID -----------

        loadUserDetails();

        // Retrieve the artwork document ID (artDocId) from Intent
        String artDocId = getIntent().getStringExtra("artDocId");
        if (artDocId == null || artDocId.isEmpty()) {
            Toast.makeText(this, "Invalid artwork document ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load comments for the artwork using artDocId
        loadComments(artDocId);

        btnSendComment.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(ArtPreviewActivity.this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (auth.getCurrentUser() == null) {
                Toast.makeText(ArtPreviewActivity.this, "Please login to comment", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.show();

            HashMap<String, Object> data = new HashMap<>();
            data.put("userId", auth.getCurrentUser().getUid());
            data.put("userName", tvUserName.getText().toString());  // This is author’s name; you may want current user's name instead
            data.put("text", text);
            data.put("timestamp", Timestamp.now());

            firestore.collection("arts")
                    .document(artDocId)
                    .collection("comments")
                    .add(data)
                    .addOnSuccessListener(docRef -> {
                        etComment.setText("");

                        CommentItem newComment = new CommentItem();
                        newComment.setUserId(auth.getCurrentUser().getUid());
                        newComment.setUserName(tvUserName.getText().toString()); // You might want to change this to current user's name
                        newComment.setText(text);
                        newComment.setTimestamp(Timestamp.now());
                        commentList.add(newComment);
                        commentAdapter.notifyItemInserted(commentList.size() - 1);
                        recyclerComments.scrollToPosition(commentList.size() - 1);

                        progressDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ArtPreviewActivity.this, "Failed to add comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
        });
    }

    private void loadComments(String artDocId) {
        commentList.clear();

        CollectionReference cr = firestore.collection("arts")
                .document(artDocId)
                .collection("comments");

        cr.orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        CommentItem c = doc.toObject(CommentItem.class);
                        c.setId(doc.getId());
                        commentList.add(c);
                    }
                    commentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Load comments error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserDetails() {
        // Get the author UID passed via Intent (instead of current user)
        String authorUid = getIntent().getStringExtra("author_uid");
        if (authorUid == null || authorUid.isEmpty()) {
            Toast.makeText(this, "Author info not available", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = firestore.collection("users").document(authorUid);
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        String userPhotoUrl = documentSnapshot.getString("profile");

                        tvUserName.setText(userName != null ? userName : "No Name");

                        if (userPhotoUrl != null && !userPhotoUrl.isEmpty()) {
                            Glide.with(this).load(userPhotoUrl).circleCrop().into(imgUserProfile);
                        } else {
                            imgUserProfile.setImageResource(R.drawable.placeholder);
                        }
                    } else {
                        Toast.makeText(this, "Author data not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load author details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
