package com.example.everythingbim.ui.user;

import android.content.Intent;
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
import com.google.android.material.textfield.TextInputEditText;

public class UserFragment extends Fragment {

    private View layoutGeneralUser;
    private View layoutBusinessUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user, container, false);

        layoutGeneralUser  = view.findViewById(R.id.layout_general_user);
        layoutBusinessUser = view.findViewById(R.id.layout_business_user);

        String userType = getUserType();
        switchUserLayout(userType);

        // General User Email edit toggle
        view.findViewById(R.id.edit_email_btn).setOnClickListener(v -> {
            toggleFieldEdit(
                    view.findViewById(R.id.edit_email_et),
                    (ImageButton) view.findViewById(R.id.edit_email_btn)
            );
        });

        // General User Password edit toggle
        view.findViewById(R.id.edit_password_btn).setOnClickListener(v -> {
            toggleFieldEdit(
                    view.findViewById(R.id.edit_password_et),
                    (ImageButton) view.findViewById(R.id.edit_password_btn)
            );
        });

        //Business Email Edit toggle
        view.findViewById(R.id.business_edit_email_btn).setOnClickListener(v ->{
            toggleFieldEdit(
                    view.findViewById(R.id.business_edit_email_et),
                    (ImageButton) view.findViewById(R.id.business_edit_email_et)
            );
        });

        //Business Password Edit toggle
        view.findViewById(R.id.business_edit_password_btn).setOnClickListener(v -> {
            toggleFieldEdit(
                    view.findViewById(R.id.business_edit_password_et),
                    (ImageButton) view.findViewById(R.id.business_edit_password_et)
            );
        });

        //Business Address Edit toggle
        view.findViewById(R.id.business_edit_address_btn).setOnClickListener(v -> {
            toggleFieldEdit(
                    view.findViewById(R.id.business_edit_address_et),
                    (ImageButton) view.findViewById(R.id.business_edit_address_et)
            );
        });

        //Business Description Edit toggle
        view.findViewById(R.id.business_edit_desc_btn).setOnClickListener(v -> {
            toggleFieldEdit(
                    view.findViewById(R.id.business_edit_desc_et),
                    (ImageButton) view.findViewById(R.id.business_edit_desc_et)
            );
        });

        view.findViewById(R.id.locreq_new_btn).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AddLocationRequestFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.locreq_view_btn).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ViewAddLocationRequestFragment())
                    .addToBackStack(null)
                    .commit();
        });
        view.findViewById(R.id.locreq_view_btn_2).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ViewCompletedLocationRequestFragment())
                    .addToBackStack(null)
                    .commit();
        });
        return view;
    }
    private void toggleFieldEdit(TextInputEditText field, ImageButton button) {
        if (!field.isFocusable()) {
            // Enable editing
            field.setFocusable(true);
            field.setFocusableInTouchMode(true);
            field.setClickable(true);
            field.requestFocus();
            button.setImageResource(R.drawable.check_circle); // confirmation icon
        } else {
            // Disable editing / save
            field.setFocusable(false);
            field.setFocusableInTouchMode(false);
            field.setClickable(false);
            button.setImageResource(R.drawable.icon_edit); // swap back to edit icon

            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(field.getWindowToken(), 0);

            // Save to backend here when ready ... :)
        }
    }

    private String getUserType() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user_prefs", requireActivity().MODE_PRIVATE);
        return prefs.getString("user_type", "General"); // fallback to General
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
}