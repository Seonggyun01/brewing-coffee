# BrewLog 로컬 실행 준비 가이드

이 문서는 새 PC에서 저장소를 처음 내려받아 BrewLog를 실행하거나, 기존 개발 환경을 다른 PC로 옮길 때 필요한 준비 사항을 정리한다.

## 1. 준비 항목 요약

최초 실행 전에 아래 항목을 준비한다.

| 구분 | 필요 여부 | 용도 |
| --- | --- | --- |
| Git | 필수 | 저장소 내려받기 |
| Java 21 | 필수 | Spring Boot 애플리케이션 실행 |
| MySQL 8.x | 필수 | 원두, 브루잉, 카페 기록 저장 |
| Python 가상환경 | 사진 자동 입력 사용 시 필수 | PaddleOCR 실행 |
| Ollama와 `gemma3:12b` | 사진 자동 입력 및 신규 향미 색상 추천 시 필수 | OCR 결과 구조화 및 색상 추천 |
| Kakao JavaScript 키 | 지도와 장소 검색 사용 시 필수 | 카페 지도와 구매처 검색 |
| Google Vision 서비스 계정 | OCR fallback 사용 시 선택 | PaddleOCR 실패 시 보조 OCR |

## 2. 저장소 내려받기

```bash
git clone <저장소_URL>
cd brewing-coffee
```

이후 명령은 별도 설명이 없으면 저장소 루트인 `brewing-coffee`를 기준으로 한다. Gradle Wrapper가 포함되어 있으므로 Gradle을 별도로 설치할 필요는 없다.

```bash
cd coffee
./gradlew --version
cd ..
```

Windows PowerShell에서는 다음 명령을 사용한다.

```powershell
cd coffee
.\gradlew.bat --version
cd ..
```

출력되는 JVM이 Java 21이 아니라면 `JAVA_HOME`과 IDE의 Project SDK를 Java 21로 변경한다.

## 3. Git에 포함되지 않는 로컬 파일

### 반드시 만들 파일

`coffee/src/main/resources/application-local.yml`은 비밀번호와 API 키를 저장하므로 Git에서 제외되어 있다. 새 PC에서는 직접 만들거나 기존 PC의 파일을 안전한 방법으로 옮긴다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brewlog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false
    username: brewlog
    password: 변경한_DB_비밀번호

brewlog:
  ocr:
    provider: local-best
    paddle:
      python-executable: auto
      fallback-enabled: true
    google-credentials-path: /절대/경로/google-service-account.json

  kakao:
    javascript-key: 카카오_JavaScript_키

  llm:
    provider: ollama
    ollama:
      model-url: http://127.0.0.1:11434/api/generate
      model-id: gemma3:12b
      timeout-seconds: 120
      num-predict: 700
      temperature: 0.0
```

현재 지도와 장소 검색 기능은 Kakao Maps JavaScript SDK를 사용하므로 `javascript-key`가 필요하다. Kakao REST API 키는 현재 실행에 필수 항목이 아니다.

Windows에서는 PaddleOCR Python 경로를 명시한다.

```yaml
brewlog:
  ocr:
    paddle:
      python-executable: C:/projects/brewing-coffee/coffee/.venv-ocr/Scripts/python.exe
```

YAML의 Windows 경로는 역슬래시 대신 `/`를 사용하면 이스케이프 문제를 피할 수 있다.

### Google Vision을 사용할 때만 옮길 파일

서비스 계정 JSON 파일은 Git에 올리지 않는다. 새 PC의 안전한 위치에 복사한 뒤 다음 중 한 가지 방법으로 연결한다.

1. `application-local.yml`의 `brewlog.ocr.google-credentials-path`에 절대 경로 지정
2. `GOOGLE_APPLICATION_CREDENTIALS` 환경변수에 절대 경로 지정

Google Vision을 사용하지 않을 경우 다음처럼 fallback을 끈다.

```yaml
brewlog:
  ocr:
    paddle:
      fallback-enabled: false
