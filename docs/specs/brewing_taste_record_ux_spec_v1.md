# 브루잉 상태 기록 UX 구현 명세서 v1.0

## 1. 문서 개요

이 문서는 `brewing-coffee` 프로젝트에서 **브루잉 기록을 사용자 친화적으로 남기기 위한 UX 및 데이터 설계 명세서**이다.

기존의 단순 입력 폼 방식은 사용자에게 숙제처럼 느껴질 수 있으므로, 이 기능은 다음 방향을 목표로 한다.

```text
빈칸 입력 최소화
선택형 태그 중심
6각형 맛 프로파일 조절
다양한 향미 노트 선택
향미 노트를 색상으로 표현
기록 상세에서는 선택한 향미 색상을 그라데이션으로 표시
```

---

## 2. 기능 목표

사용자는 브루잉 기록을 작성할 때 다음 정보를 감각적으로 남길 수 있어야 한다.

```text
전체 만족도
6각형 맛 프로파일
향미 노트
추출 느낌 태그
자유 메모
```

이 기능의 핵심은 사용자가 커피 맛을 단순히 숫자로 기록하는 것이 아니라, **맛을 고르고, 조절하고, 색으로 기억할 수 있게 만드는 것**이다.

---

## 3. 브루잉 기록 입력 구조

브루잉 기록 등록 화면은 아래 순서로 구성한다.

```text
1. 기본 브루잉 정보
2. 레시피 정보
3. 전체 만족도
4. 맛 프로파일
5. 향미 노트
6. 추출 느낌 태그
7. 메모
```

입력 영역은 다음처럼 나눈다.

```text
[기본 정보]
- 원두 선택
- 추출 날짜
- 추출 도구

[레시피 정보]
- 원두량
- 물량
- 물 온도
- 분쇄도
- 추출 시간

[맛 기록]
- 전체 만족도
- 6각형 맛 프로파일
- 향미 노트 선택
- 추출 느낌 태그 선택
- 자유 메모
```

---

# 4. 6각형 맛 프로파일 설계

## 4.1 개념

6각형 맛 프로파일은 사용자가 커피의 맛을 직관적으로 표현할 수 있는 시각적 입력 도구이다.

기본 축은 다음 6개로 구성한다.

```text
산미
단맛
쓴맛
바디감
향
밸런스
```

## 4.2 데이터 필드

`BrewRecord` 또는 별도 `BrewTasteProfile`에 아래 필드를 저장한다.

```text
acidity
sweetness
bitterness
body
aroma
balance
```

추천 타입은 `Integer`이고, 값 범위는 `0~5` 또는 `1~5`를 사용한다.

```text
0 = 기록하지 않음
1 = 매우 약함
2 = 약함
3 = 보통
4 = 강함
5 = 매우 강함
```

추천 저장 정책은 `null 허용`이다.  
사용자가 해당 항목을 기록하지 않은 것과 0점을 준 것을 구분할 수 있기 때문이다.

## 4.3 UI 입력 방식

최종 지향점은 6각형 레이더 차트의 각 꼭짓점을 사용자가 직접 드래그해서 조절하는 방식이다.

MVP에서는 구현 난이도를 고려해 다음 방식으로 시작한다.

```text
각 항목은 1~5 점 버튼 또는 슬라이더로 입력
입력 결과를 레이더 차트로 미리보기
```

확장 단계에서는 레이더 차트 꼭짓점을 직접 드래그해서 조절한다.

---

# 5. 향미 노트 설계

## 5.1 기능 개요

향미 노트는 사용자가 커피에서 느낀 향과 맛을 태그처럼 선택하는 기능이다.

핵심은 다음과 같다.

```text
향미 노트를 텍스트 태그로만 보여주지 않는다.
각 향미 노트는 고유한 색상을 가진다.
여러 향미 노트를 선택하면, 선택된 색상들이 그라데이션으로 표현된다.
```

## 5.2 선택 방식

향미 노트는 2단계 구조로 제공한다.

```text
상위 카테고리 선택
→ 세부 향미 노트 선택
```

예시:

```text
Fruity 선택
→ Berry, Citrus, Stone Fruit, Tropical Fruit 등 표시
```

