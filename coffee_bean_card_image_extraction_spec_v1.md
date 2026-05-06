# 원두 카드 이미지 기반 원두 등록 자동 입력 기능 구현 명세서 v1.0

## 1. 문서 개요

이 문서는 `brewing-coffee` 프로젝트에서 **원두 정보 카드 이미지를 업로드하면 OCR/정보 추출을 통해 원두 등록 폼을 자동으로 채워주는 기능**을 구현하기 위한 상세 명세서이다.

이 기능의 핵심은 **자동 저장이 아니라 자동 입력 보조**이다. 사용자가 원두 카드 이미지를 업로드하면 서버가 이미지에서 텍스트를 추출하고, 추출된 텍스트를 기반으로 원두 정보 후보를 만든 뒤, 기존 원두 등록 폼에 값을 채워준다. 사용자는 채워진 값을 직접 확인하고 수정한 뒤 기존 등록 버튼으로 최종 저장한다.

---

## 2. 핵심 사용자 흐름

```text
원두 등록 페이지 접속
→ [사진으로 등록] 버튼 클릭
→ 원두 카드 이미지 업로드
→ OCR로 텍스트 추출
→ 추출 텍스트 기반 원두 정보 후보 생성
→ 기존 원두 등록 폼에 값 자동 입력
→ 사용자가 확인/수정
→ [등록] 버튼 클릭
→ CoffeeBean 저장
```

중요 원칙:

```text
사진 업로드 결과를 바로 DB에 저장하지 않는다.
자동 추출 결과는 임시 후보 데이터로만 사용한다.
최종 저장은 사용자가 확인한 CoffeeBeanCreateForm 값으로만 수행한다.
OCR/파싱 실패 시에도 사용자가 직접 입력할 수 있어야 한다.
```

---

## 3. 구현 범위

### 3.1 1차 구현 범위

1차 구현에서는 **실제 OCR 연동 전 전체 흐름 검증**을 우선한다.

```text
원두 등록 폼 상단에 [사진으로 등록] 영역 추가
이미지 업로드 폼 추가
이미지 파일 서버 전송
OCR 처리용 Service 인터페이스 작성
Mock OCR 구현체 작성
OCR 결과 rawText 표시
rawText 기반 규칙 파서 구현
CoffeeBeanCreateForm에 추출 결과 자동 주입
사용자가 확인 후 기존 /coffee-beans 저장 로직 사용
```

1차 목표:

```text
이미지 업로드
→ Mock 텍스트 반환
→ 파서 실행
→ 원두 등록 폼 자동 입력
→ 사용자가 수정 후 저장
```

### 3.2 2차 구현 범위

```text
Tesseract.js 또는 PaddleOCR 연동
가공 방식 추출 개선
용량 추출 개선
가격 추출 개선
로스팅 날짜 추출 개선
국가 코드 매핑
향미 노트 매핑
```

### 3.3 3차 구현 범위

```text
OCR rawText를 LLM에 전달
CoffeeBeanCandidate JSON으로 구조화
확실하지 않은 값은 null 처리
사용자 확인 폼에 자동 입력
```

---

## 4. 핵심 정책

### 4.1 자동 저장 금지

잘못된 흐름:

```text
이미지 업로드
→ OCR
→ CoffeeBean 바로 저장
```

올바른 흐름:

```text
이미지 업로드
→ OCR
→ CoffeeBeanCreateForm 자동 입력
→ 사용자 확인/수정
→ CoffeeBean 저장
```

### 4.2 Candidate와 Entity 분리

```text
CoffeeBeanCardExtractResult
= OCR 및 정보 추출 결과

CoffeeBeanCreateForm
= 사용자가 최종 확인하는 등록 폼

CoffeeBean
= 최종 저장되는 엔티티
```

자동 추출 결과는 저장 데이터가 아니라 **폼을 채우기 위한 후보 데이터**이다.

### 4.3 실패 허용 UX

OCR이나 파싱은 항상 실패할 수 있다.

허용해야 하는 상황:

```text
이미지에서 텍스트를 일부만 추출
원두명만 추출
가공방식만 추출
향미 노트만 추출
아무 필드도 추출하지 못함
```

