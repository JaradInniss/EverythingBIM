package com.example.everythingbim;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Mock FirebaseProvider for unit testing.
 * Returns null for all Firebase methods to avoid initialization.
 * Only use when testing methods that don't call Firebase directly.
 */
public class MockFirebaseProvider implements com.example.everythingbim.data.FirebaseProvider {

    @Override
    public FirebaseAuth getAuth() {
        return null;
    }

    @Override
    public FirebaseFirestore getFirestore() {
        return null;
    }

    @Override
    public FirebaseStorage getStorage() {
        return null;
    }
}