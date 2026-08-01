# 로컬 OCR/LLM 원두 카드 매핑 테스트 리포트 v1

작성일: 2026-08-01 17:12 KST

## 목적

기존 `Google Vision OCR + Hugging Face LLM + 규칙 기반 보정` 흐름에서, 원두 정보 매핑 LLM을 로컬 Ollama 모델로 대체할 수 있는지 확인한다. 또한 로컬 OCR 모델을 실제 원두 카드 이미지에 적용해 Google Vision OCR 대체 가능성을 1차 평가한다.

## 현재 앱 매핑 흐름

- `CoffeeBeanCardExtractionService`: 이미지 검증 후 OCR, 규칙 기반 파서, LLM 매핑 결과를 병합한다.
- `GoogleVisionCoffeeBeanCardOcrService`: 현재 OCR 공급자다.
- `HuggingFaceBeanMappingService`: 현재 LLM 공급자다. 설정 모델은 `google/gemma-4-31B-it` 계열이다.
- `BeanOcrMappingValidator`: LLM 결과를 앱 enum/국가/향미/숫자 형식에 맞게 정리한다.

## 테스트 입력

- 실제 이미지: `docs/image/IMG_8608.heic` ~ `docs/image/IMG_8610.heic`
- 기존 Google Vision OCR 기준 텍스트: `docs/image/ocr-llm-results/*.ocr.txt`
- 기존 외부 LLM 기준 결과: `docs/image/ocr-llm-results/batch-response.json`

HEIC 이미지는 테스트용으로 `/private/tmp/brewlog-ocr-test/images/*_1600.png`에 변환했다.

## OCR 테스트

다운로드 및 실행한 로컬 OCR 모델:

- EasyOCR: `craft_mlt_25k.pth`, `korean_g2.pth`
- PaddleOCR: `PP-OCRv5_server_det`, `korean_PP-OCRv5_mobile_rec`

결과:

- EasyOCR는 실행 속도는 빠르지만, 원두명/중량/향미 노트가 많이 깨졌다.
- PaddleOCR는 이미지 3장 기준 본문 원두명, 고도, 향미, 가공 힌트를 Google Vision에 가깝게 추출했다.
- PaddleOCR도 손글씨 중량과 로스터리 로고/상단 작은 글자는 흔들렸다.

예시 `IMG_8608`:

- EasyOCR: `피나야`, `109`, `래드외인`, `건프도`, `밀크소홀핏`처럼 핵심 값 오인식이 많았다.
- PaddleOCR: `<파나마 핀카 하트만 카투라 내추럴>`, `해발고도:1,200~2,000m`, `레드와인/건포도/밀크초콜릿`을 안정적으로 추출했다.

OCR 1차 결론:

- 로컬 OCR 후보는 PaddleOCR 우선이다.
- 완전 대체 전에는 Google Vision fallback 또는 사용자가 수정하는 확인 UX가 필요하다.

## LLM 프롬프트

테스트 프롬프트는 LLM 역할을 백엔드 함수로 고정하고, 출력 형식과 필드 규칙을 명확히 제한했다.

핵심 지시:

- OCR 텍스트에는 오탈자, 줄바꿈 오류, 손글씨, 불필요한 안내 문구가 섞여 있다.
- OCR 원문에 근거가 있는 값만 채우고, 근거가 약하면 빈 문자열 또는 빈 배열로 둔다.
- 반드시 JSON 하나만 출력한다.
- `roastery`는 로스터리/브랜드/카페명, `name`은 원두 상품명이다.
- `originCountry`는 생산 국가만 한국어 국가명으로 쓴다.
- `region`은 국가명이 아닌 산지/지역/마을명만 쓴다.
- `farmOrStation`은 농장/스테이션/생산자명만 쓴다.
- `process`는 `NATURAL`, `WASHED`, `HONEY`, `ANAEROBIC`, `DECAF`, `OTHER`, `""` 중 하나만 쓴다.
- `remainingWeightGram`은 숫자만 문자열로 쓴다.
- `flavorNotes`는 향미 단위 배열로 쪼갠다.
- 보관방법/설명문은 필드에 억지로 넣지 않는다.

반환 스키마:

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