## 5.3 선택 개수 정책

사용자는 자세한 기록을 좋아하므로 선택 가능한 노트는 다양하게 제공한다.  
다만 한 기록에 너무 많은 노트를 선택하면 기록을 다시 볼 때 의미가 흐려질 수 있다.

추천 정책:

```text
권장 선택: 3 ~ 5개
최대 선택: 8개
```

UI 안내 문구:

```text
오늘 느껴진 향미를 선택해주세요. 권장 3~5개
```

최대 개수 초과 시 안내 문구:

```text
향미 노트는 최대 8개까지 선택할 수 있어요.
```

---

# 6. 향미 노트 카테고리 정의

## 6.1 Floral 계열

카테고리 색상: `#C8A2C8`

설명: 꽃, 허브, 향긋한 인상

| 영문 | 한글 |
|---|---|
| Jasmine | 자스민 |
| Rose | 장미 |
| Lavender | 라벤더 |
| Orange Blossom | 오렌지 블라썸 |
| Chamomile | 캐모마일 |
| Hibiscus | 히비스커스 |
| Elderflower | 엘더플라워 |
| Floral | 꽃향 |
| Tea Rose | 티로즈 |

## 6.2 Citrus 계열

카테고리 색상: `#F6C945`

설명: 상큼하고 밝은 산미

| 영문 | 한글 |
|---|---|
| Lemon | 레몬 |
| Lime | 라임 |
| Orange | 오렌지 |
| Grapefruit | 자몽 |
| Yuzu | 유자 |
| Tangerine | 귤 |
| Bergamot | 베르가못 |
| Citron | 시트론 |
| Mandarin | 만다린 |

## 6.3 Berry 계열

카테고리 색상: `#9B1B5A`

설명: 베리류의 새콤달콤한 인상

| 영문 | 한글 |
|---|---|
| Strawberry | 딸기 |
| Raspberry | 라즈베리 |
| Blueberry | 블루베리 |
| Blackberry | 블랙베리 |
| Cranberry | 크랜베리 |
| Blackcurrant | 블랙커런트 |
| Redcurrant | 레드커런트 |
| Cherry | 체리 |
| Acai | 아사이 |

## 6.4 Stone Fruit 계열

카테고리 색상: `#F4A261`

설명: 복숭아, 살구처럼 부드러운 과일감

| 영문 | 한글 |
|---|---|
| Peach | 복숭아 |
| Apricot | 살구 |
| Plum | 자두 |
| Nectarine | 천도복숭아 |
| Cherry | 체리 |
| Yellow Peach | 황도 |
| White Peach | 백도 |
| Dried Apricot | 말린 살구 |

## 6.5 Tropical Fruit 계열

카테고리 색상: `#F9A03F`

설명: 열대과일의 달고 풍성한 인상

| 영문 | 한글 |
|---|---|
| Mango | 망고 |
| Pineapple | 파인애플 |
| Passion Fruit | 패션프루트 |
| Papaya | 파파야 |
| Guava | 구아바 |
| Lychee | 리치 |
| Banana | 바나나 |
| Melon | 멜론 |
| Coconut | 코코넛 |

## 6.6 Apple / Pear 계열

카테고리 색상: `#B7D968`

설명: 사과, 배처럼 맑고 산뜻한 과일감

| 영문 | 한글 |
|---|---|
| Apple | 사과 |
| Green Apple | 청사과 |
| Red Apple | 빨간 사과 |
| Pear | 배 |
| Asian Pear | 동양배 |
| Quince | 모과 |
| Grape | 포도 |
| White Grape | 청포도 |

## 6.7 Sweet 계열

카테고리 색상: `#D6A04D`

설명: 설탕, 꿀, 시럽 같은 단맛

| 영문 | 한글 |
|---|---|
| Honey | 꿀 |
| Brown Sugar | 흑설탕 |
| Maple Syrup | 메이플 시럽 |
| Caramel | 카라멜 |
| Molasses | 당밀 |
| Vanilla | 바닐라 |
| Syrup | 시럽 |
| Toffee | 토피 |
| Cotton Candy | 솜사탕 |

## 6.8 Chocolate 계열

