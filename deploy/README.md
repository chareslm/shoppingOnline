# Local infrastructure

## Backend, MySQL 8.4, Redis 7.4 and Elasticsearch 9.4

1. Copy `.env.example` to `.env`.
2. Replace both database passwords and `JWT_SECRET` with different long random strings. Configure SMTP variables when real mail delivery is required.
3. Start the local infrastructure from the repository root:

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
```

4. Check readiness:

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

The `database/init/` directory is mounted into MySQL's initialization directory. Put versioned SQL migration scripts in `database/`; initialization scripts are only executed when the MySQL bind-mounted data directory is first created. The backend applies Flyway migrations on startup.

By default, bind-mount data is stored under `DATA_DIR` (see `.env.example`, default `D:/Project/data`):

- MySQL: `${DATA_DIR}/shopping/mysql`
- Redis: `${DATA_DIR}/shopping/redis`
- Elasticsearch: `${DATA_DIR}/shopping/elasticsearch`
- Merchant qualification files: `${DATA_DIR}/shopping/uploads`

Redis persists data in its bind-mounted directory and is bound only to `127.0.0.1:${REDIS_PORT:-6379}`. The Compose backend connects over the private Compose network.

Elasticsearch runs as a single local node with security disabled only for local development. It persists data in its bind-mounted directory and is bound only to `127.0.0.1:${ELASTICSEARCH_PORT:-9200}`. Do not copy this no-authentication configuration to a shared or production environment.

## Stop or reset

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

Do not use `down -v` unless you intentionally want to delete all local MySQL, Redis and Elasticsearch data.

## Local configuration separation

- `deploy/.env.example` is a committed template containing placeholders and option comments.
- `deploy/.env` contains real local values and is ignored by Git.
- `deploy/docker-compose.yml.example` documents the complete persistent Compose layout.
- `deploy/docker-compose.yml` is the active local Compose file; the Windows deploy script creates it from the example when missing.
- `backend/src/main/resources/application-local.yml.example` is the native-backend template.
- `backend/src/main/resources/application-local.yml` contains native local credentials and is ignored by Git.
- Frontend `.env.example` files follow the same rule; their local `.env` files are optional because the default backend URL is already usable.

Do not edit an example file to hold machine-specific credentials. Copy it to the corresponding local file instead.

## Windows scripts

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1
```

The deploy script creates missing local copies, validates required secrets, prepares `${DATA_DIR}/shopping`, starts Compose, waits for backend health, and starts both Vue development servers. The clean script is non-destructive by default; persistent data is removed only with `-PurgePersistentData`.
