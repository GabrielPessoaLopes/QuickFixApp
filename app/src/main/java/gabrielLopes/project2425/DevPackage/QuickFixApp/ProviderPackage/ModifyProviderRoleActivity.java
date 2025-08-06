package gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;

/**
 * Activity for adding or updating a service provider's role.
 * <p>
 * Allows creating a new role or updating/removing an existing one.
 */
public class ModifyProviderRoleActivity extends AppCompatActivity {
    private Spinner roleInput;
    private EditText locationInput, descriptionInput, priceInput;
    private Button saveButton, removeButton;

    private String mode;
    private String originalRole;

    /**
     * Initializes the activity, binds views, determines mode (add or edit), and sets up the spinner and listeners.
     *
     * @param savedInstanceState Bundle containing previous state, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modify_provider_role_layout);

        roleInput = findViewById(R.id.roleInput);
        locationInput = findViewById(R.id.locationInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        priceInput = findViewById(R.id.priceInput);
        saveButton = findViewById(R.id.saveButton);
        removeButton = findViewById(R.id.removeButton);

        Intent intent = getIntent();

        mode = intent.getStringExtra("mode");
        if (intent != null && "edit".equals(mode)) {
            mode = "edit";
            originalRole = intent.getStringExtra("role");
            locationInput.setText(intent.getStringExtra("location"));
            descriptionInput.setText(intent.getStringExtra("description"));
            priceInput.setText(String.valueOf(intent.getDoubleExtra("pricePerHour", 0)));
        } else
            removeButton.setVisibility(View.GONE);

        setupSpinner();
        saveButton.setOnClickListener(v -> saveRole());

        removeButton.setOnClickListener(v -> removeRole());

    }

    /**
     * Validates input and sends either an API request to add a new role or update an existing one.
     */
    private void saveRole() {
        String role = roleInput.getSelectedItem().toString();
        String location = locationInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String priceText = priceInput.getText().toString().trim();

        if (role.isEmpty() || location.isEmpty() || description.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price value.", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("edit".equals(mode)) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Update")
                    .setMessage("Are you sure you want to update this role?")
                    .setPositiveButton("Update", (dialog, which) -> {
                        ApiModels.UpdateProviderRoleRequest request = new ApiModels.UpdateProviderRoleRequest(
                                role, location, description, price
                        );

                        ApiManager.updateProviderInfo(request, new ApiManager.ProviderRoleCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(ModifyProviderRoleActivity.this, "Role updated successfully.", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(ModifyProviderRoleActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            ApiModels.ProviderRoleRequest request = new ApiModels.ProviderRoleRequest(
                    role,
                    location,
                    description,
                    price
            );

            ApiManager.addProviderRoles(request, new ApiManager.ProviderRoleCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(ModifyProviderRoleActivity.this, "Role added successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(ModifyProviderRoleActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Displays a confirmation dialog and removes the provider role if confirmed.
     */
    private void removeRole() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Role")
                .setMessage("Are you sure you want to delete this role?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    ApiManager.removeProviderRole(originalRole, new ApiManager.ProviderRoleCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(ModifyProviderRoleActivity.this, "Role removed.", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(ModifyProviderRoleActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Populates the spinner with available service types
     * and pre-selects the current role in edit mode.
     */
    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.service_type,
                android.R.layout.simple_spinner_dropdown_item
        );
        roleInput.setAdapter(adapter);

        if ("edit".equals(mode) && originalRole != null) {
            int index = adapter.getPosition(originalRole);
            if (index >= 0) {
                roleInput.setSelection(index);
            }
        }
    }


}
