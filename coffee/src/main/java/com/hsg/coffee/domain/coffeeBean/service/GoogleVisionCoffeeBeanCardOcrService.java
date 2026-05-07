package com.hsg.coffee.domain.coffeeBean.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

@Service
@ConditionalOnProperty(name = "brewlog.ocr.provider", havingValue = "google-vision")
public class GoogleVisionCoffeeBeanCardOcrService implements CoffeeBeanCardOcrService {

    private static final Logger log = LoggerFactory.getLogger(GoogleVisionCoffeeBeanCardOcrService.class);

    private final CoffeeBeanCardImagePreprocessor imagePreprocessor;

    public GoogleVisionCoffeeBeanCardOcrService(CoffeeBeanCardImagePreprocessor imagePreprocessor) {
        this.imagePreprocessor = imagePreprocessor;
    }

    @Override
    public String extractText(MultipartFile image) {
        try {
            PreparedCoffeeBeanCardImage preparedImage = imagePreprocessor.prepare(image);
            Image visionImage = Image.newBuilder()
                    .setContent(ByteString.copyFrom(preparedImage.bytes()))
                    .build();
            Feature textDetection = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(textDetection)
                    .setImage(visionImage)
                    .build();

            log.info(
                    "Google Vision OCR request started. filename={}, size={}, contentType={}",
                    preparedImage.filename(),
                    preparedImage.bytes().length,
                    preparedImage.contentType()
            );

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                AnnotateImageResponse response = client.batchAnnotateImages(List.of(request))
                        .getResponses(0);

                if (response.hasError()) {
                    log.warn("Google Vision OCR failed. message={}", response.getError().getMessage());
                    throw new IllegalArgumentException("이미지 분석 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
                }

                String rawText = response.getFullTextAnnotation().getText();
                if (!StringUtils.hasText(rawText)) {
                    throw new IllegalArgumentException("이미지에서 텍스트를 찾지 못했습니다. 직접 입력해주세요.");
                }

                log.info(
                        "Google Vision OCR request finished. filename={}, extractedLength={}",
                        preparedImage.filename(),
                        rawText.length()
                );
                return rawText;
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            log.warn("Google Vision OCR client initialization failed. filename={}", image.getOriginalFilename(), exception);
            throw new IllegalArgumentException("OCR 서비스 인증 정보를 읽을 수 없습니다. GOOGLE_APPLICATION_CREDENTIALS 설정을 확인해주세요.");
        } catch (Exception exception) {
            log.warn("Google Vision OCR request failed. filename={}", image.getOriginalFilename(), exception);
            throw new IllegalArgumentException("OCR 서비스 인증 또는 이미지 분석 중 문제가 발생했습니다. 설정을 확인해주세요.");
        }
    }
}
