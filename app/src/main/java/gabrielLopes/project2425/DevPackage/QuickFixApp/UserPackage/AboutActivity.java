package gabrielLopes.project2425.DevPackage.QuickFixApp.UserPackage;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;

/**
 * Shows app-related information on the "About" screen.
 */
public class AboutActivity extends AppCompatActivity {
    /**
     * Loads the "About" layout and applies saved theme.
     *
     * @param savedInstanceState Saved state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);
    }
}
