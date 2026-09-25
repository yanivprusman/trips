#!/bin/bash
# Deliver an APK to the phone over a link that keeps dropping (a wadi with one bar),
# then install it, launch it and say so on WhatsApp.
#
# Transport is SSH to Termux, 512 KB at a time, appended to a file on the phone's
# /sdcard and resumed from whatever size is already there — TCP over WireGuard copes
# with loss; `adb push` does not (its buffer stalls for good, see
# utilities/chunked-adb-install.sh). adb is used only for the two short commands that
# need it: `pm install -S` reading the file that is already on the phone, and `am start`.
#
# Usage: deliver-to-phone.sh <apk> <package> <activity> [notify-message]
set -u
APK=$1; PKG=$2; ACTIVITY=$3; NOTE=${4:-}
HOST=${PHONE_HOST:-10.7.0.3}; SSH_USER=${PHONE_SSH_USER:-u0_a424}; SERIAL=${PHONE_ADB:-$HOST:5555}
REMOTE=/sdcard/Download/$(basename "$APK")
CHUNK=524288
GIVE_UP_AFTER=$((8*3600))
LOG=${DELIVER_LOG:-/tmp/deliver-to-phone.log}
SSH=(ssh -o BatchMode=yes -o ConnectTimeout=20 -o ServerAliveInterval=10 -o ServerAliveCountMax=3
     -o ControlMaster=auto -o ControlPath=/run/deliver-ssh-%C -o ControlPersist=120 -o LogLevel=ERROR -p 8022 "$SSH_USER@$HOST")
log() { printf '%s %s\n' "$(date +%H:%M:%S)" "$*" | tee -a "$LOG"; }
rssh() { timeout "${1:-30}" "${SSH[@]}" "${@:2}" 2>>"$LOG"; }

SIZE=$(stat -c %s "$APK"); MD5=$(md5sum "$APK" | cut -d' ' -f1)
log "delivering $APK ($SIZE bytes, md5 $MD5) → $HOST:$REMOTE"
start=$(date +%s); pushed_since=$start

# ---- 1. get the bytes there ---------------------------------------------------------
while :; do
  if (( $(date +%s) - start > GIVE_UP_AFTER )); then log "giving up after 8 h"; exit 1; fi
  have=$(rssh 30 "stat -c %s '$REMOTE' 2>/dev/null || echo 0" | tail -1)
  if ! [[ "$have" =~ ^[0-9]+$ ]]; then log "phone not reachable, waiting"; sleep 45; continue; fi
  if (( have > SIZE )); then log "remote file is larger than ours — stale; removing"; rssh 30 "rm -f '$REMOTE'"; continue; fi
  if (( have == SIZE )); then
    rmd5=$(rssh 120 "md5sum '$REMOTE' | cut -d' ' -f1" | tail -1)
    if [[ "$rmd5" == "$MD5" ]]; then log "all $SIZE bytes on the phone, md5 ok"; break; fi
    log "md5 mismatch ($rmd5) — starting over"; rssh 30 "rm -f '$REMOTE'"; continue
  fi
  if tail -c +$((have+1)) "$APK" | head -c $CHUNK | timeout 150 "${SSH[@]}" "cat >> '$REMOTE'" 2>>"$LOG"; then
    now=$(date +%s); pct=$(( (have+CHUNK)*100/SIZE )); (( pct > 100 )) && pct=100
    log "pushed to $((have+CHUNK > SIZE ? SIZE : have+CHUNK)) / $SIZE (${pct}%)"
    pushed_since=$now
  else
    log "chunk at $have failed; retrying in 15 s"; sleep 15
  fi
done

# ---- 2. install from the file that is already on the phone --------------------------
while :; do
  if (( $(date +%s) - start > GIVE_UP_AFTER )); then log "giving up after 8 h"; exit 1; fi
  timeout 40 adb connect "$SERIAL" >>"$LOG" 2>&1
  out=$(timeout 900 adb -s "$SERIAL" shell "cat '$REMOTE' | pm install -r -g -S $SIZE" 2>&1 | tr -d '\r')
  log "pm install: ${out:-<no output>}"
  if grep -q "Success" <<<"$out"; then break; fi
  sleep 60
done
rssh 30 "rm -f '$REMOTE'"

# ---- 3. launch, and tell him ---------------------------------------------------------
timeout 60 adb -s "$SERIAL" shell "am start -n $PKG/$ACTIVITY" >>"$LOG" 2>&1 && log "launched $PKG"
if [[ -n "$NOTE" ]]; then
  CFG=/opt/automateLinux/mcpServers/whatsapp/whatsapp-mcp-server/.remote.json
  API=$(python3 -c "import json;print(json.load(open('$CFG'))['api_url'])"); TOKEN=$(python3 -c "import json;print(json.load(open('$CFG'))['token'])")
  resp=$(curl -s --max-time 30 -X POST "$API/send" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "$(python3 -c "import json,sys;print(json.dumps({'recipient':'972556677260','message':sys.argv[1]}))" "$NOTE")")
  log "whatsapp: $resp"
fi
log "DONE"
