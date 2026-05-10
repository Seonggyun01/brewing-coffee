# Hugging Face 기반 OCR 텍스트 JSON 매핑 기능 구현 명세서

## 1. 기능 목적

Google Vision OCR로 추출한 원두 카드 텍스트를 Hugging Face 모델에 전달하여, 원두 등록 폼에서 사용할 수 있는 JSON 형식의 데이터로 변환한다.

이 기능은 실제 원두 등록 기능에 바로 연결하기 전에, 먼저 `local` 환경에서 테스트 API로 검증한다.

핵심 목표는 다음과 같다.

```text
OCR 추출 텍스트
→ Hugging Face 모델 호출
→ 원두 정보 JSON 생성
→ 서버 검증 및 정리
→ 테스트 API 응답
```

---

## 2. 구현 범위

### 이번 단계에서 구현할 것

- `application-local.yml`에서 Hugging Face 설정값 읽기
- `HuggingFaceBeanMappingService`에서 설정값 직접 주입
- OCR 텍스트를 Hugging Face API에 전달
- Hugging Face 응답에서 JSON 부분 추출
- JSON을 `LlmParsingResponse`로 변환
- 서버에서 필드 검증 및 정리
- 테스트 전용 Controller 구현
- 테스트 API로 OCR 텍스트 직접 입력 후 결과 확인

### 이번 단계에서 제외할 것

- 실제 이미지 업로드와 연결
- Google Vision OCR 호출과 직접 연결
- 원두 등록 폼 자동 입력
- DB 저장
- 사용자 수정값 학습 데이터 저장
- 로컬 LLM 서버 구축
- Hugging Face 모델 fine-tuning

---

## 3. 전체 처리 흐름

```text
사용자 또는 개발자
  ↓
POST /dev/llm-parsing/huggingface
  ↓
LlmParsingTestController
  ↓
HuggingFaceBeanMappingService
  ↓
Hugging Face Inference API 호출
  ↓
모델 응답 수신
  ↓
JSON 문자열 추출
  ↓
LlmParsingResponse로 역직렬화
  ↓
BeanOcrMappingValidator로 후처리
  ↓
JSON 응답 반환
```

---

## 4. 패키지 및 파일 구조

현재 프로젝트 내에 아래 구조로 구현한다.

```text
src/main/java/{basePackage}/llmparsing
├── controller
│   └── LlmParsingTestController.java
├── dto
│   ├── LlmParsingRequest.java
│   ├── LlmParsingResponse.java
│   └── HuggingFaceRequest.java
└── service
    ├── HuggingFaceBeanMappingService.java
    └── BeanOcrMappingValidator.java
```

> 참고: Java 패키지명은 가능하면 `llmparsing`처럼 모두 소문자로 작성한다.  
> 기존에 `llmParsing`으로 만들었다면 당장 동작에는 문제 없지만, 나중에 리팩터링을 권장한다.

---

## 5. 설정 파일

### 5.1 `application.yml`

이미 아래 설정이 존재하므로 별도 프로필 활성화 없이 `application-local.yml`을 읽을 수 있다.

```yaml
spring:
  config:
    import: optional:classpath:application-local.yml
```

### 5.2 `application-local.yml`

`application-local.yml`은 `.gitignore` 대상이어야 한다.

```yaml
huggingface:
  api-key: hf_발급받은_토큰값
  model-url: https://api-inference.huggingface.co/models/numind/NuExtract-2.0-4B
  timeout-seconds: 15
  max-new-tokens: 700
  temperature: 0.1
```

### 5.3 보안 주의사항

- Hugging Face API Key는 Git에 커밋하지 않는다.
- `application-local.yml`이 `.gitignore`에 포함되어 있는지 확인한다.
- 로그에 API Key 전체를 출력하지 않는다.
- 운영 배포 시에는 환경변수 또는 Secret Manager 사용을 검토한다.

확인 명령어:

```bash
git check-ignore -v src/main/resources/application-local.yml
```

---

## 6. 요청 DTO

### 6.1 `LlmParsingRequest.java`

테스트 API 요청 본문을 받는 DTO이다.

```java
package {basePackage}.llmparsing.dto;

public record LlmParsingRequest(
        String ocrText
) {
}
```

### 요청 예시

```json
{
  "ocrText": "PLTER\n파나마\n100g\n<파나마 핀카 하트만 카투라 내추럴기\n해발고도: 1,200~2,000m\n레드와인/ 건포도/ 밀크초콜릿"
}
```

---

## 7. 응답 DTO

### 7.1 `LlmParsingResponse.java`

원두 등록 폼에 맞게 매핑된 결과를 표현한다.

