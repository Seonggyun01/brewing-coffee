# 개발 서버 실행

프로젝트 루트에서 각각 별도 터미널로 실행합니다.

```bash
./sh/01_back_start.sh
```

```bash
./sh/02_front_start.sh
```

다른 경로에서도 스크립트의 절대 경로로 실행할 수 있습니다. 종료는 각 터미널에서 `Ctrl+C`를 누릅니다.

- 백엔드: 기본 8080 포트. Java 21 및 기존 MySQL 서버가 필요하며, 기존 `application-local.yml`과 환경변수 설정을 사용합니다.
- 프론트: 기본 3000 포트. Node.js와 npm이 필요하며, `node_modules`가 없으면 처음 실행할 때 `npm ci`로 설치합니다.
- 프론트의 백엔드 주소는 기본 `http://127.0.0.1:8080`입니다. 변경하려면 `front/.env`에 `NUXT_BACKEND_URL`을 설정합니다.
- 이미 같은 포트를 사용하는 서버가 있다면 먼저 종료해 주세요.

추가 인자는 각각 Gradle과 Nuxt 개발 명령에 전달됩니다.

```bash
./sh/01_back_start.sh --args='--server.port=8081'
NUXT_BACKEND_URL=http://127.0.0.1:8081 ./sh/02_front_start.sh --port 3001
```
