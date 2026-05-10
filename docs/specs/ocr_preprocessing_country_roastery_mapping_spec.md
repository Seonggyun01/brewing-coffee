# OCR 전처리 + 국가/지역 검증 + 로스터리/원두명 구분 개선 명세서

## 1. 목적

기존 Hugging Face 기반 OCR 텍스트 JSON 매핑 기능을 개선한다.

현재 구조는 다음과 같다.

```text
OCR 텍스트
→ LLM
→ JSON 매핑
```

개선 후 구조는 다음과 같다.

```text
OCR 텍스트
→ 범용 전처리
→ 후보 정보 생성
→ LLM 매핑
→ 서버 검증/보정
→ 원두 등록 폼 DTO 반환
```

이번 개선의 핵심은 다음 3가지다.

```text
1. OCR 줄바꿈/키-값 분리 문제를 LLM이 이해하기 쉽게 정리한다.
2. 국가/지역명이 실제 값인지 서버에서 검증한다.
3. LLM이 자주 헷갈리는 로스터리명과 원두명을 후보 기반으로 구분한다.
```

---

## 2. 배경 문제

### 2.1 OCR 결과의 줄바꿈 문제

예를 들어 카드에는 시각적으로 표 형태로 보이지만 OCR 결과는 다음처럼 추출될 수 있다.

```text
COUNTRY
VILLAGE
VARIETY
ETHIOPIA
BANKO CHELCHELE
74112.74110
PROCESS
WASHED
```

사람은 다음처럼 이해할 수 있다.

```text
COUNTRY: ETHIOPIA
VILLAGE: BANKO CHELCHELE
VARIETY: 74112, 74110
PROCESS: WASHED
```

하지만 LLM에 그대로 보내면 키와 값을 잘못 매칭할 수 있다.

---

### 2.2 판매처마다 카드 디자인이 다름

원두 카드마다 다음이 다를 수 있다.

```text
- 로스터리명 위치
- 원두명 위치
- COUNTRY / REGION / PROCESS 표기 방식
- tasting notes 위치
- 한글/영문 혼합 여부
- key-value가 같은 줄에 있는지, 여러 줄로 분리되는지
```

따라서 특정 디자인 전용 파서를 만들면 안 된다.

---

### 2.3 로스터리명과 원두명 혼동

예시:

```text
CLOSE COFFEE
반코 첼첼레, 에티오피아
예가체프 소킹 워시드
```

기대 결과:

```json
{
  "roastery": "CLOSE COFFEE",
  "name": "반코 첼첼레, 에티오피아 예가체프 소킹 워시드"
}
```

잘못된 결과:

```json
{
  "roastery": "반코 첼첼레, 에티오피아 예가체프 소킹 워시드",
  "name": "CLOSE COFFEE"
}
```

---

## 3. 설계 원칙

전처리는 “정답 파서”가 아니라 “후보 생성기”로 만든다.

```text
나쁜 방향:
전처리가 COUNTRY 다음 값은 무조건 originCountry라고 확정한다.

좋은 방향:
전처리가 COUNTRY -> ETHIOPIA 후보를 만들고,
LLM이 원문과 후보를 함께 보고 최종 판단한다.
```

역할 분리는 다음과 같다.

```text
OcrTextPreprocessor
- OCR 텍스트 청소
- key-value 후보 생성
- 로스터리 후보 생성
- 원두명 후보 생성
- 향미 노트 후보 생성

LLM
- 원문과 후보를 보고 문맥 판단
- 원두 등록 폼 JSON 생성

BeanOcrMappingValidator
- 국가명 정규화
- process enum 검증
- 로스터리/원두명 교차 검증
- 숫자/날짜/향미 노트 정리
```

---

## 4. 변경 파일 구조

기존 `llmparsing` 패키지에 전처리 관련 DTO와 Service를 추가한다.

