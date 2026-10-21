# BLANK · 로컬 실행 준비

사진으로 원두 정보를 등록하고 브루잉·카페 경험을 기록하는 서비스입니다. 프론트는 Vue 3 + Nuxt SSR, 백엔드는 Spring Boot입니다.

이 문서는 **프로젝트 최상위 `coffee_project/` 기준**으로 clone 후 직접 준비해야 하는 파일과 로컬 실행 순서를 설명합니다.

## 폴더 구성

```text
coffee_project/                    # clone한 저장소 루트
├── README.md                     # 프로젝트 소개·목표
├── SETUP.md                      # 개인 설정·실행 준비
├── front/                        # Vue 3 / Nuxt SSR
├── coffee/                       # Spring Boot와 백엔드 README
├── legacy-front/                 # 기존 Thymeleaf templates / static
├── docs/                         # 설계 문서와 아키텍처 이미지
├── sh/                           # 개발 서버 실행 스크립트
└── logs/                         # 로컬 생성 로그 (Git 제외)
```

전체 프로젝트를 내려받습니다.

```bash
git clone git@github.com:Seonggyun01/brewing-coffee.git coffee_project
cd coffee_project
```

## clone 후 추가해야 하는 파일

아래 경로는 모두 `coffee_project/` 기준입니다. 비밀 설정은 Git에서 제외되어 있으므로 clone만으로 생성되지 않습니다. 환경변수로 설정을 전부 전달한다면 로컬 설정 파일은 생략할 수 있습니다.

| 파일·경로 | 필요한 경우 | 준비 방법 |
| --- | --- | --- |
| `coffee/src/main/resources/application-local.yml` | DB·카카오·Groq의 개인 설정을 파일로 관리할 때 | 아래 예시로 새로 만들고 본인 설정 입력 |
| `front/.env` | 새 프론트의 백엔드 주소 설정 | `cp front/.env.example front/.env` |
| Google 서비스 계정 JSON | Google Vision OCR 또는 로컬 OCR의 Vision 대체 처리를 사용할 때 | Google Cloud에서 발급 후 프로젝트 밖에 보관하고 절대 경로 설정 |

`application-local.yml`이 정확한 파일명입니다. `application.local.properties`, `application.local.poperties`는 현재 자동 import 대상이 아닙니다.

기존 DB의 기록도 Git에 들어 있지 않습니다. 새 DB로 시작하거나, 기존 데이터를 쓰려면 별도의 DB 백업을 복원해야 합니다.

### 1. 백엔드 로컬 설정

`coffee/src/main/resources/application-local.yml`을 다음 형태로 추가합니다. 예시 문구는 실제 값으로 교체합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brewlog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false
    username: brewlog
    password: "본인의_DB_비밀번호"
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-secret: ${brewlog.kakao.client-secret}

brewlog:
  kakao:
    rest-api-key: "카카오_REST_API_키"
    javascript-key: "카카오_JavaScript_키"
    client-secret: "카카오_로그인_Client_Secret"
  llm:
    provider: groq
    groq:
      api-key: "Groq에서_발급받은_API_키"
      model-id: qwen/qwen3.8-27b
      max-tokens: 768
  ocr:
    provider: local-best
    paddle:
      fallback-enabled: false
