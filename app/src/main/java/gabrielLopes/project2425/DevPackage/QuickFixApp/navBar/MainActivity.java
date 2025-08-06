package gabrielLopes.project2425.DevPackage.QuickFixApp.navBar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage.ProvidersListAdapter;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage.ProviderDetailsActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage.RequestsListAdapter;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage.ServiceDetailsActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage.LoginActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;

/**
 * Main system activity that displays either client service requests or available service providers,
 * depending on the mode.
 * <p>
 * Supports filters, search, distance-based sorting, and switching views through the bottom nav.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "filters";
    private static final String KEY_FILTER_TYPE = "typeFilter";
    private static final String KEY_REQUEST_BUDGET = "requestBudgetFilter";
    private static final String KEY_PROVIDER_BUDGET = "providerBudgetFilter";
    private static final String KEY_SEARCH_QUERY = "searchQuery";
    private static final String KEY_FILTER_DISTANCE = "distanceFilter";

    private EditText searchInput;
    private TextView resetFiltersBtn;
    private Spinner spinner;
    private EditText budgetInput;
    private EditText distanceInput;
    private ImageView searchIcon;
    private ListView listView;
    private List<ApiModels.RequestResponse> requestsList = new ArrayList<>();
    private List<ApiModels.ServiceProviderResponse> serviceProvidersList = new ArrayList<>();
    private RequestsListAdapter requestsListAdapter;
    private ProvidersListAdapter providersListAdapter;

    private int userId;
    private String viewMode;

    /**
     * Initializes the activity, checks login state, loads filters and sets up UI based on view mode.
     *
     * @param savedInstanceState Saved state, if available.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        // Retrieve user ID and view mode
        Intent intent = getIntent();
        userId = getIntent().getIntExtra("USER_ID", SharedPrefHelper.getUserId(this));
        viewMode = intent.getStringExtra(NavBarHandler.EXTRA_VIEW_MODE);
        if (viewMode == null)
            viewMode = NavBarHandler.SHOW_REQUESTS;

        if (userId == -1) {
            finish();
            return;
        }

        String token = ApiManager.getAuthToken();
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupMainActivity();
        NavBarHandler.setup(this, userId);
    }

    /**
     * Binds all view components from the layout to local fields.
     */
    private void initializeViews() {
        setContentView(R.layout.main_layout);
        searchInput = findViewById(R.id.searchInput);
        searchIcon = findViewById(R.id.searchIcon);
        resetFiltersBtn = findViewById(R.id.resetFiltersBtn);
        spinner = findViewById(R.id.firstSpinner);
        budgetInput = findViewById(R.id.budgetInput);
        distanceInput = findViewById(R.id.distanceInput);
        listView = findViewById(R.id.listView);
    }

    /**
     * Configures UI behavior for the current view mode (requests or providers),
     * sets listeners for filters and search, and loads initial data.
     */
    private void setupMainActivity() {
        if (viewMode.equals(NavBarHandler.SHOW_REQUESTS)) {
            NavBarHandler.highlightSelected(this, R.id.nav_home);
            requestsListAdapter = new RequestsListAdapter(this, requestsList);
            listView.setAdapter(requestsListAdapter);
            budgetInput.setHint("Min. budget (€)");
        } else {
            NavBarHandler.highlightSelected(this, R.id.nav_hire);
            providersListAdapter = new ProvidersListAdapter(this, serviceProvidersList);
            listView.setAdapter(providersListAdapter);
            budgetInput.setHint("Max budget (€)");
        }

        resetFiltersBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Filters reset", Toast.LENGTH_SHORT).show();
            searchInput.setText("");
            budgetInput.setText("");
            distanceInput.setText("");
            spinner.setSelection(0); // "Any"
            clearFiltersOnModeChange(); // clears shared prefs
            resetFiltersBtn.setVisibility(View.GONE);
            getData(); // reload list with no filters
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            saveFilters();
            getData();
            return true;
        });

        searchIcon.setOnClickListener(v -> {
            saveFilters();
            getData();
        });

        String[] serviceTypes = getResources().getStringArray(R.array.service_type);
        List<String> items = new ArrayList<>();
        items.add("Any");
        items.addAll(List.of(serviceTypes));

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(typeAdapter);

        restoreFilters();

        // Attach all listeners after restoring filter values
        spinner.setOnItemSelectedListener(new FilterListener());

        budgetInput.setOnEditorActionListener((v, actionId, event) -> {
            saveFilters();
            getData();
            return true;
        });

        distanceInput.setOnEditorActionListener((v, actionId, event) -> {
            saveFilters();
            getData();
            return true;
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent;
            if (viewMode.equals(NavBarHandler.SHOW_REQUESTS)) {
                ApiModels.RequestResponse request = requestsList.get(position);
                intent = new Intent(MainActivity.this, ServiceDetailsActivity.class);
                intent.putExtra("request_id", request.id);
                intent.putExtra("is_request", true);
            } else {
                ApiModels.ServiceProviderResponse provider = serviceProvidersList.get(position);
                intent = new Intent(MainActivity.this, ProviderDetailsActivity.class);
                intent.putExtra("SERVICE_RESPONSE_ID", provider.id);
                intent.putExtra("SERVICE_ROLE", provider.role);
            }
            startActivityForResult(intent, 2);
        });

        getData();
    }

    /**
     * Gets data from the API based on current filters and view mode,
     * and updates the list view with results.
     */
    private void getData() {
        String spinner = "";
        String searchQuery = searchInput.getText().toString().trim();
        int budget;

        String budgetText = budgetInput.getText().toString().trim();

        if (this.spinner.getSelectedItem() != null) {
            String selected = this.spinner.getSelectedItem().toString();
            if (!selected.equalsIgnoreCase("Any"))
                spinner = selected;
        }

        if (budgetText.isEmpty()) {
            if (viewMode.equals(NavBarHandler.SHOW_REQUESTS))
                budget = 0;
            else
                budget = 999999999;
        } else {
            try {
                budget = Integer.parseInt(budgetText);
            } catch (NumberFormatException e) {
                showToast("Invalid budget value");
                return;
            }
        }

        String distanceText = distanceInput.getText().toString().trim();
        int maxDistance;

        if (distanceText.isEmpty())
            maxDistance = 999999999;
        else {
            try {
                maxDistance = Integer.parseInt(distanceText);
            } catch (NumberFormatException e) {
                showToast("Invalid distance value");
                return;
            }
        }

        if (viewMode.equals(NavBarHandler.SHOW_REQUESTS)) {
            ApiManager.getRequests(spinner, budget, searchQuery, maxDistance, new ApiManager.RequestsListCallback() {
                @Override
                public void onSuccess(List<ApiModels.RequestResponse> requests) {
                    requestsList.clear();
                    requestsList.addAll(requests);
                    requestsListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFailure(String errorMessage) {
                    showToast(errorMessage);
                }
            });
        } else {
            ApiManager.getServiceProviders(spinner, budget, searchQuery,  maxDistance, new ApiManager.ServiceProvidersListCallback() {
                @Override
                public void onSuccess(List<ApiModels.ServiceProviderResponse> providers) {
                    serviceProvidersList.clear();
                    serviceProvidersList.addAll(providers);
                    providersListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFailure(String errorMessage) {
                    showToast(errorMessage);
                }
            });
        }
    }

    /**
     * Listener for the spinner that triggers when a new service type is selected.
     */
    private class FilterListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            saveFilters();
            getData();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

    /**
     * Saves the current filter values into SharedPreferences.
     */
    private void saveFilters() {
        String spinnerValue = spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "Any";
        String searchValue = searchInput.getText().toString();
        int budgetValue;
        double distanceValue;

        try {
            budgetValue = Integer.parseInt(budgetInput.getText().toString());
        } catch (NumberFormatException e) {
            budgetValue = viewMode.equals(NavBarHandler.SHOW_REQUESTS) ? 0 : 999999;
        }

        try {
            distanceValue = Double.parseDouble(distanceInput.getText().toString());
        } catch (NumberFormatException e) {
            distanceValue = 50.0;
        }

        resetFiltersBtn.setVisibility(View.VISIBLE);
        updateResetButtonVisibility();
        SharedPrefHelper.saveFilters(this, viewMode, spinnerValue, searchValue, budgetValue, distanceValue);
    }

    /**
     * Restores previously saved filter values from SharedPreferences and updates UI accordingly.
     */
    private void restoreFilters() {
        String savedSpinner = SharedPrefHelper.getFilterSpinner(this, viewMode);
        String savedQuery = SharedPrefHelper.getFilterQuery(this, viewMode);
        int savedBudget = SharedPrefHelper.getFilterBudget(this, viewMode);
        double savedDistance = SharedPrefHelper.getFilterDistance(this, viewMode);

        if (!savedSpinner.isEmpty())
            setSpinnerSelection(spinner, savedSpinner);

        if (!savedQuery.isEmpty())
            searchInput.setText(savedQuery);

        if (viewMode.equals(NavBarHandler.SHOW_REQUESTS)) {
            if (savedBudget != 0)
                budgetInput.setText(String.valueOf(savedBudget));
            else
                budgetInput.setText("");
        } else {
            if (savedBudget != 999999)
                budgetInput.setText(String.valueOf(savedBudget));
            else
                budgetInput.setText("");
        }

        if (savedDistance != 50.0)
            distanceInput.setText(String.valueOf((int) savedDistance));
        else
            distanceInput.setText("");

        resetFiltersBtn.setVisibility(View.VISIBLE);
        updateResetButtonVisibility();

    }

    /**
     * Sets the spinner to the given value if it exists in the adapter.
     *
     * @param spinner The spinner to update.
     * @param value   The string value to select.
     */
    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    /**
     * Clears all saved filter preferences when switching between view modes.
     */
    private void clearFiltersOnModeChange() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sharedPreferences.edit()
                .remove(KEY_REQUEST_BUDGET)
                .remove(KEY_PROVIDER_BUDGET)
                .remove(KEY_FILTER_TYPE)
                .remove(KEY_SEARCH_QUERY)
                .remove(KEY_FILTER_DISTANCE)
                .apply();
    }

    /**
     * Toggles the visibility of the reset button based on whether any filters are active.
     */
    private void updateResetButtonVisibility() {
        boolean hasFilter =
                !searchInput.getText().toString().trim().isEmpty() ||
                        (spinner.getSelectedItem() != null && !spinner.getSelectedItem().toString().equalsIgnoreCase("Any")) ||
                        !budgetInput.getText().toString().trim().isEmpty() ||
                        !distanceInput.getText().toString().trim().isEmpty();

        if (hasFilter)
            resetFiltersBtn.setVisibility(View.VISIBLE);
        else
            resetFiltersBtn.setVisibility(View.GONE);

    }

    /**
     * Reloads filters and data after returning from detail views.
     *
     * @param requestCode The integer request code originally supplied.
     * @param resultCode  The integer result code returned by the child activity.
     * @param data        An Intent that carries result data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2) {
            restoreFilters();
            getData();
        }
    }

    /**
     * Shows a short Toast message on the screen.
     *
     * @param message The text to display.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
