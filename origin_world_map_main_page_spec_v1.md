# 메인 페이지 원두 산지 세계지도 기능 구현 명세서 v1.0

## 1. 문서 개요

이 문서는 `brewing-coffee` 프로젝트의 메인 페이지를 **원두 산지 세계지도 중심 화면**으로 구현하기 위한 상세 명세서이다.

사용자는 메인 페이지에서 자신이 구매했거나 경험한 원두의 산지를 세계지도에서 한눈에 확인할 수 있어야 한다. 지도는 실제 지도 서비스처럼 자세한 도로/도시 정보를 보여주는 것이 아니라, **나라 단위로 구분된 깔끔한 배경형 세계지도**로 표현한다.

이 명세서의 핵심 구현 대상은 다음과 같다.

```text
메인 페이지 세계지도 표시
원두 산지 국가별 색상 표시
국가 클릭 시 해당 국가 원두 목록으로 이동
선택 국가 정보 패널 표시
원두 경험 국가 통계 표시
SVG 세계지도 기반 커스텀 디자인 적용
```

---

## 2. 기능 목표

## 2.1 사용자 관점 목표

사용자는 메인 페이지에 접속했을 때 다음을 바로 알 수 있어야 한다.

```text
내가 어떤 나라의 원두를 경험했는가
어떤 나라의 원두를 더 많이 마셨는가
선택한 나라에는 어떤 원두가 있는가
해당 나라 원두 목록으로 바로 이동할 수 있는가
```

## 2.2 서비스 관점 목표

BrewLog의 첫 화면은 단순한 목록이나 숫자 대시보드가 아니라, 사용자의 커피 경험을 시각적으로 보여주는 화면이어야 한다.

따라서 메인 페이지의 중심은 다음이어야 한다.

```text
세계지도
+
원두 산지 색상
+
선택 국가 정보
+
원두 목록 필터 이동
```

---

## 3. 최종 화면 구성

## 3.1 전체 레이아웃

메인 페이지는 다음 구조를 가진다.

```text
[Header]

[Main]
  [세계지도 영역]
  [선택 국가 정보 패널]
  [하단 요약 통계]

[Footer 또는 생략]
```

왼쪽 하단에 있던 **색상 범례 섹터는 제거한다.**

색상의 의미는 별도 범례 카드로 설명하지 않고, 사용자가 국가를 hover/click 했을 때 툴팁 또는 패널에서 자연스럽게 알 수 있게 한다.

---

## 3.2 Header

Header는 기존 프로젝트의 공통 헤더를 사용한다.

표시 항목:

```text
Brewing Coffee 로고
Home
Beans
Brew Records
Map 또는 Origin Map
검색 아이콘, 선택
```

Header에서 `Home`은 활성 상태로 표시한다.

---

## 3.3 Main 지도 영역

지도는 화면의 가장 큰 영역을 차지한다.

요구사항:

```text
지도는 나라 경계만 보이는 단순한 세계지도여야 한다.
기록이 없는 국가는 연한 회색/베이지로 표시한다.
기록이 있는 국가는 원두 경험 수 또는 구매 수에 따라 색상 강도를 다르게 표시한다.
지도는 배경처럼 부드럽고 과하게 튀지 않아야 한다.
지도 API 타일 느낌이 나지 않아야 한다.
```

---

## 3.4 선택 국가 정보 패널

사용자가 지도에서 특정 국가를 클릭하면 오른쪽 패널에 해당 국가 정보가 표시된다.

패널에 표시할 정보:

```text
선택 국가명
해당 국가 원두 개수
대표 향미 또는 최근 원두 정보, 선택
해당 국가 최근 원두 3개
해당 국가 원두 전체 보기 버튼
```

버튼 클릭 시 이동:

```text
/coffee-beans?countryCode={countryCode}
```

또는 현재 프로젝트의 검색 파라미터 구조에 맞추어:

```text
/coffee-beans?country={countryName}
```

추천은 `countryCode` 기반이다.

---

## 3.5 하단 요약 통계

지도 하단에는 간단한 통계만 표시한다.

표시 항목:

```text
경험한 국가 수
전체 원두 수
브루잉 기록 수
```

예시:

```text
Countries experienced: 17
Total beans: 68
Brew records: 142
```

이 영역은 지도보다 시각적으로 약해야 하며, 보조 정보로만 동작한다.

---

# 4. 기술 선택

## 4.1 지도 구현 방식

이 기능은 일반 지도 API가 아니라 **SVG 세계지도**를 사용한다.

추천 기술 조합:

```text
Spring Boot
Thymeleaf
SVG world map
CSS
Vanilla JavaScript
```