실패하더라도 원두 등록 폼은 정상적으로 보여야 하며, 사용자가 직접 입력할 수 있어야 한다.

---

## 5. URL 설계

### 5.1 기존 원두 등록 URL

```text
GET  /coffee-beans/new
POST /coffee-beans
```

역할:

```text
GET /coffee-beans/new
→ 빈 원두 등록 폼 표시

POST /coffee-beans
→ 사용자가 작성한 원두 등록 폼 저장
```

### 5.2 추가 URL

```text
POST /coffee-beans/card-extraction
```

역할:

```text
이미지 파일 업로드
OCR 실행
정보 추출
CoffeeBeanCreateForm 생성
기존 원두 등록 폼으로 다시 렌더링
```

중요:

```text
POST /coffee-beans/card-extraction은 DB 저장을 하지 않는다.
```

### 5.3 앱 전환 대비 API 확장

나중에 앱에서도 같은 기능을 사용할 수 있도록 REST API를 추가할 수 있다.

```text
POST /api/coffee-beans/card-extraction
```

응답 예시:

```json
{
  "rawText": "Ethiopia Yirgacheffe Kochere...",
  "candidate": {
    "name": "Ethiopia Yirgacheffe Kochere",
    "roastery": "Fritz Coffee",
    "originCountryCode": "ET",
    "originCountryName": "Ethiopia",
    "processType": "WASHED",
    "weight": 200,
    "price": 18000,
    "roastedDate": "2026-05-01"
  }
}
```

MVP에서는 Thymeleaf 렌더링 방식으로 시작하고, 앱 전환 시 API 방식으로 확장한다.

---

## 6. 패키지 구조

추천 패키지 구조:

```text
com.hsg.coffee.domain.coffeeBean
 ├── controller
 │   └── CoffeeBeanController.java
 ├── dto
 │   ├── CoffeeBeanCreateForm.java
 │   ├── CoffeeBeanCardExtractResult.java
 │   └── CoffeeBeanCardTextParseResult.java
 ├── entity
 │   ├── CoffeeBean.java
 │   ├── ProcessType.java
 │   └── FlavorNote.java
 ├── repository
 │   └── CoffeeBeanRepository.java
 └── service
     ├── CoffeeBeanService.java
     ├── CoffeeBeanCardExtractionService.java
     ├── CoffeeBeanCardOcrService.java
     ├── MockCoffeeBeanCardOcrService.java
     └── CoffeeBeanCardTextParser.java
```

OCR 구현체를 교체하기 쉽게 인터페이스를 분리한다.

---

## 7. DTO 설계

### 7.1 CoffeeBeanCardExtractResult

OCR 및 파싱 결과를 담는 DTO이다.

```java
@Getter
@AllArgsConstructor
public class CoffeeBeanCardExtractResult {

    private String rawText;
    private CoffeeBeanCreateForm form;
    private List<String> warnings;
}
```

필드 의미:

| 필드 | 설명 |
|---|---|
| `rawText` | OCR로 추출된 원문 텍스트 |
| `form` | 자동 입력된 원두 등록 폼 |
| `warnings` | 추출 실패/주의 메시지 |

### 7.2 CoffeeBeanCardTextParseResult

규칙 기반 파서 또는 LLM 파서가 반환하는 중간 후보 DTO이다.

```java
@Getter
@Setter
public class CoffeeBeanCardTextParseResult {

    private String name;
    private String roastery;
    private String originCountryCode;
    private String originCountryName;
    private String region;
    private String farm;
    private String variety;
    private ProcessType processType;
    private Set<FlavorNote> flavorNotes = new LinkedHashSet<>();
    private String customFlavorNotes;
    private Integer weight;
    private Integer price;
    private LocalDate roastedDate;
    private List<String> warnings = new ArrayList<>();
}
```

### 7.3 CoffeeBeanCreateForm 수정

기존 `CoffeeBeanCreateForm`에 아래 필드를 추가할 수 있다.

```java
private String extractedRawText;
private Boolean extractedFromCard;
```

필드 의미:

| 필드 | 설명 |
|---|---|
| `extractedRawText` | OCR 원문 확인용 |
| `extractedFromCard` | 사진 자동 입력으로 생성된 폼인지 여부 |

