package com.hsg.coffee.domain.tasteSpace.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.tasteSpace.dto.TasteSpaceResponse;
import com.hsg.coffee.domain.tasteSpace.entity.TasteSpacePoint;
import com.hsg.coffee.domain.tasteSpace.repository.TasteSpacePointRepository;
import com.hsg.coffee.domain.user.entity.AppUser;
import com.hsg.coffee.domain.user.repository.AppUserRepository;
import com.hsg.coffee.global.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TasteSpaceService {

    private final TasteSpaceDocumentBuilder documentBuilder;
    private final OpenRouterEmbeddingClient embeddingClient;
    private final PcaProjectionService projectionService;
    private final TasteSpacePointRepository pointRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public TasteSpaceResponse get() {
        Long ownerId = requireOwnerId();
        List<TasteSpaceDocument> documents = documentBuilder.build(ownerId);
        List<TasteSpacePoint> points = pointRepository.findByOwnerIdOrderByCoffeeBeanId(ownerId);
        String state = isCurrent(documents, points) ? "READY" : (points.isEmpty() ? "EMPTY" : "STALE");
        return response(state, documents, points);
    }

    @Transactional
    public TasteSpaceResponse rebuild() {
        return rebuildForOwner(requireOwnerId());
    }

    @Transactional
    public TasteSpaceResponse rebuildForOwner(Long ownerId) {
        List<TasteSpaceDocument> documents = documentBuilder.build(ownerId);
        if (documents.isEmpty()) {
            replacePoints(ownerId, documents, List.of(), new double[0][3]);
            return response("EMPTY", documents, List.of());
        }

        Map<Long, TasteSpacePoint> existingByBean = pointRepository.findByOwnerIdOrderByCoffeeBeanId(ownerId).stream()
                .collect(Collectors.toMap(point -> point.getCoffeeBean().getId(), Function.identity()));
        List<double[]> embeddings = new ArrayList<>(java.util.Collections.nCopies(documents.size(), null));
        List<Integer> changedIndices = new ArrayList<>();
        List<String> changedTexts = new ArrayList<>();

        for (int index = 0; index < documents.size(); index++) {
            TasteSpaceDocument document = documents.get(index);
            TasteSpacePoint existing = existingByBean.get(document.bean().getId());
            if (existing != null
                    && existing.getModelId().equals(embeddingClient.modelId())
                    && existing.getSourceHash().equals(document.sourceHash())) {
                embeddings.set(index, readVector(existing));
                continue;
            }
            changedIndices.add(index);
            changedTexts.add(document.text());
        }

        if (!changedTexts.isEmpty()) {
            List<double[]> changedEmbeddings = embeddingClient.embed(changedTexts);
            for (int index = 0; index < changedIndices.size(); index++) {
                embeddings.set(changedIndices.get(index), changedEmbeddings.get(index));
            }
        }

        double[][] coordinates = projectionService.project(embeddings);
        List<TasteSpacePoint> saved = replacePoints(ownerId, documents, embeddings, coordinates);
        return response("READY", documents, saved);
    }

    private List<TasteSpacePoint> replacePoints(
            Long ownerId,
            List<TasteSpaceDocument> documents,
            List<double[]> embeddings,
            double[][] coordinates
    ) {
        pointRepository.deleteByOwnerId(ownerId);
        pointRepository.flush();
        if (documents.isEmpty()) {
            return List.of();
        }
        AppUser owner = appUserRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));
        List<TasteSpacePoint> points = new ArrayList<>();
        for (int index = 0; index < documents.size(); index++) {
            TasteSpaceDocument document = documents.get(index);
            points.add(TasteSpacePoint.create(
                    owner,
                    document.bean(),
                    embeddingClient.modelId(),
                    document.sourceHash(),
                    writeVector(embeddings.get(index)),
                    coordinates[index][0],
                    coordinates[index][1],
                    coordinates[index][2]
            ));
        }
        return pointRepository.saveAll(points);
    }

    private TasteSpaceResponse response(String state, List<TasteSpaceDocument> documents, List<TasteSpacePoint> points) {
        Map<Long, TasteSpaceDocument> documentsByBean = documents.stream()
                .collect(Collectors.toMap(document -> document.bean().getId(), Function.identity()));
        Map<Long, double[]> vectors = points.stream()
                .collect(Collectors.toMap(point -> point.getCoffeeBean().getId(), this::readVector));

        List<TasteSpaceResponse.BeanPoint> beanPoints = points.stream()
                .filter(point -> documentsByBean.containsKey(point.getCoffeeBean().getId()))
                .map(point -> toResponse(point, documentsByBean.get(point.getCoffeeBean().getId()), documentsByBean, vectors))
                .toList();
        return new TasteSpaceResponse(state, embeddingClient.modelId(), documents.size(), profile(documents), beanPoints);
    }

    private TasteSpaceResponse.BeanPoint toResponse(
            TasteSpacePoint point,
            TasteSpaceDocument document,
            Map<Long, TasteSpaceDocument> documentsByBean,
            Map<Long, double[]> vectors
    ) {
        Long beanId = point.getCoffeeBean().getId();
        Map.Entry<Long, Double> closest = vectors.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(beanId))
                .map(entry -> Map.entry(entry.getKey(), cosine(vectors.get(beanId), entry.getValue())))
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        String closestName = closest != null && documentsByBean.containsKey(closest.getKey())
                ? documentsByBean.get(closest.getKey()).bean().getName()
                : null;
        return new TasteSpaceResponse.BeanPoint(
                beanId,
                point.getCoffeeBean().getName(),
                point.getCoffeeBean().getRoastery(),
                point.getCoffeeBean().getCountry(),
                point.getCoffeeBean().getProcessType() != null ? point.getCoffeeBean().getProcessType().name() : null,
                point.getCoffeeBean().getStatus() != null ? point.getCoffeeBean().getStatus().getDisplayName() : null,
                document.color(),
                document.colors(),
                document.flavorNotes(),
                document.averageRating(),
                document.brewCount(),
                point.getX(), point.getY(), point.getZ(),
                closest != null ? closest.getKey() : null,
                closestName,
                closest != null ? Math.max(0, closest.getValue()) : 0,
                "/coffee-beans/" + beanId
        );
    }

    private TasteSpaceResponse.TasteProfile profile(List<TasteSpaceDocument> documents) {
        Map<String, Long> frequencies = documents.stream()
                .flatMap(document -> document.flavorNotes().stream())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Double> preferenceScores = new LinkedHashMap<>();
        documents.stream()
                .filter(document -> document.averageRating() != null)
                .forEach(document -> document.flavorNotes().forEach(note -> preferenceScores.merge(
                        note,
                        document.averageRating() * Math.max(document.brewCount(), 1),
                        Double::sum
                )));
        List<String> dominantNotes = (preferenceScores.isEmpty() ? frequencies.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue()))
                        : preferenceScores)
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        TasteSpaceDocument favorite = documents.stream()
                .filter(document -> document.averageRating() != null)
                .max(Comparator.comparing(TasteSpaceDocument::averageRating))
                .orElse(documents.isEmpty() ? null : documents.getFirst());
        Double averageRating = documents.stream()
                .filter(document -> document.averageRating() != null)
                .mapToDouble(TasteSpaceDocument::averageRating)
                .average().stream().boxed().findFirst().orElse(null);
        String preferredOrigin = preferredValue(documents, document -> document.bean().getCountry());
        String preferredProcess = preferredValue(documents, document -> document.bean().getProcessType() != null
                ? processName(document.bean().getProcessType().name())
                : null);
        int totalBrewCount = documents.stream().mapToInt(TasteSpaceDocument::brewCount).sum();
        int ratedBeanCount = (int) documents.stream().filter(document -> document.averageRating() != null).count();
        String confidenceLabel = ratedBeanCount >= 7 ? "높음" : ratedBeanCount >= 3 ? "보통" : "탐색 중";
        String title = dominantNotes.isEmpty()
                ? "취향의 윤곽을 찾는 중이에요"
                : String.join(" · ", dominantNotes) + " 쪽으로 기울어요";
        String description = dominantNotes.isEmpty()
                ? "원두와 브루잉 기록이 늘어나면 취향의 중심이 더 또렷해집니다."
                : preferenceDescription(dominantNotes, preferredOrigin, preferredProcess, ratedBeanCount);
        return new TasteSpaceResponse.TasteProfile(
                title,
                description,
                favorite != null ? favorite.bean().getName() : null,
                averageRating,
                dominantNotes,
                preferredOrigin,
                preferredProcess,
                totalBrewCount,
                ratedBeanCount,
                confidenceLabel
        );
    }

    private String preferredValue(List<TasteSpaceDocument> documents, Function<TasteSpaceDocument, String> extractor) {
        return documents.stream()
                .filter(document -> document.averageRating() != null)
                .filter(document -> extractor.apply(document) != null && !extractor.apply(document).isBlank())
                .collect(Collectors.groupingBy(extractor, Collectors.averagingDouble(TasteSpaceDocument::averageRating)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String preferenceDescription(
            List<String> notes,
            String preferredOrigin,
            String preferredProcess,
            int ratedBeanCount
    ) {
        StringBuilder description = new StringBuilder();
        description.append(ratedBeanCount > 0 ? "평점이 높은 기록에서 " : "기록에서 ")
                .append(String.join("과 ", notes.stream().limit(2).toList()))
                .append(" 향미가 반복됩니다.");
        if (preferredOrigin != null || preferredProcess != null) {
            description.append(" ")
                    .append(String.join(" · ", Stream.of(preferredOrigin, preferredProcess)
                            .filter(value -> value != null && !value.isBlank())
                            .toList()))
                    .append(" 원두에서 만족도가 높았어요.");
        }
        return description.toString();
    }

    private String processName(String process) {
        return switch (process) {
            case "WASHED" -> "워시드";
            case "NATURAL" -> "내추럴";
            case "HONEY" -> "허니";
            case "ANAEROBIC" -> "무산소 발효";
            case "CARBONIC_MACERATION" -> "카보닉 메서레이션";
            case "DECAF" -> "디카페인";
            default -> "기타 가공";
        };
    }

    private boolean isCurrent(List<TasteSpaceDocument> documents, List<TasteSpacePoint> points) {
        if (documents.size() != points.size()) {
            return false;
        }
        Map<Long, String> hashes = points.stream().collect(Collectors.toMap(
                point -> point.getCoffeeBean().getId(),
                TasteSpacePoint::getSourceHash
        ));
        return documents.stream().allMatch(document -> document.sourceHash().equals(hashes.get(document.bean().getId())));
    }

    private String writeVector(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception exception) {
            throw new IllegalStateException("임베딩 벡터를 저장하지 못했습니다.", exception);
        }
    }

    private double[] readVector(TasteSpacePoint point) {
        try {
            return objectMapper.readValue(point.getEmbeddingJson(), double[].class);
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 임베딩 벡터를 읽지 못했습니다.", exception);
        }
    }

    private double cosine(double[] left, double[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private Long requireOwnerId() {
        return currentUserService.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));
    }
}
