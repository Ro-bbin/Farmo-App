package com.farmo.activities.commonActivities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmo.R;
import com.farmo.network.CommonServices.ConnectionService;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShowConnectionActivity extends AppCompatActivity
        implements ConnectionAdapter.OnConnectionActionListener {

    private static final String TAG = "ShowConnectionActivity";
    private static final int PAGE_SIZE = 20;

    // Views
    private View btnBack;
    private TextView tvUserCount;
    private Button btnTabConnections, btnTabSent, btnTabPending, btnLoadMore;
    private ProgressBar pbLoading;
    private RecyclerView rvConnections;

    // Data
    private final List<Connection> displayedList = new ArrayList<>();
    private ConnectionAdapter adapter;
    private SessionManager sessionManager;

    // Pagination State
    private int currentPage = 1;
    private boolean hasNextPage = false;
    private boolean isLoading = false;

    private enum Tab {
        CONNECTIONS("connected"), 
        SENT("sent"), 
        PENDING("pending");
        
        final String apiType;
        Tab(String apiType) { this.apiType = apiType; }
    }
    private Tab activeTab = Tab.CONNECTIONS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_connection);
        
        sessionManager = new SessionManager(this);
        
        bindViews();
        setupRecyclerView();
        switchToTab(Tab.CONNECTIONS);
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btn_back);
        tvUserCount = findViewById(R.id.tv_user_count);
        btnTabConnections = findViewById(R.id.btn_tab_connections);
        btnTabSent = findViewById(R.id.btn_tab_sent);
        btnTabPending = findViewById(R.id.btn_tab_pending);
        btnLoadMore = findViewById(R.id.btn_load_more);
        pbLoading = findViewById(R.id.pb_loading);
        rvConnections = findViewById(R.id.rv_connections);

        btnBack.setOnClickListener(v -> finish());
        btnTabConnections.setOnClickListener(v -> switchToTab(Tab.CONNECTIONS));
        btnTabSent.setOnClickListener(v -> switchToTab(Tab.SENT));
        btnTabPending.setOnClickListener(v -> switchToTab(Tab.PENDING));
        btnLoadMore.setOnClickListener(v -> loadNextPage());
    }

    private void setupRecyclerView() {
        adapter = new ConnectionAdapter(this, displayedList, this);
        rvConnections.setLayoutManager(new LinearLayoutManager(this));
        rvConnections.setAdapter(adapter);
    }

    private void switchToTab(Tab tab) {
        activeTab = tab;
        currentPage = 1;
        displayedList.clear();
        adapter.notifyDataSetChanged();
        
        updateTabStyles();
        fetchConnections();
    }

    private void loadNextPage() {
        if (!isLoading && hasNextPage) {
            currentPage++;
            fetchConnections();
        }
    }

    private void fetchConnections() {
        isLoading = true;
        pbLoading.setVisibility(View.VISIBLE);
        btnLoadMore.setVisibility(View.GONE);

        ConnectionService.ConnectionsRequest request = new ConnectionService.ConnectionsRequest(
                activeTab.apiType,
                currentPage,
                PAGE_SIZE
        );

        RetrofitClient.getApiService(this)
                .getConnections(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<ConnectionService.ConnectionsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ConnectionService.ConnectionsResponse> call,
                                           @NonNull Response<ConnectionService.ConnectionsResponse> response) {
                        isLoading = false;
                        pbLoading.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            handleResponse(response.body());
                        } else {
                            Toast.makeText(ShowConnectionActivity.this, 
                                    "Failed to load connections", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ConnectionService.ConnectionsResponse> call, 
                                          @NonNull Throwable t) {
                        isLoading = false;
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(ShowConnectionActivity.this, 
                                "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleResponse(ConnectionService.ConnectionsResponse response) {
        List<ConnectionService.UserConnection> results = response.getResults();
        if (results != null) {
            for (ConnectionService.UserConnection user : results) {
                displayedList.add(new Connection(
                        user.getUserId(),
                        user.getFullName(),
                        user.getProfilePic(),
                        mapStatus(user.getStatus())
                ));
            }
        }

        hasNextPage = response.isHasNext();
        adapter.notifyDataSetChanged();
        
        tvUserCount.setText(displayedList.size() + " of " + response.getTotalCount() + " users");
        btnLoadMore.setVisibility(hasNextPage ? View.VISIBLE : View.GONE);
    }

    private Connection.Status mapStatus(String status) {
        if (status == null) return Connection.Status.CONNECTED;
        switch (status.toLowerCase()) {
            case "pending": return Connection.Status.PENDING;
            case "sent": return Connection.Status.SENT;
            default: return Connection.Status.CONNECTED;
        }
    }

    private void updateTabStyles() {
        // Reset all
        btnTabConnections.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnTabSent.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnTabPending.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnTabConnections.setTextColor(0xFF3B82F6);
        btnTabSent.setTextColor(0xFF3B82F6);
        btnTabPending.setTextColor(0xFF3B82F6);

        // Highlight active
        switch (activeTab) {
            case CONNECTIONS:
                btnTabConnections.setBackgroundResource(R.drawable.bg_tab_active);
                btnTabConnections.setTextColor(0xFFFFFFFF);
                break;
            case SENT:
                btnTabSent.setBackgroundResource(R.drawable.bg_tab_active);
                btnTabSent.setTextColor(0xFFFFFFFF);
                break;
            case PENDING:
                btnTabPending.setBackgroundResource(R.drawable.bg_tab_active);
                btnTabPending.setTextColor(0xFFFFFFFF);
                break;
        }
    }

    // ── Adapter Callbacks ──────────────────────────────────────────────────

    @Override
    public void onDelete(Connection connection, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Connection")
                .setMessage("Delete " + connection.getFullName() + "?")
                .setPositiveButton("Confirm", (d, w) -> {
                    displayedList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onAccept(Connection connection, int position) {
        Toast.makeText(this, "Accepted " + connection.getFullName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReject(Connection connection, int position) {
        Toast.makeText(this, "Rejected " + connection.getFullName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCancelRequest(Connection connection, int position) {
        Toast.makeText(this, "Cancelled Request", Toast.LENGTH_SHORT).show();
    }
}