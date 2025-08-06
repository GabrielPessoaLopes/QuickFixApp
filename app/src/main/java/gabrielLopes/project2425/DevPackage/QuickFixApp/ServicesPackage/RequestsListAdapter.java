package gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;

/**
 * Adapter for displaying a list of client-created service requests.
 */
public class RequestsListAdapter extends ArrayAdapter<ApiModels.RequestResponse> {

    private final Context context;
    private final List<ApiModels.RequestResponse> serviceRequests;

    /**
     * Creates an adapter to display service requests.
     *
     * @param context         The context used to inflate views.
     * @param serviceRequests The list of requests to display.
     */
    public RequestsListAdapter(Context context, List<ApiModels.RequestResponse> serviceRequests) {
        super(context, R.layout.request_item_layout, serviceRequests);
        this.context = context;
        this.serviceRequests = serviceRequests;
    }


    /**
     * Builds and returns a list item view for a request.
     *
     * @param position     Index of the request in the list.
     * @param convertView  Recycled view or null.
     * @param parent       Parent view group.
     * @return A view with populated request data.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ApiModels.RequestResponse request = serviceRequests.get(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.request_item_layout, parent, false);
        }

        // UI references
        TextView serviceTitle = convertView.findViewById(R.id.serviceTitle);
        ImageView locationIcon = convertView.findViewById(R.id.locationIcon);
        TextView serviceLocation = convertView.findViewById(R.id.serviceLocation);
        ImageView dateIcon = convertView.findViewById(R.id.dateIcon);
        TextView serviceDate = convertView.findViewById(R.id.serviceDate);
        TextView serviceTime = convertView.findViewById(R.id.serviceTime);
        TextView requestType = convertView.findViewById(R.id.requestType);
        TextView requestBudget = convertView.findViewById(R.id.requestBudget);

        // Set content
        serviceTitle.setText(request.title);

        serviceLocation.setText(request.distanceKm + " km ");

        locationIcon.setImageResource(R.drawable.ic_location);
        dateIcon.setImageResource(R.drawable.ic_calendar);


        if (request.deadline != null && !request.deadline.isEmpty()) {
            String[] formatted = formatDeadline(request.deadline);
            serviceDate.setText(formatted[0]);
            serviceTime.setText(formatted[1]);
        } else {
            serviceDate.setText("N/A");
            serviceTime.setText("");
        }

        requestType.setText(request.type);
        requestBudget.setText(request.price == 0 ? "Free" : request.price + "€");

        return convertView;
    }

    /**
     * Converts a raw deadline string to readable date and time.
     *
     * @param rawDateTime The raw datetime string (ISO format).
     * @return Array with formatted [date, time].
     */
    private static String[] formatDeadline(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isEmpty()) {
            return new String[]{"N/A", ""};
        }

        try {
            // Normalize input and fallback to default time if missing
            String normalized = rawDateTime.replace("T", " ");
            if (!normalized.matches(".*\\d{2}:\\d{2}:\\d{2}$")) {
                normalized += " 00:00:00";
            }

            // Parse and format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date parsedDate = inputFormat.parse(normalized);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

            String dateStr = dateFormat.format(parsedDate);
            String timeStr = timeFormat.format(parsedDate);

            // Hide midnight time
            if ("00:00".equals(timeStr))
                timeStr = "";

            return new String[]{dateStr, timeStr};
        } catch (Exception e) {
            return new String[]{"Invalid date", ""};
        }
    }
}
