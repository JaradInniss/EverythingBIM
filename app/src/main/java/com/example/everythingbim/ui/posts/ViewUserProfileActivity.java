package com.example.everythingbim.ui.posts;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.RequestReportStatus;
import com.example.everythingbim.data.models.UserType;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


// Activity for Viewing A User's Profile Information

public class ViewUserProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private ViewUserProfileViewModel viewModel;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProfilePagerAdapter pagerAdapter;

    private ImageView profilePic, verificationIcon, returnBttn, reportBttn;
    private TextView username, userIdText, bio, businessTag;

    // Variables
    private long targetUserId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_user_profile);

        // Retrieve the User ID passed via Intent
        targetUserId = getIntent().getLongExtra("USER_ID", -1);
        if (targetUserId == -1) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ViewUserProfileViewModel.class);
        viewModel.setUserId(targetUserId);

        // Observe User Profile data from ViewModel
        viewModel.getUserWithProfile().observe(this, this::updateUI);

        initViews();
        setupViewPager(targetUserId);
    }

    private void initViews() {
        // TabLayout
        tabLayout = findViewById(R.id.viewprofile_tab_layout);

        // View Pager2
        viewPager = findViewById(R.id.viewprofile_viewpager);

        // ImageViews
        profilePic = findViewById(R.id.viewprofile_profile_pic);
        verificationIcon = findViewById(R.id.viewprofile_verification_icon);
        returnBttn = findViewById(R.id.return_bttn);
        returnBttn.setOnClickListener(this);
        reportBttn = findViewById(R.id.report_account_bttn);
        reportBttn.setOnClickListener(this);

        // TextViews
        username = findViewById(R.id.viewprofile_username);
        userIdText = findViewById(R.id.viewprofile_user_id);
        bio = findViewById(R.id.viewprofile_bio);
        businessTag = findViewById(R.id.viewprofile_business_tag);
    }

    @Override
    public void onClick(View view) {
        int bttnId = view.getId();
        if (bttnId == R.id.return_bttn) {
            finish();
        }
        else if (bttnId == R.id.report_account_bttn) {
            showReportDialog();
        }
    }

    private void showReportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_post, null);
        RadioGroup postReasonGroup = dialogView.findViewById(R.id.report_post_reason_group);
        // Hide Post Group
        postReasonGroup.setVisibility(View.GONE);
        RadioGroup accountReasonGroup = dialogView.findViewById(R.id.report_account_reason_group);
        EditText reportDescriptionEt = dialogView.findViewById(R.id.report_description_et);

        // Set up dialog buttons
        LinearLayout submitReportBttn = dialogView.findViewById(R.id.submit_report_bttn);
        ImageView closeReportBttn = dialogView.findViewById(R.id.close_report_bttn);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        android.view.Window window = dialog.getWindow();
        if (window != null) {
            // Set width to 90% of screen width, height to wrap_content
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

            // Optional: Change gravity to center or bottom
            window.setGravity(android.view.Gravity.CENTER);
        }


        submitReportBttn.setOnClickListener(v -> {
            int selectedId = accountReasonGroup.getCheckedRadioButtonId();

            if (selectedId != -1) {
                RadioButton radioButton = dialogView.findViewById(selectedId);
                String reason = radioButton.getText().toString();
                String description = reportDescriptionEt.getText().toString();

                viewModel.reportAccount(targetUserId, reason, description);
                Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
            else {
                Toast.makeText(this, "Please select a reason for reporting", Toast.LENGTH_LONG).show();
            }
        });

        closeReportBttn.setOnClickListener(v -> {
            reportDescriptionEt.setText("");
            dialog.dismiss();
        });
    }

    private void setupViewPager(long userId) {
        pagerAdapter = new ProfilePagerAdapter(this, userId);
        viewPager.setAdapter(pagerAdapter);

        // Attach TabLayout to ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Posts" : "Business Info");
        }).attach();
    }

    private void updateUI(UserWithProfile profile) {
        if (profile == null) return;

        username.setText(profile.user.username);
        userIdText.setText("#" + profile.user.userId);


        // Check if user is a Business
        boolean isBusiness = profile.user.userType == UserType.BUSINESS;

        // Toggle Visibility of Business-specific UI elements
        verificationIcon.setVisibility(isBusiness && profile.businessUser.verificationStatus== RequestReportStatus.ACCEPTED ? View.VISIBLE : View.GONE);
        businessTag.setVisibility(isBusiness ? View.VISIBLE : View.GONE);
        tabLayout.setVisibility(isBusiness ? View.VISIBLE : View.GONE);

        // Disable swiping if it's a general user (stays on "Posts")
        viewPager.setUserInputEnabled(isBusiness);

        // Update Bio and Profile Pic from GeneralUserEntity part of the join
        if (profile.generalUser != null) {
            bio.setText(profile.generalUser.bio);
            Glide.with(this)
                    .load(profile.generalUser.profilePictureUrl)
                    .placeholder(R.drawable.ic_user_circle)
                    .into(profilePic);
        }
        else if (profile.businessUser != null) {
            bio.setText(profile.businessUser.businessDescription);
            Glide.with(this)
                    .load(profile.businessUser.profilePictureUrl)
                    .placeholder(R.drawable.ic_user_circle)
                    .into(profilePic);
        }
    }



    /**
     * Adapter to manage fragments within the ViewPager2
     */
    private static class ProfilePagerAdapter extends FragmentStateAdapter {
        private final long userId;

        public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity, long userId) {
            super(fragmentActivity);
            this.userId = userId;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return UserPostsFragment.newInstance(userId);
            } else {
                return BusinessInfoFragment.newInstance(userId);
            }
        }

        @Override
        public int getItemCount() {
            // Always return 2; we control navigation by hiding the tab bar for non-business users
            return 2;
        }
    }
}
