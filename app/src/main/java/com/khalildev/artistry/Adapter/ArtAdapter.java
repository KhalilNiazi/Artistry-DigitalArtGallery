package com.khalildev.artistry.Adapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khalildev.artistry.ArtPreviewActivity;
import com.khalildev.artistry.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
                File imageFile = saveImageToCache(art.getTitle(), finalBitmap);

                Uri uri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        imageFile
                );

                Intent intent = new Intent(context, ArtPreviewActivity.class);
                intent.putExtra("image_uri", uri.toString());
                intent.putExtra("title", art.getTitle());
                intent.putExtra("description", art.getDescription());
                intent.putExtra("category", art.getCategory());
                intent.putExtra("artDocId", art.getId());
                intent.putExtra("author_uid", art.getUserId());  // Pass the author's UID


                context.startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(context, "Preview failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Three-dot menu
        Bitmap finalBitmapMenu = bitmap;
        holder.imgOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.imgOptions);
            popup.inflate(R.menu.art_item_menu);
            popup.setOnMenuItemClickListener(item -> handleMenuClick(item, art, finalBitmapMenu, holder.itemView));
            popup.show();
        });
    }

    private boolean handleMenuClick(MenuItem item, ArtItem art, Bitmap bitmap, View view) {
        int id = item.getItemId();
        if (id == R.id.menu_share) {
            shareImage(art, bitmap);
            return true;
        } else if (id == R.id.menu_download) {
            downloadImage(art, bitmap, view);
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
                    .document(art.getId())
                    .set(art)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(context, "You must be logged in to save to favorites.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage(ArtItem art, Bitmap bitmap) {
        try {
            File file = saveImageToCache(art.getTitle(), bitmap);
            Uri contentUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } catch (Exception e) {
            Toast.makeText(context, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadImage(ArtItem art, Bitmap bitmap, View view) {
        try {
            String fileName = art.getTitle() + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Artistry");

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                if (outputStream != null) outputStream.close();

                Snackbar.make(view, "Saved to Gallery → Artistry", Snackbar.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show();
            }

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
            imgArt = itemView.findViewById(R.id.itemImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            imgOptions = itemView.findViewById(R.id.ivOptions);
        }
    }
}