## 4.2 SVG 세계지도를 사용하는 이유

```text
나라 단위 표시가 쉽다.
디자인 커스터마이징이 자유롭다.
지도 API보다 가볍다.
도로/도시 정보가 없어 깔끔하다.
배경형 UI로 만들기 좋다.
국가별 fill 색상 변경이 쉽다.
```

## 4.3 지도 API를 사용하지 않는 이유

Kakao Maps, Google Maps, Naver Maps 같은 지도 API는 실제 지도 서비스에 적합하다.

하지만 이번 기능은 다음 이유로 지도 API보다 SVG가 더 적합하다.

```text
나라 단위만 필요하다.
상세 위치, 도로, 도시명이 필요 없다.
지도 타일 디자인이 프로젝트 톤과 맞지 않을 수 있다.
지도 스타일을 완전히 제어하기 어렵다.
```

---

# 5. 데이터 기준

## 5.1 지도에 표시되는 기준

지도에 표시되는 국가는 다음 조건을 만족해야 한다.

```text
CoffeeBean의 산지 국가 정보가 존재한다.
해당 원두가 사용자의 기록에 존재한다.
```

현재 프로젝트가 개인 기록 서비스라면 사용자의 모든 CoffeeBean을 기준으로 한다.

지도 색상 기준은 다음 중 하나를 선택할 수 있다.

```text
방식 A: 구매/등록한 원두 개수 기준
방식 B: 브루잉 기록에 한 번이라도 사용된 원두 기준
방식 C: 구매 또는 브루잉 기록 둘 중 하나라도 있으면 경험한 국가로 간주
```

추천은 방식 C이다.

즉, 사용자가 한 번이라도 해당 국가 원두를 구매하거나 기록했다면 지도에 표시한다.

---

## 5.2 국가 코드 기준

지도와 데이터를 안정적으로 연결하기 위해 국가명 문자열보다 **ISO 국가 코드**를 사용하는 것을 추천한다.

CoffeeBean에는 아래 필드를 두는 것이 좋다.

```text
originCountryName
originCountryCode
```

예:

```text
originCountryName = Ethiopia
originCountryCode = ET
```

또는 기존 `country` 필드를 유지한다면, 내부적으로는 국가 코드로 변환하는 매핑이 필요하다.

---

## 5.3 국가명만 사용하는 경우의 문제

`country` 문자열만 사용하면 다음 문제가 생긴다.

```text
Ethiopia
에티오피아
ETH
Ethiopia Sidamo
```

위 값들이 모두 다르게 저장될 수 있다.

따라서 세계지도 기능을 안정적으로 구현하려면 국가 코드를 도입하는 것이 좋다.

---

# 6. CoffeeBean 도메인 수정 명세

## 6.1 기존 필드 확인

현재 `CoffeeBean`에 국가 또는 산지 관련 필드가 있다면 아래 중 어떤 이름인지 확인한다.

```text
country
origin
originCountry
originCountryName
```

## 6.2 추천 필드

`CoffeeBean` 엔티티에 아래 필드를 추가하거나 정리한다.

```java
@Column(length = 100)
private String originCountryName;

@Column(length = 2)
private String originCountryCode;
```

## 6.3 필드 의미

| 필드 | 설명 | 예시 |
|---|---|---|
| `originCountryName` | 화면 표시용 국가명 | Ethiopia |
| `originCountryCode` | 지도 매핑용 ISO Alpha-2 코드 | ET |

## 6.4 기존 country 필드를 유지할 경우

기존에 `country` 필드를 이미 사용하고 있다면 다음 중 하나로 정리한다.

### 선택 A: 기존 필드 유지

```java
private String country;
private String countryCode;
```

### 선택 B: 명확한 이름으로 변경

```java
private String originCountryName;
private String originCountryCode;
```

추천은 선택 B이다.  
이유는 카페 방문 국가, 구매처 국가 등 다른 국가 정보와 구분하기 쉽기 때문이다.

---

# 7. 국가 데이터 매핑 명세

## 7.1 CountryInfo enum 추천

초기 구현에서는 국가 정보를 DB 테이블로 만들기보다 enum으로 관리하는 것을 추천한다.

패키지 위치:

```text
com.hsg.coffee.global.country
```

파일:

```text
CountryInfo.java
```

## 7.2 CountryInfo 필드

