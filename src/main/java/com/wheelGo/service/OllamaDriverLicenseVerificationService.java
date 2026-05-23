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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class OllamaDriverLicenseVerificationService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.ollama.driver-license.api-url:http://localhost:11434/api/chat}")
    private String ollamaApiUrl;

    @Value("${app.ollama.driver-license.model:granite3.2-vision}")
    private String ollamaModel;

    public DriverLicenseVerificationResponse verify(Path frontImagePath,
                                                    Path backImagePath) {
        try {
            return verifyAsync(frontImagePath, backImagePath).join();
        } catch (CompletionException ex) {
            throw unwrapCompletionException(ex);
        }
    }

    public CompletableFuture<DriverLicenseVerificationResponse> verifyAsync(Path frontImagePath,
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
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();

            return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .handle((response, throwable) -> {
                        if (throwable != null) {
                            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to verify driver license with Ollama");
                        }

                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama verification request failed");
                        }

                        try {
                            OllamaChatResponse ollamaResponse = objectMapper.readValue(response.body(), OllamaChatResponse.class);
                            if (ollamaResponse.message() == null || ollamaResponse.message().content() == null || ollamaResponse.message().content().isBlank()) {
                                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama returned an empty verification result");
                            }

                            SimpleAnswer ai = objectMapper.readValue(ollamaResponse.message().content(), SimpleAnswer.class);
                            return toVerificationResponse(ai);
                        } catch (IOException ex) {
                            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse Ollama verification result");
                        }
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(
                    new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse Ollama verification result")
            );
        }
    }

    private ResponseStatusException unwrapCompletionException(CompletionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ResponseStatusException responseStatusException) {
            return responseStatusException;
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to verify driver license with Ollama");
    }

    private DriverLicenseVerificationResponse toVerificationResponse(SimpleAnswer ai) {
        boolean positiveIndicators = "yes".equalsIgnoreCase(ai.answer())
                && ai.isDocumentVisible()
                && ai.isDriverLicenseLike()
                && ai.isImageQualityOk()
                && ai.frontHasPortrait()
                && ai.frontHasDriverLicenseFields()
                && ai.backHasDriverLicenseData()
                && ai.confidence() >= 0.75;

        boolean negativeIndicators = ai.containsMoney()
                || ai.containsLogoOnlyOrGraphic()
                || ai.containsNonLicenseDocument()
                || (ai.redFlags() != null && !ai.redFlags().isEmpty());

        boolean verified = positiveIndicators && !negativeIndicators;

        DriverLicenseVerificationResponse response = new DriverLicenseVerificationResponse();

        response.setVerified(verified);
        response.setVerdict(verified ? "ai_passed" : "rejected");

        String recommendation;
        if (negativeIndicators) {
            recommendation = "Rejected: The image contains non-license content (money, logos, or invalid documents).";
        } else if (ai.reason() != null && !ai.reason().isBlank()) {
            recommendation = ai.reason();
        } else {
            recommendation = verified ? "The document likely looks real." : "The document looks suspicious or unclear.";
        }

        response.setRecommendation(recommendation);

        response.setDocumentVisible(ai.isDocumentVisible());
        response.setDriverLicenseLike(ai.isDriverLicenseLike());
        response.setImageQualityOk(ai.isImageQualityOk());
        response.setRequiredFieldsExtracted(false);
        response.setProfileNameMatches(false);

        response.setTamperingSignals(ai.redFlags() == null ? List.of() : ai.redFlags());
        response.setConfidence(ai.confidence());

        return response;
    }

    private String buildPrompt() {
        return """
                You are verifying whether two uploaded images are the front and back of a real driver's license.

                Be flexible. Driver licenses vary by country, state, age, layout, and print style.
                Do not require one exact design or field placement.
                Accept plastic cards, older paper licenses, booklet-style licenses, and worn but still recognizable documents.

                Use visual evidence, not perfect OCR.
                If some text is blurry but the overall document still clearly looks like a driver's license, treat that as acceptable.

                Common front-side clues:
                - portrait photo
                - person name
                - date of birth
                - license number
                - issue date or expiry date
                - government branding or security pattern

                Common back-side clues:
                - category or class information
                - barcode, QR code, MRZ, magnetic stripe, or similar machine-readable area
                - signature
                - restrictions, endorsements, or printed license data

                A valid-looking license does not need every clue above.

                Mark it likely valid when:
                - a document is visible in both images
                - the two images appear to show two sides of the same license
                - the front looks like a license front
                - the back contains license-like data or machine-readable content

                Be lenient with glare, blur, shadows, slight crop, tilt, worn edges, or partially blocked details.
                Reject only when there is strong evidence that the images are not a driver's license.

                Strong rejection signals:
                - money
                - logo-only graphic
                - passport, ID card, bank card, registration paper, or unrelated object
                - no recognizable document visible
                - obvious mismatch between the front and back

                Return JSON only.
                Return exactly this flat schema with no extra keys and no nesting:
                {
                  "answer": "yes" or "no",
                  "reason": "short explanation",
                  "isDocumentVisible": true or false,
                  "isDriverLicenseLike": true or false,
                  "isImageQualityOk": true or false,
                  "frontHasPortrait": true or false,
                  "frontHasDriverLicenseFields": true or false,
                  "backHasDriverLicenseData": true or false,
                  "containsMoney": true or false,
                  "containsLogoOnlyOrGraphic": true or false,
                  "containsNonLicenseDocument": true or false,
                  "confidence": number between 0 and 1,
                  "redFlags": ["short strings"]
                }

                Field guidance:
                - answer: "yes" if the images likely show a driver's license pair, otherwise "no"
                - isDocumentVisible: true if each image contains a visible document rather than an unrelated scene
                - isDriverLicenseLike: true if the overall document type looks like a driver's license
                - isImageQualityOk: true unless the images are too poor to judge key features at all
                - frontHasPortrait: true if the front appears to contain the holder photo
                - frontHasDriverLicenseFields: true if the front shows normal license identity fields, even if not all are readable
                - backHasDriverLicenseData: true if the back shows category/class data, barcode/QR/MRZ, signature, restrictions, or similar license-back content
                - redFlags: only include real fraud or mismatch concerns; otherwise return []

                Confidence guide:
                - 0.85 to 1.00: clearly a driver's license front/back pair
                - 0.65 to 0.84: likely a driver's license but some details are unclear
                - 0.40 to 0.64: ambiguous
                - 0.00 to 0.39: likely not a driver's license
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