```text
src/main/java/{basePackage}/llmparsing
├── controller
│   └── LlmParsingTestController.java
├── dto
│   ├── LlmParsingRequest.java
│   ├── LlmParsingResponse.java
│   ├── HuggingFaceRequest.java
│   ├── OcrPreprocessResult.java
│   └── KeyValueCandidate.java
└── service
    ├── OcrTextPreprocessor.java
    ├── HuggingFaceBeanMappingService.java
    └── BeanOcrMappingValidator.java
```

---

## 5. DTO 명세

### 5.1 `KeyValueCandidate.java`

```java
package {basePackage}.llmparsing.dto;

public record KeyValueCandidate(
        String key,
        String value
) {
}
```

예:

```json
{
  "key": "COUNTRY",
  "value": "ETHIOPIA"
}
```

---

### 5.2 `OcrPreprocessResult.java`

```java
package {basePackage}.llmparsing.dto;

import java.util.List;

public record OcrPreprocessResult(
        String rawText,
        List<String> cleanedLines,
        List<String> roasteryCandidates,
        List<String> productNameCandidates,
        List<KeyValueCandidate> keyValueCandidates,
        List<String> tastingNoteCandidates,
        String promptText
) {
}
```

필드 설명:

| 필드 | 설명 |
|---|---|
| `rawText` | OCR 원문 |
| `cleanedLines` | 공백, bullet, 빈 줄을 정리한 라인 목록 |
| `roasteryCandidates` | 로스터리명 후보 |
| `productNameCandidates` | 원두명 후보 |
| `keyValueCandidates` | COUNTRY, PROCESS 등 키-값 후보 |
| `tastingNoteCandidates` | 향미 노트 후보 |
| `promptText` | LLM 프롬프트에 넣을 전처리 요약 텍스트 |

---

## 6. `OcrTextPreprocessor` 명세

### 6.1 역할

`OcrTextPreprocessor`는 OCR 원문을 받아 LLM이 이해하기 쉬운 후보 정보를 만든다.

주요 메서드:

```java
public OcrPreprocessResult preprocess(String rawText)
```

---

### 6.2 라인 정리 규칙

OCR 텍스트를 줄 단위로 분리한 뒤 다음을 적용한다.

```text
1. null 또는 blank 라인 제거
2. 앞뒤 공백 trim
3. 연속 공백 하나로 축소
4. bullet 문자 제거
5. 일부 구분자 보정
```

제거할 bullet 후보:

```text
◎
ㅇ
○
●
•
-
*
ㆍ
·
```

예:

```text
◎ 재스민 → 재스민
ㅇ꿀, 배 → 꿀, 배
```

---

### 6.3 구분자 보정 규칙

품종 번호처럼 보이는 경우에 한해 잘못 인식된 구분자를 보정한다.

```text
74112.74110 → 74112, 74110
```

주의:

```text
모든 소수점을 쉼표로 바꾸면 안 된다.
숫자 5자리 이상 + 점 + 숫자 5자리 이상 같은 패턴에 제한적으로 적용한다.
```

---

## 7. 키워드 사전

전처리에서 다음 키워드를 감지한다.

### 7.1 국가 관련

```text
COUNTRY
ORIGIN
PRODUCER COUNTRY
국가
원산지
생산국
산지 국가
```

### 7.2 지역 관련

```text
REGION
AREA
ZONE
DISTRICT
지역
산지
```

### 7.3 농장/마을/워싱스테이션 관련

```text
FARM
FARMER
VILLAGE
STATION
WASHING STATION
농장
마을
워싱스테이션
가공소
```

### 7.4 품종 관련

```text
VARIETY
VARIETAL
CULTIVAR
품종
```

### 7.5 가공 방식 관련

```text
PROCESS
PROCESSING
PROCESS METHOD
가공
가공 방식
프로세스
```

### 7.6 고도 관련

```text
ALTITUDE
ELEVATION
MASL
고도
해발고도
해발
```

### 7.7 향미 노트 관련

