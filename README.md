# 🎨 Artistry - Digital Art Gallery App

Artistry is a modern Android application that allows users to **explore, upload, and manage artworks**.  
With Firebase integration, it provides **real-time data syncing**, secure user authentication, and a clean UI for a smooth digital art gallery experience.  

---

## ✨ Features

- 🔐 **User Authentication**
  - Login & Register with Firebase Authentication.
  - Google Sign-in support.

- 📂 **Artwork Management**
  - Upload artworks with **title, description, category, and image**.
  - Store artworks securely in **Firebase Firestore**.
  - Display images using **Glide**.

- ⭐ **Favorites System (New!)**
  - Mark/unmark artworks as **favorites**.
  - View all your favorite artworks in a **dedicated screen**.
  - Favorites stored in Firestore under each user.

- 🔍 **Browse & Search**
  - Explore all artworks uploaded by users.
  - Filter artworks by category.
  - Search by title or description.

- 🕒 **Real-time Updates**
  - Artworks sorted by **timestamp**.
  - New uploads appear instantly.

- 🎨 **UI & UX**
  - Modern **Material Design 3**.
  - Light/Dark mode support.
  - Smooth scrolling with **RecyclerView + Shimmer Loading**.

---

## 🛠️ Tech Stack

- **Language**: Java  
- **UI**: XML, Material Design 3  
- **Database**: Firebase Firestore  
- **Authentication**: Firebase Auth  
- **Storage**: Firebase Storage (for images)  
- **Libraries**:
  - Glide (Image loading)
  - Shimmer (Loading effects)
  - RecyclerView & CardView

---

## 📂 Firestore Structure

```plaintext
users/{userId}
    name: string
    email: string
    favorites: [artId1, artId2, ...]
artworks/{artId}
    id: string
    userId: string
    title: string
    description: string
    category: string
    image: string (url)
    timestamp: Timestamp
```

## Connect your app to Firebase (Tools > Firebase).
```
Enable:
Firebase Authentication (Email/Google).
Firestore Database.
Firebase Storage.
```
---
## 📸 Screenshots
<img width="284" height="463" alt="image" src="https://github.com/user-attachments/assets/301ffa06-dc1f-4635-aea0-6a1d4e90abe9" />

---
## 🎯 Future Improvements
- 💬 Comments on artworks
- ❤️ Like system with counts
- 👤 User profiles & bio
- 📤 Share artworks to social media
