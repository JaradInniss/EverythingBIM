package com.example.everythingbim;

import android.app.Application;
import android.util.Log;

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

        Log.d(TAG, "Firebase initialized with App Check debug provider");
    }
}
