package com.hsg.coffee.domain.coffeeBean.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnExpression("'${brewlog.ocr.provider:local-best}' == 'local-best' || '${brewlog.ocr.provider:local-best}' == 'paddleocr'")
public class PaddleCoffeeBeanCardOcrService implements CoffeeBeanCardOcrService {

    private static final Logger log = LoggerFactory.getLogger(PaddleCoffeeBeanCardOcrService.class);

    private final CoffeeBeanCardImagePreprocessor imagePreprocessor;
    private final GoogleVisionCoffeeBeanCardOcrClient googleVisionClient;
    private final ObjectMapper objectMapper;
    private final String pythonExecutable;
    private final Path scriptPath;
    private final Path cacheDirectory;
    private final int timeoutSeconds;
    private final int minimumTextLength;
    private final boolean fallbackEnabled;
    private final String language;
    private final String detectionModel;
    private final String recognitionModel;

    public PaddleCoffeeBeanCardOcrService(
            CoffeeBeanCardImagePreprocessor imagePreprocessor,
            GoogleVisionCoffeeBeanCardOcrClient googleVisionClient,
            ObjectMapper objectMapper,
            @Value("${brewlog.ocr.paddle.python-executable:python3}") String pythonExecutable,
            @Value("${brewlog.ocr.paddle.script-path:scripts/paddle_ocr.py}") String scriptPath,
            @Value("${brewlog.ocr.paddle.cache-directory:${user.home}}") String cacheDirectory,
            @Value("${brewlog.ocr.paddle.timeout-seconds:90}") int timeoutSeconds,
            @Value("${brewlog.ocr.paddle.minimum-text-length:20}") int minimumTextLength,
            @Value("${brewlog.ocr.paddle.fallback-enabled:true}") boolean fallbackEnabled,
            @Value("${brewlog.ocr.paddle.language:korean}") String language,
            @Value("${brewlog.ocr.paddle.detection-model:PP-OCRv5_server_det}") String detectionModel,
            @Value("${brewlog.ocr.paddle.recognition-model:korean_PP-OCRv5_mobile_rec}") String recognitionModel
    ) {
        this.imagePreprocessor = imagePreprocessor;
        this.googleVisionClient = googleVisionClient;
        this.objectMapper = objectMapper;
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = Path.of(scriptPath).toAbsolutePath().normalize();
        this.cacheDirectory = Path.of(cacheDirectory).toAbsolutePath().normalize();
        this.timeoutSeconds = timeoutSeconds;
        this.minimumTextLength = minimumTextLength;
        this.fallbackEnabled = fallbackEnabled;
        this.language = language;
        this.detectionModel = detectionModel;
        this.recognitionModel = recognitionModel;
    }

    @Override
    public String extractText(MultipartFile image) {
        try {
            String text = extractWithPaddle(image);
            if (!StringUtils.hasText(text) || text.trim().length() < minimumTextLength) {
                throw new IllegalArgumentException("PaddleOCR 결과가 충분하지 않습니다.");
            }
            return text;
        } catch (RuntimeException exception) {
            if (!fallbackEnabled) {
                throw exception;
            }
            log.warn("PaddleOCR failed. fallback=google-vision, filename={}, reason={}",
                    image.getOriginalFilename(),
                    exception.getMessage());
            return googleVisionClient.extractText(image);
        }
    }

    private String extractWithPaddle(MultipartFile image) {
        PreparedCoffeeBeanCardImage preparedImage = imagePreprocessor.prepare(image);
        Path tempImage = null;
        try {
            Files.createDirectories(cacheDirectory);
            tempImage = Files.createTempFile("brewlog-ocr-", suffixFor(preparedImage.filename()));
            Files.write(tempImage, preparedImage.bytes());

            ProcessBuilder processBuilder = new ProcessBuilder(command(tempImage));
            processBuilder.environment().put("HOME", cacheDirectory.toString());
            processBuilder.environment().put("XDG_CACHE_HOME", cacheDirectory.toString());
            processBuilder.environment().put("BREWLOG_PADDLE_OCR_LANG", language);
            processBuilder.environment().put("BREWLOG_PADDLE_OCR_DETECTION_MODEL", detectionModel);
            processBuilder.environment().put("BREWLOG_PADDLE_OCR_RECOGNITION_MODEL", recognitionModel);
            processBuilder.redirectErrorStream(false);

            log.info(
                    "PaddleOCR request started. filename={}, script={}, detectionModel={}, recognitionModel={}",
                    preparedImage.filename(),
                    scriptPath,
                    detectionModel,
                    recognitionModel
            );
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException("PaddleOCR 응답 시간이 초과되었습니다.");
            }
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException("PaddleOCR 실행 실패: " + abbreviate(stderr));
            }

            String text = parseText(stdout);
            log.info(
                    "PaddleOCR request finished. filename={}, extractedLength={}",
                    preparedImage.filename(),
                    text.length()
            );
            return text;
        } catch (IOException exception) {
            throw new IllegalArgumentException("PaddleOCR 실행 파일 또는 이미지 파일을 준비할 수 없습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("PaddleOCR 요청이 중단되었습니다.", exception);
        } finally {
            if (tempImage != null) {
                try {
                    Files.deleteIfExists(tempImage);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private List<String> command(Path imagePath) {
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath.toString());
        command.add(imagePath.toString());
        return command;
    }

    private String parseText(String stdout) {
        try {
            JsonNode root = objectMapper.readTree(stdout);
            if (root.has("text")) {
                return root.get("text").asText();
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("PaddleOCR 응답 JSON을 읽을 수 없습니다: " + abbreviate(stdout), exception);
        }
        return "";
    }

    private String suffixFor(String filename) {
        if (!StringUtils.hasText(filename)) {
            return ".png";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? ".png" : filename.substring(dotIndex);
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }
}
