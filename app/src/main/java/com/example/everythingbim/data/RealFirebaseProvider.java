package com.example.everythingbim.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Production implementation of FirebaseProvider.
 * Returns actual Firebase instances.
 */
public class RealFirebaseProvider implements FirebaseProvider {

    @Override
    public FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    @Override
    public FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    @Override
    public FirebaseStorage getStorage() {
        return FirebaseStorage.getInstance();
    }
}