package com.khalildev.digiart.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.khalildev.digiart.Adapter.ArtAdapter;
import com.khalildev.digiart.Adapter.ArtItem;
import com.khalildev.digiart.R;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ArtAdapter artAdapter;
    private List<ArtItem> favoriteArtList;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize RecyclerView and ProgressBar
        recyclerView = view.findViewById(R.id.recyclerFavorites);
        progressBar = view.findViewById(R.id.progressBarFavorites);

        favoriteArtList = new ArrayList<>();
        artAdapter = new ArtAdapter(getContext(), favoriteArtList);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(artAdapter);

        // Load favorites
        loadFavorites();

        return view;
    }

    private void loadFavorites() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Show progress bar while loading
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Fetch the user's favorite art from Firestore
        firestore.collection("users")
                .document(uid)
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteArtList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ArtItem art = doc.toObject(ArtItem.class);
                            if (art != null) {
                                favoriteArtList.add(art);
                            }
                        }
                        artAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "No favorites found", Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}
