package com.example.everythingbim;

import android.app.Application;
import android.util.Log;

import com.example.everythingbim.data.repository.CanonicalLocationRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class EverythingBimApplication extends Application {

    private static final String TAG = "EverythingBimApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        // Install App Check debug provider for development builds
        // This automatically generates a debug token that allows Firebase Auth to work
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );

        new CanonicalLocationRepository(this).syncCanonicalLocations();

        Log.d(TAG, "Firebase initialized with App Check debug provider");
    }
}
