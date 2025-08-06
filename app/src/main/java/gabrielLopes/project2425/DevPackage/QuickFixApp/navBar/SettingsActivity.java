package gabrielLopes.project2425.DevPackage.QuickFixApp.navBar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Arrays;
import java.util.List;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage.MyProviderRolesActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage.LoginActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage.MyProfileActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage.ModifyUserActivity;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;
import gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage.AboutActivity;

/**
 * Activity for displaying and handling user settings, including theme changes,
 * profile actions, and logout.
 */

public class SettingsActivity extends AppCompatActivity {
    private ListView settingsListView;
    private int userId;
    private TextView usernameTextView;

    /**
     * Called when the activity is first created. Initializes the layout, theme, and settings list.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity_layout);
        userId = SharedPrefHelper.getUserId(this);

        getUserName();

        NavBarHandler.highlightSelected(this, R.id.nav_settings);
        settingsListView = findViewById(R.id.settingsListView);
        NavBarHandler.setup(this, userId);
        setupSettingsList();
    }

    /**
     * Prepares the settings list with various configurable user options
     * and sets up the click listeners for each item.
     */

    private void setupSettingsList() {
        List<String> items = Arrays.asList(
                "My Profile",
                "Edit user information",
                "My roles",
                "Change Theme",
                "Notification Preferences",
                "Privacy Settings",
                "Terms & Conditions",
                "About",
                "Logout"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                items
        );

        settingsListView.setAdapter(adapter);
        settingsListView.setOnItemClickListener((parent, view, position, id) -> {
            String item = items.get(position);

            switch (item) {
                case "My Profile":
                    Intent profileIntent = new Intent(SettingsActivity.this, MyProfileActivity.class);
                    startActivity(profileIntent);
                    break;
                case "Edit user information":
                    Intent editUserIntent = new Intent(SettingsActivity.this, ModifyUserActivity.class);
                    editUserIntent.putExtra("editMode", true);
                    startActivity(editUserIntent);
                    break;
                case "My roles":
                    Intent MyProviderRolesIntent = new Intent(this, MyProviderRolesActivity.class);
                    startActivity(MyProviderRolesIntent);
                    break;
                case "Change Theme":
                    toggleTheme();
                    break;
                case "About":
                    Intent aboutIntent = new Intent(SettingsActivity.this, AboutActivity.class);
                    startActivity(aboutIntent);
                    break;
                case "Logout":
                    SharedPrefHelper.clearUserData(this);
                    Intent logoutIntent = new Intent(SettingsActivity.this, LoginActivity.class);
                    logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logoutIntent);
                    finish();
                    break;
                default:
                    showToast(item + " clicked (not implemented)");
                    break;
            }
        });
    }

    /**
     * Toggles between dark and light mode, updates user preferences,
     * and recreates the activity to apply the theme.
     */

    private void toggleTheme() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode = AppCompatDelegate.MODE_NIGHT_YES;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            newMode = AppCompatDelegate.MODE_NIGHT_NO;
            showToast("Switched to Light Mode");
        } else
            showToast("Switched to Dark Mode");

        SharedPrefHelper.saveThemeMode(this, newMode);
        AppCompatDelegate.setDefaultNightMode(newMode);
        recreate();
    }

    /**
     * Gets the current user's profile from the server and updates the
     * greeting TextView with the user's name or username.
     */

    private void getUserName() {
        usernameTextView = findViewById(R.id.usernameTextView);
        ApiManager.getUser(new ApiManager.ProfileCallback() {
            @Override
            public void onSuccess(ApiModels.UserProfileResponse profile) {
                if (profile.name != null)
                    usernameTextView.setText("Hello " + profile.name + "!");
                else
                    usernameTextView.setText("Hello " + profile.username + "!");
            }

            @Override
            public void onFailure(String message) {
                showToast("Failed to load profile: " + message);
            }
        });
    }

    /**
     * Displays a short Toast message.
     *
     * @param message The message to show.
     */

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