카테고리 색상: `#5C3A2E`

설명: 초콜릿, 코코아 계열의 묵직한 단맛

| 영문 | 한글 |
|---|---|
| Milk Chocolate | 밀크 초콜릿 |
| Dark Chocolate | 다크 초콜릿 |
| Cacao | 카카오 |
| Cocoa | 코코아 |
| Chocolate | 초콜릿 |
| Cacao Nib | 카카오닙스 |
| Mocha | 모카 |
| Brownie | 브라우니 |

## 6.9 Nutty 계열

카테고리 색상: `#A47148`

설명: 견과류의 고소한 인상

| 영문 | 한글 |
|---|---|
| Almond | 아몬드 |
| Hazelnut | 헤이즐넛 |
| Peanut | 땅콩 |
| Walnut | 호두 |
| Pecan | 피칸 |
| Macadamia | 마카다미아 |
| Pistachio | 피스타치오 |
| Chestnut | 밤 |
| Nutty | 견과류 |

## 6.10 Roasted / Smoky 계열

카테고리 색상: `#3B2F2F`

설명: 로스팅, 스모키, 구운 향

| 영문 | 한글 |
|---|---|
| Toast | 토스트 |
| Roasted Nuts | 구운 견과 |
| Smoke | 스모크 |
| Tobacco | 담배 |
| Burnt Sugar | 탄 설탕 |
| Charcoal | 숯 |
| Roasted Grain | 구운 곡물 |
| Baked Bread | 구운 빵 |
| Malt | 몰트 |

## 6.11 Spice 계열

카테고리 색상: `#B5532F`

설명: 향신료의 따뜻하고 자극적인 느낌

| 영문 | 한글 |
|---|---|
| Cinnamon | 시나몬 |
| Clove | 정향 |
| Nutmeg | 넛맥 |
| Cardamom | 카다멈 |
| Black Pepper | 흑후추 |
| Ginger | 생강 |
| Anise | 아니스 |
| Spice | 향신료 |
| Vanilla Spice | 바닐라 스파이스 |

## 6.12 Tea-like 계열

카테고리 색상: `#8A9A5B`

설명: 차처럼 맑고 우아한 느낌

| 영문 | 한글 |
|---|---|
| Black Tea | 홍차 |
| Green Tea | 녹차 |
| Earl Grey | 얼그레이 |
| Oolong | 우롱차 |
| Jasmine Tea | 자스민 차 |
| Herbal Tea | 허브티 |
| White Tea | 백차 |
| Tea-like | 차 같은 느낌 |

## 6.13 Winey / Fermented 계열

카테고리 색상: `#7B2D43`

설명: 와인, 발효, 숙성된 과일 느낌

| 영문 | 한글 |
|---|---|
| Red Wine | 레드 와인 |
| White Wine | 화이트 와인 |
| Brandy | 브랜디 |
| Rum | 럼 |
| Fermented Fruit | 발효 과일 |
| Raisin | 건포도 |
| Dried Fruit | 말린 과일 |
| Winey | 와이니 |
| Overripe Fruit | 과숙 과일 |

## 6.14 Earthy / Herbal 계열

카테고리 색상: `#6B705C`

설명: 흙, 허브, 식물성 느낌

| 영문 | 한글 |
|---|---|
| Herbal | 허브 |
| Grass | 풀 |
| Hay | 건초 |
| Earth | 흙 |
| Mushroom | 버섯 |
| Cedar | 시더우드 |
| Pine | 소나무 |
| Mint | 민트 |
| Vegetal | 채소 느낌 |

---

# 7. 향미 노트 색상 표현 방식

각 향미 노트는 고유한 색상값을 가진다.

예시:

```text
Lemon           #F6C945
Jasmine         #C8A2C8
DarkChocolate   #5C3A2E
Peach           #F4A261
Blueberry       #4C3A8C
```

## 7.1 MVP 추천: enum으로 관리

초기 구현에서는 `FlavorCategory`, `FlavorNote`를 enum으로 관리한다.

