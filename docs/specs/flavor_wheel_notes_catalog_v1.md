# Flavor Wheel Notes Catalog v1

## 목적

`docs/flavor wheel.jpeg`에 저장된 SCA/WCR Coffee Taster's Flavor Wheel 계열 이미지를 기준으로, DB seed와 LLM 색상 추천 기준으로 사용할 향미 노트를 텍스트화한다.

원본 이미지 파일은 194x259px로 해상도가 낮아 외곽의 작은 텍스트를 직접 판독하기 어렵다. 그래서 이미지가 SCA/WCR 2016 Coffee Taster's Flavor Wheel 구조와 일치하는지 확인한 뒤, WCR Sensory Lexicon 및 SCA Flavor Wheel 설명 자료로 교차 확인했다.

## 산출물

- `docs/data/sca_flavor_wheel_notes.csv`
- 컬럼:
  - `id`: DB seed나 enum 생성에 사용할 안정적인 영문 snake_case id
  - `level`: `primary`, `secondary`, `leaf`
  - `primary_en`, `secondary_en`, `note_en`: 원본 휠 계층
  - `note_ko`: 앱 표시용 한글명 초안
  - `is_leaf`: 실제 향미 노트로 선택 가능한 최하위 항목 여부
  - `recommended_app_category`: 현재 앱의 `FlavorCategory`에 매핑한 값
  - `color_hex`: 휠 색상 계열과 향미 인상을 반영한 기준 색상
  - `source_confidence`: 원본 이미지/공개 자료 교차 확인 신뢰도
  - `remarks`: 계층 특이사항

## 기준 구조

SCA/WCR 휠은 9개 1차 계열로 구성된다.

- `Floral`
- `Fruity`
- `Sour/Fermented`
- `Green/Vegetative`
- `Other`
- `Roasted`
- `Spices`
- `Nutty/Cocoa`
- `Sweet`

WCR는 Sensory Lexicon이 Coffee Taster's Flavor Wheel 개정의 기반이며, 110개 커피 향미/아로마/질감 속성 및 강도 기준을 제공한다고 설명한다. SCA는 해당 Flavor Wheel을 다국어 디지털 제품으로 제공한다.

## 현재 앱 카테고리와의 차이

현재 앱의 `FlavorCategory`는 소비자 기록에 맞게 다음처럼 단순화되어 있다.

- `FLORAL`
- `CITRUS`
- `BERRY`
- `STONE_FRUIT`
- `TROPICAL_FRUIT`
- `APPLE_PEAR`
- `SWEET`
- `CHOCOLATE`
- `NUTTY`
- `ROASTED_SMOKY`
- `SPICE`
- `TEA_LIKE`
- `WINEY_FERMENTED`
- `EARTHY_HERBAL`

이 구조는 좋은 기록 UX에는 적합하지만 SCA/WCR 휠의 다음 계열을 완전히 표현하지는 못한다.

- `Sour/Fermented`: 산 관련 세부 항목과 발효/알코올 항목이 섞여 있음
- `Green/Vegetative`: 풋내, 식물성, 날것, 올리브 오일 계열
- `Other`: 종이/퀴퀴함, 화학적 결함, 짠맛/쓴맛 등
- `Nutty/Cocoa`: 현재 앱에서는 `NUTTY`, `CHOCOLATE`로 분리됨

DB를 확장한다면 원본 휠 계층은 별도 컬럼으로 보존하고, 앱 UI용 카테고리는 `recommended_app_category`처럼 별도로 매핑하는 편이 안전하다.

## 현재 enum 대비 새로 추가할 후보

현재 `FlavorNote` enum에 없거나 약하게만 표현되는 SCA/WCR 항목:

- 산/발효: `Sour Aromatics`, `Acetic Acid`, `Butyric Acid`, `Isovaleric Acid`, `Citric Acid`, `Malic Acid`, `Whiskey`, `Overripe`
- 그린/식물성: `Olive Oil`, `Raw`, `Under-ripe`, `Peapod`, `Fresh`, `Dark Green`, `Vegetative`, `Hay-like`, `Herb-like`
- 결함/기타: `Stale`, `Cardboard`, `Papery`, `Moldy/Damp`, `Musty/Dusty`, `Musty/Earthy`, `Animalic`, `Meaty/Brothy`, `Phenolic`
- 화학적: `Bitter`, `Salty`, `Medicinal`, `Petroleum`, `Skunky`, `Rubber`
- 로스티드: `Pipe Tobacco`, `Acrid`, `Ashy`, `Brown Roast`, `Grain`, `Malt`
- 향신료: `Pungent`, `Pepper`, `Anise`
- 단맛: `Molasses`, `Caramelized`, `Vanillin`, `Overall Sweet`, `Sweet Aromatics`

## 색상 추천 기준

CSV의 `color_hex`는 원본 휠의 색상 계열과 현재 앱의 팔레트를 함께 고려한 기준값이다. LLM으로 신규 향미 색상을 추천할 때는 다음 순서가 좋다.

1. 신규 향미를 SCA/WCR 계층 중 가장 가까운 `leaf` 또는 `secondary`에 매핑한다.
2. 해당 행의 `color_hex`를 기본 색상으로 사용한다.
3. 더 구체적인 향미라면 같은 계열 안에서 밝기/채도만 조정한다.
4. 현재 앱 UI에서는 `recommended_app_category`의 색상과 너무 멀어지지 않게 보정한다.

예시:

- `청귤`: `Citrus Fruit` 계열, `Lime`/`Orange` 사이, 노란빛 녹색 또는 밝은 주황색
- `라임 껍질`: `Citrus Fruit > Lime`, 라임색 기반에서 껍질 느낌으로 약간 짙게
- `흑맥주`: `Roasted > Burnt` 또는 `Sour/Fermented > Alcohol/Fermented`, 짙은 갈색
- `망고스틴`: `Fruity > Other Fruit`, 열대과일 계열의 분홍/주황색

## 주의점

- `leaf`만 DB에 “선택 가능한 향미 노트”로 넣으면 UX가 깔끔하다.
- `primary`, `secondary`는 LLM 분류 기준, 검색/필터, 향미 추천 보조 데이터로 쓰는 것이 좋다.
- 결함 계열인 `Chemical`, `Papery/Musty`, 일부 `Sour/Fermented` 항목은 일반 사용자에게 노출할 때 부정적 표현이 강할 수 있으므로 UI에서는 접어두거나 “전문/결함 노트”로 분리하는 편이 좋다.

## 참고한 공개 자료

- World Coffee Research Sensory Lexicon: Coffee Taster's Flavor Wheel의 기반 자료이며 110개 속성을 제공한다고 설명한다.
- SCA Digital Products Flavor Wheel: Coffee Taster's Flavor Wheel 공식 배포 페이지이며 한국어 버전이 있음을 확인했다.
- SCA/WCR Flavor Wheel 해설 및 인터랙티브 자료: 9개 중심 카테고리, 중심에서 바깥으로 읽는 구조, 주요 하위 계층을 교차 확인하는 데 사용했다.
