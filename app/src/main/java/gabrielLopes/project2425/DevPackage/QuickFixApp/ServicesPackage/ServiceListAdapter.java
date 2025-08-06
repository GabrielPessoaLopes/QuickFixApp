package gabrielLopes.project2425.DevPackage.QuickFixApp.ServicesPackage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Locale;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiModels;

/**
 * Adapter that displays a list of accepted services.
 */

public class ServiceListAdapter extends ArrayAdapter<ApiModels.ServiceResponse> {

    private final Context context;
    private final List<ApiModels.ServiceResponse> services;

    /**
     * Creates a new adapter to display a list of services.
     *
     * @param context  The context used to inflate the layout.
     * @param services The list of confirmed services to display.
     */
    public ServiceListAdapter(Context context, List<ApiModels.ServiceResponse> services) {
        super(context, R.layout.service_item_layout, services);
        this.context = context;
        this.services = services;
    }

    /**
     * Returns the list item view for a service at the given position.
     * Populates all fields such as title, location, type, price, and deadline.
     *
     * @param position     The position of the item in the list.
     * @param convertView  The recycled view to reuse, or null to inflate a new one.
     * @param parent       The parent view group.
     * @return The populated view for the current service.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ApiModels.ServiceResponse service = services.get(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.service_item_layout, parent, false);
        }

        TextView serviceTitle = convertView.findViewById(R.id.serviceTitle);
        ImageView serviceStatusIcon = convertView.findViewById(R.id.serviceStatusIcon);
        ImageView locationIcon = convertView.findViewById(R.id.locationIcon);
        TextView serviceLocation = convertView.findViewById(R.id.serviceLocation);
        ImageView dateIcon = convertView.findViewById(R.id.dateIcon);
        TextView serviceDate = convertView.findViewById(R.id.serviceDate);
        TextView serviceTime = convertView.findViewById(R.id.serviceTime);
        TextView serviceType = convertView.findViewById(R.id.requestType);
        TextView serviceBudget = convertView.findViewById(R.id.requestBudget);

        serviceTitle.setText(service.title);
        setStatusIcon(serviceStatusIcon, service.status);

        serviceLocation.setText(service.distanceKm + " km ");

        locationIcon.setImageResource(R.drawable.ic_location);
        dateIcon.setImageResource(R.drawable.ic_calendar);

        if (service.deadline != null && !service.deadline.isEmpty()) {
            String[] formatted = formatDeadline(service.deadline);
            serviceDate.setText(formatted[0]);
            serviceTime.setText(formatted[1]);
        } else {
            serviceDate.setText("N/A");
            serviceTime.setText("");
        }

        serviceType.setText(service.type);
        serviceBudget.setText(service.price == 0 ? "Free" : service.price + "€");

        return convertView;
    }

    /**
     * Converts a raw ISO 8601 datetime string into a date and time string.
     *
     * @param rawDateTime The raw deadline string from the backend.
     * @return A string array with [date, time].
     */
    private static String[] formatDeadline(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isEmpty()) {
            return new String[]{"N/A", ""};
        }

        try {
            String normalized = rawDateTime.replace("T", " ");
            if (!normalized.matches(".*\\d{2}:\\d{2}:\\d{2}$")) {
                normalized += " 00:00:00";
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date parsedDate = inputFormat.parse(normalized);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

            String dateStr = dateFormat.format(parsedDate);
            String timeStr = timeFormat.format(parsedDate);

            if ("00:00".equals(timeStr)) timeStr = "";

            return new String[]{dateStr, timeStr};
        } catch (Exception e) {
            return new String[]{"Invalid date", ""};
        }
    }

    /**
     * Sets the correct icon based on the service status.
     *
     * @param iconView The ImageView to update.
     * @param status   The current service status.
     */
    private void setStatusIcon(ImageView iconView, String status) {
        if (status == null) {
            iconView.setImageResource(R.drawable.ic_status_pending);
            return;
        }
        switch (status.toLowerCase(Locale.ROOT)) {
            case "accepted":
                iconView.setImageResource(R.drawable.ic_status_accepted);
                break;
            case "started":
                iconView.setImageResource(R.drawable.ic_status_started);
                break;
            case "finished":
                iconView.setImageResource(R.drawable.ic_status_finished);
                break;
            case "closed":
                iconView.setImageResource(R.drawable.ic_status_closed);
                break;
            case "cancelled":
                iconView.setImageResource(R.drawable.ic_status_cancelled);
                break;
            default:
                iconView.setImageResource(R.drawable.ic_status_pending);
                break;
        }
    }
}
