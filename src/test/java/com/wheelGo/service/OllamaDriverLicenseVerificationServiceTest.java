package com.wheelGo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaDriverLicenseVerificationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OllamaDriverLicenseVerificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ollamaApiUrl", "http://127.0.0.1:1/api/chat");
        ReflectionTestUtils.setField(service, "ollamaModel", "test-model");
    }

    @Test
    void should_throw_internal_server_error_when_image_file_missing() {
        assertThatThrownBy(() -> service.verify(Path.of("missing-front.png"), Path.of("missing-back.png")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Failed to read stored driver license image");
    }

    @Test
    void should_throw_bad_gateway_when_ollama_request_fails() throws Exception {
        Path front = Files.createTempFile("front", ".png");
        Path back = Files.createTempFile("back", ".png");
        Files.write(front, new byte[]{1});
        Files.write(back, new byte[]{1});
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenReturn("{}");

        assertThatThrownBy(() -> service.verify(front, back))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Failed to parse Ollama verification result");
    }
}
