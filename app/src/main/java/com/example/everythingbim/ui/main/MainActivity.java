package com.example.everythingbim.ui.main;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

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
import com.example.everythingbim.ui.home.AIIdentifier;
import com.example.everythingbim.ui.home.HomeFragment;
import com.example.everythingbim.ui.map.MapFragment;
import com.example.everythingbim.ui.onboarding.OnboardingOverlayView;
import com.example.everythingbim.ui.onboarding.OnboardingPreferences;
import com.example.everythingbim.ui.onboarding.OnboardingStep;
import com.example.everythingbim.ui.posts.PostFragment;
import com.example.everythingbim.ui.user.UserFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    public static final String USER_TYPE_GUEST = "guest";
    public static final String USER_TYPE_GENERAL = "general";
    public static final String USER_TYPE_BUSINESS = "business";
    public static final String USER_TYPE_ADMIN = "admin";
    public static final String EXTRA_PENDING_ACTION = "pending_action";
    public static final String ACTION_CREATE_POST = "action_create_post";
    public static final String ACTION_ADD_LOCATION_REQUEST = "action_add_location_request";
    public static final String ACTION_ADD_INFORMATION_REQUEST = "action_add_information_request";
    public static final String ACTION_ADD_BUSINESS_LOCATION_REQUEST = "action_add_business_location_request";
    public static final String ACTION_ADD_DATASET_SUBMISSION = "action_add_dataset_submission";
    public static final String ACTION_VIEW_COMPLETED_INFORMATION_REQUESTS = "action_view_completed_information_requests";
    public static final String ACTION_VIEW_COMPLETED_LOCATION_REQUESTS = "action_view_completed_location_requests";
    public static final String ACTION_VIEW_COMPLETED_LOCATION_TO_ADDRESS_REQUESTS = "action_view_completed_location_to_address_requests";
    public static final String ACTION_VIEW_COMPLETED_ACCOUNT_VERIFICATION_REQUESTS = "action_view_completed_account_verification_requests";

    // For Args for focusing location in Map Fragment
    public static final String EXTRA_OPEN_MAP_FOCUS = "open_map_focus";
    public static final String EXTRA_MAP_FOCUS_LOCATION_ID = "map_focus_location_id";
    public static final String EXTRA_MAP_FOCUS_LATITUDE = "map_focus_latitude";
    public static final String EXTRA_MAP_FOCUS_LONGITUDE = "map_focus_longitude";
    public static final String EXTRA_MAP_FOCUS_NAME = "map_focus_name";
    public static final String EXTRA_MAP_FOCUS_SUBTITLE = "map_focus_subtitle";
    public static final String EXTRA_OPEN_MAP_ROUTE = "open_map_route";
    public static final String EXTRA_MAP_ROUTE_LOCATIONS = "map_route_locations";

    private MainViewModel viewModel;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;
    private String userType; // "guest", "general" or "business"
    private boolean pendingMapFocus;
    private OnboardingPreferences onboardingPreferences;
    private OnboardingOverlayView onboardingOverlayView;
    private List<OnboardingStep> onboardingSteps;
    private int onboardingStepIndex = -1;

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
        onboardingPreferences = new OnboardingPreferences(this);

        // Retrieve user type from Intent or SharedPreferences
        if (getIntent().hasExtra("userType")) {
            userType = normalizeUserType(getIntent().getStringExtra("userType"));
            // Save it for future sessions
            sharedPreferences.edit().putString("userType", userType).apply();
        } else {
            userType = normalizeUserType(sharedPreferences.getString("userType", USER_TYPE_GUEST));
        }

        // Also save userId from intent if present (sent from Login after successful authentication)
        if (getIntent().hasExtra("userId")) {
            String userId = getIntent().getStringExtra("userId");
            sharedPreferences.edit().putString("userId", userId).apply();
        }

        // If SharedPreferences was cleared but Firebase Auth session persists, restore session
        if (USER_TYPE_GUEST.equals(userType) && sharedPreferences.getString("userId", "").isEmpty()) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String firebaseUid = auth.getCurrentUser().getUid();
                restoreUserSessionFromFirebaseAuth(firebaseUid);
            }
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
        resumePendingActionIfNeeded();
        maybeStartOnboarding();

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) {
            return;
        }

        setIntent(intent);

        if (intent.hasExtra("userType")) {
            userType = normalizeUserType(intent.getStringExtra("userType"));
            sharedPreferences.edit().putString("userType", userType).apply();
        }

        pendingMapFocus = intent.getBooleanExtra(EXTRA_OPEN_MAP_FOCUS, false)
                || intent.getBooleanExtra(EXTRA_OPEN_MAP_ROUTE, false);

        if (pendingMapFocus) {
            configureBottomNavigation();
            viewModel.setNavbarItemId(R.id.navbar_map);
            return;
        }

        resumePendingActionIfNeeded();
    }

    private void configureBottomNavigation() {
        // Hide or show certain menu items based on user type
        MenuItem mapItem = bottomNavigationView.getMenu().findItem(R.id.navbar_map);
        MenuItem postItem = bottomNavigationView.getMenu().findItem(R.id.navbar_post);

        mapItem.setVisible(true);
        postItem.setVisible(true);

        // Optionally set the default selection
        if (USER_TYPE_BUSINESS.equals(userType)) {
            // Possibly start with a different default fragment
            viewModel.setNavbarItemId(R.id.navbar_home);
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
        args.putLong(MapFragment.ARG_FOCUS_LOCATION_ID, getIntent().getLongExtra(EXTRA_MAP_FOCUS_LOCATION_ID, -1L));
        args.putDouble(MapFragment.ARG_FOCUS_LATITUDE, getIntent().getDoubleExtra(EXTRA_MAP_FOCUS_LATITUDE, 0d));
        args.putDouble(MapFragment.ARG_FOCUS_LONGITUDE, getIntent().getDoubleExtra(EXTRA_MAP_FOCUS_LONGITUDE, 0d));
        args.putString(MapFragment.ARG_FOCUS_TITLE, getIntent().getStringExtra(EXTRA_MAP_FOCUS_NAME));
        args.putString(MapFragment.ARG_FOCUS_SUBTITLE, getIntent().getStringExtra(EXTRA_MAP_FOCUS_SUBTITLE));

        args.putBoolean(MapFragment.ARG_OPEN_ROUTE_PREVIEW, getIntent().getBooleanExtra(EXTRA_OPEN_MAP_ROUTE, false));
        args.putSerializable(MapFragment.ARG_ROUTE_LOCATIONS, getIntent().getSerializableExtra(EXTRA_MAP_ROUTE_LOCATIONS));
        fragment.setArguments(args);

        pendingMapFocus = false;
        // Clean up intent so these aren't re-processed on rotation
        getIntent().removeExtra(EXTRA_OPEN_MAP_FOCUS);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LOCATION_ID);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LATITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LONGITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_NAME);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_SUBTITLE);

        getIntent().removeExtra(EXTRA_OPEN_MAP_ROUTE);
        getIntent().removeExtra(EXTRA_MAP_ROUTE_LOCATIONS);
        return fragment;
    }

    private String normalizeUserType(String rawUserType) {
        if (rawUserType == null) {
            return USER_TYPE_GUEST;
        }

        String normalized = rawUserType.trim().toLowerCase(java.util.Locale.US);
        if (USER_TYPE_BUSINESS.equals(normalized)) {
            return USER_TYPE_BUSINESS;
        }
        if (USER_TYPE_GENERAL.equals(normalized)) {
            return USER_TYPE_GENERAL;
        }
        if (USER_TYPE_ADMIN.equals(normalized)) {
            return USER_TYPE_ADMIN;
        }
        return USER_TYPE_GUEST;
    }

    private void restoreUserSessionFromFirebaseAuth(String firebaseUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Try users collection first
        db.collection("users").document(firebaseUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String userTypeFromDb = doc.getString("userType");
                        if (userTypeFromDb == null) userTypeFromDb = USER_TYPE_GENERAL;
                        restoreSession(userTypeFromDb, firebaseUid);
                    } else {
                        // Try businesses collection
                        db.collection("businesses").document(firebaseUid).get()
                                .addOnSuccessListener(bizDoc -> {
                                    if (bizDoc != null && bizDoc.exists()) {
                                        restoreSession(USER_TYPE_BUSINESS, firebaseUid);
                                    } else {
                                        // Try admin collection
                                        db.collection("admins").document(firebaseUid).get()
                                                .addOnSuccessListener(adminDoc -> {
                                                    if (adminDoc != null && adminDoc.exists()) {
                                                        restoreSession(USER_TYPE_ADMIN, firebaseUid);
                                                    }
                                                    // If nothing found, stay as guest
                                                });
                                    }
                                });
                    }
                });
    }

    private void restoreSession(String userType, String userId) {
        MainActivity.this.userType = userType;
        sharedPreferences.edit()
                .putString("userType", userType)
                .putString("userId", userId)
                .apply();
        android.util.Log.d("MainActivity", "Session restored: userType=" + userType + ", userId=" + userId);
        // Recreate activity to apply the restored session
        recreate();
    }

    public void handleLogout() {
        FirebaseAuth.getInstance().signOut();

        // Use commit() for synchronous write to SharedPreferences
        sharedPreferences.edit()
                .putString("userType", USER_TYPE_GUEST)
                .putString("userId", "")
                .commit();
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().commit();

        userType = USER_TYPE_GUEST;
        pendingMapFocus = false;

        getIntent().removeExtra("userType");
        getIntent().removeExtra(EXTRA_OPEN_MAP_FOCUS);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LOCATION_ID);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LATITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_LONGITUDE);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_NAME);
        getIntent().removeExtra(EXTRA_MAP_FOCUS_SUBTITLE);
        getIntent().removeExtra(EXTRA_OPEN_MAP_ROUTE);
        getIntent().removeExtra(EXTRA_MAP_ROUTE_LOCATIONS);

        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        configureBottomNavigation();
        viewModel.setNavbarItemId(R.id.navbar_home);

        android.widget.Toast.makeText(this, "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show();

        // Navigate to Login screen instead of closing app
        Intent loginIntent = new Intent(this, com.example.everythingbim.ui.login.Login.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void resumePendingActionIfNeeded() {
        String pendingAction = getIntent().getStringExtra(EXTRA_PENDING_ACTION);
        if (pendingAction == null || USER_TYPE_GUEST.equals(userType)) {
            return;
        }

        getIntent().removeExtra(EXTRA_PENDING_ACTION);

        if (ACTION_CREATE_POST.equals(pendingAction)) {
            startActivity(new Intent(this, com.example.everythingbim.ui.posts.CreatePostActivity.class));
            return;
        }

        if (ACTION_ADD_LOCATION_REQUEST.equals(pendingAction)) {
            openUserActionFragment(new com.example.everythingbim.ui.user.AddLocationRequestFragment());
            return;
        }

        if (ACTION_ADD_INFORMATION_REQUEST.equals(pendingAction)) {
            openUserActionFragment(new com.example.everythingbim.ui.user.AddInformationRequestFragment());
            return;
        }

        if (ACTION_ADD_BUSINESS_LOCATION_REQUEST.equals(pendingAction)) {
            if (USER_TYPE_BUSINESS.equals(userType)) {
                openUserActionFragment(new com.example.everythingbim.ui.user.AddBusinessLocationRequestFragment());
            } else {
                android.widget.Toast.makeText(this, "Business access is required for that action.", android.widget.Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (ACTION_ADD_DATASET_SUBMISSION.equals(pendingAction)) {
            openDatasetSubmissionResume();
            return;
        }

        if (ACTION_VIEW_COMPLETED_INFORMATION_REQUESTS.equals(pendingAction)) {
            openUserActionFragment(new com.example.everythingbim.ui.user.ViewCompletedInformationRequestFragment());
            return;
        }

        if (ACTION_VIEW_COMPLETED_LOCATION_REQUESTS.equals(pendingAction)) {
            openUserActionFragment(new com.example.everythingbim.ui.user.ViewCompletedLocationRequestFragment());
            return;
        }

        if (ACTION_VIEW_COMPLETED_LOCATION_TO_ADDRESS_REQUESTS.equals(pendingAction)) {
            openUserActionFragment(new com.example.everythingbim.ViewCompletedAddLocationToAddressFragment());
            return;
        }

        if (ACTION_VIEW_COMPLETED_ACCOUNT_VERIFICATION_REQUESTS.equals(pendingAction)) {
            openUserActionFragment(new com.example.everythingbim.ui.user.ViewCompletedAccountVerificationRequestFragment());
        }
    }

    private void openUserActionFragment(Fragment fragment) {
        viewModel.setNavbarItemId(R.id.navbar_user);
        bottomNavigationView.post(() -> getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit());
    }

    private void openDatasetSubmissionResume() {
        Intent intent = new Intent(this, AIIdentifier.class);
        copyIfPresent(getIntent(), intent, AIIdentifier.EXTRA_IMAGE_URI);
        copyIfPresent(getIntent(), intent, AIIdentifier.EXTRA_IMAGE_SOURCE);
        copyIfPresent(getIntent(), intent, AIIdentifier.EXTRA_DISPLAY_NAME);
        copyIfPresent(getIntent(), intent, AIIdentifier.EXTRA_GPS_AVAILABLE);
        copyIfPresent(getIntent(), intent, AIIdentifier.EXTRA_GPS_PERMISSION_GRANTED);
        copyIfPresent(getIntent(), intent, AIIdentifier.EXTRA_USER_LATITUDE);
        copyIfPresent(getIntent(), intent, AIIdentifier.EXTRA_USER_LONGITUDE);
        intent.putExtra(AIIdentifier.EXTRA_AUTO_OPEN_DATASET_SUBMISSION, true);
        startActivity(intent);
    }

    private void copyIfPresent(Intent source, Intent destination, String key) {
        if (source == null || destination == null || !source.hasExtra(key)) {
            return;
        }
        Object value = source.getExtras() != null ? source.getExtras().get(key) : null;
        if (value instanceof String) {
            destination.putExtra(key, (String) value);
        } else if (value instanceof Boolean) {
            destination.putExtra(key, (Boolean) value);
        } else if (value instanceof Double) {
            destination.putExtra(key, (Double) value);
        }
    }

    private void maybeStartOnboarding() {
        if (!USER_TYPE_GUEST.equals(userType) || onboardingPreferences.hasSeenGuestTour()) {
            return;
        }

        buildUniversalOnboardingSteps();

        findViewById(R.id.main).post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            showOnboardingStep(0);
        });
    }

    public void replayOnboarding() {
        buildUniversalOnboardingSteps();
        showOnboardingStep(0);
    }

    private void buildUniversalOnboardingSteps() {
        onboardingSteps = Arrays.asList(
                new OnboardingStep(
                        "Welcome to Everything BIM",
                        "Explore Barbados, discover places, and decide when you want to sign in for more.",
                        OnboardingStep.LayoutStyle.CENTER,
                        null
                ),
                new OnboardingStep(
                        "Home",
                        "Start here to explore landmark discovery and the app's main entry experience.",
                        OnboardingStep.LayoutStyle.BOTTOM,
                        R.id.navbar_home
                ),
                new OnboardingStep(
                        "Choose how to begin",
                        "Use the camera for a live photo or pick an image from your gallery to identify a location.",
                        OnboardingStep.LayoutStyle.AUTO,
                        R.id.navbar_home,
                        R.id.home_action_container
                ),
                new OnboardingStep(
                        "Map",
                        "Use the map to search places, inspect details, and explore Barbados visually.",
                        OnboardingStep.LayoutStyle.BOTTOM,
                        R.id.navbar_map
                ),
                new OnboardingStep(
                        "Posts",
                        "Browse community posts here, and sign in later when you're ready to share your own.",
                        OnboardingStep.LayoutStyle.BOTTOM,
                        R.id.navbar_post
                ),
                new OnboardingStep(
                        "User",
                        "Open User to log in, create an account, or manage profile and request features.",
                        OnboardingStep.LayoutStyle.BOTTOM,
                        R.id.navbar_user
                ),
                new OnboardingStep(
                        "You're all set",
                        "Need a refresher later? Use Replay Tour in Settings anytime.",
                        OnboardingStep.LayoutStyle.AUTO,
                        R.id.navbar_user,
                        R.id.guest_replay_tour_btn
                )
        );
    }

    private void showOnboardingStep(int index) {
        if (onboardingSteps == null || index < 0 || index >= onboardingSteps.size()) {
            finishOnboarding(true);
            return;
        }

        onboardingStepIndex = index;
        OnboardingStep step = onboardingSteps.get(index);
        Integer navItemId = step.getNavigationItemId();
        if (navItemId != null) {
            viewModel.setNavbarItemId(navItemId);
        }

        View root = findViewById(R.id.main);
        root.post(() -> renderOnboardingStep(step, index, 8));
    }

    private void renderOnboardingStep(OnboardingStep step, int index, int attemptsRemaining) {
        if (onboardingOverlayView == null) {
            onboardingOverlayView = new OnboardingOverlayView(this);
        }

        View root = findViewById(R.id.main);
        if (onboardingOverlayView.getParent() == null && root instanceof FrameLayout) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            ((FrameLayout) root).addView(onboardingOverlayView, params);
        } else if (onboardingOverlayView.getParent() == null && root instanceof android.view.ViewGroup) {
            android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
            );
            ((android.view.ViewGroup) root).addView(onboardingOverlayView, params);
        }

        onboardingOverlayView.post(() -> {
            if (onboardingOverlayView.getWidth() == 0 || onboardingOverlayView.getHeight() == 0) {
                if (attemptsRemaining > 0) {
                    root.postDelayed(() -> renderOnboardingStep(step, index, attemptsRemaining - 1), 100L);
                }
                return;
            }

            prepareOnboardingTarget(step);
            View targetView = resolveOnboardingTarget(step);
            if (step.getTargetViewId() != null && targetView == null && attemptsRemaining > 0) {
                root.postDelayed(() -> renderOnboardingStep(step, index, attemptsRemaining - 1), 100L);
                return;
            }

            onboardingOverlayView.render(step, index == onboardingSteps.size() - 1, targetView, new OnboardingOverlayView.Listener() {
                @Override
                public void onNext() {
                    if (onboardingStepIndex >= onboardingSteps.size() - 1) {
                        finishOnboarding(true);
                    } else {
                        showOnboardingStep(onboardingStepIndex + 1);
                    }
                }

                @Override
                public void onSkip() {
                    finishOnboarding(true);
                }
            });
        });
    }

    private View resolveOnboardingTarget(OnboardingStep step) {
        Integer targetViewId = step.getTargetViewId();
        if (targetViewId != null && targetViewId == R.id.guest_replay_tour_btn) {
            return findVisibleReplayTarget();
        }

        if (targetViewId != null) {
            View directTarget = findViewById(targetViewId);
            if (isUsableOnboardingTarget(directTarget)) {
                return directTarget;
            }

            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null && currentFragment.getView() != null) {
                View fragmentTarget = currentFragment.getView().findViewById(targetViewId);
                if (isUsableOnboardingTarget(fragmentTarget)) {
                    return fragmentTarget;
                }
            }
        }

        Integer navItemId = step.getNavigationItemId();
        View navTarget = navItemId == null ? null : findBottomNavItemView(navItemId);
        return isUsableOnboardingTarget(navTarget) ? navTarget : null;
    }

    private void prepareOnboardingTarget(OnboardingStep step) {
        Integer targetViewId = step.getTargetViewId();
        if (targetViewId == null) {
            return;
        }

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof UserFragment) {
            ((UserFragment) currentFragment).showReplayTourLocation();
        }
    }

    private View findVisibleReplayTarget() {
        int[] candidateIds = new int[] {
                R.id.guest_replay_tour_btn,
                R.id.general_replay_tour_btn,
                R.id.business_replay_tour_btn
        };

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        for (int candidateId : candidateIds) {
            View candidate = findViewById(candidateId);
            if (isUsableOnboardingTarget(candidate)) {
                return candidate;
            }

            if (currentFragment != null && currentFragment.getView() != null) {
                View fragmentCandidate = currentFragment.getView().findViewById(candidateId);
                if (isUsableOnboardingTarget(fragmentCandidate)) {
                    return fragmentCandidate;
                }
            }
        }

        return null;
    }

    private boolean isUsableOnboardingTarget(View candidate) {
        return candidate != null
                && candidate.getVisibility() == View.VISIBLE
                && candidate.getWidth() > 0
                && candidate.getHeight() > 0;
    }

    private View findBottomNavItemView(int navItemId) {
        View directTarget = bottomNavigationView.findViewById(navItemId);
        if (isUsableOnboardingTarget(directTarget)) {
            return directTarget;
        }

        return findViewByIdRecursive(bottomNavigationView, navItemId);
    }

    private View findViewByIdRecursive(View root, int targetId) {
        if (root == null) {
            return null;
        }
        if (root.getId() == targetId && isUsableOnboardingTarget(root)) {
            return root;
        }
        if (!(root instanceof android.view.ViewGroup)) {
            return null;
        }

        android.view.ViewGroup group = (android.view.ViewGroup) root;
        for (int index = 0; index < group.getChildCount(); index++) {
            View match = findViewByIdRecursive(group.getChildAt(index), targetId);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private void finishOnboarding(boolean markSeen) {
        if (markSeen) {
            onboardingPreferences.setGuestTourSeen(true);
        }

        onboardingStepIndex = -1;
        onboardingSteps = null;

        if (onboardingOverlayView != null && onboardingOverlayView.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) onboardingOverlayView.getParent()).removeView(onboardingOverlayView);
        }
        onboardingOverlayView = null;
    }
}
