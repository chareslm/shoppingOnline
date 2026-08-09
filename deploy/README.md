# Local infrastructure

## MySQL 8.4

1. Copy `.env.example` to `.env`.
2. Replace both password values with different long random strings.
3. Start MySQL from the repository root:

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
```

4. Check readiness:

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

The `database/init/` directory is mounted into MySQL's initialization directory. Put versioned SQL migration scripts in `database/`; initialization scripts are only executed when the named MySQL data volume is first created.

## Stop or reset

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

Do not use `down -v` unless you intentionally want to delete all local database data.
