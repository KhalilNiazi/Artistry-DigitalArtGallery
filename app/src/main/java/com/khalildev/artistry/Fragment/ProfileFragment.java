package com.khalildev.artistry.Fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.khalildev.artistry.Adapter.ArtAdapter;
import com.khalildev.artistry.Adapter.ArtItem;
import com.khalildev.artistry.EditProfileActivity;
import com.khalildev.artistry.R;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private ImageView ivProfile;
    private TextView tvName, tvBio;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private List<ArtItem> artList;
    private ArtAdapter artAdapter;

    private String profileUrl = null;  // Store profile image URL here

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ivProfile = view.findViewById(R.id.imgProfilePic);
        tvName = view.findViewById(R.id.tvProfileName);
        tvBio = view.findViewById(R.id.tvProfileBio);
        Button btnEditProfile = view.findViewById(R.id.btnEditProfile);
        recyclerView = view.findViewById(R.id.recyclerProfile);
        progressBar = view.findViewById(R.id.progressBarProfile);

        artList = new ArrayList<>();
        artAdapter = new ArtAdapter(getContext(), artList);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(artAdapter);

        loadProfileDetails();
        loadUserArts();

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pass name, bio, and profile image URL to the EditProfileActivity
                Intent intent = new Intent(getContext(), EditProfileActivity.class);
                intent.putExtra("name", tvName.getText().toString());
                intent.putExtra("bio", tvBio.getText().toString());
                intent.putExtra("photo", profileUrl); // Pass the profile image URL
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadProfileDetails() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.getString("name");
                        String bio = snapshot.getString("bio");
                        profileUrl = snapshot.getString("photo");  // This will be the Base64 string

                        // Set Name and Bio
                        tvName.setText(name != null ? name : "Your Name");
                        tvBio.setText(bio != null ? bio : "Your Bio");

                        // Check if profileUrl (Base64 string) is not empty
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            // Decode Base64 string to Bitmap
                            Bitmap decodedBitmap = decodeBase64ToBitmap(profileUrl);
                            if (decodedBitmap != null) {
                                ivProfile.setImageBitmap(decodedBitmap);
                            } else {
                                ivProfile.setImageResource(R.drawable.ic_user_placeholder);  // Default image if decoding fails
                            }
                        } else {
                            ivProfile.setImageResource(R.drawable.ic_user_placeholder);  // Default image
                        }
                    } else {
                        Log.d("ProfileFragment", "No document found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error loading profile: ", e);
                    Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Method to decode Base64 string into Bitmap
     */
    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadUserArts() {
        if (auth.getCurrentUser() == null) return;

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        String uid = auth.getCurrentUser().getUid();

        firestore.collection("arts")
                .document(uid)
                .collection("userArts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    artList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        ArtItem art = doc.toObject(ArtItem.class);
                        if (art != null) {
                            art.setId(doc.getId()); // If you want Firestore ID
                            artList.add(art);
                        }
                    }

                    artAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    if (artList.isEmpty()) {
                        Toast.makeText(getContext(), "No art uploaded yet!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load arts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
