package gabrielLopes.project2425.DevPackage.QuickFixApp.ProviderPackage;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.ProfilePicHandler;

/**
 * Custom ArrayAdapter for displaying service providers in a list view.
 * <p>
 * Inflates each list item with the provider info.
 */
public class ProvidersListAdapter extends ArrayAdapter<ApiModels.ServiceProviderResponse> {

    private final Context context;
    private final List<ApiModels.ServiceProviderResponse> providerList;

    /**
     * Initializes a new ProvidersListAdapter.
     *
     * @param context Activity context used to load the layout for each list item.
     * @param providerList The list of service providers to display.
     */
    public ProvidersListAdapter(Context context, List<ApiModels.ServiceProviderResponse> providerList) {
        super(context, R.layout.provider_item_layout, providerList);
        this.context = context;
        this.providerList = providerList;
    }

    /**
     * Returns the view for a specific position in the list, populating it with provider data.
     *
     * @param position     The position of the item within the adapter's data set.
     * @param convertView  The old view to reuse, if possible.
     * @param parent       The parent that this view will eventually be attached to.
     * @return The populated view corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ApiModels.ServiceProviderResponse provider = providerList.get(position);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.provider_item_layout, parent, false);
        }

        // UI Components
        ImageView profileImage = convertView.findViewById(R.id.profilePic);
        ImageView priceIcon = convertView.findViewById(R.id.spPriceIcon);
        ImageView ratingIcon = convertView.findViewById(R.id.spRatingIcon);
        TextView providerName = convertView.findViewById(R.id.providerName);
        TextView providerRoles = convertView.findViewById(R.id.providerRole);
        ImageView locationIcon = convertView.findViewById(R.id.locationIcon);
        TextView providerLocation = convertView.findViewById(R.id.providerLocation);
        TextView providerDescription = convertView.findViewById(R.id.providerDescription);
        TextView providerRating = convertView.findViewById(R.id.spRatingText);
        TextView providerPrice = convertView.findViewById(R.id.spPriceText);

        // Load provider profile image or set default
        profileImage.setImageDrawable(null);
        ProfilePicHandler.getProfilePicture((Activity) context, provider.id, profileImage);

        // Set details
        providerName.setText(provider.name);
        providerRoles.setText(provider.role);

        providerLocation.setText(provider.distanceKm + " km");

        providerDescription.setText(provider.description);
        providerRating.setText(String.valueOf(provider.rating));
        providerPrice.setText(provider.pricePerHour+" €/h");

        // Icons
        locationIcon.setImageResource(R.drawable.ic_location);
        ratingIcon.setImageResource(R.drawable.ic_rating);
        priceIcon.setImageResource(R.drawable.ic_price);

        return convertView;
    }
}
