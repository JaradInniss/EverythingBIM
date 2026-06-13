package com.example.everythingbim.ui.user;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.databinding.FragmentUserBinding;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.BusinessRegistration;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.everythingbim.ui.utils.PasswordHash;

public class UserFragment extends Fragment {
    private static final String PREF_USER_GENERAL_SCROLL_HINT_SEEN = "user_general_scroll_hint_seen";
    private static final String PREF_USER_BUSINESS_SCROLL_HINT_SEEN = "user_business_scroll_hint_seen";
    private static final String PREF_USER_PASSWORD_DIALOG_SCROLL_HINT_SEEN = "user_password_dialog_scroll_hint_seen";
    private static final String PREF_USER_DESCRIPTION_DIALOG_SCROLL_HINT_SEEN = "user_description_dialog_scroll_hint_seen";

    private UserViewModel userViewModel;
    private FragmentUserBinding binding;

    private View layoutGuestUser, layoutGeneralUser, layoutBusinessUser;
    private View generalContentSubmissions, generalContentMyProfile, generalContentSettings;
    private View generalIndSubmissions, generalIndMyProfile, generalIndSettings;
    private View layoutGuestUser;
    private View layoutGeneralUser;
    private View layoutBusinessUser;
    private ScrollView generalScrollView;
    private ScrollView businessScrollView;
    private int generalKeyboardExtraBottom = 0;
    private int businessKeyboardExtraBottom = 0;

    private View businessContentSubmissions, businessContentMyProfile, businessContentSettings;
    private View businessIndSubmissions, businessIndMyProfile, businessIndSettings;

    private Button  guestLoginBtn;

    private TextView guestAdminAccessBtn, guestReplayTourBtn;

    private LinearLayout generalSubmissionsTab, generalMyProfileTab, generalSettingsTab, businessSubmissionsTab, businessMyProfileTab, businessSettingsTab;
    private LinearLayout generalUserLogOutBtn, generalReplayTourBtn, businessUserLogOutBtn, businessReplayTourBtn, guestRegisterGeneralBtn, guestRegisterBusinessBtn;
    private LinearLayout newLocationReqBtn, viewLocationReqBtn, viewCompletedLocationReqBtn;
    private LinearLayout newInformationReqBtn, viewInformationReqBtn, viewCompletedInformationReqBtn;
    private LinearLayout viewAccVerificationBtn, viewCompletedAccVerificationBtn, viewAddBusinessLocationBtn, viewCompletedAddBusinessLocationBtn, addBusinessLocationBtn;

    private CardView guestAccountOptionsContainer;

    private long lastClickTime = 0;
    private int clickCount = 0;

    public UserFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserBinding.inflate(inflater, container, false);
        bindViews();
        setUpListeners();
        setUpObservers();
        setupGeneralUserTabs(binding.getRoot());
        setupBusinessUserTabs(binding.getRoot());
        switchUserLayout(getUserType());

        setupEditToggle(binding.getRoot(), R.id.general_user_edit_username_et, R.id.general_user_edit_username_btn, R.id.general_user_edit_username_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.general_user_edit_password_et, R.id.general_user_edit_password_btn, R.id.general_user_edit_password_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.general_user_bio_et, R.id.general_user_edit_bio_btn, R.id.general_user_edit_bio_btn_iv);

        setupEditToggle(binding.getRoot(), R.id.business_edit_email_et, R.id.business_edit_email_btn, R.id.business_edit_email_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.business_edit_password_et, R.id.business_edit_password_btn, R.id.business_edit_password_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.business_edit_desc_et, R.id.business_edit_desc_btn, R.id.business_edit_desc_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.business_user_bio_et, R.id.business_user_edit_bio_btn, R.id.business_user_edit_bio_btn_iv);

