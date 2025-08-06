package gabrielLopes.project2425.DevPackage.QuickFixApp.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Handles saving and retrieving local preferences like user info, filters, and theme mode.
 */
public class SharedPrefHelper {

    private static final String PREF_NAME = "QuickFixPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_THEME_MODE = "themeMode";

    // Request filters
    private static final String FILTER_SPINNER_REQUEST = "FILTER_SPINNER_REQUEST";
    private static final String FILTER_QUERY_REQUEST = "FILTER_QUERY_REQUEST";
    private static final String FILTER_BUDGET_REQUEST = "FILTER_BUDGET_REQUEST";
    private static final String FILTER_DISTANCE_REQUEST = "FILTER_DISTANCE_REQUEST";

    // Provider filters
    private static final String FILTER_SPINNER_PROVIDER = "FILTER_SPINNER_PROVIDER";
    private static final String FILTER_QUERY_PROVIDER = "FILTER_QUERY_PROVIDER";
    private static final String FILTER_BUDGET_PROVIDER = "FILTER_BUDGET_PROVIDER";
    private static final String FILTER_DISTANCE_PROVIDER = "FILTER_DISTANCE_PROVIDER";

    /**
     * Stores the authentication token in shared preferences.
     *
     * @param context Application context.
     * @param token Auth token to save.
     */
    public static void saveAuthToken(Context context, String token) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    /**
     * Retrieves the saved auth token.
     *
     * @param context Application context.
     * @return Saved auth token or null.
     */
    public static String getAuthToken(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    /**
     * Stores the logged-in user ID.
     *
     * @param context Application context.
     * @param userId ID to save.
     */
    public static void saveUserId(Context context, int userId) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    /**
     * Gets the saved user ID.
     *
     * @param context Application context.
     * @return User ID or -1 if not found.
     */
    public static int getUserId(Context context) {
        if (context == null) return -1;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /**
     * Clears all saved user data and preferences.
     *
     * @param context Application context.
     */
    public static void clearUserData(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Saves filter settings based on mode (REQUESTS or PROVIDERS).
     *
     * @param context Application context.
     * @param mode "REQUESTS" or "PROVIDERS"
     * @param spinner Selected service type.
     * @param query Text query.
     * @param budget Max budget.
     * @param distance Max distance.
     */
    public static void saveFilters(Context context, String mode, String spinner, String query, int budget, double distance) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (mode.equals("REQUESTS")) {
            editor.putString(FILTER_SPINNER_REQUEST, spinner);
            editor.putString(FILTER_QUERY_REQUEST, query);
            editor.putInt(FILTER_BUDGET_REQUEST, budget);
            editor.putFloat(FILTER_DISTANCE_REQUEST, (float) distance);
        } else {
            editor.putString(FILTER_SPINNER_PROVIDER, spinner);
            editor.putString(FILTER_QUERY_PROVIDER, query);
            editor.putInt(FILTER_BUDGET_PROVIDER, budget);
            editor.putFloat(FILTER_DISTANCE_PROVIDER, (float) distance);
        }

        editor.apply();
    }

    /**
     * Gets the saved spinner filter based on mode.
     */
    public static String getFilterSpinner(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return mode.equals("REQUESTS")
                ? prefs.getString(FILTER_SPINNER_REQUEST, "")
                : prefs.getString(FILTER_SPINNER_PROVIDER, "");
    }

    /**
     * Gets the saved query filter based on mode.
     */
    public static String getFilterQuery(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return mode.equals("REQUESTS")
                ? prefs.getString(FILTER_QUERY_REQUEST, "")
                : prefs.getString(FILTER_QUERY_PROVIDER, "");
    }

    /**
     * Gets the saved budget filter based on mode.
     */
    public static int getFilterBudget(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return mode.equals("REQUESTS")
                ? prefs.getInt(FILTER_BUDGET_REQUEST, 0)
                : prefs.getInt(FILTER_BUDGET_PROVIDER, 999999);
    }

    /**
     * Gets the saved distance filter based on mode.
     */
    public static double getFilterDistance(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return mode.equals("REQUESTS")
                ? prefs.getFloat(FILTER_DISTANCE_REQUEST, 50f)
                : prefs.getFloat(FILTER_DISTANCE_PROVIDER, 50f);
    }

    /**
     * Saves the selected theme mode.
     *
     * @param context Application context.
     * @param mode Theme mode constant.
     */
    public static void saveThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    /**
     * Gets the saved theme mode.
     *
     * @param context Application context.
     * @return Saved mode or default mode.
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Applies the saved theme mode to the current session.
     *
     * @param context Application context.
     */
    public static void applySavedTheme(Context context) {
        int savedMode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }
}
