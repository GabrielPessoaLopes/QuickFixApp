# QuickFix

- QuickFix is a mobile application that connects clients and service providers directly.
- Without commissions, subscriptions, or intermediaries.
- Built, specially, for rural and underserved areas.
- It allows any user to request or offer services easily.
- This project is still under development, and so am I — thanks for checking it out!

## Stack

- Frontend: Android (Java + XML)
- Backend: Flask (Python) hosted on Vercel
- Database: PostgreSQL via Supabase
- Auth: JWT (token-based authentication)

## Features

General
- Register, log in, edit, or delete your account
- Upload a profile picture
- Switch between light and dark themes

Client
- Create service requests with title, type, description, location, and budget
- View and cancel pending requests
- Filter providers by role, distance, and price
- Optionally request a provider directly

Service Provider
- Add multiple professional roles (e.g., electrician, plumber)
- View all open service requests matching your role
- Accept or reject requests
- Update service status (pending, started, finished, paid, closed)

## Structure

- `/android_app/` — Android source code
- `/flask_api/` — Python REST API
- `/docs/` — Report, pitch, ER diagram, and Postman collection

## Distance Logic

- Real distances are calculated using OpenRouteService based on full addresses.
- When the API fails, a local Haversine fallback is used.
- Results are cached to reduce load and speed up filtering.

## License

- Developed by Gabriel Lopes.

## Upcoming Changes

- UI and UX enhancements to improve layout consistency and visual structure
- Refactoring of the API to ensure better organization, escalation and adherence to REST principles
- Improved responsiveness and adaptability across different screen sizes
- Support for localization to make the app accessible to a broader audience
- Enhanced security measures, including refined token handling and validation


