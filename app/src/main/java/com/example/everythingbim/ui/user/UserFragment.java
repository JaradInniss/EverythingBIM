package com.example.everythingbim.ui.user;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.login.Login;
import com.google.android.material.textfield.TextInputEditText;

public class UserFragment extends Fragment {

    // ─── User type root layouts ──────────────────
    private View layoutGeneralUser;
    private View layoutBusinessUser;

    // ─── General tab content panels ─────────────
    private View generalContentSubmissions;
    private View generalContentMyProfile;
    private View generalContentSettings;

    // ─── General tab indicators ──────────────────
    private View generalIndSubmissions;
    private View generalIndMyProfile;
    private View generalIndSettings;

    // ─── Business tab content panels ────────────
    private View businessContentSubmissions;
    private View businessContentMyProfile;
    private View businessContentSettings;

    // ─── Business tab indicators ─────────────────
    private View businessIndSubmissions;
    private View businessIndMyProfile;
    private View businessIndSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Bind user type layouts
        layoutGeneralUser  = view.findViewById(R.id.layout_general_user);
        layoutBusinessUser = view.findViewById(R.id.layout_business_user);

        // Bind general tab content
        generalContentSubmissions = view.findViewById(R.id.general_content_submissions);
        generalContentMyProfile   = view.findViewById(R.id.general_content_my_profile);
        generalContentSettings    = view.findViewById(R.id.general_content_settings);

        // Bind general tab indicators
        generalIndSubmissions = view.findViewById(R.id.general_tab_submissions_indicator);
        generalIndMyProfile   = view.findViewById(R.id.general_tab_my_profile_indicator);
        generalIndSettings    = view.findViewById(R.id.general_tab_settings_indicator);

        // Bind business tab content
        businessContentSubmissions = view.findViewById(R.id.business_content_submissions);
        businessContentMyProfile   = view.findViewById(R.id.business_content_my_profile);
        businessContentSettings    = view.findViewById(R.id.business_content_settings);

        // Bind business tab indicators
        businessIndSubmissions = view.findViewById(R.id.business_tab_submissions_indicator);
        businessIndMyProfile   = view.findViewById(R.id.business_tab_my_profile_indicator);
        businessIndSettings    = view.findViewById(R.id.business_tab_settings_indicator);

        // Show the correct layout based on who is logged in
        switchUserLayout(getUserType());

        // Setup tabs for both layouts
        setupGeneralUserTabs(view);
        setupBusinessUserTabs(view);

        view.findViewById(R.id.general_user_btnLogout).setOnClickListener(v ->
                performLogout());

        view.findViewById(R.id.btnLogout).setOnClickListener(v ->
                performLogout());

        // ── General User navigation buttons ──────
        view.findViewById(R.id.locreq_new_btn).setOnClickListener(v ->
                navigateTo(new AddLocationRequestFragment()));

        view.findViewById(R.id.locreq_view_btn).setOnClickListener(v ->
                navigateTo(new ViewAddLocationRequestFragment()));

        view.findViewById(R.id.locreq_view_btn_2).setOnClickListener(v ->
                navigateTo(new ViewCompletedLocationRequestFragment()));

        view.findViewById(R.id.inforeq_new_btn).setOnClickListener(v -> {
            // TODO: navigate to Add Information Request screen
        });

        view.findViewById(R.id.inforeq_view_btn).setOnClickListener(v -> {
            // TODO: navigate to View Information Request screen
        });

        view.findViewById(R.id.inforeq_view_btn_2).setOnClickListener(v -> {
            // TODO: navigate to View Completed Information Request screen
        });

        // ── Business User navigation buttons ─────
        view.findViewById(R.id.accver_view_btn).setOnClickListener(v -> {
            // TODO: navigate to View Account Verification screen
        });

        view.findViewById(R.id.accver_view_btn_2).setOnClickListener(v -> {
            // TODO: navigate to View Completed Account Verification screen
        });

        view.findViewById(R.id.addloc_view_btn).setOnClickListener(v -> {
            // TODO: navigate to View Add Location to Address screen
        });

