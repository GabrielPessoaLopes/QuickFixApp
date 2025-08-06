package gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;
import okhttp3.MultipartBody;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;


public interface ApiInterface {

    // ---------------- User ----------------
    /**
     * Logs in an user.
     *
     * @param request Object containing login credentials.
     * @return Call returning a token and user ID.
     */
    @POST("login")
    Call<ApiModels.LoginResponse> login(
            @Body ApiModels.LoginRequest request);

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param authToken Bearer token.
     * @return Call with user profile data.
     */
    @GET("/user/me")
    Call<ApiModels.UserProfileResponse> getUser(
            @Header("Authorization") String authToken);

    /**
     * Retrieves the profile of any user by ID.
     *
     * @param token Bearer token.
     * @param userId Target user ID.
     * @return Call with user profile data.
     */
    @GET("/user/{userId}")
    Call<ApiModels.UserProfileResponse> getUserById(
            @Header("Authorization") String token,
            @Path("userId") int userId
    );

    /**
     * Creates a new user account.
     *
     * @param request User data.
     * @return Call with success or failure.
     */
    @POST("/user")
    Call<ApiModels.ApiResponse> addUser(
            @Body ApiModels.addUserRequest request);

    /**
     * Updates fields in the current user's profile.
     *
     * @param token Bearer token.
     * @param request Updated data.
     * @return Call with result.
     */
    @PATCH("/user")
    Call<ApiModels.ApiResponse> updateUser(
            @Header("Authorization") String token,
            @Body ApiModels.UpdateUserRequest request);

    /**
     * Removes the current user's account.
     *
     * @param token Bearer token.
     * @return Call with deletion result.
     */
    @DELETE("/user")
    Call<ApiModels.ApiResponse> removeUserAccount(
            @Header("Authorization") String token);

    /**
     * Gets the profile picture for a given user ID.
     *
     * @param userId Target user ID.
     * @return Call with image data (base64 string).
     */
    @GET("/profilePicture/{user_id}")
    Call<ApiModels.ProfilePictureResponse> getProfilePicture(
            @Path("user_id") int userId);

    /**
     * Uploads a new profile picture for the current user.
     * Uses @Multipart due to image files needing be sent as binary data, not JSON.
     *
     * @param token Bearer token.
     * @param file Multipart image file.
     * @return Call with result.
     */
    @Multipart
    @PUT("/profilePicture")
    Call<ApiModels.ApiResponse> uploadProfilePicture(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file
    );

    // ---------------- Service Providers ----------------
    /**
     * Retrieves a list of providers filtered by role, budget, search text, and distance.
     *
     * @param token Bearer token.
     * @param serviceType Filter by role.
     * @param maxBudget Filter by price.
     * @param query Free-text search.
     * @param maxDistance Distance limit in km.
     * @return Call with list of providers.
     */
    @GET("/providers")
    Call<List<ApiModels.ServiceProviderResponse>> getSPs(
            @Header("Authorization") String token,
            @Query("serviceType") String serviceType,
            @Query("maxBudget") int maxBudget,
            @Query("query") String query,
            @Query("maxDistance") int maxDistance
    );

    /**
     * Gets details for a provider’s specific role.
     *
     * @param token Bearer token.
     * @param providerId Provider's user ID.
     * @param role Role to view.
     * @return Call with provider role details.
     */
    @GET("/providers/details/{providerId}")
    Call<ApiModels.ServiceProviderResponse> getServiceProviderById(
            @Header("Authorization") String token,
            @Path("providerId") int providerId,
            @Query("role") String role
    );

    /**
     * Gets all roles created by the logged-in provider.
     *
     * @param token Bearer token.
     * @return Call with list of provider roles.
     */
    @GET("provider/roles")
    Call<List<ApiModels.ServiceProviderResponse>> getProviderRoles(
            @Header("Authorization") String token);

    /**
     * Adds a new provider role.
     *
     * @param token Bearer token.
     * @param request Role information.
     * @return Call with result.
     */
    @POST("/provider")
    Call<ApiModels.ApiResponse> addProviderRole(
            @Header("Authorization") String token,
            @Body ApiModels.ProviderRoleRequest request);

    /**
     * Updates an existing provider role.
     *
     * @param token Bearer token.
     * @param request Updated role info.
     * @return Call with result.
     */
    @PATCH("/provider")
    Call<ApiModels.ApiResponse> updateProviderInfo(
            @Header("Authorization") String token,
            @Body ApiModels.UpdateProviderRoleRequest request
    );