```

### 복사하지 않아도 되는 항목

아래 항목은 캐시, 빌드 결과 또는 IDE 설정이므로 새 PC로 옮길 필요가 없다.

- `.idea/`, `.vscode/`, `*.iml`
- `.gradle/`, `.gradle-user-home/`, `build/`, `bin/`, `out/`
- `.venv-ocr/`
- `*.log`, `.DS_Store`
- `docs/image/`, `docs/screenshots/*.zip`

Python 가상환경은 운영체제와 Python 경로에 종속되므로 `.venv-ocr/`을 복사하지 않고 새 PC에서 다시 만든다.

## 4. MySQL 준비

MySQL 8.x를 설치하고 서버를 실행한 뒤 데이터베이스와 사용자를 만든다.

```sql
CREATE DATABASE brewlog
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER 'brewlog'@'localhost' IDENTIFIED BY '변경할_비밀번호';
GRANT ALL PRIVILEGES ON brewlog.* TO 'brewlog'@'localhost';
FLUSH PRIVILEGES;
```

`application-local.yml`의 사용자명과 비밀번호를 위 설정과 일치시킨다. JPA의 기본 `ddl-auto` 값은 `update`이므로 최초 실행 시 테이블이 생성되고 이후 서버를 종료해도 데이터는 유지된다.

### 기존 데이터를 새 PC로 옮길 때

기존 PC에서 덤프를 생성한다.

```bash
mysqldump -u brewlog -p --databases brewlog > brewlog-backup.sql
```

새 PC에서 복원한다.

```bash
mysql -u root -p < brewlog-backup.sql
```

DB 데이터는 프로젝트 폴더나 Git에 저장되지 않으므로 기존 기록이 필요하면 별도로 이전해야 한다.

## 5. PaddleOCR 준비

### macOS 또는 Linux

```bash
cd coffee
python3 -m venv .venv-ocr
source .venv-ocr/bin/activate
python -m pip install --upgrade pip
python -m pip install paddleocr paddlepaddle
```

### Windows PowerShell

```powershell
cd coffee
py -m venv .venv-ocr
.\.venv-ocr\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install paddleocr paddlepaddle
```

PowerShell에서 가상환경 활성화가 차단되면 현재 사용자 범위의 실행 정책을 확인한다.

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

설치 후 프로젝트에 포함된 OCR 스크립트를 이미지로 직접 확인한다.

macOS 또는 Linux:

```bash
.venv-ocr/bin/python scripts/paddle_ocr.py /절대/경로/test-image.jpg
```

Windows:

```powershell
.\.venv-ocr\Scripts\python.exe scripts\paddle_ocr.py C:\absolute\path\test-image.jpg
```

첫 실행에서는 PaddleOCR 모델을 사용자 홈 디렉터리에 내려받기 때문에 시간이 더 걸리고 인터넷 연결이 필요할 수 있다. HEIC 이미지는 Java 전처리 단계에서 OCR이 지원하는 형식으로 변환되므로 웹 업로드 흐름에서는 그대로 사용할 수 있다.

## 6. Ollama 준비

Ollama를 설치한 뒤 프로젝트에서 사용하는 모델을 내려받는다.

```bash
ollama pull gemma3:12b
ollama list
```

Ollama가 백그라운드에서 실행되지 않는 환경이라면 별도 터미널에서 실행한다.

```bash
ollama serve
```

연결 여부는 다음 명령으로 확인한다.

```bash
curl http://127.0.0.1:11434/api/tags
```

Windows용 Ollama 앱이 이미 백그라운드에서 실행 중이라면 `ollama serve`를 중복 실행할 필요가 없다.

## 7. Kakao Maps 준비

Kakao Developers에서 애플리케이션의 JavaScript 키를 확인하고 `application-local.yml`에 입력한다. JavaScript SDK 도메인에는 실제 접속 주소를 등록한다.

```text
http://localhost:8080
http://현재_PC의_내부_IP:8080
```

같은 와이파이의 태블릿이나 휴대폰에서 접속할 때는 두 번째 주소가 필요하다. 공유기 환경에서 내부 IP가 바뀌면 Kakao Developers의 등록 주소도 갱신해야 한다.

## 8. 애플리케이션 실행 순서

평소에는 다음 순서로 실행한다.

1. MySQL 서버 실행
2. Ollama 실행 여부 확인
3. 사진 자동 입력을 사용할 경우 PaddleOCR 가상환경 확인
4. Spring Boot 실행

터미널 실행:

```bash
cd coffee
./gradlew bootRun
```

Windows PowerShell:

```powershell
cd coffee
.\gradlew.bat bootRun
```

IntelliJ에서는 `CoffeeApplication`을 실행해도 된다. 이때 다음 항목을 확인한다.

- Project SDK와 Run Configuration의 JRE: Java 21
- Working directory: 저장소의 `coffee` 디렉터리
- 로컬 설정: `src/main/resources/application-local.yml` 존재
- Windows PaddleOCR: `python-executable`에 `.venv-ocr/Scripts/python.exe` 절대 경로 지정

## 9. 실행 확인

서버가 시작되면 다음 화면을 확인한다.

```text
http://localhost:8080/
http://localhost:8080/coffee-beans
http://localhost:8080/brew-records
http://localhost:8080/cafe-filter-coffees
http://localhost:8080/cafes/map
```

기능별 확인 순서:

1. 원두 목록이 열리면 Spring Boot와 MySQL 연결 정상
2. 카페 지도에 지도가 표시되면 Kakao JavaScript 키와 SDK 도메인 정상
3. 원두 카드 사진 자동 입력이 완료되면 PaddleOCR과 Ollama 정상
4. PaddleOCR 실패 후에도 추출되면 Google Vision fallback 정상

## 10. 테스트

애플리케이션 실행과 별개로 전체 테스트를 수행한다.

```bash
cd coffee
./gradlew clean test
```

Windows:

```powershell
cd coffee
.\gradlew.bat clean test
```

테스트는 H2 인메모리 DB와 Mock provider를 사용하므로 MySQL, PaddleOCR, Ollama가 없어도 실행할 수 있다.

## 11. 자주 발생하는 문제

### `Communications link failure`

- MySQL 서버가 실행 중인지 확인한다.
- `application-local.yml`의 포트, DB 이름, 사용자명, 비밀번호를 확인한다.
- MySQL 사용자에게 `brewlog` 데이터베이스 권한이 있는지 확인한다.

### 카카오맵을 불러오지 못함

- REST API 키가 아닌 JavaScript 키인지 확인한다.
- Kakao Developers의 JavaScript SDK 도메인에 현재 접속 주소가 등록되었는지 확인한다.
- 내부 IP로 접속 중이면 `http://내부_IP:8080`도 등록한다.

### PaddleOCR 실행 파일을 찾지 못함

- `.venv-ocr`을 새 PC에서 생성했는지 확인한다.
- Windows에서는 `brewlog.ocr.paddle.python-executable`을 명시한다.
- IntelliJ의 Working directory가 `coffee`인지 확인한다.

### Ollama 응답 실패 또는 시간 초과

- `ollama list`에 `gemma3:12b`가 있는지 확인한다.
- `http://127.0.0.1:11434/api/tags`가 응답하는지 확인한다.
- 메모리가 부족한 PC에서는 첫 응답과 모델 로딩에 시간이 더 걸릴 수 있다.

### Google Vision `PERMISSION_DENIED`

- 결제 계정과 Vision API 활성화 상태를 확인한다.
- 서비스 계정 JSON 경로와 프로젝트가 일치하는지 확인한다.
- Google Vision을 사용하지 않을 경우 PaddleOCR fallback을 끈다.

## 12. 보안 주의사항

다음 값은 Git에 커밋하지 않는다.

- `application-local.yml`
- DB 비밀번호
- Kakao JavaScript 키
- Google 서비스 계정 JSON
- Hugging Face 등 외부 API 키

실수로 인증 파일을 커밋했다면 파일 삭제만으로 끝내지 말고 해당 키를 즉시 폐기하고 재발급한다.