        view.findViewById(R.id.addloc_view_btn_2).setOnClickListener(v -> {
            // TODO: navigate to View Completed Add Location to Address screen
        });

        // ── General User edit field toggles ──────
        setupEditToggle(view,
                R.id.general_user_edit_username_et,
                R.id.general_user_edit_username_btn);
        setupEditToggle(view,
                R.id.general_user_edit_email_et,
                R.id.general_user_edit_email_btn);
        setupEditToggle(view,
                R.id.general_user_edit_password_et,
                R.id.general_user_edit_password_btn);

        // ── Business User edit field toggles ─────
        setupEditToggle(view,
                R.id.business_edit_email_et,
                R.id.business_edit_email_btn);
        setupEditToggle(view,
                R.id.business_edit_password_et,
                R.id.business_edit_password_btn);
        setupEditToggle(view,
                R.id.business_edit_address_et,
                R.id.business_edit_address_btn);
        setupEditToggle(view,
                R.id.business_edit_desc_et,
                R.id.business_edit_desc_btn);

        return view;
    }

    // Inside the class, add a method to perform logout
    private void performLogout() {
        // Clear all user data from SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user_prefs", requireActivity().MODE_PRIVATE);
        prefs.edit().clear().apply();

//        Intent intent = new Intent(requireContext(), Login.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);

        requireActivity().finish();
    }


    private String getUserType() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user_prefs", requireActivity().MODE_PRIVATE);
        return prefs.getString("user_type", "General");
    }

    private void switchUserLayout(String userType) {
        if (userType.equals("Business")) {
            layoutGeneralUser.setVisibility(View.GONE);
            layoutBusinessUser.setVisibility(View.VISIBLE);
        } else {
            layoutGeneralUser.setVisibility(View.VISIBLE);
            layoutBusinessUser.setVisibility(View.GONE);
        }
    }

    private void setupGeneralUserTabs(View view) {
        // Default to Submissions on load
        switchGeneralTab(0);

        view.findViewById(R.id.general_tab_submissions).setOnClickListener(v ->
                switchGeneralTab(0));
        view.findViewById(R.id.general_tab_my_profile).setOnClickListener(v ->
                switchGeneralTab(1));
        view.findViewById(R.id.general_tab_settings).setOnClickListener(v ->
                switchGeneralTab(2));
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
        // Default to Submissions on load
        switchBusinessTab(0);

        view.findViewById(R.id.business_tab_submissions).setOnClickListener(v ->
                switchBusinessTab(0));
        view.findViewById(R.id.business_tab_my_profile).setOnClickListener(v ->
                switchBusinessTab(1));
        view.findViewById(R.id.business_tab_settings).setOnClickListener(v ->
                switchBusinessTab(2));
    }

    private void switchBusinessTab(int tab) {
        businessContentSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        businessContentMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        businessContentSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        businessIndSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        businessIndMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        businessIndSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
    }

    private void setupEditToggle(View root, int fieldId, int buttonId) {
        TextInputEditText field = root.findViewById(fieldId);
        ImageButton button      = root.findViewById(buttonId);
        if (field == null || button == null) return;
        button.setOnClickListener(v -> toggleFieldEdit(field, button));
    }

    private void toggleFieldEdit(TextInputEditText field, ImageButton button) {
        if (!field.isFocusable()) {
            // Enable editing
            field.setFocusable(true);
            field.setFocusableInTouchMode(true);
            field.setClickable(true);
            field.requestFocus();
            button.setBackgroundResource(R.drawable.bg_rectangle_blue);
            button.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            ));
        } else {
            // Save / disable editing
            field.setFocusable(false);
            field.setFocusableInTouchMode(false);
            field.setClickable(false);
            button.setBackgroundResource(R.drawable.bg_rectangle_pale_slate);
            button.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
            ));

            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireActivity().getSystemService(
                                    android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(field.getWindowToken(), 0);

            // TODO: save updated field value to backend here
        }
    }

    // ────────────────────────────────────────────────────────
    // NAVIGATION HELPER
    // ────────────────────────────────────────────────────────

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}