    /**
     * Removes a role from the current provider.
     *
     * @param token Bearer token.
     * @param role Role name to delete.
     * @return Call with result.
     */
    @DELETE("/provider")
    Call<ApiModels.ApiResponse> removeProviderRole(
            @Header("Authorization") String token,
            @Query("role") String role
    );

    // ---------------- Requests ----------------
    /**
     * Gets all public service requests filtered by type, budget, search text, and distance.
     *
     * @param token Bearer token.
     * @param spinner Request type filter.
     * @param budget Max budget.
     * @param query Free-text search.
     * @param maxDistance Distance in km.
     * @return Call with list of requests.
     */
    @GET("/requests")
    Call<List<ApiModels.RequestResponse>> getRequests(
            @Header("Authorization") String token,
            @Query("spinner") String spinner,
            @Query("budget") int budget,
            @Query("query") String query,
            @Query("maxDistance") int maxDistance
    );

    /**
     * Gets a specific service request by ID.
     *
     * @param token Bearer token.
     * @param requestId Target request ID.
     * @return Call with request data.
     */
    @GET("/request/{requestId}")
    Call<ApiModels.RequestResponse> getRequestById(
            @Header("Authorization") String token,
            @Path("requestId") int requestId
    );

    /**
     * Checks if the current user owns a specific request.
     *
     * @param token Bearer token.
     * @param requestId Target request ID.
     * @return Call with ownership boolean.
     */
    @GET("/request/check-ownership/{requestId}")
    Call<ApiModels.OwnershipResponse> checkOwnership(
            @Header("Authorization") String token,
            @Path("requestId") int requestId);

    /**
     * Gets all requests created by the current client.
     *
     * @param token Bearer token.
     * @param status Optional request status.
     * @param query Search text.
     * @param maxBudget Max price.
     * @return Call with list of client requests.
     */
    @GET("/requests/client")
    Call<List<ApiModels.RequestResponse>> getClientRequests(
            @Header("Authorization") String token,
            @Query("status") String status,
            @Query("query") String query,
            @Query("budget") double maxBudget
    );

    /**
     * Adds a service request.
     *
     * @param token Bearer token.
     * @param request Request data.
     * @return Call with result.
     */
    @POST("/request")
    Call<ApiModels.ApiResponse> addRequest(
            @Header("Authorization") String token,
            @Body ApiModels.ServiceRequestRequest request);

    /**
     * Updates an request's info (any field is optional)
     *
     * @param token Bearer token.
     * @param requestId ID to update.
     * @param body Fields to change.
     * @return Call with result.
     */
    @PATCH("/request/{requestId}")
    Call<ApiModels.ApiResponse> updateRequest(
            @Header("Authorization") String token,
            @Path("requestId") int requestId,
            @Body Map<String, Object> body
    );

    /**
     * Removes a request.
     *
     * @param token Bearer token.
     * @param requestId Request ID.
     * @return Call with result.
     */
    @DELETE("/request/{requestId}")
    Call<ApiModels.ApiResponse> removeRequest(
            @Header("Authorization") String token,
            @Path("requestId") int requestId);

    /**
     * Accepts or rejects a request.
     *
     * @param token Bearer token.
     * @param response Decision data.
     * @return Call with result.
     */
    @PATCH("/request/decision")
    Call<ApiModels.ApiResponse> handleRequestDecision(
            @Header("Authorization") String token,
            @Body ApiModels.RequestDecision response
    );

    // ---------------- Services ----------------
    /**
     * Gets full details of a service by ID.
     *
     * @param token Bearer token.
     * @param id Service ID.
     * @return Call with service info.
     */
    @GET("/service/{id}")
    Call<ApiModels.ServiceResponse> getServiceById(
            @Header("Authorization") String token,
            @Path("id") int id);

    /**
     * Gets services by a specific provider, with filters.
     *
     * @param token Bearer token.
     * @param providerId Provider user ID.
     * @param status Optional status filter.
     * @param query Optional search.
     * @param minBudget Optional budget filter.
     * @return Call with list of services.
     */
    @GET("/services/provider/{providerId}")
    Call<List<ApiModels.ServiceResponse>> getServicesByProvider(
            @Header("Authorization") String token,
            @Path("providerId") int providerId,
            @Query("status") String status,
            @Query("query") String query,
            @Query("budget") double minBudget
    );

    /**
     * Updates the status of a service
     *
     * @param token Bearer token.
     * @param request Status update data.
     * @return Call with result.
     */
    @PATCH("/service/status")
    Call<ApiModels.ApiResponse> updateServiceStatus(
            @Header("Authorization") String token,
            @Body ApiModels.ServiceStatusUpdateRequest request
    );
}
