# Local infrastructure

## MySQL 8.4, Redis 7.4 and Elasticsearch 9.4

1. Copy `.env.example` to `.env`.
2. Replace both password values with different long random strings.
3. Start the local infrastructure from the repository root:

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
```

4. Check readiness:

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

The `database/init/` directory is mounted into MySQL's initialization directory. Put versioned SQL migration scripts in `database/`; initialization scripts are only executed when the named MySQL data volume is first created.

Redis persists data in the named `shopping-redis-data` volume and is bound only to `127.0.0.1:${REDIS_PORT:-6379}`. The backend's default `REDIS_HOST=127.0.0.1` and `REDIS_PORT=6379` connect to this service without additional configuration.

Elasticsearch runs as a single local node with security disabled only for local development. It persists data in `shopping-elasticsearch-data`, is bound only to `127.0.0.1:${ELASTICSEARCH_PORT:-9200}`, and matches the backend's default `ELASTICSEARCH_URIS=http://127.0.0.1:9200`. Do not copy this no-authentication configuration to a shared or production environment.

## Stop or reset

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

Do not use `down -v` unless you intentionally want to delete all local MySQL, Redis and Elasticsearch data.