```java
package {basePackage}.llmparsing.dto;

import java.util.List;

public record LlmParsingResponse(
        String name,
        String roastery,
        String originCountry,
        String region,
        String farmOrStation,
        String variety,
        String altitude,
        String process,
        String beanStatus,
        String roastedAt,
        String purchasedAt,
        String price,
        String remainingWeightGram,
        List<String> flavorNotes
) {
    public static LlmParsingResponse empty() {
        return new LlmParsingResponse(
                "", "", "", "", "", "", "", "", "", "", "", "", "", List.of()
        );
    }
}
```

### 응답 예시

```json
{
  "name": "파나마 핀카 하트만 카투라 내추럴",
  "roastery": "PLTER",
  "originCountry": "파나마",
  "region": "",
  "farmOrStation": "핀카 하트만",
  "variety": "카투라",
  "altitude": "1,200~2,000m",
  "process": "NATURAL",
  "beanStatus": "",
  "roastedAt": "",
  "purchasedAt": "",
  "price": "",
  "remainingWeightGram": "100",
  "flavorNotes": ["레드와인", "건포도", "밀크초콜릿"]
}
```

---

## 8. Hugging Face 요청 DTO

### 8.1 `HuggingFaceRequest.java`

Hugging Face API 요청 본문을 만들기 위한 DTO이다.

```java
package {basePackage}.llmparsing.dto;

import java.util.Map;

public record HuggingFaceRequest(
        String inputs,
        Map<String, Object> parameters
) {
}
```

생성 예시:

```java
new HuggingFaceRequest(
        prompt,
        Map.of(
                "temperature", temperature,
                "max_new_tokens", maxNewTokens,
                "return_full_text", false
        )
);
```

---

## 9. Service 명세

### 9.1 `HuggingFaceBeanMappingService.java`

#### 역할

- 설정값 주입
- 프롬프트 생성
- Hugging Face API 호출
- 응답 문자열 수신
- JSON 추출
- DTO 변환
- Validator 후처리 적용

#### 설정값 주입 방식

이번 단계에서는 `HuggingFaceProperties`를 만들지 않고, Service에서 `@Value`로 직접 가져온다.

```java
@Value("${huggingface.api-key}")
private String apiKey;
```

단, 생성자 주입 방식을 사용한다.

#### 주요 메서드

```java
public LlmParsingResponse parseOcrText(String ocrText)
```

#### 처리 규칙

- `ocrText`가 `null`이거나 blank이면 `LlmParsingResponse.empty()` 반환
- Hugging Face 호출 실패 시 `LlmParsingResponse.empty()` 반환
- 응답에서 JSON을 찾지 못하면 `LlmParsingResponse.empty()` 반환
- JSON 파싱 실패 시 `LlmParsingResponse.empty()` 반환
- 정상 파싱 후 `BeanOcrMappingValidator`로 검증한 결과 반환

---

## 10. Hugging Face 호출 방식

### 10.1 Endpoint

```text
POST https://api-inference.huggingface.co/models/numind/NuExtract-2.0-4B
```

### 10.2 Header

```http
Authorization: Bearer {HUGGINGFACE_API_KEY}
Content-Type: application/json
```

### 10.3 Body

```json
{
  "inputs": "프롬프트 문자열",
  "parameters": {
    "temperature": 0.1,
    "max_new_tokens": 700,
    "return_full_text": false
  }
}
```

### 10.4 Timeout

`application-local.yml`의 값을 사용한다.

```yaml
timeout-seconds: 15
```

---

## 11. 프롬프트 명세

### 11.1 기본 프롬프트

```text
너는 커피 원두 카드 OCR 텍스트를 원두 등록 폼 JSON으로 변환하는 도우미다.

아래 OCR 텍스트는 오인식, 줄바꿈 오류, 불필요한 문장, 서로 충돌하는 단어를 포함할 수 있다.

규칙:
1. OCR 텍스트에 근거가 있는 값만 추출한다.
2. 확실하지 않거나 없는 값은 빈 문자열 ""로 반환한다.
3. 절대 추측해서 채우지 않는다.
4. JSON 외의 설명은 출력하지 않는다.
5. 국가가 여러 개 등장하면 원두명, 설명, 문맥에서 가장 일관된 국가만 선택한다.
6. 향미 노트는 "/", ",", "·", 줄바꿈 기준으로 분리한다.
7. 날짜가 없으면 빈 문자열로 반환한다.
8. 가격이 없으면 빈 문자열로 반환한다.
9. process는 다음 중 하나만 반환한다:
   NATURAL, WASHED, HONEY, ANAEROBIC, DECAF, OTHER, ""
10. remainingWeightGram은 숫자만 반환한다. 예: "100g" -> "100"

반환 JSON 형식:
{
  "name": "",
  "roastery": "",
  "originCountry": "",
  "region": "",
  "farmOrStation": "",
  "variety": "",
  "altitude": "",
  "process": "",
  "beanStatus": "",
  "roastedAt": "",
  "purchasedAt": "",
  "price": "",
  "remainingWeightGram": "",
  "flavorNotes": []
}

OCR 텍스트:
---
{ocrText}
---
```

