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

public class BusinessInfoFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private long userId;
    private ViewUserProfileViewModel viewModel;
    
    private TextView bizAddress, bizPhone, bizEmail, bizDescription;

    public static BusinessInfoFragment newInstance(long userId) {
        BusinessInfoFragment fragment = new BusinessInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID);
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
        
        viewModel.getUserWithProfile().observe(getViewLifecycleOwner(), userWithProfile -> {
            if (userWithProfile != null && userWithProfile.businessUser != null) {
                BusinessUserEntity biz = userWithProfile.businessUser;
                bizAddress.setText(biz.address);
                bizPhone.setText(biz.phoneNumber);
                bizEmail.setText(userWithProfile.user.email);
                bizDescription.setText(biz.businessDescription);
                
                // Set up images RecyclerView if needed
            }
        });
    }
}
