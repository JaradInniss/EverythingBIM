package com.example.everythingbim.ui.admin;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.ScrollView;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUserFragment extends Fragment {
    private static final String PREF_ADMIN_USER_SCROLL_HINT_SEEN =
            "admin_user_scroll_hint_seen";

    // ─── Bundle args ─────────────────────────
    public static final String ARG_INITIAL_TAB = "initial_tab";
    public static final String TAB_GENERAL = "general";
    public static final String TAB_BUSINESS = "business";

    // ─── Tab state ───────────────────────────
    // NONE = default (blank), GENERAL, BUSINESS
    private enum Tab { NONE, GENERAL, BUSINESS }
    private Tab activeTab = Tab.NONE;

    // ────────────────────────────────────────────────────────
    // FACTORY
    // ────────────────────────────────────────────────────────

    public static AdminUserFragment newInstance(String initialTab) {
        AdminUserFragment f = new AdminUserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_TAB, initialTab);
        f.setArguments(args);
        return f;
    }

    // ─── Search filter ────────────────────────
    // ID_NO or USERNAME
    private enum SearchFilter { ID_NO, USERNAME }
    private SearchFilter searchFilter = SearchFilter.USERNAME;

    // ─── Business read filter ─────────────────
    private enum BizFilter { ALL, UNREAD, READ }
    private BizFilter bizFilter = BizFilter.ALL;

    // ─── Business Location read filter ─────────────────
    private enum BizLocFilter { ALL, UNREAD, READ }
    private BizLocFilter bizLocFilter = BizLocFilter.ALL;

    // ─── Views ───────────────────────────────
    private LinearLayout btnGeneral, btnBusiness;
    private EditText searchEt;
    private TextView headerTitle, btnGeneralText, btnBusinessText;
    private ImageView btnGeneralIcon, btnBusinessIcon;
    private View searchResultsCard;
    private LinearLayout searchResultsContainer;
    private TextView filterIdPill, filterUsernamePill;
    private LinearLayout generalContent, businessContent;
    private View generalLegend;
    private ScrollView contentScrollView;

    // General lists
    private LinearLayout generalLocReqContainer;
    private LinearLayout generalSubmissionsContainer;

    // Business lists
    private LinearLayout businessVerReqContainer;
    private LinearLayout businessLocVerReqContainer;
    private TextView bizFilterAll, bizFilterUnread, bizFilterRead;

    // Business Location lists
    private TextView bizLocFilterAll, bizLocFilterUnread, bizLocFilterRead;

    // ─── Firebase ────────────────────────────
    private FirebaseFirestore db;

    // ─── Data ────────────────────────────────
    private List<RequestItem> allLocReqs      = new ArrayList<>();
    private List<RequestItem> allInfoReqs     = new ArrayList<>();
    private List<RequestItem> allDatasetReqs  = new ArrayList<>();
    private List<RequestItem> allBizVerReqs   = new ArrayList<>();
    private List<RequestItem> allBizLocVerReqs = new ArrayList<>();
    private List<UserItem>    allUsers        = new ArrayList<>();
    private List<UserItem>   allBizUsers     = new ArrayList<>();

    // ────────────────────────────────────────────────────────
    // LIFECYCLE
    // ────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_user, container, false);
        db = FirebaseFirestore.getInstance();

        // Bind header views
        btnGeneral  = view.findViewById(R.id.general_user_container);
        btnGeneralText = view.findViewById(R.id.general_user_tv);
        btnGeneralIcon = view.findViewById(R.id.general_user_iv);

        btnBusiness = view.findViewById(R.id.business_user_container);
        btnBusinessText = view.findViewById(R.id.business_user_tv);
        btnBusinessIcon = view.findViewById(R.id.business_user_iv);

        searchEt    = view.findViewById(R.id.users_search_et);
        headerTitle = view.findViewById(R.id.users_header_title);
        contentScrollView = view.findViewById(R.id.users_content_scroll);

        // Bind search card
        searchResultsCard      = view.findViewById(R.id.users_search_results_card);
        searchResultsContainer = view.findViewById(R.id.users_search_results_container);
        filterIdPill           = view.findViewById(R.id.search_filter_id);
        filterUsernamePill     = view.findViewById(R.id.search_filter_username);

        // Bind content panels
        generalContent  = view.findViewById(R.id.users_general_content);
        businessContent = view.findViewById(R.id.users_business_content);
        generalLegend   = view.findViewById(R.id.general_legend);

        // Bind general list containers
        generalLocReqContainer  = view.findViewById(R.id.general_locreq_container);
        generalSubmissionsContainer = view.findViewById(R.id.general_submissions_container);

        // Bind business containers
        businessVerReqContainer = view.findViewById(R.id.business_verreq_container);
        businessLocVerReqContainer = view.findViewById(R.id.business_locverreq_container);
        bizFilterAll    = view.findViewById(R.id.biz_filter_all);
        bizFilterUnread = view.findViewById(R.id.biz_filter_unread);
        bizFilterRead   = view.findViewById(R.id.biz_filter_read);

        // Bind business location containers
        businessLocVerReqContainer = view.findViewById(R.id.business_locverreq_container);
        bizLocFilterAll = view.findViewById(R.id.biz_loc_filter_all);
        bizLocFilterUnread = view.findViewById(R.id.biz_loc_filter_unread);
        bizLocFilterRead = view.findViewById(R.id.biz_loc_filter_read);

        // Default — both panels hidden until a tab is clicked
        setTabState(Tab.NONE);
        setupKeyboardHints(view);

        // Check if an initial tab was passed (from home screen card click)
        if (getArguments() != null) {
            String initialTab = getArguments().getString(ARG_INITIAL_TAB, TAB_GENERAL);
            if (TAB_BUSINESS.equals(initialTab)) {
                setTabState(Tab.BUSINESS);
                loadBusinessData();
            } else if (TAB_GENERAL.equals(initialTab)) {
                setTabState(Tab.GENERAL);
                loadGeneralData();
            }
        }

        // ── Tab buttons ───────────────────────
        btnGeneral.setOnClickListener(v -> {
            setTabState(Tab.GENERAL);
            loadGeneralData();
        });

        btnBusiness.setOnClickListener(v -> {
            setTabState(Tab.BUSINESS);
            loadBusinessData();
        });

        // ── View All buttons ────────────────────
        view.findViewById(R.id.general_locreq_view_all).setOnClickListener(v -> {
            // Mark location section as read (set lastReadTimestamp to now)
            markSectionAsRead("location");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.admin_fragment_container,
                            AdminUserRequestsFragment.newInstance("location"))
                    .addToBackStack(null).commit();
        });

        view.findViewById(R.id.general_submissions_view_all).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.admin_fragment_container,
                                AdminUserRequestsFragment.newInstance("submissions"))
                        .addToBackStack(null).commit());

        view.findViewById(R.id.business_locverreq_view_all).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.admin_fragment_container,
                                AdminUserRequestsFragment.newInstance("business_location"))
                        .addToBackStack(null).commit());

        view.findViewById(R.id.business_verreq_view_all).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.admin_fragment_container,
                                AdminUserRequestsFragment.newInstance("business"))
                        .addToBackStack(null).commit());

        // ── Search text watcher ───────────────
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    showSearchResults(query);
                } else {
                    searchResultsCard.setVisibility(View.GONE);
                }
            }
        });

        // ── Search filter pills ────────────────
        filterIdPill.setOnClickListener(v -> {
            searchFilter = SearchFilter.ID_NO;
            filterIdPill.setBackgroundResource(R.drawable.bg_search_filter_active);
            filterIdPill.setTextColor(android.graphics.Color.WHITE);
            filterUsernamePill.setBackgroundResource(R.drawable.bg_search_filter_inactive);
            filterUsernamePill.setTextColor(
                    getResources().getColor(R.color.black, null));
            String q = searchEt.getText().toString().trim();
            if (q.length() >= 2) showSearchResults(q);
        });

        filterUsernamePill.setOnClickListener(v -> {
            searchFilter = SearchFilter.USERNAME;
            filterUsernamePill.setBackgroundResource(R.drawable.bg_search_filter_active);
            filterUsernamePill.setTextColor(android.graphics.Color.WHITE);
            filterIdPill.setBackgroundResource(R.drawable.bg_search_filter_inactive);
            filterIdPill.setTextColor(
                    getResources().getColor(R.color.black, null));
            String q = searchEt.getText().toString().trim();
            if (q.length() >= 2) showSearchResults(q);
        });

        // ── Business read filter pills ─────────
        bizFilterAll.setOnClickListener(v -> {
            bizFilter = BizFilter.ALL;
            updateBizFilterPills();
            renderBusinessVerReqs();
        });
        bizFilterUnread.setOnClickListener(v -> {
            bizFilter = BizFilter.UNREAD;
            updateBizFilterPills();
            renderBusinessVerReqs();
        });
        bizFilterRead.setOnClickListener(v -> {
            bizFilter = BizFilter.READ;
            updateBizFilterPills();
            renderBusinessVerReqs();
        });

        // Business location filter pills
        bizLocFilterAll.setOnClickListener(v -> {
            bizLocFilter = BizLocFilter.ALL;
            updateBizLocFilterPills();
            renderBusinessLocVerReqs();
        });
        bizLocFilterUnread.setOnClickListener(v -> {
            bizLocFilter = BizLocFilter.UNREAD;
            updateBizLocFilterPills();
            renderBusinessLocVerReqs();
        });
        bizLocFilterRead.setOnClickListener(v -> {
            bizLocFilter = BizLocFilter.READ;
            updateBizLocFilterPills();
            renderBusinessLocVerReqs();
        });

        // View All for business location requests
        view.findViewById(R.id.business_locverreq_view_all).setOnClickListener(v -> {
            ReadStateManager.markBizLocVerSectionRead(requireContext());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.admin_fragment_container,
                            AdminUserRequestsFragment.newInstance("business_location"))
                    .addToBackStack(null).commit();
        });

        return view;
    }

    private void setupKeyboardHints(View root) {
        KeyboardScrollHintHelper.attach(
                root,
                root,
                contentScrollView,
                PREF_ADMIN_USER_SCROLL_HINT_SEEN,
                extraBottom -> {
                    if (contentScrollView == null) {
                        return;
                    }
                    contentScrollView.setPadding(
                            contentScrollView.getPaddingLeft(),
                            contentScrollView.getPaddingTop(),
                            contentScrollView.getPaddingRight(),
                            extraBottom);
                    contentScrollView.setClipToPadding(false);
                });
    }

    // ────────────────────────────────────────────────────────
    // TAB STATE — switches button backgrounds and content visibility
    // ────────────────────────────────────────────────────────

    private void setTabState(Tab tab) {
        activeTab = tab;

        // Clear search
        searchResultsCard.setVisibility(View.GONE);
        searchEt.setText("");

        switch (tab) {
            case GENERAL:
                if (headerTitle != null) {
                    headerTitle.setText("Users' Submissions");
                }
                btnGeneral.setBackgroundResource(R.drawable.bg_rectangle_blue);
                btnGeneralIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
                btnGeneralText.setTextColor(getResources().getColor(R.color.white));

                btnBusiness.setBackgroundResource(R.drawable.bg_users_toggle_inactive);
                btnBusinessIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dark)));
                btnBusinessText.setTextColor(getResources().getColor(R.color.dark));

                generalContent.setVisibility(View.VISIBLE);
                businessContent.setVisibility(View.GONE);
                generalLegend.setVisibility(View.VISIBLE);
                break;
            case BUSINESS:
                if (headerTitle != null) {
                    headerTitle.setText("Users");
                }
                btnBusiness.setBackgroundResource(R.drawable.bg_rectangle_blue);
                btnBusinessIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
                btnBusinessText.setTextColor(getResources().getColor(R.color.white));

                btnGeneral.setBackgroundResource(R.drawable.bg_users_toggle_inactive);
                btnGeneralIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dark)));
                btnGeneralText.setTextColor(getResources().getColor(R.color.dark));

                businessContent.setVisibility(View.VISIBLE);
                generalContent.setVisibility(View.GONE);
                generalLegend.setVisibility(View.GONE);
                break;
            default: // NONE
                if (headerTitle != null) {
                    headerTitle.setText("Users");
                }
                btnGeneral.setBackgroundResource(R.drawable.bg_users_toggle_inactive);
                btnGeneralIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dark)));
                btnGeneralText.setTextColor(getResources().getColor(R.color.dark));
                btnBusiness.setBackgroundResource(R.drawable.bg_users_toggle_inactive);
                btnBusinessIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dark)));
                btnBusinessText.setTextColor(getResources().getColor(R.color.dark));
                generalContent.setVisibility(View.GONE);
                businessContent.setVisibility(View.GONE);
                generalLegend.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-render current tab — data is already in memory, just refresh UI with latest read state
        if (activeTab == Tab.GENERAL) {
            renderGeneralLocReqs();
            renderGeneralSubmissionsReqs();
        } else if (activeTab == Tab.BUSINESS) {
            renderBusinessVerReqs();
            renderBusinessLocVerReqs();
        }
    }

