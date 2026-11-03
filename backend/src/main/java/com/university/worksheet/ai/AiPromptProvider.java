package com.university.worksheet.ai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the AI prompt template from a resource file (rather than a large
 * string embedded in Java) and fills in the runtime placeholders, including
 * the requested page count.
 */
@Component
public class AiPromptProvider {

    private static final String TEMPLATE_PATH = "prompts/worksheet-prompt.txt";

    private final String template;

    public AiPromptProvider() {
        this.template = loadTemplate();
    }

    private String loadTemplate() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Prompt template not found on classpath: " + TEMPLATE_PATH);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt template", e);
        }
    }

    public String buildPrompt(String rawContent, String subjectHint, int pageCount) {
        return buildPrompt(rawContent, subjectHint, pageCount, "");
    }

    /**
     * Same as buildPrompt(rawContent, subjectHint, pageCount), with an extra
     * reinforcement instruction appended before the raw content - used for a
     * bounded quality-driven regeneration attempt (e.g. the Aim came back
     * under 40 words, or the title was empty/literally "Title").
     */
    public String buildPrompt(String rawContent, String subjectHint, int pageCount, String extraInstruction) {
        String hint = (subjectHint == null || subjectHint.isBlank()) ? "(none provided)" : subjectHint;
        String base = template
                .replace("{{SUBJECT_HINT}}", hint)
                .replace("{{PAGE_COUNT}}", String.valueOf(pageCount))
                .replace("{{RAW_CONTENT}}", rawContent);
        if (extraInstruction == null || extraInstruction.isBlank()) {
            return base;
        }
        return base + "\n\nIMPORTANT CORRECTION FOR THIS ATTEMPT:\n" + extraInstruction;
    }
}
