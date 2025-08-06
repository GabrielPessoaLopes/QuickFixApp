package gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

import gabrielLopes.project2425.DevPackage.QuickFixApp.Utils.SharedPrefHelper;


/**
 * Central helper for calling all QuickFix API endpoints using Retrofit.
 * Handles authentication, Retrofit setup, and API execution.
 */
public class ApiManager {

    private static final String BASE_URL = "https://quickfix-api.vercel.app/";
    private static ApiInterface apiInterface;
    private static Context appContext;

    /**
     * Initializes the API manager with application context.
     * Needed to access SharedPreferences.
     *
     * @param context Application context used to access SharedPreferences.
     */
    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * Retrieves the saved JWT token for authenticated requests.
     */
    public static String getAuthToken() {
        return SharedPrefHelper.getAuthToken(appContext);
    }

    /**
     * Returns a Retrofit instance of the API interface.
     * Initializes it if not already created.
     */
    public static ApiInterface getApiService() {
        if (apiInterface == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            apiInterface = retrofit.create(ApiInterface.class);
        }
        return apiInterface;
    }

    /**
     * Logs in the user with the given credentials.
     * On success, saves token and user ID, then triggers callback.
     *
     *  * @param username User's username.
     *  * @param password User's password.
     *  * @param callback Callback with success or failure.
     */
    public static void login(String username, String password, LoginCallback callback) {
        ApiModels.LoginRequest request = new ApiModels.LoginRequest(username, password);
        getApiService().login(request).enqueue(new Callback<ApiModels.LoginResponse>() {
            @Override
            public void onResponse(Call<ApiModels.LoginResponse> call, Response<ApiModels.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SharedPrefHelper.saveAuthToken(appContext, response.body().token);
                    SharedPrefHelper.saveUserId(appContext, response.body().userId);
                    callback.onSuccess(response.body().token, response.body().userId);

                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.LoginResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /** ---------------------------------------------------------------------------------------------------------------------
     *                                              USER
     * ----------------------------------------------------------------------------------------------------------------------
     * */

    /**
     * Retrieves the current user's profile info using their token.
     *
     * @param callback Callback with the profile data or error.
     */
    public static void getUser(ProfileCallback callback) {
        getApiService().getUser(getAuthToken())
                .enqueue(new Callback<ApiModels.UserProfileResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.UserProfileResponse> call, Response<ApiModels.UserProfileResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.UserProfileResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Gets another user's profile by their user ID.
     * Used in the context of a service provider knowing a request's creator
     *
     * @param userId ID of the user to retrieve.
     * @param callback Callback with the profile or error.
     */
    public static void getClientInfo(int userId, ProfileCallback callback) {
        getApiService().getUserById(getAuthToken(), userId).enqueue(new Callback<ApiModels.UserProfileResponse>() {
            @Override
            public void onResponse(Call<ApiModels.UserProfileResponse> call, Response<ApiModels.UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.UserProfileResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Sends a request to create a new user account.
     *
     * @param request User registration data.
     * @param callback Callback for success or failure.
     */
    public static void addUser(ApiModels.addUserRequest request, UserActionCallback callback) {
        getApiService().addUser(request).enqueue(new Callback<ApiModels.ApiResponse>() {
            @Override
            public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().message);
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Updates the profile of the current user.
     *
     * @param request user profile new data.
     * @param callback Callback with result.
     */
    public static void updateUser(ApiModels.UpdateUserRequest request, UserProfileUpdateCallback callback) {
        getApiService().updateUser(getAuthToken(), request)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().message);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Removes current's user account.
     *
     * @param callback Callback with result.
     */
    public static void removeUserAccount(UserActionCallback callback) {
        getApiService().removeUserAccount(getAuthToken()).enqueue(new Callback<ApiModels.ApiResponse>() {
            @Override
            public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().message);
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /** ---------------------------------------------------------------------------------------------------------------------
     *                                              PROFILE PICTURE
     * ----------------------------------------------------------------------------------------------------------------------
     * */
    /**
     * Retrieves the profile picture of the specified user.
     *
     * @param userId ID of the user.
     * @param callback Callback with image URL or error.
     */
    public static void getProfilePicture(int userId, ProfilePictureCallback callback) {
        getApiService().getProfilePicture(userId).enqueue(new Callback<ApiModels.ProfilePictureResponse>() {
            @Override
            public void onResponse(Call<ApiModels.ProfilePictureResponse> call, Response<ApiModels.ProfilePictureResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().profilePic);
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.ProfilePictureResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Uploads a profile picture for the current user.
     *
     * @param imageFile The image file to upload.
     * @param callback Callback with upload result.
     */
    public static void uploadProfilePicture(File imageFile, ProfilePictureCallback callback) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        getApiService().uploadProfilePicture(getAuthToken(), body)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().message);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /** ---------------------------------------------------------------------------------------------------------------------
     *                                              SERVICE PROVIDERS
     * ----------------------------------------------------------------------------------------------------------------------
     * */

    /**
     * Returns a list of providers filtered by role, budget, text query, and distance.
     *
     * @param serviceType Type of service.
     * @param maxBudget Maximum budget allowed.
     * @param query Text query to filter names or descriptions.
     * @param maxDistance Max distance in kilometers.
     * @param callback Callback with list or error.
     */
    public static void getServiceProviders(String serviceType, int maxBudget, String query,  int maxDistance, ServiceProvidersListCallback callback) {
        getApiService().getSPs(getAuthToken(), serviceType, maxBudget, query,  maxDistance)
                .enqueue(new Callback<List<ApiModels.ServiceProviderResponse>>() {
                    @Override
                    public void onResponse(Call<List<ApiModels.ServiceProviderResponse>> call, @NonNull Response<List<ApiModels.ServiceProviderResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiModels.ServiceProviderResponse>> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Gets full details for a specific provider's role
     *
     * @param providerId ID of the provider.
     * @param roleName Name of the role.
     * @param callback Callback with provider details or error.
     */
    public static void getServiceProviderById(int providerId, String roleName, ServiceProviderCallback callback) {
        getApiService().getServiceProviderById(getAuthToken(), providerId, roleName)
                .enqueue(new Callback<ApiModels.ServiceProviderResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ServiceProviderResponse> call, Response<ApiModels.ServiceProviderResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ServiceProviderResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Retrieves all current's user provider roles.
     *
     * @param callback Callback with list of roles.
     */
    public static void getProviderRoles(ServiceProviderRolesCallback callback) {
        Call<List<ApiModels.ServiceProviderResponse>> call = getApiService().getProviderRoles(getAuthToken());
        call.enqueue(new Callback<List<ApiModels.ServiceProviderResponse>>() {
            @Override
            public void onResponse(Call<List<ApiModels.ServiceProviderResponse>> call, Response<List<ApiModels.ServiceProviderResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<List<ApiModels.ServiceProviderResponse>> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Adds a role to the current service provider.
     *
     * @param request Role information.
     * @param callback Callback with result.
     */
    public static void addProviderRoles(ApiModels.ProviderRoleRequest request, ProviderRoleCallback callback) {
        getApiService().addProviderRole(getAuthToken(), request)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess("Role added.");
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Updates a specific role
     *
     * @param request Updated role info.
     * @param callback Callback with result.
     */
    public static void updateProviderInfo(ApiModels.UpdateProviderRoleRequest request, ProviderRoleCallback callback) {
        getApiService().updateProviderInfo(getAuthToken(), request)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
            @Override
            public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Role updated.");
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Removes a specific role from the current provider.
     *
     * @param role Name of the role to remove.
     * @param callback Callback with result.
     */
    public static void removeProviderRole(String role, ProviderRoleCallback callback) {
        getApiService().removeProviderRole(getAuthToken(), role).enqueue(new Callback<ApiModels.ApiResponse>() {
            @Override
            public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess("Role removed");
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /** ---------------------------------------------------------------------------------------------------------------------
     *                                                    REQUESTS
     * ----------------------------------------------------------------------------------------------------------------------
     * */

    /**
     * Retrieves all public service requests filtered by type, budget, query, and distance.
     *
     * @param spinner Service type filter.
     * @param budget Max price.
     * @param query Text query.
     * @param maxDistance Distance in km.
     * @param callback Callback with list or error.
     */
    public static void getRequests(String spinner, int budget, String query,  int maxDistance,RequestsListCallback callback) {
        String spinnerFilter = spinner.equalsIgnoreCase("Any") ? "" : spinner;
        getApiService().getRequests(getAuthToken(), spinnerFilter, budget, query,  maxDistance)
                .enqueue(new Callback<List<ApiModels.RequestResponse>>() {
                    @Override
                    public void onResponse(Call<List<ApiModels.RequestResponse>> call, Response<List<ApiModels.RequestResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }
                    @Override
                    public void onFailure(Call<List<ApiModels.RequestResponse>> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Gets requests created by the current user.
     *
     * @param status Status filter.
     * @param query Text query.
     * @param maxBudget Budget limit.
     * @param callback Callback with result list.
     */
    public static void getClientRequests(String status, String query, double maxBudget, RequestsListCallback callback) {
        getApiService().getClientRequests(getAuthToken(), status, query, maxBudget)
                .enqueue(new Callback<List<ApiModels.RequestResponse>>() {
                    @Override
                    public void onResponse(Call<List<ApiModels.RequestResponse>> call, Response<List<ApiModels.RequestResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiModels.RequestResponse>> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Gets the details of a specific request by ID.
     *
     * @param requestId ID of the request.
     * @param callback Callback with request details.
     */
    public static void getRequestById(int requestId, RequestCallback callback) {
        getApiService().getRequestById(getAuthToken(), requestId)
                .enqueue(new Callback<ApiModels.RequestResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.RequestResponse> call, Response<ApiModels.RequestResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.RequestResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Adds a new request.
     *
     * @param request The request data.
     * @param callback Callback with result of creation.
     */
    public static void addRequest(ApiModels.ServiceRequestRequest request, UserActionCallback callback) {
        getApiService().addRequest(getAuthToken(), request)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().message);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Updates an existing request by its ID.
     *
     * @param requestId ID of the request.
     * @param request Updated request fields.
     * @param callback Callback with update result.
     */
    public static void updateRequest(int requestId, ApiModels.ServiceRequestRequest request, UserActionCallback callback) {
        Map<String, Object> body = getStringObjectMap(request);

        getApiService().updateRequest(getAuthToken(), requestId, body)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().message);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Removes a request by ID.
     *
     * @param requestId ID of the request.
     * @param callback Callback with result.
     */
    public static void removeRequest(int requestId, UserActionCallback callback) {
        getApiService().removeRequest(getAuthToken(), requestId)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().message);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Accepts or rejects a pending request by the provider.
     *
     * @param requestId ID of the request.
     * @param accept Whether to accept (true) or reject (false).
     * @param callback Callback with decision result.
     */
    public static void handleRequestDecision(int requestId, boolean accept, UserActionCallback callback) {
        ApiModels.RequestDecision response = new ApiModels.RequestDecision(requestId, accept);
        getApiService().handleRequestDecision(getAuthToken(), response)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().message);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Checks if the current user is the creator of a given request.
     *
     * @param requestId ID of the request.
     * @param callback Callback returning true if owner, false otherwise.
     */
    public static void isRequestCreator(int requestId, RequestCreatorCallback callback) {
        getApiService().checkOwnership(getAuthToken(), requestId)
                .enqueue(new Callback<ApiModels.OwnershipResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.OwnershipResponse> call, Response<ApiModels.OwnershipResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().isOwner);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.OwnershipResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /** ---------------------------------------------------------------------------------------------------------------------
     *                                                    SERVICES
     * ----------------------------------------------------------------------------------------------------------------------
     * */

    /**
     * Updates the status of an active service.
     *
     * @param serviceId ID of the service.
     * @param newStatus New status to set.
     * @param callback Callback with result.
     */
    public static void updateServiceStatus(int serviceId, String newStatus, UserActionCallback callback) {
        ApiModels.ServiceStatusUpdateRequest body = new ApiModels.ServiceStatusUpdateRequest(serviceId, newStatus);
        getApiService().updateServiceStatus(getAuthToken(), body)
                .enqueue(new Callback<ApiModels.ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiModels.ApiResponse> call, Response<ApiModels.ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().message);
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiModels.ApiResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /**
     * Gets a service by its ID.
     *
     * @param serviceId ID of the service.
     * @param callback Callback with service data.
     */
    public static void getServiceById(int serviceId, ServiceCallback callback) {
        Call<ApiModels.ServiceResponse> call = getApiService().getServiceById(getAuthToken(), serviceId);
        call.enqueue(new Callback<ApiModels.ServiceResponse>() {
            @Override
            public void onResponse(Call<ApiModels.ServiceResponse> call, Response<ApiModels.ServiceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiModels.ServiceResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Retrieves all services handled by the specified provider.
     *
     * @param providerId ID of the provider.
     * @param status Status filter.
     * @param query Search query.
     * @param minBudget Minimum price.
     * @param callback Callback with list of services.
     */
    public static void getServicesByProvider(int providerId, String status, String query, double minBudget, ServiceListCallback callback) {
        getApiService().getServicesByProvider(getAuthToken(), providerId, status, query, minBudget)
                .enqueue(new Callback<List<ApiModels.ServiceResponse>>() {
                    @Override
                    public void onResponse(Call<List<ApiModels.ServiceResponse>> call, Response<List<ApiModels.ServiceResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure(getErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiModels.ServiceResponse>> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /** ---------------------------------------------------------------------------------------------------------------------
     *                                                    OTHER METHODS
     * ----------------------------------------------------------------------------------------------------------------------
     * */

    /**
     * Extracts a human-readable error message from a failed Retrofit response.
     * Used across all callbacks to get API error messages.
     *
     * @param response The failed Retrofit response.
     * @return The extracted error message, or a default one if parsing fails.
     */
    private static String getErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                org.json.JSONObject json = new org.json.JSONObject(errorBody);
                return json.optString("message", "Unexpected error.");
            }
        } catch (Exception e) {
            // fall through
        }
        return "Unexpected error.";
    }

    /**
     * Converts a ServiceRequestRequest object into a map of non-null fields for partial update.
     * Used in PATCH requests to update only changed fields.
     *
     * @param request The service request data to be transformed.
     * @return Map with field names and values to send in the update body.
     */
    private static @NonNull Map<String, Object> getStringObjectMap(ApiModels.ServiceRequestRequest request) {
        Map<String, Object> body = new HashMap<>();
        if (request.title != null)
            body.put("service_title", request.title);
        if (request.type != null)
            body.put("service_type", request.type);
        if (request.description != null)
            body.put("service_description", request.description);
        if (request.location != null)
            body.put("service_location", request.location);
        if (request.price > 0)
            body.put("service_price", request.price);
        if (request.deadline != null)
            body.put("service_deadline", request.deadline);
        return body;
    }
    // -------------------- CALLBACKS --------------------
    /**
     * Callback for login responses.
     * Used in login flow to handle token and user ID or error.
     */
    public interface LoginCallback {
        void onSuccess(String token, int userId);
        void onFailure(String errorMessage);
    }

    /**
     * Generic callback for user account actions like register or delete.
     */
    public interface UserActionCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    /**
     * Callback used when retrieving the current user's profile.
     */
    public interface ProfileCallback {
        void onSuccess(ApiModels.UserProfileResponse profile);
        void onFailure(String message);
    }

    /**
     * Callback for updating user profile information.
     */
    public interface UserProfileUpdateCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    /**
     * Callback used when retrieving or uploading a profile picture.
     */
    public interface ProfilePictureCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    // -------------------- SERVICE PROVIDERS --------------------
    /**
     * Callback for actions like adding or updating a provider role.
     */
    public interface ProviderRoleCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    /**
     * Callback for getting a filtered list of service providers.
     * Used in provider listings.
     */
    public interface ServiceProvidersListCallback {
        void onSuccess(List<ApiModels.ServiceProviderResponse> providers);
        void onFailure(String errorMessage);
    }

    /**
     * Callback for getting full details about a specific service provider role.
     */
    public interface ServiceProviderCallback {
        void onSuccess(ApiModels.ServiceProviderResponse provider);
        void onFailure(String errorMessage);
    }

    /**
     * Callback for retrieving all roles created by the current provider.
     */
    public interface ServiceProviderRolesCallback {
        void onSuccess(List<ApiModels.ServiceProviderResponse> roles);
        void onFailure(String errorMessage);
    }

    // -------------------- REQUESTS  --------------------
    /**
     * Callback for retrieving a filtered list of service requests.
     * Used in main feed and client profile.
     */
    public interface RequestsListCallback {
        void onSuccess(List<ApiModels.RequestResponse> requests);
        void onFailure(String errorMessage);
    }

    /**
     * Callback for fetching details about a single service request.
     */
    public interface RequestCallback {
        void onSuccess(ApiModels.RequestResponse request);
        void onFailure(String errorMessage);
    }

    /**
     * Callback for checking if the current user is the creator of a request.
     */
    public interface RequestCreatorCallback {
        void onSuccess(boolean creator);
        void onFailure(String errorMessage);
    }

    // -------------------- SERVICE --------------------
    /**
     * Callback for retrieving detailed data about a service.
     */
    public interface ServiceCallback {
        void onSuccess(ApiModels.ServiceResponse service);
        void onFailure(String errorMessage);
    }

    /**
     * Callback for retrieving a list of services owned by the provider or requested by the client.
     */
    public interface ServiceListCallback {
        void onSuccess(List<ApiModels.ServiceResponse> services);
        void onFailure(String errorMessage);
    }
}
