package com.hsg.coffee.domain.coffeeBean.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CoffeeBeanCardImagePreprocessorTest {

    @Test
    void resizeLargeImageBeforeOcr() throws Exception {
        CoffeeBeanCardImagePreprocessor preprocessor = new CoffeeBeanCardImagePreprocessor(1216);
        BufferedImage source = new BufferedImage(2268, 4032, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawString("INDONESIA IJEN ANAEROBIC NATURAL", 120, 240);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(source, "png", outputStream);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "coffee-card.png",
                "image/png",
                outputStream.toByteArray()
        );

        PreparedCoffeeBeanCardImage preparedImage = preprocessor.prepare(image);

        BufferedImage resizedImage = ImageIO.read(new ByteArrayInputStream(preparedImage.bytes()));
        assertThat(preparedImage.filename()).isEqualTo("coffee-card.png");
        assertThat(preparedImage.contentType()).isEqualTo("image/png");
        assertThat(Math.max(resizedImage.getWidth(), resizedImage.getHeight())).isEqualTo(1216);
        assertThat(resizedImage.getWidth()).isEqualTo(684);
    }

    @Test
    void convertHeicToResizedPngBeforeOcr() throws Exception {
        Path sampleImage = Path.of("../docs/image/IMG_9154.heic");
        Assumptions.assumeTrue(Files.exists(sampleImage));
        CoffeeBeanCardImagePreprocessor preprocessor = new CoffeeBeanCardImagePreprocessor(1216);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "IMG_9154.heic",
                "image/heic",
                Files.readAllBytes(sampleImage)
        );

        PreparedCoffeeBeanCardImage preparedImage = preprocessor.prepare(image);

        BufferedImage convertedImage = ImageIO.read(new ByteArrayInputStream(preparedImage.bytes()));
        assertThat(preparedImage.filename()).isEqualTo("IMG_9154.png");
        assertThat(preparedImage.contentType()).isEqualTo("image/png");
        assertThat(Math.max(convertedImage.getWidth(), convertedImage.getHeight())).isEqualTo(1216);
        assertThat(hasVisiblePixels(convertedImage)).isTrue();
    }

    private boolean hasVisiblePixels(BufferedImage image) {
        int sampleGap = Math.max(1, Math.min(image.getWidth(), image.getHeight()) / 24);
        for (int y = 0; y < image.getHeight(); y += sampleGap) {
            for (int x = 0; x < image.getWidth(); x += sampleGap) {
                if ((image.getRGB(x, y) & 0x00ffffff) != 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