### 11.2 프롬프트 작성 시 주의

- 응답에 설명을 포함하지 않도록 명시한다.
- JSON 형식만 출력하도록 요구한다.
- 없는 값은 반드시 `""`로 반환하도록 한다.
- `flavorNotes`는 빈 값일 경우 `[]`로 반환하도록 한다.
- 모델이 `null`을 반환할 수 있으므로 서버 Validator에서 `""`로 보정한다.

---

## 12. JSON 추출 규칙

Hugging Face 응답은 모델/Provider에 따라 다음처럼 올 수 있다.

```json
[
  {
    "generated_text": "{...}"
  }
]
```

또는 문자열 안에 설명과 JSON이 섞일 수 있다.

따라서 Service에서는 응답 문자열에서 첫 번째 `{`와 마지막 `}` 사이를 추출한다.

```java
private String extractJson(String response) {
    int start = response.indexOf("{");
    int end = response.lastIndexOf("}");

    if (start == -1 || end == -1 || start > end) {
        return "";
    }

    return response.substring(start, end + 1);
}
```

추후 개선 시에는 Hugging Face 응답 구조를 DTO로 먼저 파싱한 뒤 `generated_text`만 꺼내는 방식을 적용할 수 있다.

---

## 13. Validator 명세

### 13.1 `BeanOcrMappingValidator.java`

#### 역할

LLM 응답을 신뢰하지 않고 서버에서 안전하게 정리한다.

#### 검증 규칙

| 필드 | 검증 규칙 |
|---|---|
| 모든 문자열 필드 | `null`이면 `""`, 앞뒤 공백 제거 |
| `process` | 허용 enum 값이 아니면 `""` |
| `remainingWeightGram` | 숫자만 추출, 없으면 `""` |
| `price` | 숫자만 추출, 없으면 `""` |
| `flavorNotes` | `null`이면 빈 배열 |
| `flavorNotes` | 각 항목 trim |
| `flavorNotes` | 빈 문자열 제거 |
| `flavorNotes` | 최대 8개까지만 유지 |
| `roastedAt` | 날짜 형식 불명확하면 `""` |
| `purchasedAt` | 날짜 형식 불명확하면 `""` |

#### 허용 process 값

```text
NATURAL
WASHED
HONEY
ANAEROBIC
DECAF
OTHER
""
```

---

## 14. Controller 명세

### 14.1 `LlmParsingTestController.java`

#### 역할

local 테스트용 API를 제공한다.

#### Endpoint

```http
POST /dev/llm-parsing/huggingface
```

#### Request Body

```json
{
  "ocrText": "OCR 결과 텍스트"
}
```

#### Response Body

```json
{
  "name": "",
  "roastery": "",
  "originCountry": "",
  "region": "",
  "farmOrStation": "",
  "variety": "",
  "altitude": "",
  "process": "",
  "beanStatus": "",
  "roastedAt": "",
  "purchasedAt": "",
  "price": "",
  "remainingWeightGram": "",
  "flavorNotes": []
}
```

#### 권장 설정

해당 Controller는 개발용이므로 운영 배포에서 노출되지 않도록 한다.

방법 1:

```java
@Profile("local")
@RestController
@RequestMapping("/dev/llm-parsing")
public class LlmParsingTestController {
}
```

방법 2:

`dev` 경로를 사용하되, 배포 전에 제거하거나 Security 설정으로 차단한다.

현재 프로젝트가 `spring.config.import`로 `application-local.yml`을 직접 import하고 있다면, `@Profile("local")` 사용 여부는 현재 실행 방식에 맞춰 결정한다.

---

## 15. 예외 처리 정책

이번 단계에서는 테스트 기능이므로 복잡한 커스텀 예외 대신 안전한 fallback을 우선한다.

| 상황 | 처리 |
|---|---|
| OCR 텍스트 없음 | 빈 응답 DTO 반환 |
| Hugging Face API 401 | 로그에 인증 실패 메시지 출력 후 빈 DTO 반환 |
| Hugging Face API 429 | 로그에 rate limit 메시지 출력 후 빈 DTO 반환 |
| Hugging Face API 5xx | 로그 출력 후 빈 DTO 반환 |
| 응답 timeout | 빈 DTO 반환 |
| JSON 파싱 실패 | 빈 DTO 반환 |

API 키는 로그에 출력하지 않는다.

---

## 16. 테스트 방법

### 16.1 서버 실행

```bash
./gradlew bootRun
```

### 16.2 API 호출 예시

