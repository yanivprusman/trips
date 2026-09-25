#!/usr/bin/env python3
"""Build mobile/app/src/main/assets/trips/karakash.json — the Nahal Karakash loop.

Inputs (checked in under data/karakash/):
  loop-raw.json        four ordered segments (green Karakash, blue Havarim, red climb,
                       the walk back through the Midrasha), from OSM relation 3438295
                       + ways 509318196 / 266508978 + a shortest path over the
                       Midrasha's roads. See data/karakash/README.md for the recipe.
  loop-dense-ele.json  the same loop densified to <=15 m steps with SRTM elevation
                       (Open-Meteo elevation API).

Everything the phone shows about the trip — text, waypoints, distances — comes
from here, so a change to the route is a re-run of this script, not an edit of
the JSON by hand.
"""
import json, math, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = os.path.join(ROOT, "data", "karakash")
OUT = os.path.join(ROOT, "mobile", "app", "src", "main", "assets", "trips", "karakash.json")

def dist(a, b):
    R = 6371000.0
    dlat = math.radians(b[0] - a[0]); dlon = math.radians(b[1] - a[1])
    la1 = math.radians(a[0]); la2 = math.radians(b[0])
    h = math.sin(dlat / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin(dlon / 2) ** 2
    return 2 * R * math.asin(math.sqrt(h))

raw = json.load(open(os.path.join(D, "loop-raw.json")))
dense = json.load(open(os.path.join(D, "loop-dense-ele.json")))

# ---- the route line, with chainage -------------------------------------------------
line = []          # (lat, lon)
seg_of = []        # segment index per vertex
for si, s in enumerate(raw["segments"]):
    for p in s["points"]:
        if line and line[-1] == (p[0], p[1]):
            continue
        line.append((p[0], p[1])); seg_of.append(si)
chain = [0.0]
for i in range(1, len(line)):
    chain.append(chain[-1] + dist(line[i - 1], line[i]))
total = chain[-1]

def project(p, seg):
    """Chainage (m) of the point on the line nearest to p, looking only at segment
    `seg` — the loop closes on itself, so without that hint the start would project
    onto the finish."""
    best = (1e18, 0.0)
    for i in range(len(line) - 1):
        if seg_of[i] != seg:
            continue
        a, b = line[i], line[i + 1]
        # planar projection is fine at this scale (a few km, lat 30.85)
        kx = math.cos(math.radians(a[0]))
        ax, ay = a[1] * kx, a[0]; bx, by = b[1] * kx, b[0]; px, py = p[1] * kx, p[0]
        dx, dy = bx - ax, by - ay
        L2 = dx * dx + dy * dy
        t = 0.0 if L2 == 0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L2))
        q = (ay + dy * t, (ax + dx * t) / kx)
        d = dist(p, q)
        if d < best[0]:
            best = (d, chain[i] + t * (chain[i + 1] - chain[i]))
    return best[1]

# ---- elevation profile: distance along, elevation ----------------------------------
prof = []
c = 0.0
for i, p in enumerate(dense):
    if i > 0:
        c += dist((dense[i - 1]["lat"], dense[i - 1]["lon"]), (p["lat"], p["lon"]))
    prof.append({"d": round(c, 1), "e": p["ele"]})
# smooth (3-pt) before computing gain/loss so SRTM noise does not add fake climbs
sm = [(prof[max(0, i - 1)]["e"] + prof[i]["e"] + prof[min(len(prof) - 1, i + 1)]["e"]) / 3 for i in range(len(prof))]
gain = sum(max(0.0, sm[i] - sm[i - 1]) for i in range(1, len(sm)))
loss = sum(max(0.0, sm[i - 1] - sm[i]) for i in range(1, len(sm)))
for i, p in enumerate(prof):
    p["e"] = round(sm[i], 1)

def at_fraction_of_segment(si, f):
    pts = raw["segments"][si]["points"]
    L = sum(dist(pts[i], pts[i + 1]) for i in range(len(pts) - 1))
    target = L * f; acc = 0.0
    for i in range(len(pts) - 1):
        d = dist(pts[i], pts[i + 1])
        if acc + d >= target:
            t = (target - acc) / d if d else 0
            return (pts[i][0] + (pts[i + 1][0] - pts[i][0]) * t, pts[i][1] + (pts[i + 1][1] - pts[i][1]) * t)
        acc += d
    return tuple(pts[-1])

