# Google Vision OCR 구현 명세서 v1

## 1. 문서 목적

본 문서는 BrewLog 프로젝트에서 **원두 카드 이미지 업로드 후 Google Cloud Vision OCR을 호출하여 텍스트를 추출하는 기능**을 구현하기 위한 명세서이다.

현재 단계의 핵심 목표는 다음과 같다.

- 사용자가 업로드한 원두 카드 이미지를 서버에서 수신한다.
- 서버가 이미지를 Google Cloud Vision API로 전송한다.
- Vision API의 OCR 결과 텍스트를 받아온다.
- 추출된 원문 텍스트를 기존 원두 등록 흐름에 연결한다.
- 추후 파서 기반 추출 또는 LLM 기반 추출 중 어떤 방식을 사용할지 성능 비교가 가능하도록 구조를 분리한다.

---

## 2. 현재 결정 사항

| 항목 | 결정 |
|---|---|
| OCR 서비스 | Google Cloud Vision API |
| 인증 방식 | Application Default Credentials 우선 사용 |
| 로컬 인증 | `GOOGLE_APPLICATION_CREDENTIALS` 환경 변수 사용 |
| OCR 결과 저장 | OCR 결과는 바로 DB에 저장하지 않음 |
| 사용자 흐름 | OCR 결과를 폼 후보값으로 보여준 뒤 사용자가 확인 후 저장 |
| 값 추출 방식 | 1차 구현에서는 raw text 추출까지만 확실히 구현 |
| 후속 추출 방식 | 파서 기반 또는 LLM 기반 중 성능을 보고 결정 |
| 구현 방향 | Controller에 외부 API 호출 로직을 직접 넣지 않음 |
| 민감 정보 | 서비스 계정 JSON, API Key, secret 설정 파일은 Git에 올리지 않음 |

---

## 3. 이번 구현 범위

### 3.1 포함 범위

이번 작업에서 구현할 내용은 다음과 같다.

1. Google Vision OCR 호출을 담당하는 Service 또는 Client 구현
2. 업로드된 `MultipartFile` 이미지를 Vision API 요청 형식으로 변환
3. Vision API 응답에서 전체 OCR 텍스트 추출
4. 기존 Mock OCR 구조가 있다면 실제 OCR 구현체와 교체 가능하도록 유지
5. OCR 성공/실패 결과를 Controller 또는 상위 Service에 반환
6. OCR 결과를 원두 등록 폼에 후보값으로 전달
7. OCR 실패 시 사용자에게 에러 메시지 표시
8. Google Vision 의존성 추가
9. 인증 파일 및 secret 설정 파일 `.gitignore` 처리

### 3.2 제외 범위

이번 작업에서 하지 않을 내용은 다음과 같다.

- OCR 결과를 완벽하게 필드별로 파싱하는 기능
- LLM API 호출 기능
- DB 스키마 변경
- 원두 도메인 구조 변경
- 로그인/회원 기능 수정
- 전체 UI 디자인 변경
- 원산지 지도 기능 수정
- 브루잉 기록 기능 수정
- 배포 환경 Secret Manager 연동
- 대량 이미지 처리 또는 비동기 큐 처리

---

## 4. 권장 문서 참조 규칙

Codex는 이번 작업에서 모든 문서를 읽지 않는다.

### 먼저 확인할 파일 후보

현재 프로젝트 구조에 맞게 실제 파일명을 찾아 확인한다.

- 원두 등록 Controller
- 원두 등록 Form 또는 DTO
- 원두 등록 Thymeleaf 템플릿
- 기존 Mock OCR Service 또는 OCR 관련 클래스
- `build.gradle`
- `application.yml`
- `.gitignore`
- `AGENTS.md`

### 문서가 필요할 때만 확인할 명세

- `docs/DOCS_INDEX.md`
- `docs/specs/coffee_bean_card_image_extraction_spec_v1.md`

### 이번 작업에서 읽지 않아도 되는 문서

- 브루잉 기록 UX 명세
- 원산지 지도 메인 페이지 명세
- 전체 프론트엔드 디자인 명세
- README 전체 문서

---

## 5. 사용자 흐름

### 5.1 기본 흐름

