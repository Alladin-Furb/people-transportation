# How to Add a New Microservice

This guide explains how to add a new microservice with its own database to the `docker-compose.yml`.

---

## Project Structure

Every microservice follows this folder pattern:

```
microservices-auth/
├── .env                    ← single env file for ALL services
├── docker-compose.yml      ← defines all containers
├── auth-service/
│   ├── Dockerfile
│   └── src/...
├── api-gateway/
│   ├── Dockerfile
│   └── src/...
└── your-new-service/       ← new folder
    ├── Dockerfile
    └── src/...
```

---

## Step 1: Create the service folder and Dockerfile

Create a folder at the root of the project with its own `Dockerfile`:

```
mkdir your-new-service
```

The Dockerfile follows a two-stage build pattern — the first stage compiles the code, the second stage runs it:

```dockerfile
# Stage 1: Build
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> Change the `EXPOSE` port to match what your service uses (8082, 8083, etc.).

---

## Step 2: Add environment variables to `.env`

Use a unique prefix so variable names don't collide with existing services:

```env
# ============================================
# Your New Service Database
# ============================================
NEW_SERVICE_DB_NAME=your_database
NEW_SERVICE_DB_USER=your_user
NEW_SERVICE_DB_PASSWORD=your_secret_password
```

> **Important:** Docker Compose reads this file from the host machine — it is never copied inside the containers.
>
> **Warning:** If you have any of these same variable names exported in your OS/shell (e.g. in `~/.zshrc`), the OS values take priority over the `.env` file. Use `echo $VARIABLE_NAME` to check.

---

## Step 3: Add the database container to `docker-compose.yml`

Each microservice can have its own independent database. Here are two examples:

### Example A: PostgreSQL

```yaml
services:
  # ... existing services ...

  your-new-db:
    image: postgres:16
    container_name: your-new-db
    environment:
      POSTGRES_DB: ${NEW_SERVICE_DB_NAME}
      POSTGRES_USER: ${NEW_SERVICE_DB_USER}
      POSTGRES_PASSWORD: ${NEW_SERVICE_DB_PASSWORD}
    volumes:
      - your-new-db-data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${NEW_SERVICE_DB_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Example B: MariaDB

```yaml
  your-new-db:
    image: mariadb:11.2
    container_name: your-new-db
    environment:
      MYSQL_ROOT_PASSWORD: ${NEW_SERVICE_DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${NEW_SERVICE_DB_NAME}
      MYSQL_USER: ${NEW_SERVICE_DB_USER}
      MYSQL_PASSWORD: ${NEW_SERVICE_DB_PASSWORD}
    volumes:
      - your-new-db-data:/var/lib/mysql
    networks:
      - backend
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Key concepts

| Field | What it does |
|---|---|
| `image` | The database engine and version to use |
| `environment` | Variables that the DB reads on **first initialization only** to create the user, password, and database. Changing them later has no effect unless you delete the volume |
| `volumes` | Persistent storage — data survives `docker-compose down`. Use `docker-compose down -v` to wipe it and reinitialize |
| `networks: backend` | Places the DB on the internal network so only other containers can access it (not the outside world) |
| `healthcheck` | Lets other containers wait until the DB is ready before starting |

---

## Step 4: Add the microservice container to `docker-compose.yml`

```yaml
  your-new-service:
    build:
      context: ./your-new-service    # folder containing the Dockerfile
      dockerfile: Dockerfile
    container_name: your-new-service
    environment:
      # Spring Boot reads these env vars and overrides application.yml defaults
      SPRING_DATASOURCE_URL: jdbc:postgresql://your-new-db:5432/${NEW_SERVICE_DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${NEW_SERVICE_DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${NEW_SERVICE_DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    networks:
      - backend
    depends_on:
      your-new-db:
        condition: service_healthy    # waits for the DB healthcheck to pass
```

### How the connection URL works

```
jdbc:postgresql://your-new-db:5432/${NEW_SERVICE_DB_NAME}
                  ^^^^^^^^^^^  ^^^^
                  container    default
                  name         port
```

Docker resolves container names as hostnames inside the same network. So `your-new-db` becomes the IP of the database container — no need for `localhost` or hardcoded IPs.

### How environment variable resolution works

The full chain from `.env` to your application:

```
.env                          docker-compose.yml                    application.yml (inside container)
──────────────                ─────────────────────────             ──────────────────────────────────
NEW_SERVICE_DB_PASSWORD=1234  SPRING_DATASOURCE_PASSWORD: ${NEW..}  password: ${SPRING_DATASOURCE_PASSWORD:default}
     │                                   │                                          │
     └── Docker Compose reads ──────────►└── Injected into container env ──────────►└── Spring Boot reads
```

---

## Step 5: Register the route in the API Gateway

Open `api-gateway/src/main/resources/application.yml` and add a new route:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: ${AUTH_SERVICE_URL:http://auth-service:8081}
          predicates:
            - Path=/api/auth/**

        # --------- NEW ROUTE ---------
        - id: your-new-service
          uri: http://your-new-service:8082
          predicates:
            - Path=/api/your-resource/**
```

This tells the gateway: "any request matching `/api/your-resource/**` should be forwarded to `your-new-service` on port 8082".

---

## Step 6: Register the new volume

At the bottom of `docker-compose.yml`, add your new volume name:

```yaml
volumes:
  mariadb-data:
  your-new-db-data:     # ← add this
```

Without this, the database data is lost every time you run `docker-compose down`.

---

## Step 7: Build and run

```bash
docker-compose down
docker-compose up -d --build
```

Verify all containers are running:

```bash
docker ps
```

You should see your new DB and service listed with status `Up`.

---

## Networks overview

```
                    Internet
                       │
                  ┌────▼─────┐
  frontend        │ api-gateway │ :8080 (only public entry point)
  (bridge)        └────┬─────┘
─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
                       │
  backend        ┌─────▼──────┐     ┌──────────────────┐
  (internal)     │ auth-service │────│ mariadb           │
                 └─────────────┘    └──────────────────┘
                 ┌──────────────┐   ┌──────────────────┐
                 │ your-service  │────│ your-new-db      │
                 └──────────────┘   └──────────────────┘
```

- **frontend**: Exposed to the outside world. Only the `api-gateway` should be here.
- **backend** (`internal: true`): Isolated. Services and databases talk to each other here, but nothing from outside can reach them directly.

---

## Common issues

| Problem | Cause | Fix |
|---|---|---|
| `Access denied (using password: NO)` | OS environment variables overriding `.env` values | Run `echo $VARIABLE_NAME` to check. Use `unset VARIABLE_NAME` to clear, or remove from `~/.zshrc` |
| `Failed to resolve 'service-name'` | The target container is not running or not on the same network | Run `docker ps` — if it exited, check its logs with `docker logs container-name` |
| DB changes in `.env` are ignored | Database volumes persist old data from first initialization | Run `docker-compose down -v` to delete volumes and reinitialize |
| Container starts before DB is ready | Missing `depends_on` with `condition: service_healthy` | Add the healthcheck to the DB and the condition to the service |