green, blue, red, road = raw["segments"]
# ---- waypoints, in walking order ---------------------------------------------------
W = [
    dict(id="start", kind="start", seg=0, at=(30.854247, 34.770708),
         title="חניון הכניסה למדרשה",
         text="חונים בחניון העפר שמשמאל לכביש הכניסה למדרשה, מיד אחרי הפנייה מכביש 40. השלט החום \"נחל קרקש\" מימין לכביש הוא תחילת הסימון הירוק."),
    dict(id="view", kind="view", seg=0, at=(30.850716, 34.772629),
         title="תצפית נחל קרקש",
         text="השביל יורד לאט ובמתינות אל שולי הערוץ. מכאן רואים את הנחל כולו נפרש למטה, עד המפגש עם נחל חווארים."),
    dict(id="plates", kind="nature", seg=0, at=at_fraction_of_segment(0, 0.62),
         title="משטחי הסלע והעץ הגדול",
         text="הערוץ נפתח במשטחי סלע ארוכים וישרים כרצפה, ולצידם עץ גדול ויפה. השביל הצר עובר בין אזור סלעי למצוקים התלויים מעל הערוץ — הגיר הלבן־רך נותן לנוף את צבעו."),
    dict(id="flint", kind="nature", seg=0, at=at_fraction_of_segment(0, 0.82),
         title="סלעי הצור השחורים",
         text="הקטע היפה ביותר במסלול: הנוף מתחלף מגיר לבן לסלעי צור ענקיים בצבע חום־שחור על רקע הגבעות הלבנות. חולפים על פניהם בזהירות — יש ירידות תלולות על סלע, ונעלי הליכה סגורות הן חובה."),
    dict(id="junction", kind="junction", seg=1, at=tuple(green["points"][-1]),
         title="מפגש נחל חווארים — פונים שמאלה",
         text="הסימון הירוק נגמר בסימון הכחול של נחל חווארים. אפשר ימינה או שמאלה — פונים שמאלה (מזרחה) וממשיכים כל הזמן ישר עם הכחול."),
    dict(id="marl", kind="nature", seg=1, at=at_fraction_of_segment(1, 0.55),
         title="רגלי הפיל — עמודי החוואר",
         text="בשולי הערוץ עמודי חוואר בצורת רגלי פיל, שמגדירים את גבולות הנחל. אלה החווארים שנתנו לנחל את שמו: גיר רך שהגשמים סודקים ומפוררים, וכך הערוץ מתרחב משנה לשנה."),
    dict(id="road", kind="road", seg=2, at=tuple(blue["points"][-1]),
         title="כביש הסרפנטינות",
         text="הליכה קצרה מביאה לכביש שיורד מהמדרשה אל הכניסה לשמורת עין עבדת. עם רכב מאסף בחניון הסרפנטינות (שמאלה, ליד עמדת רשות הטבע) — כאן המסלול הקווי נגמר. אחרת: השילוט \"מדרשת בן־גוריון\" והסימון האדום מטפסים במקביל לכביש."),
    dict(id="climb", kind="climb", seg=2, at=at_fraction_of_segment(2, 0.5),
         title="העלייה למדרשה",
         text="עלייה קצרה אך תלולה מאוד, כ־110 מטר טיפוס, בשביל מסומן אדום שעובר ממש במקביל לכביש. לאט, עם הפסקות, ועם מים."),
    dict(id="top", kind="road", seg=3, at=tuple(red["points"][-1]),
         title="סוף העלייה — פנייה שמאלה",
         text="השביל מתחבר לכביש. פונים שמאלה וממשיכים כל הזמן ישר, דרך המדרשה ובשדרה הראשית, עד ליציאה ולחניון."),
    dict(id="finish", kind="finish", seg=3, at=tuple(road["points"][-1]),
         title="חזרה לחניון",
         text="סוף המסלול המעגלי. עד הרכב נשאר רק לחצות את הכביש."),
]
for w in W:
    w["lat"], w["lon"] = round(w["at"][0], 6), round(w["at"][1], 6)
    w["atM"] = round(project(w["at"], w["seg"]))
    del w["at"]; del w["seg"]
W.sort(key=lambda w: w["atM"])
W[0]["atM"] = 0; W[-1]["atM"] = round(total)

# distance to the road (the linear alternative ends there)
road_wp = next(w for w in W if w["id"] == "road")

