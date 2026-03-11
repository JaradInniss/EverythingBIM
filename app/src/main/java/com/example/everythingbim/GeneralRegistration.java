package com.example.everythingbim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GeneralRegistration extends AppCompatActivity {

    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView loginOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_general_registration);

        // ImageViews
        userTypeGeneral = findViewById(R.id.user_type_general);
        userTypeBusiness = findViewById(R.id.user_type_business);
        // TextViews
        loginOption = findViewById(R.id.login_opt);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set ImageViews On Click Behavior
        userTypeGeneral.setOnClickListener(v -> {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "General User", Toast.LENGTH_SHORT).show();
            }
        });

        userTypeBusiness.setOnClickListener(v -> {
            Intent intent = new Intent(GeneralRegistration.this, BusinessRegistration.class);
            startActivity(intent);
        });

        // Return to Login
        loginOption.setOnClickListener(v -> {
           Intent intent = new Intent(GeneralRegistration.this, Login.class);
           startActivity(intent);
        });
    }
}