```

기본 설정은 이 파일을 `optional:classpath:application-local.yml`로 읽습니다. 카카오 로그인 Client ID는 `brewlog.kakao.rest-api-key`를 사용합니다. 동일한 환경변수를 별도로 설정했다면 그 값도 확인하세요.

위 OCR 예시는 Google 인증 파일 없이 로컬 OCR만 사용하는 구성입니다. PaddleOCR 실행 환경을 별도로 설치해야 사진 분석이 동작합니다. 설치 절차는 [백엔드 로컬 준비 가이드](docs/LOCAL_SETUP_GUIDE.md)를 참고하세요. Groq를 사용하므로 LLM용 Ollama 설치는 필요하지 않습니다.

### 2. API 키를 발급받는 사이트

| 용도 | 발급 사이트 | 설정 |
| --- | --- | --- |
| OCR 텍스트 → 원두 JSON | [Groq API Keys](https://console.groq.com/keys) | `brewlog.llm.groq.api-key` 또는 `GROQ_API_KEY` |
| 카카오 로그인·장소 검색 | [Kakao Developers](https://developers.kakao.com/) | REST API 키, 로그인 Client Secret |
| 지도 표시 | [Kakao Developers](https://developers.kakao.com/) | JavaScript 키 |
| Google Vision OCR | [Google Cloud Console](https://console.cloud.google.com/) | 서비스 계정 JSON 경로 |

현재 원두 파싱 모델은 **Groq의 `qwen/qwen3.8-27b`**입니다. OpenRouter 키와 Groq 키는 서로 다릅니다. API 키는 프론트에 넣지 않고 백엔드에서만 사용합니다.

카카오 콘솔에도 실제 접속 주소를 등록해야 합니다.

- 새 프론트 로그인 Redirect URI: `http://127.0.0.1:3000/login/oauth2/code/kakao`
- `localhost`로 접속한다면: `http://localhost:3000/login/oauth2/code/kakao`
- 지도 JavaScript SDK 도메인: 사용하는 주소에 맞춰 `http://127.0.0.1:3000`, `http://localhost:3000` 등록
- 백엔드의 기존 화면으로 직접 로그인할 때는 해당 `8080` 주소의 Redirect URI도 별도 등록

### 3. Google Vision 인증 파일 — 사용하는 경우만

서비스 계정 JSON은 새 PC에 별도로 준비해야 합니다. 기존 개발자의 인증 파일을 그대로 가정하거나 README에 키 내용을 복사하지 않습니다.

예를 들어 JSON을 프로젝트 밖의 안전한 경로에 보관한 뒤, 백엔드 실행 터미널에서 지정합니다.

```bash
export GOOGLE_APPLICATION_CREDENTIALS='/본인의/절대경로/google-service-account.json'
```

로컬 OCR 실패 시 Vision을 사용하려면 로컬 YAML에서 `brewlog.ocr.paddle.fallback-enabled: true`로 바꿉니다. Vision만 사용하려면 `brewlog.ocr.provider: google-vision`으로 지정합니다. Google Cloud 프로젝트에서 Vision API를 활성화하고 해당 서비스 계정의 권한·결제 설정도 준비해야 합니다.

환경변수 대신 `brewlog.ocr.google-credentials-path`에 JSON의 절대 경로를 넣을 수도 있습니다. 다른 PC에서는 경로를 그 PC에 맞게 수정해야 합니다.

### 4. 프론트 환경 파일

프로젝트 최상위에서 실행합니다.

```bash
cp front/.env.example front/.env
```

파일 내용:

```dotenv
NUXT_BACKEND_URL=http://127.0.0.1:8080
```

백엔드 포트를 바꿨다면 이 주소도 맞춥니다. 이 값은 Nuxt 서버가 백엔드에 연결할 때 사용합니다. Groq·카카오 비밀 키는 넣지 않습니다.

## 별도 파일을 복사하지 않아도 되는 항목

| 항목 | 준비 방식 |
| --- | --- |
| `front/node_modules/` | `npm ci` 또는 프론트 시작 스크립트가 설치 |
| `front/.nuxt/`, `front/.output/` | 개발 실행·빌드 시 생성 |
| Gradle 캐시·`coffee/build/` | Gradle 실행 시 생성 |
| `coffee/.venv-ocr/` | 로컬 OCR 설치 절차로 새 PC에서 생성. 기존 가상환경을 복사하지 않음 |
| `logs/*.log`, `logs/*.jsonl` | 백엔드 실행·사진 분석 시 생성 |

## AI API 강제 사용 한도