trip = {
    "id": "karakash",
    "name": "נחל קרקש",
    "subtitle": "מדרשת בן־גוריון · הר הנגב",
    "kind": "loop",
    "lengthM": round(total),
    "durationMin": 120,
    "gainM": round(gain),
    "lossM": round(loss),
    "minEleM": round(min(p["e"] for p in prof)),
    "maxEleM": round(max(p["e"] for p in prof)),
    "difficulty": "קל–בינוני",
    "family": True,
    "markers": ["green", "blue", "red"],
    "summary": "ערוץ מדברי קצר ומרהיב היורד משולי המדרשה אל נחל חווארים — משטחי סלע, מצוקי גיר לבנים וסלעי צור שחורים. מהיפים והקלים באזור שדה בוקר.",
    "description": [
        "נחל קרקש הוא נחל אכזב שמתחיל ממש בכניסה למדרשת בן־גוריון ויורד בכ־2 קילומטרים אל נחל חווארים ומשם אל בקעת צין. הוא קרוי על שם שיח הקרקש הצהוב, ממשפחת הפרפרניים, שפירותיו הנפוחים מרשרשים ברוח; שיא הפריחה באביב.",
        "השביל מסומן ירוק בצורה בולטת וברורה. הוא מתחיל בירידה מתונה, עובר על משטחי סלע ישרים כרצפה, נכנס לקטע צר בין מצוקי גיר לבנים, ומגיע לקטע שבו סלעי צור ענקיים בצבע חום־שחור בולטים מקו הערוץ — הקטע היפה ביותר. יש ירידות תלולות על סלע: נעלי הליכה סגורות.",
        "במפגש עם הסימון הכחול של נחל חווארים פונים שמאלה. הערוץ מתרחב, ובשוליו עמודי חוואר שמזכירים רגלי פיל. הליכה קצרה מביאה לכביש הסרפנטינות, ומשם — עם הסימון האדום — עלייה קצרה ותלולה חזרה למדרשה, והליכה בשדרה הראשית עד לרכב.",
    ],
    "practical": [
        {"icon": "clock", "title": "כשעתיים", "text": "כ־4 ק״מ במסלול המעגלי. עם רכב מאסף בחניון הסרפנטינות: כ־2.5 ק״מ, שעה ורבע, בלי העלייה."},
        {"icon": "water", "title": "אין מים במסלול", "text": "לפחות 2 ליטר לאדם. המסלול חשוף לשמש — כובע, ויציאה מוקדמת בקיץ."},
        {"icon": "shoes", "title": "נעלי הליכה סגורות", "text": "ירידות תלולות על סלע בנחל קרקש, ועלייה תלולה מאוד בסימון האדום."},
        {"icon": "season", "title": "סתיו, חורף ואביב", "text": "באביב שיחי הקרקש פורחים ומרשרשים. אחרי גשם — לא נכנסים לערוץ (שיטפונות)."},
        {"icon": "family", "title": "מתאים למשפחות", "text": "קצר, ברור ומסומן היטב; לא נגיש לעגלות. אין להביא כלבים לשמורה."},
        {"icon": "parking", "title": "חניה", "text": "חניון עפר בכניסה למדרשה, משמאל לכביש. חניון הסרפנטינות — ליד עמדת רשות הטבע, לפני הכניסה לעין עבדת."},
    ],
    "rules": ["לא יורדים מהשביל המסומן", "לא לוקחים כלום ולא משאירים כלום", "ללא חיות מחמד"],
    "startLat": W[0]["lat"], "startLon": W[0]["lon"],
    "bounds": [round(min(p[1] for p in line), 5), round(min(p[0] for p in line), 5),
               round(max(p[1] for p in line), 5), round(max(p[0] for p in line), 5)],
    "segments": [
        {"name": s["name"], "marker": s["color"], "kind": s["kind"], "lengthM": s["length_m"],
         "points": [[round(p[0], 6), round(p[1], 6)] for p in s["points"]]}
        for s in raw["segments"]
    ],
    "profile": prof,
    "waypoints": W,
    "linear": {"lengthM": road_wp["atM"], "durationMin": 75, "endWaypointId": "road",
               "text": "עם רכב שני בחניון הסרפנטינות: מסיימים בכביש, בלי העלייה."},
    "photos": [
        {"file": "photos/karakash-2.jpg", "caption": "סלע צור עם הסימון הירוק — הקטע היפה במסלול", "hero": True},
        {"file": "photos/karakash-3.jpg", "caption": "משטחי הסלע בתחילת הערוץ"},
        {"file": "photos/karakash-1.jpg", "caption": "שיח הקרקש ופירותיו הנפוחים — שם הנחל"},
        {"file": "photos/karakash-5.jpg", "caption": "פריחת אביב בערוץ"},
        {"file": "photos/karakash-4.jpg", "caption": "הערוץ הרחב לקראת נחל חווארים"},
    ],
    "credits": [
        "מפה: Israel Hiking Map · © OpenStreetMap contributors (ODbL)",
        "מסלול: OpenStreetMap, relation 3438295 · גבהים: Open-Meteo (SRTM)",
        "תיאור: baliletayel.co.il · צילומים: Tsur Halamish (נחלת הכלל)",
    ],
    "mapMaxZoom": 16,
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(trip, open(OUT, "w"), ensure_ascii=False, separators=(",", ":"))
print(f"wrote {OUT}: {os.path.getsize(OUT)//1024} KB, {round(total)} m, +{round(gain)}/-{round(loss)} m, {len(W)} waypoints")
for w in W: print(f"  {w['atM']:5d} m  {w['id']:9s} {w['title']}")
