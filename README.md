# Flight Management System (Docker)

# From repo root
docker compose up
- Eureka: http://localhost:8761
- API Gateway: http://localhost:8765
- Flight Service: http://localhost:8080 (through gateway)
- Booking Service: http://localhost:8081 (through gateway)
Databases:
- flight DB: localhost:5433/flight_db (postgres/postgres:root)
- booking DB: localhost:5434/booking_db
- gateway DB: localhost:5435/api_gateway_db

## Endpoints (via Gateway)
Base: `http://localhost:8765`

### Auth (public)
- POST `/api/v1.0/auth/signup`
- POST `/api/v1.0/auth/signin`
- POST `/api/v1.0/auth/signout`

### Flight (admin, Bearer token with ROLE_ADMIN)
- POST `/api/v1.0/flight/admin/flights`
- GET `/api/v1.0/flight/admin/flights`
- GET `/api/v1.0/flight/admin/flights/{flightNumber}`
- DELETE `/api/v1.0/flight/admin/flights/{id}`
- POST `/api/v1.0/flight/admin/inventory`
- GET `/api/v1.0/flight/admin/flights/health`

### Flight Search (public)
- POST `/api/v1.0/flight/admin/search`

### Booking/Ticket (user or admin)
- POST `/api/v1.0/flight/booking/{scheduleId}`
- GET `/api/v1.0/flight/booking/{pnr}`
- GET `/api/v1.0/flight/bookings/email/{email}`
- DELETE `/api/v1.0/flight/booking/cancel/{pnr}`
- GET `/api/v1.0/flight/ticket/{pnr}`
- GET `/api/v1.0/flight/health`

## 
- Obtain JWT via signin, then include `Authorization: Bearer <token>` on protected routes.
- Eureka handles service discovery; gateway routes to flight-service and booking-service.