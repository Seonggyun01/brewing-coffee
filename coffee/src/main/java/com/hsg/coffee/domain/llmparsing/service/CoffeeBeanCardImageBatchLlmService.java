package com.hsg.coffee.domain.llmparsing.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanCardOcrService;
import com.hsg.coffee.domain.llmparsing.dto.LlmImageBatchResponse;
import com.hsg.coffee.domain.llmparsing.dto.LlmImageBatchResult;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingDebugResponse;

import tools.jackson.databind.ObjectMapper;

@Service
public class CoffeeBeanCardImageBatchLlmService {

    private final CoffeeBeanCardOcrService ocrService;
    private final HuggingFaceBeanMappingService huggingFaceBeanMappingService;
    private final ObjectMapper objectMapper;
    private final Path imageDirectory;
    private final Path outputDirectory;

    public CoffeeBeanCardImageBatchLlmService(
            CoffeeBeanCardOcrService ocrService,
            HuggingFaceBeanMappingService huggingFaceBeanMappingService,
            ObjectMapper objectMapper,
            @Value("${brewlog.dev.image-directory:../docs/image}") String imageDirectory,
            @Value("${brewlog.dev.llm-output-directory:../docs/image/ocr-llm-results}") String outputDirectory
    ) {
        this.ocrService = ocrService;
        this.huggingFaceBeanMappingService = huggingFaceBeanMappingService;
        this.objectMapper = objectMapper;
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
    }

    public LlmImageBatchResponse processAllImages() {
        List<LlmImageBatchResult> results = new ArrayList<>();

        try {
            Files.createDirectories(outputDirectory);
            for (Path imagePath : findImages()) {
                results.add(processImage(imagePath));
            }
        } catch (Exception exception) {
            results.add(new LlmImageBatchResult(
                    "",
                    "ERROR",
                    exception.getMessage(),
                    "",
                    "",
                    null
            ));
        }

        return new LlmImageBatchResponse(
                imageDirectory.toString(),
                outputDirectory.toString(),
                results
        );
    }

    private List<Path> findImages() throws IOException {
        if (!Files.isDirectory(imageDirectory)) {
            return List.of();
        }

        try (var paths = Files.list(imageDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedImage)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private LlmImageBatchResult processImage(Path imagePath) {
        String baseName = removeExtension(imagePath.getFileName().toString());
        Path ocrTextFile = outputDirectory.resolve(baseName + ".ocr.txt");
        Path llmDebugFile = outputDirectory.resolve(baseName + ".llm-debug.json");

        try {
            String ocrText = ocrService.extractText(new PathMultipartFile(imagePath));
            Files.writeString(ocrTextFile, ocrText);

            LlmParsingDebugResponse llmDebugResponse = huggingFaceBeanMappingService.debugParseOcrText(ocrText);
            Files.writeString(
                    llmDebugFile,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(llmDebugResponse)
            );

            return new LlmImageBatchResult(
                    imagePath.getFileName().toString(),
                    llmDebugResponse.status(),
                    llmDebugResponse.error(),
                    ocrTextFile.toString(),
                    llmDebugFile.toString(),
                    llmDebugResponse.parsedResponse()
            );
        } catch (Exception exception) {
            return new LlmImageBatchResult(
                    imagePath.getFileName().toString(),
                    "ERROR",
                    exception.getMessage(),
                    ocrTextFile.toString(),
                    llmDebugFile.toString(),
                    null
            );
        }
    }

    private boolean isSupportedImage(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp")
                || filename.endsWith(".heic")
                || filename.endsWith(".heif");
    }

    private String removeExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? filename : filename.substring(0, dotIndex);
    }

    private static class PathMultipartFile implements MultipartFile {

        private final Path path;

        private PathMultipartFile(Path path) {
            this.path = path;
        }

        @Override
        public String getName() {
            return "image";
        }

        @Override
        public String getOriginalFilename() {
            return path.getFileName().toString();
        }

        @Override
        public String getContentType() {
            String filename = getOriginalFilename().toLowerCase();
            if (filename.endsWith(".heic")) {
                return "image/heic";
            }
            if (filename.endsWith(".heif")) {
                return "image/heif";
            }
            try {
                return Files.probeContentType(path);
            } catch (IOException exception) {
                return "application/octet-stream";
            }
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException exception) {
                return 0;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(Path dest) throws IOException {
            Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
