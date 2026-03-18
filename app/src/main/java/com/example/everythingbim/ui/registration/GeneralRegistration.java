package com.example.everythingbim.ui.registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.login.Login;

public class GeneralRegistration extends AppCompatActivity implements View.OnClickListener {

    private GeneralRegViewModel viewModel;

    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView loginOption;
    private ViewFlipper genRegFormViewFlipper;
    private LinearLayout nextBttn, prevBttn;
    private Button submitBttn;

    // Variables
    private int currPage = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_general_registration);

        viewModel = new ViewModelProvider(this).get(GeneralRegViewModel.class);

        initViews();
        setupObservers();

        // Set Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        // ImageViews
        userTypeGeneral = findViewById(R.id.user_type_general);
        userTypeBusiness = findViewById(R.id.user_type_business);
        userTypeGeneral.setOnClickListener(this);
        userTypeBusiness.setOnClickListener(this);
        // TextViews
        loginOption = findViewById(R.id.login_opt);
        loginOption.setOnClickListener(this);
        // ViewFlipper
        genRegFormViewFlipper = findViewById(R.id.reg_form_viewflipper);
        // Linear Layouts
        nextBttn = findViewById(R.id.next_bttn);
        prevBttn = findViewById(R.id.prev_bttn);
        nextBttn.setOnClickListener(this);
        prevBttn.setOnClickListener(this);
        // Buttons
        submitBttn = findViewById(R.id.submit_bttn);
        submitBttn.setOnClickListener(this);
    }

    private void setupObservers() {
        viewModel.getCurrentPage().observe(this, page -> {
            genRegFormViewFlipper.setDisplayedChild(page);
        });

        // Observe Navigation
        viewModel.getNavigationEvent().observe(this, destination -> {
            if (destination != null) {
                Intent intent = new Intent(GeneralRegistration.this, destination);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onClick(View view) {
        int bttn_id = view.getId();

        if (bttn_id == R.id.user_type_general) {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "Currently General User", Toast.LENGTH_SHORT).show();
            }
        }
        else if (bttn_id == R.id.user_type_business) {
            viewModel.navigateTo(BusinessRegistration.class);
        }
        else if (bttn_id == R.id.login_opt) {
            // Return to Log in
            viewModel.navigateTo(Login.class);
        }
        else if (bttn_id == R.id.next_bttn) {
            viewModel.nextPage();
        }
        else if (bttn_id == R.id.prev_bttn) {
            viewModel.prevPage();
        }
        else if (bttn_id == R.id.submit_bttn) {
            Toast.makeText(this, "Submitted", Toast.LENGTH_SHORT).show();
            viewModel.navigateTo(Login.class);
        }

    }
}