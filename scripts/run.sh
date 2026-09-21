#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
release=${1:?Usage: scripts/run.sh releases/dev-VERSION}
[[ -f "$release/images.txt" ]] || { echo 'Not a prepared release'; exit 1; }
version=$(sed -n 's/^APP_VERSION=//p' "$release/.env" | tail -1)
actual=$(for module in rate-provider rate-printer; do docker image inspect --format '{{.Id}}' "homework/$module:$version"; done)
[[ "$actual" == "$(cat "$release/images.txt")" ]] || { echo 'Image changed since release; refusing to start'; exit 1; }
docker compose --project-directory "$release" --profile apps up -d --wait
