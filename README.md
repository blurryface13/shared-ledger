# Shared Ledger

Shared Ledger is a WeChat Mini Program for collaborative bookkeeping and settlement in scenarios such as group travel, shared meals, and team activities.

## Tech Stack

- Backend: Spring Boot 4, Spring Security, MyBatis-Plus, MySQL
- Frontend: uni-app, Vue 3, Pinia, Wot Design Uni
- Database: MySQL with Docker Compose scripts

## Project Structure

```text
backend/trip-ledger              Spring Boot backend
fronted/_legacy-ledger-fronted   uni-app WeChat Mini Program frontend
fronted/ledger-workbench         standalone frontend workbench
sql                              MySQL schema and Docker Compose files
```

## Local Development

Create local environment files from the examples before running services.

```bash
cp sql/.env.dev.example sql/.env.local
cp backend/trip-ledger/deploy/dev.env.example backend/trip-ledger/deploy/dev.env
```

Start MySQL:

```bash
cd sql
docker compose --env-file .env.local up -d
```

Start the backend:

```bash
cd backend/trip-ledger
set -a
source deploy/dev.env
set +a
./mvnw spring-boot:run
```

Build the WeChat Mini Program frontend:

```bash
cd fronted/_legacy-ledger-fronted
npm install
npm run build:mp-weixin
```

Then import `fronted/_legacy-ledger-fronted/dist/build/mp-weixin` in WeChat Developer Tools.

## Security Notes

Do not commit real `.env` files, WeChat App Secret, JWT secrets, database passwords, runtime logs, uploaded files, or build artifacts. Use the included `*.example` files as templates.
