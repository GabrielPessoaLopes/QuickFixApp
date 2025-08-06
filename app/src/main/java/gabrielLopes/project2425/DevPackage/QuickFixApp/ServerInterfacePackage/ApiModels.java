package gabrielLopes.project2425.DevPackage.QuickFixApp.ServerInterfacePackage;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Contains all data models used for communicating with the backend.
 * Each class represents a request or response body exchanged through the API.
 */
public class ApiModels {
    public static class ApiResponse {
        public String message;
    }

    /**
     * Sent to the API for user login.
     */
    public static class LoginRequest {
        public String username;
        public String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Received after successful login.
     * Contains the JWT token and user ID.
     */
    public static class LoginResponse {
        public String token;
        public int userId;
    }

    /**
     * Sent to create a new user account.
     */
    public static class addUserRequest {
        @SerializedName("user_name")
        public String name;

        @SerializedName("user_username")
        public String username;

        @SerializedName("user_mail")
        public String email;

        @SerializedName("user_location")
        public String location;

        @SerializedName("user_password")
        public String password;

        public addUserRequest(String name, String username, String email, String location, String password) {
            this.name = name;
            this.username = username;
            this.email = email;
            this.location = location;
            this.password = password;
        }
    }

    /**
     * Received when loading a user's profile info.
     */
    public static class UserProfileResponse {
        @SerializedName("userId")
        public int userId;

        @SerializedName("name")
        public String name;

        @SerializedName("username")
        public String username;

        @SerializedName("email")
        public String email;

        @SerializedName("personal_location")
        public String location;

        @SerializedName("rating")
        public double rating;

        @SerializedName("roles")
        public List<ProviderRoleRequest> roles;
    }

    /**
     * Sent when updating user data.
     * Any field is optional.
     */
    public static class UpdateUserRequest {
        @SerializedName("user_name")
        public String name;

        @SerializedName("user_username")
        public String username;

        @SerializedName("user_mail")
        public String email;

        @SerializedName("user_location")
        public String location;

        @SerializedName("user_password")
        public String password;

        public UpdateUserRequest(String name, String username, String email, String location) {
            this.name = name;
            this.username = username;
            this.email = email;
            this.location = location;
        }

        public UpdateUserRequest(String name, String username, String email, String location, String password) {
            this(name, username, email, location);
            this.password = password;
        }
    }

    /**
     * Contains the URL key for the user's profile picture.
     */
    public static class ProfilePictureResponse {
        public String profilePic;
    }

    /**
     * Represents a single role that a provider offers.
     */
    public static class ServiceProviderResponse {
        public int id;
        public String name;
        public String role;
        public String location;
        public String description;
        public double rating;
        public double pricePerHour;
        public int distanceKm;
    }

    /**
     * Sent when adding a new provider role.
     */
    public static class ProviderRoleRequest {
        public String role;
        public String location;
        public String description;
        public Double pricePerHour;

        public ProviderRoleRequest(String role, String location, String description, Double pricePerHour) {
            this.role = role;
            this.location = location;
            this.description = description;
            this.pricePerHour = pricePerHour;
        }
    }

    /**
     * Sent when updating a provider role.
     */
    public static class UpdateProviderRoleRequest {
        @SerializedName("role")
        public String role;

        @SerializedName("pro_location")
        public String location;

        @SerializedName("pro_description")
        public String description;

        @SerializedName("pro_price_per_hour")
        public double pricePerHour;

        public UpdateProviderRoleRequest(String role, String location, String description, double pricePerHour) {
            this.role = role;
            this.location = location;
            this.description = description;
            this.pricePerHour = pricePerHour;
        }
    }

    /**
     * Sent when adding a new request.
     */
    public static class ServiceRequestRequest {
        @SerializedName("service_title")
        public String title;

        @SerializedName("service_type")
        public String type;

        @SerializedName("service_description")
        public String description;

        @SerializedName("service_location")
        public String location;

        @SerializedName("service_price")
        public Double price;

        @SerializedName("service_deadline")
        public String deadline;

        public ServiceRequestRequest(String title, String type, String description, String location, Double price, String deadline) {
            this.title = title;
            this.type = type;
            this.description = description;
            this.location = location;
            this.price = price;
            this.deadline = deadline;
        }
    }

    /**
     * Full response for a client-created request.
     */
    public static class RequestResponse {
        @SerializedName("request_id")
        public int id;

        @SerializedName("service_title")
        public String title;

        @SerializedName("service_type")
        public String type;

        @SerializedName("service_description")
        public String description;

        @SerializedName("service_location")
        public String location;

        @SerializedName("service_deadline")
        public String deadline;

        @SerializedName("service_price")
        public double price;

        @SerializedName("request_status")
        public String status;

        @SerializedName("service_isAccepted")
        public boolean isAccepted;

        @SerializedName("requester")
        public Integer clientID;

        @SerializedName("requested_provider")
        public Integer requestedProviderID;

        @SerializedName("distanceKm")
        public int distanceKm;
    }

    /**
     * Used to check if the current user owns a specific request/service.
     */
    public static class OwnershipResponse {
        public boolean isOwner;
    }

    /**
     * Sent by a provider to accept or reject a request.
     */
    public static class RequestDecision {
        public int requestId;
        public boolean accept;

        public RequestDecision(int requestId, boolean accept) {
            this.requestId = requestId;
            this.accept = accept;
        }
    }

    /**
     * Response for an accepted service (from request).
     */
    public static class ServiceResponse {
        @SerializedName("service_id")
        public int id;

        @SerializedName("service_title")
        public String title;

        @SerializedName("service_type")
        public String type;

        @SerializedName("service_description")
        public String description;

        @SerializedName("service_location")
        public String location;

        @SerializedName("service_deadline")
        public String deadline;

        @SerializedName("service_price")
        public double price;

        @SerializedName("service_status")
        public String status;

        @SerializedName("service_provider")
        public int provider;

        @SerializedName("service_client")
        public int client;

        @SerializedName("distanceKm")
        public int distanceKm;
    }

    public static class ServiceStatusUpdateRequest {
        public int serviceId;
        public String status;

        public ServiceStatusUpdateRequest(int serviceId, String status) {
            this.serviceId = serviceId;
            this.status = status;
        }
    }
}