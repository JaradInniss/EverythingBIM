package com.example.everythingbim.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import com.example.everythingbim.R;

public class AdminSettingsFragment extends Fragment {
    private static final String PREF_ADMIN_SETTINGS_SCROLL_HINT_SEEN = "admin_settings_scroll_hint_seen";

    // UI components
    private ImageButton editUsernameBtn, editEmailBtn, editPasswordBtn, eyeBtn;
    private LinearLayout usernameContainer, emailContainer;
    private RelativeLayout passwordContainer;
    private TextInputEditText usernameField, emailField, passwordField;
    private ScrollView settingsScrollView;
    private int currentKeyboardExtraBottom = 0;

    // Edit state flags
    private boolean isEditingUsername = false;
    private boolean isEditingEmail = false;
    private boolean isEditingPassword = false;

    // Password visibility state
    private boolean isPasswordVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_settings, container, false);

        // Initialize views
        initViews(root);
        setupKeyboardInsets(root);
        setupEditButtons();
        setupPasswordToggle();

        return root;
    }

    private void initViews(View root) {
        editUsernameBtn = root.findViewById(R.id.admin_edit_username_btn);
        editEmailBtn = root.findViewById(R.id.admin_edit_email_btn);
        editPasswordBtn = root.findViewById(R.id.admin_edit_password_btn);
        eyeBtn = root.findViewById(R.id.admin_password_eye_btn);

        usernameContainer = root.findViewById(R.id.username_container);
        emailContainer = root.findViewById(R.id.email_container);
        passwordContainer = root.findViewById(R.id.password_container);
        settingsScrollView = root.findViewById(R.id.admin_settings_scroll);

        usernameField = root.findViewById(R.id.admin_username_et);
        emailField = root.findViewById(R.id.admin_email_et);
        passwordField = root.findViewById(R.id.admin_password_et);

        // Logout button
        View logoutBtn = root.findViewById(R.id.admin_logout_btn);
        logoutBtn.setOnClickListener(v -> performLogout());
    }

    private void setupKeyboardInsets(View root) {
        if (settingsScrollView == null) {
            return;
        }

        int initialLeft = settingsScrollView.getPaddingLeft();
        int initialTop = settingsScrollView.getPaddingTop();
        int initialRight = settingsScrollView.getPaddingRight();
        int initialBottom = settingsScrollView.getPaddingBottom();

        KeyboardScrollHintHelper.attach(
                root,
                settingsScrollView,
                settingsScrollView,
                PREF_ADMIN_SETTINGS_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> {
                    currentKeyboardExtraBottom = keyboardExtraBottom;
                    settingsScrollView.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + keyboardExtraBottom
                    );
                }
        );
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void setupEditButtons() {
        editUsernameBtn.setOnClickListener(v -> toggleEditMode(
                isEditingUsername,
                editUsernameBtn,
                usernameContainer,
                usernameField,
                R.drawable.bg_admin_edit_btn,
                R.drawable.bg_admin_edit_btn_grey,
                R.drawable.bg_admin_field_highlight,
                R.drawable.bg_admin_field,
                () -> isEditingUsername = !isEditingUsername
        ));

        editEmailBtn.setOnClickListener(v -> toggleEditMode(
                isEditingEmail,
                editEmailBtn,
                emailContainer,
                emailField,
                R.drawable.bg_admin_edit_btn,
                R.drawable.bg_admin_edit_btn_grey,
                R.drawable.bg_admin_field_highlight,
                R.drawable.bg_admin_field,
                () -> isEditingEmail = !isEditingEmail
        ));

        editPasswordBtn.setOnClickListener(v -> toggleEditMode(
                isEditingPassword,
                editPasswordBtn,
                passwordContainer,
                passwordField,
                R.drawable.bg_admin_edit_btn,
                R.drawable.bg_admin_edit_btn_grey,
                R.drawable.bg_admin_field_highlight,
                R.drawable.bg_admin_field,
                () -> isEditingPassword = !isEditingPassword
        ));
    }

    /**
     * Helper method to toggle edit mode for a field.
     *
     * @param isCurrentlyEditing Current edit state (true = editing)
     * @param editButton The edit button (ImageButton)
     * @param fieldContainer The container (LinearLayout or RelativeLayout) that holds the label and field
     * @param textField The TextInputEditText to enable/disable
     * @param activeButtonBg Drawable for active edit button (blue)
     * @param inactiveButtonBg Drawable for inactive edit button (grey)
     * @param activeBorder Drawable for highlighted field border (blue stroke)
     * @param inactiveBorder Drawable for normal field border (grey stroke)
     * @param toggleState Runnable to flip the boolean state after operation
     */
    private void toggleEditMode(boolean isCurrentlyEditing,
                                ImageButton editButton,
                                View fieldContainer,
                                TextInputEditText textField,
                                int activeButtonBg,
                                int inactiveButtonBg,
                                int activeBorder,
                                int inactiveBorder,
                                Runnable toggleState) {
        if (!isCurrentlyEditing) {
            // Activate edit mode
            editButton.setBackgroundResource(activeButtonBg);
            editButton.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            fieldContainer.setBackgroundResource(activeBorder);

            textField.setFocusable(true);
            textField.setFocusableInTouchMode(true);
            textField.setClickable(true);
            textField.requestFocus();
            scrollAnchorAboveKeyboard(fieldContainer);

            // Show keyboard
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(textField, InputMethodManager.SHOW_IMPLICIT);
            }
        } else {
            // Deactivate edit mode
            editButton.setBackgroundResource(inactiveButtonBg);
            editButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.dark)));
            fieldContainer.setBackgroundResource(inactiveBorder);

            textField.setFocusable(false);
            textField.setFocusableInTouchMode(false);
            textField.setClickable(false);

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(textField.getWindowToken(), 0);
            }
        }
        // Flip the state after the operation
        toggleState.run();
    }

    private void scrollAnchorAboveKeyboard(View anchorView) {
        if (settingsScrollView == null) {
            return;
        }

        settingsScrollView.post(() -> {
            if (currentKeyboardExtraBottom <= 0) {
                return;
            }
            if (!isDescendant(settingsScrollView, anchorView)) {
                return;
            }

            Rect rect = new Rect();
            anchorView.getDrawingRect(rect);
            settingsScrollView.offsetDescendantRectToMyCoords(anchorView, rect);

            int visibleHeight = settingsScrollView.getHeight() - currentKeyboardExtraBottom;
            int desiredBottomMargin = dpToPx(24);
            int targetBottom = visibleHeight - desiredBottomMargin;
            int delta = rect.bottom - targetBottom;
            if (delta > 0) {
                settingsScrollView.smoothScrollBy(0, delta);
            }
        });
    }

    private boolean isDescendant(ViewGroup parent, View child) {
        ViewParent p = child.getParent();
        while (p != null) {
            if (p == parent) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void setupPasswordToggle() {
        eyeBtn.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide password
                passwordField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                eyeBtn.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.dark)));
            } else {
                // Show password
                passwordField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                eyeBtn.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.prussian_blue)));
            }
            // Keep cursor at the end
            passwordField.setSelection(passwordField.getText() != null ? passwordField.getText().length() : 0);
            isPasswordVisible = !isPasswordVisible;
        });
    }
}