```java
public enum FlavorNote {
    LEMON("레몬", FlavorCategory.CITRUS, "#F6C945"),
    JASMINE("자스민", FlavorCategory.FLORAL, "#C8A2C8"),
    DARK_CHOCOLATE("다크 초콜릿", FlavorCategory.CHOCOLATE, "#5C3A2E");

    private final String displayName;
    private final FlavorCategory category;
    private final String color;
}
```

장점:

```text
구현이 쉽다.
DB 테이블이 늘어나지 않는다.
초기 MVP에 적합하다.
```

단점:

```text
향미 노트를 추가하려면 코드를 수정해야 한다.
사용자 커스텀 노트를 만들기 어렵다.
```

## 7.2 확장 추천: DB 테이블로 관리

확장 버전에서는 향미 노트를 DB 테이블로 관리할 수 있다.

```text
FlavorNote
- id
- name
- displayName
- category
- color
```

---

# 8. 선택한 향미 노트 그라데이션 표시

## 8.1 기능 개요

사용자가 브루잉 기록에서 여러 향미 노트를 선택하면, 해당 노트들의 색상을 조합해 그라데이션 바를 만든다.

예시:

```text
선택 노트:
Jasmine, Lemon, Peach

표시:
연보라 → 노랑 → 복숭아색
```

## 8.2 목록 카드 표시

브루잉 기록 목록 카드에서는 상세 텍스트보다 색상 그라데이션을 먼저 보여준다.

```text
[그라데이션 바]
Ethiopia Chelbesa
V60 · 2026.05.02
Jasmine · Lemon · Peach
```

## 8.3 상세 페이지 표시

브루잉 기록 상세 페이지에서는 더 크게 보여준다.

```text
오늘의 향미

[큰 그라데이션 바]

Jasmine
Lemon
Peach
```

## 8.4 CSS 구현 예시

선택한 노트 색상이 다음과 같다면:

```text
#C8A2C8
#F6C945
#F4A261
```

CSS는 다음처럼 생성한다.

```css
.flavor-gradient {
    background: linear-gradient(
        90deg,
        #C8A2C8,
        #F6C945,
        #F4A261
    );
}
```

향미 노트가 없는 경우 기본 색상은 다음을 사용한다.

```text
#E8E1D5
```

---

# 9. 추출 느낌 태그 설계

## 9.1 기능 개요

향미 노트가 커피의 향과 맛을 표현한다면, 추출 느낌 태그는 브루잉 결과의 상태를 표현한다.

예시:

```text
잘 내려졌다.
조금 연하다.
과추출 느낌이 있다.
산미가 튄다.
밸런스가 좋다.
```

---

## 9.2 태그 카테고리

### 긍정 태그

```text
깔끔함
맑음
균형 좋음
향이 좋음
단맛 좋음
산미 좋음
부드러움
여운 좋음
복합적
마시기 편함
```

### 추출 상태 태그

```text
언더 느낌
과추출 느낌
조금 연함
조금 진함
쓴맛 강함
산미가 튐
바디가 약함
떫음
잡미 있음
끝맛이 거침
```

### 질감 태그

```text
가벼움
부드러움
실키함
묵직함
쥬시함
크리미함
드라이함
물 같음
```

### 온도/시간 관련 태그

```text
식으니 좋아짐
따뜻할 때 좋음
식으니 산미 증가
시간 지나며 단맛 증가
처음보다 후반이 좋음
```

### 재시도 태그

```text
분쇄도 더 곱게
분쇄도 더 굵게
물 온도 낮추기
물 온도 높이기
추출 시간 줄이기
추출 시간 늘리기
원두량 늘리기
물량 줄이기
```

## 9.3 선택 개수 정책

```text
권장 선택: 1 ~ 3개
최대 선택: 5개
```

브루잉 기록 카드에서는 최대 3개만 표시한다.

```text
깔끔함 · 산미 좋음 · 식으니 좋아짐
```

상세 페이지에서는 전체 태그를 표시한다.

---

# 10. 데이터 모델 설계

## 10.1 BrewRecord 직접 저장 방식

MVP에서 가장 단순한 방식이다.

```text
BrewRecord
- id
- coffeeBean
- brewedDate
- brewMethod
- beanAmount
- waterAmount
- waterTemperature
- grindSize
- brewTimeSec
- rating
- acidity
- sweetness
- bitterness
- body
- aroma
- balance
- flavorNotes
- brewTags
- memo
```