1. 사용자가 원두 등록 페이지에 접근한다.
2. 사용자가 원두 카드 이미지를 업로드한다.
3. 서버가 이미지를 검증한다.
4. 서버가 Google Vision OCR Service를 호출한다.
5. Google Vision API가 이미지 내 텍스트를 인식한다.
6. 서버가 OCR raw text를 받는다.
7. 서버가 raw text를 화면에 표시하거나, 현재 가능한 필드 후보값으로 변환한다.
8. 원두 등록 폼으로 다시 이동한다.
9. 사용자는 자동 입력된 값 또는 OCR 원문을 확인한다.
10. 사용자가 직접 수정 후 최종 저장한다.

### 5.2 실패 흐름

1. 사용자가 이미지를 업로드한다.
2. 이미지 파일이 비어 있거나 지원하지 않는 형식이다.
3. 서버는 Google Vision API를 호출하지 않는다.
4. 등록 폼으로 돌아가 에러 메시지를 표시한다.

또는

1. 이미지 업로드는 성공한다.
2. Google Vision API 호출 중 인증 오류, 네트워크 오류, API 오류가 발생한다.
3. 서버는 예외를 잡아 사용자 친화적인 메시지를 표시한다.
4. 등록 폼으로 돌아간다.
5. 기존 사용자가 입력한 값은 최대한 유지한다.

---

## 6. 권장 아키텍처

### 6.1 기본 구조

```text
Controller
  ↓
Application Service / OCR Facade
  ↓
OcrService interface
  ↓
GoogleVisionOcrService
  ↓
Google Cloud Vision API
```

### 6.2 역할 분리

| 계층 | 역할 |
|---|---|
| Controller | 이미지 업로드 요청 수신, Form/Model 처리, View 반환 |
| OCR Facade 또는 Application Service | OCR 호출 흐름 조합, 실패 처리, Form 반영 처리 |
| OcrService interface | OCR 구현체 교체를 위한 추상화 |
| GoogleVisionOcrService | Google Vision API 호출 |
| MockOcrService | 개발/테스트용 OCR 대체 구현 |
| Parser 또는 Extractor | 추후 raw text에서 원두 필드 추출 |
| Thymeleaf Template | 업로드 버튼, OCR 결과 메시지, 후보값 표시 |

---

## 7. OCR Service 인터페이스 설계

기존 OCR 구조가 있다면 기존 이름을 우선 유지한다. 없다면 다음 구조를 권장한다.

```java
public interface OcrService {
    OcrResult extractText(MultipartFile imageFile);
}
```

또는 파일 변환 책임을 분리하고 싶다면 다음 방식도 가능하다.

```java
public interface OcrService {
    OcrResult extractText(byte[] imageBytes, String originalFilename, String contentType);
}
```

### 7.1 권장 DTO

```java
public class OcrResult {
    private final boolean success;
    private final String rawText;
    private final String errorMessage;

    public static OcrResult success(String rawText) {
        return new OcrResult(true, rawText, null);
    }

    public static OcrResult failure(String errorMessage) {
        return new OcrResult(false, null, errorMessage);
    }
}
```

실제 프로젝트 스타일에 맞춰 record, Lombok, 일반 class 중 하나를 선택한다.

---

## 8. Google Vision OCR 구현 방향

### 8.1 구현 클래스 예시

```text
GoogleVisionOcrService
```

역할:

- `MultipartFile`을 byte array로 변환
- Vision API 요청 생성
- Text Detection 또는 Document Text Detection 호출
- 응답에서 OCR 전체 텍스트 추출
- 예외 발생 시 `OcrResult.failure()` 반환 또는 도메인 예외 발생

### 8.2 OCR 기능 선택

원두 카드 이미지처럼 일반 이미지 안의 텍스트를 추출하는 경우 우선 다음 중 하나를 사용한다.

| 기능 | 용도 |
|---|---|
| TEXT_DETECTION | 일반 이미지 내 텍스트 추출 |
| DOCUMENT_TEXT_DETECTION | 문서 형태, 긴 텍스트, 구조화된 OCR에 유리 |

초기 구현에서는 원두 카드 이미지가 짧은 텍스트 중심이면 `TEXT_DETECTION`으로 시작한다.  
추후 실제 OCR 품질을 보고 `DOCUMENT_TEXT_DETECTION`으로 변경할 수 있도록 OCR Service 내부 구현에만 종속되게 한다.

