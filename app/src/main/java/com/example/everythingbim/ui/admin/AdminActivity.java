package com.example.everythingbim.ui.admin;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.everythingbim.R;

public class AdminActivity extends AppCompatActivity {
    public static final String EXTRA_NOTIFICATION_TARGET = "notification_target";
    public static final String EXTRA_NOTIFICATION_DOC_ID = "notification_doc_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.admin_fragment_container, resolveInitialFragment())
                    .commit();
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, resolveInitialFragment())
                .commit();
    }

    private androidx.fragment.app.Fragment resolveInitialFragment() {
        android.content.Intent intent = getIntent();
        if (intent == null) {
            return new AdminFragment();
        }

        String target = intent.getStringExtra(EXTRA_NOTIFICATION_TARGET);
        String docId = intent.getStringExtra(EXTRA_NOTIFICATION_DOC_ID);
        if (docId == null || docId.trim().isEmpty()) {
            return new AdminFragment();
        }

        if (AdminNotificationHelper.TARGET_INFO_DETAIL.equals(target)) {
            return AdminInfoRequestDetailFragment.newInstance(docId);
        }
        if (AdminNotificationHelper.TARGET_LOCATION_DETAIL.equals(target)) {
            return AdminLocationRequestDetailsFragment.newInstance(docId);
        }
        if (AdminNotificationHelper.TARGET_DATASET_DETAIL.equals(target)) {
            return AdminDatasetSubmissionDetailFragment.newInstance(docId);
        }

        return new AdminFragment();
    }
}