이 필드는 엔티티에 저장하지 않는다. 원본 OCR 텍스트 저장이 필요하면 나중에 별도 테이블로 분리한다.

---

## 8. Controller 설계

### 8.1 CoffeeBeanController 메서드 추가

```java
@PostMapping("/coffee-beans/card-extraction")
public String extractFromCard(
        @RequestParam("image") MultipartFile image,
        Model model
) {
    CoffeeBeanCardExtractResult result = coffeeBeanCardExtractionService.extract(image);

    model.addAttribute("coffeeBeanCreateForm", result.getForm());
    model.addAttribute("extractedRawText", result.getRawText());
    model.addAttribute("extractionWarnings", result.getWarnings());

    addCoffeeBeanFormOptions(model);

    return "coffeeBeans/form";
}
```

### 8.2 addCoffeeBeanFormOptions 메서드

원두 등록 폼과 사진 추출 후 렌더링 모두 같은 옵션 데이터를 필요로 한다.

```java
private void addCoffeeBeanFormOptions(Model model) {
    model.addAttribute("processTypes", ProcessType.values());
    model.addAttribute("flavorNotes", FlavorNote.values());
    model.addAttribute("countryInfos", CountryInfo.values());
    model.addAttribute("purchasePlaces", purchasePlaceService.findAll());
}
```

프로젝트에 아직 `CountryInfo` 또는 `FlavorNote`가 없다면 현재 존재하는 enum/옵션 기준으로 조정한다.

### 8.3 기존 createForm 메서드 수정

```java
@GetMapping("/coffee-beans/new")
public String createForm(Model model) {
    model.addAttribute("coffeeBeanCreateForm", new CoffeeBeanCreateForm());
    addCoffeeBeanFormOptions(model);
    return "coffeeBeans/form";
}
```

### 8.4 기존 create 메서드는 변경 최소화

사진 등록 기능을 추가하더라도 최종 저장은 기존 `/coffee-beans` 저장 로직을 그대로 사용한다.

```java
@PostMapping("/coffee-beans")
public String create(
        @Valid @ModelAttribute CoffeeBeanCreateForm form,
        BindingResult bindingResult,
        Model model
) {
    if (bindingResult.hasErrors()) {
        addCoffeeBeanFormOptions(model);
        return "coffeeBeans/form";
    }

    Long coffeeBeanId = coffeeBeanService.save(form);
    return "redirect:/coffee-beans/" + coffeeBeanId;
}
```

---

## 9. Service 설계

### 9.1 CoffeeBeanCardExtractionService

사진 자동 입력 기능의 전체 흐름을 담당한다.

```java
@Service
@RequiredArgsConstructor
public class CoffeeBeanCardExtractionService {

    private final CoffeeBeanCardOcrService ocrService;
    private final CoffeeBeanCardTextParser textParser;

    public CoffeeBeanCardExtractResult extract(MultipartFile image) {
        validateImage(image);

        String rawText = ocrService.extractText(image);
        CoffeeBeanCardTextParseResult parseResult = textParser.parse(rawText);

        CoffeeBeanCreateForm form = toCreateForm(parseResult);
        form.setExtractedRawText(rawText);
        form.setExtractedFromCard(true);

        return new CoffeeBeanCardExtractResult(
                rawText,
                form,
                parseResult.getWarnings()
        );
    }

    private CoffeeBeanCreateForm toCreateForm(CoffeeBeanCardTextParseResult parseResult) {
        CoffeeBeanCreateForm form = new CoffeeBeanCreateForm();

        form.setName(parseResult.getName());
        form.setRoastery(parseResult.getRoastery());
        form.setOriginCountryCode(parseResult.getOriginCountryCode());
        form.setOriginCountryName(parseResult.getOriginCountryName());
        form.setRegion(parseResult.getRegion());
        form.setFarm(parseResult.getFarm());
        form.setVariety(parseResult.getVariety());
        form.setProcessType(parseResult.getProcessType());
        form.setFlavorNotes(parseResult.getFlavorNotes());
        form.setCustomFlavorNotes(parseResult.getCustomFlavorNotes());
        form.setWeight(parseResult.getWeight());
        form.setPrice(parseResult.getPrice());
        form.setRoastedDate(parseResult.getRoastedDate());

        return form;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("원두 카드 이미지를 업로드해주세요.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        long maxSize = 5 * 1024 * 1024;
        if (image.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 파일은 5MB 이하만 업로드할 수 있습니다.");
        }
    }
}
```

