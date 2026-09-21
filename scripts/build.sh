#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
version=${1:?Usage: scripts/build.sh VERSION [BROKER_URL]}
[[ "$version" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || { echo 'Invalid version'; exit 1; }
for module in rate-provider rate-printer; do
  if docker image inspect "homework/$module:$version" >/dev/null 2>&1; then
    echo "Version already exists: homework/$module:$version. Choose a new version."
    exit 1
  fi
done
mvn clean verify "-Dpact.broker.url=${2:-http://localhost:9292}" "-Dpact.app.version=$version"
for module in rate-provider rate-printer; do
  docker build --build-arg "MODULE=$module" --build-arg "VERSION=$version" -t "homework/$module:$version" .
done
