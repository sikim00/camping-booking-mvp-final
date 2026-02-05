# camping-booking-mvp (Spring Boot + Kotlin)

## Requirements
- JDK 17
- Docker (optional)

## Local PostgreSQL (optional)
```bash
docker run --name camp-pg -e POSTGRES_DB=camp -e POSTGRES_USER=camp -e POSTGRES_PASSWORD=camp -p 5432:5432 -d postgres:16-alpine
```

## Run
```bash
./gradlew bootRun
```

## Gate-1 tests (Testcontainers)
```bash
./gradlew test
```

## Notes
- Overlap prevention: UNIQUE(site_id, night_date) on `booking_nights`
- Refund idempotency: UNIQUE(idempotency_key) on `refunds`
- JWT (minimal): claims must include `userId` (number) and `role` (string)

## macOS (Apple Silicon) quick run (Colima or Docker Desktop)

### 1) Start Docker engine
- **Colima (recommended lightweight)**:
  ```bash
  colima start --cpu 4 --memory 6 --disk 60
  docker context use colima
  docker info
  ```
- **Docker Desktop**: launch Docker Desktop and ensure it is running.

### 2) Run app (DB + API)
```bash
docker compose up --build -d db app
curl http://localhost:8080/health
```

### 3) Run Gate-1 tests (Testcontainers)
```bash
docker compose run --rm test
```

### 4) Shutdown
```bash
docker compose down
```

