package com.example.everythingbim.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Interface for Firebase services dependency injection.
 * Allows mocking Firebase in unit tests without requiring actual Firebase initialization.
 */
public interface FirebaseProvider {

    /**
     * @return FirebaseAuth instance for authentication
     */
    FirebaseAuth getAuth();

    /**
     * @return FirebaseFirestore instance for Firestore database
     */
    FirebaseFirestore getFirestore();

    /**
     * @return FirebaseStorage instance for file storage
     */
    FirebaseStorage getStorage();
}