```java
@Getter
@RequiredArgsConstructor
public enum CountryInfo {

    ETHIOPIA("ET", "Ethiopia", "에티오피아"),
    COLOMBIA("CO", "Colombia", "콜롬비아"),
    BRAZIL("BR", "Brazil", "브라질"),
    KENYA("KE", "Kenya", "케냐"),
    GUATEMALA("GT", "Guatemala", "과테말라"),
    COSTA_RICA("CR", "Costa Rica", "코스타리카"),
    PANAMA("PA", "Panama", "파나마"),
    RWANDA("RW", "Rwanda", "르완다"),
    HONDURAS("HN", "Honduras", "온두라스"),
    EL_SALVADOR("SV", "El Salvador", "엘살바도르"),
    PERU("PE", "Peru", "페루"),
    INDONESIA("ID", "Indonesia", "인도네시아"),
    YEMEN("YE", "Yemen", "예멘"),
    MEXICO("MX", "Mexico", "멕시코");

    private final String code;
    private final String englishName;
    private final String koreanName;
}
```

## 7.3 CountryInfo 역할

```text
원두 등록 폼에서 국가 선택 옵션 제공
국가명과 국가 코드 매핑
지도 path id와 서버 데이터 연결
검색 파라미터 생성
```

## 7.4 원두 등록 화면 적용

원두 등록/수정 폼에서는 국가명을 직접 입력하게 하기보다 드롭다운으로 선택하게 한다.

```text
에티오피아
콜롬비아
브라질
케냐
과테말라
...
```

선택 시 저장값:

```text
originCountryCode = ET
originCountryName = Ethiopia
```

MVP에서는 `originCountryCode`만 저장하고, 화면 표시 시 `CountryInfo`로 이름을 가져와도 된다.

---

# 8. 데이터 조회 API 설계

## 8.1 API 목적

메인 페이지의 세계지도는 서버에서 국가별 원두 경험 데이터를 받아와야 한다.

API는 국가별로 아래 정보를 반환한다.

```text
국가 코드
국가명
원두 개수
브루잉 기록 수
색상 단계
최근 원두 목록
```

## 8.2 API URL

```text
GET /api/origin-map/countries
```

## 8.3 응답 예시

```json
[
  {
    "countryCode": "ET",
    "countryName": "Ethiopia",
    "beanCount": 4,
    "brewRecordCount": 12,
    "colorLevel": "HIGH",
    "fillColor": "#5C2E1F",
    "recentBeans": [
      {
        "id": 1,
        "name": "Kochere Washed",
        "roastery": "Fritz",
        "process": "WASHED"
      },
      {
        "id": 2,
        "name": "Aricha Natural",
        "roastery": "Center Coffee",
        "process": "NATURAL"
      }
    ]
  },
  {
    "countryCode": "CO",
    "countryName": "Colombia",
    "beanCount": 2,
    "brewRecordCount": 5,
    "colorLevel": "MEDIUM",
    "fillColor": "#B88746",
    "recentBeans": []
  }
]
```

---

## 8.4 DTO 설계

패키지 위치:

```text
com.hsg.coffee.domain.originMap.dto
```

파일:

```text
OriginCountryResponse.java
OriginBeanPreviewResponse.java
```

## 8.5 OriginCountryResponse

```java
@Getter
@AllArgsConstructor
public class OriginCountryResponse {

    private String countryCode;
    private String countryName;
    private long beanCount;
    private long brewRecordCount;
    private String colorLevel;
    private String fillColor;
    private List<OriginBeanPreviewResponse> recentBeans;
}
```

## 8.6 OriginBeanPreviewResponse

```java
@Getter
@AllArgsConstructor
public class OriginBeanPreviewResponse {

    private Long id;
    private String name;
    private String roastery;
    private String process;
}
```

---

# 9. 색상 정책

## 9.1 색상 기준

지도 색상은 국가별 원두 개수 기준으로 정한다.

추천 기준:

```text
0개: 기본 지도색
1개: 연한 베이지
2~3개: 카라멜 브라운
4개 이상: 진한 커피 브라운
```

## 9.2 색상값

```text
기록 없음: #E8E1D5
1개: #E8C28A
2~3개: #C98A3B
4개 이상: #5C2E1F
hover: #D9A35F
selected stroke: #3A2117
```

## 9.3 ColorLevel enum

패키지 위치:

```text
com.hsg.coffee.domain.originMap.entity
```

파일:

```text
OriginColorLevel.java
```

```java
@Getter
@RequiredArgsConstructor
public enum OriginColorLevel {

    NONE(0, "#E8E1D5"),
    LOW(1, "#E8C28A"),
    MEDIUM(2, "#C98A3B"),
    HIGH(4, "#5C2E1F");

    private final int minCount;
    private final String color;

    public static OriginColorLevel fromBeanCount(long beanCount) {
        if (beanCount >= HIGH.minCount) {
            return HIGH;
        }
        if (beanCount >= MEDIUM.minCount) {
            return MEDIUM;
        }
        if (beanCount >= LOW.minCount) {
            return LOW;
        }
        return NONE;
    }
}
```

