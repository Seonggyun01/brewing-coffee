# DOCS_INDEX.md

이 문서는 Codex가 어떤 문서를 언제 읽어야 하는지 판단하기 위한 문서 라우팅 인덱스이다.

## 기본 원칙

- 모든 문서를 한 번에 읽지 않는다.
- 작업과 직접 관련된 문서만 읽는다.
- 문서를 읽기 전에 먼저 관련 소스 파일 후보를 좁힌다.
- 작은 수정은 문서보다 코드와 에러 로그를 우선 확인한다.
- 긴 명세서는 기능 흐름이나 설계 의도가 불명확할 때만 읽는다.

## 문서별 사용 기준

### `docs/specs/coffee_bean_card_image_extraction_spec_v1.md`

읽어야 하는 경우:
- 원두 카드 이미지 업로드 기능을 구현할 때
- OCR 또는 이미지 분석으로 원두 정보를 추출할 때
- Google Vision API 연동을 구현할 때
- OCR 결과를 원두 등록 폼에 자동 반영할 때
- OCR 실패 처리, 파싱 규칙, 사용자 확인 흐름이 필요할 때

읽지 말아야 하는 경우:
- 브루잉 기록 CRUD만 수정할 때
- 원산지 지도 화면만 수정할 때
- 단순 CSS 수정일 때
- 로그인/회원 기능만 수정할 때

---

### `docs/specs/google_vision_ocr_implementation_spec_v1.md`

읽어야 하는 경우:
- Google Vision OCR 실제 API 연동 방식을 구현하거나 수정할 때
- Google Vision 인증, 요청, 응답 처리 방식을 확인할 때
- OCR Provider를 mock에서 실제 Google Vision으로 전환할 때
- OCR 호출 실패 처리나 인증 설정 문제를 확인할 때

읽지 말아야 하는 경우:
- OCR 이후 텍스트 파싱이나 LLM 매핑만 수정할 때
- 원두 등록 폼 UI만 수정할 때
- 브루잉 기록 기능만 수정할 때

---

### `docs/specs/huggingface_ocr_json_mapping_spec.md`

읽어야 하는 경우:
- OCR 텍스트를 Hugging Face LLM으로 JSON 매핑하는 기능을 구현하거나 수정할 때
- `HuggingFaceBeanMappingService`, LLM 테스트 Controller, LLM DTO 구조를 확인할 때
- LLM 응답 JSON 추출, 검증, 후처리 규칙을 수정할 때
- `/dev/llm-parsing/huggingface` 테스트 API 동작을 확인할 때

읽지 말아야 하는 경우:
- Google Vision OCR 이미지 인식 자체만 수정할 때
- 원두/브루잉 DB 엔티티 구조만 수정할 때
- 단순 Thymeleaf/CSS 수정일 때

---

### `docs/specs/ocr_preprocessing_country_roastery_mapping_spec.md`

읽어야 하는 경우:
- OCR 텍스트를 LLM에 보내기 전에 전처리하는 기능을 구현하거나 수정할 때
- OCR 줄바꿈, key-value 분리, 후보 정보 생성 규칙을 확인할 때
- 국가/지역 검증 또는 보정 로직을 구현하거나 수정할 때
- 로스터리명과 원두명 구분 개선 로직을 구현하거나 수정할 때

읽지 말아야 하는 경우:
- Google Vision API 호출 자체만 수정할 때
- Hugging Face API 연결 설정만 수정할 때
- 원두 등록 폼 UI만 수정할 때
- 브루잉 기록 기능만 수정할 때

---

### `docs/specs/brewlog_erd_backend_design_v1.md`

읽어야 하는 경우:
- Entity, Repository, Service, Controller 구조를 변경할 때
- DB 테이블, 관계, 컬럼을 수정할 때
- 원두, 브루잉 기록, 사용자, 산지 등 백엔드 도메인 구조를 확인해야 할 때
- JPA 연관관계나 저장 흐름이 불명확할 때

읽지 말아야 하는 경우:
- 단순 UI 문구 수정일 때
- CSS만 수정할 때
- OCR API 호출 방식만 수정할 때

---

### `docs/specs/brewlog_frontend_design_v1.md`

읽어야 하는 경우:
- 화면 구조, 페이지 레이아웃, CSS, 프론트엔드 UI 흐름을 수정할 때
- Thymeleaf 템플릿 구조를 크게 변경할 때
- 반응형 화면이나 공통 UI 규칙을 확인할 때

읽지 말아야 하는 경우:
- Service, Repository만 수정할 때
- Google Vision API 호출 코드만 수정할 때
- DB 구조만 수정할 때

---

### `docs/specs/brewlog_frontend_app_redesign_plan_v1.md`

읽어야 하는 경우:
- BrewLog를 모바일 앱 같은 UI/UX로 개편할 때
- 홈, 원두 목록, 원두 상세, 브루잉 기록, 카페 필터, 지도 화면의 사용자 플로우를 재정의할 때
- 프론트엔드 개편 작업을 커밋 단위로 나누어 진행할 때
- Thymeleaf 유지와 Vue3/React 분리 여부를 판단할 때

읽지 말아야 하는 경우:
- 백엔드 Service, Repository만 수정할 때
- OCR/LLM 호출 로직만 수정할 때
- DB 마이그레이션만 수정할 때

---

### `docs/specs/brewing_taste_record_ux_spec_v1.md`

읽어야 하는 경우:
- 브루잉 기록 작성 UX를 수정할 때
- 맛 기록, 향미, 평점, 추출 기록 화면 흐름을 변경할 때
- 브루잉 기록 폼의 사용자 경험을 확인할 때

읽지 말아야 하는 경우:
- 원두 카드 OCR만 수정할 때
- Google Vision API 호출만 구현할 때
- 산지 지도 화면만 수정할 때

---

### `docs/specs/brewlog_tech_stack_local_roadmap_v1.md`

읽어야 하는 경우:
- 기술 스택, 로컬 개발 환경, 단계별 개발 로드맵을 확인할 때
- 의존성 추가가 프로젝트 방향과 맞는지 확인할 때
- 빌드/실행 환경이나 설정 방향을 확인할 때

읽지 말아야 하는 경우:
- 단순 버그 수정일 때
- 이미 구현 방향이 명확한 작은 기능 수정일 때

---

### `docs/specs/origin_world_map_main_page_spec_v1.md`

읽어야 하는 경우:
- 원산지 세계지도 메인 페이지를 구현하거나 수정할 때
- 원산지별 원두 목록, 지도 UI, 산지 필터링을 수정할 때

읽지 말아야 하는 경우:
- OCR 기능만 수정할 때
- 브루잉 기록 기능만 수정할 때
- Google Vision API 호출만 구현할 때

---

### `docs/specs/INDEX.md`

읽어야 하는 경우:
- docs/specs 폴더의 전체 문서 구성을 이해해야 할 때
- 어떤 명세서를 읽어야 할지 DOCS_INDEX만으로 판단하기 어려울 때

읽지 말아야 하는 경우:
- 작업 대상 문서가 이미 명확할 때
- 작은 코드 수정일 때

---

### `README.md`

읽어야 하는 경우:
- 프로젝트 실행 방법, 설치 방법, 전체 소개를 확인해야 할 때
- README 자체를 수정해야 할 때
- 사용자가 프로젝트 전체 설명을 요청했을 때

읽지 말아야 하는 경우:
- 일반 기능 구현
- 작은 버그 수정
- OCR 기능 구현
- 단순 UI 수정