---

## 9. 인증 정보 관리

### 9.1 로컬 개발

로컬에서는 서비스 계정 JSON 파일을 사용한다.

예시:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/local/path/google-vision-service-account.json
```

Windows PowerShell 예시:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\google-vision-service-account.json"
```

### 9.2 코드 작성 규칙

금지:

```java
String keyPath = "/Users/me/google-key.json";
```

금지:

```yaml
google:
  credentials: "{ json content ... }"
```

권장:

- Google Cloud Client Library의 Application Default Credentials 사용
- 로컬에서는 `GOOGLE_APPLICATION_CREDENTIALS` 환경 변수 사용
- 배포 환경에서는 추후 Secret Manager, 환경 변수, VM/Cloud Run 서비스 계정 방식 검토

### 9.3 `.gitignore` 추가 권장

```gitignore
# Google Cloud credentials
*google-vision*.json
*service-account*.json
*gcp*.json
application-secret.yml
.env
```

---

## 10. Gradle 의존성

`build.gradle`에 Google Cloud Vision Java Client 의존성을 추가한다.

버전은 현재 프로젝트 Gradle/Spring Boot 환경과 충돌하지 않도록 확인한다.  
가능하면 Google Cloud Libraries BOM을 사용하는 방식을 권장한다.

예시:

```gradle
dependencies {
    implementation platform("com.google.cloud:libraries-bom:<version>")
    implementation "com.google.cloud:google-cloud-vision"
}
```

주의:

- `<version>`은 Codex가 현재 최신 안정 버전 또는 프로젝트와 호환되는 버전으로 확인 후 적용한다.
- 이미 BOM을 사용 중이라면 중복 선언하지 않는다.
- 의존성 추가 후 `./gradlew dependencies` 또는 `./gradlew build`로 충돌 여부를 확인한다.

---

## 11. 이미지 업로드 검증

Google Vision API 호출 전에 다음을 검증한다.

| 검증 항목 | 처리 |
|---|---|
| 파일 없음 | OCR 호출하지 않고 에러 메시지 |
| 빈 파일 | OCR 호출하지 않고 에러 메시지 |
| 파일 크기 초과 | OCR 호출하지 않고 에러 메시지 |
| 이미지가 아닌 MIME 타입 | OCR 호출하지 않고 에러 메시지 |
| 읽기 실패 | 사용자 친화적 에러 메시지 |
| OCR 결과 빈 문자열 | “텍스트를 인식하지 못했습니다” 메시지 |

권장 허용 MIME 타입:

```text
image/jpeg
image/png
image/webp
```

프로젝트 정책에 따라 webp는 제외해도 된다.

---

## 12. Controller 처리 규칙

Controller는 다음 책임만 가진다.

- 업로드 파일 수신
- Form 또는 Model 준비
- OCR Service 호출
- 결과를 View에 전달
- 성공/실패 메시지 전달

Controller에 넣지 말아야 할 것:

- Google Vision API 요청 생성
- Vision API 응답 파싱
- OCR raw text 상세 후처리
- 인증 정보 처리
- 복잡한 필드 추출 로직

---

## 13. 화면 처리 규칙

원두 등록 화면에는 다음 요소를 둘 수 있다.

### 13.1 업로드 영역

- 원두 카드 이미지 업로드 input
- OCR 실행 버튼
- “사진에서 정보 추출” 버튼 문구 권장

### 13.2 OCR 결과 표시

OCR 성공 시:

- “이미지에서 텍스트를 추출했습니다.”
- OCR raw text 확인 영역
- 추후 파싱된 후보값이 있으면 각 필드에 자동 입력

OCR 실패 시:

- “이미지에서 텍스트를 읽지 못했습니다. 직접 입력해주세요.”
- 기존 입력값 유지

### 13.3 UX 원칙

- OCR 결과는 자동 저장하지 않는다.
- 사용자가 확인 후 저장 버튼을 눌러야 DB에 저장된다.
- OCR raw text는 디버깅과 사용자 확인을 위해 임시로 화면에 보여줄 수 있다.
- 운영 단계에서 raw text 표시 여부는 UX를 보고 조정한다.

---

## 14. 파서와 LLM 확장 설계

