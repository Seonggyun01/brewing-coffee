# AGENTS.md

## Project Overview

이 프로젝트는 Spring Boot + Thymeleaf 기반 브루잉 커피 기록 서비스이다.

주요 기능은 다음과 같다.

- 원두 정보 등록/수정/삭제
- 브루잉 기록 등록/조회
- 원두 사진 업로드
- Google Vision OCR을 활용한 원두 정보 추출
- OCR 결과를 등록 폼에 자동 반영
- 사용자가 확인 후 최종 저장

## Core Principles

- 기존 패키지 구조와 네이밍을 최대한 유지한다.
- 요청받은 기능과 직접 관련된 파일만 수정한다.
- 불필요한 대규모 리팩터링을 하지 않는다.
- Controller → Service → Repository 계층 구조를 따른다.
- Thymeleaf 화면은 기존 템플릿 구조와 스타일을 재사용한다.
- 기능 구현 전 변경 대상 파일을 먼저 좁힌다.
- 코드 수정 전 간단한 구현 계획을 먼저 작성한다.
- 사용자가 요청하지 않은 기능을 추가하지 않는다.

## Token Saving Rules

- 긴 README나 전체 명세서를 무조건 읽지 않는다.
- 작업과 직접 관련된 문서만 읽는다.
- 작은 UI 수정은 관련 Thymeleaf 템플릿과 CSS만 확인한다.
- 작은 버그 수정은 에러 로그와 관련 Controller, Service, Repository, Template만 확인한다.
- OCR 작업일 때만 OCR 관련 문서를 확인한다.
- 전체 프로젝트 구조 분석은 사용자가 명시적으로 요청했을 때만 수행한다.
- 파일 탐색은 최소 범위에서 시작하고, 필요한 경우에만 확장한다.

## Spring Boot Rules

- Controller는 요청 처리, Form 바인딩, Model 전달에 집중한다.
- Service는 비즈니스 로직과 트랜잭션 처리를 담당한다.
- Repository는 데이터 접근만 담당한다.
- Entity에 화면 전용 로직을 넣지 않는다.
- 외부 API 연동 코드는 Service 계층 또는 별도 Client 클래스로 분리한다.
- 민감 정보는 코드에 직접 작성하지 않는다.

## Thymeleaf Rules

- 기존 화면 구조와 CSS 스타일을 최대한 유지한다.
- 공통 fragment가 있다면 재사용한다.
- 단순 UI 수정에서 백엔드 구조를 변경하지 않는다.
- form field 이름은 기존 Form/DTO 필드명과 일치시킨다.
- 서버에서 전달하는 model attribute 이름을 임의로 바꾸지 않는다.

## OCR Rules

- OCR 결과는 DB에 바로 저장하지 않는다.
- OCR 결과는 등록 폼에 자동 입력된 후보값으로만 사용한다.
- 사용자가 내용을 확인한 뒤 최종 저장하도록 한다.
- OCR 실패 시 기존 등록 폼으로 돌아가고 에러 메시지를 표시한다.
- Mock OCR 구현이 있다면 실제 OCR 구현과 교체 가능한 구조를 유지한다.
- Google Vision API 인증 정보는 코드에 하드코딩하지 않는다.
- 인증 파일, API Key, secret 설정 파일은 Git에 올리지 않는다.
- OCR 응답 파싱 로직은 Controller에 직접 작성하지 않는다.

## Documentation Rules

- 전체 명세가 필요할 때만 docs/specs 문서를 확인한다.
- OCR 작업일 때만 OCR 관련 문서를 확인한다.
- 버그 수정 시 문서보다 에러 로그와 관련 코드를 우선 확인한다.
- 문서를 읽어야 한다면, 먼저 어떤 문서를 읽을지 간단히 말한다.

## Document Routing Rules

Codex must not read all markdown files by default.

Default order:
1. Read this `AGENTS.md`.
2. Identify the task type from the user request.
3. Inspect the smallest set of relevant source files first.
4. If documentation is needed, read `docs/DOCS_INDEX.md`.
5. Based on `docs/DOCS_INDEX.md`, read only the minimum required spec document.
6. Do not read `README.md` or every file in `docs/specs` unless explicitly requested.

For the current OCR / Google Vision task:
- First inspect the current upload/controller/service/template flow.
- If documentation is needed, read `docs/DOCS_INDEX.md`.
- Then read only `docs/specs/coffee_bean_card_image_extraction_spec_v1.md`.
- Do not read unrelated specs such as world map, frontend design, taste record UX, or full README.

Before reading any spec document, briefly state:
- why the document is needed
- which document will be read
- which documents will not be read

## Git Rules

- 민감 정보 파일은 .gitignore에 포함한다.
- 불필요한 IDE 설정 파일, 빌드 산출물, 로컬 DB 파일은 커밋하지 않는다.
- 변경 범위가 크면 기능 단위로 커밋할 수 있게 파일 변경을 분리한다.
- 커밋은 컨벤션에 맞춰서 한글로 작성한다.

## Before Editing

코드를 수정하기 전에 다음을 먼저 정리한다.

- 작업 목표
- 확인할 파일 후보
- 수정할 파일 후보
- 수정하지 않을 영역
- 예상 구현 흐름

## After Editing

작업 후 다음 형식으로 요약한다.

- 수정 파일:
- 수정 내용:
- 확인 방법:
- 주의할 점:

## Do Not

- 요청하지 않은 대규모 리팩터링을 하지 않는다.
- 전체 파일을 무작정 탐색하지 않는다.
- 긴 문서를 매번 전부 읽지 않는다.
- API Key나 인증 정보를 코드에 작성하지 않는다.
- 기존 기능의 URL, Form 필드명, Model 이름을 임의로 바꾸지 않는다.
- 테스트 없이 성공했다고 단정하지 않는다.