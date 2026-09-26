# Connected travel UI

This is the current mobile-first web UI for the Java backend. Run `python3 serve.py` here after starting the backend on port 8081, then open <http://127.0.0.1:4178>. Override `PORT` and `TRIP_LEDGER_API_PORT` for an isolated preview. No npm install is needed.

In a trip, **打开地图** opens a Leaflet map. Choose a day or the whole trip, search a place explicitly, select a map marker or list row, or find sights, stays and food within 3 km of a selected place. Nearby distance is straight-line distance. The route button asks the Java backend for a walking road route through the day's positioned stops. Weather comes from Open-Meteo within its forecast window. Existing stops without coordinates can be linked to a matching search result before searching nearby.

The connected UI uses server-backed trips and account permissions. `index.html` loads only `connected.js`; the old `app.js` remains as historical prototype source and cannot write into this page. There is no native mini-program frontend in this repository.

Leaflet 1.9.4 is vendored under `vendor/leaflet` with its BSD-2-Clause license. OpenStreetMap attribution remains visible. Public Nominatim, tile and OSRM services are for bounded development traffic; see [map atlas notes](../../docs/map-atlas.md) before deployment.

For a public deployment, replace `map-config.js` before serving the static files with `window.tripMapConfig={tileUrl:'https://tiles.example/{z}/{x}/{y}.png',attribution:'© Your tile provider',weatherUrl:'https://weather.example/forecast'}`. Do not put API keys in this browser file. Configure the Java geocoder and walking route URLs through its environment variables and monitor `/actuator/prometheus` on the loopback management port.

Run browser-independent checks with `node --test *.test.cjs` and `python3 -m unittest serve_test.py`.
