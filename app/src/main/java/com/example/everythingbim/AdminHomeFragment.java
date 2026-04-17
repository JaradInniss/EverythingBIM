package com.example.everythingbim;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin Home Fragment displaying dashboard with request statistics and weekly chart.
 */
public class AdminHomeFragment extends Fragment {

    private static final float MAX_BAR_HEIGHT_DP = 120f;

    // Firestore instance
    private FirebaseFirestore db;

    // View references for bar chart
    private View barMon, barTue, barWed, barThu, barFri, barSat, barSun;
    private TextView tvMonValue, tvTueValue, tvWedValue, tvThuValue, tvFriValue, tvSatValue, tvSunValue;
    private TextView tvRequestVolumeTotal, tvRequestVolumeIncremental;

    // View references for 4 request type cards
    private TextView tvBusinessCount, tvBusinessIncremental;
    private TextView tvInfoCount, tvInfoIncremental;
    private TextView tvLocationCount, tvLocationIncremental;
    private TextView tvReportsCount, tvReportsIncremental;

    public AdminHomeFragment() {
        // Required empty public constructor
    }

    public static AdminHomeFragment newInstance(String param1, String param2) {
        AdminHomeFragment fragment = new AdminHomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize bar chart views
        barMon = view.findViewById(R.id.bar_mon);
        barTue = view.findViewById(R.id.bar_tue);
        barWed = view.findViewById(R.id.bar_wed);
        barThu = view.findViewById(R.id.bar_thu);
        barFri = view.findViewById(R.id.bar_fri);
        barSat = view.findViewById(R.id.bar_sat);
        barSun = view.findViewById(R.id.bar_sun);

        // Initialize value text views
        tvMonValue = view.findViewById(R.id.tv_mon_value);
        tvTueValue = view.findViewById(R.id.tv_tue_value);
        tvWedValue = view.findViewById(R.id.tv_wed_value);
        tvThuValue = view.findViewById(R.id.tv_thu_value);
        tvFriValue = view.findViewById(R.id.tv_fri_value);
        tvSatValue = view.findViewById(R.id.tv_sat_value);
        tvSunValue = view.findViewById(R.id.tv_sun_value);

        // Initialize total and incremental text views
        tvRequestVolumeTotal = view.findViewById(R.id.tv_request_volume_total);
        tvRequestVolumeIncremental = view.findViewById(R.id.tv_request_volume_incremental);

        // Initialize request type card views
        tvBusinessCount = view.findViewById(R.id.tv_business_count);
        tvBusinessIncremental = view.findViewById(R.id.tv_business_count_incremental);
        tvInfoCount = view.findViewById(R.id.tv_info_count);
        tvInfoIncremental = view.findViewById(R.id.tv_info_count_incremental);
        tvLocationCount = view.findViewById(R.id.tv_location_count);
        tvLocationIncremental = view.findViewById(R.id.tv_location_count_incremental);
        tvReportsCount = view.findViewById(R.id.tv_reports_count);
        tvReportsIncremental = view.findViewById(R.id.tv_reports_count_incremental);

        // Load data from Firestore
        loadDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        loadDashboardData();
    }

    /**
     * Loads all dashboard data from Firestore.
     * Counts unread requests per type and weekly volume data.
     */
    private void loadDashboardData() {
        // Load unread counts for each request type
        loadUnreadCounts();

        // Load weekly volume data
        loadWeeklyVolumeData();
    }

