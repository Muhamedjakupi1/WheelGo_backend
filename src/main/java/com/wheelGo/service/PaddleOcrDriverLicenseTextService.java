package com.wheelGo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaddleOcrDriverLicenseTextService {

    private final ObjectMapper objectMapper;

    @Value("${app.paddleocr.python-command:python}")
    private String pythonCommand;

    @Value("${app.paddleocr.script-path:scripts/paddle_driver_license_ocr.py}")
    private String scriptPath;

    @Value("${app.paddleocr.timeout-seconds:60}")
    private long timeoutSeconds;

    public OcrResult readText(Path frontImagePath, Path backImagePath) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonCommand,
                resolveScriptPath().toString(),
                frontImagePath.toAbsolutePath().toString(),
                backImagePath.toAbsolutePath().toString()
        );
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PaddleOCR verification timed out");
            }

            // KËTU BËHET DEBUG-I I VËRTETË
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            System.out.println("========= PADDLE OCR DEBUG START =========");
            System.out.println("OUTPUT RAW: " + output);
            System.out.println("========= PADDLE OCR DEBUG END =========");

            if (process.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PaddleOCR verification failed: " + summarizeOutput(output));
            }

            PaddleOcrResponse response = objectMapper.treeToValue(extractLastJsonObject(output), PaddleOcrResponse.class);
            String text = response.text() == null ? "" : response.text();
            List<String> lines = response.lines() == null ? List.of() : response.lines();

            return new OcrResult(text, lines);

        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to run PaddleOCR verification");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PaddleOCR verification was interrupted");
        }
    }

    private JsonNode extractLastJsonObject(String output) throws IOException {
        int jsonStart = output.lastIndexOf('{');
        if (jsonStart < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PaddleOCR returned no JSON output");
        }
        return objectMapper.readTree(output.substring(jsonStart));
    }

    private String summarizeOutput(String output) {
        if (output == null || output.isBlank()) {
            return "no output";
        }
        String compact = output.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(compact.length() - 500);
    }

    private Path resolveScriptPath() {
        Path path = Path.of(scriptPath);
        if (Files.exists(path)) {
            return path;
        }

        Path backendRelativePath = Path.of("Backend").resolve(scriptPath);
        if (Files.exists(backendRelativePath)) {
            return backendRelativePath;
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PaddleOCR script was not found at: " + scriptPath);
    }

    @Getter
    public static class OcrResult {
        private final String text;
        private final List<String> lines;

        public OcrResult(String text, List<String> lines) {
            this.text = text == null ? "" : text;
            this.lines = lines == null ? List.of() : lines;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaddleOcrResponse(String text, List<String> lines) {}
}