```text
TASTING NOTES
CUP NOTES
FLAVOR
FLAVOUR
NOTES
향미
테이스팅 노트
컵노트
```

### 7.8 날짜 관련

```text
ROASTED
ROAST DATE
ROASTED ON
로스팅일
볶은날
```

---

## 8. Key-Value 후보 생성 규칙

### 8.1 같은 줄 key-value

입력:

```text
COUNTRY ETHIOPIA
PROCESS WASHED
```

후보:

```text
COUNTRY -> ETHIOPIA
PROCESS -> WASHED
```

---

### 8.2 key 다음 줄 value

입력:

```text
PROCESS
WASHED
```

후보:

```text
PROCESS -> WASHED
```

---

### 8.3 key 여러 개 다음 value 여러 개

입력:

```text
COUNTRY
VILLAGE
VARIETY
ETHIOPIA
BANKO CHELCHELE
74112.74110
```

후보:

```text
COUNTRY -> ETHIOPIA
VILLAGE -> BANKO CHELCHELE
VARIETY -> 74112, 74110
```

조건:

```text
1. 연속된 라인이 모두 key이다.
2. 그 다음 라인들이 key 개수만큼 존재한다.
3. value 후보 라인은 다른 key가 아니다.
4. 순서대로 매칭한다.
```

주의:

```text
이 결과는 확정값이 아니라 후보이다.
LLM 프롬프트에 “후보는 틀릴 수 있다”고 명시한다.
```

---

## 9. 향미 노트 후보 생성 규칙

### 9.1 TASTING NOTES 블록 감지

다음 키워드 이후 라인을 향미 노트 후보로 본다.

```text
TASTING NOTES
CUP NOTES
FLAVOR
향미
테이스팅 노트
컵노트
```

다음 키워드가 나오면 블록을 종료할 수 있다.

```text
COUNTRY
REGION
VARIETY
PROCESS
ALTITUDE
ROASTED
PRICE
```

---

### 9.2 향미 노트 분리

다음 구분자를 기준으로 분리한다.

```text
,
/
·
ㆍ
|
```

예:

```text
청포도, 복숭아 → 청포도 / 복숭아
꿀, 배 → 꿀 / 배
```

---

### 9.3 후보 정리

```text
1. bullet 제거
2. trim
3. 빈 문자열 제거
4. 중복 제거
5. 최대 8개까지만 유지
```

---

## 10. 로스터리 후보 생성 규칙

로스터리는 보통 다음 특징을 가진다.

```text
1. 카드 상단에 위치한다.
2. 브랜드명 또는 카페명이다.
3. 짧은 영문/한글 브랜드명인 경우가 많다.
4. 국가, 품종, 가공방식, 고도 표현이 포함되지 않는다.
5. COFFEE, ROASTERS, ROASTERY, CAFE, 커피 같은 단어가 포함될 수 있다.
```

후보 생성 방식:

```text
1. cleanedLines 상위 1~3개 라인을 후보로 고려한다.
2. known roastery 사전에 포함된 라인을 후보로 추가한다.
3. COFFEE, ROASTERS, ROASTERY, CAFE, 커피, 로스터스가 포함된 라인을 후보로 추가한다.
```

초기 known roastery 예시:

```java
private static final Set<String> KNOWN_ROASTERIES = Set.of(
        "CLOSE COFFEE",
        "PLTER",
        "FELT",
        "FRITZ",
        "CENTER COFFEE",
        "COFFEE LIBRE",
        "MOMOS COFFEE"
);
```

---

## 11. 원두명 후보 생성 규칙

원두명은 보통 다음 특징을 가진다.

```text
1. 로스터리명 아래에 위치한다.
2. 국가, 지역, 농장, 품종, 가공방식이 포함될 수 있다.
3. TASTING NOTES나 COUNTRY/PROCESS 같은 상세 키워드보다 위에 있다.
4. 상품 제목처럼 보이는 라인이다.
```

