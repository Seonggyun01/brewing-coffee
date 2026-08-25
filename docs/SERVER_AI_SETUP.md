# BLANK 서버 외부 AI 설정

서버에서는 로컬 모델을 실행하지 않고 다음 외부 서비스를 사용한다.

```text
원두 카드 이미지
  -> Google Vision OCR
  -> OpenRouter
       고정 모델: minimax/minimax-m3:free
  -> 규칙 검증 및 원두 등록 폼 반영
```

Google Vision OCR과 OpenRouter LLM 모두 다른 provider 또는 모델 fallback을 두지 않는다.

LLM 요청에는 원두 카드 이미지나 DB 데이터를 포함하지 않는다. Google Vision이 반환한 OCR 원문에 고정된 JSON 변환 지시문만 붙여 MiniMax M3에 전달하며, OCR 전처리 후보값은 응답 검증과 정규화에만 사용한다.

## 1. 서버 프로필 활성화

배포 서버에서 다음 환경변수를 설정한다.

```bash
export SPRING_PROFILES_ACTIVE=server
```

`server` 프로필은 `coffee/src/main/resources/application-server.yml`을 읽어 다음 provider를 선택한다.

```yaml
brewlog:
  ocr:
    provider: google-vision
  llm:
    provider: openrouter
```

이 프로필에서는 PaddleOCR 프로세스를 실행하지 않으므로 서버에 Python, PaddleOCR 모델, PaddleOCR 가상환경이 필요하지 않다. Ollama도 설치할 필요가 없다.

## 2. Google Vision OCR 설정

Google Cloud에서 다음 항목을 준비한다.

1. 결제가 연결된 Google Cloud 프로젝트
2. 활성화된 Cloud Vision API
3. Vision API를 호출할 권한이 있는 서비스 계정
4. 서비스 계정 JSON 인증 파일

인증 파일은 저장소 외부의 읽기 제한된 경로에 둔다.

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/opt/brewlog/secrets/google-vision-service-account.json
```

또는 애플리케이션 설정으로 경로를 전달할 수 있다.

```bash
export BREWLOG_GOOGLE_CREDENTIALS_PATH=/opt/brewlog/secrets/google-vision-service-account.json
```

현재 애플리케이션은 `BREWLOG_GOOGLE_CREDENTIALS_PATH`를 `brewlog.ocr.google-credentials-path`로 매핑한다.

```yaml
brewlog:
  ocr:
    google-credentials-path: ${BREWLOG_GOOGLE_CREDENTIALS_PATH:}
```

`GOOGLE_APPLICATION_CREDENTIALS`를 사용하면 위 YAML 추가 설정 없이 Google 인증 라이브러리가 자동으로 읽는다.

### OCR fallback 정책

- `brewlog.ocr.provider=google-vision`은 Google Vision 구현체만 생성한다.
- Google Vision 인증, 결제, 네트워크 또는 API 호출이 실패하면 사용자에게 OCR 오류를 반환한다.
- PaddleOCR이나 다른 OCR 서비스로 자동 전환하지 않는다.

## 3. OpenRouter 설정

[OpenRouter](https://openrouter.ai/)에서 API 키를 발급하고 서버 환경변수로 등록한다.

```bash
export OPENROUTER_API_KEY=발급받은_API_키
```

기본 모델 설정은 다음과 같다.

| 모델 ID | 역할 |
| --- | --- |
| `minimax/minimax-m3:free` | OCR 정보 구조화 및 향미 색상 추천 |

OpenRouter 요청에는 단일 `model` 값만 전달한다. 모델의 rate limit이나 서비스 장애가 발생하면 다른 LLM으로 자동 전환하지 않고 해당 요청을 실패 처리한다.

무료 모델은 제공 기간, 모델 ID, 요청 제한이 바뀔 수 있다. 코드 변경 없이 교체할 수 있도록 다음 환경변수를 제공한다.

```bash
export BREWLOG_OPENROUTER_MODEL_ID=minimax/minimax-m3:free
export BREWLOG_OPENROUTER_TIMEOUT_SECONDS=60
export BREWLOG_OPENROUTER_MAX_TOKENS=700
export BREWLOG_OPENROUTER_TEMPERATURE=0.0
```

선택적으로 OpenRouter 대시보드에 애플리케이션 출처를 표시할 수 있다.

```bash
export BREWLOG_OPENROUTER_SITE_URL=https://brewlog.example.com
export BREWLOG_OPENROUTER_APP_NAME=BLANK
```

무료 endpoint는 제공자 정책에 따라 입력과 출력이 저장되거나 모델 개선에 사용될 수 있다. 개인 메모에 민감한 정보가 포함되지 않도록 주의한다.

## 4. 필수 환경변수 예시

```bash
export SPRING_PROFILES_ACTIVE=server

export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=brewlog
export DB_USERNAME=brewlog
export DB_PASSWORD=변경한_DB_비밀번호
export DB_TIMEZONE=Asia/Seoul

export GOOGLE_APPLICATION_CREDENTIALS=/opt/brewlog/secrets/google-vision-service-account.json
export OPENROUTER_API_KEY=발급받은_OpenRouter_API_키

export BREWLOG_KAKAO_JAVASCRIPT_KEY=카카오_JavaScript_키
```

환경변수는 셸 히스토리나 공개 저장소에 남기지 않고 서버의 Secret Manager, systemd `EnvironmentFile`, 컨테이너 secret 등의 방법으로 주입한다.

## 5. 실행

```bash
cd coffee
./gradlew bootRun
```

JAR로 실행할 경우:

```bash
./gradlew clean bootJar
java -jar build/libs/blank-0.0.1-SNAPSHOT.jar
```

## 6. 동작 확인

서버 로그에서 다음 순서가 나타나는지 확인한다.

1. Google Vision OCR 요청 시작과 완료
2. OpenRouter LLM 매핑 요청
3. 응답 JSON 파싱 및 규칙 검증
4. 원두 등록 폼 자동 입력

원두 카드 사진을 업로드했을 때 프론트 진행 상태는 다음 백엔드 단계와 연결된다.

```text
OCR 추출 중 -> LLM 원두 정보 정리 중 -> 결과 파싱 중
```

## 7. 실패 시 확인 순서

### OCR이 바로 실패함

- `GOOGLE_APPLICATION_CREDENTIALS` 경로와 파일 권한 확인
- 서비스 계정 프로젝트와 Vision API 프로젝트 일치 여부 확인
- Cloud Vision API와 결제 활성화 여부 확인
- 서버에서 Google API로 나가는 HTTPS 연결 확인

### LLM 결과가 비어 있음

- `OPENROUTER_API_KEY` 설정 여부 확인
- OpenRouter 대시보드의 rate limit과 요청 기록 확인
- 두 무료 모델이 현재 제공 중인지 확인
- 필요하면 모델 ID 환경변수만 현재 사용 가능한 무료 모델로 변경

OpenRouter가 실패하면 애플리케이션은 OCR 원문에 대한 기존 규칙 기반 파서를 사용한다. 신규 향미 색상 추천이 실패하면 내장된 향미 계열 색상표를 사용하므로 원두 저장 자체는 계속할 수 있다.

## 8. 로컬 모드로 되돌리기

서버가 아닌 기존 로컬 PaddleOCR과 Ollama 구성을 사용할 때는 `server` 프로필을 활성화하지 않거나 다음 값을 지정한다.

```bash
export BREWLOG_OCR_PROVIDER=local-best
export BREWLOG_LLM_PROVIDER=ollama
```

로컬 설치 방법은 [로컬 실행 준비 가이드](LOCAL_SETUP_GUIDE.md)를 참고한다.
