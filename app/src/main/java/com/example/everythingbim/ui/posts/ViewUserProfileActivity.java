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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.RequestReportStatus;
import com.example.everythingbim.data.models.UserType;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;


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
    private String targetUserUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_user_profile);

        viewModel = new ViewModelProvider(this).get(ViewUserProfileViewModel.class);

        targetUserId = getIntent().getLongExtra("USER_ID", -1);
        targetUserUid = getIntent().getStringExtra("USER_UID");

        initViews();

        // If we have userId but no UID, look up the user's firebaseUid from Room
        if (targetUserId != -1 && (targetUserUid == null || targetUserUid.isEmpty())) {
            viewModel.getUserById(targetUserId).observe(this, user -> {
                if (user != null && user.firebaseUid != null && !user.firebaseUid.isEmpty()) {
                    // Now we have the UID - load from Firestore and set up posts with UID
                    targetUserUid = user.firebaseUid;
                    setupViewPager(targetUserId, targetUserUid);
                    loadUserProfileFromFirestore(targetUserUid);
                } else {
                    // No UID available - just use Room data
                    setupViewPager(targetUserId, null);
                    viewModel.getUserWithProfile().observe(this, this::updateUIFromRoom);
                }
            });
        } else if (targetUserUid != null && !targetUserUid.isEmpty()) {
            // Have UID - load from Firestore directly
            viewModel.setUserId(targetUserId);
            // Set up posts with the UID (userId might be -1 but that's ok - UserPostsFragment will use UID)
            setupViewPager(targetUserId, targetUserUid);
            loadUserProfileFromFirestore(targetUserUid);
        } else {
            // No identifier - close
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data when returning to this activity
        if (targetUserUid != null && !targetUserUid.isEmpty()) {
            loadUserProfileFromFirestore(targetUserUid);
        } else if (targetUserId != -1) {
            // Reload from Room
            viewModel.getUserWithProfile().observe(this, this::updateUIFromRoom);
        }
    }

    /**
     * Directly load user profile from Firestore by UID.
     */
    private void loadUserProfileFromFirestore(String uid) {
        if (uid == null || uid.isEmpty()) {
            // No UID - can't load from Firestore
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    username.setText("Unknown User");
                    userIdText.setText("#");
                    bio.setText("Unable to load profile");
                }
            });
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Try users collection first (general users)
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isFinishing() && !isDestroyed()) {
                        if (doc != null && doc.exists()) {
                            // Found in users - it's a general user
                            String displayUsername = doc.getString("username");
                            String bioText = doc.getString("bio");

                            if (displayUsername == null || displayUsername.isEmpty()) {
                                displayUsername = "User";
                            }

                            username.setText(displayUsername);
                            userIdText.setText("#" + uid.substring(0, Math.min(8, uid.length())).toUpperCase());
                            bio.setText(bioText != null ? bioText : "");

                            // Load profile pic
                            String profilePicUrl = doc.getString("profilePictureUrl");
                            Glide.with(this)
                                    .load(profilePicUrl)
                                    .placeholder(R.drawable.ic_user_circle)
                                    .into(profilePic);

                            // General user - hide business elements, show posts
                            businessTag.setVisibility(View.GONE);
                            verificationIcon.setVisibility(View.GONE);
                            tabLayout.setVisibility(View.GONE);
                            viewPager.setVisibility(View.VISIBLE);
                            viewPager.setUserInputEnabled(false);
                        } else {
                            // Not in users - try businesses
                            loadBusinessProfileFromFirestore(uid);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        // Failed to check users - try businesses
                        loadBusinessProfileFromFirestore(uid);
                    }
                });
    }

    private void loadBusinessProfileFromFirestore(String uid) {
        if (isFinishing() || isDestroyed()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(uid).get()
                .addOnSuccessListener(bizDoc -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (bizDoc != null && bizDoc.exists()) {
                        // Try businessName first, fall back to companyName
                        String displayUsername = bizDoc.getString("businessName");
                        if (displayUsername == null || displayUsername.isEmpty()) {
                            displayUsername = bizDoc.getString("companyName");
                        }
                        String bioText = bizDoc.getString("description");
                        if (bioText == null) bioText = bizDoc.getString("businessDescription");

                        if (displayUsername == null || displayUsername.isEmpty()) {
                            displayUsername = "Business";
                        }

                        username.setText(displayUsername);
                        userIdText.setText("#" + uid.substring(0, Math.min(8, uid.length())).toUpperCase());
                        bio.setText(bioText != null ? bioText : "");

                        // Load profile pic - first item from imageUrls list
                        String profilePicUrl = null;
                        Object imageUrlsObj = bizDoc.get("imageUrls");
                        if (imageUrlsObj instanceof List) {
                            List<?> list = (List<?>) imageUrlsObj;
                            if (!list.isEmpty() && list.get(0) != null) {
                                profilePicUrl = list.get(0).toString();
                            }
                        }
                        Glide.with(this)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.ic_user_circle)
                                .into(profilePic);

                        // Business user - show business elements and tabs
                        businessTag.setVisibility(View.VISIBLE);
                        // Set business type text (e.g., "Restaurant", "Retail")
                        String businessType = bizDoc.getString("businessType");
                        businessTag.setText(businessType != null && !businessType.isEmpty() ? businessType : "Business");
                        // verificationStatus is a String ("Completed", "In Review", "Rejected") - check for "Completed"
                        // OR verified field is a Boolean
                        Boolean verified = bizDoc.getBoolean("verified");
                        String verificationStatus = bizDoc.getString("verificationStatus");
                        boolean isVerified = Boolean.TRUE.equals(verified) || "Completed".equals(verificationStatus);
                        verificationIcon.setVisibility(isVerified ? View.VISIBLE : View.GONE);
                        tabLayout.setVisibility(View.VISIBLE);
                        viewPager.setVisibility(View.VISIBLE);
                        viewPager.setUserInputEnabled(true);
                    } else {
                        // User not found in either collection
                        username.setText("User");
                        userIdText.setText("#" + uid.substring(0, Math.min(8, uid.length())).toUpperCase());
                        bio.setText("Profile not found");
                        businessTag.setVisibility(View.GONE);
                        verificationIcon.setVisibility(View.GONE);
                        tabLayout.setVisibility(View.GONE);
                        viewPager.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;

                    // Business query failed
                    username.setText("User");
                    userIdText.setText("#" + uid.substring(0, Math.min(8, uid.length())).toUpperCase());
                    bio.setText("Unable to load profile");
                    businessTag.setVisibility(View.GONE);
                    verificationIcon.setVisibility(View.GONE);
                    tabLayout.setVisibility(View.GONE);
                    viewPager.setVisibility(View.GONE);
                });
    }

    /**
     * Update UI from Room/ViewModel (original method)
     */
    private void updateUIFromRoom(UserWithProfile profile) {
        updateUI(profile);
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
        androidx.core.widget.NestedScrollView dialogScrollView = dialogView.findViewById(R.id.dialog_report_post_scroll);

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

        KeyboardScrollHintHelper.attach(
                dialogView,
                dialogView,
                dialogScrollView,
                "view_profile_report_dialog_scroll_hint_seen",
                extraBottom -> {
                    if (dialogScrollView == null) return;
                    dialogScrollView.setPadding(
                            dialogScrollView.getPaddingLeft(),
                            dialogScrollView.getPaddingTop(),
                            dialogScrollView.getPaddingRight(),
                            extraBottom);
                    dialogScrollView.setClipToPadding(false);
                });


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

    private void setupViewPager(long userId, @Nullable String authorUid) {
        pagerAdapter = new ProfilePagerAdapter(this, userId, authorUid);
        viewPager.setAdapter(pagerAdapter);

        // Attach TabLayout to ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Posts" : "Business Info");
        }).attach();
    }

    private void updateUI(UserWithProfile profile) {
        if (profile == null || profile.user == null) return;

        // Username - use username field
        String displayUsername = profile.user.username;
        if (displayUsername == null || displayUsername.isEmpty()) {
            displayUsername = "User";
        }
        username.setText(displayUsername);

        // User ID - for Firestore-loaded users (userId=0), use firebaseUid or show "#0"
        String displayId;
        if (profile.user.userId > 0) {
            displayId = "#" + profile.user.userId;
        } else if (profile.user.firebaseUid != null && !profile.user.firebaseUid.isEmpty()) {
            // Use first 8 chars of firebaseUid as display ID for Firestore users
            String uid = profile.user.firebaseUid;
            displayId = "#" + uid.substring(0, Math.min(8, uid.length())).toUpperCase();
        } else {
            displayId = "#0";
        }
        userIdText.setText(displayId);

        // Check if user is a Business
        boolean isBusiness = profile.user.userType == UserType.BUSINESS;

        // Toggle Visibility of Business-specific UI elements
        boolean isVerified = isBusiness && profile.businessUser != null &&
                profile.businessUser.verificationStatus == RequestReportStatus.ACCEPTED;
        verificationIcon.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        businessTag.setVisibility(isBusiness ? View.VISIBLE : View.GONE);
        tabLayout.setVisibility(isBusiness ? View.VISIBLE : View.GONE);

        // Disable swiping if it's a general user (stays on "Posts")
        viewPager.setUserInputEnabled(isBusiness);

        // Update Bio and Profile Pic from GeneralUserEntity or BusinessUserEntity
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
        private final String authorUid;

        public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity, long userId, @Nullable String authorUid) {
            super(fragmentActivity);
            this.userId = userId;
            this.authorUid = authorUid;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                if (authorUid != null && !authorUid.isEmpty()) {
                    return UserPostsFragment.newInstance(userId, authorUid);
                } else {
                    return UserPostsFragment.newInstance(userId);
                }
            } else {
                // Business Info tab - pass authorUid if available for Firestore loading
                if (authorUid != null && !authorUid.isEmpty()) {
                    return BusinessInfoFragment.newInstance(userId, authorUid);
                } else {
                    return BusinessInfoFragment.newInstance(userId);
                }
            }
        }

        @Override
        public int getItemCount() {
            // Always return 2; we control navigation by hiding the tab bar for non-business users
            return 2;
        }
    }
}