후보 생성 방식:

```text
1. 로스터리 후보 바로 다음 1~3개 라인
2. COUNTRY, PROCESS, TASTING NOTES 같은 key가 나오기 전의 제목형 라인
3. 국가명, 지역명, process 키워드가 포함된 라인
4. 인접한 상품명 후보 라인을 합친 문자열
```

예:

```text
반코 첼첼레, 에티오피아
예가체프 소킹 워시드
```

후보:

```text
반코 첼첼레, 에티오피아
예가체프 소킹 워시드
반코 첼첼레, 에티오피아 예가체프 소킹 워시드
```

---

## 12. LLM 프롬프트 변경

LLM에는 raw text만 보내지 않고, 전처리 후보를 함께 보낸다.

### 12.1 프롬프트에 포함할 구조

```text
[RAW_LINES]
1. CLOSE COFFEE
2. 반코 첼첼레, 에티오피아
3. 예가체프 소킹 워시드
4. COUNTRY
5. VILLAGE
6. VARIETY
7. ETHIOPIA
8. BANKO CHELCHELE
9. 74112, 74110
10. PROCESS
11. WASHED
12. TASTING NOTES
13. 재스민
14. 청포도, 복숭아
15. 캐모마일
16. 말린망고
17. 꿀, 배

[ROASTERY_CANDIDATES]
CLOSE COFFEE

[PRODUCT_NAME_CANDIDATES]
반코 첼첼레, 에티오피아
예가체프 소킹 워시드
반코 첼첼레, 에티오피아 예가체프 소킹 워시드

[KEY_VALUE_CANDIDATES]
COUNTRY -> ETHIOPIA
VILLAGE -> BANKO CHELCHELE
VARIETY -> 74112, 74110
PROCESS -> WASHED

[TASTING_NOTE_CANDIDATES]
재스민
청포도
복숭아
캐모마일
말린망고
꿀
배

[RAW_OCR_TEXT]
...
```

---

### 12.2 후보 관련 규칙

프롬프트에 다음 규칙을 추가한다.

```text
아래에는 OCR 원문과 서버가 추출한 후보 정보가 함께 제공된다.

중요 규칙:
1. 후보 정보는 참고용이다.
2. 후보가 틀릴 수 있으므로 OCR 원문과 함께 판단한다.
3. OCR 원문과 후보가 충돌하면 더 자연스럽고 일관된 값을 선택한다.
4. 확실하지 않으면 빈 문자열 ""로 둔다.
5. 없는 값은 만들지 않는다.
6. JSON만 반환한다.
```

---

### 12.3 로스터리/원두명 구분 규칙

프롬프트에 다음 규칙을 추가한다.

```text
로스터리와 원두명 구분 규칙:
1. roastery는 커피를 판매하거나 로스팅한 브랜드/카페 이름이다.
2. name은 원두 상품명이다.
3. name에는 보통 국가, 지역, 농장/마을, 품종, 가공방식이 포함될 수 있다.
4. roastery에는 국가, 지역, 품종, 가공방식을 넣지 않는다.
5. 카드 상단의 로고/브랜드처럼 보이는 짧은 영문 텍스트는 roastery 후보로 우선 고려한다.
6. roastery와 name이 같으면 안 된다.
7. 확실하지 않으면 roastery 또는 name을 빈 문자열로 둔다.
```

---

### 12.4 국가/지역 판단 규칙

프롬프트에 다음 규칙을 추가한다.

```text
국가/지역 검증 규칙:
1. originCountry는 실제 커피 생산국으로 판단되는 값만 넣는다.
2. 국가명이 여러 개 등장하면 원두명, 설명, key-value 후보와 가장 일관된 값을 선택한다.
3. region은 국가보다 작은 산지/지역명이다.
4. farmOrStation은 농장, 마을, 워싱스테이션, 가공소 이름이다.
5. region과 farmOrStation이 애매하면 더 구체적인 이름은 farmOrStation에 넣고, region은 빈 문자열로 둔다.
6. 국가를 확신할 수 없으면 originCountry는 빈 문자열로 둔다.
```

