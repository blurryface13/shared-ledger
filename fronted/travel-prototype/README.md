# Connected travel UI

This is the current mobile-first web UI for the Java backend. Run `python3 serve.py` here after starting the backend on port 8081, then open <http://127.0.0.1:4178>. Override `PORT` and `TRIP_LEDGER_API_PORT` for an isolated preview. No npm install is needed.

In a trip, **打开地图** opens a Leaflet map. Choose a day or the whole trip, search a place explicitly, select a map marker or list row, or find sights, stays and food within 3 km of a selected place. Nearby distance is straight-line distance. The route button asks the Java backend for a walking road route through the day's positioned stops. Weather comes from Open-Meteo within its forecast window. Existing stops without coordinates can be linked to a matching search result before searching nearby.

The connected UI uses server-backed trips and account permissions. `app.js` and older styles retain the original standalone prototype logic for now; `connected.js` is the active data layer. Some labels and inactive prototype paths still need consolidation. The web UI is not yet the native mini-program.

Leaflet 1.9.4 is vendored under `vendor/leaflet` with its BSD-2-Clause license. OpenStreetMap attribution remains visible. Public Nominatim, tile and OSRM services are for bounded development traffic; see [map atlas notes](../../docs/map-atlas.md) before deployment.

Run browser-independent checks with `node --test *.test.cjs` and `python3 -m unittest serve_test.py`.
