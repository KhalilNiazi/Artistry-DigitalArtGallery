package com.khalildev.digiart.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.khalildev.digiart.Adapter.ArtAdapter;
import com.khalildev.digiart.Adapter.ArtItem;
import com.khalildev.digiart.R;

import java.util.ArrayList;
import java.util.List;

public class Home_Fragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FirebaseFirestore firestore;
    private List<ArtItem> artList;
    private ArtAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_, container, false);


        recyclerView = view.findViewById(R.id.recyclerFeed);
        progressBar = view.findViewById(R.id.progressBar);
        firestore = FirebaseFirestore.getInstance();

        artList = new ArrayList<>();
        adapter = new ArtAdapter(getContext(), artList);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        loadArts();

        return view;
    }

    private void loadArts() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        firestore.collectionGroup("userArts") // 🔍 Load all user arts
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    artList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ArtItem art = doc.toObject(ArtItem.class);
                        if (art != null) {
                            art.setId(doc.getId());
                            artList.add(art);
                        }
                    }

                    // 🔀 Shuffle the list randomly
                    java.util.Collections.shuffle(artList);

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    if (artList.isEmpty()) {
                        Toast.makeText(getContext(), "No art found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load arts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}