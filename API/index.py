from flask import Flask, jsonify, request
from functools import wraps
from datetime import datetime, timedelta
import os, jwt, requests
import openrouteservice
from math import radians, cos, sin, asin, sqrt
import uuid
import requests

#The SQL query is automatically built and executed by Supabase based on:
#The GET request made to the Supabase REST endpoint
#The query parameters (params) sent
#The headers (Prefer) like sorting
#Supabase handles authentication, builds the SQL query behind the scenes, 
# executes it on its PostgreSQL database, 
# and returns the filtered and sorted result as JSON.

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get("SECRET_KEY", "default_secret_key")

# Keys
SUPABASE_KEY = os.environ.get("SUPABASE_KEY")

SUPABASE_SERVICE_ROLE_KEY = os.environ.get("SUPABASE_SERVICE_ROLE_KEY")
DISTANCE_API_KEY = os.environ.get("DISTANCE_API_KEY")

# Wrong approach to saving keys, it should be secret, however it was not working properly when used as an environment variabl
SUPABASE_SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRybGpob2Zuam9mc3JjaHR0dXZqIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0Mjk5NDc5MiwiZXhwIjoyMDU4NTcwNzkyfQ.ZRn0T84wGXWD-d3keLSXQMii0ilqL2jYs-GDTaqr8vw"
DISTANCE_API_KEY = "5b3ce3597851110001cf62482c6792ba82624c3cb4c07b632dac13c2"


distance_client = openrouteservice.Client(key=DISTANCE_API_KEY)
coord_cache = {}
distance_cache = {} 

# Common Supabase table URLs
SUPABASE_URL = os.environ.get("SUPABASE_URL")
SUPABASE_REST_URL = f"{SUPABASE_URL}/rest/v1"
USER_URL = f"{SUPABASE_REST_URL}/user"
PRO_INFO_URL = f"{SUPABASE_REST_URL}/pro_info"
SERVICE_URL = f"{SUPABASE_REST_URL}/service"

# HTTP Status Codes
OK_CODE = 200
CREATED_CODE = 201
BAD_REQUEST_CODE = 400
UNAUTHORIZED_CODE = 401
FORBIDDEN_CODE = 403
NOT_FOUND_CODE = 404
CONFLICT_CODE = 409
SERVER_ERROR = 500

##################################### Supabase Request Headers ########################################
def supabase_headers():
    return {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json"
    }

@app.route('/', methods=['GET'])
def home():
    return jsonify({"message": "Welcome to the QuickFix API!"})

#|---------------------------------------------------------------------------------------------------|
#|                                      USER ENDPOINTS                                               |
#| Handle user registration, login, profile retrieval and updates.                                   |
#| All users are clients by default. They become service providers by adding a role.                 |
#| Enforces unique email and username during creation and update.                                    |
#|---------------------------------------------------------------------------------------------------|

###################################### AUTH DECORATOR #############################################
def auth_user(f):
    """ Decorator to authenticate requests using JWT. 
        Adds request.user_id if valid."""
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get("Authorization")
        if not token:
            return jsonify({"message": "Token is missing"}), UNAUTHORIZED_CODE
        try:
            data = jwt.decode(token, app.config["SECRET_KEY"], algorithms=["HS256"])
            request.user_id = data["id"]
        except:
            return jsonify({"message": "Invalid token"}), UNAUTHORIZED_CODE
        return f(*args, **kwargs)
    return decorated

######################################## GET ALL USERS   
@app.route("/users", methods=["GET"])
def get_all_users():
    """Returns all users in the database. Test-only endpoint."""
    try:
        res = requests.get(USER_URL, headers=supabase_headers())
        return jsonify(res.json()), OK_CODE
    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR
    
####################################### GET CURRENT USER PROFILE INFORMATION
@app.route("/user/me", methods=["GET"])
@auth_user
def get_current_user_profile():
    """Returns the profile data of the currently logged-in user, including professionalroles"""
    try:
        user_id = request.user_id

        # Get user base info
        user_res = requests.get(f"{USER_URL}?user_id=eq.{user_id}", headers=supabase_headers())
        user_data = user_res.json()
        if not user_data:
            return jsonify({"message": "User not found"}), NOT_FOUND_CODE
        user = user_data[0]

        # Get provider roles
        pro_res = requests.get(f"{PRO_INFO_URL}?pro_id=eq.{user_id}", headers=supabase_headers())
        pro_roles = pro_res.json()

        result = {
            "userId": user["user_id"],
            "name": user["user_name"],
            "username": user["user_username"],
            "email": user["user_mail"],
            "rating": user.get("user_rating"),
            "personal_location":user.get("user_location"),
            "roles": [],
        }

        if not pro_roles:
            result["accountType"] = "client account"
        else:
            complete_roles = []
            incomplete_roles = []
            for role in pro_roles:
                role_info = {
                    "role": role.get("pro_role"),
                    "location": role.get("pro_location"),
                    "description": role.get("pro_description"),
                    "pricePerHour": role.get("pro_price_per_hour")
                }
                result["roles"].append(role_info)

                # Check completeness
                if all([role_info["location"], role_info["description"], role_info["pricePerHour"]]):
                    complete_roles.append(role_info)
                else:
                    missing_fields = []
                    if not role_info["location"]: missing_fields.append("location")
                    if not role_info["description"]: missing_fields.append("description")
                    if not role_info["pricePerHour"]: missing_fields.append("pricePerHour")
                    role_info["missingFields"] = missing_fields
                    incomplete_roles.append(role_info)

            result["accountType"] = (
                "Service Provider" if not incomplete_roles
                else "Service Provider with missing information"
            )

        return jsonify(result), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

