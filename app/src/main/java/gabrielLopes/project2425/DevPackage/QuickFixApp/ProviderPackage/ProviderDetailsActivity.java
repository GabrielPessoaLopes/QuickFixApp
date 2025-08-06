package gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.ProfilePicHandler;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;
import gabrielLopes.project2425.DevPackage.QuickFixApp.navBar.NavBarHandler;

/**
 * Activity that displays detailed information about a service provider
 * and allows initiating a hiring action.
 */
public class ProviderDetailsActivity extends AppCompatActivity {
    private int providerId;
    String roleName;
    private ApiModels.ServiceProviderResponse provider;
    private ImageView profilePic;
    private TextView providerName, providerRole, providerLocation, providerRating, providerPricing, providerDescription;
    private Button hireButton;
    private boolean isHired = false;

    /**
     * Initializes the activity, loads provider ID and role from intent extras,
     * sets up UI components, and fetches provider details.
     *
     * @param savedInstanceState Bundle containing previous activity state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provider_details_layout);

        providerId = getIntent().getIntExtra("SERVICE_RESPONSE_ID", -1);
        roleName = getIntent().getStringExtra("SERVICE_ROLE");
        if (providerId == -1 || roleName == null || roleName.isEmpty()) {
            showToast("Invalid service provider.");
            finish();
            return;
        }

        initializeViews();
        getProviderDetails();
        NavBarHandler.setup(this, providerId);
    }

    /**
     * Binds all view components from the layout and sets the behavior of the hire button.
     */
    private void initializeViews() {
        profilePic = findViewById(R.id.profilePic);
        providerName = findViewById(R.id.providerName);
        providerRole = findViewById(R.id.providerRole);
        providerLocation = findViewById(R.id.providerLocation);
        providerRating = findViewById(R.id.providerRating);
        providerPricing = findViewById(R.id.providerPricing);
        providerDescription = findViewById(R.id.providerDescription);
        hireButton = findViewById(R.id.hireButton);

        hireButton.setOnClickListener(v -> {
            if (provider != null) {
                isHired = !isHired; // toggle the state
                if (isHired) {
                    hireButton.setText("Cancel");
                    hireButton.setBackgroundTintList(getColorStateList(R.color.red));
                    hireProvider(providerId);
                } else {
                    hireButton.setText("Hire");
                    hireButton.setBackgroundTintList(getColorFromAttr(androidx.appcompat.R.attr.colorPrimary));
                    showToast("Cancelled request for " + provider.name);
                }
            }
        });

    }

    /**
     * Placeholder method for initiating the hire flow for a specific provider.
     * To be implemented
     * @param providerId The ID of the provider to be hired.
     */
    private void hireProvider(int providerId) {
        /*
        Intent intent = new Intent(ServiceProvíderDetailsActivity.this, AddServiceRequestActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("DIRECT_HIRE", true);
        intent.putExtra("PROVIDER_ID", providerId);

        activity.startActivity(intent);
        */
        showToast("Requested to hire " + provider.name + "\nRedirecting to Add Request...");
    }

    /**
     * Calls the API to retrieve detailed information about the selected provider
     */
    private void getProviderDetails() {
        ApiManager.getServiceProviderById(providerId, roleName, new ApiManager.ServiceProviderCallback() {
            @Override
            public void onSuccess(ApiModels.ServiceProviderResponse providerResponse) {
                runOnUiThread(() -> {
                    provider = providerResponse;
                    populateDetails();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> showToast("Failed to load details: " + errorMessage));
            }
        });
    }

    /**
     * Populates the UI with provider information after a successful API response.
     */
    private void populateDetails() {
        providerName.setText(provider.name);
        providerRole.setText(provider.role);
        providerLocation.setText(provider.location + " ("+provider.distanceKm + " km)");
        providerRating.setText(String.valueOf(provider.rating));
        providerPricing.setText(provider.pricePerHour + "€/h");
        providerDescription.setText(provider.description);

        ProfilePicHandler.getProfilePicture(this, provider.id, profilePic);
        LinearLayout root = findViewById(R.id.rootContent);
        root.setVisibility(View.VISIBLE);
    }

    /**
     * Returns a theme-based color.
     * <p>
     * Used when setting theme colors like colorPrimary in code.
     * Useful due to the dynamic nature of the hire button.
     *
     * @param attr The theme attribute to resolve (e.g. R.attr.colorPrimary).
     * @return The resolved ColorStateList.
     */

    private ColorStateList getColorFromAttr(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return getResources().getColorStateList(typedValue.resourceId, getTheme());
    }

    /**
     * Displays a short Toast message with the given text.
     *
     * @param msg The message to be shown.
     */
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
