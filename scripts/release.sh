#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
version=${1:?Usage: scripts/release.sh VERSION dev|prod}
environment=${2:?Choose dev or prod}
[[ "$version" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || exit 1
[[ "$environment" == dev || "$environment" == prod ]] || exit 1
release="releases/$environment-$version"
[[ ! -e "$release" ]] || { echo "Release already exists: $release"; exit 1; }
[[ -f "env/$environment.env" ]] || { echo "Copy env/$environment.env.example to env/$environment.env first"; exit 1; }
for module in rate-provider rate-printer; do
  docker image inspect "homework/$module:$version" >/dev/null
done
mkdir -p "$release"
cp compose.yaml "$release/compose.yaml"
cp -R monitoring "$release/monitoring"
cp "env/$environment.env" "$release/.env"
printf '\nAPP_VERSION=%s\n' "$version" >> "$release/.env"
docker compose --project-directory "$release" --profile apps config --quiet
for module in rate-provider rate-printer; do
  docker image inspect --format '{{.Id}}' "homework/$module:$version"
done > "$release/images.txt"
echo "Release created: $release"
