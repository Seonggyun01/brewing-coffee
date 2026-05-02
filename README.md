# BrewLog

BrewLog는 브루잉 커피 기록을 관리하기 위한 개인 웹 서비스입니다. 원두 정보, 브루잉 기록, 카페 방문 기록을 단계적으로 구현하며, 초기 버전은 Spring Boot 안에서 서버 사이드 렌더링 방식으로 화면까지 처리합니다.

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

## 프로젝트 구조

```text
coffee_project
├── coffee
│   ├── build.gradle
│   ├── src/main/java/com/hsg/coffee
│   │   ├── domain
│   │   │   ├── bean
│   │   │   ├── brewrecord
│   │   │   ├── cafevisit
│   │   │   └── dashboard
│   │   └── global
│   │       ├── config
│   │       ├── entity
│   │       └── exception
│   └── src/main/resources/application.yml
├── brewlog_erd_backend_design_v_1.md
├── brewlog_frontend_design_v1.md
└── brewlog_tech_stack_local_roadmap_v1.md
```

## 실행 방법

Java 21이 필요합니다.

```powershell
java -version
```

Spring Boot 애플리케이션 실행:

```powershell
cd coffee
.\gradlew.bat bootRun
```

브라우저 접속:

```text
http://localhost:8080
```

현재는 루트 페이지가 아직 구현되지 않았기 때문에 `Whitelabel Error Page`가 보일 수 있습니다. 서버가 꺼지지 않고 실행 중이면 환경 설정은 정상입니다.

## 빌드

```powershell
cd coffee
.\gradlew.bat clean build
```

빌드 결과물은 `coffee/build/` 아래에 생성되며 Git에는 포함하지 않습니다.

## 개발 설정

현재 개발용 데이터베이스는 인메모리 H2를 사용합니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:brewlog
```

H2 콘솔은 애플리케이션 실행 후 아래 주소에서 확인할 수 있습니다.

```text
http://localhost:8080/h2-console
```

## 개발 로드맵

1. 프로젝트 환경 설정
2. 원두 CRUD
3. 브루잉 기록 CRUD
4. 카페 방문 CRUD
5. 메인 통계 지도
6. 카페 지도
7. 대시보드, 검색, 정렬
8. 디자인 정리
9. OCR 고도화