######################################### LOGIN 
@app.route("/login", methods=["POST"])
def login():
    """Authenticates an user and returns a JWT token if credentials are valid."""

    content = request.get_json()
    if "username" not in content or "password" not in content:
        return jsonify({"message": "Missing credentials"}), BAD_REQUEST_CODE

    params = {
        "user_username": f"eq.{content['username']}"
    }

    res = requests.get(USER_URL, headers=supabase_headers(), params=params)
    users = res.json()

    if not users:
        return jsonify({"message": "Invalid credentials"}), UNAUTHORIZED_CODE

    user = users[0]
    if user["user_password"] != content["password"]:
        return jsonify({"message": "Invalid credentials"}), UNAUTHORIZED_CODE

    token = jwt.encode({
        "id": user["user_id"],
    }, app.config["SECRET_KEY"], algorithm="HS256")

    return jsonify({"token": token, "userId": user["user_id"]}), OK_CODE

####################################### GET USER BY ID        
@app.route("/user/<int:user_id>", methods=["GET"])
@auth_user
def get_user_by_id(user_id):
    """Returns a user by their ID."""
    try:
        res = requests.get(f"{USER_URL}?user_id=eq.{user_id}", headers=supabase_headers())
        users = res.json()

        if not users:
            return jsonify({"message": "User not found"}), NOT_FOUND_CODE

        user = users[0]
        return jsonify({
            "userId": user["user_id"],
            "name": user["user_name"],
            "rating": user.get("user_rating")
        }), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error geting user: {str(e)}"}), SERVER_ERROR

####################################### ADD USER
@app.route("/user", methods=["POST"])
def add_user():
    """Adds a new user to the database."""

    content = request.get_json()
    if not all(k in content for k in ["user_username", "user_password", "user_name", "user_mail", "user_location"]):
        return jsonify({"message": "Missing fields"}), UNAUTHORIZED_CODE

    username = content["user_username"].lower()
    email = content["user_mail"].lower()

    # Check for duplicate username or email
    try:
        # Make a GET request to Supabase filtering users with the same username
        check_username_req = requests.get(USER_URL, headers=supabase_headers(), params={"user_username": f"eq.{username}"})

        # Parse the JSON response 
        if check_username_req.json():
            return jsonify({"message": "Username already exists"}), CONFLICT_CODE

        # Make a GET request to Supabase filtering users with the same username
        check_email_req = requests.get(USER_URL, headers=supabase_headers(), params={"user_mail": f"eq.{email}"})
        if check_email_req.json():
            return jsonify({"message": "Email already exists"}), CONFLICT_CODE

        data = {
            "user_username": username,
            "user_password": content["user_password"],
            "user_mail": email,
            "user_name": content["user_name"],
            "user_location": content["user_location"]
        }

        res = requests.post(USER_URL, headers=supabase_headers(), json=data)
        if res.status_code in [OK_CODE, CREATED_CODE]:
            return jsonify({"message": "User registered"}), CREATED_CODE
        return jsonify({"message": res.text}), res.status_code

    except Exception as e:
        return jsonify({"message": f"Registration failed: {str(e)}"}), SERVER_ERROR


####################################### UPDATE USER    
@app.route("/user", methods=["PATCH"])
@auth_user
def update_user():
    """Updates a user's information in the database."""

    content = request.get_json()
    allowed_fields = ["user_name", "user_username", "user_password", "user_mail", "user_location"]
    data = {k: content[k] for k in allowed_fields if k in content}

    if not data:
        return jsonify({"message": "No valid fields to update"}), BAD_REQUEST_CODE

    try:
        # # Check uniqueness only if the user is changing username or email
        if "user_username" in data:
            # Make a GET request to Supabase filtering users with the same username
            check_username_req = requests.get(USER_URL, headers=supabase_headers(), params={
                "user_username": f"eq.{data['user_username']}"
            })
            # Parse the JSON response
            existing = check_username_req.json()
            if any(u["user_id"] != request.user_id for u in existing):
                return jsonify({"message": "Username already exists"}), CONFLICT_CODE

        if "user_mail" in data:
            # Make a GET request to Supabase filtering users with the same email
            check_email_req = requests.get(USER_URL, headers=supabase_headers(), params={
                "user_mail": f"eq.{data['user_mail']}"
            })
            # Parse the JSON response
            existing = check_email_req.json()
            if any(u["user_id"] != request.user_id for u in existing):
                return jsonify({"message": "Email already exists"}), CONFLICT_CODE

        url = f"{SUPABASE_REST_URL}/user?user_id=eq.{request.user_id}"
        req = requests.patch(url, headers=supabase_headers(), json=data)
        return jsonify({"message": "User updated"}), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### REMOVE USER ACCOUNT     
@app.route("/user", methods=["DELETE"])
@auth_user
def remove_user_account():
    """Removes an user's account from the database."""
    try:
        user_id = request.user_id
        headers = supabase_headers()

        # Delete services
        requests.delete(f"{SUPABASE_REST_URL}/service?service_provider=eq.{user_id}", headers=headers)
        requests.delete(f"{SUPABASE_REST_URL}/service?service_client=eq.{user_id}", headers=headers)

        # Delete service requests
        requests.delete(f"{SUPABASE_REST_URL}/service_request?requester=eq.{user_id}", headers=headers)

        # Delete provider roles
        requests.delete(f"{SUPABASE_REST_URL}/pro_info?pro_id=eq.{user_id}", headers=headers)

        # Delete profile picture reference
        requests.delete(f"{SUPABASE_REST_URL}/pfp?pfp_user_id=eq.{user_id}", headers=headers)

        # Delete user
        res = requests.delete(f"{SUPABASE_REST_URL}/user?user_id=eq.{user_id}", headers=headers)
        if res.status_code in [OK_CODE, 204]:
            return jsonify({"message": "User and all associated data deleted"}), OK_CODE
        return jsonify({"message": res.text}), res.status_code

    except Exception as e:
        return jsonify({"message": f"Error deleting user: {str(e)}"}), SERVER_ERROR
    

#|---------------------------------------------------------------------------------------------------|
#|                                  SERVICE PROVIDER ENDPOINTS                                       |
#| Manage service provider roles (one entry per role in `pro_info` table).                           |
#| Includes listing, creating, updating, and deleting roles for the authenticated user.              |
#| Supports filtering by role type, budget, search text, and distance.                               |
#|---------------------------------------------------------------------------------------------------|