// ────────────────────────────────────────────────────────
    // READ STATE HELPERS — Location / Info requests
    // (delegated to ReadStateManager, shared with AdminUserRequestsFragment)
    // ────────────────────────────────────────────────────────

    private boolean isRequestEffectivelyRead(RequestItem item) {
        if (item.docId == null || item.docId.isEmpty()) return item.read;
        return item.read || ReadStateManager.isRequestRead(requireContext(), item.docId);
    }

    private void markRequestAsRead(RequestItem item) {
        if (item.docId == null || item.docId.isEmpty()) return;
        ReadStateManager.markRequestRead(requireContext(), item.docId);
    }

    // ────────────────────────────────────────────────────────
    // READ STATE HELPERS — Business Verification requests
    // ────────────────────────────────────────────────────────

    private boolean isBizVerEffectivelyRead(RequestItem item) {
        if (item == null || item.docId == null || item.docId.isEmpty()) return false;
        // Check both Firestore read field AND local SharedPreferences
        return item.read || ReadStateManager.isBizVerRead(requireContext(), item.docId);
    }

    private boolean isBizLocVerEffectivelyRead(RequestItem item) {
        if (item == null || item.docId == null || item.docId.isEmpty()) return false;
        // Check both Firestore read field AND local SharedPreferences
        return item.read || ReadStateManager.isBizLocVerRead(requireContext(), item.docId);
    }

    private void markBizVerAsRead(RequestItem item) {
        if (item == null || item.docId == null || item.docId.isEmpty()) return;
        ReadStateManager.markBizVerRead(requireContext(), item.docId);
    }

    private void markBizLocVerAsRead(RequestItem item) {
        if (item == null || item.docId == null || item.docId.isEmpty()) return;
        ReadStateManager.markBizLocVerRead(requireContext(), item.docId);
    }

    private void markSectionAsRead(String sectionType) {
        if ("location".equals(sectionType)) {
            ReadStateManager.markLocationSectionRead(requireContext());
        } else if ("info".equals(sectionType)) {
            ReadStateManager.markInfoSectionRead(requireContext());
        } else if ("dataset".equals(sectionType)) {
            ReadStateManager.markDatasetSectionRead(requireContext());
        }
    }

    // ────────────────────────────────────────────────────────
    // LOAD GENERAL DATA
    // Loads Location Requests and Info Requests for all users
    // ────────────────────────────────────────────────────────

    private void loadGeneralData() {
        if (!isAdded()) return;

        // Clear lists at the START before any Firestore calls
        allUsers.clear();
        allLocReqs.clear();
        allInfoReqs.clear();
        allDatasetReqs.clear();

        // Load all users (for search)
        db.collection("users")
                .whereEqualTo("userType", "general")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        allUsers.add(new UserItem(
                                doc.getId(),
                                doc.getString("username") != null
                                        ? doc.getString("username") : "User",
                                false
                        ));
                    }
                });

        final int[] locCount = {-1};
        final int[] infoCount = {-1};
        final int[] datasetCount = {-1};

        // Location Requests — ordered by createdAt desc, fetch ALL for accurate count
        db.collection("add_location_requests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        allLocReqs.add(buildRequestItem(doc));
                    }
                    locCount[0] = snap.size();
                    if (allLocReqs.isEmpty()) addLocReqPlaceholders();
                    renderGeneralLocReqs();
                    if (infoCount[0] >= 0 && datasetCount[0] >= 0) {
                        updateGeneralCounts(locCount[0], infoCount[0] + datasetCount[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    addLocReqPlaceholders();
                    renderGeneralLocReqs();
                    locCount[0] = 0;
                    if (infoCount[0] >= 0 && datasetCount[0] >= 0) {
                        updateGeneralCounts(0, infoCount[0] + datasetCount[0]);
                    }
                });

        // Information Requests — ordered by createdAt desc, fetch ALL for accurate count
        db.collection("add_information_requests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        RequestItem item = buildRequestItem(doc);
                        item.requestType = "info";
                        allInfoReqs.add(item);
                    }
                    infoCount[0] = snap.size();
                    if (allInfoReqs.isEmpty()) addInfoReqPlaceholders();
                    renderGeneralSubmissionsReqs();
                    if (locCount[0] >= 0 && datasetCount[0] >= 0) {
                        updateGeneralCounts(locCount[0], infoCount[0] + datasetCount[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    addInfoReqPlaceholders();
                    renderGeneralSubmissionsReqs();
                    infoCount[0] = 0;
                    if (locCount[0] >= 0 && datasetCount[0] >= 0) {
                        updateGeneralCounts(locCount[0], datasetCount[0]);
                    }
                });

        db.collection("dataset_image_submissions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        RequestItem item = buildRequestItem(doc);
                        item.requestType = "dataset";
                        allDatasetReqs.add(item);
                    }
                    datasetCount[0] = snap.size();
                    renderGeneralSubmissionsReqs();
                    if (locCount[0] >= 0 && infoCount[0] >= 0) {
                        updateGeneralCounts(locCount[0], infoCount[0] + datasetCount[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    datasetCount[0] = 0;
                    renderGeneralSubmissionsReqs();
                    if (locCount[0] >= 0 && infoCount[0] >= 0) {
                        updateGeneralCounts(locCount[0], infoCount[0]);
                    }
                });
    }

    private void addLocReqPlaceholders() {
        allLocReqs.add(new RequestItem("#201","Hackerton's Pub","User","2026/02/11",false,"ph_loc_201"));
        allLocReqs.add(new RequestItem("#200","Marton Gardens",  "User","2026/02/01",false,"ph_loc_200"));
        allLocReqs.add(new RequestItem("#199","Larton's Cemetery","User","2026/01/28",true, "ph_loc_199"));
    }

    private void addInfoReqPlaceholders() {
        RequestItem item1 = new RequestItem("#88","The Emancipation Statue","User","2026/02/11",false,"ph_info_88");
        item1.requestType = "info";
        allInfoReqs.add(item1);

        RequestItem item2 = new RequestItem("#87","Marton Gardens",         "User","2026/02/01",true, "ph_info_87");
        item2.requestType = "info";
        allInfoReqs.add(item2);

        RequestItem item3 = new RequestItem("#86","St. George Parish Church","User","2026/01/28",true,"ph_info_86");
        item3.requestType = "info";
        allInfoReqs.add(item3);
    }

    private void updateGeneralCounts(int locCount, int submissionsCount) {
        if (!isAdded()) return;
        View view = getView();
        if (view == null) return;

        if (locCount >= 0) {
            TextView tv = view.findViewById(R.id.general_locreq_count);
            if (tv != null) tv.setText(String.valueOf(locCount));
        }
        if (submissionsCount >= 0) {
            TextView tv = view.findViewById(R.id.general_submissions_count);
            if (tv != null) tv.setText(String.valueOf(submissionsCount));
        }
    }

    // ────────────────────────────────────────────────────────
    // LOAD BUSINESS DATA
    // ────────────────────────────────────────────────────────

    private void loadBusinessData() {
        // Clear lists at the START before any Firestore calls
        allBizUsers.clear();
        allBizVerReqs.clear();
        allBizLocVerReqs.clear();

        // Load business users for search
        db.collection("businesses")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        boolean verified = Boolean.TRUE.equals(doc.getBoolean("verified"));
                        // Get business name - registration stores as 'companyName'
                        String name = doc.getString("companyName");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("BusinessName");
                        }
                        if (name == null || name.isEmpty()) {
                            name = "Business";
                        }
                        allBizUsers.add(new UserItem(doc.getId(), name, verified));
                    }
                });

        // Business verification requests — fetch ALL for accurate count
        db.collection("businesses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("AdminUserFragment", "loadBusinessData: snapshot size=" + snap.size());
                    for (QueryDocumentSnapshot doc : snap) {
                        boolean read = Boolean.TRUE.equals(doc.getBoolean("read"));
                        // Get business name - check multiple possible field names
                        // Registration stores as 'companyName', but older code may use 'BusinessName'
                        String name = doc.getString("companyName");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("BusinessName"); // Older field name
                        }
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("name");
                        }
                        if (name == null || name.isEmpty()) {
                            name = "Business";
                        }
                        // Get username for display
                        String submittedBy = doc.getString("username");
                        if (submittedBy == null || submittedBy.isEmpty()) {
                            submittedBy = "User";
                        }
                        // Get additional business details
                        String phone = doc.getString("phone");
                        if (phone == null) phone = "";
                        String email = doc.getString("businessEmail"); // Business email field
                        if (email == null) email = "";
                        String address = doc.getString("address");
                        if (address == null) address = "";
                        String description = doc.getString("description");
                        if (description == null) description = "";
                        String businessType = doc.getString("businessType");
                        if (businessType == null) businessType = "";
                        Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null ? new SimpleDateFormat("yyyy/MM/dd",
                                Locale.getDefault()).format(ts.toDate()) : "";
                        // Get request number from doc ID (first 6 chars uppercased) to match detail screen
                        String requestNum = doc.getId().substring(0, Math.min(6, doc.getId().length())).toUpperCase();
                        RequestItem item = new RequestItem(
                                "#" + requestNum, name,
                                submittedBy, date, read, doc.getId());
                        item.phone = phone;
                        item.email = email;
                        item.address = address;
                        item.description = description;
                        item.businessType = businessType;
                        allBizVerReqs.add(item);
                    }
                    if (allBizVerReqs.isEmpty()) addBizVerReqPlaceholders();
                    renderBusinessVerReqs();
                    updateBizVerReqCount(snap.size());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminUserFragment", "loadBusinessData: failed=" + e.getMessage());
                    addBizVerReqPlaceholders();
                    renderBusinessVerReqs();
                });

        // Business Location Verification Requests
        db.collection("add_business_location_requests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    allBizLocVerReqs.clear();
                    final int totalDocs = snap.size();
                    final int[] processedCount = {0};

                    if (totalDocs == 0) {
                        addBizLocVerReqPlaceholders();
                        renderBusinessLocVerReqs();
                        updateBizLocVerReqCount(0);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snap) {
                        boolean read = Boolean.TRUE.equals(doc.getBoolean("read"));
                        String locationName = doc.getString("locationName");
                        if (locationName == null || locationName.isEmpty()) {
                            locationName = "Business Location";
                        }
                        String userId = doc.getString("userId");
                        String requestNum = doc.getId().substring(0, Math.min(6, doc.getId().length())).toUpperCase();
                        String placeType = doc.getString("placeType");
                        if (placeType == null) placeType = "";
                        Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null ? new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(ts.toDate()) : "";
                        String status = doc.getString("status") != null ? doc.getString("status") : "In Review";
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String coordinates = "";
                        if (lat != null && lng != null) {
                            coordinates = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
                        }

                        // Create item first with placeholder
                        final RequestItem item = new RequestItem("#" + requestNum, locationName, "Loading...", date, read, doc.getId());
                        item.placeType = placeType;
                        item.status = status;
                        item.latitude = lat != null ? lat : 0.0;
                        item.longitude = lng != null ? lng : 0.0;
                        item.coordinates = coordinates;
                        allBizLocVerReqs.add(item);

                        // Look up business name from businesses collection using userId
                        if (userId != null && !userId.isEmpty()) {
                            final String docId = doc.getId();
                            db.collection("businesses").document(userId).get()
                                    .addOnSuccessListener(bizDoc -> {
                                        String businessName = null;
                                        if (bizDoc != null && bizDoc.exists()) {
                                            businessName = bizDoc.getString("businessName");
                                            if (businessName == null || businessName.isEmpty()) {
                                                businessName = bizDoc.getString("companyName");
                                            }
                                            if (businessName == null || businessName.isEmpty()) {
                                                businessName = bizDoc.getString("name");
                                            }
                                        }
                                        if (businessName == null || businessName.isEmpty()) {
                                            businessName = "Business User";

                                        }
                                        // Find and update the item
                                        for (RequestItem i : allBizLocVerReqs) {
                                            if (docId.equals(i.docId)) {
                                                i.submittedBy = businessName;
                                                break;
                                            }
                                        }
                                        processedCount[0]++;
                                        if (processedCount[0] == totalDocs) {
                                            renderBusinessLocVerReqs();
                                            updateBizLocVerReqCount(totalDocs);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Update with fallback
                                        for (RequestItem i : allBizLocVerReqs) {
                                            if (docId.equals(i.docId)) {
                                                i.submittedBy = userId != null ? userId : "Business User";
                                                break;
                                            }
                                        }
                                        processedCount[0]++;
                                        if (processedCount[0] == totalDocs) {
                                            renderBusinessLocVerReqs();
                                            updateBizLocVerReqCount(totalDocs);
                                        }
                                    });
                        } else {
                            item.submittedBy = "Business User";
                            processedCount[0]++;
                            if (processedCount[0] == totalDocs) {
                                renderBusinessLocVerReqs();
                                updateBizLocVerReqCount(totalDocs);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    addBizLocVerReqPlaceholders();
                    renderBusinessLocVerReqs();
                });
    }

    private void addBizLocVerReqPlaceholders() {
        allBizLocVerReqs.add(new RequestItem("#101", "Chefette Restaurant", "User", "2026/02/11", false, "ph_bizloc_101"));
        allBizLocVerReqs.add(new RequestItem("#100", "KFC Wildey", "User", "2026/02/01", true, "ph_bizloc_100"));
        allBizLocVerReqs.add(new RequestItem("#99", "Lighthouse Restaurant", "User", "2026/01/28", false, "ph_bizloc_99"));
        allBizLocVerReqs.get(0).placeType = "Restaurant";
        allBizLocVerReqs.get(1).placeType = "Restaurant";
        allBizLocVerReqs.get(2).placeType = "Restaurant";
    }

    private void updateBizLocVerReqCount(int count) {
        if (!isAdded()) return;
        View view = getView();
        if (view == null) return;
        TextView tv = view.findViewById(R.id.business_locverreq_count);
        if (tv != null) tv.setText(String.valueOf(count));
    }

    private void renderGeneralLocReqs() {
        if (generalLocReqContainer == null) return;
        generalLocReqContainer.removeAllViews();
        int count = 0;
        for (RequestItem item : allLocReqs) {
            if (count >= 3) break; // Only show first 3
            generalLocReqContainer.addView(inflateGeneralRequestRow(item, "location"));
            count++;
        }
    }

    private void renderGeneralSubmissionsReqs() {
        if (generalSubmissionsContainer == null) return;
        generalSubmissionsContainer.removeAllViews();

        List<RequestItem> combined = new ArrayList<>();
        combined.addAll(allInfoReqs);
        combined.addAll(allDatasetReqs);
        combined.sort((a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));

        int count = 0;
        for (RequestItem item : combined) {
            if (count >= 3) break; // Only show first 3
            generalSubmissionsContainer.addView(inflateGeneralRequestRow(item, item.requestType));
            count++;
        }
    }

    private void renderBusinessVerReqs() {
        if (businessVerReqContainer == null) {
            android.util.Log.w("AdminUserFragment", "renderBusinessVerReqs: container is null");
            return;
        }
        android.util.Log.d("AdminUserFragment", "renderBusinessVerReqs: allBizVerReqs size=" + allBizVerReqs.size());
        android.util.Log.d("AdminUserFragment", "businessContent visibility=" + businessContent.getVisibility() + " businessVerReqContainer=" + businessVerReqContainer.getVisibility());
        android.util.Log.d("AdminUserFragment", "bizFilter=" + bizFilter + " allBizVerReqs[3] effectivelyRead=" + (allBizVerReqs.size() > 3 ? isBizVerEffectivelyRead(allBizVerReqs.get(3)) : "N/A"));
        businessVerReqContainer.removeAllViews();
        int renderedCount = 0;
        for (RequestItem item : allBizVerReqs) {
            boolean effectivelyRead = isBizVerEffectivelyRead(item);
            if (bizFilter == BizFilter.UNREAD && effectivelyRead) continue;
            if (bizFilter == BizFilter.READ && !effectivelyRead) continue;
            businessVerReqContainer.addView(inflateRequestRow(item));
            renderedCount++;
            if (renderedCount >= 3) break;
        }
        updateBizVerReqCount(allBizVerReqs.size());
    }

    private void renderBusinessLocVerReqs() {
        if (businessLocVerReqContainer == null) return;
        businessLocVerReqContainer.removeAllViews();
        int renderedCount = 0;
        for (RequestItem item : allBizLocVerReqs) {
            boolean effectivelyRead = isBizLocVerEffectivelyRead(item);
            if (bizLocFilter == BizLocFilter.UNREAD && effectivelyRead) continue;
            if (bizLocFilter == BizLocFilter.READ && !effectivelyRead) continue;
            businessLocVerReqContainer.addView(inflateBizLocVerRequestRow(item));
            renderedCount++;
            if (renderedCount >= 3) break;
        }
        updateBizLocVerReqCount(allBizLocVerReqs.size());
    }

    // Inflates item_admin_user_request.xml for BUSINESS LOCATION requests
    private View inflateBizLocVerRequestRow(RequestItem item) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_user_request,
                        businessLocVerReqContainer, false);

        TextView numTv = row.findViewById(R.id.user_req_number);
        TextView titleTv = row.findViewById(R.id.user_req_title);
        TextView byTv = row.findViewById(R.id.user_req_submitted_by);
        TextView dateTv = row.findViewById(R.id.user_req_date);
        if (numTv != null) numTv.setText(item.number);
        if (titleTv != null) titleTv.setText(item.title);
        if (byTv != null) byTv.setText("Submitted By: " + item.submittedBy);
        if (dateTv != null) dateTv.setText(item.date);

        boolean effectivelyRead = isBizLocVerEffectivelyRead(item);
        row.findViewById(R.id.user_req_dot).setBackgroundResource(
                effectivelyRead ? R.drawable.bg_dot_grey : R.drawable.bg_dot_red);

        // Status dot
        String status = item.status != null ? item.status : "In Review";
        View statusDot = row.findViewById(R.id.status_dot);
        if ("In Review".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_light_blue);
        } else if ("Completed".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_green);
        } else if ("Rejected".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_orange);
        } else {
            statusDot.setBackgroundResource(R.drawable.bg_dot_grey);
        }

        TextView viewBtn = row.findViewById(R.id.user_req_view_btn);
        viewBtn.setTextColor(effectivelyRead
                ? android.graphics.Color.parseColor("#9e9e9e")
                : android.graphics.Color.parseColor("#203088"));

        viewBtn.setOnClickListener(v -> {
            markBizLocVerAsRead(item);

            View dot = row.findViewById(R.id.user_req_dot);
            dot.setBackgroundResource(R.drawable.bg_dot_grey);
            dot.invalidate();
            viewBtn.setTextColor(android.graphics.Color.parseColor("#9e9e9e"));
            viewBtn.invalidate();

            renderBusinessLocVerReqs();

            Fragment detail = AdminBizLocationDetailFragment.newInstance(
                    item.docId,
                    item.number,
                    item.status,
                    item.date,
                    item.submittedBy,
                    item.title,
                    item.placeType,
                    item.coordinates,
                    item.latitude,
                    item.longitude
            );

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.admin_fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });

        return row;
    }

    private void addBizVerReqPlaceholders() {
        allBizVerReqs.add(new RequestItem("#523","Hackerton's Pub",      "User","2026/02/11",false,"ph_biz_523"));
        allBizVerReqs.add(new RequestItem("#522","Jordan's Supermarket", "User","2026/02/01",true, "ph_biz_522"));
        allBizVerReqs.add(new RequestItem("#521","Grillz By Kriz",       "User","2026/01/28",false,"ph_biz_521"));
        allBizVerReqs.get(0).businessType = "Restaurant";
        allBizVerReqs.get(1).businessType = "Retail";
        allBizVerReqs.get(2).businessType = "Food";
    }

    private void updateBizVerReqCount(int count) {
        if (!isAdded()) return;
        View view = getView();
        if (view == null) return;
        TextView tv = view.findViewById(R.id.business_verreq_count);
        if (tv != null) tv.setText(String.valueOf(count));
    }

