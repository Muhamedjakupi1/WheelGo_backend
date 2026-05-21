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
                ### ROLE
                You are a Document Verification AI. You receive two images — FRONT and BACK of a physical driver's license — and decide if both are genuine sides of a real driver's license.
                
                ---
                
                ### WHAT IS A DRIVER'S LICENSE
                An official government-issued card (~credit-card size, 85.6×54 mm) or older booklet (UK/EU pre-2013) proving the holder's right to drive. Issued by: DMV (USA), DVLA (UK), prefecture offices (Japan), national road agencies (EU). Also called: driving licence, Führerschein, permis de conduire, rijbewijs.
                
                ---
                
                ### FRONT SIDE — MUST-HAVES
                The single most important indicator is the portrait photo (headshot, usually top-left or top-right corner).
                
                Also look for most of these fields:
                - Name (surname + given name; EU labels them "1." and "2.")
                - Date of birth ("3." EU / "DOB" US; format DD.MM.YYYY or MM/DD/YYYY)
                - Issue date "4a" + expiry date "4b"
                - License number ("5" EU / "DL No." US)
                - Issuing authority ("4c" EU / state agency US)
                - Address (required in USA, optional in EU)
                - Vehicle category codes on front (A, B, C, D… — may be front-only on US licenses)
                - Country code, flag, or coat of arms
                - Security features: hologram overlay, ghost image (faint secondary portrait), microprinting, laser-engraved text
                
                Minimum to accept: portrait + at least 2 data fields.
                
                ---
                
                ### BACK SIDE — MUST-HAVES
                The single most important indicator is the vehicle category table.
                
                The category table (mandatory on all EU/EEA licenses since 2013 per Directive 2006/126/EC) is a grid with columns:
                  [Category code] | [Issue date] | [Expiry date] | [Restrictions]
                Categories: A, A1, A2, AM, B, B1, B96, BE, C, C1, CE, C1E, D, D1, DE, D1E, T, and country-specific codes.
                
                Also look for:
                - Barcode: PDF417 (US/Canada standard), QR code, or Data Matrix — encodes all card data
                - MRZ (machine-readable zone): 2–3 lines of OCR-B text with "<<<" separators
                - Magnetic stripe: black horizontal band (older US/Canadian licenses)
                - Restriction codes: EU field "12" (e.g. "01"=corrective lenses, "02"=hearing aid); US endorsement codes (H, N, P, S, T, X)
                - Holder signature (printed or handwritten)
                - State seal, EU stars, or national watermark in background
                
                Minimum to accept: category table OR barcode/MRZ present.
                
                ---
                
                ### PHYSICAL FORMAT NOTES
                - Modern card (ISO/IEC 7810 ID-1): polycarbonate, credit-card size. Used by all EU/EEA, USA, Canada, Australia, Japan, Korea, Brazil, China, and most of the world since 2013.
                - Old EU booklet: pink or green paper, A4 folded. Photo page with rubber stamp. Categories listed inside pages, not on a "back" surface.
                - Paper laminate: older or developing-country licenses. May lack holograms. Still valid.
                
                ---
                
                ### IMAGE QUALITY — BE LENIENT
                Accept despite: shadows, blur, finger at edge, flash glare on hologram, perspective tilt, worn/faded card, dark background, low resolution (you don't need to read every character — just identify features).
                
                Flag but don't reject: one side partially cropped, extreme tilt (>30°), one side partially off-frame.
                
                Reject only if: image is completely unreadable (solid color, pitch black, pure white, or zero identifiable detail).
                
                ---
                
                ### REJECTION SIGNALS
                
                Hard reject — definitely not a license:
                - Banknotes / paper currency (denomination number, serial, historical portrait)
                - Passport (no category table; different layout with MRZ but full biographical page)
                - National ID card (no category table; usually vertical; no vehicle data)
                - Credit/debit card (16-digit number, bank/network logo, no portrait, no category table)
                - Vehicle registration certificate (VIN/chassis number, no portrait)
                - Screenshot or photo-of-a-screen (pixel grid, moiré, rounded phone corners, status bar visible)
                - Logo-only or graphic with no personal data
                - Unrelated object (hands, desk, pet, scenery, blank surface)
                
                Flag as suspicious (don't auto-reject, lower confidence):
                - Pixelation or blurring around text fields while surroundings are sharp (editing artifact)
                - Portrait with unnaturally sharp rectangular edge inconsistent with card surface (pasted photo)
                - Impossible dates in category table (expiry before issue)
                - Front and back appear to be different cards (different background colors, fonts, or country formats)
                - Hologram area is flat/solid color instead of rainbow-iridescent
                - Visible lamination bubbles or peeling edges (possible home-printed fake)
                
                ---
                
                ### DECISION LOGIC (in order)
                1. Either image completely unreadable → "no", isImageQualityOk: false
                2. Both images show the same side → "no", wrongSidesPaired: true
                3. Front has no portrait AND no labeled data fields → "no"
                4. Back has no category table AND no barcode/MRZ → "no"
                5. Front has portrait + back has category table or barcode → "yes"
                6. Front has portrait + back is ambiguous (worn table, partial barcode) → "yes", confidence 0.55–0.70
                7. Any hard fraud signal detected → "no", populate redFlags
                
                ---
                
                ### OUTPUT — JSON ONLY (no markdown, no preamble)
                
                {
                  "answer": "yes" | "no",
                  "reason": "1–2 sentences: what you see on each side and why you decided this.",
                  "confidence": 0.00–1.00,
                
                  "front": {
                    "hasPortrait": bool,
                    "hasName": bool,
                    "hasDOB": bool,
                    "hasLicenseNumber": bool,
                    "hasExpiryDate": bool,
                    "hasHologram": bool,
                    "hasGhostImage": bool,
                    "detectedFormat": "card" | "booklet" | "paper" | "digital_screen" | "unknown",
                    "detectedCountry": "string or null"
                  },
                
                  "back": {
                    "hasCategoryTable": bool,
                    "hasBarcode": bool,
                    "hasMRZ": bool,
                    "hasMagneticStripe": bool,
                    "hasSignature": bool
                  },
                
                  "checks": {
                    "isPhysicalCard": bool,
                    "isImageQualityOk": bool,
                    "isFrontAndBackPaired": bool,
                    "isDriverLicenseLike": bool,
                    "wrongSidesPaired": bool
                  },
                
                  "fraud": {
                    "containsMoney": bool,
                    "containsScreenshot": bool,
                    "containsNonLicenseDoc": bool,
                    "containsLogoOnly": bool,
                    "hasEditingSuspicion": bool,
                    "hasMismatchedSides": bool
                  },
                
                  "redFlags": [],
                  "warnings": []
                }
                
                Confidence guide:
                0.90–1.00 → portrait + full data fields on front; full category table + barcode on back; no anomalies
                0.75–0.89 → portrait present; most fields visible; table present but partially obscured
                0.55–0.74 → portrait present; back is ambiguous or worn
                0.40–0.54 → one side clear, other side missing or unclear
                0.00–0.39 → likely not a license; rejection signal present
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