여기서 `flavorNotes`, `brewTags`는 문자열로 저장할 수 있다.

```text
flavorNotes = "JASMINE,LEMON,PEACH"
brewTags = "CLEAN,BRIGHT_ACIDITY,GOOD_AFTER_COOLING"
```

장점:

```text
구현이 쉽다.
테이블이 늘어나지 않는다.
MVP에 적합하다.
```

단점:

```text
검색/필터링이 불편하다.
태그별 통계가 어렵다.
```

---

## 10.2 ElementCollection 방식

MVP와 확장 사이에서 추천하는 방식이다.

```java
@ElementCollection(targetClass = FlavorNote.class)
@Enumerated(EnumType.STRING)
private Set<FlavorNote> flavorNotes = new HashSet<>();

@ElementCollection(targetClass = BrewTag.class)
@Enumerated(EnumType.STRING)
private Set<BrewTag> brewTags = new HashSet<>();
```

장점:

```text
enum 기반으로 안전하게 관리할 수 있다.
검색/필터링 확장이 가능하다.
별도 Entity까지 만들 필요는 없다.
```

단점:

```text
별도 collection 테이블이 생긴다.
H2나 JPA 사용 시 enum 값 변경에 주의해야 한다.
```

---

## 10.3 별도 Entity 방식

확장 버전에서 추천한다.

```text
BrewRecord 1 : N BrewRecordFlavorNote
BrewRecord 1 : N BrewRecordTag
```

장점:

```text
검색/통계/확장에 좋다.
노트별 색상, 정렬 순서, 사용 빈도 관리가 쉽다.
```

단점:

```text
초기 구현이 복잡하다.
```

---

## 10.4 추천 방식

현재 프로젝트 단계에서는 다음을 추천한다.

```text
FlavorCategory enum
FlavorNote enum
BrewTag enum
BrewRecord에서는 ElementCollection으로 저장
```

이 방식은 다음 장점이 있다.

```text
사용자에게 다양한 선택지를 제공할 수 있다.
개발자는 enum으로 안전하게 관리할 수 있다.
나중에 검색/필터링도 가능하다.
별도 Entity까지는 만들지 않아도 된다.
```

---

# 11. enum 설계 초안

## 11.1 FlavorCategory

```java
public enum FlavorCategory {
    FLORAL("꽃", "#C8A2C8"),
    CITRUS("시트러스", "#F6C945"),
    BERRY("베리", "#9B1B5A"),
    STONE_FRUIT("핵과류", "#F4A261"),
    TROPICAL_FRUIT("열대과일", "#F9A03F"),
    APPLE_PEAR("사과/배", "#B7D968"),
    SWEET("단맛", "#D6A04D"),
    CHOCOLATE("초콜릿", "#5C3A2E"),
    NUTTY("견과", "#A47148"),
    ROASTED_SMOKY("로스팅/스모키", "#3B2F2F"),
    SPICE("향신료", "#B5532F"),
    TEA_LIKE("차", "#8A9A5B"),
    WINEY_FERMENTED("와인/발효", "#7B2D43"),
    EARTHY_HERBAL("허브/흙", "#6B705C");

    private final String displayName;
    private final String color;
}
```

## 11.2 FlavorNote 초안