---

# 10. Repository 쿼리 설계

## 10.1 CoffeeBeanRepository 추가 메서드

국가별 원두 개수 조회가 필요하다.

```java
@Query("""
        select cb.originCountryCode, count(cb)
        from CoffeeBean cb
        where cb.originCountryCode is not null
        group by cb.originCountryCode
        """)
List<Object[]> countBeansGroupByOriginCountryCode();
```

최근 원두 조회:

```java
List<CoffeeBean> findTop3ByOriginCountryCodeOrderByIdDesc(String originCountryCode);
```

국가 필터 원두 목록:

```java
List<CoffeeBean> findByOriginCountryCodeOrderByIdDesc(String originCountryCode);
```

기존 검색 구조가 있다면 `keyword`, `countryCode`를 동시에 받을 수 있도록 확장한다.

---

## 10.2 BrewRecordRepository 추가 메서드

국가별 브루잉 기록 수를 보여주려면 아래 쿼리가 필요하다.

```java
@Query("""
        select br.coffeeBean.originCountryCode, count(br)
        from BrewRecord br
        where br.coffeeBean.originCountryCode is not null
        group by br.coffeeBean.originCountryCode
        """)
List<Object[]> countBrewRecordsGroupByOriginCountryCode();
```

이 값은 지도 패널에서 보조 정보로 사용한다.

---

# 11. Service 설계

## 11.1 OriginMapService

패키지 위치:

```text
com.hsg.coffee.domain.originMap.service
```

파일:

```text
OriginMapService.java
```

## 11.2 책임

```text
국가별 원두 개수 조회
국가별 브루잉 기록 수 조회
국가별 색상 단계 계산
국가별 최근 원두 3개 조회
지도 응답 DTO 생성
```

## 11.3 메서드

```java
public List<OriginCountryResponse> getOriginCountries()
```

## 11.4 처리 흐름

```text
1. CoffeeBeanRepository에서 국가별 원두 개수 조회
2. BrewRecordRepository에서 국가별 브루잉 기록 수 조회
3. 원두 개수가 있는 국가만 응답 생성
4. 국가 코드로 CountryInfo 조회
5. beanCount 기준으로 OriginColorLevel 계산
6. 최근 원두 3개 조회
7. OriginCountryResponse 리스트 반환
```

## 11.5 Service 코드 초안

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OriginMapService {

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final BrewRecordRepository brewRecordRepository;

    public List<OriginCountryResponse> getOriginCountries() {
        Map<String, Long> beanCounts = toCountMap(
                coffeeBeanRepository.countBeansGroupByOriginCountryCode()
        );

        Map<String, Long> brewRecordCounts = toCountMap(
                brewRecordRepository.countBrewRecordsGroupByOriginCountryCode()
        );

        return beanCounts.entrySet().stream()
                .map(entry -> createResponse(
                        entry.getKey(),
                        entry.getValue(),
                        brewRecordCounts.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();
    }

    private OriginCountryResponse createResponse(
            String countryCode,
            long beanCount,
            long brewRecordCount
    ) {
        CountryInfo countryInfo = CountryInfo.findByCode(countryCode);

        OriginColorLevel colorLevel = OriginColorLevel.fromBeanCount(beanCount);

        List<OriginBeanPreviewResponse> recentBeans =
                coffeeBeanRepository.findTop3ByOriginCountryCodeOrderByIdDesc(countryCode)
                        .stream()
                        .map(bean -> new OriginBeanPreviewResponse(
                                bean.getId(),
                                bean.getName(),
                                bean.getRoastery(),
                                bean.getProcessType() == null ? null : bean.getProcessType().name()
                        ))
                        .toList();

        return new OriginCountryResponse(
                countryCode,
                countryInfo.getEnglishName(),
                beanCount,
                brewRecordCount,
                colorLevel.name(),
                colorLevel.getColor(),
                recentBeans
        );
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }
}
```

---

# 12. Controller 설계

## 12.1 HomeController

메인 페이지는 `/`에서 렌더링한다.

```java
@Controller
@RequiredArgsConstructor
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }
}
```

지도 데이터는 JavaScript가 API로 가져온다.

---

## 12.2 OriginMapApiController

패키지 위치:

```text
com.hsg.coffee.domain.originMap.controller
```

파일:

```text
OriginMapApiController.java
```

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/origin-map")
public class OriginMapApiController {

    private final OriginMapService originMapService;

    @GetMapping("/countries")
    public List<OriginCountryResponse> countries() {
        return originMapService.getOriginCountries();
    }
}
```

