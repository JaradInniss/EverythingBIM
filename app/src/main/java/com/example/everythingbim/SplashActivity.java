package com.example.everythingbim;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.everythingbim.ui.main.MainActivity;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

public class SplashActivity extends AppCompatActivity implements OnMapsSdkInitializedCallback {

    private static final  String TAG = "SplashActivity";

    private AnimatorSet pulseAnimator;

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

        ImageView splashLogo = findViewById(R.id.splash_logo);
        if (splashLogo != null) {
            startPulseAnimation(splashLogo);
        }

        // Redirect users into the main shell; authentication now happens on demand.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 5000);
    }

    private void startPulseAnimation(ImageView splashLogo) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, 1f, 1.05f, 1f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.RESTART);
        scaleY.setRepeatMode(ValueAnimator.RESTART);

        pulseAnimator = new AnimatorSet();
        pulseAnimator.playTogether(scaleX, scaleY);
        pulseAnimator.setDuration(1600);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();
    }

    @Override
    protected void onDestroy() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        super.onDestroy();
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