```java
public enum FlavorNote {
    JASMINE("자스민", FlavorCategory.FLORAL, "#C8A2C8"),
    ROSE("장미", FlavorCategory.FLORAL, "#D8A7B1"),
    LAVENDER("라벤더", FlavorCategory.FLORAL, "#B497BD"),

    LEMON("레몬", FlavorCategory.CITRUS, "#F6C945"),
    LIME("라임", FlavorCategory.CITRUS, "#BFD641"),
    ORANGE("오렌지", FlavorCategory.CITRUS, "#F4A340"),
    GRAPEFRUIT("자몽", FlavorCategory.CITRUS, "#F26D6D"),

    STRAWBERRY("딸기", FlavorCategory.BERRY, "#E85D75"),
    RASPBERRY("라즈베리", FlavorCategory.BERRY, "#C2185B"),
    BLUEBERRY("블루베리", FlavorCategory.BERRY, "#4C3A8C"),
    BLACKBERRY("블랙베리", FlavorCategory.BERRY, "#2E1A47"),

    PEACH("복숭아", FlavorCategory.STONE_FRUIT, "#F4A261"),
    APRICOT("살구", FlavorCategory.STONE_FRUIT, "#F6B26B"),
    PLUM("자두", FlavorCategory.STONE_FRUIT, "#8E4585"),

    MANGO("망고", FlavorCategory.TROPICAL_FRUIT, "#F9A03F"),
    PINEAPPLE("파인애플", FlavorCategory.TROPICAL_FRUIT, "#F7D154"),
    LYCHEE("리치", FlavorCategory.TROPICAL_FRUIT, "#F7B7C4"),

    APPLE("사과", FlavorCategory.APPLE_PEAR, "#B7D968"),
    GREEN_APPLE("청사과", FlavorCategory.APPLE_PEAR, "#A7D948"),
    PEAR("배", FlavorCategory.APPLE_PEAR, "#D9E8A3"),

    HONEY("꿀", FlavorCategory.SWEET, "#D6A04D"),
    BROWN_SUGAR("흑설탕", FlavorCategory.SWEET, "#A66A2C"),
    CARAMEL("카라멜", FlavorCategory.SWEET, "#B7793E"),
    VANILLA("바닐라", FlavorCategory.SWEET, "#E8D8A8"),

    MILK_CHOCOLATE("밀크 초콜릿", FlavorCategory.CHOCOLATE, "#7B4B3A"),
    DARK_CHOCOLATE("다크 초콜릿", FlavorCategory.CHOCOLATE, "#4B2E2A"),
    CACAO("카카오", FlavorCategory.CHOCOLATE, "#5C3A2E"),

    ALMOND("아몬드", FlavorCategory.NUTTY, "#A47148"),
    HAZELNUT("헤이즐넛", FlavorCategory.NUTTY, "#8B5A2B"),
    PEANUT("땅콩", FlavorCategory.NUTTY, "#C08A4B"),

    TOAST("토스트", FlavorCategory.ROASTED_SMOKY, "#7A4A2E"),
    SMOKE("스모크", FlavorCategory.ROASTED_SMOKY, "#3B2F2F"),
    MALT("몰트", FlavorCategory.ROASTED_SMOKY, "#8A5A2B"),

    CINNAMON("시나몬", FlavorCategory.SPICE, "#B5532F"),
    CLOVE("정향", FlavorCategory.SPICE, "#6E2C1A"),
    GINGER("생강", FlavorCategory.SPICE, "#D89A3A"),

    BLACK_TEA("홍차", FlavorCategory.TEA_LIKE, "#8A6F3D"),
    GREEN_TEA("녹차", FlavorCategory.TEA_LIKE, "#8A9A5B"),
    EARL_GREY("얼그레이", FlavorCategory.TEA_LIKE, "#7A7F5A"),

    RED_WINE("레드 와인", FlavorCategory.WINEY_FERMENTED, "#7B2D43"),
    RAISIN("건포도", FlavorCategory.WINEY_FERMENTED, "#5E3023"),
    FERMENTED_FRUIT("발효 과일", FlavorCategory.WINEY_FERMENTED, "#8E3A59"),

    HERBAL("허브", FlavorCategory.EARTHY_HERBAL, "#6B705C"),
    GRASS("풀", FlavorCategory.EARTHY_HERBAL, "#7A8F5A"),
    EARTH("흙", FlavorCategory.EARTHY_HERBAL, "#5C4A3D");

    private final String displayName;
    private final FlavorCategory category;
    private final String color;
}
```

## 11.3 BrewTag 초안

