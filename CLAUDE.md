@AGENTS.md

# trips (טיולים)

Native phone app — see README.md for the layout. Rules that matter here:

- **Dev flavor only**: `./gradlew :app:assembleDevDebug`; install with
  `chunked-adb-install.sh` (a raw `adb install` over WireGuard stalls forever).
- **Never edit `assets/trips/*.json` by hand** — it is generated. Change the script under
  `scripts/` and re-run it.
- **All UI is in `mobile/shared/commonMain`**; the Android module only contributes the map
  view, location, asset loading and Hilt wiring. Keep it that way so iOS stays a launcher away.
- The map is raster tiles from Israel Hiking Map packed in the APK plus the same tiles
  online on top. There is no z17 on that server; MapLibre overzooms z16.
- Commit before building: the APK's version is the git commit count.
