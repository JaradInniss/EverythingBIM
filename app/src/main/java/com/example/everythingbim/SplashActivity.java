package com.example.everythingbim;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.everythingbim.ui.login.Login;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

public class SplashActivity extends AppCompatActivity implements OnMapsSdkInitializedCallback {

    private static final  String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Maps SDK to use latest renderer
        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Redirect User to Login Screen after 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, Login.class);
            startActivity(intent);
            finish();
        }, 2000);
    }

    @Override
    public void onMapsSdkInitialized(@NonNull MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d(TAG, "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d(TAG, "The legacy version of the renderer is used.");
                break;
        }
    }
}