### 9.2 CoffeeBeanCardOcrService 인터페이스

OCR 구현체를 쉽게 교체하기 위한 인터페이스이다.

```java
public interface CoffeeBeanCardOcrService {

    String extractText(MultipartFile image);
}
```

### 9.3 MockCoffeeBeanCardOcrService

MVP 초기에는 실제 OCR 없이 Mock으로 전체 흐름을 먼저 검증한다.

```java
@Service
@Profile("local")
public class MockCoffeeBeanCardOcrService implements CoffeeBeanCardOcrService {

    @Override
    public String extractText(MultipartFile image) {
        return """
                Ethiopia Yirgacheffe Kochere
                Washed
                Jasmine, Lemon, Peach
                Roasted by Fritz Coffee
                200g
                18,000 KRW
                Roasted 2026.05.01
                """;
    }
}
```

이 구현체를 사용하면 OCR API 연동 전에도 전체 흐름을 개발할 수 있다.

### 9.4 이후 OCR 구현체

나중에 OCR 방식에 따라 구현체를 추가한다.

```text
TesseractCoffeeBeanCardOcrService
PaddleCoffeeBeanCardOcrService
GoogleVisionCoffeeBeanCardOcrService
OcrSpaceCoffeeBeanCardOcrService
```

서비스 코드는 인터페이스만 의존하므로 구현체 교체가 쉽다.

---

## 10. 규칙 기반 파서 설계

### 10.1 CoffeeBeanCardTextParser

OCR rawText에서 원두 정보 후보를 추출한다.

```java
@Component
public class CoffeeBeanCardTextParser {

    public CoffeeBeanCardTextParseResult parse(String rawText) {
        CoffeeBeanCardTextParseResult result = new CoffeeBeanCardTextParseResult();

        String normalizedText = normalize(rawText);

        result.setProcessType(extractProcessType(normalizedText));
        result.setWeight(extractWeight(normalizedText));
        result.setPrice(extractPrice(normalizedText));
        result.setRoastedDate(extractRoastedDate(normalizedText));
        extractCountry(normalizedText, result);
        extractFlavorNotes(normalizedText, result);
        result.setName(extractName(normalizedText));
        result.setRoastery(extractRoastery(normalizedText));

        return result;
    }
}
```

### 10.2 normalize

```java
private String normalize(String rawText) {
    if (rawText == null) {
        return "";
    }

    return rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceAll("[ ]{2,}", " ")
            .trim();
}
```

### 10.3 가공 방식 추출

대상 키워드:

```text
Washed
Wash
Natural
Honey
Anaerobic
Anaerobic Natural
Anaerobic Washed
Pulped Natural
Wet Hulled
워시드
내추럴
허니
무산소
```

예시:

```java
private ProcessType extractProcessType(String text) {
    String lower = text.toLowerCase();

    if (lower.contains("anaerobic") || text.contains("무산소")) {
        return ProcessType.ANAEROBIC;
    }
    if (lower.contains("washed") || lower.contains("wash") || text.contains("워시드")) {
        return ProcessType.WASHED;
    }
    if (lower.contains("natural") || text.contains("내추럴")) {
        return ProcessType.NATURAL;
    }
    if (lower.contains("honey") || text.contains("허니")) {
        return ProcessType.HONEY;
    }

    return null;
}
```

프로젝트의 실제 `ProcessType` enum 값에 맞게 수정한다.

### 10.4 용량 추출

대상 예시:

```text
200g
200 g
100g
1kg
```

```java
private Integer extractWeight(String text) {
    Pattern gramPattern = Pattern.compile("(\\d{2,4})\\s*g", Pattern.CASE_INSENSITIVE);
    Matcher gramMatcher = gramPattern.matcher(text);

    if (gramMatcher.find()) {
        return Integer.parseInt(gramMatcher.group(1));
    }

    Pattern kgPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kg", Pattern.CASE_INSENSITIVE);
    Matcher kgMatcher = kgPattern.matcher(text);

    if (kgMatcher.find()) {
        BigDecimal kg = new BigDecimal(kgMatcher.group(1));
        return kg.multiply(BigDecimal.valueOf(1000)).intValue();
    }

    return null;
}
```

