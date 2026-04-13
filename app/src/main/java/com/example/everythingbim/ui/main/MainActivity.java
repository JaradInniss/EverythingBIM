package com.example.everythingbim.ui.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.everythingbim.R;
import com.example.everythingbim.ui.home.HomeFragment;
import com.example.everythingbim.ui.map.MapFragment;
import com.example.everythingbim.ui.posts.PostFragment;
import com.example.everythingbim.ui.user.UserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;
    private String userType; // "general" or "business"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // Retrieve user type from Intent or SharedPreferences
        if (getIntent().hasExtra("userType")) {
            userType = getIntent().getStringExtra("userType");
            // Save it for future sessions
            sharedPreferences.edit().putString("userType", userType).apply();
        } else {
            userType = sharedPreferences.getString("userType", "general");
        }

        bottomNavigationView = findViewById(R.id.navigation_bar);
        configureBottomNavigation();

        // Set up bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            viewModel.setNavbarItemId(item.getItemId());
            return true;
        });

        setupObservers();

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Set to 'false' to make status bar icons light (white)
        // Set to 'true' if your background was light and you needed dark icons
        windowInsetsController.setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });
    }

    private void configureBottomNavigation() {
        // Hide or show certain menu items based on user type
        MenuItem mapItem = bottomNavigationView.getMenu().findItem(R.id.navbar_map);
        MenuItem postItem = bottomNavigationView.getMenu().findItem(R.id.navbar_post);

        if (userType.equals("business")) {
            // For business users, hide map? Or show different set? Adjust as needed.
            mapItem.setVisible(false);
            postItem.setVisible(true);
        } else {
            // General users: show all or hide some
            mapItem.setVisible(true);
            postItem.setVisible(true);
        }

        // Optionally set the default selection
        if (userType.equals("business")) {
            // Possibly start with a different default fragment
            viewModel.setNavbarItemId(R.id.navbar_post);
        } else {
            viewModel.setNavbarItemId(R.id.navbar_home);
        }
    }

    private void setupObservers() {
        viewModel.getNavbarItemId().observe(this, id -> {
            Fragment fragment = null;
            if (id == R.id.navbar_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.navbar_post) {
                fragment = new PostFragment();
            } else if (id == R.id.navbar_map) {
                fragment = new MapFragment();
            } else if (id == R.id.navbar_user) {
                fragment = new UserFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }

            // Ensure bottom navigation selection matches (avoid loop)
            if (bottomNavigationView.getSelectedItemId() != id) {
                bottomNavigationView.setSelectedItemId(id);
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}