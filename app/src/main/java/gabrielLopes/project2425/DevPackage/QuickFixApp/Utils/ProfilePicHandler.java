package gabrielLopes.project2425.DevPackage.QuickFixApp.Utils;

import android.app.Activity;
import android.content.Context;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResult;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;
import java.io.OutputStream;

import gabrielLopes.project2425.DevPackage.QuickFixApp.R;
import gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage.ApiManager;

/**
 * Utility class for handling profile picture selection, upload, and loading.
 */
public class ProfilePicHandler {

    /**
     * Callback interface for profile picture upload result.
     */
    public interface UploadCallback {
        void onUploadSuccess();
        void onUploadFailure(String error);
    }

    /**
     * Downloads and loads a user's profile picture into an ImageView using Glide.
     * <p>
     * Glide prevents UI lag and avoids memory leaks by handling image decoding and recycling internally.
     * </p>
     * @param activity The current activity context.
     * @param userId ID of the user whose picture should be loaded.
     * @param imageView ImageView to update.
     */
    public static void getProfilePicture(Activity activity, int userId, ImageView imageView) {
        ApiManager.getProfilePicture(userId, new ApiManager.ProfilePictureCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                Context context = activity instanceof AppCompatActivity
                        ? (AppCompatActivity) activity
                        : activity.getApplicationContext();

                Glide.with(context).clear(imageView);
                imageView.setImageDrawable(null);

                Glide.with(context)
                        .load(imageUrl)
                        .signature(new ObjectKey(System.currentTimeMillis()))  // Forces cache refresh
                        .error(R.drawable.ic_user)
                        .into(imageView);
            }

            @Override
            public void onFailure(String errorMessage) {
                activity.runOnUiThread(() -> imageView.setImageResource(R.drawable.ic_user));
            }
        });
    }

    /**
     * Uploads a selected image URI to the server as the new profile picture.
     * <p>
     * Glide ensures smooth image transitions, handles disk and memory caching, and avoids unnecessary reloads.
     * </p>
     * @param activity The current activity.
     * @param imageUri The URI of the selected image.
     * @param profileImageView ImageView to update after successful upload.
     * @param callback Optional callback to notify success/failure.
     */
    public static void uploadProfilePicture(Activity activity, Uri imageUri, ImageView profileImageView, UploadCallback callback) {
        try {
            File imageFile = createTempFileFromUri(activity, imageUri);
            ApiManager.uploadProfilePicture(imageFile, new ApiManager.ProfilePictureCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    activity.runOnUiThread(() -> {
                        Glide.with(activity).clear(profileImageView);
                        profileImageView.setImageDrawable(null);

                        Glide.with(activity)
                                .load(imageUrl)
                                .signature(new ObjectKey(System.currentTimeMillis()))  // Forces cache refresh
                                .error(R.drawable.ic_user)
                                .into(profileImageView);


                        if (callback != null) callback.onUploadSuccess();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onUploadFailure(errorMessage);
                    });
                }
            });
        } catch (IOException e) {
            Toast.makeText(activity, "Failed to process image", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onUploadFailure("File conversion failed.");
        }
    }

    /**
     * Launches the image picker to select a profile picture.
     *
     * @param activity The current activity context.
     * @param launcher The result launcher to handle the result.
     */
    public static void startImagePicker(Activity activity, ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        launcher.launch(intent);
    }

    /**
     * Handles the result of the image picker and initiates upload if successful.
     *
     * @param activity The current activity.
     * @param result The result returned from the image picker.
     * @param profileImageView ImageView to update on success.
     * @param callback Callback to notify upload result.
     */
    public static void handleImagePickerResult(Activity activity, ActivityResult result, ImageView profileImageView, UploadCallback callback) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri imageUri = result.getData().getData();
            if (imageUri != null) {
                uploadProfilePicture(activity, imageUri, profileImageView, callback);
            } else {
                Toast.makeText(activity, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Converts a content URI to a temporary File in the cache directory.
     *
     * @param context The context for accessing content resolver and cache.
     * @param uri The URI of the image to convert.
     * @return A temporary File object created from the image URI.
     * @throws IOException If reading or writing the file fails.
     */
    public static File createTempFileFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        String fileName = "temp_profile_pic.jpg";
        File tempFile = new File(context.getCacheDir(), fileName);

        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        }
        return tempFile;
    }
}