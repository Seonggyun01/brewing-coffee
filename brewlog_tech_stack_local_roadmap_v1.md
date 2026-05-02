# BrewLog 구현 기술 스택 및 로컬 개발 로드맵 v1.0

## 1. 문서 개요

이 문서는 브루잉 커피 기록 웹 서비스 **BrewLog**를 실제 로컬 환경에서 구현하기 위한 기술 스택과 단계별 개발 순서를 정리한 문서이다.

목표는 처음부터 모든 기능을 완성하는 것이 아니라, 다음 순서로 안정적으로 기능을 확장하는 것이다.

```text
프로젝트 세팅
→ 원두 CRUD
→ 브루잉 기록 CRUD
→ 카페 방문 CRUD
→ 메인 세계 지도
→ 카페 지도
→ 통계/검색
→ OCR 등 고도화
```

---

## 2. 최종 추천 기술 스택

## 2.1 전체 구조

BrewLog는 개인 기록용 웹 서비스이므로 초기 버전에서는 프론트엔드와 백엔드를 완전히 분리하지 않고, Spring Boot 안에서 화면까지 렌더링하는 SSR 방식을 추천한다.

```text
Spring Boot + Thymeleaf + JPA + H2
지도 기능만 JavaScript로 처리
```

---

## 2.2 백엔드 기술 스택

| 영역 | 기술 | 사용 이유 |
|---|---|---|
| Language | Java 21 | Spring Boot 4.0.x와 잘 맞고 장기적으로 사용하기 좋음 |
| Framework | Spring Boot 4.0.6 | 현재 생성된 프로젝트 기준으로 사용 |
| Web | Spring MVC | Controller 기반 SSR 웹 구현 |
| Template | Thymeleaf | Spring Boot와 연동하기 쉬운 서버 사이드 템플릿 |
| ORM | Spring Data JPA | Entity/Repository 기반 DB CRUD 처리 |
| Validation | Bean Validation | 입력 폼 검증 처리 |
| Database | H2 Database | 로컬 개발용으로 빠르고 간단함 |
| Build Tool | Gradle | Spring Boot 프로젝트 기본 빌드 도구로 사용 |
| Boilerplate | Lombok | Getter, 생성자, RequiredArgsConstructor 등 코드 감소 |

