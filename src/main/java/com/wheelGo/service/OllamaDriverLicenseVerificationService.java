package com.wheelGo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wheelGo.model.driver_licenses.DriverLicenseVerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OllamaDriverLicenseVerificationService {

    private final ObjectMapper objectMapper;

    @Value("${app.ollama.driver-license.api-url:http://localhost:11434/api/chat}")
    private String ollamaApiUrl;

    @Value("${app.ollama.driver-license.model:granite3.2-vision}")
    private String ollamaModel;

    public DriverLicenseVerificationResponse verify(Path frontImagePath,
                                                    Path backImagePath) {
        String frontImageBase64 = encodeImage(frontImagePath);
        String backImageBase64 = encodeImage(backImagePath);

        OllamaChatRequest request = new OllamaChatRequest(
                ollamaModel,
                List.of(new OllamaMessage(
                        "user",
                        buildPrompt(),
                        List.of(frontImageBase64, backImageBase64)
                )),
                false,
                SimpleAnswer.schema(),
                new OllamaOptions(0)
        );

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama verification request failed");
            }

            OllamaChatResponse ollamaResponse = objectMapper.readValue(response.body(), OllamaChatResponse.class);
            if (ollamaResponse.message() == null || ollamaResponse.message().content() == null || ollamaResponse.message().content().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama returned an empty verification result");
            }

            SimpleAnswer ai = objectMapper.readValue(ollamaResponse.message().content(), SimpleAnswer.class);
            return toVerificationResponse(ai);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse Ollama verification result");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to verify driver license with Ollama");
        }
    }

    private DriverLicenseVerificationResponse toVerificationResponse(SimpleAnswer ai) {
        // 1. Kontrolli i rreptë i kushteve pozitive
        boolean positiveIndicators = "yes".equalsIgnoreCase(ai.answer())
                && ai.isDocumentVisible()
                && ai.isDriverLicenseLike()
                && ai.isImageQualityOk()
                && ai.frontHasPortrait()
                && ai.frontHasDriverLicenseFields()
                && ai.backHasDriverLicenseData()
                && ai.confidence() >= 0.75;

        // 2. Kontrolli i "flamujve të kuq" (Negative constraints)
        // Nëse AI thotë që ka para, logo, ose dokument tjetër, verifikimi dështon menjëherë
        boolean negativeIndicators = ai.containsMoney()
                || ai.containsLogoOnlyOrGraphic()
                || ai.containsNonLicenseDocument()
                || (ai.redFlags() != null && !ai.redFlags().isEmpty());

        // 3. Vendimi final: Duhet të ketë indikatorë pozitivë dhe ASNJE indikator negativ
        boolean verified = positiveIndicators && !negativeIndicators;

        DriverLicenseVerificationResponse response = new DriverLicenseVerificationResponse();

        response.setVerified(verified);
        response.setVerdict(verified ? "ai_passed" : "rejected");

        // 4. Përcaktimi i rekomandimit (Recommendation)
        String recommendation;
        if (negativeIndicators) {
            // Nëse dështoi për shkak të parave ose logove (si rasti i 50 Euro apo Rikon)
            recommendation = "Rejected: The image contains non-license content (money, logos, or invalid documents).";
        } else if (ai.reason() != null && !ai.reason().isBlank()) {
            recommendation = ai.reason();
        } else {
            recommendation = verified ? "The document likely looks real." : "The document looks suspicious or unclear.";
        }

        response.setRecommendation(recommendation);

        // 5. Mbushja e fushave të tjera të modelit
        response.setDocumentVisible(ai.isDocumentVisible());
        response.setDriverLicenseLike(ai.isDriverLicenseLike());
        response.setImageQualityOk(ai.isImageQualityOk());
        response.setRequiredFieldsExtracted(false);
        response.setProfileNameMatches(false);

        // Sigurohemi që lista e redFlags të mos jetë kurrë null
        response.setTamperingSignals(ai.redFlags() == null ? List.of() : ai.redFlags());
        response.setConfidence(ai.confidence());

        return response;
    }

    private String buildPrompt() {
        return """
    ### ROLE
    You are a helpful Document Assistant. Your job is to identify if the two uploaded images show the front and back of a physical Driver's License.

    ### SIMPLE CHECKLIST:
    1. FRONT IMAGE: Look for a person's photo (portrait) and text fields (name, dates).
    2. BACK IMAGE: Look for a table with vehicle categories (A, B, C, D icons) and a barcode or QR code.
    3. REALITY CHECK: If you see money (banknotes), logos only, or random objects, the answer is "no".

    ### QUALITY NOTES:
    - Photos are taken with a basic phone. Ignore shadows, blur, or bad lighting.
    - As long as you can see the 'Face' on the front and the 'Category Table' on the back, consider it a valid license.

    ### DECISION LOGIC:
    - If Front has a Face and Back has a Table -> "answer": "yes".
    - If images are money, screenshots, or not a license -> "answer": "no".

    ### OUTPUT FORMAT (JSON ONLY)
    {
      "answer": "yes/no",
      "reason": "Short explanation of what you see",
      "isDocumentVisible": true,
      "isDriverLicenseLike": true,
      "isImageQualityOk": true,
      "frontHasPortrait": true,
      "frontHasDriverLicenseFields": true,
      "backHasDriverLicenseData": true,
      "containsMoney": false,
      "containsLogoOnlyOrGraphic": false,
      "containsNonLicenseDocument": false,
      "confidence": 0.85,
      "redFlags": []
    }
    """;
    }
    private String encodeImage(Path path) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read stored driver license image");
        }
    }

    private record OllamaChatRequest(String model, List<OllamaMessage> messages, boolean stream, Object format, OllamaOptions options) {}

    private record OllamaMessage(String role, String content, List<String> images) {}

    private record OllamaOptions(int temperature) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaChatResponse(OllamaResponseMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaResponseMessage(String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SimpleAnswer(
            String answer,
            String reason,
            boolean isDocumentVisible,
            boolean isDriverLicenseLike,
            boolean isImageQualityOk,
            boolean frontHasPortrait,
            boolean frontHasDriverLicenseFields,
            boolean backHasDriverLicenseData,
            boolean containsMoney,
            boolean containsLogoOnlyOrGraphic,
            boolean containsNonLicenseDocument,
            double confidence,
            List<String> redFlags
    ) {
        static Object schema() {
            return java.util.Map.of(
                    "type", "object",
                    "properties", java.util.Map.ofEntries(
                            java.util.Map.entry("answer", java.util.Map.of("type", "string", "enum", List.of("yes", "no"))),
                            java.util.Map.entry("reason", java.util.Map.of("type", "string")),
                            java.util.Map.entry("isDocumentVisible", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("isDriverLicenseLike", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("isImageQualityOk", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("frontHasPortrait", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("frontHasDriverLicenseFields", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("backHasDriverLicenseData", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("containsMoney", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("containsLogoOnlyOrGraphic", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("containsNonLicenseDocument", java.util.Map.of("type", "boolean")),
                            java.util.Map.entry("confidence", java.util.Map.of("type", "number")),
                            java.util.Map.entry("redFlags", java.util.Map.of("type", "array", "items", java.util.Map.of("type", "string")))
                    ),
                    "required", List.of(
                            "answer",
                            "reason",
                            "isDocumentVisible",
                            "isDriverLicenseLike",
                            "isImageQualityOk",
                            "frontHasPortrait",
                            "frontHasDriverLicenseFields",
                            "backHasDriverLicenseData",
                            "containsMoney",
                            "containsLogoOnlyOrGraphic",
                            "containsNonLicenseDocument",
                            "confidence",
                            "redFlags"
                    )
            );
        }
    }
}