## 로컬 LLM 3장 비교

대상 모델:

- `gemma3:12b`
- `qwen3:14b`
- `qwen3:8b`
- `qwen2.5:14b`

입력:

- Google Vision OCR 텍스트 3장
- PaddleOCR 로컬 OCR 텍스트 3장

요약 점수:

| 모델 | 호출 수 | 평균 시간 | 필드 점수 |
| --- | ---: | ---: | ---: |
| `gemma3:12b` | 6 | 5.92초 | 33/34 |
| `qwen2.5:14b` | 6 | 8.80초 | 31/34 |
| `qwen3:14b` | 6 | 7.08초 | 30/34 |
| `qwen3:8b` | 6 | 4.16초 | 30/34 |

관찰:

- `gemma3:12b`가 JSON 안정성, 필드 구분, 속도 균형이 가장 좋았다.
- `qwen3:14b`는 품질은 괜찮지만 로스터리를 원두명 일부로 오인하는 케이스가 있었다.
- `qwen3:8b`는 빠르지만 국가/로스터리 혼동이 있어 기본 모델로 쓰기엔 불안하다.
- `qwen2.5:14b`는 안정적이지만 `gemma3:12b`보다 느리고 우위가 뚜렷하지 않았다.

## `gemma3:12b` 전체 10장 회귀 테스트

입력:

- 기존 Google Vision OCR 텍스트 10장

결과:

- 총점: 34/40
- 평균 시간: 5.09초

주요 성공:

- `IMG_8608`, `IMG_8609`, `IMG_8610`은 원두명, 국가, 고도, 가공, 향미, 중량을 대체로 안정적으로 추출했다.
- `IMG_8611`처럼 영어 카드도 JSON 구조화에 성공했다.

주요 실패/주의:

- `IMG_8614`, `IMG_8615`에서 `74158, 74112, 74110` 같은 품종 번호를 altitude로 넣는 오류가 있었다.
- `IMG_8614`에서 `ANAEROBIC NATURAL`을 그대로 반환했다. 현재 validator는 `ANAEROBIC`으로 정규화 가능하다.
- 로스터리 로고 OCR이 깨지면 `뿌린`, `엔치아거` 같은 잘못된 로스터리명이 들어갈 수 있다.
- 기준값이 비어 있던 `IMG_8616`은 실제로 LLM이 많은 값을 추출했다. 이 이미지는 기존 외부 LLM 기준값 자체를 재검토해야 한다.

## 결론

LLM 매핑은 로컬 `gemma3:12b`로 대체 가능성이 높다. 다만 저장 직전 자동 반영에는 아직 위험이 있으므로, 기존 구조처럼 `규칙 기반 파서 + LLM + validator`를 유지하고 LLM 공급자만 Ollama로 교체하는 방식이 가장 안전하다.

OCR은 PaddleOCR가 로컬 후보로 가장 낫지만, Google Vision을 바로 제거하기보다는 다음 순서가 좋다.

1. 1차 OCR: PaddleOCR
2. 신뢰도 낮음 또는 핵심 필드 부족 시 Google Vision fallback
3. LLM 매핑: `gemma3:12b`
4. 후처리: `BeanOcrMappingValidator`
5. 사용자 확인 화면에서 수정 후 저장

## 구현 제안

- `BeanMappingService` 인터페이스를 두고 Hugging Face/Ollama 구현체를 분리한다.
- `brewlog.llm.provider=ollama|huggingface` 설정으로 공급자를 바꾼다.
- Ollama 설정 기본값은 `model-url=http://127.0.0.1:11434/api/generate`, `model-id=gemma3:12b`로 둔다.
- Ollama 요청은 JSON schema `format`을 사용하고, `temperature=0`, `num_predict=700`으로 둔다.
- `process`는 JSON schema enum과 validator 양쪽에서 제한한다.
- `variety`/`altitude` 혼동 방지를 위해 `ALTITUDE`, `ELEVATION`, `MASL`, `m` 근거가 없으면 altitude를 비운다는 프롬프트와 validator 규칙을 추가한다.
- 로스터리 후보는 known roastery/상단 텍스트/카페명 DB 후보를 같이 제공하고, 후보 밖의 깨진 단어는 버린다.