---

# 13. Thymeleaf 화면 설계

## 13.1 home.html 위치

```text
src/main/resources/templates/home.html
```

기존 `index.html` 또는 루트 홈 화면이 있다면 `home.html`로 통일하거나 기존 파일을 유지한다.

추천은 다음 중 하나다.

```text
templates/home.html
```

또는 기존 컨벤션을 따르는 경우:

```text
templates/index.html
```

명세서에서는 `home.html` 기준으로 작성한다.

---

## 13.2 home.html 구조

```html
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Brewing Coffee</title>
    <link rel="stylesheet" th:href="@{/css/main.css}">
    <link rel="stylesheet" th:href="@{/css/origin-map.css}">
</head>
<body>

<header>
    <!-- 공통 헤더 -->
</header>

<main class="origin-map-page">
    <section class="origin-map-section">
        <div class="world-map-wrapper">
            <div th:replace="~{fragments/world-map :: world-map}"></div>
        </div>

        <aside class="origin-country-panel" id="originCountryPanel">
            <button type="button" class="panel-close-button" id="closeCountryPanel">×</button>

            <p class="panel-label">SELECTED COUNTRY</p>
            <h2 id="selectedCountryName">Select a country</h2>

            <p id="selectedCountrySummary">
                Click a highlighted country to see beans.
            </p>

            <a id="viewCountryBeansLink" class="country-beans-link" href="/coffee-beans">
                View beans
            </a>

            <div class="recent-beans">
                <div class="recent-beans-header">
                    <h3>Recent beans</h3>
                </div>
                <div id="recentBeanList" class="recent-bean-list"></div>
            </div>
        </aside>
    </section>

    <section class="origin-summary-section">
        <div class="summary-card">
            <span>Countries experienced</span>
            <strong id="experiencedCountryCount">0</strong>
        </div>
        <div class="summary-card">
            <span>Total beans</span>
            <strong id="totalOriginBeanCount">0</strong>
        </div>
        <div class="summary-card">
            <span>Brew records</span>
            <strong id="totalOriginBrewCount">0</strong>
        </div>
    </section>
</main>

<script th:src="@{/js/origin-map.js}"></script>
</body>
</html>
```

---

# 14. SVG 세계지도 구성

## 14.1 파일 위치

SVG를 fragment로 분리하는 것을 추천한다.

```text
src/main/resources/templates/fragments/world-map.html
```

또는 정적 SVG 파일로 둘 수도 있다.

```text
src/main/resources/static/images/world-map.svg
```

추천은 fragment 방식이다.  
이유는 각 국가 path에 class/id를 직접 접근하기 쉽기 때문이다.

---

## 14.2 SVG path 규칙

각 국가 path는 ISO Alpha-2 코드를 id로 가져야 한다.

```html
<svg id="worldMap" viewBox="0 0 1000 500" xmlns="http://www.w3.org/2000/svg">
    <path id="ET" class="map-country" d="..." />
    <path id="CO" class="map-country" d="..." />
    <path id="BR" class="map-country" d="..." />
    <path id="KE" class="map-country" d="..." />
</svg>
```

필수 조건:

```text
id는 국가 코드와 일치해야 한다.
class는 모든 국가에 map-country를 준다.
기록이 있는 국가는 JS에서 active 클래스를 추가한다.
선택된 국가는 JS에서 selected 클래스를 추가한다.
```

---

# 15. JavaScript 구현 명세

## 15.1 파일 위치

```text
src/main/resources/static/js/origin-map.js
```

## 15.2 역할

```text
/api/origin-map/countries 호출
국가별 데이터 Map으로 변환
SVG path에 색상 적용
국가 hover 효과 처리
국가 click 처리
오른쪽 국가 패널 갱신
하단 통계 갱신
/coffee-beans?countryCode=XX 링크 생성
```

## 15.3 데이터 구조

API 응답을 JS에서 아래 형태로 변환한다.

```js
const originCountryMap = new Map();

countries.forEach(country => {
    originCountryMap.set(country.countryCode, country);
});
```

## 15.4 초기화 흐름

```text
1. DOMContentLoaded 이벤트 실행
2. /api/origin-map/countries fetch
3. 국가 데이터 Map 생성
4. SVG 국가 path 색상 적용
5. 하단 통계 갱신
6. 국가 클릭 이벤트 등록
```

## 15.5 JS 코드 초안

