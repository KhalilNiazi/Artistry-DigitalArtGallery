package com.khalildev.digiart.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khalildev.digiart.ArtPreviewActivity;
import com.khalildev.digiart.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ArtAdapter extends RecyclerView.Adapter<ArtAdapter.ArtViewHolder> {

    private Context context;
    private List<ArtItem> artList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public ArtAdapter(Context context, List<ArtItem> artList) {
        this.context = context;
        this.artList = artList;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ArtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
        return new ArtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtViewHolder holder, int position) {
        ArtItem art = artList.get(position);
        holder.tvTitle.setText(art.getTitle());
        holder.tvCategory.setText(art.getCategory());
        holder.tvDescription.setText(art.getDescription());

        // Decode Base64 image
        Bitmap bitmap = null;
        try {
            byte[] decodedBytes = Base64.decode(art.getImage(), Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.imgArt.setImageBitmap(bitmap);
        } catch (Exception e) {
            holder.imgArt.setImageResource(android.R.color.darker_gray);
        }

        // Open full preview when item clicked
        Bitmap finalBitmap = bitmap;
        holder.itemView.setOnClickListener(v -> {
            try {
                // Save the image to cache and create the intent
                File imageFile = saveImageToCache(art.getTitle(), finalBitmap);

                Intent intent = new Intent(context, ArtPreviewActivity.class);
                intent.putExtra("image_uri", Uri.fromFile(imageFile).toString());
                intent.putExtra("title", art.getTitle());
                intent.putExtra("description", art.getDescription());
                intent.putExtra("category", art.getCategory());

                // Pass the document ID (artDocId) for potential use in the preview activity
                intent.putExtra("artDocId", art.getId()); // Pass the art document ID here

                context.startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(context, "Preview failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Three-dot menu for options (share, download, favorite)
        Bitmap finalBitmapMenu = bitmap;
        holder.imgOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.imgOptions);
            popup.inflate(R.menu.art_item_menu);
            popup.setOnMenuItemClickListener(item -> handleMenuClick(item, art, finalBitmapMenu));
            popup.show();
        });
    }

    private boolean handleMenuClick(MenuItem item, ArtItem art, Bitmap bitmap) {
        int id = item.getItemId();
        if (id == R.id.menu_share) {
            shareImage(art, bitmap);
            return true;
        } else if (id == R.id.menu_download) {
            downloadImage(art, bitmap);
            return true;
        } else if (id == R.id.menu_favorite) {
            saveToFavorites(art);
            Toast.makeText(context, "Added to Favorites: " + art.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void saveToFavorites(ArtItem art) {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            firestore.collection("users")
                    .document(uid)
                    .collection("favorites")
                    .document(art.getId()) // Art ID is used as the document ID
                    .set(art)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to add to favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(context, "You must be logged in to save to favorites.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage(ArtItem art, Bitmap bitmap) {
        try {
            File file = saveImageToCache(art.getTitle(), bitmap);
            Uri contentUri = Uri.fromFile(file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            context.startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } catch (Exception e) {
            Toast.makeText(context, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadImage(ArtItem art, Bitmap bitmap) {
        try {
            File dir = new File(context.getExternalFilesDir(null), "DigiArt");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, art.getTitle() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Toast.makeText(context, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File saveImageToCache(String title, Bitmap bitmap) throws Exception {
        File cachePath = new File(context.getCacheDir(), "preview");
        if (!cachePath.exists()) cachePath.mkdirs();

        File file = new File(cachePath, title + "_preview.png");
        FileOutputStream stream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        stream.close();

        return file;
    }

    @Override
    public int getItemCount() {
        return artList.size();
    }

    static class ArtViewHolder extends RecyclerView.ViewHolder {
        ImageView imgArt, imgOptions;
        TextView tvTitle, tvCategory, tvDescription;

        public ArtViewHolder(@NonNull View itemView) {
            super(itemView);
            imgArt = itemView.findViewById(R.id.imgArt);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            imgOptions = itemView.findViewById(R.id.ivOptions);
        }
    }
}
