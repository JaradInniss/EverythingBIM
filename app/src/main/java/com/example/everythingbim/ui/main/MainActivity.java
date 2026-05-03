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
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.AdminReportsFragment;
import com.example.everythingbim.R;
import com.example.everythingbim.ui.home.NearbySavedLocation;
import com.example.everythingbim.ui.home.HomeFragment;
import com.example.everythingbim.ui.map.MapFragment;
import com.example.everythingbim.ui.posts.PostFragment;
import com.example.everythingbim.ui.user.UserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_MAP_FOCUS = "open_map_focus";
    public static final String EXTRA_MAP_FOCUS_LATITUDE = "map_focus_latitude";
    public static final String EXTRA_MAP_FOCUS_LONGITUDE = "map_focus_longitude";
    public static final String EXTRA_MAP_FOCUS_TITLE = "map_focus_title";
    public static final String EXTRA_MAP_FOCUS_SUBTITLE = "map_focus_subtitle";
    public static final String EXTRA_OPEN_MAP_ROUTE = "open_map_route";
    public static final String EXTRA_MAP_ROUTE_LOCATIONS = "map_route_locations";

    private MainViewModel viewModel;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;
    private String userType; // "general" or "business"
    private boolean pendingMapFocus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

//        // Check if the fragment has already been added
//        if (savedInstanceState == null) {
//            // Start a FragmentTransaction
//            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//            // Replace the container with your new fragment
//            transaction.replace(R.id.fragment_container_view, AdminReportsFragment.class, null);
//            // Commit the transaction
//            transaction.commit();
//        }


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

        pendingMapFocus = getIntent().getBooleanExtra(EXTRA_OPEN_MAP_FOCUS, false)
                || getIntent().getBooleanExtra(EXTRA_OPEN_MAP_ROUTE, false);

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
        } else if (pendingMapFocus) {
            viewModel.setNavbarItemId(R.id.navbar_map);
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
                fragment = createMapFragment();
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

    private Fragment createMapFragment() {
        MapFragment fragment = new MapFragment();
        if (!pendingMapFocus) {
            return fragment;
        }

        Bundle args = new Bundle();
        args.putBoolean(MapFragment.ARG_OPEN_FOCUS_LOCATION, true);
        args.putDouble(MapFragment.ARG_FOCUS_LATITUDE, getIntent().getDoubleExtra(EXTRA_MAP_FOCUS_LATITUDE, 0d));
        args.putDouble(MapFragment.ARG_FOCUS_LONGITUDE, getIntent().getDoubleExtra(EXTRA_MAP_FOCUS_LONGITUDE, 0d));
        args.putString(MapFragment.ARG_FOCUS_TITLE, getIntent().getStringExtra(EXTRA_MAP_FOCUS_TITLE));
        args.putString(MapFragment.ARG_FOCUS_SUBTITLE, getIntent().getStringExtra(EXTRA_MAP_FOCUS_SUBTITLE));
        args.putBoolean(MapFragment.ARG_OPEN_ROUTE_PREVIEW, getIntent().getBooleanExtra(EXTRA_OPEN_MAP_ROUTE, false));
        args.putSerializable(MapFragment.ARG_ROUTE_LOCATIONS, getIntent().getSerializableExtra(EXTRA_MAP_ROUTE_LOCATIONS));
        fragment.setArguments(args);

        pendingMapFocus = false;
        getIntent().removeExtra(EXTRA_OPEN_MAP_FOCUS);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LATITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LONGITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_TITLE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_SUBTITLE);
        getIntent().removeExtra(EXTRA_OPEN_MAP_ROUTE);
        getIntent().removeExtra(EXTRA_MAP_ROUTE_LOCATIONS);
        return fragment;
    }
}