---

## 13. Validator 개선 명세

`BeanOcrMappingValidator`는 `OcrPreprocessResult`를 함께 받아 후처리한다.

기존:

```java
public LlmParsingResponse validate(LlmParsingResponse response)
```

변경:

```java
public LlmParsingResponse validate(
        LlmParsingResponse response,
        OcrPreprocessResult preprocessResult
)
```

---

### 13.1 기본 null 처리

문자열 필드:

```text
null → ""
trim 적용
```

리스트 필드:

```text
null → []
```

---

### 13.2 국가 검증 및 정규화

국가는 서버 사전 기반으로 강하게 검증한다.

초기 alias 예시:

```java
private static final Map<String, String> COUNTRY_ALIASES = Map.ofEntries(
        Map.entry("ETHIOPIA", "에티오피아"),
        Map.entry("에티오피아", "에티오피아"),
        Map.entry("PANAMA", "파나마"),
        Map.entry("파나마", "파나마"),
        Map.entry("COLOMBIA", "콜롬비아"),
        Map.entry("콜롬비아", "콜롬비아"),
        Map.entry("KENYA", "케냐"),
        Map.entry("케냐", "케냐"),
        Map.entry("GUATEMALA", "과테말라"),
        Map.entry("과테말라", "과테말라"),
        Map.entry("BRAZIL", "브라질"),
        Map.entry("브라질", "브라질"),
        Map.entry("COSTA RICA", "코스타리카"),
        Map.entry("코스타리카", "코스타리카"),
        Map.entry("EL SALVADOR", "엘살바도르"),
        Map.entry("엘살바도르", "엘살바도르"),
        Map.entry("HONDURAS", "온두라스"),
        Map.entry("온두라스", "온두라스"),
        Map.entry("RWANDA", "르완다"),
        Map.entry("르완다", "르완다"),
        Map.entry("BURUNDI", "부룬디"),
        Map.entry("부룬디", "부룬디"),
        Map.entry("INDONESIA", "인도네시아"),
        Map.entry("인도네시아", "인도네시아")
);
```

처리:

```text
1. originCountry를 trim + 대문자 처리한다.
2. alias에 있으면 폼 옵션 값으로 정규화한다.
3. alias에 없으면 빈 문자열로 둔다.
```

예:

```text
ETHIOPIA → 에티오피아
Panama → 파나마
Unknown → ""
```

---

### 13.3 지역 검증 정책

지역은 너무 엄격하게 삭제하지 않는다.

처리:

```text
1. region은 trim한다.
2. region이 명확히 국가명인 경우 빈 문자열로 둔다.
3. region이 farmOrStation과 같으면 region을 빈 문자열로 둔다.
4. region이 너무 긴 설명 문장이면 빈 문자열로 둔다.
5. 나중에 country-region 사전이 생기면 confidence 조정에 사용한다.
```

예:

```text
region = "ETHIOPIA" → ""
region = "예가체프" → "예가체프"
region = "BANKO CHELCHELE"이고 farmOrStation도 동일 → region = ""
```

---

### 13.4 farmOrStation 검증 정책

`farmOrStation`은 자유 텍스트를 허용한다.

처리:

```text
1. trim한다.
2. 국가명만 들어온 경우 빈 문자열로 둔다.
3. process 값만 들어온 경우 빈 문자열로 둔다.
4. 너무 긴 설명 문장은 빈 문자열로 둔다.
```

---

### 13.5 process 검증

허용 값:

```text
NATURAL
WASHED
HONEY
ANAEROBIC
DECAF
OTHER
""
```

alias 예시:

```text
내추럴 → NATURAL
NATURAL PROCESS → NATURAL
워시드 → WASHED
WASHED PROCESS → WASHED
허니 → HONEY
무산소 → ANAEROBIC
ANAEROBIC NATURAL → ANAEROBIC
디카페인 → DECAF
DECAF → DECAF
```

허용 값 또는 alias가 아니면 빈 문자열로 둔다.

---

## 14. 로스터리/원두명 교차 검증

### 14.1 roastery가 원두명처럼 보이는 경우

roastery에 다음 요소가 포함되면 의심한다.

```text
국가명
지역명
process
variety
고도
tasting note
```

예:

```text
roastery = "에티오피아 예가체프 워시드"
```

초기 처리:

```text
roastery를 빈 문자열로 둔다.
```

---

### 14.2 name이 known roastery와 같은 경우

예:

```text
name = "CLOSE COFFEE"
roastery = ""
```

처리:

```text
1. name이 known roastery에 있으면 roastery로 이동한다.
2. name은 productNameCandidates 중 가장 그럴듯한 값으로 대체한다.
3. 대체 후보가 없으면 name은 빈 문자열로 둔다.
```

---

### 14.3 name과 roastery가 같은 경우

예:

```text
name = "PLTER"
roastery = "PLTER"
```

처리:

```text
1. roastery는 유지한다.
2. name은 productNameCandidates에서 다른 후보를 찾는다.
3. 없으면 name은 빈 문자열로 둔다.
```

---

### 14.4 name이 너무 짧은 경우

예:

```text
name = "CLOSE"
name = "PLTER"
```

처리:

```text
1. known roastery와 일치하면 roastery 후보로 이동한다.
2. 원두명 후보가 있으면 대체한다.
3. 없으면 빈 문자열로 둔다.
```

---

## 15. Controller 추가 명세

### 15.1 전처리 테스트 API

LLM 호출 없이 전처리 결과만 확인하는 API를 추가한다.

```http
POST /dev/llm-parsing/preprocess
```

Request:

```json
{
  "ocrText": "CLOSE COFFEE\n반코 첼첼레, 에티오피아\n..."
}
```

Response:

```json
{
  "rawText": "...",
  "cleanedLines": [
    "CLOSE COFFEE",
    "반코 첼첼레, 에티오피아",
    "예가체프 소킹 워시드"
  ],
  "roasteryCandidates": [
    "CLOSE COFFEE"
  ],
  "productNameCandidates": [
    "반코 첼첼레, 에티오피아",
    "예가체프 소킹 워시드",
    "반코 첼첼레, 에티오피아 예가체프 소킹 워시드"
  ],
  "keyValueCandidates": [
    {
      "key": "COUNTRY",
      "value": "ETHIOPIA"
    }
  ],
  "tastingNoteCandidates": [
    "재스민",
    "청포도",
    "복숭아"
  ],
  "promptText": "..."
}
```

---

### 15.2 전체 매핑 API

기존 API는 내부 흐름을 변경한다.

```http
POST /dev/llm-parsing/huggingface
```

변경 후 처리:

```text
1. ocrText 입력
2. OcrTextPreprocessor.preprocess()
3. preprocessResult 기반 프롬프트 생성
4. Hugging Face API 호출
5. JSON 파싱
6. BeanOcrMappingValidator.validate(response, preprocessResult)
7. LlmParsingResponse 반환
```

---

## 16. HuggingFaceBeanMappingService 수정 명세

기존:

```text
ocrText
→ prompt 생성
→ Hugging Face 호출
→ JSON 파싱
→ Validator
```

변경:

```text
ocrText
→ OcrTextPreprocessor.preprocess(ocrText)
→ preprocessResult 기반 prompt 생성
→ Hugging Face 호출
→ JSON 파싱
→ Validator.validate(response, preprocessResult)
```

메서드 예시:

```java
public LlmParsingResponse parseOcrText(String ocrText) {
    if (ocrText == null || ocrText.isBlank()) {
        return LlmParsingResponse.empty();
    }

    OcrPreprocessResult preprocessResult = preprocessor.preprocess(ocrText);

    String prompt = buildPrompt(preprocessResult);

    String response = callHuggingFace(prompt);

    String json = extractJson(response);

    LlmParsingResponse parsed = parseJson(json);

    return validator.validate(parsed, preprocessResult);
}
```

---

## 17. 예시: CLOSE COFFEE 카드

### 17.1 OCR 원문

```text
CLOSE COFFEE
반코 첼첼레, 에티오피아
예가체프 소킹 워시드
COUNTRY
VILLAGE
VARIETY
ETHIOPIA
BANKO CHELCHELE
74112.74110
PROCESS
WASHED
TASTING NOTES
◎ 재스민
◎ 청포도, 복숭아
캐모마일
◎말린망고
ㅇ꿀, 배
```

### 17.2 전처리 기대 결과

```text
[RAW_LINES]
1. CLOSE COFFEE
2. 반코 첼첼레, 에티오피아
3. 예가체프 소킹 워시드
4. COUNTRY
5. VILLAGE
6. VARIETY
7. ETHIOPIA
8. BANKO CHELCHELE
9. 74112, 74110
10. PROCESS
11. WASHED
12. TASTING NOTES
13. 재스민
14. 청포도, 복숭아
15. 캐모마일
16. 말린망고
17. 꿀, 배

[ROASTERY_CANDIDATES]
CLOSE COFFEE

[PRODUCT_NAME_CANDIDATES]
반코 첼첼레, 에티오피아
예가체프 소킹 워시드
반코 첼첼레, 에티오피아 예가체프 소킹 워시드

[KEY_VALUE_CANDIDATES]
COUNTRY -> ETHIOPIA
VILLAGE -> BANKO CHELCHELE
VARIETY -> 74112, 74110
PROCESS -> WASHED

[TASTING_NOTE_CANDIDATES]
재스민
청포도
복숭아
캐모마일
말린망고
꿀
배
```

### 17.3 최종 기대 JSON

```json
{
  "name": "반코 첼첼레, 에티오피아 예가체프 소킹 워시드",
  "roastery": "CLOSE COFFEE",
  "originCountry": "에티오피아",
  "region": "예가체프",
  "farmOrStation": "BANKO CHELCHELE",
  "variety": "74112, 74110",
  "altitude": "",
  "process": "WASHED",
  "beanStatus": "",
  "roastedAt": "",
  "purchasedAt": "",
  "price": "",
  "remainingWeightGram": "",
  "flavorNotes": [
    "재스민",
    "청포도",
    "복숭아",
    "캐모마일",
    "말린망고",
    "꿀",
    "배"
  ]
}
```

---

## 18. 테스트 기준

### 18.1 전처리 테스트

| 항목 | 성공 기준 |
|---|---|
| cleanedLines | 불필요한 bullet과 빈 줄 제거 |
| keyValueCandidates | COUNTRY, PROCESS 등 후보 생성 |
| roasteryCandidates | 상단 브랜드명 후보 생성 |
| productNameCandidates | 원두명 후보 1개 이상 생성 |
| tastingNoteCandidates | 향미 노트 분리 |

---

### 18.2 LLM 매핑 테스트

| 항목 | 성공 기준 |
|---|---|
| JSON 파싱 | JSON 추출 가능 |
| originCountry | 국가 alias로 정규화 |
| roastery | 브랜드명으로 추출 |
| name | 원두 상품명으로 추출 |
| process | enum 값으로 정규화 |
| flavorNotes | 최대 8개 이내로 분리 |
| 빈 값 처리 | 없는 값은 `""` 또는 `[]` |

---

## 19. 실패 처리 정책