// Inflates item_admin_user_request.xml for GENERAL requests
    private View inflateGeneralRequestRow(RequestItem item, String requestType) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_user_request,
                        generalLocReqContainer, false);

        TextView numTv = row.findViewById(R.id.user_req_number);
        TextView titleTv = row.findViewById(R.id.user_req_title);
        TextView byTv = row.findViewById(R.id.user_req_submitted_by);
        TextView dateTv = row.findViewById(R.id.user_req_date);
        if (numTv != null) numTv.setText(item.number);
        if (titleTv != null) titleTv.setText(item.title);
        if (byTv != null) byTv.setText("Submitted By: " + item.submittedBy);
        if (dateTv != null) dateTv.setText(item.date);

        TextView typeBadge = row.findViewById(R.id.user_req_type_badge);
        if ("info".equals(requestType)) {
            typeBadge.setVisibility(View.VISIBLE);
            typeBadge.setText("Info Request");
            typeBadge.setBackgroundResource(R.drawable.bg_submission_badge_info);
            typeBadge.setTextColor(android.graphics.Color.WHITE);
        } else if ("dataset".equals(requestType)) {
            typeBadge.setVisibility(View.VISIBLE);
            typeBadge.setText("Dataset Image");
            typeBadge.setBackgroundResource(R.drawable.bg_submission_badge_dataset);
            typeBadge.setTextColor(android.graphics.Color.parseColor("#8B0000"));
        } else {
            typeBadge.setVisibility(View.GONE);
        }

        // Dot colour — use effective read state (Firestore read OR local SharedPreferences)
        boolean effectivelyRead = isRequestEffectivelyRead(item);
        row.findViewById(R.id.user_req_dot).setBackgroundResource(
                effectivelyRead ? R.drawable.bg_dot_grey : R.drawable.bg_dot_red);

        // Status dot — based on item.status (In Review, Completed, Rejected)
        String status = item.status != null ? item.status : "In Review";
        View statusDot = row.findViewById(R.id.status_dot);
        if ("In Review".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_light_blue);
        } else if ("Completed".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_green);
        } else if ("Rejected".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_orange);
        } else {
            statusDot.setBackgroundResource(R.drawable.bg_dot_grey);
        }

        // View button colour — cobalt if unread, grey if read
        TextView viewBtn = row.findViewById(R.id.user_req_view_btn);
        viewBtn.setTextColor(effectivelyRead
                ? android.graphics.Color.parseColor("#9e9e9e")
                : android.graphics.Color.parseColor("#203088"));

        viewBtn.setOnClickListener(v -> {
            // Mark as read in SharedPreferences before navigating
            markRequestAsRead(item);

            // Immediately update this row's dot and button colour
            View dot = row.findViewById(R.id.user_req_dot);
            dot.setBackgroundResource(R.drawable.bg_dot_grey);
            dot.invalidate();
            viewBtn.setTextColor(android.graphics.Color.parseColor("#9e9e9e"));
            viewBtn.invalidate();

            // Re-render current tab so state is consistent across tab switches
            if (activeTab == Tab.GENERAL) {
                renderGeneralLocReqs();
                renderGeneralSubmissionsReqs();
            }

            Fragment detail;
            if ("location".equals(requestType)) {
                detail = AdminLocationRequestDetailsFragment.newInstance(
                        item.docId,
                        item.number,
                        item.status,
                        item.date,
                        item.submittedBy,
                        item.locationName,
                        item.coordinates,
                        item.description,
                          item.placeType,
                          item.reason,
                          item.latitude,
                          item.longitude
                  );
              } else if ("dataset".equals(requestType)) {
                  detail = AdminDatasetSubmissionDetailFragment.newInstance(item.docId);
              } else {
                  detail = AdminInfoRequestDetailFragment.newInstance(
                          item.docId,
                        item.number,
                        item.status,
                        item.date,
                        item.submittedBy,
                        item.locationName,
                        item.coordinates,
                        item.description,
                        item.placeType,
                        item.latitude,
                        item.longitude
                );
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.admin_fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });

        return row;
    }

    // Inflates item_admin_user_request.xml for BUSINESS requests
    private View inflateRequestRow(RequestItem item) {
        android.util.Log.d("AdminUserFragment", "inflateRequestRow: called for docId=" + item.docId);
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_user_request,
                        businessVerReqContainer, false);

        TextView numTv = row.findViewById(R.id.user_req_number);
        TextView titleTv = row.findViewById(R.id.user_req_title);
        TextView byTv = row.findViewById(R.id.user_req_submitted_by);
        TextView dateTv = row.findViewById(R.id.user_req_date);
        if (numTv != null) numTv.setText(item.number);
        if (titleTv != null) titleTv.setText(item.title);
        if (byTv != null) byTv.setText("Submitted By: " + item.submittedBy);
        if (dateTv != null) dateTv.setText(item.date);

        // Dot colour — use effective read state (SharedPreferences only)
        boolean effectivelyRead = isBizVerEffectivelyRead(item);
        row.findViewById(R.id.user_req_dot).setBackgroundResource(
                effectivelyRead ? R.drawable.bg_dot_grey : R.drawable.bg_dot_red);

        // Status dot
        String status = item.status != null ? item.status : "In Review";
        View statusDot = row.findViewById(R.id.status_dot);
        if ("In Review".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_light_blue);
        } else if ("Completed".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_green);
        } else if ("Rejected".equals(status)) {
            statusDot.setBackgroundResource(R.drawable.bg_dot_orange);
        } else {
            statusDot.setBackgroundResource(R.drawable.bg_dot_grey);
        }

        // View button colour — cobalt if unread, grey if read
        TextView viewBtn = row.findViewById(R.id.user_req_view_btn);
        viewBtn.setTextColor(effectivelyRead
                ? android.graphics.Color.parseColor("#9e9e9e")
                : android.graphics.Color.parseColor("#203088"));

        viewBtn.setOnClickListener(v -> {
            // Mark as read in SharedPreferences before navigating
            android.util.Log.d("AdminUserFragment", "View clicked docId=" + item.docId);
            markBizVerAsRead(item);

            // Immediately update THIS row's dot and button colour so it changes right now
            View dot = row.findViewById(R.id.user_req_dot);
            dot.setBackgroundResource(R.drawable.bg_dot_grey);
            dot.invalidate();
            viewBtn.setTextColor(android.graphics.Color.parseColor("#9e9e9e"));
            viewBtn.invalidate();

            // Also re-render all visible rows so that if the user switches filter
            // tabs and comes back, state is consistent with SharedPreferences
            renderBusinessVerReqs();

            Fragment detail = AdminBizVerificationDetailFragment.newInstance(
                    item.docId,
                    item.number,
                    item.status,
                    item.date,
                    item.submittedBy,
                    item.title,
                    item.phone,
                    item.email,
                    item.address,
                    item.description,
                    item.businessType
            );

            android.util.Log.d("AdminUserFragment", "Navigating to detail, current visible=" + this.isVisible() + " isResumed=" + this.isResumed());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.admin_fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });

        return row;
    }

    // ────────────────────────────────────────────────────────
    // SEARCH — shows matching users in the search results card
    // ────────────────────────────────────────────────────────

    private void showSearchResults(String query) {
        searchResultsContainer.removeAllViews();

        List<UserItem> source = (activeTab == Tab.BUSINESS) ? allBizUsers : allUsers;
        List<UserItem> matches = new ArrayList<>();

        String lowerQuery = query.toLowerCase();

        for (UserItem user : source) {
            boolean match = searchFilter == SearchFilter.USERNAME
                    ? user.username.toLowerCase().contains(lowerQuery)
                    : user.userId.toLowerCase().contains(lowerQuery);
            if (match) matches.add(user);
        }

        // If Firestore data not loaded yet, show placeholder results
        if (source.isEmpty()) {
            if (activeTab == Tab.BUSINESS) {
                matches.add(new UserItem("biz001", "Chefette", true));
            } else {
                matches.add(new UserItem("usr001", "HarryOsborne12", false));
                matches.add(new UserItem("usr002", "BevOsborne", false));
            }
        }

        for (UserItem user : matches) {
            searchResultsContainer.addView(inflateUserSearchRow(user));
        }

        searchResultsCard.setVisibility(View.VISIBLE);
    }

