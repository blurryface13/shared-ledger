# Shared Ledger

Shared Ledger combines a Java travel itinerary and collaborative ledger. The active connected UI is a mobile-first web client; the uni-app mini-program remains in the legacy directory.

## Tech Stack

- Backend: Spring Boot 4, Spring Security, MyBatis-Plus, MySQL
- Connected UI: browser JavaScript and Leaflet
- Legacy mini-program: uni-app, Vue 3, Pinia, Wot Design Uni
- Infrastructure: MySQL, Redis and RabbitMQ through Docker Compose

## Project Structure

```text
backend/trip-ledger              Spring Boot backend
fronted/_legacy-ledger-fronted   uni-app WeChat Mini Program frontend
fronted/travel-prototype         connected web UI and local API proxy
fronted/ledger-workbench         standalone frontend workbench
sql                              MySQL schema and Docker Compose files
```

## Local Development

Create local environment files from the examples before running services.

```bash
cp sql/.env.dev.example sql/.env.local
cp backend/trip-ledger/deploy/dev.env.example backend/trip-ledger/deploy/dev.env
```

Start the local infrastructure:

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

Use Java 21. In another terminal, start the connected web UI:

```bash
cd fronted/travel-prototype
python3 serve.py
```

Open http://127.0.0.1:4178. The proxy uses backend port 8081 by default. Create a local login via the development test-account flow. In a trip, choose **打开地图** to see its places, search explicitly, find nearby sights/stays/food, and request a walking route for a day with at least two positioned stops. The map does not request device location. See [map atlas notes](docs/map-atlas.md) for provider limits and coordinate handling.

The legacy WeChat Mini Program can still be built separately:

```bash
cd fronted/_legacy-ledger-fronted
npm install
npm run build:mp-weixin
```

Then import `fronted/_legacy-ledger-fronted/dist/build/mp-weixin` in WeChat Developer Tools.

## Security Notes

Do not commit real `.env` files, WeChat App Secret, JWT secrets, database passwords, runtime logs, uploaded files, or build artifacts. Use the included `*.example` files as templates.

## Engineering Notes

- [RabbitMQ and Redis engineering notes](docs/rabbitmq-redis-engineering-notes.md)
