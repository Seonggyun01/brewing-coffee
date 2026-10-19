#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd -P)"

if ! command -v java >/dev/null 2>&1; then
  echo "Java 21을 설치하고 JAVA_HOME을 설정한 뒤 다시 실행해 주세요." >&2
  exit 1
fi

export BREWLOG_TRACE_DIRECTORY="${BREWLOG_TRACE_DIRECTORY:-$PROJECT_ROOT/logs}"
mkdir -p "$BREWLOG_TRACE_DIRECTORY"

cd "$PROJECT_ROOT/coffee"
echo "백엔드를 실행합니다. 기본 주소: http://127.0.0.1:8080"
echo "DB와 외부 서비스는 기존 application-local.yml 및 환경변수 설정을 사용합니다."
exec ./gradlew bootRun "$@"
