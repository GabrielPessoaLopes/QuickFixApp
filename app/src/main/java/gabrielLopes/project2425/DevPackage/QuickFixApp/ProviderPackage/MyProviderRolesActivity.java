package gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;

/**
 * Activity that displays all service providing roles associated with the logged-in user.
 */
public class MyProviderRolesActivity extends AppCompatActivity {

    private ListView roleListView;
    private Button addRoleButton;
    private List<ApiModels.ServiceProviderResponse> roles;
    private ProvidersListAdapter adapter;

    /**
     * Initializes the layout, sets up view bindings, loads provider roles from API,
     * and sets listeners for item selection and the add role button.
     *
     * @param savedInstanceState Bundle containing previous state, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_provider_roles_layout);

        roleListView = findViewById(R.id.roleListView);
        addRoleButton = findViewById(R.id.addRoleButton);

        getRoles();

        roleListView.setOnItemClickListener((adapterView, view, position, id) -> {
            ApiModels.ServiceProviderResponse provider = roles.get(position);

            Intent intent = new Intent(MyProviderRolesActivity.this, ModifyProviderRoleActivity.class);
            intent.putExtra("mode", "edit");
            intent.putExtra("role", provider.role);
            intent.putExtra("location", provider.location);
            intent.putExtra("description", provider.description);
            intent.putExtra("pricePerHour", provider.pricePerHour);
            startActivity(intent);
        });

        addRoleButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyProviderRolesActivity.this, ModifyProviderRoleActivity.class);
            intent.putExtra("mode", "add");
            startActivity(intent);
        });
    }

    /**
     * Gets the list of provider roles from the API and updates the list view adapter.
     */
    private void getRoles() {
        ApiManager.getProviderRoles(new ApiManager.ServiceProviderRolesCallback() {
            @Override
            public void onSuccess(List<ApiModels.ServiceProviderResponse> rolesList) {
                roles = rolesList;
                adapter = new ProvidersListAdapter(MyProviderRolesActivity.this, roles);
                roleListView.setAdapter(adapter);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MyProviderRolesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Reloads the provider roles list when returning to this activity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        getRoles();
    }

}
