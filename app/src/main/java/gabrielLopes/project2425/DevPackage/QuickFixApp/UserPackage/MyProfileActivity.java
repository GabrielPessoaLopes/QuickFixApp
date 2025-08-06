package gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.ProfilePicHandler;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;
import gabrielLopes.project2425.DevPackage.QuickFixApp.navBar.NavBarHandler;

/**
 * Displays the current user's profile with personal info and profile picture.
 * <p>
 * Allows editing user data, changing profile picture, and removing the account.
 */
public class MyProfileActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView profileImageView;
    private Button editButton, removeButton;
    private TextView nameTextView, usernameTextView, emailTextView, locationTextView, ratingTextView;
    private ApiModels.UserProfileResponse currentUser;

    /**
     * Sets up the profile screen, initializes views, loads user data,
     * and configures profile image, edit, and remove actions.
     *
     * @param savedInstanceState The saved activity state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.my_profile_activity_layout);
        profileImageView = findViewById(R.id.profileImageView);
        nameTextView = findViewById(R.id.nameTextView);
        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        locationTextView = findViewById(R.id.locationTextView);
        ratingTextView = findViewById(R.id.ratingTextView);
        editButton = findViewById(R.id.editButton);
        removeButton = findViewById(R.id.removeButton);

        NavBarHandler.setup(this, SharedPrefHelper.getUserId(this));
        loadUserProfile();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> ProfilePicHandler.handleImagePickerResult(this, result, profileImageView, new ProfilePicHandler.UploadCallback() {
                    @Override
                    public void onUploadSuccess() {
                        // Reload profile picture from backend
                        if (currentUser != null) {
                            ProfilePicHandler.getProfilePicture(MyProfileActivity.this, currentUser.userId, profileImageView);
                        }
                    }

                    @Override
                    public void onUploadFailure(String error) {
                        showToast("Error loading profile picture. ");
                    }
                })
        );

        profileImageView.setOnClickListener(v -> promptEditProfilePicture());

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyProfileActivity.this, ModifyUserActivity.class);
            intent.putExtra("editMode", true);
            startActivity(intent);
        });

        removeButton.setOnClickListener(v -> promptRemoveAccount());
    }

    /**
     * Calls the API to get the current user's profile and updates the UI.
     */
    private void loadUserProfile() {
        ApiManager.getUser(new ApiManager.ProfileCallback() {
            @Override
            public void onSuccess(ApiModels.UserProfileResponse user) {
                currentUser = user;
                runOnUiThread(() -> {
                    nameTextView.setText(user.name);
                    usernameTextView.setText(user.username);
                    emailTextView.setText(user.email);
                    locationTextView.setText(user.location);
                    ratingTextView.setText(String.valueOf(user.rating));
                    ProfilePicHandler.getProfilePicture(MyProfileActivity.this, user.userId, profileImageView);

                    ConstraintLayout rootContent = findViewById(R.id.rootContent);
                    rootContent.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> Toast.makeText(MyProfileActivity.this, "Failed to load profile: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Shows a confirmation dialog before launching the image picker for profile picture update.
     */
    private void promptEditProfilePicture() {
        new AlertDialog.Builder(this)
                .setTitle("Add profile picture")
                .setMessage("Add new profile picture?")
                .setPositiveButton("Yes", (dialog, which) -> ProfilePicHandler.startImagePicker(this, imagePickerLauncher))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a confirmation dialog before permanently removing the user account.
     */
    private void promptRemoveAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Account")
                .setMessage("Remove account? This action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeAccount();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Sends a request to remove the user account and redirects to the login screen.
     */
    private void removeAccount() {
        ApiManager.removeUserAccount(new ApiManager.UserActionCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(MyProfileActivity.this, "Account will be removed.", Toast.LENGTH_SHORT).show();
                SharedPrefHelper.clearUserData(MyProfileActivity.this);
                Intent intent = new Intent(MyProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MyProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * Reloads the user profile each time the activity becomes active.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    /**
     * Displays a Toast with the given message.
     *
     * @param message The message to show.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
