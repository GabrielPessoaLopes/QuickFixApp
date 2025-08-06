package gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;

/**
 * Activity for signing up new users or editing an existing user's profile.
 * <p>
 * Switches behavior based on the "editMode" flag.
 */
public class ModifyUserActivity extends AppCompatActivity {

    String emailCheck = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    String usernameCheck = "^[a-z0-9_]+$";
    private boolean editMode = false;
    private ApiModels.UserProfileResponse currentUser;


    /**
     * Initializes the activity, sets theme, checks if it's edit mode, and sets up the UI.
     *
     * @param savedInstanceState Saved instance state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        editMode = getIntent().getBooleanExtra("editMode", false);
        if (editMode)
            loadUserData();

        initializeViews();
    }

    /**
     * Binds all layout views and sets up button logic for creating or updating the user.
     */
    private void initializeViews() {
        setContentView(R.layout.modify_user_layout);
        EditText nameField = findViewById(R.id.nameInput);
        EditText emailField = findViewById(R.id.emailInput);
        EditText usernameField = findViewById(R.id.usernameInput);
        EditText locationField = findViewById(R.id.locationInput);
        EditText passwordField = findViewById(R.id.passwordInput);
        EditText confirmPasswordField = findViewById(R.id.confirmPasswordInput);
        Button saveButton = findViewById(R.id.saveButton);

        if (editMode && currentUser != null) {
            nameField.setText(currentUser.name);
            emailField.setText(currentUser.email);
            usernameField.setText(currentUser.username);
            locationField.setText(currentUser.location);
        }

        saveButton.setOnClickListener(v -> {
            String name = nameField.getText().toString().trim();
            String username = usernameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String location = locationField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();

            if (validateInputs(name, username, email, location, password, confirmPassword)) {
                if (editMode) {
                    updateUser(name, username, email, location, password);
                } else {
                    addUser(name, username, email, location, password);
                }
            }
        });
    }

    /**
     * Loads current user data from the API and fills the input fields for editing.
     */
    private void loadUserData() {
        ApiManager.getUser(new ApiManager.ProfileCallback() {
            @Override
            public void onSuccess(ApiModels.UserProfileResponse user) {
                runOnUiThread(() -> {
                    currentUser = user;

                    ((EditText) findViewById(R.id.nameInput)).setText(user.name);
                    ((EditText) findViewById(R.id.usernameInput)).setText(user.username);
                    ((EditText) findViewById(R.id.emailInput)).setText(user.email);
                    ((EditText) findViewById(R.id.locationInput)).setText(user.location);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> showToast("Failed to load user: " + message));
            }
        });
    }

    /**
     * Sends a request to add an user with the provided input.
     *
     * @param name      User's name
     * @param username  Chosen username
     * @param email     User's email
     * @param location  User's location
     * @param password  Chosen password
     */
    private void addUser(String name, String username, String email, String location, String password) {
        ApiModels.addUserRequest request = new ApiModels.addUserRequest(name, username, email, location, password);

        ApiManager.addUser(request, new ApiManager.UserActionCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showToast("User signed up!");
                    startActivity(new Intent(ModifyUserActivity.this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    finish();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> showToast(errorMessage));
            }
        });
    }

    /**
     * Sends an API request to update the user's profile.
     * Skips password if left blank.
     *
     * @param name      Updated name
     * @param username  Updated username
     * @param email     Updated email
     * @param location  Updated location
     * @param password  Optional new password
     */
    private void updateUser(String name, String username, String email, String location, String password) {
        ApiModels.UpdateUserRequest update;
        if (password.isEmpty()) {
            update = new ApiModels.UpdateUserRequest(name, username, email, location);
        } else {
            update = new ApiModels.UpdateUserRequest(name, username, email, location, password);
        }

        ApiManager.updateUser(update, new ApiManager.UserProfileUpdateCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showToast("User information updated successfully");
                    finish();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> showToast(errorMessage));
            }
        });
    }

    /**
     * Validates all input fields before creating or updating a user.
     *
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateInputs(String name, String username, String email, String location, String password, String confirmPassword) {
        if (name.isEmpty()){
            showToast("Name is required");
            return false;
        }
        if (username.isEmpty()){
            showToast("Username is required");
            return false;
        }
        if (email.isEmpty()){
            showToast("Email is required");
        return false;
        }
        if (location.isEmpty()) {
            showToast("Location is required");
            return false;
        }

        if (!editMode || (!password.isEmpty() || !confirmPassword.isEmpty())) {
            if (password.isEmpty() || confirmPassword.isEmpty()) {
                showToast("Both password fields are required");
                return false;
            }
            if (!password.equals(confirmPassword)) {
                showToast("Passwords must be the same");
                return false;
            }
        }

        if (!username.matches(usernameCheck)) {
            showToast("Username must contain only lowercase letters and digits (no spaces or special characters)");
            return false;
        }

        if (!email.matches(emailCheck)) {
            showToast("Invalid email format");
            return false;
        }

        if (!isValidLocationFormat(location)) {
            showToast("Location must be in the format 'City, Country' or 'Street, No, City, Country'");
            return false;
        }

        return true;
    }

    /**
     * Checks if the location string is in an accepted format.
     *
     * @param location The user-provided location string
     * @return true if location ends with "City, Country" format
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
     * Displays a short Toast with the given message.
     *
     * @param message Text to show
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