### 10.5 가격 추출

대상 예시:

```text
18,000원
18000 KRW
₩18,000
KRW 18000
```

```java
private Integer extractPrice(String text) {
    Pattern pattern = Pattern.compile(
            "(?:₩|KRW)?\\s*(\\d{1,3}(?:,\\d{3})+|\\d{4,6})\\s*(?:원|KRW)?",
            Pattern.CASE_INSENSITIVE
    );
    Matcher matcher = pattern.matcher(text);

    while (matcher.find()) {
        String number = matcher.group(1).replace(",", "");
        int value = Integer.parseInt(number);

        if (value >= 1000) {
            return value;
        }
    }

    return null;
}
```

주의:

```text
가격과 용량 숫자가 혼동될 수 있다.
200g의 200을 가격으로 잡지 않도록 최소값을 둔다.
```

### 10.6 로스팅 날짜 추출

지원 형식:

```text
yyyy.MM.dd
yyyy-MM-dd
yyyy/MM/dd
yy.MM.dd
```

```java
private LocalDate extractRoastedDate(String text) {
    Pattern pattern = Pattern.compile("(20\\d{2})[./-](\\d{1,2})[./-](\\d{1,2})");
    Matcher matcher = pattern.matcher(text);

    if (matcher.find()) {
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));

        return LocalDate.of(year, month, day);
    }

    return null;
}
```

확장:

```text
26.05.01 같은 2자리 연도는 20xx로 보정
로스팅 키워드 주변 날짜를 우선 선택
여러 날짜가 있으면 로스팅/제조 키워드와 가장 가까운 날짜 선택
```

### 10.7 국가 추출

국가는 `CountryInfo` enum을 기준으로 매핑한다.

```java
private void extractCountry(String text, CoffeeBeanCardTextParseResult result) {
    for (CountryInfo countryInfo : CountryInfo.values()) {
        if (text.contains(countryInfo.getEnglishName())
                || text.contains(countryInfo.getKoreanName())) {
            result.setOriginCountryCode(countryInfo.getCode());
            result.setOriginCountryName(countryInfo.getEnglishName());
            return;
        }
    }
}
```

초기 지원 국가:

```text
Ethiopia / 에티오피아 / ET
Colombia / 콜롬비아 / CO
Brazil / 브라질 / BR
Kenya / 케냐 / KE
Guatemala / 과테말라 / GT
Costa Rica / 코스타리카 / CR
Panama / 파나마 / PA
Rwanda / 르완다 / RW
Honduras / 온두라스 / HN
Peru / 페루 / PE
Indonesia / 인도네시아 / ID
```

### 10.8 향미 노트 추출

`FlavorNote` enum이 있다면 displayName 또는 영문 키워드와 매칭한다.

```java
private void extractFlavorNotes(String text, CoffeeBeanCardTextParseResult result) {
    Set<FlavorNote> notes = new LinkedHashSet<>();
    String lower = text.toLowerCase();

    for (FlavorNote note : FlavorNote.values()) {
        String enumKeyword = note.name().toLowerCase().replace("_", " ");
        if (lower.contains(enumKeyword) || text.contains(note.getDisplayName())) {
            notes.add(note);
        }
    }

    result.setFlavorNotes(notes);
}
```

주의:

```text
향미 노트는 OCR 오타가 자주 발생할 수 있다.
완전 자동 매핑보다 후보로 제안하는 방식이 안전하다.
```

### 10.9 원두명 추출

규칙 기반으로 가장 어려운 필드이다.

MVP 정책:

```text
첫 번째 의미 있는 줄을 원두명 후보로 사용한다.
단, 가격/용량/가공방식/로스팅 날짜만 있는 줄은 제외한다.
```

```java
private String extractName(String text) {
    return text.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .filter(line -> !isMetadataLine(line))
            .findFirst()
            .orElse(null);
}

private boolean isMetadataLine(String line) {
    String lower = line.toLowerCase();

    return lower.contains("washed")
            || lower.contains("natural")
            || lower.contains("honey")
            || lower.contains("roasted")
            || lower.matches(".*\\d+\\s*g.*")
            || lower.matches(".*\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}.*");
}
```

