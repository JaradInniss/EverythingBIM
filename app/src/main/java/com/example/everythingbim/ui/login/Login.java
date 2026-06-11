package com.example.everythingbim.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.databinding.ActivityLoginBinding;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity implements View.OnClickListener {
    private static final String PREF_LOGIN_SCROLL_HINT_SEEN = "login_scroll_hint_seen";

    private LoginViewModel viewModel;
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;

    private ImageView generalUserIcon;
    private ImageView businessUserIcon;
    private TextView userTypeText;
    private TextView createAccountOption;
    private TextView generalUserText;
    private TextView businessUserText;
    private Button loginBttn;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private ProgressBar progressBar;
    private TextView errorTextView;
    private TextView adminAccessHint;
    private LinearLayout generalUserContainer;
    private LinearLayout businessUserContainer;
    private View userTypeSelection;

    private final Handler handler = new Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.setSharedPreferences(sharedPreferences);

        initViews();
        applyPreselectedUserType();
        setupObservers();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setupKeyboardInsets();
    }

    private void setupKeyboardInsets() {
        int initialLeft = binding.loginScroll.getPaddingLeft();
        int initialTop = binding.loginScroll.getPaddingTop();
        int initialRight = binding.loginScroll.getPaddingRight();
        int initialBottom = binding.loginScroll.getPaddingBottom();

        KeyboardScrollHintHelper.attach(
                binding.getRoot(),
                binding.loginScroll,
                binding.loginScroll,
                PREF_LOGIN_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> binding.loginScroll.setPadding(
                        initialLeft,
                        initialTop,
                        initialRight,
                        initialBottom + keyboardExtraBottom
                )
        );
    }

    private void initViews() {
        generalUserIcon = binding.generalUserIv;
        businessUserIcon = binding.businessUserIv;

        generalUserContainer = binding.generalUserContainer;
        businessUserContainer = binding.businessUserContainer;
        userTypeSelection = binding.userTypeSelection;
        generalUserContainer.setOnClickListener(this);
        businessUserContainer.setOnClickListener(this);

        generalUserText = binding.generalUserTv;
        businessUserText = binding.businessUserTv;
        userTypeText = binding.userTypeText;
        adminAccessHint = binding.adminAccessHint;
        errorTextView = binding.loginError;
        createAccountOption = binding.createAccountOpt;
        createAccountOption.setOnClickListener(this);

        loginBttn = binding.loginBttn;
        loginBttn.setOnClickListener(this);

        emailEditText = binding.loginEmailEt;
        passwordEditText = binding.loginPasswordEt;

        progressBar = binding.loginProgressBar;

        View.OnLongClickListener resetHintsListener = v -> {
            KeyboardScrollHintHelper.resetAllHints(this);
            Toast.makeText(this, "Scroll hints reset", Toast.LENGTH_SHORT).show();
            return true;
        };
        binding.logo.setOnLongClickListener(resetHintsListener);
        binding.titleTxt.setOnLongClickListener(v -> {
            KeyboardScrollHintHelper.showPreview(binding.getRoot());
            Toast.makeText(this, "Scroll hint preview", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void applyPreselectedUserType() {
        String preselectedUserType = getIntent().getStringExtra("preselectedUserType");
        if (preselectedUserType == null) {
            return;
        }

        try {
            viewModel.setSelectedUserType(UserType.valueOf(preselectedUserType));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void setupObservers() {
        viewModel.getSelectedUserType().observe(this, userType -> {
            if (userType == UserType.GENERAL) {
                userTypeSelection.setVisibility(View.VISIBLE);
                adminAccessHint.setVisibility(View.GONE);
                generalUserIcon.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.white)));
                businessUserIcon.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.dark)));

                generalUserText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                businessUserText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.dark));

                generalUserContainer.setBackgroundResource(R.drawable.bg_rectangle_blue);
                businessUserContainer.setBackgroundResource(0);

                userTypeText.setText("General User");
            } else if (userType == UserType.BUSINESS) {
                userTypeSelection.setVisibility(View.VISIBLE);
                adminAccessHint.setVisibility(View.GONE);
                generalUserIcon.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.dark)));
                businessUserIcon.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.white)));

                generalUserText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.dark));
                businessUserText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));

                generalUserContainer.setBackgroundResource(0);
                businessUserContainer.setBackgroundResource(R.drawable.bg_rectangle_blue);
                userTypeText.setText("Business User");
            } else if (userType == UserType.ADMIN) {
                userTypeSelection.setVisibility(View.GONE);
                adminAccessHint.setVisibility(View.VISIBLE);
                userTypeText.setText("Administrator");
            } else {
                userTypeSelection.setVisibility(View.VISIBLE);
                adminAccessHint.setVisibility(View.GONE);
                generalUserIcon.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.dark)));
                businessUserIcon.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.dark)));

                generalUserText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.dark));
                businessUserText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.dark));

                generalUserContainer.setBackgroundResource(0);
                businessUserContainer.setBackgroundResource(0);
                userTypeText.setText("");
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                loginBttn.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                loginBttn.setEnabled(true);
            }
        });

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorFields().observe(this, errors -> {
            handler.removeCallbacksAndMessages(null);
            if (errors == null) {
                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                errorTextView.setVisibility(View.GONE);
                binding.tilLoginEmail.setError(null);
                binding.tilLoginPassword.setError(null);
                return;
            }

            HashMap<Integer, TextInputLayout> fieldMap = new HashMap<>();
            fieldMap.put(R.id.login_email_et, binding.tilLoginEmail);
            fieldMap.put(R.id.login_password_et, binding.tilLoginPassword);

            for (Map.Entry<Integer, String> entry : errors.entrySet()) {
                int fieldId = entry.getKey();
                String error = entry.getValue();

                TextInputLayout fieldLayout = fieldMap.get(fieldId);
                if (fieldLayout == null) {
                    continue;
                }

                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                fieldLayout.setError(error);
                fieldLayout.setErrorEnabled(true);
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(error);

                handler.postDelayed(() -> {
                    TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                    errorTextView.setVisibility(View.GONE);
                    fieldLayout.setError(null);
                    fieldLayout.setErrorEnabled(false);
                }, 2000);
            }
        });

        viewModel.getNavigationEvent().observe(this, command -> {
            if (command != null) {
                Intent intent = new Intent(Login.this, command.getDestination());
                if (command.getExtras() != null) {
                    intent.putExtras(command.getExtras());
                }
                String pendingAction = getIntent().getStringExtra(MainActivity.EXTRA_PENDING_ACTION);
                if (pendingAction != null && (MainActivity.class.equals(command.getDestination())
                        || com.example.everythingbim.ui.registration.GeneralRegistration.class.equals(command.getDestination())
                        || com.example.everythingbim.ui.registration.BusinessRegistration.class.equals(command.getDestination()))) {
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, pendingAction);
                    copyDatasetResumeExtras(getIntent(), intent);
                }
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.login_bttn) {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            viewModel.onLoginClicked(email, password);
        } else if (id == R.id.general_user_container) {
            viewModel.setSelectedUserType(UserType.GENERAL);
        } else if (id == R.id.business_user_container) {
            viewModel.setSelectedUserType(UserType.BUSINESS);
        } else if (id == R.id.create_account_opt) {
            viewModel.onCreateAccountClicked();
        }
    }

    private void copyDatasetResumeExtras(Intent source, Intent destination) {
        if (source == null || destination == null) {
            return;
        }

        copyExtraIfPresent(source, destination, com.example.everythingbim.ui.home.AIIdentifier.EXTRA_IMAGE_URI);
        copyExtraIfPresent(source, destination, com.example.everythingbim.ui.home.AIIdentifier.EXTRA_IMAGE_SOURCE);
        copyExtraIfPresent(source, destination, com.example.everythingbim.ui.home.AIIdentifier.EXTRA_DISPLAY_NAME);
        copyExtraIfPresent(source, destination, com.example.everythingbim.ui.home.AIIdentifier.EXTRA_GPS_AVAILABLE);
        copyExtraIfPresent(source, destination, com.example.everythingbim.ui.home.AIIdentifier.EXTRA_GPS_PERMISSION_GRANTED);
        copyExtraIfPresent(source, destination, com.example.everythingbim.ui.home.AIIdentifier.EXTRA_USER_LATITUDE);
        copyExtraIfPresent(source, destination, com.example.everythingbim.ui.home.AIIdentifier.EXTRA_USER_LONGITUDE);
    }

    private void copyExtraIfPresent(Intent source, Intent destination, String key) {
        if (!source.hasExtra(key) || source.getExtras() == null) {
            return;
        }

        Object value = source.getExtras().get(key);
        if (value instanceof String) {
            destination.putExtra(key, (String) value);
        } else if (value instanceof Boolean) {
            destination.putExtra(key, (Boolean) value);
        } else if (value instanceof Double) {
            destination.putExtra(key, (Double) value);
        }
    }
}
