package gabrielLopes.project2425.DevPackage.QuickFixApp.navBar;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
/**
 * Utility class for handling bottom navigation bar behavior across activities.
 * <p>
 * Manages navigation actions, selected tab highlighting, and sizing adjustments.
 */

public class NavBarHandler {

    /** Intent extra key used to indicate the view mode (requests or providers). */
    public static final String EXTRA_VIEW_MODE = "VIEW_MODE";

    /** Constants representing the providers and requests view modes. */
    public static final String SHOW_REQUESTS = "REQUESTS";
    public static final String SHOW_PROVIDERS = "PROVIDERS";

    /**
     * Initializes navigation bar item click listeners to switch between app sections.
     *
     * @param activity The current activity context used to bind and start activities.
     * @param userId   The ID of the logged-in user to pass with navigation intents.
     */
    public static void setup(Activity activity, int userId) {
        LinearLayout navHome = activity.findViewById(R.id.nav_home);
        LinearLayout navHire = activity.findViewById(R.id.nav_hire);
        LinearLayout navAddRequest = activity.findViewById(R.id.nav_add_request);
        LinearLayout navMyRequests = activity.findViewById(R.id.nav_my_services_requests);
        LinearLayout navSettings = activity.findViewById(R.id.nav_settings);

        if (navHome == null || navHire == null || navAddRequest == null || navMyRequests == null || navSettings == null) {
            return;
        }

        // Home option
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra(EXTRA_VIEW_MODE, SHOW_REQUESTS);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        });

        // Hire option
        navHire.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra(EXTRA_VIEW_MODE, SHOW_PROVIDERS);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        });

        // Add request option
        navAddRequest.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ModifyRequestActivity.class);
            intent.putExtra("USER_ID", userId);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        });

        // My requests option
        navMyRequests.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MyServicesAndRequestsActivity.class);
            intent.putExtra("USER_ID", userId);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        });

        // Settings option
        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            intent.putExtra("USER_ID", userId);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        });
    }

    /**
     * Highlights the selected navigation item by resizing its icon and toggling label visibility.
     *
     * @param activity       The current activity that hosts the navigation bar.
     * @param selectedNavId  The resource ID of the selected navigation item.
     */

    public static void highlightSelected(Activity activity, int selectedNavId) {
        int[] navIds = new int[]{
                R.id.nav_home, R.id.nav_hire, R.id.nav_add_request, R.id.nav_my_services_requests, R.id.nav_settings
        };

        int selectedSize = dpToPx(activity, 32);
        int defaultSize = dpToPx(activity, 24);

        LinearLayout navbar = activity.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.setPadding(
                    dpToPx(activity, 8),   // LEFT
                    dpToPx(activity, 4),  // TOP
                    dpToPx(activity, 8),   // RIGHT
                    dpToPx(activity, 4)   // BOTTOM
            );
            navbar.setMinimumHeight(dpToPx(activity, 56)); // optional
        }


        for (int navId : navIds) {
            LinearLayout navItem = activity.findViewById(navId);
            if (navItem == null)
                continue;
            ImageView icon = null;
            TextView label = null;

            for (int i = 0; i < navItem.getChildCount(); i++) {
                View child = navItem.getChildAt(i);
                if (child instanceof ImageView)
                    icon = (ImageView) child;
                else if (child instanceof TextView)
                    label = (TextView) child;
            }

            if (icon != null) {
                if (navId == selectedNavId) {
                    icon.getLayoutParams().width = selectedSize;
                    icon.getLayoutParams().height = selectedSize;
                } else {
                    icon.getLayoutParams().width = defaultSize;
                    icon.getLayoutParams().height = defaultSize;
                }
                icon.requestLayout();
            }

            if (label != null)
                label.setVisibility(navId == selectedNavId ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Converts a value in density-independent pixels (dp) to pixels (px).
     * for consistent sizing across different screen densities.
     * @param context The context used to access display metrics.
     * @param dp      The value in dp to convert.
     * @return The corresponding value in pixels.
     */
    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

}
