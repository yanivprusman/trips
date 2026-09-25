# טיולים · trips

A native Android app (Kotlin Multiplatform + Compose, MapLibre) that draws a trip's marked
route on the Israel Hiking Map and reads it out as you walk: how far you have come, what is
next, the climb still ahead. **The map and the trips ship inside the APK** — a wadi is
exactly where there is no signal.

The Next.js app in this repo is only the feedback-lib backend (dev port 3157).

## Layout

- `mobile/` — the phone app. `shared/commonMain` holds everything that is not a platform
  API: the trip model, route geometry + progress (`geo/`), Hebrew formatting, the theme
  tokens and every screen. `app/` is the Android launcher: MapLibre (`ui/map/`),
  `LocationManager` (`location/`), the asset repository, Hilt, and feedback-lib in the dev
  flavor.
- `mobile/app/src/main/assets/`
  - `trips/<id>.json` — one file per trip, **generated** by `scripts/build-trip-<id>.py`.
  - `tiles/{z}/{x}/{y}.png` — the Israel Hiking Map raster pack for the trip area
    (z12–16; the server has no z17). `assets/style.json` layers the pack under the same
    tiles fetched online.
  - `photos/` — 1400-px JPEGs the trip file names.
- `data/<id>/` — the raw inputs the generator reads (OSM geometry, elevation samples,
  the tile manifest). Recipe in `data/karakash/README.md`.

## Build / install

```bash
cd mobile && ./gradlew :app:assembleDevDebug
/opt/automateLinux/utilities/chunked-adb-install.sh app/build/outputs/apk/dev/debug/app-dev-debug.apk 10.7.0.3:5555 com.automatelinux.trips.dev
```

Never raw `adb install` over WireGuard. `mobile/.env` → `API_BASE_URL=http://10.7.0.2:3157/`.

## Adding a trip

1. Put the route geometry + elevation under `data/<id>/`, write `scripts/build-trip-<id>.py`
   (copy the karakash one — waypoints, text and the practical cards live in the script).
2. Fetch the tile pack for its area into `assets/tiles/` and widen the `bounds` in
   `assets/style.json` if the area is new.
3. Run the script; the app lists whatever is in `assets/trips/`.
