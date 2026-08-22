package com.hsg.coffee.domain.coffeeBean.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;
import openize.io.IOFileStream;
import openize.io.IOMode;
import openize.io.IOStream;

@Service
public class CoffeeBeanCardImagePreprocessor {

    private final int maxInputSide;

    public CoffeeBeanCardImagePreprocessor(
            @Value("${brewlog.ocr.paddle.max-input-side:1216}") int maxInputSide
    ) {
        this.maxInputSide = maxInputSide;
    }

    public PreparedCoffeeBeanCardImage prepare(MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();
            if (!isHeic(image)) {
                return resizeIfNeeded(imageBytes, image.getOriginalFilename(), image.getContentType());
            }

            BufferedImage convertedImage = convertHeicToImage(imageBytes);
            return toPngPreparedImage(
                    resizeImageIfNeeded(convertedImage),
                    replaceExtension(image.getOriginalFilename(), "png")
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다. 다시 업로드해주세요.", exception);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("HEIC 이미지를 변환할 수 없습니다. 다시 촬영하거나 JPG/PNG로 변환해 업로드해주세요.", exception);
        }
    }

    private boolean isHeic(MultipartFile image) {
        String contentType = normalize(image.getContentType());
        String filename = normalize(image.getOriginalFilename());
        return "image/heic".equals(contentType)
                || "image/heif".equals(contentType)
                || filename.endsWith(".heic")
                || filename.endsWith(".heif");
    }

    private BufferedImage convertHeicToImage(byte[] imageBytes) throws Exception {
        Path tempFile = Files.createTempFile("coffee-card-", ".heic");
        Files.write(tempFile, imageBytes);

        try (IOStream stream = new IOFileStream(tempFile, IOMode.READ)) {
            HeicImage heicImage = HeicImage.load(stream);

            int width = (int) heicImage.getWidth();
            int height = (int) heicImage.getHeight();
            int[] pixels = heicImage.getInt32Array(PixelFormat.Argb32);
            if (width <= 0 || height <= 0 || pixels == null || pixels.length == 0) {
                throw new IllegalArgumentException("HEIC 이미지 픽셀 데이터를 읽을 수 없습니다.");
            }

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bufferedImage.setRGB(0, 0, width, height, pixels, 0, width);
            return bufferedImage;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private PreparedCoffeeBeanCardImage resizeIfNeeded(byte[] imageBytes, String filename, String contentType) throws IOException {
        BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            return new PreparedCoffeeBeanCardImage(imageBytes, filename, contentType);
        }

        BufferedImage resizedImage = resizeImageIfNeeded(originalImage);
        if (resizedImage == originalImage) {
            return new PreparedCoffeeBeanCardImage(imageBytes, filename, contentType);
        }

        return toPngPreparedImage(resizedImage, replaceExtension(filename, "png"));
    }

    private BufferedImage resizeImageIfNeeded(BufferedImage image) {
        if (maxInputSide <= 0) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxInputSide) {
            return image;
        }

        double scale = (double) maxInputSide / longestSide;
        int resizedWidth = Math.max(1, (int) Math.round(width * scale));
        int resizedHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resizedImage = new BufferedImage(resizedWidth, resizedHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_RENDERING,
                    java.awt.RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.drawImage(image, 0, 0, resizedWidth, resizedHeight, null);
        } finally {
            graphics.dispose();
        }
        return resizedImage;
    }

    private PreparedCoffeeBeanCardImage toPngPreparedImage(BufferedImage image, String filename) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", outputStream)) {
                throw new IllegalArgumentException("OCR용 PNG 이미지를 만들 수 없습니다.");
            }
            return new PreparedCoffeeBeanCardImage(outputStream.toByteArray(), filename, "image/png");
        }
    }

    private String replaceExtension(String filename, String extension) {
        if (filename == null || filename.isBlank()) {
            return "coffee-card." + extension;
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return filename + "." + extension;
        }

        return filename.substring(0, dotIndex + 1) + extension;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
