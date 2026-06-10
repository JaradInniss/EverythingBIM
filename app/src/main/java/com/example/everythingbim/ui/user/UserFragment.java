package com.example.everythingbim.ui.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.data.repository.PostRepository;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.posts.PostAdapter;
import com.example.everythingbim.ui.posts.ViewUserProfileActivity;
import com.example.everythingbim.ui.registration.BusinessRegistration;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class UserFragment extends Fragment {

    private View layoutGuestUser;
    private View layoutGeneralUser;
    private View layoutBusinessUser;

    private View generalContentSubmissions;
    private View generalContentMyProfile;
    private View generalContentSettings;

    private View generalIndSubmissions;
    private View generalIndMyProfile;
    private View generalIndSettings;

    private View businessContentSubmissions;
    private View businessContentMyProfile;
    private View businessContentSettings;

    private View businessIndSubmissions;
    private View businessIndMyProfile;
    private View businessIndSettings;

    private RecyclerView generalUserPostsGrid;
    private RecyclerView businessUserPostsGrid;
    private PostAdapter generalPostsAdapter;
    private PostAdapter businessPostsAdapter;

    private PostRepository postRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user, container, false);

        layoutGuestUser = view.findViewById(R.id.layout_guest_user);
        layoutGeneralUser = view.findViewById(R.id.layout_general_user);
        layoutBusinessUser = view.findViewById(R.id.layout_business_user);

        generalContentSubmissions = view.findViewById(R.id.general_content_submissions);
        generalContentMyProfile = view.findViewById(R.id.general_content_my_profile);
        generalContentSettings = view.findViewById(R.id.general_content_settings);

        generalIndSubmissions = view.findViewById(R.id.general_tab_submissions_indicator);
        generalIndMyProfile = view.findViewById(R.id.general_tab_my_profile_indicator);
        generalIndSettings = view.findViewById(R.id.general_tab_settings_indicator);

        businessContentSubmissions = view.findViewById(R.id.business_content_submissions);
        businessContentMyProfile = view.findViewById(R.id.business_content_my_profile);
        businessContentSettings = view.findViewById(R.id.business_content_settings);

        businessIndSubmissions = view.findViewById(R.id.business_tab_submissions_indicator);
        businessIndMyProfile = view.findViewById(R.id.business_tab_my_profile_indicator);
        businessIndSettings = view.findViewById(R.id.business_tab_settings_indicator);

        // Profile posts grids (now RecyclerViews)
        generalUserPostsGrid = view.findViewById(R.id.general_user_posts_grid);
        businessUserPostsGrid = view.findViewById(R.id.business_user_posts_grid);

        setupPostsGrids();

        switchUserLayout(getUserType());
        setupGeneralUserTabs(view);
        setupBusinessUserTabs(view);

        view.findViewById(R.id.general_user_btnLogout).setOnClickListener(v -> performLogout());
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
        view.findViewById(R.id.general_replay_tour_btn).setOnClickListener(v -> replayTour());
        view.findViewById(R.id.business_replay_tour_btn).setOnClickListener(v -> replayTour());

        view.findViewById(R.id.guest_login_btn).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), Login.class)));
        view.findViewById(R.id.guest_register_general_btn).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), GeneralRegistration.class)));
        view.findViewById(R.id.guest_register_business_btn).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BusinessRegistration.class)));
        view.findViewById(R.id.guest_admin_access_btn).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Login.class);
            intent.putExtra("preselectedUserType", UserType.ADMIN.name());
            startActivity(intent);
        });
        view.findViewById(R.id.guest_replay_tour_btn).setOnClickListener(v -> replayTour());

        view.findViewById(R.id.locreq_new_btn).setOnClickListener(v ->
                navigateTo(new AddLocationRequestFragment()));
        view.findViewById(R.id.locreq_view_btn).setOnClickListener(v ->
                navigateTo(new ViewAddLocationRequestFragment()));
        view.findViewById(R.id.locreq_view_btn_2).setOnClickListener(v ->
                navigateTo(new ViewCompletedLocationRequestFragment()));

        view.findViewById(R.id.inforeq_new_btn).setOnClickListener(v ->
                navigateTo(new AddInformationRequestFragment()));
        view.findViewById(R.id.inforeq_view_btn).setOnClickListener(v ->
                navigateTo(new ViewAddInformationRequestFragment()));
        view.findViewById(R.id.inforeq_view_btn_2).setOnClickListener(v ->
                navigateTo(new ViewCompletedInformationRequestFragment()));

        view.findViewById(R.id.accver_view_btn).setOnClickListener(v ->
                navigateTo(new ViewAccountVerificationRequestFragment()));
        view.findViewById(R.id.accver_view_btn_2).setOnClickListener(v ->
                navigateTo(new ViewCompletedAccountVerificationRequestFragment()));

        view.findViewById(R.id.addloc_view_btn).setOnClickListener(v ->
                navigateTo(new com.example.everythingbim.ViewAddLocationToAddressFragment()));
        view.findViewById(R.id.addloc_view_btn_2).setOnClickListener(v ->
                navigateTo(new com.example.everythingbim.ViewCompletedAddLocationToAddressFragment()));

        view.findViewById(R.id.business_add_field_btn).setOnClickListener(v ->
                navigateTo(new AddBusinessLocationRequestFragment()));

        setupEditToggle(view, R.id.general_user_edit_username_et, R.id.general_user_edit_username_btn);
        setupEditToggle(view, R.id.general_user_edit_email_et, R.id.general_user_edit_email_btn);
        setupEditToggle(view, R.id.general_user_edit_password_et, R.id.general_user_edit_password_btn);
        setupEditToggle(view, R.id.general_user_bio_et, R.id.general_user_edit_bio_btn);

        setupEditToggle(view, R.id.business_edit_email_et, R.id.business_edit_email_btn);
        setupEditToggle(view, R.id.business_edit_password_et, R.id.business_edit_password_btn);
        setupEditToggle(view, R.id.business_edit_address_et, R.id.business_edit_address_btn);
        setupEditToggle(view, R.id.business_edit_desc_et, R.id.business_edit_desc_btn);
        setupEditToggle(view, R.id.business_user_bio_et, R.id.business_user_edit_bio_btn);

        return view;
    }

    /**
     * Initializes the two profile-post RecyclerViews. Tapping a thumbnail in
     * either grid opens the post detail screen for that post.
     */
    private void setupPostsGrids() {
        generalPostsAdapter = new PostAdapter();
        businessPostsAdapter = new PostAdapter();

        generalUserPostsGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        businessUserPostsGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        generalUserPostsGrid.setAdapter(generalPostsAdapter);
        businessUserPostsGrid.setAdapter(businessPostsAdapter);

        // Reuse the same click handler for both grids.
        PostAdapter.OnPostClickListener openPost = post -> {
            Intent intent = new Intent(requireContext(), ViewUserProfileActivity.class);
            intent.putExtra("USER_ID", post.authorId);
            startActivity(intent);
        };
        generalPostsAdapter.setOnPostClickListener(openPost);
        businessPostsAdapter.setOnPostClickListener(openPost);

        // requireContext() returns a Context which is what PostRepository needs.
        postRepository = new PostRepository(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the grids every time the user returns to the tab so freshly
        // created posts show up without requiring a full app restart.
        loadCurrentUserPosts();
    }

    /**
     * Pulls the current Firebase user's posts from Firestore and pushes them
     * into whichever grid is currently visible (general vs. business).
     * Guests fall through silently - the guest layout has no grids.
     */
    private void loadCurrentUserPosts() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Not signed in (guest). Clear any stale posts and bail.
            generalPostsAdapter.setPosts(new ArrayList<>());
            businessPostsAdapter.setPosts(new ArrayList<>());
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        postRepository.getPostsByAuthorUid(uid).observe(getViewLifecycleOwner(), posts -> {
            if (posts == null) return;
            String userType = getUserType();
            if (MainActivity.USER_TYPE_GENERAL.equals(userType)) {
                generalPostsAdapter.setPosts(posts);
            } else if (MainActivity.USER_TYPE_BUSINESS.equals(userType)) {
                businessPostsAdapter.setPosts(posts);
            }
        });
    }

    private void performLogout() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).handleLogout();
        }
    }

    private String getUserType() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        return prefs.getString("userType", MainActivity.USER_TYPE_GUEST);
    }

    private void switchUserLayout(String userType) {
        if (MainActivity.USER_TYPE_BUSINESS.equals(userType)) {
            layoutGuestUser.setVisibility(View.GONE);
            layoutGeneralUser.setVisibility(View.GONE);
            layoutBusinessUser.setVisibility(View.VISIBLE);
        } else if (MainActivity.USER_TYPE_GENERAL.equals(userType)) {
            layoutGuestUser.setVisibility(View.GONE);
            layoutGeneralUser.setVisibility(View.VISIBLE);
            layoutBusinessUser.setVisibility(View.GONE);
        } else {
            layoutGuestUser.setVisibility(View.VISIBLE);
            layoutGeneralUser.setVisibility(View.GONE);
            layoutBusinessUser.setVisibility(View.GONE);
        }
    }

    private void setupGeneralUserTabs(View view) {
        switchGeneralTab(0);

        view.findViewById(R.id.general_tab_submissions).setOnClickListener(v -> switchGeneralTab(0));
        view.findViewById(R.id.general_tab_my_profile).setOnClickListener(v -> switchGeneralTab(1));
        view.findViewById(R.id.general_tab_settings).setOnClickListener(v -> switchGeneralTab(2));
    }

    private void switchGeneralTab(int tab) {
        generalContentSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        generalContentMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        generalContentSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        generalIndSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        generalIndMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        generalIndSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
    }

    private void setupBusinessUserTabs(View view) {
        switchBusinessTab(0);

        view.findViewById(R.id.business_tab_submissions).setOnClickListener(v -> switchBusinessTab(0));
        view.findViewById(R.id.business_tab_my_profile).setOnClickListener(v -> switchBusinessTab(1));
        view.findViewById(R.id.business_tab_settings).setOnClickListener(v -> switchBusinessTab(2));
    }

    private void switchBusinessTab(int tab) {
        businessContentSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        businessContentMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        businessContentSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        businessIndSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        businessIndMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        businessIndSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
    }

    public void showReplayTourLocation() {
        String currentUserType = getUserType();
        if (MainActivity.USER_TYPE_GENERAL.equals(currentUserType)) {
            switchGeneralTab(2);
        } else if (MainActivity.USER_TYPE_BUSINESS.equals(currentUserType)) {
            switchBusinessTab(2);
        }
    }

    private void setupEditToggle(View root, int fieldId, int buttonId) {
        TextInputEditText field = root.findViewById(fieldId);
        ImageButton button = root.findViewById(buttonId);
        if (field == null || button == null) {
            return;
        }
        button.setOnClickListener(v -> toggleFieldEdit(field, button));
    }

    private void toggleFieldEdit(TextInputEditText field, ImageButton button) {
        ViewParent parent = field.getParent();
        TextInputLayout fieldLayout = (parent instanceof TextInputLayout) ? (TextInputLayout) parent : null;
        if (!field.isFocusable()) {
            field.setFocusable(true);
            field.setFocusableInTouchMode(true);
            field.setClickable(true);
            field.requestFocus();
            button.setBackgroundResource(R.drawable.bg_rectangle_blue);
            button.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            ));
            if (fieldLayout != null) {
                fieldLayout.setBoxStrokeColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.persian_blue)
                );
            }
        } else {
            field.setFocusable(false);
            field.setFocusableInTouchMode(false);
            field.setClickable(false);
            button.setBackgroundResource(R.drawable.bg_rectangle_edit_btn);
            button.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
            ));
            if (fieldLayout != null) {
                fieldLayout.setBoxStrokeColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.light_grey)
                );
            }

            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(field.getWindowToken(), 0);

            // TODO: save updated field value to backend here
        }
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void replayTour() {
        showReplayTourLocation();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).replayOnboarding();
        }
    }
}
