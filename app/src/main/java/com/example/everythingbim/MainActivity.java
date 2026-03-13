package com.example.everythingbim;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.navigation_bar);
        loadFragment(new HomeFragment());
        bottomNavigationView.setSelectedItemId(R.id.navbar_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set On Item Selected Listener for Navbar Items
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navbar_home) {
                loadFragment(new HomeFragment());
            }
            else if (item.getItemId() == R.id.navbar_post) {
                loadFragment(new PostFragment());
            }
            else if (item.getItemId() == R.id.navbar_map) {
                loadFragment(new MapFragment());
            }
            else if (item.getItemId() == R.id.navbar_user) {
                loadFragment(new UserFragment());
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}