```bash
curl -X POST http://localhost:8080/dev/llm-parsing/huggingface \
  -H "Content-Type: application/json" \
  -d '{
    "ocrText": "PLTER\nS\nDE로 명성하여 보관하십시요.\nMEMO\n파나마\n100g\n<파나마 핀카 하트만 카투라 내추럴기\n파나마의 유서 깊은 농장인 핀카 하트만에서 생산되\n두입니다.\n해발고도: 1,200~2,000m\n레드와인/ 건포도/ 밀크초콜릿\nEthiopia\nTi"
  }'
```

### 16.3 기대 응답 예시

```json
{
  "name": "파나마 핀카 하트만 카투라 내추럴",
  "roastery": "PLTER",
  "originCountry": "파나마",
  "region": "",
  "farmOrStation": "핀카 하트만",
  "variety": "카투라",
  "altitude": "1,200~2,000m",
  "process": "NATURAL",
  "beanStatus": "",
  "roastedAt": "",
  "purchasedAt": "",
  "price": "",
  "remainingWeightGram": "100",
  "flavorNotes": ["레드와인", "건포도", "밀크초콜릿"]
}
```

---

## 17. 성능 및 UX 고려사항

이번 단계는 테스트 API이므로 사용자 화면과 직접 연결하지 않는다.

추후 화면 연결 시에는 다음 UX를 적용한다.

```text
1. OCR 중입니다...
2. AI가 원두 정보를 정리하고 있습니다...
3. 자동 입력이 완료되었습니다. 저장 전 확인해주세요.
```

Hugging Face API가 느릴 수 있으므로 다음 정책을 적용한다.

- timeout 설정
- 실패 시 빈 DTO 반환
- 추후 OCR 텍스트 해시 기반 캐싱 적용
- 사용자가 직접 수정 후 저장하는 구조 유지

---

## 18. 향후 확장 계획

### 18.1 Google Vision OCR과 연결

현재:

```text
OCR 텍스트 직접 입력
→ Hugging Face 매핑
```

향후:

```text
이미지 업로드
→ Google Vision OCR
→ Hugging Face 매핑
→ 폼 자동 입력
```

### 18.2 캐싱 추가

동일 OCR 텍스트에 대해 LLM을 반복 호출하지 않도록 한다.

```text
ocrTextHash = SHA-256(ocrText)
```

저장 후보:

```text
ocr_text_hash
ocr_text
llm_response_json
validated_response_json
created_at
model_url
```

### 18.3 사용자 수정값 저장

나중에 성능 개선 또는 fine-tuning을 위해 다음 데이터를 저장한다.

```text
ocr_text
llm_result_json
user_corrected_json
model_name
prompt_version
created_at
```

### 18.4 모델 교체 가능 구조

현재는 Hugging Face 전용 Service지만, 향후 아래 구조로 확장할 수 있다.

```text
BeanInfoMappingClient interface
├── HuggingFaceBeanInfoMappingClient
├── GeminiBeanInfoMappingClient
└── OpenAiBeanInfoMappingClient
```

---

## 19. 완료 기준

이번 기능의 완료 기준은 다음과 같다.

- `application-local.yml`의 Hugging Face 설정값을 Service에서 정상 주입한다.
- `/dev/llm-parsing/huggingface` API가 동작한다.
- OCR 텍스트를 요청으로 보내면 Hugging Face API를 호출한다.
- 모델 응답에서 JSON을 추출한다.
- `LlmParsingResponse`로 변환한다.
- 잘못된 값은 Validator에서 정리한다.
- API Key가 로그나 Git에 노출되지 않는다.
- 실패 상황에서 서버가 죽지 않고 빈 DTO를 반환한다.

---

## 20. Codex 작업 지시 요약

```text
현재 Spring Boot 프로젝트에 Hugging Face 기반 OCR 텍스트 JSON 매핑 테스트 기능을 구현한다.

요구사항:
1. application-local.yml의 huggingface 설정값을 @Value로 Service에서 직접 주입한다.
2. LlmParsingRequest, LlmParsingResponse, HuggingFaceRequest DTO를 만든다.
3. HuggingFaceBeanMappingService에서 OCR 텍스트를 프롬프트로 감싸 Hugging Face API에 POST 요청한다.
4. WebClient를 사용한다.
5. timeout-seconds 설정값으로 요청 timeout을 적용한다.
6. 응답에서 JSON 부분을 추출하여 ObjectMapper로 LlmParsingResponse로 변환한다.
7. BeanOcrMappingValidator에서 null, process enum, 숫자 필드, flavorNotes를 정리한다.
8. LlmParsingTestController에서 POST /dev/llm-parsing/huggingface 테스트 API를 제공한다.
9. API 키는 절대 로그에 출력하지 않는다.
10. 오류 발생 시 LlmParsingResponse.empty()를 반환한다.
```