####################################### GET ALL SERVICE PROVIDERS   
@app.route("/providers", methods=["GET"])
@auth_user
def get_providers():
    """Returns a list of all service providers."""
    try:
        service_type = request.args.get("serviceType", "").lower()
        max_budget = float(request.args.get("maxBudget", 1e9))
        query_text = request.args.get("query", "").lower()
        max_distance_km = float(request.args.get("maxDistance", 1e9))

        user_location = get_user_location(request.user_id)        
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE
        
        # Get provider data
        res_users = requests.get(USER_URL, headers=supabase_headers())
        users = res_users.json()
        user_map = {u["user_id"]: u for u in users}

        res_roles = requests.get(PRO_INFO_URL, headers=supabase_headers())
        roles = res_roles.json()

        filtered = []
        for r in roles:
            uid = r["pro_id"]
            #Skip own roles
            if uid == request.user_id:
                continue

            if uid not in user_map:
                continue

            price = r.get("pro_price_per_hour", 0)
            if price is not None and price > max_budget:
                continue

            if query_text and not (
                query_text in user_map[uid]["user_name"].lower()
                or query_text in r.get("pro_location", "").lower()
                or query_text in r.get("pro_description", "").lower()
            ):
                continue


            if service_type and service_type not in r.get("pro_role", "").lower():
                continue

            provider_location = r.get("pro_location")
            if not provider_location:
                continue

            filtered.append({
                "id": uid,
                "name": user_map[uid]["user_name"],
                "rating": user_map[uid].get("user_rating", 0),
                "role": r["pro_role"],
                "location": provider_location,
                "description": r["pro_description"],
                "pricePerHour": price
            })


        filtered = get_distance(
            filtered,
            user_location,
            lambda r: r.get("location")
        )

        filtered = [r for r in filtered if r.get("distanceKm", 1e9) <= max_distance_km]
        filtered.sort(key=lambda r: r.get("distanceKm", 1e9))

        return jsonify(filtered), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### GET SP BY ID AND ROLE