| 실패 상황 | 처리 |
|---|---|
| 전처리 후보 없음 | rawText만 포함해 LLM 호출 |
| LLM 응답 실패 | `LlmParsingResponse.empty()` 반환 |
| JSON 파싱 실패 | `LlmParsingResponse.empty()` 반환 |
| 국가 검증 실패 | `originCountry = ""` |
| process 검증 실패 | `process = ""` |
| roastery/name 충돌 | 더 안전한 값만 유지, 불확실하면 빈 문자열 |
| flavorNotes 없음 | `[]` |

---

## 20. 구현 우선순위

### 1단계

```text
OcrTextPreprocessor 추가
cleanedLines 생성
tastingNoteCandidates 생성
preprocess 테스트 API 추가
```

### 2단계

```text
keyValueCandidates 생성
COUNTRY/VILLAGE/VARIETY/PROCESS 패턴 복원
promptText 생성
```

### 3단계

```text
roasteryCandidates 생성
productNameCandidates 생성
LLM 프롬프트 수정
```

### 4단계

```text
country alias 검증
process alias 검증
region/farm 검증
roastery/name 교차 검증
```

### 5단계

```text
다양한 원두 카드 OCR 샘플 10~20개로 평가
프롬프트와 전처리 규칙 조정
```

---

## 21. Codex 작업 지시 요약

```text
현재 Hugging Face 기반 OCR JSON 매핑 기능에 OCR 전처리와 검증 개선을 추가한다.

요구사항:
1. OcrTextPreprocessor 클래스를 추가한다.
2. OcrPreprocessResult, KeyValueCandidate DTO를 추가한다.
3. OCR 원문에서 cleanedLines를 만든다.
4. bullet 문자와 불필요한 공백을 제거한다.
5. COUNTRY, REGION, VILLAGE, VARIETY, PROCESS, ALTITUDE, TASTING NOTES 관련 키워드를 감지한다.
6. key-value 후보를 생성한다.
   - 같은 줄 key-value
   - key 다음 줄 value
   - key 여러 개 다음 value 여러 개 패턴
7. TASTING NOTES 이후의 향미 노트 후보를 분리한다.
8. roasteryCandidates를 생성한다.
   - 상단 1~3개 라인
   - known roastery
   - COFFEE/ROASTERS/CAFE/커피 포함 라인
9. productNameCandidates를 생성한다.
   - roastery 후보 아래 라인
   - 상세 key 이전의 제목형 라인
   - 인접 후보 라인 결합
10. LLM 프롬프트에 raw lines, roastery candidates, product name candidates, key-value candidates, tasting note candidates를 포함한다.
11. 프롬프트에 후보는 참고용이며 원문과 함께 판단하라는 규칙을 추가한다.
12. 프롬프트에 roastery/name 구분 규칙을 추가한다.
13. 프롬프트에 국가/지역/farmOrStation 판단 규칙을 추가한다.
14. LlmParsingTestController에 POST /dev/llm-parsing/preprocess API를 추가한다.
15. BeanOcrMappingValidator가 OcrPreprocessResult를 함께 받아 후처리하도록 수정한다.
16. Validator에 country alias 정규화, process alias 정규화, region/farm 검증, roastery/name 교차 검증을 추가한다.
17. 오류가 발생해도 서버가 죽지 않고 빈 DTO 또는 안전한 값으로 응답하도록 한다.
18. API 키는 절대 로그에 출력하지 않는다.
```

---

## 22. 최종 목표

이 개선의 목표는 AI가 모든 값을 완벽히 맞히는 것이 아니다.

목표는 다음과 같다.

```text
사용자가 원두 카드 사진을 올렸을 때,
AI가 등록 폼의 대부분을 자동으로 채워주고,
사용자는 저장 전 확인과 수정만 하면 되는 수준의 자동 초안 생성 기능을 만든다.
```

시스템 원칙:

```text
1. 확실한 값은 자동 입력한다.
2. 불확실한 값은 빈 문자열로 둔다.
3. 사용자가 최종 확인 후 저장한다.
4. 나중에 사용자 수정값을 저장해 품질 개선에 활용한다.
```
