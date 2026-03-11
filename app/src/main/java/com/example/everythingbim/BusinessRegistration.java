package com.example.everythingbim;

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

public class BusinessRegistration extends AppCompatActivity {

    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView loginOption;
    private ViewFlipper regFormViewFlipper;
    private LinearLayout nextBttn1, nextBttn2, nextBttn3, prevBttn1, prevBttn2, prevBttn3;
    private Button submitBttn;

    // Variables
    private int currPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_business_registration);

        // ImageViews
        userTypeGeneral = findViewById(R.id.user_type_general);
        userTypeBusiness = findViewById(R.id.user_type_business);
        // TextViews
        loginOption = findViewById(R.id.login_opt);
        // ViewFlipper
        regFormViewFlipper = findViewById(R.id.reg_form_viewflipper);
        // Buttons
        nextBttn1 = findViewById(R.id.next_bttn1);
        nextBttn2 = findViewById(R.id.next_bttn2);
        nextBttn3 = findViewById(R.id.next_bttn3);
        prevBttn1 = findViewById(R.id.prev_bttn1);
        prevBttn2 = findViewById(R.id.prev_bttn2);
        prevBttn3 = findViewById(R.id.prev_bttn3);
        submitBttn = findViewById(R.id.submit_bttn);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set ImageViews On Click Behavior
        userTypeBusiness.setOnClickListener(v -> {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "Business User", Toast.LENGTH_SHORT).show();
            }
        });

        userTypeGeneral.setOnClickListener(v -> {
            Intent intent = new Intent(BusinessRegistration.this, GeneralRegistration.class);
            startActivity(intent);
        });

        // Return to Login
        loginOption.setOnClickListener(v -> {
            Intent intent = new Intent(BusinessRegistration.this, Login.class);
            startActivity(intent);
        });

        // Form Page Switching
        View.OnClickListener nextListener = v -> {
            regFormViewFlipper.showNext();
            currPage++;
        };
        nextBttn1.setOnClickListener(nextListener);
        nextBttn2.setOnClickListener(nextListener);
        nextBttn3.setOnClickListener(nextListener);

        View.OnClickListener prevListener = v -> {
            regFormViewFlipper.showPrevious();
            currPage--;
        };
        prevBttn1.setOnClickListener(prevListener);
        prevBttn2.setOnClickListener(prevListener);
        prevBttn3.setOnClickListener(prevListener);

        submitBttn.setOnClickListener(v -> {
            Toast.makeText(this, "Submitted", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BusinessRegistration.this, Login.class);
            startActivity(intent);
        });
    }
}