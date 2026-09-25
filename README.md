# CARRY - Peer-to-Peer Campus Delivery Platform

CARRY is a peer-to-peer delivery platform designed for university students. It connects students who need items purchased and delivered (Customers) with peers who are already traveling to those areas (Delivery Partners). 

By leveraging existing campus commutes, CARRY makes small-scale logistics affordable, reliable, and community-driven.

## Tech Stack
- **Backend:** Java 17+, Spring Boot 3, Spring Security, Spring Data JPA, JWT Authentication, Flyway, MySQL
- **Frontend:** React 18, React Router DOM v6, Vite
- **Testing & Tooling:** JUnit 5, MockMvc, Maven, npm

---

## Architecture & Workflows

1. **User Management & Auth**: Registration with bcrypt password hashing and stateless JWT-based authentication.
2. **Trips (Delivery Partners)**: Partners log their planned trips (`PLANNED` -> `IN_CITY` -> `RETURNING` -> `COMPLETED`).
3. **Product Requests (Customers)**: Customers post requests for items from specific zones with a given budget.
4. **Matching Engine**: Partners are shown available requests in their destination zone. Concurrency is handled via Optimistic Locking (`@Version`).
5. **Lifecycle & State Machines**: Orders follow a strict chronological state machine (`ACCEPTED` -> `COLLECTED` -> `RETURNING` -> `READY_FOR_DELIVERY` -> `DELIVERED`).
6. **Ratings & Oversight**: Customers rate completed deliveries. Dispute resolution is handled through an Admin panel.

---

## API Documentation

All secured endpoints require the `Authorization: Bearer <token>` header.

### Authentication & Users
* **`POST /api/auth/register`**: Register a new student user.
  * **Payload:** `{ fullName, studentId, email, phone, department, password }`
* **`POST /api/auth/login`**: Authenticate and retrieve JWT. Rate-limited (5 requests/min).
  * **Payload:** `{ email, password }` 
  * **Returns:** `{ token, user: { ... } }`
* **`GET /api/users/me`**: Get current user profile (safely omitting sensitive fields).

### Trips (Delivery Partners)
* **`POST /api/trips`**: Post a new trip.
  * **Payload:** `{ departureTime, expectedReturnTime, destinationArea, capacityNotes }`
* **`GET /api/trips/mine`**: Retrieve current user's trips.
* **`PUT /api/trips/{id}/status`**: Update trip status.
  * **Valid Flow:** `PLANNED` -> `IN_CITY` -> `RETURNING` -> `COMPLETED` (or `CANCELLED`).

### Product Requests (Customers)
* **`POST /api/requests`**: Create a new product request.
  * **Payload:** `{ productName, category, quantity, preferredShop, pickupArea, budget, instructions }`
* **`GET /api/requests/mine`**: Retrieve current user's requests.
* **`PUT /api/requests/{id}`**: Edit an unassigned request.
* **`PUT /api/requests/{id}/status`**: Cancel an order (if unassigned).

### Deliveries (State Machine)
* **`GET /api/requests/available`**: List `REQUESTED` items matching the partner's active trip zones.
* **`PUT /api/requests/{id}/accept`**: Atomically assign an available request to the partner's trip.
* **`PUT /api/requests/{id}/status`**: Advance order status.
  * **Valid Flow:** `ACCEPTED` -> `COLLECTED` -> `RETURNING` -> `READY_FOR_DELIVERY` -> `DELIVERED`.
* **`GET /api/requests/{id}/timeline`**: Get history of status updates.

### Ratings & Complaints
* **`POST /api/requests/{id}/rating`**: Rate a `DELIVERED` order (1-5 score, comment). Auto-updates the partner's global average.
* **`POST /api/complaints`**: Raise a dispute.
  * **Payload:** `{ description, requestId, againstUserId }`

### Admin (Requires `isAdmin = true`)
* **`GET /api/admin/users`**: List all users.
* **`GET /api/admin/requests`**: List/filter all requests.
* **`GET /api/admin/complaints`**: View dispute queue.
* **`PUT /api/admin/complaints/{id}`**: Update dispute status (`OPEN`, `IN_REVIEW`, `RESOLVED`) and `adminNotes`.

---

## Known Limitations vs. Original Brief

To streamline development for the academic/MVP scope, the following simplifications were made deliberately:
1. **Manual Status Checkpoints vs. Live GPS Tracking:** Instead of live WebSockets and coordinate tracking, the app uses discrete, manually triggered lifecycle checkpoints (e.g., clicking "In City" or "Returning").
2. **Simulated vs. Real Payments:** Payment integrations (like SSLCommerz or Stripe) were omitted. When an order hits `DELIVERED`, a dummy `Payment` entity is generated with status `SIMULATED_PAID`.
3. **Fixed-Zone vs. Real Route Matching:** True geospatial distance calculations (PostGIS, Google Maps API) were simplified into fixed campus zones via the `LocationArea` Enum (e.g., `KHULNA_CITY`, `DAKBUGLA`, `NEW_MARKET`). Matching occurs strictly via string equivalence rather than radius mapping.

## Future Improvements

To take CARRY to a full-scale commercial launch, the following improvements are recommended:
1. **Live GPS Tracking:** Integrate `navigator.geolocation` on the frontend and WebSockets on the backend to provide real-time partner locations to the customer once an order is collected.
2. **Route-Based Matching:** Use a routing engine (e.g., OSRM) or geospatial queries to match requests based on actual coordinate proximity and route deviation metrics.
3. **Real Payment Gateway Integration:** Implement a secure escrow system holding the customer's funds upon request creation, releasing them to the partner (minus a platform fee) post-delivery.
4. **Push Notifications:** Integrate Firebase Cloud Messaging (FCM) to notify users of state transitions immediately.
5. **Multi-University Expansion:** Add a `University` entity and multi-tenancy logic to scope users, trips, and requests strictly to their own campus ecosystems.

---

## Local Setup

### Backend
1. Ensure MySQL is running on `localhost:3306`.
2. Configure `.env` using `.env.example`.
3. Build and run:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./mvnw clean spring-boot:run
```

### Frontend
1. `cd frontend`
2. `npm install`
3. `npm run dev`