현재는 Google Vision OCR로 raw text를 추출하는 단계까지만 구현한다.  
이후 raw text에서 원두명을 찾을지, 로스터리명을 찾을지, 산지/품종/가공방식/노트 등을 추출할지는 별도 모듈로 분리한다.

### 14.1 확장 인터페이스 권장

```java
public interface CoffeeBeanInfoExtractor {
    CoffeeBeanExtractionResult extract(String rawText);
}
```

### 14.2 구현 후보

```text
RuleBasedCoffeeBeanInfoExtractor
LlmCoffeeBeanInfoExtractor
```

### 14.3 초기 단계 구현

초기에는 다음 중 하나를 선택한다.

#### 선택 A: No-op Extractor

raw text만 반환하고 필드 추출은 하지 않는다.

```text
OCR raw text → 화면 표시 → 사용자가 직접 입력
```

#### 선택 B: 단순 파서

키워드 기반으로 일부 필드만 추출한다.

```text
OCR raw text → 로스터리/원두명/원산지 정도만 후보 추출
```

현재 명세 기준으로는 **선택 A 또는 최소한의 선택 B**를 권장한다.

---

## 15. 파서 vs LLM 결정 기준

추후 성능 비교 시 다음 기준으로 결정한다.

| 기준 | 파서 방식 | LLM 방식 |
|---|---|---|
| 비용 | 낮음 | 상대적으로 높음 |
| 속도 | 빠름 | 상대적으로 느릴 수 있음 |
| 구현 난이도 | 규칙 설계 필요 | 프롬프트/응답 파싱 필요 |
| 다양한 카드 레이아웃 대응 | 약할 수 있음 | 강할 수 있음 |
| 예측 가능성 | 높음 | 낮을 수 있음 |
| 개인정보/보안 고려 | 상대적으로 단순 | 외부 LLM 전송 정책 필요 |
| 유지보수 | 카드 형식 증가 시 규칙 증가 | 모델/프롬프트 관리 필요 |

### 15.1 평가 데이터 구성

실제 원두 카드 이미지 20~50장을 수집하여 다음 값을 사람이 정답으로 작성한다.

- 로스터리명
- 원두명
- 원산지
- 지역
- 농장
- 품종
- 가공방식
- 로스팅 날짜
- 컵노트
- 고도
- 중량
- 가격

### 15.2 평가 지표

| 지표 | 설명 |
|---|---|
| 필드별 정확도 | 각 필드가 맞게 추출되었는지 |
| 전체 카드 정확도 | 한 이미지에서 주요 필드가 모두 맞는지 |
| 누락률 | 있어야 할 값이 비어 있는 비율 |
| 오추출률 | 잘못된 값을 채운 비율 |
| 평균 응답 시간 | OCR 이후 추출 처리 시간 |
| 요청당 비용 | OCR + LLM 사용 시 비용 |
| 사용자 수정량 | 사용자가 최종 저장 전에 수정한 필드 수 |

### 15.3 의사결정 기준

- 카드 형식이 제한적이고 비슷하면 파서 우선
- 카드 형식이 다양하고 텍스트 구조가 불규칙하면 LLM 검토
- 비용이 중요하면 파서 우선
- 빠른 MVP가 중요하면 단순 파서 또는 LLM 후보 추출을 실험
- 운영에서는 파서 + LLM fallback 조합도 고려 가능

---

## 16. 예외 처리

### 16.1 예외 유형

| 상황 | 사용자 메시지 |
|---|---|
| 파일 없음 | 이미지를 선택해주세요. |
| 지원하지 않는 파일 형식 | JPG, PNG 형식의 이미지만 업로드할 수 있습니다. |
| OCR 인증 실패 | OCR 서비스 인증에 실패했습니다. 관리자에게 문의해주세요. |
| OCR API 호출 실패 | 이미지 분석 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요. |
| OCR 결과 없음 | 이미지에서 텍스트를 찾지 못했습니다. 직접 입력해주세요. |
| 파일 읽기 실패 | 이미지 파일을 읽을 수 없습니다. 다시 업로드해주세요. |

### 16.2 로그 정책

서버 로그에는 다음을 남긴다.

- OCR 호출 시작/종료
- 파일명
- 파일 크기
- MIME 타입
- 실패 원인
- Google Vision API 예외 메시지

로그에 남기지 말아야 할 것:

