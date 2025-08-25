package com.khalildev.digiart.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.khalildev.digiart.Adapter.ArtAdapter;
import com.khalildev.digiart.Adapter.ArtItem;
import com.khalildev.digiart.R;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private SearchView searchView;
    private ArtAdapter artAdapter;
    private List<ArtItem> artList;

    private FirebaseFirestore firestore;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        recyclerView = view.findViewById(R.id.recyclerSearch);
        searchView = view.findViewById(R.id.searchView);

        firestore = FirebaseFirestore.getInstance();

        artList = new ArrayList<>();
        artAdapter = new ArtAdapter(getContext(), artList);

        // Grid layout with 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(artAdapter);

        setupSearchListener();

        return view;
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchArt(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    artList.clear();
                    artAdapter.notifyDataSetChanged();
                } else {
                    searchArt(newText);
                }
                return true;
            }
        });
    }

    private void searchArt(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            artList.clear();
            artAdapter.notifyDataSetChanged();
            return;
        }

        String lowerKeyword = keyword.toLowerCase();

        firestore.collectionGroup("userArts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    artList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String category = doc.getString("category");
                        String description = doc.getString("description");

                        Log.d("SearchDebug", "Checking: " + title);

                        if ((title != null && title.toLowerCase().contains(lowerKeyword)) ||
                                (category != null && category.toLowerCase().contains(lowerKeyword)) ||
                                (description != null && description.toLowerCase().contains(lowerKeyword))) {

                            ArtItem art = doc.toObject(ArtItem.class);
                            artList.add(art);
                        }
                    }

                    if (artList.isEmpty()) {
                        Toast.makeText(getContext(), "No results found.", Toast.LENGTH_SHORT).show();
                    }

                    artAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
