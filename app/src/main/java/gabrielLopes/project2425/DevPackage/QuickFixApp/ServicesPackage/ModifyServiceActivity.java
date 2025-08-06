package gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;
import gabrielLopes.project2425.DevPackage.QuickFixApp.navBar.NavBarHandler;

/**
 * Activity used by providers to update the status of accepted services.
 */
public class ModifyServiceActivity extends AppCompatActivity {

    private EditText titleText, locationText, priceText, deadlineText, deadlineTimeText, descriptionText;
    private Spinner spinner, statusSpinner;
    private Button saveButton;

    private int requestId;
    private boolean statusOnly;
    private int userId;

    /**
     * Initializes the activity, loads mode flags from intent, and prepares the UI.
     *
     * @param savedInstanceState The saved instance state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPrefHelper.applySavedTheme(this);
        setContentView(R.layout.modify_service_requests_layout);

        requestId = getIntent().getIntExtra("REQUEST_ID", -1);
        statusOnly = getIntent().getBooleanExtra("status_mode", false);
        userId = SharedPrefHelper.getUserId(this);

        initializeViews();
        setupListeners();
        NavBarHandler.setup(this, userId);
    }

    /**
     * Binds all layout views and sets up the UI depending on whether only status editing is enabled.
     */
    private void initializeViews() {
        titleText = findViewById(R.id.titleText);
        locationText = findViewById(R.id.locationText);
        priceText = findViewById(R.id.priceText);
        deadlineText = findViewById(R.id.deadlineDateText);
        deadlineTimeText = findViewById(R.id.deadlineTimeText);
        descriptionText = findViewById(R.id.descriptionText);
        spinner = findViewById(R.id.spinner);
        saveButton = findViewById(R.id.saveButton);
        statusSpinner = new Spinner(this);

        if (statusOnly) {
            disableAllFields();
            setupStatusSpinner();
        } else {
            statusSpinner.setVisibility(View.GONE);
        }
    }

    /**
     * Attaches behavior to the save button for updating service status.
     */
    private void setupListeners() {
        saveButton.setOnClickListener(v -> {
            if (statusOnly)
                updateOnlyStatus();
        });
    }

    /**
     * Prepares the status dropdown with available service status options and adds it to the layout.
     */
    private void setupStatusSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.service_status,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        ((LinearLayout) saveButton.getParent()).addView(statusSpinner, 5);
    }

    /**
     * Disables all service detail fields to allow editing only the status.
     */
    private void disableAllFields() {
        titleText.setEnabled(false);
        locationText.setEnabled(false);
        priceText.setEnabled(false);
        deadlineText.setEnabled(false);
        descriptionText.setEnabled(false);
        spinner.setEnabled(false);
    }

    /**
     * Sends the selected status to the backend to update the service.
     */
    private void updateOnlyStatus() {
        String selectedStatus = statusSpinner.getSelectedItem().toString();
        ApiManager.updateServiceStatus(requestId, selectedStatus, new ApiManager.UserActionCallback() {
            @Override
            public void onSuccess(String message) {
                showToast("Status updated.");
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                showToast("Failed: " + errorMessage);
            }
        });
    }

    /**
     * Displays a Toast with the given message.
     *
     * @param msg The message to show.
     */
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
