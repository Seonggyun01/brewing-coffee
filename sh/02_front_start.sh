#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd -P)"

if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
  echo "Node.js와 npm을 설치한 뒤 다시 실행해 주세요. 지원 버전은 front/README.md를 참고해 주세요." >&2
  exit 1
fi

cd "$PROJECT_ROOT/front"
if [[ ! -d node_modules ]]; then
  echo "프론트 의존성을 설치합니다."
  npm ci
fi

echo "Vue 3 SSR 프론트를 실행합니다. 기본 주소: http://127.0.0.1:3000"
echo "백엔드 기본 주소는 http://127.0.0.1:8080이며 front/.env의 NUXT_BACKEND_URL로 변경할 수 있습니다."
exec npm run dev -- "$@"