@app.route("/providers/details/<int:provider_id>", methods=["GET"])
@auth_user
def get_provider_by_id_and_role(provider_id):
    """Gets detailed info about a provider for a specific role, including distance from current user."""
    try:
        role_name = request.args.get("role", "").strip().lower()
        if not role_name:
            return jsonify({"message": "Role is required"}), BAD_REQUEST_CODE

        user_location = get_user_location(request.user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE

        user_res = requests.get(f"{USER_URL}?user_id=eq.{provider_id}", headers=supabase_headers())
        user_data = user_res.json()
        if not user_data:
            return jsonify({"message": "Provider not found"}), NOT_FOUND_CODE
        provider = user_data[0]

        role_res = requests.get(
            f"{PRO_INFO_URL}?pro_id=eq.{provider_id}&pro_role=ilike.*{role_name}*",
            headers=supabase_headers()
        )

        roles = role_res.json()
        if not roles:
            return jsonify({"message": "Role not found for this provider"}), NOT_FOUND_CODE

        role = roles[0]
        distance_km = get_single_distance(user_location, role["pro_location"])

        return jsonify({
            "id": provider_id,
            "name": provider["user_name"],
            "role": role["pro_role"],
            "location": role["pro_location"],
            "description": role["pro_description"],
            "rating": provider.get("user_rating", 0),
            "pricePerHour": role["pro_price_per_hour"],
            "distanceKm": distance_km
        }), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error getting provider details: {str(e)}"}), SERVER_ERROR

####################################### GET SP'S ROLES    
@app.route("/provider/roles", methods=["GET"])
@auth_user
def get_my_provider_roles():
    """Returns all roles associated with the logged-in provider."""

    try:
        provider_id = request.user_id
        res = requests.get(f"{PRO_INFO_URL}?pro_id=eq.{provider_id}", headers=supabase_headers())
        roles = res.json()

        if not roles:
            return jsonify([]), OK_CODE

        user_res = requests.get(f"{USER_URL}?user_id=eq.{provider_id}", headers=supabase_headers())
        user_data = user_res.json()
        if not user_data:
            return jsonify({"message": "User not found"}), NOT_FOUND_CODE

        provider = user_data[0]
        enriched = []

        for r in roles:
            enriched.append({
                "id": provider_id,
                "name": provider["user_name"],
                "rating": provider.get("user_rating", 0),
                "role": r["pro_role"],
                "location": r["pro_location"],
                "description": r["pro_description"],
                "pricePerHour": r["pro_price_per_hour"]
            })

        return jsonify(enriched), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error fetching roles: {str(e)}"}), SERVER_ERROR

####################################### ADD SP'S DETAILS    
@app.route("/provider", methods=["POST"])
@auth_user
def add_provider_info():
    """Adds a provider's role/profession details."""
    content = request.get_json()
    required_fields = ["role", "location", "description", "pricePerHour"]
    if not all(field in content for field in required_fields):
        return jsonify({"message": "Missing required fields"}), BAD_REQUEST_CODE

    try:
        provider_id = request.user_id
        role = content["role"]

        # Check if user already added a similar role
        params = {
            "pro_id": f"eq.{provider_id}",
            "pro_role": f"eq.{role}"
        }
        check_res = requests.get(PRO_INFO_URL, headers=supabase_headers(), params=params)
        existing = check_res.json()
        if existing:
            return jsonify({"message": "Role already exists for this provider"}), CONFLICT_CODE

        # Add the new role
        payload = {
            "pro_id": provider_id,
            "pro_role": role,
            "pro_location": content["location"],
            "pro_description": content["description"],
            "pro_price_per_hour": content["pricePerHour"]
        }

        insert_res = requests.post(PRO_INFO_URL, headers=supabase_headers(), json=payload)
        if insert_res.status_code in [OK_CODE, CREATED_CODE]:
            return jsonify({"message": "Role added"}), CREATED_CODE
        return jsonify({"message": insert_res.text}), insert_res.status_code
    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR
    
####################################### UPDATE SP'S DETAILS    
@app.route("/provider", methods=["PATCH"])
@auth_user
def update_provider_info():
    """Updates provider's role details."""
    content = request.get_json()
    if "role" not in content:
        return jsonify({"message": "Missing 'role' field to identify entry"}), BAD_REQUEST_CODE

    try:
        provider_id = request.user_id
        role = content["role"]

        update_data = {
            key: content[key] for key in ["pro_location", "pro_description", "pro_price_per_hour"]
            if key in content
        }
        if not update_data:
            return jsonify({"message": "No fields to update"}), BAD_REQUEST_CODE

        update_url = f"{SUPABASE_REST_URL}/pro_info?pro_id=eq.{provider_id}&pro_role=eq.{role}"
        update_res = requests.patch(update_url, headers=supabase_headers(), json=update_data)
        if update_res.status_code == OK_CODE:
            return jsonify({"message": "Role info updated"}), OK_CODE
        return jsonify({"message": update_res.text}), update_res.status_code
    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### REMOVE SP'S ROLE       
@app.route("/provider", methods=["DELETE"])
@auth_user
def remove_provider_role():
    """Removes provider's role."""

    role = request.args.get("role")
    if not role:
        return jsonify({"message": "Missing 'role' parameter"}), BAD_REQUEST_CODE

    try:
        provider_id = request.user_id
        delete_url = f"{PRO_INFO_URL}?pro_id=eq.{provider_id}&pro_role=eq.{role}"
        res = requests.delete(delete_url, headers=supabase_headers())

        if res.status_code in [OK_CODE, 204]:
            return jsonify({"message": "Provider role removed"}), OK_CODE
        return jsonify({"message": res.text}), res.status_code

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

#|---------------------------------------------------------------------------------------------------|
#|                                  SERVICE REQUEST ENDPOINTS                                        |
#| Clients create service requests acceptable by matching providers.                                 |
#| Requests can be updated or removed by the creator.                                                |
#| Providers can independently accept or reject requests directed to them.                           |
#| Supports filters like distance, service type, and max budget.                                     |
#|---------------------------------------------------------------------------------------------------|
####################################### GET SERVICE REQUESTS
@app.route("/requests", methods=["GET"])
@auth_user
def get_service_requests():
    """Returns all service requests, filtered by type, distance, and budget."""

    try:
        spinner = request.args.get("spinner", "")
        budget = float(request.args.get("budget", 1e9))
        query_text = request.args.get("query", "")
        max_distance_km = float(request.args.get("maxDistance", "1e9"))

        user_location = get_user_location(request.user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE

        # Restrict to pending requests
        filters = {
            "request_status": "eq.pending"
        }
        if spinner:
            filters["service_type"] = f"ilike.*{spinner}*"

        res = requests.get(f"{SUPABASE_REST_URL}/service_request", headers=supabase_headers(), params=filters)
        service_requests = res.json()

        filtered = []
        for r in service_requests:
            if r["service_price"] < budget:
                continue

            if query_text:
                title = r.get("service_title", "").lower()
                description = r.get("service_description", "").lower()
                location = r.get("service_location", "").lower()
                if query_text.lower() not in f"{title} {description} {location}":
                    continue

            location = r.get("service_location")
            if not location:
                location = get_request_location(r.get("request_id"))

            if not location:
                continue

            r["resolved_location"] = location
            filtered.append(r)

        # Assign distances
        filtered = get_distance(
            filtered,
            user_location,
            lambda r: r.get("resolved_location")
        )

        # Apply distance filter
        filtered = [r for r in filtered if r.get("distanceKm", 1e9) <= max_distance_km]

        # Sort by distance then deadline
        filtered.sort(key=lambda r: (r.get("distanceKm", float('inf')), r.get("service_deadline", "")))

        return jsonify(filtered), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR
  
####################################### GET SERVICE REQUEST BY ID      
@app.route("/request/<int:request_id>", methods=["GET"])
@auth_user
def get_request_by_id(request_id):
    """Fetches a specific service request by ID, including distance from current user."""

    try:
        user_location = get_user_location(request.user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE

        url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}&select=*"
        res = requests.get(url, headers=supabase_headers())

        if res.status_code != OK_CODE:
            return jsonify({"message": f"Supabase error: {res.status_code}"}), SERVER_ERROR

        data = res.json()
        if not data or not isinstance(data, list):
            return jsonify({"message": "Request not found"}), NOT_FOUND_CODE

        request_obj = data[0]

        request_location = get_request_location(request_id)
        if not request_location:
            return jsonify({"message": "Service request location not available"}), BAD_REQUEST_CODE

        request_obj["distanceKm"] = get_single_distance(user_location, request_location)
        return jsonify(request_obj), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Unhandled error: {str(e)}"}), SERVER_ERROR

####################################### GET REQUEST OWNER/CREATOR       
@app.route("/request/check-ownership/<int:request_id>", methods=["GET"])
@auth_user
def check_request_ownership(request_id):
    """Checks whether the current user is the creator of the specified request."""

    try:
        user_id = request.user_id
        url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
        res = requests.get(url, headers=supabase_headers())
        data = res.json()

        if not data:
            return jsonify({"message": "Request not found"}), NOT_FOUND_CODE

        is_owner = (data[0]["requester"] == user_id)
        return jsonify({"isOwner": is_owner}), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR
    
####################################### GET SERVICE PROVIDER PENDING REQUESTS      
@app.route("/requests/provider", methods=["GET"])
@auth_user
def get_pending_requests_for_provider():
    """Returns all pending requests visible to the logged-in service provider."""

    try:
        user_id = request.user_id
        user_location = get_user_location(user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE
        headers = supabase_headers()

        # Get provider's roles
        roles_res = requests.get(f"{PRO_INFO_URL}?pro_id=eq.{user_id}", headers=headers)
        roles_data = roles_res.json()
        provider_roles = {r["pro_role"] for r in roles_data if "pro_role" in r}

        # Get all pending requests
        req_res = requests.get(f"{SUPABASE_REST_URL}/service_request?request_status=eq.pending", headers=headers)
        all_requests = req_res.json()

        # Get requested providers for current user
        rp_res = requests.get(f"{SUPABASE_REST_URL}/requested_providers?sp_id=eq.{user_id}", headers=headers)
        rp_data = rp_res.json()
        requested_ids = {r["request_id"] for r in rp_data}

        # Filter visible requests
        visible = []
        for req in all_requests:
            if req.get("requested_provider") == user_id:
                visible.append(req)
            elif (
                not req.get("requested_provider")
                and req.get("service_type") in provider_roles
                and req.get("request_id") in requested_ids
            ):
                visible.append(req)

        # Assign distances efficiently
        visible = get_distance(
            visible,
            user_location,
            lambda r: r.get("service_location") or get_request_location(r.get("request_id"))
        )

        # Sort by deadline and distance
        visible.sort(key=lambda r: (r.get("service_deadline", ""), r.get("distanceKm", float('inf'))))

        return jsonify(visible), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### GET CLIENT'S REQUESTS  
@app.route("/requests/client", methods=["GET"])
@auth_user
def get_client_requests():
    """Returns all service requests created by the current user, filtered by status, query, and budget."""

    try:
        status = request.args.get("status")
        query_text = request.args.get("query", "").lower()
        budget = float(request.args.get("budget", 1e9)) 
        user_id = request.user_id

        user_location = get_user_location(user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE

        params = {
            "requester": f"eq.{user_id}"
        }
        if status:
            params["request_status"] = f"eq.{status}"

        headers = supabase_headers()
        headers["Prefer"] = "order=service_deadline.asc"

        res = requests.get(f"{SUPABASE_REST_URL}/service_request", headers=headers, params=params)
        requests_data = res.json()

        filtered = []
        for r in requests_data:
            if r["service_price"] > budget:
                continue

            if query_text:
                title = r.get("service_title", "").lower()
                description = r.get("service_description", "").lower()
                location = r.get("service_location", "").lower()
                if query_text not in f"{title} {description} {location}":
                    continue

            location = r.get("service_location") or get_request_location(r.get("request_id"))
            if not location:
                continue

            r["resolved_location"] = location
            filtered.append(r)

        # Distance assignment only (no distance filtering)
        filtered = get_distance(
            filtered,
            user_location,
            lambda r: r.get("resolved_location")
        )

        # Sort by deadline then distance
        filtered.sort(key=lambda r: (r.get("service_deadline", ""), r.get("distanceKm", float('inf'))))

        return jsonify(filtered), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### ADD REQUEST
@app.route("/request", methods=["POST"])
@auth_user
def add_service_request():
    """Creates a new service request and optionally notifies matching providers."""

    content = request.get_json()
    required_fields = ["service_title", "service_type", "service_description", "service_location", "service_price", "service_deadline"]

    if not all(field in content for field in required_fields):
        return jsonify({"message": "Missing required fields"}), BAD_REQUEST_CODE

    try:
        headers = supabase_headers()
        headers["Prefer"] = "return=representation"
        
        request_body = {
            "service_title": content["service_title"],
            "service_type": content["service_type"],
            "service_description": content["service_description"],
            "service_location": content["service_location"],
            "service_price": content["service_price"],
            "service_deadline": content["service_deadline"],
            "requester": request.user_id,
            "request_status": "pending"
        }

        if "requested_provider" in content:
            request_body["requested_provider"] = content["requested_provider"]

        res = requests.post(f"{SUPABASE_REST_URL}/service_request", headers=headers, json=request_body)
        if res.status_code not in [OK_CODE, CREATED_CODE]:
            return jsonify({"message": res.text}), res.status_code

        created_request = res.json()[0]
        request_id = created_request["request_id"]

        # Notify all providers with matching roles via the requested_providers table
        if "requested_provider" not in content:
            pro_res = requests.get(
                f"{PRO_INFO_URL}?pro_role=eq.{content['service_type']}", headers=headers
            )
            for sp in pro_res.json():
                requests.post(f"{SUPABASE_REST_URL}/requested_providers", headers=headers, json={
                    "request_id": request_id,
                    "sp_id": sp["pro_id"],
                    "sp_status": "pending"
                })

        return jsonify({"message": "Service request created"}), CREATED_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### UPDATE REQUEST
@app.route("/request/<int:request_id>", methods=["PATCH"])
@auth_user
def update_service_request(request_id):
    """Updates a service request's fields if the logged-in user is the owner."""

    content = request.get_json()
    allowed_fields = [
        "service_title",
        "service_type",
        "service_description",
        "service_location",
        "service_price",
        "service_deadline"
    ]
    data = {k: content[k] for k in allowed_fields if k in content}

    if not data:
        return jsonify({"message": "No valid fields to update"}), BAD_REQUEST_CODE

    try:
        check_url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
        check_res = requests.get(check_url, headers=supabase_headers())
        existing = check_res.json()

        if not existing:
            return jsonify({"message": "Request not found"}), NOT_FOUND_CODE

        if existing[0]["requester"] != request.user_id:
            return jsonify({"message": "You are not authorized to edit this request"}), FORBIDDEN_CODE

        update_url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
        update_res = requests.patch(update_url, headers=supabase_headers(), json=data)

        if update_res.status_code in [OK_CODE, 204]:
            return jsonify({"message": "Request updated"}), OK_CODE
        return jsonify({"message": update_res.text}), update_res.status_code

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### REMOVE REQUEST   
@app.route("/request/<int:request_id>", methods=["DELETE"])
@auth_user
def remove_service_request(request_id):
    """Removes a service request if the user is the owner."""

    try:
        # Verify ownership
        check_url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
        check_res = requests.get(check_url, headers=supabase_headers())
        data = check_res.json()

        if not data:
            return jsonify({"message": "Service request not found"}), NOT_FOUND_CODE

        if data[0]["requester"] != request.user_id:
            return jsonify({"message": "You are not authorized to delete this request"}), FORBIDDEN_CODE

        # Remove if the user is the request creator
        delete_url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
        delete_res = requests.delete(delete_url, headers=supabase_headers())

        if delete_res.status_code in [OK_CODE, 204]:
            return jsonify({"message": "Service request deleted"}), OK_CODE

        return jsonify({"message": delete_res.text}), delete_res.status_code

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### HANDLE REQUEST DECISION
@app.route("/request/decision", methods=["PATCH"])
@auth_user
def handle_service_request():
    """Handles acceptance or rejection of a service request by a provider."""

    content = request.get_json()
    request_id = content.get("requestId")
    provider_id = request.user_id
    decision = content.get("accept")

    if request_id is None or decision not in [True, False]:
        return jsonify({"message": "Missing or invalid fields"}), BAD_REQUEST_CODE

    try:
        headers = supabase_headers()

        # Get the request
        req_url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
        res = requests.get(req_url, headers=headers)
        request_data = res.json()
        if not request_data:
            return jsonify({"message": "Service request not found"}), NOT_FOUND_CODE

        original = request_data[0]

        if decision is True:
            if original.get("request_status") != "pending":
                return jsonify({"message": "Service request has already been accepted or closed"}), CONFLICT_CODE
            
            # Get provider roles 
            role_res = requests.get(f"{PRO_INFO_URL}?pro_id=eq.{provider_id}", headers=headers)
            roles = [r["pro_role"].lower() for r in role_res.json() if "pro_role" in r]
            # Get request service type 
            request_type = original.get("service_type", "").lower()
            # Check if provider has a matching role for the service type
            if request_type not in roles:
                return jsonify({"message": "You do not have the required role to accept this request"}), FORBIDDEN_CODE

            # Change request status (to accepted)
            patch_url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
            patch_data = {"requested_provider": provider_id, "request_status": "accepted"}
            patch_res = requests.patch(patch_url, headers=headers, json=patch_data)

            if patch_res.status_code not in [OK_CODE, 204]:
                return jsonify({"message": "Failed to accept request"}), SERVER_ERROR

            # Accepted requests are converted into services for further tracking (status, payments, etc)
            new_service = {
                "service_title": original["service_title"],
                "service_type": original["service_type"],
                "service_description": original["service_description"],
                "service_location": original["service_location"],
                "service_price": original["service_price"],
                "service_deadline": original["service_deadline"],
                "service_client": original["requester"],
                "service_provider": provider_id,
                "service_status": "accepted"
            }

            service_res = requests.post(f"{SUPABASE_REST_URL}/service", headers=headers, json=new_service)
            if service_res.status_code not in [OK_CODE, CREATED_CODE]:
                return jsonify({"message": "Service creation failed"}), SERVER_ERROR

            return jsonify({"message": "Service request accepted"}), OK_CODE

        else:
            # Cancel the previously accepted request
            patch_url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
            patch_data = {
                "requested_provider": None,
                "request_status": "pending"
            }
            patch_res = requests.patch(patch_url, headers=headers, json=patch_data)
            if patch_res.status_code not in [OK_CODE, 204]:
                return jsonify({"message": "Failed to cancel acceptance"}), SERVER_ERROR

            # Delete service if it exists
            delete_url = f"{SUPABASE_REST_URL}/service?service_provider=eq.{provider_id}&service_client=eq.{original['requester']}&service_title=eq.{original['service_title']}"
            requests.delete(delete_url, headers=headers)

            return jsonify({"message": "Service request returned to pending"}), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR
    
#|---------------------------------------------------------------------------------------------------|
#|                                    SERVICE ENDPOINTS                                              |
#| Manage active and completed services created from accepted requests.                              |
#| Allows users and providers to view service details and update service status.                     |
#| Filterable by distance, service type, and status.                                                 |
#|---------------------------------------------------------------------------------------------------|
####################################### GET ALL SERVICES
@app.route("/services", methods=["GET"])
@auth_user
def get_services():
    """Returns list of all accepted requests (which now are services) filtered by user location and status."""

    try:
        user_id = request.user_id
        user_location = get_user_location(user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE

        service_type = request.args.get("serviceType", "")
        budget = float(request.args.get("maxBudget", 1e9))
        query_text = request.args.get("query", "")
        status = request.args.get("status", "")

        filters = {}
        if service_type:
            filters["service_type"] = f"ilike.*{service_type}*"
        if query_text:
            filters["service_description"] = f"ilike.*{query_text}*"
        if status:
            filters["service_status"] = f"eq.{status}"

        headers = supabase_headers()
        res = requests.get(SERVICE_URL, headers=headers, params=filters)
        services = res.json()

        filtered = []
        for s in services:
            if s["service_price"] > budget:
                continue

            if query_text and query_text.lower() not in f"{s['service_description']} {s.get('service_location', '')}".lower():
                continue

            filtered.append(s)

        # Efficient batch distance assignment
        filtered = get_distance(
            filtered,
            user_location,
            lambda s: s.get("service_location") or get_service_location(s.get("service_id"))
        )

        # Sort by deadline first, then distance
        filtered.sort(key=lambda x: (x.get("service_deadline", ""), x.get("distanceKm", float('inf'))))

        return jsonify(filtered), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### GET SERVICE BY ID
@app.route("/service/<int:service_id>", methods=["GET"])
@auth_user
def get_service_by_id(service_id):
    """Returns full details of a specific service including distance to user."""

    try:
        user_location = get_user_location(request.user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE

        url = f"{SUPABASE_REST_URL}/service?service_id=eq.{service_id}"
        res = requests.get(url, headers=supabase_headers())
        data = res.json()

        if not data:
            return jsonify({"message": "Service not found"}), NOT_FOUND_CODE

        service = data[0]
        service_location = service.get("service_location")

        if not service_location:
            return jsonify({"message": "Service location not available"}), BAD_REQUEST_CODE

        service["distanceKm"] = get_single_distance(user_location, service_location)

        return jsonify(service), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR

####################################### GET PROVIDER'S SERVICES
@app.route("/services/provider/<int:provider_id>", methods=["GET"])
@auth_user
def get_services_by_provider(provider_id):
    """Returns all services handled by a specific provider, filtered by query, status, and min. budget."""

    try:
        query_text = request.args.get("query", "").lower()
        status = request.args.get("status", "").lower()
        budget = float(request.args.get("budget", 0))  # Min budget

        user_location = get_user_location(request.user_id)
        if not user_location:
            return jsonify({"message": "User location not available"}), BAD_REQUEST_CODE

        headers = supabase_headers()
        url = f"{SUPABASE_REST_URL}/service?service_provider=eq.{provider_id}"

        res = requests.get(url, headers=headers)
        if res.status_code != OK_CODE:
            return jsonify({"message": "Failed to fetch services"}), res.status_code

        services = res.json()

        filtered = []
        for s in services:
            if s["service_price"] < budget:
                continue

            if status and s.get("service_status", "").lower() != status:
                continue

            if query_text and query_text not in f"{s.get('service_title', '')} {s.get('service_description', '')} {s.get('service_location', '')}".lower():
                continue

            s["resolved_location"] = s.get("service_location") or get_service_location(s.get("service_id"))
            filtered.append(s)

        # Assign distances
        filtered = get_distance(
            filtered,
            user_location,
            lambda s: s.get("resolved_location")
        )

        # Sort by deadline then distance
        filtered.sort(key=lambda s: (s.get("service_deadline", ""), s.get("distanceKm", float('inf'))))

        return jsonify(filtered), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error fetching provider's services: {str(e)}"}), SERVER_ERROR

####################################### GET ALL REQUEST AND SERVICE TYPES AND PROVIDER ROLES
@app.route("/serviceTypes", methods=["GET"])
@auth_user
def get_all_service_types():
    """Returns the union of all available service types (from requests, services, and roles)."""

    try:
        headers = supabase_headers()

        # Distinct types from service table
        service_res = requests.get(f"{SUPABASE_REST_URL}/service?select=service_type", headers=headers)
        service_types = {entry["service_type"].capitalize() for entry in service_res.json() if entry.get("service_type")}

        # Distinct types from service_request table
        request_res = requests.get(f"{SUPABASE_REST_URL}/service_request?select=service_type", headers=headers)
        request_types = {entry["service_type"].capitalize() for entry in request_res.json() if entry.get("service_type")}

        # Distinct roles from pro_info table
        pro_res = requests.get(f"{SUPABASE_REST_URL}/pro_info?select=pro_role", headers=headers)
        provider_roles = {entry["pro_role"].capitalize() for entry in pro_res.json() if entry.get("pro_role")}

        # Merge and sort all types
        all_types = sorted(service_types.union(request_types).union(provider_roles))

        return jsonify({"types": all_types}), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error fetching service types: {str(e)}"}), SERVER_ERROR
        
####################################### UPDATE SERVICE STATUS
@app.route("/service/status", methods=["PATCH"])
@auth_user
def update_service_status():
    """ Updates the status of a service
        STATUS: Accepted (by both parts) || Started (By SP) || finished (by SP) || Paid (By client) || Closed (finished and paid) || Cancelled (with justification)
    """

    # Receive service data
    content = request.get_json()
    service_id = content.get("serviceId")
    new_status = content.get("status")

    allowed_statuses = ["accepted", "started", "finished", "paid", "cancelled", "closed"]

    # Check if status is sent and allowed
    if service_id is None or new_status not in allowed_statuses:
        return jsonify({"message": "Missing or invalid fields"}), BAD_REQUEST_CODE

    try:
        url = f"{SUPABASE_REST_URL}/service?service_id=eq.{service_id}"
        payload = {"service_status": new_status}
        # Updates service status
        res = requests.patch(url, headers=supabase_headers(), json=payload)
        if res.status_code in [OK_CODE, 204]:
            return jsonify({"message": f"Service status updated to '{new_status}'"}), OK_CODE
        return jsonify({"message": res.text}), res.status_code

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR
    
#|---------------------------------------------------------------------------------------------------|
#|                                  PROFILE PICTURE ENDPOINTS                                        |
#| Uploads and retrieves user profile pictures via Supabase Storage.                                 |
#| Uses a separate decorator to forward the user's token for secure uploads.                         |
#|---------------------------------------------------------------------------------------------------|

def auth_with_token_forwarding(f):
    """ 
        Decorator to authenticate and forward the Bearer token for Supabase Storage authorization.
        A bucket (storage > new bucket > public) was created to store the profile pictures
        The pfp table only stores the reference to these pictures
    """
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get("Authorization")
        if not token:
            return jsonify({"message": "Token is missing"}), UNAUTHORIZED_CODE
        try:
            clean_token = token.replace("Bearer ", "")
            data = jwt.decode(clean_token, app.config["SECRET_KEY"], algorithms=["HS256"])
            request.user_id = data["id"]
            request.jwt_token = token 
        except:
            return jsonify({"message": "Invalid token"}), UNAUTHORIZED_CODE
        return f(*args, **kwargs)
    return decorated

####################################### GET PROFILE PICTURE       
@app.route("/profilePicture/<int:user_id>", methods=["GET"])
def get_profile_picture(user_id):
    """Returns the profile picture URL of the specified user, or empty if not set."""

    try:
        url = f"{SUPABASE_REST_URL}/pfp?pfp_user_id=eq.{user_id}"
        res = requests.get(url, headers=supabase_headers())
        results = res.json()

        if not results:
            return jsonify({"profilePic": ""}), 200

        return jsonify({"profilePic": results[0]["user_pfp"]}), 200
    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), 500
    
####################################### UPLOAD/UPDATE PROFILE PICTURE   
@app.route("/profilePicture", methods=["PUT"])
@auth_with_token_forwarding
def upload_profile_picture():
    """Uploads or updates the logged-in user's profile picture to Supabase Storage."""

    if "file" not in request.files:
        return jsonify({"message": "No file part in the request"}), BAD_REQUEST_CODE

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"message": "No selected file"}), BAD_REQUEST_CODE

    try:
        filename = f"{uuid.uuid4()}.png"
        upload_url = f"{SUPABASE_URL}/storage/v1/object/pfps/{filename}"
        public_url = f"{SUPABASE_URL}/storage/v1/object/public/pfps/{filename}"

        # Supabase requires the file to be uploaded using a separate service role key for public buckets
        storage_headers = {
            "Authorization": f"Bearer {SUPABASE_SERVICE_ROLE_KEY}",
            "Content-Type": file.content_type,
            "x-upsert": "true"
        }

        file_bytes = file.read()
        res = requests.put(upload_url, headers=storage_headers, data=file_bytes)
        if not res.ok:
            return jsonify({"message": "Failed to upload", "details": res.text}), SERVER_ERROR

        # DB update
        db_headers = supabase_headers()
        check = requests.get(f"{SUPABASE_REST_URL}/pfp?pfp_user_id=eq.{request.user_id}", headers=db_headers).json()

        if check:
            requests.patch(f"{SUPABASE_REST_URL}/pfp?pfp_user_id=eq.{request.user_id}", headers=db_headers, json={"user_pfp": public_url})
        else:
            requests.post(f"{SUPABASE_REST_URL}/pfp", headers=db_headers, json={
                "pfp_user_id": request.user_id,
                "user_pfp": public_url
            })

        return jsonify({"message": "Profile picture updated", "url": public_url}), OK_CODE

    except Exception as e:
        return jsonify({"message": f"Error: {str(e)}"}), SERVER_ERROR
        
#|---------------------------------------------------------------------------------------------------|
#|                              LOCATION AND DISTANCE LOGIC                                          |
#| Handles user and destination geocoding using OpenRouteService.                                    |
#| Calculates distances between users, services, requests, and providers.                            |
#| Uses caching and fallback logic for performance and reliability.                                  |
#|---------------------------------------------------------------------------------------------------|
 
######################################### GET DISTANCE FOR A SINGLE ROUTE/ADDRESS
def get_single_distance(user_location, destination_location):
    """Computes distance in kilometers between two addresses using cached geocoding."""

    user_coords = get_coords(user_location)
    dest_coords = get_coords(destination_location)

    if not user_coords or not dest_coords:
        return -1

    return haversine_km(
        user_coords[0], user_coords[1],
        dest_coords[0], dest_coords[1]
    )

######################################################### GET DISTANCE
def get_distance(items, user_location, get_location_fn):
    """
        Assigns distance in km to a list of items based on a shared location extractor function.
        This function is used for batch distance calculation and supports any object type
        As long as a location-extractor lambda is provided.    
    """

    # Uses get_coords() to resolve each destination, and calculates user-to-item distance
    user_coords = get_coords(user_location)

    # Error codes are used instead of raising to avoid interrupting batch filters/sorting
    if not user_coords:
        for item in items:
            item["distanceKm"] = -101  # User location not resolved
        return items

    for item in items:
        dest_text = get_location_fn(item)
        if not dest_text:
            item["distanceKm"] = -102  # Destination location missing
            continue

        dest_coords = get_coords(dest_text)
        if not dest_coords:
            item["distanceKm"] = -103  # Destination location could not be geocoded
            continue

        item["distanceKm"] = haversine_km(
            user_coords[0], user_coords[1],
            dest_coords[0], dest_coords[1]
        )

    return items

#################################################### GET USER LOCATION 
def get_user_location(user_id):
    """Gets the user’s stored location based on their user ID."""

    try:
        res = requests.get(f"{USER_URL}?user_id=eq.{user_id}", headers=supabase_headers())
        user_data = res.json()
        if user_data:
            return user_data[0].get("user_location", "")
        return None
    except Exception:
        return None

################################################### GET SERVICE LOCATION 
def get_service_location(service_id):
    """Returns the location of a given service by ID."""

    try:
        url = f"{SUPABASE_REST_URL}/service?service_id=eq.{service_id}"
        res = requests.get(url, headers=supabase_headers())
        data = res.json()
        if data:
            return data[0].get("service_location", "")
        return None
    except Exception:
        return None

################################################### GET REQUEST LOCATION 
def get_request_location(request_id):
    """Returns the location of a given service request by ID."""

    try:
        url = f"{SUPABASE_REST_URL}/service_request?request_id=eq.{request_id}"
        res = requests.get(url, headers=supabase_headers())
        data = res.json()
        if data:
            return data[0].get("service_location", "")
        return None
    except Exception:
        return None

############################################### GET COORDS FROM ADDRESS
# OpenRouteService client and cache
address_cache = {}  # Cache full addresses

def get_coords(location_text):
    """Geocodes a location string into coordinates using OpenRouteService (with fallback caching)."""

    location_key = location_text.strip().lower()

    if location_key in address_cache:
        cached = address_cache[location_key]
        if cached:  # Only return if not None
            return cached
        else:
            return None  # Avoid returning previously cached invalid entry

    try:
        # Use Pelias search for more reliable address-to-coordinates resolution;
        response = distance_client.pelias_search(text=location_text, size=1)
        features = response.get("features", [])
        if features:
            coords = features[0]["geometry"]["coordinates"]
            lat, lon = coords[1], coords[0]
            address_cache[location_key] = (lat, lon)
            return lat, lon
        
    # Pelias search fallback is autocomplete
    except openrouteservice.exceptions.ApiError as e:
        if "Rate limit exceeded" in str(e):
            try:
                response = distance_client.pelias_autocomplete(text=location_text, size=1)
                features = response.get("features", [])
                if features:
                    coords = features[0]["geometry"]["coordinates"]
                    lat, lon = coords[1], coords[0]
                    address_cache[location_key] = (lat, lon)
                    return lat, lon
            except:
                pass

    # Does not cache failed result
    return None

################################################## DISTANCE THROUGH HAVERSINE METHOD
def haversine_km(lat1, lon1, lat2, lon2):
    """Calculates approximate distance between two coordinates using the Haversine formula."""

    R = 6371
    dlat = radians(lat2 - lat1)
    dlon = radians(lon2 - lon1)
    a = sin(dlat/2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon/2)**2
    c = 2 * asin(sqrt(a))
    return int(R * c)

######################################################### Main function #########################################################
if __name__ == "__main__":
    app.run(port=8080, debug=True)