백엔드는 외부 요청 전에 서비스 전체 예산을 예약하고, 한도 초과 시 HTTP 429로 차단합니다.

| 환경변수 | 기본값 | 기준 |
| --- | --- | --- |
| `BREWLOG_GROQ_DAILY_TOKENS` | `200000` | Groq 채팅·향미 색상 합산 토큰/일 |
| `BREWLOG_OPENROUTER_DAILY_REQUESTS` | `50` | OpenRouter 채팅·임베딩 합산 요청/일 |
| `BREWLOG_VISION_MONTHLY_UNITS` | `1000` | Google Vision OCR/월 |
| `BREWLOG_AI_MAX_CONCURRENT_REQUESTS` | `2` | 백엔드 인스턴스별 외부 호출 및 사진 분석 동시 처리 수 |
| `BREWLOG_API_USAGE_FILE` | `../logs/api-usage.jsonl` | 기존 사용량과 예약·정산 저장 경로 |

일간·월간 초기화는 기존 집계와 같은 UTC 기준입니다. UTC 자정은 한국 시간 오전 9시입니다.
Groq는 입력 UTF-8 바이트 수 + 최대 출력 토큰 + 템플릿 여유량 1,024로 보수적으로 예약하고 실제 응답 토큰으로 정산합니다.
남은 예산이 예약량보다 작으면 실제 응답이 더 짧을 수 있어도 요청하지 않습니다.
실패·타임아웃·사용량 누락·프로세스 종료 시에는 예약량을 반환하지 않아 재시작으로 제한을 우회하지 않습니다.
사용량 파일 손상이나 저장 실패 시 HTTP 503으로 외부 호출을 차단합니다.
동일 파일을 사용하는 프로세스 사이에는 파일 잠금으로 예산 확인과 예약을 직렬화합니다.

배포 시 이 파일은 영구 볼륨에 보존하고 로그 정리 대상으로 삭제하지 마세요. 다른 서버가 별도 파일을 쓰면 각각의 예산이 적용됩니다.
이는 앱에서 사용하는 키의 호출 제한이며, 같은 키를 다른 프로그램에서 쓰는 사용량이나 제공사 자체 요금 한도를 대신하지 않습니다.
현재 제한 대상은 Groq, OpenRouter, Google Vision입니다. 다른 공급자를 추가하면 호출 전 예산 검증도 추가해야 합니다.

## 로컬 실행 순서

Java 21, MySQL, 프론트가 지원하는 Node.js와 npm을 준비합니다. Node 버전과 SSR 실행 설명은 [프론트 README](front/README.md)를 확인하세요.

1. MySQL에 `brewlog` 데이터베이스와 로컬 YAML에 적은 계정·권한을 준비합니다. DB 준비 SQL은 [백엔드 README](coffee/README.md#mysql-준비)를 참고합니다.
2. 위의 백엔드 설정 파일과 프론트 `.env`를 추가합니다.
3. 사진 OCR을 사용할 경우 PaddleOCR 또는 Google Vision 인증을 준비합니다.
4. 프로젝트 최상위에서 별도 터미널 두 개로 실행합니다.

백엔드:

```bash
./sh/01_back_start.sh
```

프론트:

```bash
./sh/02_front_start.sh
```

- 프론트 접속: `http://127.0.0.1:3000`
- 백엔드 기본 주소: `http://127.0.0.1:8080`
- 설정 변경 후에는 해당 서버를 재시작합니다.

백엔드만 실행하려면 저장소의 `coffee/`에서 `./gradlew bootRun`을 실행합니다.

## 추가 문서

- [백엔드: Groq 모델·토큰 사용량·무료 한도](coffee/README.md#원두-카드-ocr--llm-파싱-설정)
- [프론트 실행과 인증 연결](front/README.md)
- [개발 서버 실행 스크립트](sh/README.md)

실제 API 키, 서비스 계정 JSON, DB 비밀번호가 들어 있는 로컬 파일은 커밋하지 않습니다.
