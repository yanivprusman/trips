# Nahal Karakash — where the data came from (2026-09-25)

- **Route**: OSM hiking relation 3438295 (green, ways 929186435 + 156647457) from the
  Midrasha entrance parking to the junction with Nahal Havarim; then the tail of blue way
  509318196 (relation 3579466) to the serpentine road; then red way 266508978 (relation
  3579469) reversed, up to the Midrasha; then the shortest walk over the Midrasha's roads
  (Dijkstra over highway=* ways from an OSM `/api/0.6/map` bbox download) back to the
  parking. `loop-raw.json` holds the four ordered segments. 3,717 m total.
- **Elevation**: `loop-dense-ele.json` — the loop densified to ≤15 m and sampled with the
  Open-Meteo elevation API (90 m SRTM). Smoothed 3-point before gain/loss.
- **Tiles**: `tiles-manifest.json` — Israel Hiking Map `Hebrew/Tiles/{z}/{x}/{y}.png`,
  boxes per zoom; z17 answers 204 (empty) everywhere, so the pack stops at 16.
- **Text**: the OSM relation's description, baliletayel.co.il's route page, and the
  Wikimedia Commons photos on the relation (Tsur Halamish, public domain).

`scripts/build-trip-karakash.py` turns all of this into `assets/trips/karakash.json`.
