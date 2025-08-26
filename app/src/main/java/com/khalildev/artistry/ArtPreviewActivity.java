package com.khalildev.artistry;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.khalildev.artistry.Adapter.CommentAdapter;
import com.khalildev.artistry.Adapter.RelatedAdapter;
import com.khalildev.artistry.Model_Class.CommentItem;
import com.khalildev.artistry.Adapter.ArtItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArtPreviewActivity extends AppCompatActivity {

    private ImageView imgFullArt, imgUserProfile, btnShare, btnSendComment;
    private Button btnSave;
    private TextView tvUserName, tvTitle, tvDescription;

    private RecyclerView recyclerComments, recyclerRelated;
    private EditText etComment;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private List<CommentItem> commentList;
    private CommentAdapter commentAdapter;

    private List<ArtItem> relatedList;
    private RelatedAdapter relatedAdapter;

    private ProgressDialog progressDialog;

    private String artDocId;
    private String authorUid;
    private String artCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_art_preview);

        // Initialize views
        imgFullArt = findViewById(R.id.imgFullArt);
        imgUserProfile = findViewById(R.id.imgUserProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvTitle = findViewById(R.id.tvFullTitle);
        tvDescription = findViewById(R.id.tvFullDescription);
        btnShare = findViewById(R.id.btnShare);
        btnSave = findViewById(R.id.btnSave);
        recyclerComments = findViewById(R.id.recyclerComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        recyclerRelated = findViewById(R.id.recyclerRelated);

        // Firebase initialization
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize lists and adapters
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerComments.setAdapter(commentAdapter);

        relatedList = new ArrayList<>();
        relatedAdapter = new RelatedAdapter(relatedList, this::openArtPreview);
        recyclerRelated.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerRelated.setAdapter(relatedAdapter);

        // ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading comment...");
        progressDialog.setCancelable(false);

        // Get data from Intent
        String imageUri = getIntent().getStringExtra("image_uri");
        String title = getIntent().getStringExtra("title");
        artCategory = getIntent().getStringExtra("category");
        String description = getIntent().getStringExtra("description");
        artDocId = getIntent().getStringExtra("artDocId");
        authorUid = getIntent().getStringExtra("author_uid");

        // Load image using Glide (if available)
        if (imageUri != null && !imageUri.isEmpty()) {
            Glide.with(this).load(Uri.parse(imageUri)).into(imgFullArt);
        }

        // Set title and description
        tvTitle.setText(title != null ? title : "No Title");
        tvDescription.setText(description != null ? description : "No Description");

        // Load author info (user details)
        if (authorUid != null && !authorUid.isEmpty()) {
            loadUserDetails(authorUid);
        } else {
            Toast.makeText(this, "Author UID is missing", Toast.LENGTH_SHORT).show();
        }

        // Load comments and related pins
        if (artDocId != null && !artDocId.isEmpty() && authorUid != null) {
            loadComments();
            loadRelatedPins(artCategory);
        }

        // Set listeners for buttons
        btnSendComment.setOnClickListener(v -> sendComment());
        btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "Saved to your collection!", Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this artwork on Artistry!");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }

    // ---------------- Load User Info ----------------
    private void loadUserDetails(String uid) {
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        String userPhotoBase64 = documentSnapshot.getString("photo"); // Base64 photo

                        tvUserName.setText(userName != null ? userName : "Unknown");

                        if (userPhotoBase64 != null && !userPhotoBase64.isEmpty()) {
                            try {
                                // Decode Base64 string to bytes
                                byte[] decodedBytes = Base64.decode(userPhotoBase64, Base64.DEFAULT);
                                // Convert bytes to Bitmap
                                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                // Set Bitmap to ImageView
                                imgUserProfile.setImageBitmap(decodedBitmap);
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                                // If decoding fails, use default image
                                imgUserProfile.setImageResource(R.drawable.ic_profile);
                            }
                        } else {
                            // If photo is empty, use default image
                            imgUserProfile.setImageResource(R.drawable.ic_profile);
                        }
                    } else {
                        Toast.makeText(this, "Author not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load author: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // ---------------- Load Comments ----------------
    private void loadComments() {
        commentList.clear();
        CollectionReference cr = firestore.collection("arts")
                .document(authorUid)
                .collection("userArts")
                .document(artDocId)
                .collection("comments");

        cr.orderBy("timestamp", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        CommentItem c = doc.toObject(CommentItem.class);
                        c.setId(doc.getId());
                        commentList.add(c);
                    }
                    commentAdapter.notifyDataSetChanged();
                });
    }

    // ---------------- Send Comment ----------------
    private void sendComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to comment", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        HashMap<String, Object> data = new HashMap<>();
        data.put("userId", auth.getCurrentUser().getUid());
        data.put("userName", auth.getCurrentUser().getEmail());
        data.put("text", text);
        data.put("timestamp", Timestamp.now());

        firestore.collection("arts")
                .document(authorUid)
                .collection("userArts")
                .document(artDocId)
                .collection("comments")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    etComment.setText("");
                    CommentItem newComment = new CommentItem();
                    newComment.setId(docRef.getId());
                    newComment.setUserId(auth.getCurrentUser().getUid());
                    newComment.setUserName(auth.getCurrentUser().getEmail());
                    newComment.setText(text);
                    newComment.setTimestamp(Timestamp.now());

                    commentList.add(newComment);
                    commentAdapter.notifyItemInserted(commentList.size() - 1);
                    recyclerComments.scrollToPosition(commentList.size() - 1);
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
    }

    // ---------------- Load Related Pins ----------------
    private void loadRelatedPins(String category) {
        if (category == null || category.isEmpty()) return;

        firestore.collectionGroup("userArts") // 🔥 fetch arts across all users
                .whereEqualTo("category", category)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshot -> {
                    relatedList.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ArtItem art = doc.toObject(ArtItem.class);
                        art.setId(doc.getId());
                        relatedList.add(art);
                    }
                    relatedAdapter.notifyDataSetChanged();
                });
    }

    // ---------------- Open another pin ----------------
    private void openArtPreview(ArtItem art) {
        Intent intent = new Intent(this, ArtPreviewActivity.class);
        intent.putExtra("image_uri", art.getImage());
        intent.putExtra("title", art.getTitle());
        intent.putExtra("category", art.getCategory());
        intent.putExtra("description", art.getDescription());
        intent.putExtra("author_uid", art.getUserId());
        intent.putExtra("artDocId", art.getId());
        startActivity(intent);
    }
}