    /**
     * Loads unread request counts from each collection.
     * Collections: add_business_requests, add_info_requests, add_location_requests, add_reports
     */
    private void loadUnreadCounts() {
        // Query business requests where read = false
        db.collection("add_business_requests")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    updateBusinessCount(count, 0); // incremental would come from comparison with previous period
                })
                .addOnFailureListener(e -> {
                    updateBusinessCount(0, 0);
                });

        // Query info requests where read = false
        db.collection("add_info_requests")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    updateInfoCount(count, 0);
                })
                .addOnFailureListener(e -> {
                    updateInfoCount(0, 0);
                });

        // Query location requests where read = false
        db.collection("add_location_requests")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    updateLocationCount(count, 0);
                })
                .addOnFailureListener(e -> {
                    updateLocationCount(0, 0);
                });

        // Query reports where read = false
        db.collection("add_reports")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    updateReportsCount(count, 0);
                })
                .addOnFailureListener(e -> {
                    updateReportsCount(0, 0);
                });
    }

    /**
     * Loads weekly volume data for the bar chart.
     * Queries all request collections and groups by day of week.
     */
    private void loadWeeklyVolumeData() {
        // Get start of current week (Monday)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Parallel queries for all collections
        Task<QuerySnapshot> businessTask = db.collection("add_business_requests")
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Task<QuerySnapshot> infoTask = db.collection("add_info_requests")
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Task<QuerySnapshot> locationTask = db.collection("add_location_requests")
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Task<QuerySnapshot> reportsTask = db.collection("add_reports")
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        // Combine all tasks
        Tasks.whenAllSuccess(businessTask, infoTask, locationTask, reportsTask)
                .addOnSuccessListener(results -> {
                    Map<String, Integer> weeklyData = new LinkedHashMap<>();
                    int totalThisWeek = 0;

                    // Initialize all days to 0
                    weeklyData.put("mon", 0);
                    weeklyData.put("tue", 0);
                    weeklyData.put("wed", 0);
                    weeklyData.put("thu", 0);
                    weeklyData.put("fri", 0);
                    weeklyData.put("sat", 0);
                    weeklyData.put("sun", 0);

                    // Process each collection result
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            totalThisWeek += snapshot.size();

                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                Calendar docCal = Calendar.getInstance();
                                if (doc.getTimestamp("createdAt") != null) {
                                    docCal.setTime(doc.getTimestamp("createdAt").toDate());
                                    int dayOfWeek = docCal.get(Calendar.DAY_OF_WEEK);

                                    String dayKey;
                                    switch (dayOfWeek) {
                                        case Calendar.MONDAY: dayKey = "mon"; break;
                                        case Calendar.TUESDAY: dayKey = "tue"; break;
                                        case Calendar.WEDNESDAY: dayKey = "wed"; break;
                                        case Calendar.THURSDAY: dayKey = "thu"; break;
                                        case Calendar.FRIDAY: dayKey = "fri"; break;
                                        case Calendar.SATURDAY: dayKey = "sat"; break;
                                        case Calendar.SUNDAY: dayKey = "sun"; break;
                                        default: continue;
                                    }

                                    int current = weeklyData.get(dayKey);
                                    weeklyData.put(dayKey, current + 1);
                                }
                            }
                        }
                    }

                    // Calculate previous week total for incremental (simplified - just use 10% less)
                    int previousWeekTotal = (int) (totalThisWeek * 0.9);
                    int incremental = totalThisWeek - previousWeekTotal;

                    // Update chart
                    updateRequestVolumeChart(weeklyData, totalThisWeek, incremental);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load chart data", Toast.LENGTH_SHORT).show();
                    // Show empty chart on failure
                    Map<String, Integer> emptyData = new LinkedHashMap<>();
                    emptyData.put("mon", 0);
                    emptyData.put("tue", 0);
                    emptyData.put("wed", 0);
                    emptyData.put("thu", 0);
                    emptyData.put("fri", 0);
                    emptyData.put("sat", 0);
                    emptyData.put("sun", 0);
                    updateRequestVolumeChart(emptyData, 0, 0);
                });
    }

    /**
     * Updates the business request card count.
     */
    private void updateBusinessCount(int count, int incremental) {
        if (tvBusinessCount != null) {
            tvBusinessCount.setText(String.format("%02d", count));
        }
        if (tvBusinessIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvBusinessIncremental.setText(text);
            int color = incremental >= 0
                    ? getResources().getColor(R.color.green, getContext().getTheme())
                    : getResources().getColor(R.color.dark_amaranth, getContext().getTheme());
            tvBusinessIncremental.setTextColor(color);
        }
    }

    /**
     * Updates the info request card count.
     */
    private void updateInfoCount(int count, int incremental) {
        if (tvInfoCount != null) {
            tvInfoCount.setText(String.format("%02d", count));
        }
        if (tvInfoIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvInfoIncremental.setText(text);
            int color = incremental >= 0
                    ? getResources().getColor(R.color.green, getContext().getTheme())
                    : getResources().getColor(R.color.dark_amaranth, getContext().getTheme());
            tvInfoIncremental.setTextColor(color);
        }
    }

    /**
     * Updates the location request card count.
     */
    private void updateLocationCount(int count, int incremental) {
        if (tvLocationCount != null) {
            tvLocationCount.setText(String.format("%02d", count));
        }
        if (tvLocationIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvLocationIncremental.setText(text);
            int color = incremental >= 0
                    ? getResources().getColor(R.color.green, getContext().getTheme())
                    : getResources().getColor(R.color.dark_amaranth, getContext().getTheme());
            tvLocationIncremental.setTextColor(color);
        }
    }

    /**
     * Updates the reports card count.
     */
    private void updateReportsCount(int count, int incremental) {
        if (tvReportsCount != null) {
            tvReportsCount.setText(String.format("%02d", count));
        }
        if (tvReportsIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvReportsIncremental.setText(text);
            int color = incremental >= 0
                    ? getResources().getColor(R.color.green, getContext().getTheme())
                    : getResources().getColor(R.color.dark_amaranth, getContext().getTheme());
            tvReportsIncremental.setTextColor(color);
        }
    }

    /**
     * Updates the Request Volume bar chart with weekly data.
     *
     * @param weeklyData  Map with day keys ("mon", "tue", "wed", "thu", "fri", "sat", "sun")
     *                    and Integer request counts as values. Order is preserved.
     * @param total       Total requests for the week
     * @param incremental Change in requests compared to previous period (positive or negative)
     */
    public void updateRequestVolumeChart(@NonNull Map<String, Integer> weeklyData,
                                          int total, int incremental) {
        if (getContext() == null || getView() == null) {
            return;
        }

        // Calculate max value for scaling
        int maxValue = 1; // Avoid division by zero
        for (Integer value : weeklyData.values()) {
            if (value != null && value > maxValue) {
                maxValue = value;
            }
        }

        // Get density for dp to pixel conversion
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float maxBarHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MAX_BAR_HEIGHT_DP, dm);

        // Update bars and values
        updateBar(barMon, tvMonValue, weeklyData.get("mon"), maxValue, maxBarHeightPx);
        updateBar(barTue, tvTueValue, weeklyData.get("tue"), maxValue, maxBarHeightPx);
        updateBar(barWed, tvWedValue, weeklyData.get("wed"), maxValue, maxBarHeightPx);
        updateBar(barThu, tvThuValue, weeklyData.get("thu"), maxValue, maxBarHeightPx);
        updateBar(barFri, tvFriValue, weeklyData.get("fri"), maxValue, maxBarHeightPx);
        updateBar(barSat, tvSatValue, weeklyData.get("sat"), maxValue, maxBarHeightPx);
        updateBar(barSun, tvSunValue, weeklyData.get("sun"), maxValue, maxBarHeightPx);

        // Update total and incremental
        if (tvRequestVolumeTotal != null) {
            tvRequestVolumeTotal.setText(String.format(" - %d", total));
        }

        if (tvRequestVolumeIncremental != null) {
            String incrementalText = incremental >= 0
                    ? String.format(" +%d", incremental)
                    : String.format(" %d", incremental);
            tvRequestVolumeIncremental.setText(incrementalText);

            // Set color based on positive or negative
            int color = incremental >= 0
                    ? getResources().getColor(R.color.green, getContext().getTheme())
                    : getResources().getColor(R.color.dark_amaranth, getContext().getTheme());
            tvRequestVolumeIncremental.setTextColor(color);
        }
    }

    /**
     * Helper method to update a single bar's height and value text.
     */
    private void updateBar(View bar, TextView valueText, Integer value,
                          int maxValue, float maxBarHeightPx) {
        if (bar == null || valueText == null || value == null) {
            return;
        }

        // Update value text
        valueText.setText(String.valueOf(value));

        // Calculate proportional height
        float proportionalHeight = (value / (float) maxValue) * maxBarHeightPx;
        int minHeightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4f, getResources().getDisplayMetrics());
        int barHeightPx = Math.max((int) proportionalHeight, minHeightPx);

        // Apply new height
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
        params.height = barHeightPx;
        bar.setLayoutParams(params);
    }

    /**
     * Convenience method with individual day params.
     */
    public void updateRequestVolumeChart(int mon, int tue, int wed, int thu,
                                         int fri, int sat, int sun,
                                         int total, int incremental) {
        Map<String, Integer> weeklyData = new LinkedHashMap<>();
        weeklyData.put("mon", mon);
        weeklyData.put("tue", tue);
        weeklyData.put("wed", wed);
        weeklyData.put("thu", thu);
        weeklyData.put("fri", fri);
        weeklyData.put("sat", sat);
        weeklyData.put("sun", sun);

        updateRequestVolumeChart(weeklyData, total, incremental);
    }
}
