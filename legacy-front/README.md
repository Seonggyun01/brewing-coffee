# 기존 Thymeleaf 프론트

기존 `coffee/src/main/resources/templates`, `static` 파일을 내용 변경 없이 이 폴더로 이동했습니다.
`MANIFEST.sha256`은 이동 시점의 파일 해시입니다. 이동 이후 수정이 필요하면 이 폴더에서 수정합니다.

`coffee/build.gradle`의 리소스 경로에 연결되어 있어 기존 Spring Boot 실행과 URL은 유지됩니다.

```bash
cd ../coffee
./gradlew bootRun
```

기존 화면: http://localhost:8080
새 Vue 3 / Nuxt 프론트는 ../../front에 있습니다. 새 프론트를 localhost:3000에서 실행하면 같은 서버 데이터로 두 화면을 비교할 수 있습니다.
