package com.example.everythingbim.ui.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class UserFragment extends Fragment {

    private UserViewModel userViewModel;
    private FragmentUserBinding binding;

    private View layoutGuestUser, layoutGeneralUser, layoutBusinessUser;
    private View generalContentSubmissions, generalContentMyProfile, generalContentSettings;
    private View generalIndSubmissions, generalIndMyProfile, generalIndSettings;

    private View businessContentSubmissions, businessContentMyProfile, businessContentSettings;
    private View businessIndSubmissions, businessIndMyProfile, businessIndSettings;

    private Button  guestLoginBtn;

    private TextView guestAdminAccessBtn, guestReplayTourBtn;

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
        setupEditToggle(binding.getRoot(), R.id.general_user_edit_email_et, R.id.general_user_edit_email_btn, R.id.general_user_edit_email_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.general_user_edit_password_et, R.id.general_user_edit_password_btn, R.id.general_user_edit_password_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.general_user_bio_et, R.id.general_user_edit_bio_btn, R.id.general_user_edit_bio_btn_iv);

        setupEditToggle(binding.getRoot(), R.id.business_edit_email_et, R.id.business_edit_email_btn, R.id.business_edit_email_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.business_edit_password_et, R.id.business_edit_password_btn, R.id.business_edit_password_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.business_edit_address_et, R.id.business_edit_address_btn, R.id.business_edit_address_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.business_edit_desc_et, R.id.business_edit_desc_btn, R.id.business_edit_desc_btn_iv);
        setupEditToggle(binding.getRoot(), R.id.business_user_bio_et, R.id.business_user_edit_bio_btn, R.id.business_user_edit_bio_btn_iv);

        return binding.getRoot();
    }

    private void bindViews() {
        layoutGuestUser = binding.layoutGuestUser;
        layoutGeneralUser = binding.layoutGeneralUser;
        layoutBusinessUser = binding.layoutBusinessUser;

        generalContentSubmissions = binding.generalContentSubmissions;
        generalContentMyProfile = binding.generalContentMyProfile;
        generalContentSettings = binding.generalContentSettings;

        businessContentSubmissions = binding.businessContentSubmissions;
        businessContentMyProfile = binding.businessContentMyProfile;
        businessContentSettings = binding.businessContentSettings;

        generalIndSubmissions = binding.generalTabSubmissions;
        generalIndMyProfile = binding.generalTabMyProfile;
        generalIndSettings = binding.generalTabSettings;

        businessIndSubmissions = binding.businessTabSubmissions;
        businessIndMyProfile = binding.businessTabMyProfile;
        businessIndSettings = binding.businessTabSettings;

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
        guestReplayTourBtn.setOnClickListener(v -> replayTour());

        // Linear Layouts
        generalUserLogOutBtn.setOnClickListener(v -> performLogout());
        businessUserLogOutBtn.setOnClickListener(v -> performLogout());

        generalReplayTourBtn.setOnClickListener(v -> replayTour());
        businessReplayTourBtn.setOnClickListener(v -> replayTour());

        guestRegisterGeneralBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), GeneralRegistration.class)));
        guestRegisterBusinessBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), BusinessRegistration.class)));

        newLocationReqBtn.setOnClickListener(v -> navigateTo(new AddLocationRequestFragment()));
        viewLocationReqBtn.setOnClickListener(v -> navigateTo(new ViewAddLocationRequestFragment()));
        viewCompletedLocationReqBtn.setOnClickListener(v -> navigateTo(new ViewCompletedLocationRequestFragment()));

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

        generalIndSubmissions.setOnClickListener(v -> switchGeneralTab(0));
        generalIndMyProfile.setOnClickListener(v -> switchGeneralTab(1));
        generalIndSettings.setOnClickListener(v -> switchGeneralTab(2));
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
        switchBusinessTab(0);

        businessIndSubmissions.setOnClickListener(v -> switchBusinessTab(0));
        businessIndMyProfile.setOnClickListener(v -> switchBusinessTab(1));
        businessIndSettings.setOnClickListener(v -> switchBusinessTab(2));
    }

    private void switchBusinessTab(int tab) {
        businessContentSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        businessContentMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        businessContentSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        businessIndSubmissions.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        businessIndMyProfile.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        businessIndSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
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

        button.setOnClickListener(v -> toggleFieldEdit(field, button, icon));
    }

    private void toggleFieldEdit(TextInputEditText field, LinearLayout button, ImageView icon) {
        // TextInputLayout is the direct parent of TextInputEditText
        ViewParent parent = field.getParent();
        TextInputLayout fieldLayout = (parent instanceof TextInputLayout) ? (TextInputLayout) parent : null;
        if (!field.isFocusable()) {
            field.setFocusable(true);
            field.setFocusableInTouchMode(true);
            field.setClickable(true);
            field.requestFocus();
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
            field.setFocusable(false);
            field.setFocusableInTouchMode(false);
            field.setClickable(false);
            // Grey background, black icon
            button.setBackgroundResource(R.drawable.bg_rectangle_pale_slate);
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
            ));
            // Reset field outline to grey
            if (fieldLayout != null) {
                fieldLayout.setBackgroundResource(R.drawable.bg_rectangle_pale_slate);
            }

            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(field.getWindowToken(), 0);

            // TODO: save updated field value to backend here
        }
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void replayTour() {
        showReplayTourLocation();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).replayOnboarding();
        }
    }
}