### 10.10 로스터리 추출

우선 지원할 키워드:

```text
Roasted by
Roaster
Roastery
로스터리
볶은 곳
```

```java
private String extractRoastery(String text) {
    Pattern pattern = Pattern.compile(
            "(?:Roasted by|Roaster|Roastery)\\s*[:\\-]?\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );
    Matcher matcher = pattern.matcher(text);

    if (matcher.find()) {
        return matcher.group(1).trim();
    }

    return null;
}
```

MVP에서는 로스터리가 추출되지 않아도 사용자가 직접 입력하게 둔다.

---

## 11. Thymeleaf 화면 설계

### 11.1 원두 등록 폼 상단 영역

`coffeeBeans/form.html` 상단에 사진 등록 영역을 추가한다.

```html
<section class="card-extraction-section">
    <div class="card-extraction-header">
        <h2>원두 카드 사진으로 자동 입력</h2>
        <p>원두 카드 이미지를 업로드하면 가능한 정보를 자동으로 채워드려요.</p>
    </div>

    <form th:action="@{/coffee-beans/card-extraction}"
          method="post"
          enctype="multipart/form-data"
          class="card-extraction-form">
        <input type="file"
               name="image"
               accept="image/*"
               required>
        <button type="submit" class="btn btn-outline-primary">
            사진으로 등록
        </button>
    </form>
</section>
```

### 11.2 OCR 결과 표시 영역

사진 추출 결과가 있는 경우에만 표시한다.

```html
<section class="extracted-result-section"
         th:if="${extractedRawText != null}">
    <h3>추출된 텍스트</h3>
    <p>아래 텍스트를 기준으로 원두 정보가 자동 입력되었어요.</p>

    <textarea readonly
              class="extracted-raw-text"
              th:text="${extractedRawText}">
    </textarea>
</section>
```

### 11.3 경고 메시지 표시

```html
<div class="extraction-warning-box"
     th:if="${extractionWarnings != null and !extractionWarnings.isEmpty()}">
    <p>일부 정보는 자동으로 찾지 못했어요. 폼에서 직접 확인해주세요.</p>
    <ul>
        <li th:each="warning : ${extractionWarnings}"
            th:text="${warning}">
        </li>
    </ul>
</div>
```

### 11.4 폼 자동 입력

기존 원두 등록 폼은 그대로 사용한다.

```html
<input type="text"
       th:field="*{name}"
       placeholder="원두 이름">
```

`coffeeBeanCreateForm`에 값이 들어 있으면 Thymeleaf가 자동으로 값을 표시한다.

---

## 12. CSS 설계

```css
.card-extraction-section {
    margin-bottom: 32px;
    padding: 24px;
    border: 1px solid #E6DED2;
    border-radius: 20px;
    background: #FFFDF8;
}

.card-extraction-header h2 {
    margin: 0 0 8px;
    color: #3A2117;
}

.card-extraction-header p {
    margin: 0 0 16px;
    color: #7A6A58;
}

.card-extraction-form {
    display: flex;
    gap: 12px;
    align-items: center;
}

.extracted-result-section {
    margin-bottom: 24px;
    padding: 20px;
    border-radius: 16px;
    background: #F7F1E8;
}

.extracted-raw-text {
    width: 100%;
    min-height: 160px;
    resize: vertical;
    border: 1px solid #E0D5C8;
    border-radius: 12px;
    padding: 16px;
    font-family: monospace;
    background: #FFFFFF;
}

.extraction-warning-box {
    margin-bottom: 24px;
    padding: 16px;
    border-radius: 14px;
    background: #FFF4E5;
    color: #7A4B00;
}
```

---

## 13. 예외 처리

### 13.1 업로드 파일이 없는 경우

메시지:

```text
원두 카드 이미지를 업로드해주세요.
```

### 13.2 이미지가 아닌 파일인 경우

메시지:

```text
이미지 파일만 업로드할 수 있습니다.
```

### 13.3 파일 크기 초과

기준:

```text
5MB 이하
```

메시지:

```text
이미지 파일은 5MB 이하만 업로드할 수 있습니다.
```