```js
document.addEventListener("DOMContentLoaded", async () => {
    const response = await fetch("/api/origin-map/countries");
    const countries = await response.json();

    const countryMap = new Map();
    countries.forEach(country => {
        countryMap.set(country.countryCode, country);
    });

    paintCountries(countryMap);
    bindCountryEvents(countryMap);
    updateSummary(countries);
});

function paintCountries(countryMap) {
    document.querySelectorAll(".map-country").forEach(path => {
        const countryCode = path.id;
        const country = countryMap.get(countryCode);

        path.classList.remove("experienced-country");
        path.style.fill = "";

        if (country) {
            path.classList.add("experienced-country");
            path.style.fill = country.fillColor;
            path.dataset.beanCount = country.beanCount;
            path.dataset.countryName = country.countryName;
        }
    });
}

function bindCountryEvents(countryMap) {
    document.querySelectorAll(".map-country").forEach(path => {
        path.addEventListener("click", () => {
            const countryCode = path.id;
            const country = countryMap.get(countryCode);

            if (!country) {
                return;
            }

            clearSelectedCountries();
            path.classList.add("selected-country");
            updateCountryPanel(country);
        });
    });
}

function clearSelectedCountries() {
    document.querySelectorAll(".map-country").forEach(path => {
        path.classList.remove("selected-country");
    });
}

function updateCountryPanel(country) {
    document.getElementById("selectedCountryName").textContent = country.countryName;
    document.getElementById("selectedCountrySummary").textContent =
        `Experienced beans: ${country.beanCount} · Brew records: ${country.brewRecordCount}`;

    const link = document.getElementById("viewCountryBeansLink");
    link.href = `/coffee-beans?countryCode=${country.countryCode}`;
    link.textContent = `View beans from ${country.countryName}`;

    renderRecentBeans(country.recentBeans);
}

function renderRecentBeans(beans) {
    const container = document.getElementById("recentBeanList");
    container.innerHTML = "";

    if (!beans || beans.length === 0) {
        container.innerHTML = `<p class="empty-recent-beans">No recent beans.</p>`;
        return;
    }

    beans.forEach(bean => {
        const card = document.createElement("a");
        card.className = "recent-bean-card";
        card.href = `/coffee-beans/${bean.id}`;
        card.innerHTML = `
            <strong>${bean.name}</strong>
            <span>${bean.roastery ?? ""}</span>
            <small>${bean.process ?? ""}</small>
        `;
        container.appendChild(card);
    });
}

function updateSummary(countries) {
    const experiencedCountryCount = countries.length;
    const totalBeanCount = countries.reduce((sum, country) => sum + country.beanCount, 0);
    const totalBrewCount = countries.reduce((sum, country) => sum + country.brewRecordCount, 0);

    document.getElementById("experiencedCountryCount").textContent = experiencedCountryCount;
    document.getElementById("totalOriginBeanCount").textContent = totalBeanCount;
    document.getElementById("totalOriginBrewCount").textContent = totalBrewCount;
}
```

---

# 16. CSS 구현 명세

## 16.1 파일 위치

```text
src/main/resources/static/css/origin-map.css
```

## 16.2 전체 스타일 방향

```text
배경: 따뜻한 크림색
지도 기본 국가색: 연한 베이지/그레이
기록 국가색: 브라운 계열
선택 국가: 테두리 강조
패널: 흰색 또는 크림색 카드
그림자: 아주 은은하게
```

## 16.3 CSS 코드 초안

