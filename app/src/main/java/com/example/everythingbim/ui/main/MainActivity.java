package com.example.everythingbim.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

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
    public static final String EXTRA_OPEN_MAP = "open_map_focus";
    public static final String EXTRA_MAP_FOCUS_LATITUDE = "map_focus_latitude";
    public static final String EXTRA_MAP_FOCUS_LONGITUDE = "map_focus_longitude";
    public static final String EXTRA_MAP_FOCUS_NAME = "map_focus_title";
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

        pendingMapFocus = getIntent().getBooleanExtra(EXTRA_OPEN_MAP, false)
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
        windowInsetsController.setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        pendingMapFocus = intent.getBooleanExtra(EXTRA_OPEN_MAP, false)
                || intent.getBooleanExtra(EXTRA_OPEN_MAP_ROUTE, false);

        if (pendingMapFocus) {
            // Force selection of Map tab in ViewModel
            viewModel.setNavbarItemId(R.id.navbar_map);
            
            // If we are already on the map tab, the LiveData observer in setupObservers 
            // might not trigger because the value is the same. In that case, we force 
            // the fragment to reload with the new intent's arguments.
            if (bottomNavigationView.getSelectedItemId() == R.id.navbar_map) {
                loadFragment(createMapFragment());
            }
        }
    }

    private void configureBottomNavigation() {
        MenuItem mapItem = bottomNavigationView.getMenu().findItem(R.id.navbar_map);
        MenuItem postItem = bottomNavigationView.getMenu().findItem(R.id.navbar_post);

        if ("business".equals(userType)) {
            mapItem.setVisible(false);
            postItem.setVisible(true);
        } else {
            mapItem.setVisible(true);
            postItem.setVisible(true);
        }

        if ("business".equals(userType)) {
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
        args.putString(MapFragment.ARG_FOCUS_TITLE, getIntent().getStringExtra(EXTRA_MAP_FOCUS_NAME));
        args.putString(MapFragment.ARG_FOCUS_SUBTITLE, getIntent().getStringExtra(EXTRA_MAP_FOCUS_SUBTITLE));
        args.putBoolean(MapFragment.ARG_OPEN_ROUTE_PREVIEW, getIntent().getBooleanExtra(EXTRA_OPEN_MAP_ROUTE, false));
        args.putSerializable(MapFragment.ARG_ROUTE_LOCATIONS, getIntent().getSerializableExtra(EXTRA_MAP_ROUTE_LOCATIONS));
        fragment.setArguments(args);

        pendingMapFocus = false;
        // Clean up intent so these aren't re-processed on rotation
        getIntent().removeExtra(EXTRA_OPEN_MAP);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LATITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LONGITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_NAME);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_SUBTITLE);
        getIntent().removeExtra(EXTRA_OPEN_MAP_ROUTE);
        getIntent().removeExtra(EXTRA_MAP_ROUTE_LOCATIONS);
        return fragment;
    }
}