### 13.4 OCR 실패

메시지:

```text
이미지에서 텍스트를 추출하지 못했어요. 사진을 다시 찍거나 직접 입력해주세요.
```

처리:

```text
빈 CoffeeBeanCreateForm 반환
extractionWarnings에 실패 메시지 추가
```

### 13.5 파싱 실패

OCR rawText는 있지만 필드 추출에 실패한 경우:

```text
추출된 텍스트를 기준으로 일부 정보만 자동 입력했어요.
나머지는 직접 확인해주세요.
```

---

## 14. 파일 업로드 설정

### 14.1 application.yml

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 6MB
```

### 14.2 이미지 저장 정책

MVP에서는 원본 이미지를 저장하지 않는다.

```text
이미지 업로드
→ OCR 처리
→ 결과 반환
→ 이미지 파일 저장하지 않음
```

이유:

```text
개인정보/저장공간 부담 감소
구현 단순
OCR 결과만 필요
```

나중에 원두 카드 이미지를 보관하고 싶다면 다음 기능으로 확장한다.

```text
CoffeeBeanCardImage
- id
- coffeeBean
- imagePath
- originalFilename
- createdAt
```

---

## 15. 보안 및 운영 정책

### 15.1 파일 확장자 제한

MVP 허용:

```text
jpg
jpeg
png
webp
```

### 15.2 Content-Type 검증

`MultipartFile.getContentType()`이 `image/`로 시작하는지 확인한다.

### 15.3 API 키 관리

외부 OCR API나 LLM API를 사용할 경우 API 키는 절대 프론트에 노출하지 않는다.

올바른 구조:

```text
브라우저
→ Spring Boot 서버
→ 외부 OCR/LLM API
```

잘못된 구조:

```text
브라우저
→ 외부 OCR/LLM API 직접 호출
```

---

## 16. 테스트 명세

### 16.1 Service 테스트

대상:

```text
CoffeeBeanCardTextParser
CoffeeBeanCardExtractionService
```

테스트 케이스:

```text
Washed 키워드가 있으면 ProcessType.WASHED 추출
Natural 키워드가 있으면 ProcessType.NATURAL 추출
200g이 있으면 weight=200 추출
18,000원이 있으면 price=18000 추출
2026.05.01이 있으면 roastedDate=2026-05-01 추출
Ethiopia가 있으면 originCountryCode=ET 추출
Jasmine, Lemon이 있으면 FlavorNote 매핑
rawText가 비어 있어도 예외 없이 빈 후보 반환
```

### 16.2 Controller 테스트

대상:

```text
POST /coffee-beans/card-extraction
```

테스트 케이스:

```text
이미지 파일 업로드 시 form.html 반환
추출 결과가 model에 포함됨
이미지가 없으면 오류 메시지 반환
이미지가 아닌 파일이면 오류 메시지 반환
```

### 16.3 수동 테스트

```text
/coffee-beans/new 접속
사진으로 등록 버튼이 보인다.
이미지를 선택할 수 있다.
사진 업로드 후 같은 등록 폼으로 돌아온다.
원두명/가공방식/용량/가격 등이 자동 입력된다.
추출된 텍스트가 표시된다.
사용자가 값을 수정할 수 있다.
등록 버튼을 누르면 CoffeeBean이 저장된다.
저장된 원두 상세 페이지로 이동한다.
```

---

## 17. 구현 순서

### 17.1 1단계: DTO 및 인터페이스 작성

```text
CoffeeBeanCardExtractResult
CoffeeBeanCardTextParseResult
CoffeeBeanCardOcrService
```

커밋:

```text
feat: 원두 카드 추출 DTO와 OCR 인터페이스 추가
```

### 17.2 2단계: Mock OCR 구현

```text
MockCoffeeBeanCardOcrService
CoffeeBeanCardExtractionService
CoffeeBeanCardTextParser 기본 구조
```

커밋:

```text
feat: 원두 카드 Mock OCR 추출 흐름 추가
```

### 17.3 3단계: 원두 등록 Controller 연동

```text
POST /coffee-beans/card-extraction
CoffeeBeanController 메서드 추가
form options 공통 메서드 분리
```

커밋:

```text
feat: 원두 카드 이미지 자동 입력 컨트롤러 추가
```

### 17.4 4단계: 원두 등록 화면 수정

```text
coffeeBeans/form.html 상단에 사진 등록 영역 추가
OCR rawText 표시 영역 추가
extractionWarnings 표시 영역 추가
```

커밋:

```text
feat: 원두 등록 화면에 사진 자동 입력 UI 추가
```

### 17.5 5단계: 규칙 기반 파서 구현

```text
가공 방식 추출
용량 추출
가격 추출
날짜 추출
국가 추출
향미 노트 추출
원두명 후보 추출
로스터리 후보 추출
```

커밋:

```text
feat: 원두 카드 텍스트 파서 추가
```

### 17.6 6단계: 실제 OCR 연동

아래 중 하나를 선택한다.

```text
Tesseract.js
PaddleOCR
OCR.space
Google Vision OCR
```

추천 구현 방식:

```text
CoffeeBeanCardOcrService 인터페이스 구현체 추가
application.yml에서 OCR provider 선택
```

커밋:

```text
feat: 원두 카드 OCR 연동 추가
```

---

## 18. 완료 기준

아래 조건을 만족하면 1차 기능 완료로 본다.

```text
원두 등록 페이지에 [사진으로 등록] 기능이 있다.
이미지 파일을 업로드할 수 있다.
이미지 업로드 후 원두 등록 폼으로 돌아온다.
OCR rawText가 화면에 표시된다.
추출된 정보가 원두 등록 폼에 자동 입력된다.
자동 입력된 값은 사용자가 수정할 수 있다.
등록 버튼을 누르기 전까지 CoffeeBean은 저장되지 않는다.
사용자가 등록 버튼을 누르면 기존 CoffeeBean 저장 로직으로 저장된다.
OCR 또는 파싱 실패 시에도 직접 입력이 가능하다.
```

---

## 19. 이후 확장 방향

### 19.1 LLM 구조화 추가

OCR rawText를 LLM으로 보내서 JSON 후보를 생성한다.

```text
rawText
→ LLM
→ CoffeeBeanCardTextParseResult
```

프롬프트 정책:

```text
확실하지 않은 값은 null로 둔다.
존재하지 않는 값을 추측하지 않는다.
지원 enum에 없는 가공방식은 OTHER 또는 null로 둔다.
향미 노트는 enum 매핑 가능한 값과 custom 값으로 분리한다.
```

### 19.2 앱 전환 대비 API 추가

웹/앱 공통 사용을 위해 REST API를 추가한다.

```text
POST /api/coffee-beans/card-extraction
```

웹에서는 Thymeleaf 폼 재렌더링을 사용하고, 앱에서는 JSON API를 사용한다.

### 19.3 이미지 보관 기능

원두 카드 이미지를 저장하고 싶다면 아래 기능을 추가한다.

```text
CoffeeBeanCardImage Entity
이미지 파일 저장소
CoffeeBean과 이미지 연결
상세 페이지에서 카드 이미지 표시
```

---

## 20. Codex 구현 시 주의사항

Codex는 아래 원칙을 반드시 지켜야 한다.

```text
기존 CoffeeBean 저장 로직을 깨지 않는다.
POST /coffee-beans/card-extraction에서는 DB 저장을 하지 않는다.
자동 추출 결과는 CoffeeBeanCreateForm에만 반영한다.
OCR 구현체는 인터페이스로 분리한다.
처음에는 Mock OCR로 전체 흐름을 먼저 구현한다.
실제 OCR 연동은 나중에 구현체만 교체할 수 있게 한다.
모든 enum 저장은 EnumType.STRING을 사용한다.
사용자가 최종 확인 후 POST /coffee-beans로 저장하는 구조를 유지한다.
```

---

## 21. 최종 요약

최종 구조:

```text
사진 업로드
→ OCR rawText 추출
→ rawText 파싱
→ CoffeeBeanCreateForm 자동 입력
→ 사용자 확인/수정
→ 기존 CoffeeBean 저장 로직으로 저장
```

가장 중요한 설계 원칙:

```text
자동 추출은 입력 보조 기능이다.
최종 저장은 사용자가 확인한 폼 데이터만 저장한다.
```
