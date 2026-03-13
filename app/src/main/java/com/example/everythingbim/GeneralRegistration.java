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

public class GeneralRegistration extends AppCompatActivity implements View.OnClickListener {

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

        // Set Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Form Next Page Function
    private void nextPage() {
        genRegFormViewFlipper.showNext();
        currPage++;
    }

    // Form Previous Page Function
    private void prevPage() {
        genRegFormViewFlipper.showPrevious();
        currPage--;
    }

    @Override
    public void onClick(View view) {
        int bttn_id = view.getId();

        if (bttn_id == R.id.user_type_general) {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "General User", Toast.LENGTH_SHORT).show();
            }
        }
        else if (bttn_id == R.id.user_type_business) {
            Intent intent = new Intent(GeneralRegistration.this, BusinessRegistration.class);
            startActivity(intent);
        }
        else if (bttn_id == R.id.login_opt) {
            // Return to Login
            Intent intent = new Intent(GeneralRegistration.this, Login.class);
            startActivity(intent);
        }
        else if (bttn_id == R.id.next_bttn) {
            nextPage();
        }
        else if (bttn_id == R.id.prev_bttn) {
            prevPage();
        }
        else if (bttn_id == R.id.submit_bttn) {
            Toast.makeText(this, "Submitted", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(GeneralRegistration.this, Login.class);
            startActivity(intent);
        }

    }
}