        return binding.getRoot();
    }

    private void bindViews() {
        layoutGuestUser = binding.layoutGuestUser;
        layoutGeneralUser = binding.layoutGeneralUser;
        layoutBusinessUser = binding.layoutBusinessUser;

        generalSubmissionsTab = binding.generalTabSubmissions;
        generalMyProfileTab = binding.generalTabMyProfile;
        generalSettingsTab = binding.generalTabSettings;

        businessSubmissionsTab = binding.businessTabSubmissions;
        businessMyProfileTab = binding.businessTabMyProfile;
        businessSettingsTab = binding.businessTabSettings;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user, container, false);

        layoutGuestUser = view.findViewById(R.id.layout_guest_user);
        layoutGeneralUser = view.findViewById(R.id.layout_general_user);
        layoutBusinessUser = view.findViewById(R.id.layout_business_user);
        generalScrollView = view.findViewById(R.id.general_user_scroll);
        businessScrollView = view.findViewById(R.id.business_user_scroll);

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

        String currentUserType = getUserType();
        switchUserLayout(currentUserType);
        setupKeyboardHints(view);
        setupGeneralUserTabs(view);
        setupBusinessUserTabs(view);

        setClickIfPresent(view, R.id.general_user_btnLogout, v -> performLogout());
        setClickIfPresent(view, R.id.btnLogout, v -> performLogout());
        setClickIfPresent(view, R.id.general_replay_tour_btn, v -> replayTour());
        setClickIfPresent(view, R.id.business_replay_tour_btn, v -> replayTour());
        generalContentSubmissions = binding.generalContentSubmissions;
        generalContentMyProfile = binding.generalContentMyProfile;
        generalContentSettings = binding.generalContentSettings;

        setClickIfPresent(view, R.id.guest_login_btn, v ->
                startActivity(new Intent(requireContext(), Login.class)));
        setClickIfPresent(view, R.id.guest_register_general_btn, v ->
                startActivity(new Intent(requireContext(), GeneralRegistration.class)));
        setClickIfPresent(view, R.id.guest_register_business_btn, v ->
                startActivity(new Intent(requireContext(), BusinessRegistration.class)));
        setClickIfPresent(view, R.id.guest_admin_access_btn, v -> {
        businessContentSubmissions = binding.businessContentSubmissions;
        businessContentMyProfile = binding.businessContentMyProfile;
        businessContentSettings = binding.businessContentSettings;

        generalIndSubmissions = binding.generalTabSubmissionsIndicator;
        generalIndMyProfile = binding.generalTabMyProfileIndicator;
        generalIndSettings = binding.generalTabSettingsIndicator;

        businessIndSubmissions = binding.businessTabSubmissionsIndicator;
        businessIndMyProfile = binding.businessTabMyProfileIndicator;
        businessIndSettings = binding.businessTabSettingsIndicator;

        // Buttons
        generalUserLogOutBtn = binding.generalUserBtnLogout;
        businessUserLogOutBtn = binding.businessUserLogoutBtn;
        generalReplayTourBtn = binding.generalReplayTourBtn;
        businessReplayTourBtn = binding.businessReplayTourBtn;
        guestLoginBtn = binding.guestLoginBtn;

        // Text Views
        guestAdminAccessBtn = binding.guestAdminAccessBtn;
        guestReplayTourBtn = binding.guestReplayTourBtn;

        // Linear Layouts
        guestRegisterGeneralBtn = binding.guestRegisterGeneralBtn;
        guestRegisterBusinessBtn = binding.guestRegisterBusinessBtn;

        newLocationReqBtn = binding.locreqNewBtn;
        viewLocationReqBtn = binding.locreqViewBtn;
        viewCompletedLocationReqBtn = binding.locreqViewBtn2;

        newInformationReqBtn = binding.inforeqNewBtn;
        viewInformationReqBtn = binding.inforeqViewBtn;
        viewCompletedInformationReqBtn = binding.inforeqViewBtn2;

        viewAccVerificationBtn = binding.accverViewBtn;
        viewCompletedAccVerificationBtn = binding.accverViewBtn2;

        viewAddBusinessLocationBtn = binding.addlocViewBtn;
        viewCompletedAddBusinessLocationBtn = binding.addlocViewBtn2;

        addBusinessLocationBtn = binding.businessAddFieldBtn;

        guestAccountOptionsContainer = binding.guestAccountOptionsContainer;
    }

    private void setUpListeners() {
        // Buttons
        guestLoginBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), Login.class)));

        // Text Views
        guestAdminAccessBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Login.class);
            intent.putExtra("preselectedUserType", UserType.ADMIN.name());
            startActivity(intent);
        });
        setClickIfPresent(view, R.id.guest_replay_tour_btn, v -> replayTour());

        setClickIfPresent(view, R.id.locreq_new_btn, v ->
                navigateTo(new AddLocationRequestFragment()));
        setClickIfPresent(view, R.id.locreq_view_btn, v ->
                navigateTo(new ViewAddLocationRequestFragment()));
        setClickIfPresent(view, R.id.locreq_view_btn_2, v ->
                navigateTo(new ViewCompletedLocationRequestFragment()));
        guestReplayTourBtn.setOnClickListener(v -> replayTour());

        setClickIfPresent(view, R.id.inforeq_new_btn, v ->
                navigateTo(new AddInformationRequestFragment()));
        setClickIfPresent(view, R.id.inforeq_view_btn, v ->
                navigateTo(new ViewAddInformationRequestFragment()));
        setClickIfPresent(view, R.id.inforeq_view_btn_2, v ->
                navigateTo(new ViewCompletedInformationRequestFragment()));
        // Linear Layouts
        generalUserLogOutBtn.setOnClickListener(v -> performLogout());
        businessUserLogOutBtn.setOnClickListener(v -> performLogout());

        setClickIfPresent(view, R.id.accver_view_btn, v ->
                navigateTo(new ViewAccountVerificationRequestFragment()));
        setClickIfPresent(view, R.id.accver_view_btn_2, v ->
                navigateTo(new ViewCompletedAccountVerificationRequestFragment()));
        generalReplayTourBtn.setOnClickListener(v -> replayTour());
        businessReplayTourBtn.setOnClickListener(v -> replayTour());

        setClickIfPresent(view, R.id.addloc_view_btn, v ->
                navigateTo(new com.example.everythingbim.ViewAddLocationToAddressFragment()));
        guestRegisterGeneralBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), GeneralRegistration.class)));
        guestRegisterBusinessBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), BusinessRegistration.class)));

        setClickIfPresent(view, R.id.addloc_view_btn_2, v ->
                navigateTo(new com.example.everythingbim.ViewCompletedAddLocationToAddressFragment()));
        newLocationReqBtn.setOnClickListener(v -> navigateTo(new AddLocationRequestFragment()));
        viewLocationReqBtn.setOnClickListener(v -> navigateTo(new ViewAddLocationRequestFragment()));
        viewCompletedLocationReqBtn.setOnClickListener(v -> navigateTo(new ViewCompletedLocationRequestFragment()));

        setClickIfPresent(view, R.id.business_add_field_btn, v ->
                navigateTo(new AddBusinessLocationRequestFragment()));
        newInformationReqBtn.setOnClickListener(v -> navigateTo(new AddInformationRequestFragment()));
        viewInformationReqBtn.setOnClickListener(v -> navigateTo(new ViewAddInformationRequestFragment()));
        viewCompletedInformationReqBtn.setOnClickListener(v -> navigateTo(new ViewCompletedInformationRequestFragment()));

        viewAccVerificationBtn.setOnClickListener(v -> navigateTo(new ViewAccountVerificationRequestFragment()));
        viewCompletedAccVerificationBtn.setOnClickListener(v -> navigateTo(new ViewCompletedAccountVerificationRequestFragment()));

        viewAddBusinessLocationBtn.setOnClickListener(v -> navigateTo(new com.example.everythingbim.ViewAddLocationToAddressFragment()));
        viewCompletedAddBusinessLocationBtn.setOnClickListener(v -> navigateTo(new com.example.everythingbim.ViewCompletedAddLocationToAddressFragment()));

        addBusinessLocationBtn.setOnClickListener(v -> navigateTo(new AddBusinessLocationRequestFragment()));

        // Card View
        guestAccountOptionsContainer.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 500) {
                clickCount++;
            } else {
                clickCount = 1;
            }
            lastClickTime = currentTime;
        if (MainActivity.USER_TYPE_BUSINESS.equals(currentUserType)) {
            loadAddressesOnMainScreen(view);

            TextView categoryTv = view.findViewById(R.id.business_category_tv);
            if (categoryTv != null) {
                categoryTv.setOnClickListener(v -> showCategoryDialog());
            }
        }
            if (clickCount == 3) {
                userViewModel.onAdminSecretTriggered();
                clickCount = 0;
            }
        });
    }

    private void setUpObservers() {
        userViewModel.isAdminAccessVisible().observe(getViewLifecycleOwner(), isAdminAccessVisible -> {
            if (isAdminAccessVisible) {
                guestAdminAccessBtn.setVisibility(View.VISIBLE);
            } else {
                guestAdminAccessBtn.setVisibility(View.GONE);
            }
        });
    }

    private void setupKeyboardHints(View root) {
        if (generalScrollView != null) {
            int initialLeft = generalScrollView.getPaddingLeft();
            int initialTop = generalScrollView.getPaddingTop();
            int initialRight = generalScrollView.getPaddingRight();
            int initialBottom = generalScrollView.getPaddingBottom();

            KeyboardScrollHintHelper.attach(
                    root,
                    generalScrollView,
                    generalScrollView,
                    PREF_USER_GENERAL_SCROLL_HINT_SEEN,
                    keyboardExtraBottom -> {
                        generalKeyboardExtraBottom = keyboardExtraBottom;
                        generalScrollView.setPadding(
                                initialLeft,
                                initialTop,
                                initialRight,
                                initialBottom + keyboardExtraBottom
                        );
                    }
            );
        }

        if (businessScrollView != null) {
            int initialLeft = businessScrollView.getPaddingLeft();
            int initialTop = businessScrollView.getPaddingTop();
            int initialRight = businessScrollView.getPaddingRight();
            int initialBottom = businessScrollView.getPaddingBottom();

            KeyboardScrollHintHelper.attach(
                    root,
                    businessScrollView,
                    businessScrollView,
                    PREF_USER_BUSINESS_SCROLL_HINT_SEEN,
                    keyboardExtraBottom -> {
                        businessKeyboardExtraBottom = keyboardExtraBottom;
                        businessScrollView.setPadding(
                                initialLeft,
                                initialTop,
                                initialRight,
                                initialBottom + keyboardExtraBottom
                        );
                    }
            );
        }
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

        generalSubmissionsTab.setOnClickListener(v -> switchGeneralTab(0));
        generalMyProfileTab.setOnClickListener(v -> switchGeneralTab(1));
        generalSettingsTab.setOnClickListener(v -> switchGeneralTab(2));
        setClickIfPresent(view, R.id.general_tab_submissions, v -> switchGeneralTab(0));
        setClickIfPresent(view, R.id.general_tab_my_profile, v -> switchGeneralTab(1));
        setClickIfPresent(view, R.id.general_tab_settings, v -> switchGeneralTab(2));
    }

    private void switchGeneralTab(int tab) {
        generalContentSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        generalContentMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        generalContentSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        generalIndSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        generalIndMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        generalIndSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        setVisibleIfPresent(generalContentSubmissions, tab == 0);
        setVisibleIfPresent(generalContentMyProfile, tab == 1);
        setVisibleIfPresent(generalContentSettings, tab == 2);
        setVisibleIfPresent(generalIndSubmissions, tab == 0);
        setVisibleIfPresent(generalIndMyProfile, tab == 1);
        setVisibleIfPresent(generalIndSettings, tab == 2);
    }

    private void setupBusinessUserTabs(View view) {
        switchBusinessTab(0);

        businessSubmissionsTab.setOnClickListener(v -> switchBusinessTab(0));
        businessMyProfileTab.setOnClickListener(v -> switchBusinessTab(1));
        businessSettingsTab.setOnClickListener(v -> switchBusinessTab(2));
        setClickIfPresent(view, R.id.business_tab_submissions, v -> switchBusinessTab(0));
        setClickIfPresent(view, R.id.business_tab_my_profile, v -> switchBusinessTab(1));
        setClickIfPresent(view, R.id.business_tab_settings, v -> switchBusinessTab(2));
    }

    private void setClickIfPresent(View root, int viewId, View.OnClickListener listener) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setOnClickListener(listener);
        } else {
            android.util.Log.w("UserFragment", "Missing expected view id: " + getResources().getResourceEntryName(viewId));
        }
    }

    private void switchBusinessTab(int tab) {
        setVisibleIfPresent(businessContentSubmissions, tab == 0);
        setVisibleIfPresent(businessContentMyProfile, tab == 1);
        setVisibleIfPresent(businessContentSettings, tab == 2);
        setVisibleIfPresent(businessIndSubmissions, tab == 0);
        setVisibleIfPresent(businessIndMyProfile, tab == 1);
        setVisibleIfPresent(businessIndSettings, tab == 2);
    }

    private void setVisibleIfPresent(View target, boolean visible) {
        if (target != null) {
            target.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void showReplayTourLocation() {
        String currentUserType = getUserType();
        if (MainActivity.USER_TYPE_GENERAL.equals(currentUserType)) {
            switchGeneralTab(2);
        } else if (MainActivity.USER_TYPE_BUSINESS.equals(currentUserType)) {
            switchBusinessTab(2);
        }
    }

    private void setupEditToggle(View root, int fieldId, int buttonId, int iconId) {
        TextInputEditText field = root.findViewById(fieldId);
        LinearLayout button = root.findViewById(buttonId);
        if (field == null || button == null) {
            return;
        }
        ImageView icon = root.findViewById(iconId);

        // Special handling for password and description - open dialog immediately
        if (fieldId == R.id.business_edit_password_et || fieldId == R.id.general_user_edit_password_et) {
            button.setOnClickListener(v -> openPasswordDialog());
            return;
        }
        if (fieldId == R.id.business_edit_desc_et) {
            button.setOnClickListener(v -> openDescriptionDialog());
            return;
        }

        // Normal behavior for other fields
        button.setOnClickListener(v -> toggleFieldEdit(field, button, icon, fieldId));
    }

    private void toggleFieldEdit(TextInputEditText field, LinearLayout button, ImageView icon, int fieldId) {
        // TextInputLayout is the direct parent of TextInputEditText
        ViewParent parent = field.getParent();
        android.util.Log.d("UserFragment", "direct parent=" + (parent != null ? parent.getClass().getName() : "null"));

        TextInputLayout fieldLayout = (parent instanceof TextInputLayout) ? (TextInputLayout) parent : null;
        android.util.Log.d("UserFragment", "fieldLayout direct=" + (fieldLayout != null ? "found" : "null"));

        // Try to find TextInputLayout by walking up the hierarchy if not found directly
        if (fieldLayout == null && parent != null) {
            ViewParent current = parent;
            int depth = 0;
            while (current != null && depth < 10) {
                android.util.Log.d("UserFragment", "Hierarchy[" + depth + "]=" + current.getClass().getName());
                if (current instanceof TextInputLayout) {
                    fieldLayout = (TextInputLayout) current;
                    android.util.Log.d("UserFragment", "Found TextInputLayout at depth=" + depth);
                    break;
                }
                if (current instanceof View) {
                    current = ((View) current).getParent();
                    depth++;
                } else {
                    break;
                }
            }
        }

        final TextInputLayout finalFieldLayout = fieldLayout;
        android.util.Log.d("UserFragment", "final fieldLayout=" + (finalFieldLayout != null ? "found" : "null"));

        // Store original value before entering edit mode
        String originalValue = field.getText() != null ? field.getText().toString() : "";

        if (!field.isFocusable()) {
            // Entering edit mode - store original value in tag
            field.setTag(originalValue);
            field.setFocusable(true);
            field.setFocusableInTouchMode(true);
            field.setClickable(true);
            field.requestFocus();

            scrollFieldAboveKeyboard(field);
            // Blue background, white icon
            button.setBackgroundResource(R.drawable.bg_rectangle_blue);
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            ));
            // Blue outline on field
            if (fieldLayout != null) {
                fieldLayout.setBackgroundResource(R.drawable.bg_border_rectangle_alice_blue_2);
            }

        } else {
            // Exiting edit mode - retrieve original value from tag (set when entering edit mode)
            String storedOriginal = (String) field.getTag();
            String trimmedOriginal = (storedOriginal != null ? storedOriginal : "").trim();
            String newValue = field.getText() != null ? field.getText().toString().trim() : "";

            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(field.getWindowToken(), 0);

            field.setFocusable(false);
            field.setFocusableInTouchMode(false);
            field.setClickable(false);

            // Grey background, black icon
            button.setBackgroundResource(R.drawable.bg_rectangle_alice_blue);
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
            ));
            // Reset field outline to grey
            if (fieldLayout != null) {
                fieldLayout.setBackgroundResource(R.drawable.bg_rectangle_alice_blue);
            }

            // Check if value actually changed (for email/bio fields)
            if (!newValue.equals(trimmedOriginal)) {
                android.util.Log.d("UserFragment", "Value changed - showing dialog");
                showFieldDialog(fieldId, newValue,
                        () -> saveField(fieldId, newValue),
                        () -> resetFieldToOriginal(field, button, icon, finalFieldLayout, trimmedOriginal));
            } else {
                // No change - just reset UI without dialog
                android.util.Log.d("UserFragment", "No change - no dialog");
            }
        }
    }

    private void scrollFieldAboveKeyboard(View anchorView) {
        ScrollView targetScroll = getActiveUserScrollView(anchorView);
        int keyboardExtraBottom = targetScroll == businessScrollView
                ? businessKeyboardExtraBottom
                : generalKeyboardExtraBottom;

        if (targetScroll == null) {
            return;
        }

        targetScroll.post(() -> {
            if (keyboardExtraBottom <= 0) {
                return;
            }

            Rect rect = new Rect();
            anchorView.getDrawingRect(rect);
            targetScroll.offsetDescendantRectToMyCoords(anchorView, rect);

            int visibleHeight = targetScroll.getHeight() - keyboardExtraBottom;
            int desiredBottomMargin = dpToPx(24);
            int targetBottom = visibleHeight - desiredBottomMargin;
            int delta = rect.bottom - targetBottom;
            if (delta > 0) {
                targetScroll.smoothScrollBy(0, delta);
            }
        });
    }

    @Nullable
    private ScrollView getActiveUserScrollView(View target) {
        if (target == null) {
            return null;
        }
        if (layoutBusinessUser != null && layoutBusinessUser.getVisibility() == View.VISIBLE) {
            return businessScrollView;
        }
        if (layoutGeneralUser != null && layoutGeneralUser.getVisibility() == View.VISIBLE) {
            return generalScrollView;
        }
        return null;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void setupDialogKeyboardHints(Dialog dialog, ScrollView dialogScrollView, String prefKey) {
        if (dialog == null || dialogScrollView == null) {
            return;
        }
        View dialogRoot = dialog.findViewById(R.id.dialog_keyboard_root);
        if (dialogRoot == null) {
            return;
        }
        final int[] keyboardExtraBottom = {0};
        KeyboardScrollHintHelper.attach(
                dialogRoot,
                dialogRoot,
                dialogScrollView,
                prefKey,
                extraBottom -> {
                    keyboardExtraBottom[0] = extraBottom;
                    dialogScrollView.setPadding(
                            dialogScrollView.getPaddingLeft(),
                            dialogScrollView.getPaddingTop(),
                            dialogScrollView.getPaddingRight(),
                            extraBottom);
                    dialogScrollView.setClipToPadding(false);
                },
                null);
        dialogScrollView.setTag(R.id.scroll_hint_overlay_host, keyboardExtraBottom);
    }

    private void bindDialogFocusScroll(@Nullable TextInputEditText editText,
                                       @Nullable ScrollView dialogScrollView,
                                       @Nullable View anchorView) {
        if (editText == null || dialogScrollView == null) {
            return;
        }
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollDialogAnchorAboveKeyboard(dialogScrollView, anchorView != null ? anchorView : v);
            }
        });
    }

    private void scrollDialogAnchorAboveKeyboard(@Nullable ScrollView dialogScrollView,
                                                 @Nullable View anchorView) {
        if (dialogScrollView == null || anchorView == null) {
            return;
        }
        dialogScrollView.post(() -> {
            Rect anchorRect = new Rect();
            Rect scrollRect = new Rect();
            anchorView.getDrawingRect(anchorRect);
            dialogScrollView.offsetDescendantRectToMyCoords(anchorView, anchorRect);
            dialogScrollView.getDrawingRect(scrollRect);
            Object keyboardTag = dialogScrollView.getTag(R.id.scroll_hint_overlay_host);
            int keyboardExtraBottom = 0;
            if (keyboardTag instanceof int[]) {
                keyboardExtraBottom = ((int[]) keyboardTag)[0];
            }
            int visibleBottom = scrollRect.bottom - keyboardExtraBottom - dpToPx(24);
            if (anchorRect.bottom > visibleBottom) {
                int delta = anchorRect.bottom - visibleBottom;
                dialogScrollView.smoothScrollBy(0, delta);
            }
        });
    }

    private void openPasswordDialog() {
        android.util.Log.d("UserFragment", "Opening password dialog immediately");
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        String userType = getUserType();

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_change_password);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        TextInputEditText currentPasswordEt = dialog.findViewById(R.id.dialog_current_password_et);
        TextInputEditText newPasswordEt = dialog.findViewById(R.id.dialog_new_password_et);
        TextInputEditText reenterPasswordEt = dialog.findViewById(R.id.dialog_reenter_password_et);
        TextView errorTv = dialog.findViewById(R.id.dialog_password_error_tv);
        Button saveBtn = dialog.findViewById(R.id.dialog_save_btn);
        ImageButton closeBtn = dialog.findViewById(R.id.dialog_close_btn);
        ScrollView dialogScrollView = dialog.findViewById(R.id.dialog_change_password_scroll);

        setupDialogKeyboardHints(dialog, dialogScrollView, PREF_USER_PASSWORD_DIALOG_SCROLL_HINT_SEEN);
        bindDialogFocusScroll(currentPasswordEt, dialogScrollView, currentPasswordEt);
        bindDialogFocusScroll(newPasswordEt, dialogScrollView, newPasswordEt);
        bindDialogFocusScroll(reenterPasswordEt, dialogScrollView, saveBtn);

        saveBtn.setOnClickListener(v -> {
            String current = currentPasswordEt.getText() != null ? currentPasswordEt.getText().toString() : "";
            String newPass = newPasswordEt.getText() != null ? newPasswordEt.getText().toString() : "";
            String reenter = reenterPasswordEt.getText() != null ? reenterPasswordEt.getText().toString() : "";

            if (current.isEmpty() || newPass.isEmpty() || reenter.isEmpty()) {
                errorTv.setText("All fields are required");
                errorTv.setVisibility(View.VISIBLE);
                return;
            }
            if (!newPass.equals(reenter)) {
                errorTv.setText("New passwords do not match");
                errorTv.setVisibility(View.VISIBLE);
                return;
            }
            if (newPass.length() < 8) {
                errorTv.setText("Password must be at least 8 characters");
                errorTv.setVisibility(View.VISIBLE);
                return;
            }

            String collection = MainActivity.USER_TYPE_BUSINESS.equals(userType) ? "businesses" : "users";

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            dialog.dismiss();

            // Show loading indicator
            android.widget.Toast.makeText(requireContext(), "Verifying current password...", android.widget.Toast.LENGTH_SHORT).show();

            // Verify current password directly from Firestore (app uses custom auth, not Firebase Auth)
            db.collection(collection).document(userId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String storedPassword = doc.getString("password");
                            // Check hashed password first, then fall back to plain text for backwards compatibility
                            boolean passwordMatches = storedPassword != null &&
                                (PasswordHash.verify(current, storedPassword) || storedPassword.equals(current));

                            if (passwordMatches) {
                                // Check if new password is same as current (comparing plain text)
                                if (newPass.equals(current)) {
                                    android.widget.Toast.makeText(requireContext(), "New password cannot be the same as current password", android.widget.Toast.LENGTH_LONG).show();
                                    return;
                                }
                                // Current password verified - update in Firestore
                                int passwordFieldId = MainActivity.USER_TYPE_GENERAL.equals(userType)
                                        ? R.id.general_user_edit_password_et
                                        : R.id.business_edit_password_et;
                                saveField(passwordFieldId, newPass);
                                android.widget.Toast.makeText(requireContext(), "Password updated successfully", android.widget.Toast.LENGTH_SHORT).show();
                            } else {
                                android.widget.Toast.makeText(requireContext(), "Current password is incorrect", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            android.widget.Toast.makeText(requireContext(), "User document not found", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(requireContext(), "Failed to verify: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    });
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openDescriptionDialog() {
        android.util.Log.d("UserFragment", "Opening description dialog immediately");
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        // Get current description
        TextInputEditText descField = requireView().findViewById(R.id.business_edit_desc_et);
        String currentDesc = descField.getText() != null ? descField.getText().toString() : "";

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_business_description);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        TextView titleTv = dialog.findViewById(R.id.dialog_title);
        TextInputEditText descriptionEt = dialog.findViewById(R.id.dialog_description_et);
        Button saveBtn = dialog.findViewById(R.id.dialog_save_btn);
        ImageButton closeBtn = dialog.findViewById(R.id.dialog_close_btn);
        ScrollView dialogScrollView = dialog.findViewById(R.id.dialog_business_description_scroll);

        setupDialogKeyboardHints(dialog, dialogScrollView, PREF_USER_DESCRIPTION_DIALOG_SCROLL_HINT_SEEN);
        bindDialogFocusScroll(descriptionEt, dialogScrollView, saveBtn);

        if (titleTv != null) titleTv.setText("Business Description");
        if (descriptionEt != null) descriptionEt.setText(currentDesc);

        saveBtn.setOnClickListener(v -> {
            String newDesc = descriptionEt.getText() != null ? descriptionEt.getText().toString().trim() : "";
            if (!newDesc.isEmpty()) {
                dialog.dismiss();
                saveField(R.id.business_edit_desc_et, newDesc);
            }
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void resetFieldToOriginal(TextInputEditText field, LinearLayout button, ImageView icon,TextInputLayout fieldLayout, String originalValue) {
        field.setText(originalValue);
        resetField(field, button, icon, fieldLayout);
    }

    private String getFieldName(int fieldId) {
        if (fieldId == R.id.general_user_edit_username_et || fieldId == R.id.business_user_tv) return "Username";

        if (fieldId == R.id.business_edit_username_et) return "Username";
        if (fieldId == R.id.general_user_edit_password_et || fieldId == R.id.business_edit_password_et) return "Password";
        if (fieldId == R.id.general_user_bio_et || fieldId == R.id.business_user_bio_et) return "Bio";
        if (fieldId == R.id.business_edit_desc_et) return "Business Description";
        return "Field";
    }

    private void showFieldDialog(int fieldId, String currentValue, Runnable onSave, Runnable onCancel) {
        try {
            android.util.Log.d("UserFragment", "showFieldDialog called for fieldId=" + fieldId + ", value=" + currentValue);
            Dialog dialog;
            if (fieldId == R.id.business_edit_password_et || fieldId == R.id.general_user_edit_password_et) {
                android.util.Log.d("UserFragment", "Creating password dialog");
                dialog = createPasswordDialog(fieldId, currentValue, onSave, onCancel);
            } else if (fieldId == R.id.business_edit_desc_et) {
                android.util.Log.d("UserFragment", "Creating description dialog");
                dialog = createDescriptionDialog(fieldId, currentValue, onSave, onCancel);
            } else {
                android.util.Log.d("UserFragment", "Creating confirm dialog");
                dialog = createConfirmDialog(fieldId, currentValue, onSave, onCancel);
            }
            if (dialog != null) {
                android.util.Log.d("UserFragment", "Dialog created, showing now");
                dialog.show();
            } else {
                android.util.Log.e("UserFragment", "Dialog was null!");
            }
        } catch (Exception e) {
            android.util.Log.e("UserFragment", "Error showing dialog: " + e.getMessage(), e);
        }
    }

    private void showCategoryDialog() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        String currentCategory = prefs.getString("businessCategory", "Restaurant");

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_business_description);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        TextView titleTv = dialog.findViewById(R.id.dialog_title);
        TextInputEditText descriptionEt = dialog.findViewById(R.id.dialog_description_et);
        Button saveBtn = dialog.findViewById(R.id.dialog_save_btn);
        ImageButton closeBtn = dialog.findViewById(R.id.dialog_close_btn);

        if (titleTv != null) titleTv.setText("Business Type");
        if (descriptionEt != null) descriptionEt.setText(currentCategory);

        saveBtn.setOnClickListener(v -> {
            String newCategory = descriptionEt.getText() != null ? descriptionEt.getText().toString().trim() : "";
            if (!newCategory.isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("businessType", newCategory);

                db.collection("businesses").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        requireActivity().runOnUiThread(() -> {
                            prefs.edit().putString("businessCategory", newCategory).apply();
                            TextView categoryTv = requireView().findViewById(R.id.business_category_tv);
                            if (categoryTv != null) categoryTv.setText(newCategory);
                        });
                    });
                dialog.dismiss();
            }
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private Dialog createPasswordDialog(int fieldId, String currentValue, Runnable onSave, Runnable onCancel) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_change_password);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        TextInputEditText currentPasswordEt = dialog.findViewById(R.id.dialog_current_password_et);
        TextInputEditText newPasswordEt = dialog.findViewById(R.id.dialog_new_password_et);
        TextInputEditText reenterPasswordEt = dialog.findViewById(R.id.dialog_reenter_password_et);
        TextView errorTv = dialog.findViewById(R.id.dialog_password_error_tv);
        Button saveBtn = dialog.findViewById(R.id.dialog_save_btn);
        ImageButton closeBtn = dialog.findViewById(R.id.dialog_close_btn);
        ScrollView dialogScrollView = dialog.findViewById(R.id.dialog_change_password_scroll);

        setupDialogKeyboardHints(dialog, dialogScrollView, PREF_USER_PASSWORD_DIALOG_SCROLL_HINT_SEEN);
        bindDialogFocusScroll(currentPasswordEt, dialogScrollView, currentPasswordEt);
        bindDialogFocusScroll(newPasswordEt, dialogScrollView, newPasswordEt);
        bindDialogFocusScroll(reenterPasswordEt, dialogScrollView, saveBtn);

        saveBtn.setOnClickListener(v -> {
            String current = currentPasswordEt.getText() != null ? currentPasswordEt.getText().toString() : "";
            String newPass = newPasswordEt.getText() != null ? newPasswordEt.getText().toString() : "";
            String reenter = reenterPasswordEt.getText() != null ? reenterPasswordEt.getText().toString() : "";

            if (current.isEmpty() || newPass.isEmpty() || reenter.isEmpty()) {
                errorTv.setText("All fields are required");
                errorTv.setVisibility(View.VISIBLE);
                return;
            }
            if (!newPass.equals(reenter)) {
                errorTv.setText("New passwords do not match");
                errorTv.setVisibility(View.VISIBLE);
                return;
            }
            if (newPass.length() < 6) {
                errorTv.setText("Password must be at least 6 characters");
                errorTv.setVisibility(View.VISIBLE);
                return;
            }
            dialog.dismiss();
            onSave.run();
        });

        closeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            onCancel.run();
        });

        return dialog;
    }

    private Dialog createDescriptionDialog(int fieldId, String currentValue, Runnable onSave, Runnable onCancel) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_business_description);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        TextInputEditText descriptionEt = dialog.findViewById(R.id.dialog_description_et);
        TextView titleTv = dialog.findViewById(R.id.dialog_title);
        Button saveBtn = dialog.findViewById(R.id.dialog_save_btn);
        ImageButton closeBtn = dialog.findViewById(R.id.dialog_close_btn);
        ScrollView dialogScrollView = dialog.findViewById(R.id.dialog_business_description_scroll);

        setupDialogKeyboardHints(dialog, dialogScrollView, PREF_USER_DESCRIPTION_DIALOG_SCROLL_HINT_SEEN);
        bindDialogFocusScroll(descriptionEt, dialogScrollView, saveBtn);

        if (descriptionEt != null && currentValue != null) {
            descriptionEt.setText(currentValue);
        }

        saveBtn.setOnClickListener(v -> {
            String newDesc = descriptionEt.getText() != null ? descriptionEt.getText().toString() : "";
            if (!newDesc.isEmpty()) {
                dialog.dismiss();
                onSave.run();
            }
        });

        closeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            onCancel.run();
        });

        return dialog;
    }

    private Dialog createConfirmDialog(int fieldId, String currentValue, Runnable onSave, Runnable onCancel) {
            final Dialog dialog = new Dialog(requireContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_confirm_field_change);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
                window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = android.view.Gravity.CENTER;
                window.setAttributes(params);
            }

            TextView titleTv = dialog.findViewById(R.id.dialog_confirm_title);
            TextView messageTv = dialog.findViewById(R.id.dialog_confirm_message);
            Button yesBtn = dialog.findViewById(R.id.dialog_confirm_yes);
            Button noBtn = dialog.findViewById(R.id.dialog_confirm_no);

            String fieldName = getFieldName(fieldId);
            if (titleTv != null) titleTv.setText("Confirm " + fieldName + " Change?");
            if (messageTv != null) messageTv.setText("Update " + fieldName.toLowerCase() + " to:\n\n" + currentValue);

            yesBtn.setOnClickListener(v -> {
                dialog.dismiss();
                onSave.run();
            });

            noBtn.setOnClickListener(v -> {
                dialog.dismiss();
                onCancel.run();
            });

            return dialog;
    }

    private String getAddressFromObject(Object addressObj) {
        if (addressObj == null) return "";
        if (addressObj instanceof String) return (String) addressObj;
        if (addressObj instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) addressObj;
            Object addr = map.get("address");
            return addr != null ? addr.toString() : "";
        }
        return addressObj.toString();
    }

    private void showAddressesDialog() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_addresses);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        LinearLayout addressesList = dialog.findViewById(R.id.dialog_addresses_list);
        ImageButton closeBtn = dialog.findViewById(R.id.dialog_close_btn);
        Button saveBtn = dialog.findViewById(R.id.dialog_save_btn);

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        saveBtn.setOnClickListener(v -> dialog.dismiss());

        // Load addresses from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(userId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    requireActivity().runOnUiThread(() -> {
                        addressesList.removeAllViews();
                        Object addressesObj = doc.get("addresses");
                        if (addressesObj instanceof java.util.List) {
                            java.util.List<?> addresses = (java.util.List<?>) addressesObj;
                            for (int i = 0; i < addresses.size(); i++) {
                                final int addressIndex = i;
                                Object addr = addresses.get(i);
                                final String addressText = getAddressFromObject(addr);
                                View itemView = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.dialog_address_item, addressesList, false);
                                TextInputEditText addressEt = itemView.findViewById(R.id.dialog_address_item_et);
                                ImageButton editBtn = itemView.findViewById(R.id.dialog_address_edit_btn);
                                ImageButton deleteBtn = itemView.findViewById(R.id.dialog_address_delete_btn);

                                if (addressEt != null) {
                                    addressEt.setText(addressText);
                                    addressEt.setFocusable(false);
                                    addressEt.setClickable(false);
                                }
                                if (editBtn != null) {
                                    editBtn.setVisibility(View.VISIBLE);
                                    editBtn.setOnClickListener(v -> showEditAddressDialog(addressIndex, addressText, addresses));
                                }
                                if (deleteBtn != null) {
                                    deleteBtn.setVisibility(View.VISIBLE);
                                    deleteBtn.setOnClickListener(v -> showDeleteAddressDialog(addressIndex, addressText, addresses));
                                }
                                addressesList.addView(itemView);
                            }
                        }
                    });
                }
            });

        dialog.show();
    }

    private void showEditAddressDialog(int index, String currentAddress, java.util.List<?> addresses) {
        android.util.Log.d("UserFragment", "showEditAddressDialog called for index=" + index);

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_business_description);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        TextView titleTv = dialog.findViewById(R.id.dialog_title);
        TextInputEditText addressEt = dialog.findViewById(R.id.dialog_description_et);
        Button saveBtn = dialog.findViewById(R.id.dialog_save_btn);
        ImageButton closeBtn = dialog.findViewById(R.id.dialog_close_btn);
        ScrollView dialogScrollView = dialog.findViewById(R.id.dialog_business_description_scroll);

        setupDialogKeyboardHints(dialog, dialogScrollView, PREF_USER_DESCRIPTION_DIALOG_SCROLL_HINT_SEEN);
        bindDialogFocusScroll(addressEt, dialogScrollView, saveBtn);

        if (titleTv != null) titleTv.setText("Edit Address");
        if (addressEt != null) addressEt.setText(currentAddress);

        saveBtn.setOnClickListener(v -> {
            String newAddress = addressEt.getText() != null ? addressEt.getText().toString().trim() : "";
            if (!newAddress.isEmpty()) {
                dialog.dismiss();
                // Show confirm dialog before updating
                showAddressConfirmDialog("Edit Address", "Update address to:\n\n" + newAddress, () -> {
                    updateAddressInList(index, newAddress, addresses);
                });
            }
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteAddressDialog(int index, String currentAddress, java.util.List<?> addresses) {
        android.util.Log.d("UserFragment", "showDeleteAddressDialog called for index=" + index);

        // Show confirm dialog before deleting
        showAddressConfirmDialog("Delete Address", "Are you sure you want to delete this address?\n\n" + currentAddress, () -> {
            deleteAddressFromList(index, addresses);
        });
    }

    private void showAddressConfirmDialog(String title, String message, Runnable onConfirm) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_field_change);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        TextView titleTv = dialog.findViewById(R.id.dialog_confirm_title);
        TextView messageTv = dialog.findViewById(R.id.dialog_confirm_message);
        Button yesBtn = dialog.findViewById(R.id.dialog_confirm_yes);
        Button noBtn = dialog.findViewById(R.id.dialog_confirm_no);

        if (titleTv != null) titleTv.setText(title);
        if (messageTv != null) messageTv.setText(message);

        yesBtn.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        noBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateAddressInList(int index, String newAddress, java.util.List<?> addresses) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        // Create new list with updated address
        java.util.List<java.util.Map<String, Object>> updatedAddresses = new java.util.ArrayList<>();
        for (int i = 0; i < addresses.size(); i++) {
            Object addr = addresses.get(i);
            if (addr instanceof java.util.Map) {
                java.util.Map<String, Object> map = new java.util.HashMap<>((java.util.Map<String, Object>) addr);
                if (i == index) {
                    map.put("address", newAddress);
                }
                updatedAddresses.add(map);
            } else if (i == index) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("address", newAddress);
                updatedAddresses.add(map);
            } else {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("address", addr.toString());
                updatedAddresses.add(map);
            }
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(userId)
            .update("addresses", updatedAddresses)
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d("UserFragment", "Address updated successfully");
                // Reload addresses on main screen
                loadAddressesOnMainScreen(requireView());
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("UserFragment", "Failed to update address: " + e.getMessage());
            });
    }

    private void deleteAddressFromList(int index, java.util.List<?> addresses) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        // Create new list without the deleted address
        java.util.List<java.util.Map<String, Object>> updatedAddresses = new java.util.ArrayList<>();
        for (int i = 0; i < addresses.size(); i++) {
            if (i == index) continue; // Skip the one being deleted
            Object addr = addresses.get(i);
            if (addr instanceof java.util.Map) {
                updatedAddresses.add(new java.util.HashMap<>((java.util.Map<String, Object>) addr));
            } else {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("address", addr.toString());
                updatedAddresses.add(map);
            }
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(userId)
            .update("addresses", updatedAddresses)
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d("UserFragment", "Address deleted successfully");
                // Reload addresses on main screen
                loadAddressesOnMainScreen(requireView());
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("UserFragment", "Failed to delete address: " + e.getMessage());
            });
    }

    private void loadAddressesOnMainScreen(View view) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        LinearLayout addressesContainer = view.findViewById(R.id.business_addresses_container);
        TextView addressesCount = view.findViewById(R.id.business_addresses_count);
        TextView viewAllBtn = view.findViewById(R.id.business_view_all_addresses_tv);

        viewAllBtn.setOnClickListener(v -> showAddressesDialog());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(userId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    requireActivity().runOnUiThread(() -> {
                        addressesContainer.removeAllViews();
                        Object addressesObj = doc.get("addresses");
                        if (addressesObj instanceof java.util.List) {
                            java.util.List<?> addresses = (java.util.List<?>) addressesObj;
                            if (addressesCount != null) addressesCount.setText("(" + addresses.size() + ")");

                            int displayCount = Math.min(addresses.size(), 3);
                            for (int i = 0; i < displayCount; i++) {
                                final int addressIndex = i;
                                Object addr = addresses.get(i);
                                final String addressText = getAddressFromObject(addr);
                                View itemView = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.dialog_address_item, addressesContainer, false);
                                TextInputEditText addressEt = itemView.findViewById(R.id.dialog_address_item_et);
                                ImageButton editBtn = itemView.findViewById(R.id.dialog_address_edit_btn);
                                ImageButton deleteBtn = itemView.findViewById(R.id.dialog_address_delete_btn);

                                if (addressEt != null) {
                                    addressEt.setText(addressText);
                                    addressEt.setFocusable(false);
                                    addressEt.setClickable(false);
                                }
                                // Show edit/delete buttons on main screen
                                if (editBtn != null) {
                                    editBtn.setVisibility(View.VISIBLE);
                                    editBtn.setOnClickListener(v -> showEditAddressDialog(addressIndex, addressText, addresses));
                                }
                                if (deleteBtn != null) {
                                    deleteBtn.setVisibility(View.VISIBLE);
                                    deleteBtn.setOnClickListener(v -> showDeleteAddressDialog(addressIndex, addressText, addresses));
                                }

                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.bottomMargin = 8;
                                itemView.setLayoutParams(params);
                                addressesContainer.addView(itemView);
                            }
                        } else {
                            if (addressesCount != null) addressesCount.setText("(0)");
                        }
                    });
                }
            });
    }

    private void resetField(TextInputEditText field, LinearLayout button, ImageView icon, TextInputLayout fieldLayout) {
        field.setFocusable(false);
        field.setFocusableInTouchMode(false);
        field.setClickable(false);
        button.setBackgroundResource(R.drawable.bg_rectangle_edit_btn);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
        ));
        if (fieldLayout != null) {
            fieldLayout.setBoxStrokeColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.light_grey)
            );
        }
    }

    private void saveField(int fieldId, String newValue) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        String userType = getUserType();
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        String collection = MainActivity.USER_TYPE_BUSINESS.equals(userType) ? "businesses" : "users";

        boolean hasValidUpdate = false;
        String fieldName = getFieldName(fieldId);
        if (MainActivity.USER_TYPE_GENERAL.equals(userType)) {
            if (fieldId == R.id.general_user_edit_username_et) {
                updates.put("username", newValue);
                hasValidUpdate = true;
            } else if (fieldId == R.id.general_user_bio_et) {
                updates.put("bio", newValue);
                hasValidUpdate = true;
            } else if (fieldId == R.id.general_user_edit_password_et) {
                if (!newValue.isEmpty()) {
                    updates.put("password", PasswordHash.hash(newValue));
                    hasValidUpdate = true;
                }
            }
        } else if (MainActivity.USER_TYPE_BUSINESS.equals(userType)) {
            if (fieldId == R.id.business_edit_username_et) {
                updates.put("username", newValue);
                hasValidUpdate = true;
            } else if (fieldId == R.id.business_edit_password_et) {
                if (!newValue.isEmpty()) {
                    updates.put("password", PasswordHash.hash(newValue));
                    hasValidUpdate = true;
                }
            } else if (fieldId == R.id.business_edit_desc_et) {
                updates.put("description", newValue);
                hasValidUpdate = true;
            } else if (fieldId == R.id.business_user_bio_et) {
                updates.put("bio", newValue);
                hasValidUpdate = true;
            }
        }

        if (!hasValidUpdate) return;

        db.collection(collection).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("UserFragment", fieldName + " saved successfully");
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), fieldName + " updated successfully", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserFragment", "Failed to save " + fieldName + ": " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to update " + fieldName + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadUserProfileData(View view) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        String userType = getUserType();

        if (userId.isEmpty()) return;

        if (MainActivity.USER_TYPE_GENERAL.equals(userType)) {
            // General user - load from Firestore users collection
            TextView generalUsernameTv = view.findViewById(R.id.general_user_username);
            TextView generalUserIdTv = view.findViewById(R.id.general_user_id_tv);
            TextInputEditText usernameEt = view.findViewById(R.id.general_user_edit_username_et);
            TextInputEditText bioEt = view.findViewById(R.id.general_user_bio_et);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            requireActivity().runOnUiThread(() -> {
                                String username = doc.getString("username");
                                String shortUserId = doc.getString("shortUserId");
                                String bio = doc.getString("bio");

                                // Update header from Firestore
                                if (generalUsernameTv != null) generalUsernameTv.setText(username != null ? username : "");
                                if (generalUserIdTv != null) {
                                    String displayId = shortUserId != null ? shortUserId : "#" + userId.substring(0, Math.min(6, userId.length())).toUpperCase();
                                    generalUserIdTv.setText(displayId);
                                }

                                // Update fields from Firestore
                                if (usernameEt != null) usernameEt.setText(username != null ? username : "");
                                if (bioEt != null) bioEt.setText(bio != null ? bio : "");
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to SharedPreferences
                        requireActivity().runOnUiThread(() -> {
                            String username = prefs.getString("username", "User");
                            String shortUserId = prefs.getString("shortUserId", "#" + userId.substring(0, Math.min(6, userId.length())).toUpperCase());


                            if (generalUsernameTv != null) generalUsernameTv.setText(username);
                            if (generalUserIdTv != null) generalUserIdTv.setText(shortUserId);
                            if (usernameEt != null) usernameEt.setText(username);
                            if (bioEt != null) bioEt.setText("");
                        });
                    });
        } else if (MainActivity.USER_TYPE_BUSINESS.equals(userType)) {
            // Business user - load from Firestore businesses collection
            TextView businessUserTv = view.findViewById(R.id.business_user_tv);
            TextView businessUserIdTv = view.findViewById(R.id.business_user_id_tv);
            TextView businessCategoryTv = view.findViewById(R.id.business_category_tv);

            TextInputEditText usernameEt = view.findViewById(R.id.business_edit_username_et);
            TextInputEditText passwordEt = view.findViewById(R.id.business_edit_password_et);
            TextInputEditText descEt = view.findViewById(R.id.business_edit_desc_et);
            TextInputEditText bioEt = view.findViewById(R.id.business_user_bio_et);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("businesses").document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
requireActivity().runOnUiThread(() -> {
                                String username = doc.getString("username");
                                String shortUserId = doc.getString("shortUserId");
                                String category = doc.getString("businessType");
                                String desc = doc.getString("description");
                                String bio = doc.getString("bio");

                                // Update header from Firestore
                                if (businessUserTv != null) businessUserTv.setText(username != null ? username : "");
                                if (businessUserIdTv != null) {
                                    String displayId = shortUserId != null ? shortUserId : "#" + userId.substring(0, Math.min(6, userId.length())).toUpperCase();
                                    businessUserIdTv.setText(displayId);
                                }
                                if (businessCategoryTv != null) businessCategoryTv.setText(category != null ? category : "Business");

                                // Update fields from Firestore
                                if (usernameEt != null) usernameEt.setText(username != null ? username : "");
                                if (descEt != null) descEt.setText(desc != null ? desc : "");
                                if (bioEt != null) bioEt.setText(bio != null ? bio : "");
                                if (passwordEt != null) passwordEt.setText("");
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to SharedPreferences
                        requireActivity().runOnUiThread(() -> {
                            String username = prefs.getString("username", "BusinessUser");
                            String shortUserId = prefs.getString("shortUserId", "#" + userId.substring(0, Math.min(6, userId.length())).toUpperCase());
                            String category = prefs.getString("businessCategory", "Business");
                            String email = prefs.getString("email", "");
                            String businessDescription = prefs.getString("businessDescription", "");

                            if (businessUserTv != null) businessUserTv.setText(username);
                            if (businessUserIdTv != null) businessUserIdTv.setText(shortUserId);
                            if (businessCategoryTv != null) businessCategoryTv.setText(category);
                            if (descEt != null) descEt.setText(businessDescription.isEmpty() ? "Not set" : businessDescription);
                            if (bioEt != null) bioEt.setText("Not set");
                            if (passwordEt != null) passwordEt.setText("");
                        });
                    });
        }
    }

    private void replayTour() {
        showReplayTourLocation();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).replayOnboarding();
        }
    }
}
