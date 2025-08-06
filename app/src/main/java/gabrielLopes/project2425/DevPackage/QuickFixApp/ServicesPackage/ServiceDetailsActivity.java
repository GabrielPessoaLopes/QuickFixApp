package gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.res.ColorStateList;
import android.util.TypedValue;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.widget.LinearLayout;
import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;
import gabrielLopes.project2425.DevPackage.QuickFixApp.navBar.NavBarHandler;
import gabrielLopes.project2425.DevPackage.QuickFixApp.navBar.ModifyRequestActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage.ProviderDetailsActivity;

/**
 * Displays full details of a service or request.
 * Allows the owner to update or remove requests,
 * lets providers accept or update services.
 */
public class ServiceDetailsActivity extends AppCompatActivity {
    private Button acceptButton;
    private boolean isAccepted = false;
    private int itemId;
    private boolean isRequest;
    private TextView titleTextView, locationTextView, serviceDateTextView, priceTextView,
            descriptionTextView, usernameTextView, ratingTextView, typeTextView, statusTextView,
            providerNameTextView, providerNoteTextView;

    private Button editButton, removeButton;

    /**
     * Initializes the layout and loads request or service details based on intent extras.
     *
     * @param savedInstanceState The saved activity state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        isRequest = getIntent().getBooleanExtra("is_request", true);

        if (!isRequest)
            itemId = getIntent().getIntExtra("service_id", -1);
        else
            itemId = getIntent().getIntExtra("request_id", -1);

        if (itemId == -1) {
            showToast("Invalid ID.");
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.service_details_layout);

        initializeViews();
        NavBarHandler.setup(this, SharedPrefHelper.getUserId(this));

        getDetails();
        if (isRequest)
            checkOwnership();
        else
            hideRequestControls();
    }

    /**
     * Binds view components and sets up listeners for edit, remove, and accept buttons.
     */
    private void initializeViews() {
        titleTextView = findViewById(R.id.titleTextView);
        locationTextView = findViewById(R.id.locationTextView);
        serviceDateTextView = findViewById(R.id.serviceDateTextView);
        priceTextView = findViewById(R.id.priceTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        usernameTextView = findViewById(R.id.usernameTextView);
        ratingTextView = findViewById(R.id.ratingTextView);
        typeTextView = findViewById(R.id.typeTextView);

        editButton = findViewById(R.id.editButton);
        editButton.setOnClickListener(v -> editButtonLogic());

        removeButton = findViewById(R.id.removeButton);
        removeButton.setOnClickListener(v -> removeButtonLogic());

        acceptButton = findViewById(R.id.acceptButton);
        acceptButton.setOnClickListener(v -> {
            if (!isRequest)
                showStatusUpdateDialog();
            else
                acceptButtonLogic();
        });

        statusTextView = findViewById(R.id.statusTextView);
        providerNameTextView = findViewById(R.id.providerNameTextView);
        providerNoteTextView = findViewById(R.id.providerNoteTextView);

        statusTextView.setVisibility(View.GONE);
        providerNameTextView.setVisibility(View.GONE);
        providerNameTextView.setOnClickListener(v -> openProviderDetails());
        providerNoteTextView.setVisibility(View.GONE);

    }

    /**
     * Loads either service or request data from the API based on mode.
     */
    private void getDetails() {
        if (!isRequest) {
            ApiManager.getServiceById(itemId, new ApiManager.ServiceCallback() {
                @Override
                public void onSuccess(ApiModels.ServiceResponse service) {
                    runOnUiThread(() -> getServiceData(service));
                }

                @Override
                public void onFailure(String errorMessage) {
                    showToast(errorMessage);
                }
            });
        } else {
            ApiManager.getRequestById(itemId, new ApiManager.RequestCallback() {
                @Override
                public void onSuccess(ApiModels.RequestResponse request) {
                    runOnUiThread(() -> getRequestData(request));
                }
                @Override
                public void onFailure(String errorMessage) {
                    showToast(errorMessage);
                }
            });
        }
    }

    /**
     * Displays the content of a request, including client info and request metadata.
     *
     * @param request The request response from the API.
     */
    private void getRequestData(ApiModels.RequestResponse request) {
        titleTextView.setText(request.title);

        locationTextView.setText(request.location + " ("+request.distanceKm + " km)");

        serviceDateTextView.setText(formatDate(request.deadline));
        priceTextView.setText(String.format("%.2f €", request.price));
        typeTextView.setText(request.type);
        descriptionTextView.setText(request.description);

        if (request.clientID != null&& request.clientID > 0) {
            ApiManager.getClientInfo(request.clientID, new ApiManager.ProfileCallback() {
                @Override
                public void onSuccess(ApiModels.UserProfileResponse user) {
                    runOnUiThread(() -> {
                        usernameTextView.setText(user.name != null ? user.name : "Unknown");
                        ratingTextView.setText(String.format("%.1f", user.rating));
                    });
                }

                @Override
                public void onFailure(String message) {
                    runOnUiThread(() -> {
                        usernameTextView.setText("Unknown");
                        ratingTextView.setText("-");
                    });
                }
            });
        } else {
            usernameTextView.setText("Unknown");
            ratingTextView.setText("-");
        }
        ConstraintLayout root = findViewById(R.id.rootContent);
        root.setVisibility(View.VISIBLE);
    }

    /**
     * Displays the content of a service, including client info and current status.
     *
     * @param service The service response from the API.
     */
    private void getServiceData(ApiModels.ServiceResponse service) {
        titleTextView.setText(service.title);
        acceptButton.setText("Update status");
        acceptButton.setBackgroundTintList(getColorFromAttr(androidx.appcompat.R.attr.colorPrimary));
        locationTextView.setText(service.location + " ("+service.distanceKm + " km)");
        serviceDateTextView.setText(formatDate(service.deadline));
        priceTextView.setText(String.format("%.2f €/h", service.price));
        typeTextView.setText(service.type);
        descriptionTextView.setText(service.description);
        statusTextView.setText(service.status.toUpperCase());
        statusTextView.setVisibility(View.VISIBLE);

        ApiManager.getClientInfo(service.client, new ApiManager.ProfileCallback() {
            @Override
            public void onSuccess(ApiModels.UserProfileResponse user) {
                runOnUiThread(() -> {
                    usernameTextView.setText(user.name != null ? user.name : "Unknown");
                    ratingTextView.setText(String.format("%.1f", user.rating));
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    usernameTextView.setText("Unknown");
                    ratingTextView.setText("-");
                });
            }
        });
        ConstraintLayout root = findViewById(R.id.rootContent);
        root.setVisibility(View.VISIBLE);
    }

    /**
     * Launches ModifyRequestActivity to edit the current request.
     */
    private void editButtonLogic() {
        Intent intent = new Intent(this, ModifyRequestActivity.class);
        intent.putExtra("REQUEST_ID", itemId);
        startActivityForResult(intent, 1);
    }

    /**
     * Shows confirmation dialog before removing the request.
     */
    private void removeButtonLogic() {
        new AlertDialog.Builder(this)
                .setTitle("Remove")
                .setMessage("Are you sure you want to remove this request?")
                .setPositiveButton("Yes", (dialog, which) -> removeRequest())
                .setNegativeButton("No", null)
                .show();

    }

    /**
     * Calls the API to remove the request and closes the activity on success.
     */
    private void removeRequest() {
        ApiManager.removeRequest(itemId, new ApiManager.UserActionCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showToast("Request removed.");
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    /**
     * Toggles acceptance state and sends accept/cancel decision to the API.
     */
    private void acceptButtonLogic() {
        boolean decision = !isAccepted;
        new AlertDialog.Builder(this)
                .setTitle(decision ? "Accept Request" : "Cancel response")
                .setMessage(decision
                        ? "Are you sure you want to accept this request?"
                        : "Cancel response to this request?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    ApiManager.handleRequestDecision(itemId, decision, new ApiManager.UserActionCallback() {
                        @Override
                        public void onSuccess(String message) {
                            runOnUiThread(() -> {
                                isAccepted = decision;
                                updateAcceptButtonUI();
                                showToast(message);
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> showToast(error));
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Updates the accept button text and color based on current acceptance state.
     */
    private void updateAcceptButtonUI() {
        if (isAccepted) {
            acceptButton.setText("Cancel");
            acceptButton.setBackgroundTintList(getColorStateList(R.color.red));
        } else {
            acceptButton.setText("Accept");
            acceptButton.setBackgroundTintList(getColorFromAttr(androidx.appcompat.R.attr.colorPrimary));
        }
    }

    /**
     * Checks if the current user is the request owner and updates the UI accordingly.
     */
    private void checkOwnership() {
        LinearLayout requesterInfo = findViewById(R.id.requester_info);
        ApiManager.isRequestCreator(itemId, new ApiManager.RequestCreatorCallback() {
            @Override
            public void onSuccess(boolean ownsIt) {
                runOnUiThread(() -> {
                    if (ownsIt) {
                        editButton.setVisibility(View.VISIBLE);
                        removeButton.setVisibility(View.VISIBLE);
                        acceptButton.setVisibility(View.GONE);
                        requesterInfo.setVisibility(View.GONE);
                        displayStatusAndProviderInfo();
                    } else
                        hideRequestControls();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                showToast("Ownership check failed.");
            }
        });
    }

    /**
     * Hides edit and remove buttons for users who don't own the request.
     */
    private void hideRequestControls() {
        editButton.setVisibility(View.GONE);
        removeButton.setVisibility(View.GONE);
    }

    /**
     * Opens a dialog to update the status of a service
     */
    private void showStatusUpdateDialog() {
        String[] serviceStatus = getResources().getStringArray(R.array.service_status);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Service Status");

        builder.setItems(serviceStatus, (dialog, which) -> {
            String selectedStatus = serviceStatus[which];

            ApiManager.updateServiceStatus(itemId, selectedStatus, new ApiManager.UserActionCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        showToast("Status updated to: " + selectedStatus);
                        titleTextView.setText(titleTextView.getText().toString().split(" \\(")[0] + " (" + selectedStatus + ")");
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> showToast(errorMessage));
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Shows request status and provider details if the request has been accepted.
     */
    private void displayStatusAndProviderInfo() {
        ApiManager.getRequestById(itemId, new ApiManager.RequestCallback() {
            @Override
            public void onSuccess(ApiModels.RequestResponse request) {
                runOnUiThread(() -> {
                    statusTextView.setText("Status: \n" + request.status.toUpperCase(Locale.ROOT));
                    statusTextView.setVisibility(View.VISIBLE);

                    if ("accepted".equalsIgnoreCase(request.status) && request.requestedProviderID != null) {
                        ApiManager.getClientInfo(request.requestedProviderID, new ApiManager.ProfileCallback() {
                            @Override
                            public void onSuccess(ApiModels.UserProfileResponse user) {
                                providerNameTextView.setText("By: \n" + (user.name != null ? user.name : "Unknown"));
                                providerNameTextView.setVisibility(View.VISIBLE);

                                // Search through user's roles to find the correct one (matching service_type)
                                String matchingNote = null;
                                if (user.roles != null) {
                                    for (ApiModels.ProviderRoleRequest role : user.roles) {
                                        if (role.role != null && role.role.equalsIgnoreCase(request.type)) {
                                            matchingNote = role.description;
                                            break;
                                        }
                                    }
                                }

                                if (matchingNote != null && !matchingNote.trim().isEmpty()) {
                                    providerNoteTextView.setText("Note: " + matchingNote);
                                    providerNoteTextView.setVisibility(View.VISIBLE);
                                } else {
                                    providerNoteTextView.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onFailure(String message) {
                                providerNameTextView.setText("Provider: Unknown");
                                providerNoteTextView.setVisibility(View.GONE);
                                providerNameTextView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                showToast("Could not load request details.");
            }
        });
    }

    /**
     * Opens ProviderDetailsActivity with the selected provider’s ID.
     */
    private void openProviderDetails() {
        ApiManager.getRequestById(itemId, new ApiManager.RequestCallback() {
            @Override
            public void onSuccess(ApiModels.RequestResponse request) {
                if (request.requestedProviderID != null) {
                    Intent intent = new Intent(ServiceDetailsActivity.this, ProviderDetailsActivity.class);
                    intent.putExtra("SERVICE_RESPONSE_ID", request.requestedProviderID);
                    startActivity(intent);
                } else {
                    showToast("Provider info not available.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                showToast("Failed to get provider.");
            }
        });
    }

    /**
     * Reloads the data when returning from the edit screen.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK)
            getDetails();

    }

    /**
     * Formats a raw ISO 8601 datetime into a human-readable string.
     *
     * @param rawDateTime The original deadline string.
     * @return Formatted date and optional time.
     */
    private String formatDate(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isEmpty())
            return "No deadline";
        try {
            String normalized = rawDateTime.replace("T", " ");
            if (!normalized.matches(".*\\d{2}:\\d{2}:\\d{2}$"))
                normalized += " 00:00:00";

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date parsed = inputFormat.parse(normalized);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

            String date = dateFormat.format(parsed);
            String time = timeFormat.format(parsed);

            // If time is exactly midnight, skip it
            if ("00:00".equals(time)) {
                return date;
            }

            return date + " " + time;
        } catch (Exception e) {
            return "Invalid date";
        }
    }

    /**
     /**
     * Returns a color from the current theme using its attribute ID.
     * <p>
     * This is used to apply theme-aware colors (like colorPrimary) to UI elements at runtime,
     * especially for buttons that need to reflect light/dark mode or custom themes dynamically.
     *
     * @param attr The attribute ID.
     * @return The corresponding color state list.
     */
    private ColorStateList getColorFromAttr(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return getResources().getColorStateList(typedValue.resourceId, getTheme());
    }

    /**
     * Displays a short Toast message.
     *
     * @param msg The message to show.
     */
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
