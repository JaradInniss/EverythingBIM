package com.example.everythingbim.ui.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.R;

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