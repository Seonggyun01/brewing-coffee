package com.hsg.coffee.domain.coffeeBean.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;
import openize.io.IOFileStream;
import openize.io.IOMode;
import openize.io.IOStream;

@Service
public class CoffeeBeanCardImagePreprocessor {

    public PreparedCoffeeBeanCardImage prepare(MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();
            if (!isHeic(image)) {
                return new PreparedCoffeeBeanCardImage(
                        imageBytes,
                        image.getOriginalFilename(),
                        image.getContentType()
                );
            }

            byte[] convertedBytes = convertHeicToPng(imageBytes);
            return new PreparedCoffeeBeanCardImage(
                    convertedBytes,
                    replaceExtension(image.getOriginalFilename(), "png"),
                    "image/png"
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

    private byte[] convertHeicToPng(byte[] imageBytes) throws Exception {
        Path tempFile = Files.createTempFile("coffee-card-", ".heic");
        Files.write(tempFile, imageBytes);

        try (IOStream stream = new IOFileStream(tempFile, IOMode.READ);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            HeicImage heicImage = HeicImage.load(stream);

            int width = (int) heicImage.getWidth();
            int height = (int) heicImage.getHeight();
            int[] pixels = heicImage.getInt32Array(PixelFormat.Argb32);
            if (width <= 0 || height <= 0 || pixels == null || pixels.length == 0) {
                throw new IllegalArgumentException("HEIC 이미지 픽셀 데이터를 읽을 수 없습니다.");
            }

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bufferedImage.setRGB(0, 0, width, height, pixels, 0, width);
            if (!ImageIO.write(bufferedImage, "png", outputStream)) {
                throw new IllegalArgumentException("HEIC 이미지를 PNG로 변환할 수 없습니다.");
            }

            return outputStream.toByteArray();
        } finally {
            Files.deleteIfExists(tempFile);
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
