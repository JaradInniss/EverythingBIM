package com.example.everythingbim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Login extends AppCompatActivity {
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView userTypeText, createAccountOption;
    private String currUserType;
    private String[] userTypes = {"General", "Business"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        currUserType = userTypes[0];

        // ImageViews
        userTypeGeneral = findViewById(R.id.user_type_general);
        userTypeBusiness = findViewById(R.id.user_type_business);
        // TextViews
        userTypeText = findViewById(R.id.user_type_txt);
        createAccountOption = findViewById(R.id.create_account_opt);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set click listeners for the user type icons
        userTypeGeneral.setOnClickListener(v -> {
            // Set background yellow
            userTypeGeneral.setBackgroundResource(R.drawable.pill_bg_yllw);
            // Remove background of Business User Icon
            userTypeBusiness.setBackgroundResource(0);
            // Change text to "General User"
            userTypeText.setText("General User");
            currUserType = userTypes[0];
        });

        userTypeBusiness.setOnClickListener(v -> {
            // Set background yellow
            userTypeBusiness.setBackgroundResource(R.drawable.pill_bg_yllw);
            // Remove background of Business User Icon
            userTypeGeneral.setBackgroundResource(0);
            // Change text to "General User"
            userTypeText.setText("Business User");
            currUserType = userTypes[1];
        });

        // Create Account option
        createAccountOption.setOnClickListener(v -> {
            // Create Account option
            if (currUserType.equals("General")) {
                Intent intent = new Intent(Login.this, GeneralRegistration.class);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(Login.this, BusinessRegistration.class);
                startActivity(intent);
            }
        });
    }

}