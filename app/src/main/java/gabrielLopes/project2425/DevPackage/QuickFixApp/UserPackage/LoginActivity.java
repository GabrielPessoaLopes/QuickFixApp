package gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;
import gabrielLopes.project2425.DevPackage.QuickFixApp.navBar.MainActivity;

/**
 * Handles the login screen where users enter credentials.
 * Redirects to MainActivity if already authenticated.
 */

public class LoginActivity extends AppCompatActivity {
    /**
     * Sets up the login screen, checks if user is already authenticated,
     * and wires up login and registration actions.
     *
     * @param savedInstanceState Saved state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);

        // Initialize ApiManager
        ApiManager.initialize(this);

        // Check if the user is already logged in
        String token = ApiManager.getAuthToken();
        int userId = SharedPrefHelper.getUserId(this);
        if (token != null && userId != -1) {
            openMainActivity(userId);  // Redirect to MainActivity if logged in
            return;
        }

        setContentView(R.layout.login_activity_layout);

        // Initialize UI components
        EditText usernameField = findViewById(R.id.usernameField);
        EditText passwordField = findViewById(R.id.passwordField);
        Button loginButton = findViewById(R.id.loginButton);
        TextView registerLink = findViewById(R.id.registerLink);

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String username = usernameField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (!username.isEmpty() && !password.isEmpty()) {
                login(username, password);
            } else {
                if (username.isEmpty())
                    usernameField.setError("Username required");
                if (password.isEmpty())
                    passwordField.setError("Password required");
            }
        });

        // Handle register link click
        registerLink.setOnClickListener(v -> openRegisterActivity());
    }

    /**
     * Sends login request and, if valid, stores the token and user ID.
     *
     * @param username Entered username
     * @param password Entered password
     */
    protected void login(String username, String password) {
        ApiManager.login(username, password, new ApiManager.LoginCallback() {
            @Override
            public void onSuccess(String token, int userId) {
                runOnUiThread(() -> {
                    // Save credentials only if login is successful
                    SharedPrefHelper.saveUserId(LoginActivity.this, userId);
                    SharedPrefHelper.saveAuthToken(LoginActivity.this, token);
                    openMainActivity(userId);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    // Show invalid credentials message
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Opens the main screen after successful login.
     *
     * @param userId Logged-in user ID
     */
    private void openMainActivity(int userId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }

    /**
     * Opens the registration screen (ModifyUserActivity).
     */
    private void openRegisterActivity() {
        Intent intent = new Intent(this, ModifyUserActivity.class);
        startActivityForResult(intent, 1);
    }

    /**
     * Handles return from registration screen and opens main screen if signup succeeded.
     *
     * @param requestCode Code originally supplied to identify source activity.
     * @param resultCode Code returned by the child activity.
     * @param data The intent returned by the child activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            int userId = SharedPrefHelper.getUserId(this);
            if (userId != -1)
                openMainActivity(userId);
            else
                Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
        }
    }
}
