#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
umask 077

OWNER="xboyrex"
REPO="MiTool"
BRANCH="main"
ZIP_NAME="MiUnlockTool-Fixed-8.5.0.zip"
BASE="$HOME/.miunlocktool-deploy"
EXTRACT="$BASE/extracted"
DEST="$BASE/repo"
API="https://api.github.com/repos/$OWNER/$REPO"

info(){ printf '\n[INFO] %s\n' "$*"; }
ok(){ printf '[ OK ] %s\n' "$*"; }
fail(){ printf '[ERROR] %s\n' "$*" >&2; exit 1; }

echo "=============================================="
echo " Mi Unlock Tool 8.5.0 | Audit + Push + Build"
echo "=============================================="

[ -n "${PREFIX:-}" ] || fail "Run this script inside Termux."

pkg install -y git curl unzip rsync python

termux-setup-storage 2>/dev/null || true
DOWNLOAD="$HOME/storage/shared/Download"
[ -d "$DOWNLOAD" ] || DOWNLOAD="/storage/emulated/0/Download"
[ -d "$DOWNLOAD" ] || fail "Android Download folder not available."

ZIP_PATH="$(find "$DOWNLOAD" -maxdepth 3 -type f -name "$ZIP_NAME" -print -quit)"
[ -n "$ZIP_PATH" ] || fail "$ZIP_NAME not found in Download. Download the fixed ZIP first."

rm -rf "$BASE"
mkdir -p "$EXTRACT" "$DEST"

info "Validating source archive..."
python - "$ZIP_PATH" "$EXTRACT" <<'PY'
import sys, zipfile, stat
archive, destination = sys.argv[1:]
with zipfile.ZipFile(archive) as z:
    bad = z.testzip()
    if bad:
        raise SystemExit(f"Corrupt ZIP member: {bad}")
    for item in z.infolist():
        name = item.filename.replace("\\", "/")
        if name.startswith("/") or ".." in name.split("/"):
            raise SystemExit(f"Unsafe ZIP path: {name}")
        if stat.S_ISLNK(item.external_attr >> 16):
            raise SystemExit(f"Symlink rejected: {name}")
    z.extractall(destination)
print("ZIP CRC and path safety checks passed.")
PY

PROJECT="$(python - "$EXTRACT" <<'PY'
import sys
from pathlib import Path
base = Path(sys.argv[1])
matches = []
for settings in base.rglob("settings.gradle.kts"):
    root = settings.parent
    if (root / "app/build.gradle.kts").is_file() and \
       (root / "app/src/main/AndroidManifest.xml").is_file():
        matches.append(root)
if len(matches) != 1:
    raise SystemExit(f"Expected one Android project root; found {len(matches)}")
print(matches[0])
PY
)"

info "Project root: $PROJECT"

[ -f "$PROJECT/tools/verify_source.py" ] || fail "Source audit script missing."
python "$PROJECT/tools/verify_source.py" || fail "Static source audit failed."

ok "Static audit passed."

# Reuse the already-configured GitHub login/credential helper.
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
    gh auth setup-git
fi
export GIT_TERMINAL_PROMPT=0

info "Cloning target repository..."
git clone --depth 1 --branch "$BRANCH" \
    "https://github.com/$OWNER/$REPO.git" "$DEST" \
    || fail "Repository clone failed."

cd "$DEST"
git config user.name "xboyrex"
git config user.email "xboyrex@users.noreply.github.com"

info "Synchronizing audited source into $OWNER/$REPO ..."
rsync -a --delete \
    --exclude='.git/' \
    --exclude='.gradle/' \
    --exclude='**/build/' \
    --exclude='local.properties' \
    "$PROJECT/" "$DEST/"

python tools/verify_source.py || fail "Audit failed after repository synchronization."
git diff --check || fail "Git whitespace/conflict check failed."

git add -A
git diff --cached --check || fail "Staged changes failed Git checks."
if git diff --cached --quiet; then
    fail "Repository already matches this source ZIP; no new push was made."
fi

git commit -m "Import audited Mi Unlock Tool 8.5.0 source and CI fixes"
SHA="$(git rev-parse HEAD)"

info "Pushing commit $SHA ..."
git push origin "$BRANCH" || fail "Push failed. Existing Git credential helper lacks push access."

ok "Source pushed. Waiting for GitHub Actions (up to 15 minutes)..."
echo "Actions: https://github.com/$OWNER/$REPO/actions"

RUN_ID=""
RUN_NUMBER=""
RUN_URL=""
STATUS=""
CONCLUSION=""

for attempt in $(seq 1 15); do
    DATA="$(curl -fsSL \
        "$API/actions/runs?branch=$BRANCH&per_page=10" 2>/dev/null || true)"

    if [ -n "$DATA" ]; then
        RESULT="$(python -c '
import json,sys
sha=sys.argv[1]
data=json.load(sys.stdin)
for run in data.get("workflow_runs",[]):
    if run.get("head_sha")==sha and run.get("name")=="MiUnlockTool Android Build and Auto Release":
        print(run.get("id",""),run.get("run_number",""),run.get("status",""),run.get("conclusion") or "pending",run.get("html_url",""))
        break
' "$SHA" <<< "$DATA")" || true

        if [ -n "$RESULT" ]; then
            read -r RUN_ID RUN_NUMBER STATUS CONCLUSION RUN_URL <<< "$RESULT"
            echo "Attempt $attempt/15 | Run #$RUN_NUMBER | $STATUS | $CONCLUSION"

            if [ "$STATUS" = "completed" ]; then
                break
            fi
        fi
    fi
    sleep 60
done

echo
echo "=============================================="
echo "                 FINAL RESULT"
echo "=============================================="
echo "Repository: https://github.com/$OWNER/$REPO"
echo "Commit:     $SHA"
echo "Actions:    ${RUN_URL:-https://github.com/$OWNER/$REPO/actions}"

if [ -z "$RUN_ID" ]; then
    fail "Workflow run not detected within 15 minutes. Open Actions link."
fi

if [ "$STATUS" != "completed" ] || [ "$CONCLUSION" != "success" ]; then
    fail "Build/release workflow failed. Open: $RUN_URL"
fi

RELEASE_TAG="miunlocktool-ci-$RUN_NUMBER"
RELEASE="$(curl -fsSL "$API/releases/tags/$RELEASE_TAG" 2>/dev/null || true)"
RELEASE_URL="$(python -c 'import json,sys; print(json.load(sys.stdin).get("html_url",""))' <<< "$RELEASE" 2>/dev/null || true)"

ok "Android build and release workflow completed successfully."
if [ -n "$RELEASE_URL" ]; then
    echo "APK Release: $RELEASE_URL"
else
    echo "Releases: https://github.com/$OWNER/$REPO/releases"
fi
echo "=============================================="