```css
.origin-map-page {
    min-height: calc(100vh - 80px);
    background: #FAF8F4;
    padding: 32px;
}

.origin-map-section {
    position: relative;
    display: grid;
    grid-template-columns: 1fr 360px;
    gap: 32px;
    align-items: center;
}

.world-map-wrapper {
    width: 100%;
    min-height: 600px;
    display: flex;
    align-items: center;
    justify-content: center;
}

#worldMap {
    width: 100%;
    max-width: 1100px;
    height: auto;
}

.map-country {
    fill: #E8E1D5;
    stroke: #FFFFFF;
    stroke-width: 0.5;
    transition: fill 0.2s ease, opacity 0.2s ease, stroke 0.2s ease;
    cursor: default;
}

.map-country.experienced-country {
    cursor: pointer;
}

.map-country.experienced-country:hover {
    fill: #D9A35F;
    opacity: 0.92;
}

.map-country.selected-country {
    stroke: #3A2117;
    stroke-width: 1.2;
}

.origin-country-panel {
    min-height: 420px;
    background: rgba(255, 253, 248, 0.92);
    border: 1px solid #E6DED2;
    border-radius: 24px;
    box-shadow: 0 20px 60px rgba(60, 40, 24, 0.12);
    padding: 32px;
}

.panel-close-button {
    float: right;
    border: none;
    background: transparent;
    font-size: 28px;
    color: #6F4E37;
    cursor: pointer;
}

.panel-label {
    margin: 0 0 8px;
    font-size: 12px;
    letter-spacing: 0.12em;
    color: #A47148;
    font-weight: 700;
}

.origin-country-panel h2 {
    margin: 0 0 16px;
    font-size: 42px;
    color: #3A2117;
}

.country-beans-link {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 52px;
    margin: 24px 0;
    border-radius: 14px;
    background: #5C2E1F;
    color: #FFFFFF;
    text-decoration: none;
    font-weight: 700;
}

.recent-bean-list {
    display: grid;
    grid-template-columns: 1fr;
    gap: 10px;
}

.recent-bean-card {
    display: block;
    padding: 14px;
    border: 1px solid #E6DED2;
    border-radius: 14px;
    color: #3A2117;
    text-decoration: none;
    background: #FFFFFF;
}

.recent-bean-card strong,
.recent-bean-card span,
.recent-bean-card small {
    display: block;
}

.origin-summary-section {
    width: min(720px, 100%);
    margin: 32px auto 0;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    border: 1px solid #E6DED2;
    border-radius: 24px;
    background: rgba(255, 253, 248, 0.86);
    box-shadow: 0 12px 40px rgba(60, 40, 24, 0.08);
}

.summary-card {
    padding: 24px;
    text-align: center;
}

.summary-card span {
    display: block;
    font-size: 13px;
    color: #8A7A6B;
}

.summary-card strong {
    display: block;
    margin-top: 8px;
    font-size: 32px;
    color: #3A2117;
}
```

## 16.4 반응형

모바일 또는 좁은 화면에서는 패널을 아래로 내린다.

```css
@media (max-width: 1024px) {
    .origin-map-section {
        grid-template-columns: 1fr;
    }

    .origin-country-panel {
        width: 100%;
    }

    .origin-summary-section {
        grid-template-columns: 1fr;
    }
}
```

---

# 17. 원두 목록 필터 연동

## 17.1 이동 URL

국가 클릭 후 버튼을 누르면 전체 원두 목록 페이지로 이동한다.

```text
/coffee-beans?countryCode=ET
```

또는 기존 검색 방식에 맞춰:

```text
/coffee-beans?country=Ethiopia
```

추천은 다음이다.

```text
/coffee-beans?countryCode=ET
```

## 17.2 CoffeeBeanController 수정

기존 `/coffee-beans` 목록 메서드가 있다면 `countryCode` 파라미터를 추가한다.

```java
@GetMapping
public String list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String countryCode,
        Model model
) {
    model.addAttribute("coffeeBeans", coffeeBeanService.findCoffeeBeans(keyword, countryCode));
    model.addAttribute("selectedCountryCode", countryCode);
    return "coffeeBeans/list";
}
```

## 17.3 CoffeeBeanService 수정

```java
public List<CoffeeBean> findCoffeeBeans(String keyword, String countryCode) {
    if (StringUtils.hasText(countryCode)) {
        return coffeeBeanRepository.findByOriginCountryCodeOrderByIdDesc(countryCode);
    }

    if (StringUtils.hasText(keyword)) {
        return coffeeBeanRepository.findByNameContainingIgnoreCaseOrderByIdDesc(keyword);
    }

    return coffeeBeanRepository.findAllByOrderByIdDesc();
}
```

검색과 국가 필터를 동시에 지원하려면 추가 쿼리를 만든다.

```java
List<CoffeeBean> findByOriginCountryCodeAndNameContainingIgnoreCaseOrderByIdDesc(
        String originCountryCode,
        String name
);
```

---

# 18. 빈 상태 처리

## 18.1 원두 산지 데이터가 하나도 없는 경우

메인 페이지 지도는 기본 상태로 표시하고, 오른쪽 패널에는 안내 문구를 보여준다.

```text
아직 지도에 표시할 원두가 없어요.
첫 원두를 등록하면 산지가 지도에 표시됩니다.
```

버튼:

```text
원두 등록하기
```

이동:

```text
/coffee-beans/new
```

## 18.2 국가를 클릭했는데 데이터가 없는 경우

기록이 없는 국가는 클릭 이벤트를 무시한다.

또는 클릭 시 아래 안내를 보여줄 수 있다.

```text
아직 이 국가의 원두 기록이 없습니다.
```

추천은 클릭 이벤트를 비활성화하는 것이다.

---

# 19. 테스트 명세

## 19.1 Service 테스트

테스트 대상:

```text
국가별 원두 개수 집계
국가별 브루잉 기록 수 집계
beanCount 기준 색상 단계 계산
최근 원두 3개 조회
국가 코드 기반 응답 생성
```

테스트 예시:

