package gabrielLopes.project2425.DevPackage.QuickFixApp.navBar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;

import java.util.Calendar;

/**
 * Activity for creating or editing a service request.
 * <p>
 * Provides UI for inputting service title, type, description, price, location, and deadline.
 */
public class ModifyRequestActivity extends AppCompatActivity {

    private EditText titleText, locationText, priceText, deadlineText, deadlineTimeText, descriptionText;
    private Spinner spinner;
    private Button saveButton;
    private int userId;
    private boolean editMode;
    private int requestId;

    /**
     * Initializes the activity, determines if its in edit mode, and sets up the view.
     *
     * @param savedInstanceState Bundle containing saved state, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modify_service_requests_layout);
        userId = getIntent().getIntExtra("USER_ID", SharedPrefHelper.getUserId(this));

        requestId = getIntent().getIntExtra("REQUEST_ID", -1);
        editMode = requestId != -1;

        initializeViews();
        setupListeners();

        if (editMode)
            getAndPopulateRequest(requestId);

    }

    /**
     * Binds all UI components and sets up the navigation bar and spinner.
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

        NavBarHandler.setup(this, userId);
        NavBarHandler.highlightSelected(this, R.id.nav_add_request);
        setupTypeSpinner(null);
    }

    /**
     * Attaches listeners to date/time fields and the save button.
     */
    private void setupListeners() {
        deadlineText.setOnClickListener(v -> openDatePicker());
        deadlineTimeText.setOnClickListener(v -> openTimePicker());
        saveButton.setOnClickListener(v -> submitRequest());
    }

    /**
     * Retrieves an existing service request from the API and populates form fields.
     *
     * @param id The ID of the request to get and edit.
     */
    private void getAndPopulateRequest(int id) {
        ApiManager.getRequestById(id, new ApiManager.RequestCallback() {
            @Override
            public void onSuccess(ApiModels.RequestResponse request) {
                titleText.setText(request.title);
                locationText.setText(request.location);
                priceText.setText(String.valueOf(request.price));
                descriptionText.setText(request.description);

                String[] parts = request.deadline.split("T");
                if (parts.length >= 2) {
                    deadlineText.setText(parts[0]);
                    deadlineTimeText.setText(parts[1].substring(0, 5));
                } else {
                    deadlineText.setText(request.deadline);
                    deadlineTimeText.setText("00:00");
                }

                setupTypeSpinner(request.type);
            }

            @Override
            public void onFailure(String errorMessage) {
                showToast(errorMessage);
                finish();
            }
        });
    }

    /**
     * Validates user input and either submits a new request or updates an existing one.
     */
    private void submitRequest() {
        String title = titleText.getText().toString().trim();
        String type = spinner.getSelectedItem().toString().trim();
        String description = descriptionText.getText().toString().trim();
        String location = locationText.getText().toString().trim();
        String priceTextValue = priceText.getText().toString().trim();
        String datePart = deadlineText.getText().toString().trim();
        String timePart = deadlineTimeText.getText().toString().trim();
        String deadline = buildDeadline(datePart, timePart);

        if (!validateRequest(title, type, description, location, priceTextValue, datePart, timePart))
            return;

        double price = Double.parseDouble(priceTextValue);

        ApiModels.ServiceRequestRequest request = new ApiModels.ServiceRequestRequest(
                title, type, description, location, price, deadline
        );

        if (editMode) {
            ApiManager.updateRequest(requestId, request, new ApiManager.UserActionCallback() {
                @Override
                public void onSuccess(String message) {
                    showToast("Request updated.");
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    showToast(errorMessage);
                }
            });
        } else {
            ApiManager.addRequest(request, new ApiManager.UserActionCallback() {
                @Override
                public void onSuccess(String message) {
                    showToast("Request submitted.");
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    showToast(errorMessage);
                }
            });
        }
    }

    /**
     * Initializes the service type spinner and optionally pre-selects a value.
     *
     * @param selectedType The type to pre-select (only in edit mode).
     */
    private void setupTypeSpinner(String selectedType) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.service_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (selectedType != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(selectedType)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * Opens a DatePicker dialog to let the user select a deadline date for the request.
     */
    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = String.format("%04d-%02d-%02d", year, month + 1, day);
                    deadlineText.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    /**
     * Opens a TimePicker dialog to let the user select a deadline time for the request.
     */
    private void openTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            String time = String.format("%02d:%02d", hourOfDay, minute1);
            deadlineTimeText.setText(time);
        }, hour, minute, true);

        dialog.show();
    }

    /**
     * Validates all input fields before submitting a request.
     *
     * @return true if the request is valid, false otherwise.
     */

    private boolean validateRequest(String title, String type, String description, String location, String priceText, String datePart, String timePart) {
        if (title.isEmpty()) {
            showToast("Title is required.");
            return false;
        }
        if (type.isEmpty()) {
            showToast("Service type is required.");
            return false;
        }
        if (location.isEmpty()) {
            showToast("Location is required.");
            return false;
        }
        if (!isValidLocationFormat(location)) {
            showToast("Location must be in the format 'City, Country' or 'Street, No, City, Country'.");
            return false;
        }
        if (description.isEmpty()) {
            showToast("Description is required.");
            return false;
        }
        if (datePart.isEmpty()) {
            showToast("Deadline date is required.");
            return false;
        }
        if (!isValidFutureDateTime(datePart, timePart)) {
            showToast("Deadline must be a valid future date and time.");
            return false;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showToast("Invalid price value.");
            return false;
        }

        if (price <= 0) {
            showToast("Price must be a positive number.");
            return false;
        }

        return true;
    }

    /**
     * Checks whether a location string is in a valid format.
     *
     * @param location The location string to validate.
     * @return true if it has a valid format, false otherwise.
     */
    private boolean isValidLocationFormat(String location) {
        if (location == null)
            return false;

        String[] parts = location.split(",");
        if (parts.length < 2)
            return false;

        String city = parts[parts.length - 2].trim();
        String country = parts[parts.length - 1].trim();

        return !city.isEmpty() && !country.isEmpty();
    }

    /**
     * Checks if a given date and time string represents a valid moment in the future.
     *
     * @param date The date part of the deadline.
     * @param time The time part of the deadline.
     * @return true if the date and time are valid and in the future.
     */
    private boolean isValidFutureDateTime(String date, String time) {
        try {
            String full = buildDeadline(date, time);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setLenient(false);
            java.util.Date input = sdf.parse(full);
            return input.after(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Combines date and time input fields into a single deadline string for the backend.
     *
     * @param datePart The date in yyyy-MM-dd format.
     * @param timePart The time in HH:mm format or empty.
     * @return Combined deadline in yyyy-MM-ddTHH:mm:ss format.
     */
    private String buildDeadline(String datePart, String timePart) {
        return datePart + "T" + (timePart.isEmpty() ? "00:00:00" : timePart + ":00");
    }

    /**
     * Displays a Toast message with the given text.
     *
     * @param msg The message to show.
     */
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
