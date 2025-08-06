package gabrielLopes.project2425.DevPackage.QuickFixApp.navBar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage.ServiceDetailsActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage.ServiceListAdapter;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage.RequestsListAdapter;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;
/**
 * Activity that displays the logged-in user's requests and accepted services.
 * <p>
 * Provides tab switching, filtering, and query searching.
 */
public class MyServicesAndRequestsActivity extends AppCompatActivity {

    private Button tabRequests, tabServices;
    private Spinner statusSpinner;
    private EditText budgetInput, searchInput;
    private ImageView searchIcon;
    private ListView listView;

    private boolean isShowingRequests = true;
    private int userId;

    private final List<ApiModels.RequestResponse> requestsList = new ArrayList<>();
    private final List<ApiModels.ServiceResponse> servicesList = new ArrayList<>();
    private RequestsListAdapter requestsAdapter;
    private ServiceListAdapter servicesAdapter;

    /**
     * Initializes the layout, views, listeners, and loads the initial data based on selected tab.
     *
     * @param savedInstanceState Bundle containing saved state, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_services_requests_area_layout);
        NavBarHandler.highlightSelected(this, R.id.nav_my_services_requests);

        userId = SharedPrefHelper.getUserId(this);
        if (userId == -1)
            finish();

        initializeViews();
        setupListeners();
        setupStatusSpinner();
        loadData();
        NavBarHandler.setup(this, userId);
    }

    /**
     * Finds and initializes all view components, sets default tab and adapter.
     */
    private void initializeViews() {
        tabRequests = findViewById(R.id.tabRequests);
        tabServices = findViewById(R.id.tabServices);
        statusSpinner = findViewById(R.id.statusSpinner);
        budgetInput = findViewById(R.id.budgetInput);
        searchInput = findViewById(R.id.searchInput);
        searchIcon = findViewById(R.id.searchIcon);
        listView = findViewById(R.id.listView);

        tabRequests.setBackgroundResource(R.drawable.tab_selected);
        tabServices.setBackgroundResource(R.drawable.tab_unselected);

        requestsAdapter = new RequestsListAdapter(this, requestsList);
        listView.setAdapter(requestsAdapter);

        if (isShowingRequests) {
            budgetInput.setHint("Max budget (€)");
        } else {
            budgetInput.setHint("Min. budget (€)");
        }

    }

    /**
     * Sets up click and interaction listeners for tabs, search, input fields, and item clicks.
     */
    private void setupListeners() {
        tabRequests.setOnClickListener(v -> {
            if (!isShowingRequests) {
                isShowingRequests = true;
                tabRequests.setBackgroundResource(R.drawable.tab_selected);
                tabServices.setBackgroundResource(R.drawable.tab_unselected);
                budgetInput.setHint("Max budget (€)");
                loadData();
            }
        });

        tabServices.setOnClickListener(v -> {
            if (isShowingRequests) {
                isShowingRequests = false;
                tabServices.setBackgroundResource(R.drawable.tab_selected);
                tabRequests.setBackgroundResource(R.drawable.tab_unselected);
                budgetInput.setHint("Min. budget (€)");
                loadData();
            }
        });

        searchIcon.setOnClickListener(v -> loadData());

        budgetInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) loadData();
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            loadData();
            return true;
        });

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (isShowingRequests) {
                ApiModels.RequestResponse request = requestsList.get(position);
                Intent intent = new Intent(MyServicesAndRequestsActivity.this, ServiceDetailsActivity.class);
                intent.putExtra("request_id", request.id);
                intent.putExtra("is_request", true);
                startActivity(intent);

            } else {
                ApiModels.ServiceResponse service = servicesList.get(position);
                Intent intent = new Intent(MyServicesAndRequestsActivity.this, ServiceDetailsActivity.class);
                intent.putExtra("service_id", service.id);
                intent.putExtra("is_request", false);
                startActivity(intent);
            }
        });
    }

    /**
     * Initializes the spinner used to filter items by status.
     */
    private void setupStatusSpinner() {
        String[] statuses = new String[]{"Any", "Pending", "Accepted", "Started", "Finished", "Paid", "Closed", "Cancelled"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
    }

    /**
     * Determines current tab (requests or services) and loads filtered data accordingly.
     */
    private void loadData() {
        String selectedStatus = statusSpinner.getSelectedItem().toString();
        String query = searchInput.getText().toString().toLowerCase(Locale.ROOT).trim();
        String budgetText = budgetInput.getText().toString().trim();
        double budget;

        if (budgetText.isEmpty()) {
            if (isShowingRequests)
                budget = Double.MAX_VALUE;
            else
                budget = 0;
        } else
            budget = Double.parseDouble(budgetText);

        if(isShowingRequests)
            loadRequests(budget, selectedStatus, query);
        else
            loadServices(budget, selectedStatus, query);
    }

    /**
     * Loads and filters the requests created by the user.
     *
     * @param budget          The maximum price filter for requests.
     * @param selectedStatus  The status filter (Pending, Accepted, etc.).
     * @param query           A free-text search query.
     */
    private void loadRequests(double budget, String selectedStatus, String query) {
        requestsList.clear();
        requestsAdapter.notifyDataSetChanged();
        listView.setAdapter(requestsAdapter);

        String statusParam;
        if (selectedStatus.equalsIgnoreCase("any"))
            statusParam = null;
        else
            statusParam = selectedStatus;

        ApiManager.getClientRequests(statusParam, query, budget, new ApiManager.RequestsListCallback() {
            @Override
            public void onSuccess(List<ApiModels.RequestResponse> serviceRequests) {
                runOnUiThread(() -> {
                    requestsList.clear();
                    requestsList.addAll(serviceRequests);
                    requestsAdapter.notifyDataSetChanged();
                    listView.setAdapter(requestsAdapter);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> showToast("Failed to load requests: " + message));
            }
        });
    }

    /**
     * Loads and filters services the user has accepted as a provider.
     *
     * @param budget          The minimum price filter for services.
     * @param selectedStatus  The status filter (Pending, Started, etc.).
     * @param query           A free-text search query.
     */
    private void loadServices(double budget, String selectedStatus, String query) {
        servicesList.clear();
        servicesAdapter = new ServiceListAdapter(MyServicesAndRequestsActivity.this, servicesList);
        listView.setAdapter(servicesAdapter);

        String statusParam;
        if (selectedStatus.equalsIgnoreCase("any"))
            statusParam = null;
        else
            statusParam = selectedStatus;

        ApiManager.getServicesByProvider(userId, statusParam, query, budget, new ApiManager.ServiceListCallback() {
            @Override
            public void onSuccess(List<ApiModels.ServiceResponse> services) {
                runOnUiThread(() -> {
                    servicesList.clear();
                    servicesList.addAll(services);
                    servicesAdapter.notifyDataSetChanged();
                    listView.setAdapter(servicesAdapter);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> showToast("Failed to load services: " + errorMessage));
            }
        });
    }

    /**
     * Reloads data when returning to this activity to ensure updates are reflected.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    /**
     * Displays a short Toast message to the user.
     *
     * @param message The message to show.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