```java
public enum BrewTag {
    CLEAN("깔끔함"),
    CLEAR("맑음"),
    BALANCED("균형 좋음"),
    GOOD_AROMA("향이 좋음"),
    GOOD_SWEETNESS("단맛 좋음"),
    GOOD_ACIDITY("산미 좋음"),
    SOFT("부드러움"),
    LONG_AFTERTASTE("여운 좋음"),
    COMPLEX("복합적"),
    EASY_TO_DRINK("마시기 편함"),

    UNDER_EXTRACTED("언더 느낌"),
    OVER_EXTRACTED("과추출 느낌"),
    WEAK("조금 연함"),
    STRONG("조금 진함"),
    TOO_BITTER("쓴맛 강함"),
    SHARP_ACIDITY("산미가 튐"),
    LIGHT_BODY("바디가 약함"),
    ASTRINGENT("떫음"),
    OFF_FLAVOR("잡미 있음"),
    ROUGH_FINISH("끝맛이 거침"),

    LIGHT("가벼움"),
    SILKY("실키함"),
    HEAVY("묵직함"),
    JUICY("쥬시함"),
    CREAMY("크리미함"),
    DRY("드라이함"),

    BETTER_WHEN_COOL("식으니 좋아짐"),
    GOOD_WHEN_HOT("따뜻할 때 좋음"),
    ACIDITY_INCREASED_WHEN_COOL("식으니 산미 증가"),
    SWEETER_OVER_TIME("시간 지나며 단맛 증가"),

    GRIND_FINER_NEXT("분쇄도 더 곱게"),
    GRIND_COARSER_NEXT("분쇄도 더 굵게"),
    LOWER_TEMP_NEXT("물 온도 낮추기"),
    HIGHER_TEMP_NEXT("물 온도 높이기"),
    SHORTER_TIME_NEXT("추출 시간 줄이기"),
    LONGER_TIME_NEXT("추출 시간 늘리기");

    private final String displayName;
}
```

---

# 12. 브루잉 기록 상세 페이지 표시 방식

## 12.1 목록 카드

브루잉 기록 목록에서는 간결하게 표시한다.

```text
[향미 그라데이션 바]

Ethiopia Chelbesa
V60 · 2026.05.02

Jasmine · Lemon · Peach
깔끔함 · 산미 좋음
```

## 12.2 상세 페이지

상세 페이지에서는 다음 순서로 표시한다.

```text
1. 원두 이름
2. 추출 날짜 / 도구
3. 향미 그라데이션
4. 선택한 향미 노트
5. 6각형 맛 프로파일
6. 추출 느낌 태그
7. 레시피 정보
8. 메모
```

---

# 13. 구현 우선순위

## 13.1 1차 구현

```text
FlavorCategory enum
FlavorNote enum
BrewTag enum
BrewRecord에 flavorNotes, brewTags 추가
브루잉 등록 폼에서 체크박스 또는 칩 선택
상세 페이지에서 선택한 노트 표시
```

## 13.2 2차 구현

```text
향미 노트 색상 표시
선택한 향미 노트 기반 그라데이션 바 표시
카테고리별 향미 노트 접기/펼치기
```

## 13.3 3차 구현

```text
6각형 레이더 차트 표시
슬라이더와 레이더 차트 연동
향미 노트 검색
향미 노트 사용 빈도 통계
```

---

# 14. 최종 추천 구조

현재 프로젝트에서는 다음 구조를 추천한다.

```text
FlavorCategory enum
FlavorNote enum
BrewTag enum
BrewRecord Entity
BrewRecordFlavorNote ElementCollection
BrewRecordBrewTag ElementCollection
```

이 방식이 좋은 이유는 다음과 같다.

```text
다양한 향미 노트를 제공할 수 있다.
색상 정보를 enum에 포함할 수 있다.
사용자 입장에서는 선택형 UX를 제공할 수 있다.
개발 입장에서는 DB 테이블을 과하게 늘리지 않아도 된다.
나중에 통계/검색으로 확장 가능하다.
```

최종적으로 사용자는 브루잉 기록을 다음처럼 남기게 된다.

```text
오늘의 커피:
Ethiopia Chelbesa

맛 프로파일:
산미 4 / 단맛 3 / 쓴맛 1 / 바디감 2 / 향 5 / 밸런스 4

향미 노트:
Jasmine, Lemon, Peach

추출 느낌:
깔끔함, 산미 좋음, 식으니 좋아짐

화면 표시:
연보라 → 노랑 → 복숭아색 그라데이션
```
