package com.khalildev.digiart.Adapter;

import com.google.firebase.Timestamp;

public class ArtItem {
    private String id;           // Firestore document ID
    private String userId;
    private String title;
    private String description;
    private String category;
    private String image;        // Base64
    private Timestamp timestamp; // ✅ FIXED type

    // Required empty constructor for Firestore
    public ArtItem() {}

    // Full constructor
    public ArtItem(String id, String userId, String title, String description, String category, String image, Timestamp timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.image = image;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Timestamp getTimestamp() { return timestamp; }   // ✅ FIXED
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
