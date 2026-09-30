# cocos-backend
cocos-challenge-backend

## Run locally with Docker Compose

Docker Compose starts the Spring Boot API and a PostgreSQL 18.6 database. On first startup, Flyway creates the schema and loads the committed database snapshot.

```bash
docker compose up --build
```

The API is available at <http://localhost:8080>; Swagger UI is at <http://localhost:8080/swagger-ui/index.html>. PostgreSQL is published on port `5432` by default. The local defaults are database `portfolio`, username `portfolio`, and password `portfolio`.

Override the local defaults with `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, or `APP_PORT` in the environment before starting Compose. These defaults are for local development only.

Stop the containers and keep the database volume with:

```bash
docker compose down
```

To delete the local database and rerun both migrations from scratch, use:

```bash
docker compose down --volumes
docker compose up --build
```

The seed migration contains the selected snapshot from the configured database.
