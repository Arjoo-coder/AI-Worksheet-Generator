package com.university.worksheet.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.worksheet.config.AiProperties;
import com.university.worksheet.dto.WorksheetContentDto;
import com.university.worksheet.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Talks to the configured AI provider.
 *
 * DEFAULT / PRIMARY PROVIDER: local Ollama, called via its native
 * "/api/chat" endpoint (NOT the OpenAI-compatibility route). Ollama needs
 * no API key and never receives an Authorization header.
 *
 * An OpenAI-compatible provider is still supported (app.ai.provider=openai
 * or anything other than "ollama") purely so a hosted fallback remains
 * possible later - it is never the default and is never required for this
 * app to work.
 *
 * Raw Content -> AiPromptProvider -> AiService -> Ollama -> WorksheetContentDto -> DocxGeneratorService
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiProperties aiProperties;
    private final AiPromptProvider promptProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiService(AiProperties aiProperties, AiPromptProvider promptProvider, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.promptProvider = promptProvider;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public WorksheetContentDto analyzeContent(String rawContent, String subjectHint, int pageCount) {
        return analyzeContent(rawContent, subjectHint, pageCount, "");
    }

    /**
     * Same as the 3-arg overload, with an extra reinforcement instruction
     * folded into the prompt - used by WorksheetService for a single bounded
     * quality-driven regeneration (aim word count / title validity), kept
     * separate from the malformed-JSON retry loop below.
     */
    public WorksheetContentDto analyzeContent(String rawContent, String subjectHint, int pageCount, String extraInstruction) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Raw content must not be empty");
        }

        // Non-Ollama providers need a key; Ollama never does.
        if (!aiProperties.isOllama() && (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI service is not configured on the server (missing API key for provider '" + aiProperties.getProvider() + "').");
        }

        log.info("AI request: provider={} model={} apiUrl={} timeoutSeconds={} maxTokens={}",
                aiProperties.getProvider(), aiProperties.getModel(), aiProperties.getApiUrl(),
                aiProperties.getTimeoutSeconds(), aiProperties.getMaxTokens());

        String trimmed = truncateIfNeeded(rawContent);
        String prompt = promptProvider.buildPrompt(trimmed, subjectHint, pageCount, extraInstruction);

        int totalAttempts = 1 + Math.max(0, aiProperties.getMaxRetries());
        ApiException lastFailure = null;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                String rawResponse = callModel(prompt, attempt);
                String jsonOnly = extractJson(rawResponse);
                return objectMapper.readValue(jsonOnly, WorksheetContentDto.class);
            } catch (ApiException e) {
                throw e; // provider/network failures are not retried
            } catch (Exception parseFailure) {
                log.warn("AI returned malformed JSON on attempt {}/{}: {}", attempt, totalAttempts, parseFailure.getMessage());
                lastFailure = new ApiException(HttpStatus.BAD_GATEWAY,
                        "The AI service returned an unexpected response. Please try regenerating.");
            }
        }
        throw lastFailure;
    }

    private String callModel(String prompt, int attempt) {
        String strictnessSuffix = attempt > 1
                ? "\n\nIMPORTANT: Your previous response was not valid JSON. Return ONLY valid JSON, nothing else."
                : "";

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "You return only strict JSON, with no markdown fences and no commentary."),
                Map.of("role", "user", "content", prompt + strictnessSuffix)
        );

        Map<String, Object> requestBody = aiProperties.isOllama()
                ? buildOllamaRequestBody(messages)
                : buildOpenAiCompatibleRequestBody(messages);

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build AI request", e);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(aiProperties.getApiUrl()))
                .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson));

        // Ollama: never send Authorization, even if an apiKey happens to be set.
        if (!aiProperties.isOllama() && aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + aiProperties.getApiKey());
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException te) {
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                    "The AI service took too long to respond (timeout=" + aiProperties.getTimeoutSeconds()
                            + "s). If qwen2.5 is generating slowly, increase LLM_TIMEOUT_SECONDS.");
        } catch (Exception e) {
            String hint = aiProperties.isOllama()
                    ? "Make sure Ollama is running (ollama serve) and reachable at " + aiProperties.getApiUrl() + "."
                    : "Could not reach the AI service.";
            throw new ApiException(HttpStatus.BAD_GATEWAY, hint + " Please try again.", e);
        }

        if (response.statusCode() / 100 != 2) {
            log.error("AI provider returned HTTP {} from {} : {}", response.statusCode(), aiProperties.getApiUrl(), response.body());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI service returned an error. Please try again later.");
        }

        return extractAssistantContent(response.body());
    }

    private Map<String, Object> buildOllamaRequestBody(List<Map<String, String>> messages) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0.3);
        options.put("num_predict", aiProperties.getMaxTokens());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", messages);
        body.put("stream", false);
        body.put("options", options);
        return body;
    }

    private Map<String, Object> buildOpenAiCompatibleRequestBody(List<Map<String, String>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", aiProperties.getMaxTokens());
        body.put("stream", false);
        return body;
    }

    /**
     * Reads the assistant's text out of the provider response. Tries the
     * native Ollama shape first ("/message/content"), then falls back to
     * the OpenAI-compatible shape ("/choices/0/message/content") for
     * robustness - but Ollama's native format is what's actually expected
     * by default.
     */
    private String extractAssistantContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode ollamaContent = root.at("/message/content");
            if (!ollamaContent.isMissingNode() && !ollamaContent.isNull() && !ollamaContent.asText().isBlank()) {
                return ollamaContent.asText();
            }

            JsonNode openAiContent = root.at("/choices/0/message/content");
            if (!openAiContent.isMissingNode() && !openAiContent.isNull() && !openAiContent.asText().isBlank()) {
                return openAiContent.asText();
            }

            throw new IllegalStateException("No content found at /message/content or /choices/0/message/content");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unexpected AI response format.", e);
        }
    }

    /** Strips markdown code fences etc. in case the model ignores the "raw JSON only" instruction. */
    private String extractJson(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalStateException("No JSON object found in AI response");
        }
        return t.substring(start, end + 1);
    }

    /**
     * Simple truncation guard for very large LMS pastes. A production V2 could
     * chunk + summarize-then-merge; V1 caps input size so nothing hangs.
     */
    private String truncateIfNeeded(String rawContent) {
        int max = aiProperties.getMaxContentChars();
        if (rawContent.length() <= max) {
            return rawContent;
        }
        log.warn("Raw content length {} exceeds max {}, truncating", rawContent.length(), max);
        return rawContent.substring(0, max);
    }
}
