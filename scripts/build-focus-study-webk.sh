#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
webk_root="$project_root/third_party/telegram-web-k"
android_assets="$project_root/app/src/main/assets"
if [[ -n "${PNPM_BIN:-}" ]]; then
  pnpm_command=(node "$PNPM_BIN")
else
  pnpm_command=(corepack pnpm@11.16.0)
fi

node_version="$(node -p 'process.versions.node')"
if ! node -e 'const [major, minor] = process.versions.node.split(".").map(Number); process.exit(major > 22 || (major === 22 && minor >= 18) || major >= 24 ? 0 : 1)'; then
  printf 'Telegram Web K requires Node.js 22.18+ or 24.11+ (found %s).\n' "$node_version" >&2
  exit 1
fi

"${pnpm_command[@]}" --dir "$webk_root" install --frozen-lockfile
"${pnpm_command[@]}" --dir "$webk_root" run typecheck
"${pnpm_command[@]}" --dir "$webk_root" run build:focus-study

rm -rf "$android_assets/telegram-web-k" "$android_assets/focus-web-public"
mkdir -p "$android_assets/telegram-web-k" "$android_assets/focus-web-public"
cp -a "$webk_root/dist/." "$android_assets/telegram-web-k/"
cp -a "$webk_root/public/assets/." "$android_assets/focus-web-public/"

printf 'Bundled restricted Telegram Web K into %s\n' "$android_assets"
