package com.example.everythingbim;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

public class AdminSettingsFragment extends Fragment {

    // UI components
    private ImageButton editUsernameBtn, editEmailBtn, editPasswordBtn;
    private LinearLayout usernameContainer, emailContainer;
    private RelativeLayout passwordContainer;
    private TextInputEditText usernameField, emailField, passwordField;

    // Edit state flags
    private boolean isEditingUsername = false;
    private boolean isEditingEmail = false;
    private boolean isEditingPassword = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_settings, container, false);

        // Initialize views
        initViews(root);
        setupEditButtons();

        return root;
    }

    private void initViews(View root) {
        editUsernameBtn = root.findViewById(R.id.admin_edit_username_btn);
        editEmailBtn = root.findViewById(R.id.admin_edit_email_btn);
        editPasswordBtn = root.findViewById(R.id.admin_edit_password_btn);

        usernameContainer = root.findViewById(R.id.username_container);
        emailContainer = root.findViewById(R.id.email_container);
        passwordContainer = root.findViewById(R.id.password_container);

        usernameField = root.findViewById(R.id.admin_username_et);
        emailField = root.findViewById(R.id.admin_email_et);
        passwordField = root.findViewById(R.id.admin_password_et);
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
}