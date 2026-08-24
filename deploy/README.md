# Local infrastructure

## One-click (Windows)

From the repository root, with Docker Desktop running:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1
```

The script copies missing gitignored files from examples, fills empty `DATA_DIR` / `MAVEN_REPO_DIR` / secrets, starts Compose, waits until `http://127.0.0.1:${BACKEND_PORT}/actuator/health` is `UP`, then starts Vite on ports 5173 (admin) and 5174 (web).

Do not log in while Maven is still compiling. Use `scripts/windows/wait-backend.ps1` if you restart the backend yourself.

## Manual Compose

1. Copy `deploy/.env.example` to `deploy/.env`, or let the script do it.
2. `[REQUIRED]` secrets may stay as `replace-*` for the script to generate, or set them yourself.
3. `[OPTIONAL]` `DATA_DIR` and `MAVEN_REPO_DIR` may stay empty; the script writes `%USERPROFILE%/shopping-data` and `%USERPROFILE%/.m2`.
4. Leave `MAIL_HOST` empty unless you have a real SMTP server.

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

`database/init/` is mounted for first-time MySQL data directories only. Flyway runs from the backend on startup (`database/V*__*.sql`).

Bind mounts (all under `${DATA_DIR}/shopping/`):

- `mysql`, `redis`, `elasticsearch`, `uploads` (qualifications + product images)

Redis and Elasticsearch bind to `127.0.0.1` only. Elasticsearch security is off for local use only.

## Stop or reset

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1
```

Do not use `down -v` unless you intend to drop Docker named volumes (this project uses bind mounts; purge host data with `clean-local.ps1 -PurgePersistentData`).

## File roles (keep examples decoupled from secrets)

| File | Git | Notes |
| --- | --- | --- |
| `deploy/.env.example` | tracked | Comments for `[REQUIRED]` / `[OPTIONAL]` / `[NATIVE]` |
| `deploy/.env` | ignored | Machine secrets and paths |
| `deploy/docker-compose.yml` | tracked | No host-specific directories |
| `deploy/docker-compose.yml.example` | tracked | Same layout; do not store `D:\...` here |
| `backend/.../application.yml` | tracked | Env placeholders only |
| `backend/.../application.yml.example` | tracked | Documentation; never overwrite `application.yml` |
| `application-local.yml` | ignored | Host-run Spring Boot only |
| Frontend `.env` | ignored | Optional; default API URL already works |

Never put credentials into an example file. Copy the example, then edit the local file (or let `deploy-local.ps1` fill placeholders).