```text
에티오피아 원두 4개 저장 시 HIGH 색상 단계 반환
콜롬비아 원두 2개 저장 시 MEDIUM 색상 단계 반환
브라질 원두 1개 저장 시 LOW 색상 단계 반환
원두가 없는 국가는 응답에 포함하지 않음
```

## 19.2 Repository 테스트

테스트 대상:

```text
countBeansGroupByOriginCountryCode
findTop3ByOriginCountryCodeOrderByIdDesc
findByOriginCountryCodeOrderByIdDesc
```

## 19.3 화면 동작 테스트

수동 확인 항목:

```text
메인 페이지 접속 시 세계지도가 보인다.
원두가 있는 국가에 색상이 적용된다.
원두가 없는 국가는 기본 색상이다.
국가 클릭 시 오른쪽 패널이 갱신된다.
패널 버튼 클릭 시 /coffee-beans?countryCode=XX로 이동한다.
원두 목록 페이지에서 해당 국가 원두만 보인다.
```

---

# 20. 구현 순서

## 20.1 1단계: 국가 코드 도입

```text
CoffeeBean에 originCountryCode 추가
CountryInfo enum 작성
원두 등록/수정 폼에 국가 선택 추가
기존 더미 데이터에 국가 코드 입력
```

커밋:

```text
feat: 원두 산지 국가 코드 추가
```

## 20.2 2단계: 국가별 집계 API 구현

```text
OriginColorLevel enum 작성
OriginCountryResponse 작성
OriginBeanPreviewResponse 작성
OriginMapService 작성
OriginMapApiController 작성
CoffeeBeanRepository 집계 쿼리 추가
BrewRecordRepository 집계 쿼리 추가
```

커밋:

```text
feat: 원두 산지 지도 데이터 API 추가
```

## 20.3 3단계: SVG 세계지도 추가

```text
world-map.html fragment 작성
국가 path id를 ISO 코드로 정리
home.html에 지도 삽입
```

커밋:

```text
feat: 메인 세계지도 화면 추가
```

## 20.4 4단계: 지도 색상 적용 JS 작성

```text
origin-map.js 작성
API 호출
국가별 색상 적용
국가 클릭 이벤트
선택 국가 패널 갱신
하단 요약 통계 갱신
```

커밋:

```text
feat: 원두 산지 지도 인터랙션 추가
```

## 20.5 5단계: 원두 목록 국가 필터 연동

```text
/coffee-beans?countryCode=XX 지원
CoffeeBeanController 수정
CoffeeBeanService 검색 로직 수정
CoffeeBeanRepository 국가 필터 메서드 추가
목록 화면에 현재 필터 표시
```

커밋:

```text
feat: 국가별 원두 목록 필터 추가
```

## 20.6 6단계: 디자인 정리

```text
origin-map.css 작성
패널 디자인
지도 hover/selected 디자인
반응형 처리
기존 main.css와 톤 맞추기
```

커밋:

```text
style: 원두 산지 세계지도 디자인 적용
```

---

# 21. 완료 기준

아래 조건을 만족하면 기능 구현 완료로 본다.

```text
메인 페이지에서 세계지도가 보인다.
기록 없는 국가는 기본 색상으로 표시된다.
원두가 있는 국가는 beanCount 기준 색상으로 표시된다.
원두가 있는 국가만 클릭 가능하다.
국가 클릭 시 오른쪽 패널에 국가 정보가 표시된다.
패널에서 해당 국가 원두 개수와 최근 원두가 표시된다.
패널 버튼 클릭 시 /coffee-beans?countryCode=XX로 이동한다.
원두 목록 페이지에서 해당 국가의 원두만 필터링되어 보인다.
원두 산지 데이터가 없을 때 빈 상태가 표시된다.
지도는 전체적으로 배경처럼 깔끔하고 미니멀하게 보인다.
왼쪽 색상 범례 카드가 없다.
```

---

# 22. 이후 확장 방향

1차 구현 이후에는 다음 기능을 추가할 수 있다.

```text
대표 향미 노트 기준으로 국가 색상 변경
평균 평점 기준으로 국가 색상 변경
브루잉 기록 수 기준으로 국가 색상 변경
색상 기준 전환 드롭다운
국가 hover 툴팁
국가별 미니 통계 모달
원두 산지 지역 단위 표시
지도에서 선택한 국가의 원두 카드 슬라이더
```

색상 기준 전환 예시:

```text
보기 기준:
- 원두 개수
- 브루잉 기록 수
- 평균 평점
- 대표 향미
```

이 기능은 나중에 데이터가 충분히 쌓인 뒤 구현하는 것이 좋다.
