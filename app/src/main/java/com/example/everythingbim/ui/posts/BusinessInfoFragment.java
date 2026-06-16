package com.example.everythingbim.ui.posts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.BusinessUserEntity;
import com.google.firebase.firestore.FirebaseFirestore;

// Fragment for Business Info in TabLayout ViewUserProfile

public class BusinessInfoFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_AUTHOR_UID = "author_uid";
    private long userId;
    private String authorUid;
    private ViewUserProfileViewModel viewModel;

    private TextView bizAddress, bizPhone, bizEmail, bizDescription;

    public static BusinessInfoFragment newInstance(long userId) {
        BusinessInfoFragment fragment = new BusinessInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    public static BusinessInfoFragment newInstance(long userId, String authorUid) {
        BusinessInfoFragment fragment = new BusinessInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putString(ARG_AUTHOR_UID, authorUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID, -1);
            authorUid = getArguments().getString(ARG_AUTHOR_UID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_business_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bizAddress = view.findViewById(R.id.biz_address);
        bizPhone = view.findViewById(R.id.biz_phone);
        bizEmail = view.findViewById(R.id.biz_email);
        bizDescription = view.findViewById(R.id.biz_description);

        // Share the same ViewModel as the Activity to get the loaded user data
        viewModel = new ViewModelProvider(requireActivity()).get(ViewUserProfileViewModel.class);

        // If we have authorUid, load from Firestore directly (bypassing Room)
        if (authorUid != null && !authorUid.isEmpty()) {
            loadBusinessInfoFromFirestore(authorUid);
        } else {
            // Use Room data via ViewModel
            try {
                viewModel.getUserWithProfile().observe(getViewLifecycleOwner(), userWithProfile -> {
                    if (userWithProfile != null && userWithProfile.businessUser != null && bizAddress != null) {
                        BusinessUserEntity biz = userWithProfile.businessUser;
                        bizAddress.setText(biz.address);
                        bizPhone.setText(biz.phoneNumber);
                        bizEmail.setText(userWithProfile.user.email);
                        bizDescription.setText(biz.businessDescription);
                    }
                });
            } catch (Exception e) {
                // Handle error silently
            }
        }
    }

    private void loadBusinessInfoFromFirestore(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        requireActivity().runOnUiThread(() -> {
                            bizAddress.setText(doc.getString("address"));
                            bizPhone.setText(doc.getString("phone"));
                            bizEmail.setText(doc.getString("businessEmail"));
                            // Try description first, fall back to bio
                            String description = doc.getString("description");
                            if (description == null || description.isEmpty()) {
                                description = doc.getString("bio");
                            }
                            bizDescription.setText(description);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error silently
                });
    }
}