// Inflates item_admin_user_search_result.xml and binds data
    private View inflateUserSearchRow(UserItem user) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_user_search_result,
                        searchResultsContainer, false);

        TextView usernameTv = row.findViewById(R.id.search_result_username);
        if (usernameTv != null) usernameTv.setText(user.username);

        // Show verified badge for business users
        ImageView verifiedBadge = row.findViewById(R.id.search_result_verified);
        if (verifiedBadge != null) {
            verifiedBadge.setVisibility(user.verified ? View.VISIBLE : View.GONE);
        }

        row.findViewById(R.id.search_result_view_btn).setOnClickListener(v -> {
            // TODO: navigate to user detail
        });

        return row;
    }

    // ────────────────────────────────────────────────────────
    // BUSINESS FILTER PILLS — update active state visually
    // ────────────────────────────────────────────────────────

    private void updateBizFilterPills() {
        // Reset all to inactive
        bizFilterAll.setBackgroundResource(R.drawable.bg_search_filter_inactive);
        bizFilterUnread.setBackgroundResource(R.drawable.bg_biz_unread_pill);
        bizFilterRead.setBackgroundResource(R.drawable.bg_biz_read_pill);

        int white  = android.graphics.Color.WHITE;
        int dark   = getResources().getColor(R.color.black, null);

        bizFilterAll.setTextColor(dark);
        bizFilterUnread.setTextColor(dark);
        bizFilterRead.setTextColor(dark);

        switch (bizFilter) {
            case ALL:
                bizFilterAll.setBackgroundResource(R.drawable.bg_search_filter_active);
                bizFilterAll.setTextColor(white);
                break;
            case UNREAD:
                bizFilterUnread.setBackgroundResource(R.drawable.bg_search_filter_active);
                bizFilterUnread.setTextColor(white);
                break;
            case READ:
                bizFilterRead.setBackgroundResource(R.drawable.bg_search_filter_active);
                bizFilterRead.setTextColor(white);
                break;
        }
    }

    private void updateBizLocFilterPills() {
        bizLocFilterAll.setBackgroundResource(R.drawable.bg_search_filter_inactive);
        bizLocFilterUnread.setBackgroundResource(R.drawable.bg_biz_unread_pill);
        bizLocFilterRead.setBackgroundResource(R.drawable.bg_biz_read_pill);

        int white  = android.graphics.Color.WHITE;
        int dark   = getResources().getColor(R.color.black, null);

        bizLocFilterAll.setTextColor(dark);
        bizLocFilterUnread.setTextColor(dark);
        bizLocFilterRead.setTextColor(dark);

        switch (bizLocFilter) {
            case ALL:
                bizLocFilterAll.setBackgroundResource(R.drawable.bg_search_filter_active);
                bizLocFilterAll.setTextColor(white);
                break;
            case UNREAD:
                bizLocFilterUnread.setBackgroundResource(R.drawable.bg_search_filter_active);
                bizLocFilterUnread.setTextColor(white);
                break;
            case READ:
                bizLocFilterRead.setBackgroundResource(R.drawable.bg_search_filter_active);
                bizLocFilterRead.setTextColor(white);
                break;
        }
    }

    // ────────────────────────────────────────────────────────
    // HELPERS — build model from Firestore document
    // ────────────────────────────────────────────────────────

    private RequestItem buildRequestItem(QueryDocumentSnapshot doc) {
        String number = doc.contains("number")
                ? "#" + doc.getLong("number")
                : "#" + doc.getId().substring(0, 3).toUpperCase();
        String name = doc.getString("locationName") != null
                ? doc.getString("locationName")
                : doc.getString("title") != null
                ? doc.getString("title") : "Request";
        String submittedBy = doc.getString("submittedByUsername") != null
                ? doc.getString("submittedByUsername") : "User";
        boolean read = Boolean.TRUE.equals(doc.getBoolean("read"));
        Timestamp ts = doc.getTimestamp("createdAt");
        String date = ts != null ? new SimpleDateFormat("yyyy/MM/dd",
                Locale.getDefault()).format(ts.toDate()) : "";

        RequestItem item = new RequestItem(number, name, submittedBy, date, read, doc.getId());
        item.createdAtMillis = ts != null ? ts.toDate().getTime() : 0L;

        item.status = doc.getString("status") != null ? doc.getString("status") : "In Review";
        item.locationName = doc.getString("locationName") != null ? doc.getString("locationName") : "";
        item.description = doc.getString("description") != null ? doc.getString("description") : "";
        item.placeType = doc.getString("placeType") != null ? doc.getString("placeType") : "";
        item.reason = doc.getString("reason") != null ? doc.getString("reason") : "";
        item.imageUrl = doc.getString("imageUrl") != null ? doc.getString("imageUrl") : "";
        item.userNote = doc.getString("userNote") != null ? doc.getString("userNote") : "";

        Double lat = doc.getDouble("latitude");
        Double lng = doc.getDouble("longitude");
        if (lat != null && lng != null) {
            item.latitude = lat;
            item.longitude = lng;
            item.coordinates = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
        }

        if (item.coordinates.isEmpty()) {
            Double userLat = doc.getDouble("userLatitude");
            Double userLng = doc.getDouble("userLongitude");
            if (userLat != null && userLng != null) {
                item.latitude = userLat;
                item.longitude = userLng;
                item.coordinates = String.format(Locale.getDefault(), "%.5f, %.5f", userLat, userLng);
            }
        }

        item.phone = doc.getString("phone") != null ? doc.getString("phone") : "";
        item.email = doc.getString("email") != null ? doc.getString("email") : "";
        item.address = doc.getString("address") != null ? doc.getString("address") : "";
        item.businessType = doc.getString("businessType") != null ? doc.getString("businessType") : "";
        if (item.businessType.isEmpty()) {
            item.businessType = doc.getString("type") != null ? doc.getString("type") : "";
        }
        if (item.businessType.isEmpty()) {
            item.businessType = doc.getString("businessCategory") != null ? doc.getString("businessCategory") : "";
        }

        return item;
    }

    // ────────────────────────────────────────────────────────
    // DATA MODELS
    // ────────────────────────────────────────────────────────

    private static class RequestItem {
        String number, title, submittedBy, date, docId, status;
        boolean read;
        String requestType;
        long createdAtMillis;
        String locationName, description, placeType, reason, coordinates;
        double latitude, longitude;
        String imageUrl, userNote;
        String phone, email, address, businessType;

        RequestItem(String number, String title, String submittedBy,
                    String date, boolean read, String docId) {
            this.number = number; this.title = title;
            this.submittedBy = submittedBy; this.date = date;
            this.read = read; this.docId = docId;
            this.status = "In Review";
            this.requestType = "info";
            this.createdAtMillis = 0L;
            this.locationName = ""; this.description = "";
            this.placeType = ""; this.reason = "";
            this.coordinates = "";
            this.latitude = 0.0; this.longitude = 0.0;
            this.imageUrl = ""; this.userNote = "";
            this.phone = ""; this.email = "";
            this.address = ""; this.businessType = "";
        }
    }

    private static class UserItem {
        String userId, username;
        boolean verified;

        UserItem(String userId, String username, boolean verified) {
            this.userId   = userId;
            this.username = username;
            this.verified = verified;
        }
    }
}