- 서비스 계정 JSON 내용
- API Key
- 전체 인증 경로가 민감할 경우 해당 경로
- 사용자의 민감한 개인정보

---

## 17. 설정 파일 예시

`application.yml`에는 민감하지 않은 설정만 둘 수 있다.

```yaml
ocr:
  provider: google-vision
  max-file-size-mb: 5
```

서비스 계정 JSON 경로는 코드나 yml에 고정하지 않고, 로컬에서는 환경 변수로 관리한다.

---

## 18. 테스트 계획

### 18.1 단위 테스트

- 빈 파일이면 OCR 호출하지 않는지
- 이미지가 아닌 파일이면 OCR 호출하지 않는지
- OCR 결과가 빈 문자열이면 실패 결과로 처리하는지
- Google Vision 예외 발생 시 사용자 메시지로 변환되는지
- MockOcrService와 GoogleVisionOcrService가 같은 인터페이스로 교체 가능한지

### 18.2 통합 테스트

- 실제 이미지 업로드 요청
- OCR 성공 후 등록 폼으로 복귀
- raw text가 Model에 담기는지
- OCR 실패 후 기존 폼 값이 유지되는지
- 인증 정보가 없을 때 적절히 실패하는지

### 18.3 수동 확인

1. `GOOGLE_APPLICATION_CREDENTIALS` 환경 변수 설정
2. 애플리케이션 실행
3. 원두 등록 페이지 접속
4. 원두 카드 이미지 업로드
5. OCR 실행
6. OCR raw text 표시 확인
7. 사용자가 수정 후 저장
8. DB에 최종 저장값 확인

---

## 19. Codex 구현 지시사항

Codex는 다음 순서로 작업한다.

1. `AGENTS.md` 확인
2. 이번 작업이 Google Vision OCR 작업임을 인식
3. `docs/DOCS_INDEX.md`가 있으면 확인
4. 필요 시 `docs/specs/coffee_bean_card_image_extraction_spec_v1.md`만 확인
5. 원두 등록 Controller/Form/Template/OCR Service 구조 확인
6. 기존 Mock OCR 구조 확인
7. 구현 계획 작성
8. 사용자 확인 후 코드 수정
9. Google Vision 의존성 추가
10. GoogleVisionOcrService 구현
11. 기존 OCR 흐름에 연결
12. `.gitignore` 보강
13. 빌드 또는 테스트 실행
14. 변경 파일과 확인 방법 요약

---

## 20. Codex 금지사항

Codex는 이번 작업에서 다음을 하지 않는다.

- 전체 명세서 무작위 탐색
- README 전체 읽기
- 원두 Entity 구조 변경
- DB 스키마 변경
- 로그인/회원 기능 수정
- 전체 UI 리디자인
- Controller에 Google Vision API 호출 코드 직접 작성
- 인증 정보 하드코딩
- OCR 결과 자동 저장
- LLM API 연동
- 파서 고도화
- 대규모 리팩터링

---

## 21. 완료 조건

이번 기능은 다음 조건을 만족하면 완료로 본다.

- 이미지 업로드 후 Google Vision API 호출이 가능하다.
- OCR raw text를 서버에서 받을 수 있다.
- OCR 결과를 등록 폼에 전달할 수 있다.
- OCR 실패 시 사용자에게 에러 메시지를 보여준다.
- OCR 결과는 자동 저장되지 않는다.
- 사용자가 확인 후 최종 저장한다.
- 인증 정보가 코드에 하드코딩되어 있지 않다.
- 민감 정보 파일이 `.gitignore`에 포함되어 있다.
- Mock OCR과 실제 Google Vision OCR 구현을 교체 가능한 구조로 유지한다.
- 추후 Parser 또는 LLM Extractor를 붙일 수 있는 구조가 열려 있다.

---

## 22. 이후 작업 후보

이번 구현 이후 다음 작업을 별도 이슈로 진행한다.

1. 실제 원두 카드 이미지 샘플 수집
2. OCR raw text 품질 확인
3. 단순 파서 구현
4. LLM 기반 추출 실험
5. 파서 vs LLM 성능 비교
6. 필드별 정확도 평가
7. 사용자 수정량 기반 UX 개선
8. 배포 환경 인증 방식 정리
9. 비용 모니터링
10. OCR 결과 캐싱 또는 재시도 정책 검토
