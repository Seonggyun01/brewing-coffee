# BrewLog

BrewLog는 커피 원두 정보를 기록하고 관리하기 위한 개인용 웹 애플리케이션입니다. 현재는 원두 등록, 조회, 수정, 삭제와 구매처 기록 기능을 중심으로 개발하고 있습니다.

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| View | Thymeleaf |
| Persistence | Spring Data JPA |
| Database | H2 Database |
| Validation | Bean Validation |
| Build | Gradle |
| Utility | Lombok |

## 현재 구현 기능

- 루트 페이지 제공
- 원두 목록 조회
- 원두 등록, 상세 조회, 수정, 삭제
- 원두 이름 검색
- 구매처 정보 선택 입력
- 기존 구매처 선택 후 원두와 연결
- 새 구매처 입력 후 원두와 함께 저장
- 구매처 주소 저장
- 지도 연동을 위한 구매처 좌표 필드 준비
- 서버 실행 시 개발용 더미 데이터 생성
- H2 콘솔 접속

## 프로젝트 구조

```text
coffee_project
├── README.md
└── coffee
    ├── build.gradle
    └── src
        ├── main
        │   ├── java/com/hsg/coffee
        │   │   ├── CoffeeApplication.java
        │   │   ├── domain
        │   │   │   ├── coffeeBean
        │   │   │   │   ├── controller
        │   │   │   │   ├── dto
        │   │   │   │   ├── entity
        │   │   │   │   ├── repository
        │   │   │   │   └── service
        │   │   │   ├── dashboard
        │   │   │   └── purchasePlace
        │   │   │       ├── entity
        │   │   │       ├── repository
        │   │   │       └── service
        │   │   └── global
        │   │       ├── config
        │   │       └── entity
        │   └── resources
        │       ├── application.yml
        │       ├── static/css
        │       └── templates
        └── test
```

## 실행 방법

Java 21이 필요합니다.

```powershell
java -version
```

애플리케이션 실행:

```powershell
cd coffee
.\gradlew.bat bootRun
```

브라우저 접속:

```text
http://localhost:8080
```

원두 관리 화면:

```text
http://localhost:8080/coffee-beans
```

## 빌드 및 테스트

```powershell
cd coffee
.\gradlew.bat clean build
```

테스트는 원두 레포지토리, 서비스, 컨트롤러 흐름을 확인합니다. 테스트 콘솔 출력은 UTF-8 기준으로 설정되어 있습니다.

## 개발 데이터베이스

현재 개발 단계에서는 인메모리 H2를 사용합니다. 서버를 재시작하면 데이터가 초기화됩니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:brewlog
```

H2 콘솔:

```text
http://localhost:8080/h2-console
```

접속 정보:

```text
JDBC URL: jdbc:h2:mem:brewlog
User Name: sa
Password:
```

## 구매처 정책

구매처 정보는 선택 입력입니다.

- 구매처를 남기지 않아도 원두 저장 가능
- 기존에 등록된 구매처가 있으면 선택해서 연결 가능
- 기존 구매처를 선택하지 않고 구매처 이름을 입력하면 새 구매처 생성
- 사용자는 주소까지만 입력
- 위도, 경도는 엔티티에만 준비해두고 추후 지도 연동 또는 주소 변환으로 자동 저장 예정

## 개발 로드맵

1. 원두 관리 기능 고도화
2. 구매처 관리 화면 분리
3. 지도 연동으로 구매처 위치 표시
4. 브루잉 기록 CRUD
5. 카페 방문 기록 CRUD
6. 대시보드 통계
7. 검색 및 정렬 개선
8. 파일 기반 DB 또는 운영 DB 전환