### 참고 링크

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/index.html)
- [Spring Boot 프로젝트 페이지](https://spring.io/projects/spring-boot)
- [Spring Data JPA 공식 프로젝트 페이지](https://spring.io/projects/spring-data-jpa)
- [Spring 공식 가이드 - Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa)
- [Spring 공식 가이드 - Handling Form Submission](https://spring.io/guides/gs/handling-form-submission)
- [Thymeleaf + Spring 공식 튜토리얼](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html)
- [H2 Database 공식 Quickstart](https://www.h2database.com/html/quickstart.html)

---

## 2.3 프론트엔드 기술 스택

| 영역 | 기술 | 사용 이유 |
|---|---|---|
| Markup | HTML | Thymeleaf 템플릿 기반 화면 작성 |
| Style | CSS | 커스텀 디자인 적용 |
| UI Framework | Bootstrap 5 | 카드, 폼, 버튼, 반응형 레이아웃 구현 |
| Script | Vanilla JavaScript | 지도, 마커, 간단한 클릭 이벤트 처리 |
| Custom Style | custom.css | BrewLog만의 베이지/브라운 톤 적용 |

### 참고 링크

- [Bootstrap 5 Navbar 공식 문서](https://getbootstrap.com/docs/5.3/components/navbar/)
- [Bootstrap 5 Card 공식 문서](https://getbootstrap.com/docs/5.0/components/card/)
- [Bootstrap 공식 Examples](https://getbootstrap.kr/docs/5.0/examples/)

---

## 2.4 지도 기술 스택

| 기능 | 추천 기술 | 설명 |
|---|---|---|
| 원두 원산지 세계 지도 | Leaflet.js | 국가별 마커 표시 |
| 카페 방문 대한민국 지도 | Kakao Maps JavaScript API | 국내 카페 위치 표시 |
| 주소 → 좌표 변환 | Kakao Local API, 추후 | MVP에서는 좌표 직접 입력으로 시작 가능 |

### 참고 링크

- [Leaflet Quick Start Guide](https://leafletjs.com/examples/quick-start/)
- [Leaflet 1.9.4 API Reference](https://leafletjs.com/reference.html)
- [Kakao 지도 Web API 공식 가이드](https://apis.map.kakao.com/web/guide/)
- [Kakao 지도 Web API 공식 문서](https://apis.map.kakao.com/web/documentation/)
- [Kakao 지도 앱 키 발급 및 Web 플랫폼 등록 참고 글](https://save-idea.tistory.com/163)
- [Kakao 지도 API 사용 시 403 에러 관련 참고 글](https://greed-yb.tistory.com/335)

---

## 2.5 OCR 기술 스택, 추후 구현

OCR 기능은 초기 MVP 이후에 붙이는 것을 추천한다.

| 단계 | 기술 | 설명 |
|---|---|---|
| 1차 | 이미지 업로드 | 원두 카드 이미지만 저장 |
| 2차 | Tesseract OCR | 무료 OCR로 텍스트 추출 |
| 3차 | Cloud OCR | Google Vision, Naver CLOVA OCR 등 |
| 4차 | LLM 구조화 | OCR 텍스트를 원두 필드 JSON으로 변환 |

MVP에서는 OCR을 구현하지 않고, 원두 등록 폼을 먼저 안정화하는 것이 좋다.

---

# 3. 로컬 구현 전체 단계

## 전체 단계 요약

```text
Phase 1. 프로젝트 세팅
Phase 2. 원두 CRUD
Phase 3. 브루잉 기록 CRUD
Phase 4. 카페 방문 CRUD
Phase 5. 메인 세계 지도
Phase 6. 카페 지도
Phase 7. 대시보드 / 검색 / 정렬
Phase 8. 디자인 정리
Phase 9. OCR 등 고도화
```

---

# 4. Phase 1. 프로젝트 세팅

## 4.1 목표

Spring Boot 프로젝트를 생성하고, 로컬에서 실행 가능한 기본 상태를 만든다.

---

## 4.2 Spring Initializr 설정

추천 설정:

```text
Project: Gradle
Language: Java
Spring Boot: 4.0.6
Java: 21
Packaging: Jar
```

추가 의존성:

```text
Spring Web
Thymeleaf
Spring Data JPA
Validation
H2 Database
Lombok
```

### 참고 링크

- [Spring Initializr](https://start.spring.io/)
- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/index.html)

---

## 4.3 프로젝트 실행 확인

프로젝트 생성 후 IntelliJ에서 열고 다음 명령어로 확인한다.

```bash
./gradlew clean build
./gradlew bootRun
```

브라우저 접속:

```text
http://localhost:8080
```

---

## 4.4 기본 패키지 구조 생성

추천 패키지 구조:

```text
com.example.brewlog
 ├── domain
 │   ├── bean
 │   ├── brewrecord
 │   ├── cafevisit
 │   └── dashboard
 └── global
     ├── entity
     ├── exception
     └── config
```

도메인 내부 구조:

```text
entity
repository
service
controller
dto
exception
```

---

## 4.5 application.yml 작성

개발 초기 설정 예시:

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/brewlog
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true

  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: debug
```

초기 개발 중에는 다음 설정을 사용한다.

```yaml
ddl-auto: create
```

기능이 안정되면 다음으로 변경한다.

```yaml
ddl-auto: update
```

### 참고 링크

- [H2 Database 공식 Quickstart](https://www.h2database.com/html/quickstart.html)
- [H2 Database 공식 사이트](https://www.h2database.com/)
- [Spring Boot + H2 + JPA 연동 참고 글](https://turtledev.tistory.com/65)

---

## 4.6 BaseTimeEntity 작성

모든 엔티티에 공통으로 생성일, 수정일을 넣기 위해 작성한다.

파일 위치:

```text
src/main/java/com/example/brewlog/global/entity/BaseTimeEntity.java
```

역할:

```text
createdAt
updatedAt
```

메인 애플리케이션 클래스에는 다음 어노테이션을 추가한다.

```java
@EnableJpaAuditing
```

---

# 5. Phase 2. 원두 CRUD 구현

## 5.1 목표

사용자가 원두를 등록, 조회, 수정, 삭제할 수 있게 만든다.

처음 목표는 다음 하나다.

```text
원두를 등록하고, 목록에서 보고, 상세 페이지로 들어갈 수 있게 만들기
```

---

## 5.2 작성 파일 순서

```text
1. Bean.java
2. BeanRepository.java
3. BeanCreateForm.java
4. BeanUpdateForm.java
5. BeanListResponse.java
6. BeanDetailResponse.java
7. BeanService.java
8. BeanController.java
9. templates/beans/list.html
10. templates/beans/form.html
11. templates/beans/detail.html
```

---

## 5.3 우선 구현할 필드

처음부터 모든 필드를 구현하지 말고, 핵심 필드만 먼저 구현한다.

```text
원두 이름
로스터리
국가
가공 방식
향미 노트
메모
```

그다음 추가한다.

```text
지역
농장
품종
고도
로스팅 날짜
구매 날짜
가격
용량
```

---

## 5.4 구현 URL

```text
GET  /beans
GET  /beans/new
POST /beans
GET  /beans/{beanId}
GET  /beans/{beanId}/edit
POST /beans/{beanId}/edit
POST /beans/{beanId}/delete
```

---

## 5.5 구현 순서

### 1. Bean Entity 작성

필수 검증:

```text
원두 이름은 null 또는 blank 불가
원두 이름은 100자 이하
```

### 2. BeanRepository 작성

기본 상속:

```java
JpaRepository<Bean, Long>
```

초기 검색 메서드:

```java
List<Bean> findByNameContainingIgnoreCase(String name);
```

### 3. BeanCreateForm 작성

`@NotBlank`, `@Size`를 사용한다.

### 4. BeanService 작성

책임:

```text
saveBean
findBeans
findBean
updateBean
deleteBean
```

### 5. BeanController 작성

화면 이동과 폼 처리를 담당한다.

### 6. Thymeleaf 화면 작성

화면:

```text
원두 목록
원두 등록/수정 폼
원두 상세
```

### 참고 링크

- [Spring 공식 가이드 - Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa)
- [Spring Data JPA 공식 프로젝트 페이지](https://spring.io/projects/spring-data-jpa)
- [Spring 공식 가이드 - Handling Form Submission](https://spring.io/guides/gs/handling-form-submission)
- [Thymeleaf + Spring 공식 튜토리얼](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html)

---

# 6. Phase 3. 브루잉 기록 CRUD 구현

## 6.1 목표

특정 원두로 직접 내린 커피 기록을 저장할 수 있게 만든다.

관계:

```text
Bean 1 : N BrewRecord
```

---

## 6.2 작성 파일 순서

```text
1. BrewRecord.java
2. BrewRecordRepository.java
3. BrewRecordCreateForm.java
4. BrewRecordUpdateForm.java
5. BrewRecordListResponse.java
6. BrewRecordDetailResponse.java
7. BrewRecordService.java
8. BrewRecordController.java
9. templates/brewrecords/list.html
10. templates/brewrecords/form.html
11. templates/brewrecords/detail.html
```

---

## 6.3 우선 구현할 필드

```text
원두 선택
추출 날짜
추출 도구
원두량
물량
물 온도
추출 시간
평점
한줄 감상
메모
```

상세 맛 평가는 나중에 추가해도 된다.

```text
산미
단맛
쓴맛
바디감
향
밸런스
```

---

## 6.4 구현 URL

```text
GET  /brew-records
GET  /brew-records/new
POST /brew-records
GET  /brew-records/{recordId}
GET  /brew-records/{recordId}/edit
POST /brew-records/{recordId}/edit
POST /brew-records/{recordId}/delete
```

---

## 6.5 핵심 구현 흐름

원두 상세 페이지에서 버튼을 제공한다.

```text
[브루잉 기록 남기기]
```

클릭 시 이동:

```text
/brew-records/new?beanId={beanId}
```

이 경우 브루잉 기록 등록 폼에서 해당 원두가 미리 선택되어 있어야 한다.

---

## 6.6 구현 포인트

- `BrewRecord`는 반드시 `Bean`을 가진다.
- `@ManyToOne(fetch = FetchType.LAZY)`를 사용한다.
- 평점은 1~5 범위로 검증한다.
- 원두 상세 페이지 하단에 해당 원두의 브루잉 기록 목록을 보여준다.
- 브루잉 상세 페이지에서 원두 이름 클릭 시 원두 상세로 이동한다.

### 참고 링크

- [Spring 공식 가이드 - Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa)
- [Spring 공식 가이드 - Handling Form Submission](https://spring.io/guides/gs/handling-form-submission)

---

# 7. Phase 4. 카페 방문 CRUD 구현

## 7.1 목표

카페에서 마신 커피 경험을 기록할 수 있게 만든다.

카페 방문 기록은 `Bean`과 직접 연결하지 않고 독립 엔티티로 관리한다.

```text
CafeVisit 독립 관리
```

---

## 7.2 작성 파일 순서

```text
1. CafeVisit.java
2. CafeVisitRepository.java
3. CafeVisitCreateForm.java
4. CafeVisitUpdateForm.java
5. CafeVisitListResponse.java
6. CafeVisitDetailResponse.java
7. CafeVisitService.java
8. CafeVisitController.java
9. templates/cafevisits/list.html
10. templates/cafevisits/form.html
11. templates/cafevisits/detail.html
```

---

## 7.3 우선 구현할 필드

```text
카페 이름
주소
위도
경도
방문 날짜
메뉴
원두 이름
평점
메모
```

MVP에서는 카페 주소 검색 API를 바로 붙이지 않고, 위도/경도를 직접 입력해도 된다.

---

## 7.4 구현 URL

```text
GET  /cafe-visits
GET  /cafe-visits/new
POST /cafe-visits
GET  /cafe-visits/{visitId}
GET  /cafe-visits/{visitId}/edit
POST /cafe-visits/{visitId}/edit
POST /cafe-visits/{visitId}/delete
```

---

## 7.5 구현 포인트

- 카페 이름과 방문 날짜는 필수다.
- 평점은 1~5 범위로 검증한다.
- 좌표가 있는 기록만 카페 지도에 표시한다.
- 상세 페이지에 `[지도에서 보기]` 버튼을 둔다.

이동 예시:

```text
/cafes/map?visitId={visitId}
```

---

# 8. Phase 5. 메인 세계 지도 구현

## 8.1 목표

메인 페이지에 지금까지 마신 원두의 원산지 세계 지도를 표시한다.

메인 페이지의 핵심은 다음이다.

```text
메인 화면의 가장 큰 영역 = 세계 지도
```

---

## 8.2 작성 파일

```text
HomeController.java
templates/home.html
static/js/bean-map.js
```

지도 데이터 API:

```text
MapApiController.java
```

---

## 8.3 구현 URL

```text
GET /
GET /api/maps/beans
```

---

## 8.4 `/api/maps/beans` 응답 예시

```json
[
  {
    "country": "Ethiopia",
    "count": 5,
    "latitude": 9.145,
    "longitude": 40.489673
  },
  {
    "country": "Colombia",
    "count": 3,
    "latitude": 4.570868,
    "longitude": -74.297333
  }
]
```

---

## 8.5 국가 좌표 처리

MVP에서는 국가별 중심 좌표를 코드에 하드코딩한다.

예시:

```java
Map<String, CountryCoordinate> coordinates = Map.of(
    "Ethiopia", new CountryCoordinate(9.145, 40.489673),
    "Colombia", new CountryCoordinate(4.570868, -74.297333),
    "Kenya", new CountryCoordinate(-0.023559, 37.906193)
);
```

나중에 국가 데이터가 많아지면 JSON 파일이나 DB 테이블로 분리한다.

---

## 8.6 Leaflet 적용 흐름

```text
home.html 로딩
→ bean-map.js 실행
→ /api/maps/beans 호출
→ 국가별 마커 생성
→ 마커 클릭 시 국가 요약 팝업 표시
```

### 참고 링크

- [Leaflet Quick Start Guide](https://leafletjs.com/examples/quick-start/)
- [Leaflet 1.9.4 API Reference](https://leafletjs.com/reference.html)

---

# 9. Phase 6. 카페 지도 구현

## 9.1 목표

대한민국 지도에 카페 방문 기록을 마커로 표시한다.

---

## 9.2 작성 파일

```text
templates/maps/cafe-map.html
static/js/cafe-map.js
```

지도 데이터 API:

```text
GET /api/maps/cafes
```

화면 URL:

```text
GET /cafes/map
```

---

## 9.3 `/api/maps/cafes` 응답 예시

```json
[
  {
    "id": 1,
    "cafeName": "프릳츠 원서점",
    "latitude": 37.577,
    "longitude": 126.987,
    "visitDate": "2026-05-03",
    "menuName": "핸드드립",
    "rating": 5
  }
]
```

---

## 9.4 Kakao Maps 준비

Kakao 지도 JavaScript API를 사용하려면 다음 작업이 필요하다.

```text
1. Kakao Developers 접속
2. 애플리케이션 생성
3. JavaScript 키 확인
4. Web 플랫폼 등록
5. 사이트 도메인에 http://localhost:8080 등록
6. HTML에서 appkey로 JavaScript SDK 로드
```

스크립트 예시:

```html
<script type="text/javascript" src="//dapi.kakao.com/v2/maps/sdk.js?appkey=발급받은키"></script>
```

### 참고 링크

- [Kakao 지도 Web API 공식 가이드](https://apis.map.kakao.com/web/guide/)
- [Kakao 지도 Web API 공식 문서](https://apis.map.kakao.com/web/documentation/)
- [Kakao 지도 앱 키 발급 및 Web 플랫폼 등록 참고 글](https://save-idea.tistory.com/163)
- [Kakao 지도 API 사용 시 403 에러 참고 글](https://greed-yb.tistory.com/335)

---

# 10. Phase 7. 대시보드 / 검색 / 정렬 구현

## 10.1 목표

사용자의 기록을 간단한 통계로 보여주고, 목록 페이지에서 검색/정렬을 제공한다.

---

## 10.2 Dashboard 작성 파일

```text
DashboardController.java
DashboardService.java
DashboardResponse.java
templates/dashboard.html
```

---

## 10.3 대시보드 표시 정보

처음에는 간단하게 4~6개만 보여준다.

```text
등록 원두 수
브루잉 기록 수
카페 방문 수
평균 평점
가장 많이 마신 국가
가장 많이 사용한 추출 도구
```

---

## 10.4 검색 URL 예시

### 원두

```text
/beans?keyword=ethiopia
/beans?country=Ethiopia
/beans?sort=ratingDesc
```

### 브루잉 기록

```text
/brew-records?keyword=v60
/brew-records?brewMethod=V60
/brew-records?sort=latest
```

### 카페 방문

```text
/cafe-visits?keyword=프릳츠
/cafe-visits?cafeName=프릳츠
```

---

## 10.5 구현 순서

처음에는 검색만 구현한다.

```text
1. keyword 검색
2. 정렬
3. 필터
```

정렬과 필터는 처음부터 무리해서 구현하지 않아도 된다.

---

# 11. Phase 8. 디자인 정리

## 11.1 목표

BrewLog만의 심플하고 따뜻한 디자인을 적용한다.

---

## 11.2 작성 파일

```text
src/main/resources/static/css/custom.css
```

---

## 11.3 디자인 방향

```text
배경: 아이보리 / 크림
카드: 흰색
포인트: 브라운 / 딥그린
텍스트: 다크그레이
테두리: 연한 베이지
```

---

## 11.4 공통 UI 컴포넌트

```text
header
card
button
form
empty-state
map-container
```

---

## 11.5 Bootstrap 참고

- [Bootstrap 5 Navbar 공식 문서](https://getbootstrap.com/docs/5.3/components/navbar/)
- [Bootstrap 5 Card 공식 문서](https://getbootstrap.com/docs/5.0/components/card/)
- [Bootstrap 공식 Examples](https://getbootstrap.kr/docs/5.0/examples/)

---

# 12. Phase 9. OCR 등 고도화

## 12.1 목표

원두 카드 이미지를 등록하면 OCR을 통해 원두 정보를 자동 입력할 수 있게 한다.

---

## 12.2 구현 순서

```text
1. 원두 이미지 업로드
2. OCR 원문 추출
3. 추출 텍스트 표시
4. 원두 필드 자동 매핑
5. 사용자 확인 후 저장
6. 기존 원두와 중복 후보 비교
```

---

## 12.3 추천 정책

OCR은 자동 저장 기능이 아니라 자동 입력 보조 기능으로 만든다.

```text
이미지 업로드
→ OCR 추출
→ 폼에 자동 입력
→ 사용자가 수정
→ 저장
```

---

# 13. 예외 처리 구현

## 13.1 목표

없는 원두나 기록에 접근했을 때 기본 Whitelabel Error Page가 아니라, 사용자에게 이해하기 쉬운 에러 화면을 보여준다.

---

## 13.2 작성 파일

```text
global/exception/ErrorCode.java
global/exception/BaseException.java
global/exception/ErrorResponseDto.java
global/exception/GlobalExceptionHandler.java
templates/error/custom-error.html
```

---

## 13.3 우선 처리할 예외

```text
원두를 찾을 수 없습니다.
브루잉 기록을 찾을 수 없습니다.
카페 방문 기록을 찾을 수 없습니다.
브루잉 기록이 있는 원두는 삭제할 수 없습니다.
평점은 1점 이상 5점 이하로 입력해야 합니다.
```

---

# 14. Git 브랜치 전략

혼자 하는 프로젝트라도 기능 단위로 브랜치를 나누면 좋다.

```text
main
develop
feature/init-project
feature/bean-crud
feature/brew-record-crud
feature/cafe-visit-crud
feature/world-map
feature/cafe-map
feature/dashboard
feature/design
feature/ocr
```

---

## 14.1 커밋 메시지 예시

Angular 스타일 커밋 메시지를 추천한다.

```text
feat: 원두 등록 기능 추가
feat: 브루잉 기록 엔티티 추가
feat: 원두 세계 지도 API 추가
style: 원두 카드 UI 스타일 적용
refactor: 원두 서비스 검증 로직 분리
fix: 원두 삭제 시 브루잉 기록 참조 오류 수정
docs: 프로젝트 구현 단계 문서 추가
```

---

# 15. 최종 구현 우선순위

## 15.1 제일 먼저 만들 파일 순서

```text
1. build.gradle
2. application.yml
3. BrewlogApplication.java
4. BaseTimeEntity.java
5. Bean.java
6. BeanRepository.java
7. BeanCreateForm.java
8. BeanService.java
9. BeanController.java
10. templates/beans/list.html
11. templates/beans/form.html
12. templates/beans/detail.html
13. static/css/custom.css
```

이 단계까지 완료하면 첫 번째 기능 단위인 **원두 CRUD**가 완성된다.

---

## 15.2 이후 구현 순서

```text
1. BrewRecord CRUD
2. CafeVisit CRUD
3. Home 세계 지도
4. Kakao 카페 지도
5. Dashboard
6. 검색 / 정렬
7. 디자인 정리
8. OCR
```

---

# 16. 지금 당장 로컬에서 할 일

바로 시작한다면 다음 순서로 진행한다.

```text
1. Spring Initializr에서 프로젝트 생성
2. Java 21, Gradle, Spring Boot 4.0.6 선택
3. Web, Thymeleaf, JPA, Validation, H2, Lombok 추가
4. IntelliJ에서 프로젝트 열기
5. application.yml 작성
6. H2 연결 확인
7. BaseTimeEntity 작성
8. Bean 도메인부터 구현
```

초기 목표는 하나다.

```text
원두를 등록하고, 목록에서 보고, 상세 페이지로 들어갈 수 있게 만들기
```

이 기능이 완성되면 BrewLog의 첫 번째 뼈대가 잡힌다.
