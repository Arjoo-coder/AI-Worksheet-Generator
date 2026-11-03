package com.university.worksheet.ai;

import com.university.worksheet.dto.WorksheetContentDto;
import com.university.worksheet.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration layer between the controller and AiService. Also
 * enforces the two "strict" content-quality rules that matter most for a
 * usable worksheet:
 *   - aim must be 40-60 words
 *   - experimentTitle must be present and not literally "Title"
 * If the first AI response misses either, one bounded regeneration attempt
 * is made with an explicit correction instruction. If it's still off after
 * that, the worksheet is returned as-is (with a warning logged) rather than
 * failing the whole request or looping indefinitely against a local model.
 */
@Service
public class WorksheetService {

    private static final Logger log = LoggerFactory.getLogger(WorksheetService.class);

    private static final int MIN_CONTENT_LENGTH = 20;
    private static final int AIM_MIN_WORDS = 40;
    private static final int AIM_MAX_WORDS = 60;

    private final AiService aiService;

    public WorksheetService(AiService aiService) {
        this.aiService = aiService;
    }

    public WorksheetContentDto analyze(String rawContent, String subjectHint, int pageCount) {
        if (rawContent == null || rawContent.trim().length() < MIN_CONTENT_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Pasted content is too short to generate a worksheet from. Please paste the full LMS content.");
        }
        int safePageCount = Math.max(2, Math.min(6, pageCount <= 0 ? 4 : pageCount));
        String trimmedContent = rawContent.trim();

        WorksheetContentDto content = aiService.analyzeContent(trimmedContent, subjectHint, safePageCount);

        String issue = describeQualityIssue(content);
        if (issue != null) {
            log.warn("AI response failed quality check ({}); attempting one bounded regeneration.", issue);
            WorksheetContentDto retried = aiService.analyzeContent(trimmedContent, subjectHint, safePageCount, issue);
            if (describeQualityIssue(retried) == null) {
                return retried;
            }
            log.warn("Regeneration still did not satisfy the quality check - returning the latest response as-is.");
            return retried;
        }
        return content;
    }

    /** Returns null if content passes, or a human-readable correction instruction if it doesn't. */
    private String describeQualityIssue(WorksheetContentDto content) {
        StringBuilder issue = new StringBuilder();

        String title = content.getExperimentTitle();
        if (title == null || title.isBlank() || title.trim().equalsIgnoreCase("title")) {
            issue.append("The experimentTitle must be a specific, real practical/experiment title based on the ")
                    .append("supplied content (e.g. \"IoT-Based LED Control Using Arduino\") - it must never be empty ")
                    .append("or literally the word \"Title\". ");
        }

        int aimWords = wordCount(content.getAim());
        if (aimWords < AIM_MIN_WORDS || aimWords > AIM_MAX_WORDS) {
            issue.append("The aim must be between 40 and 60 words (it was ").append(aimWords)
                    .append(" words) - rewrite it as one meaningful academic paragraph in that exact range, ")
                    .append("based only on the supplied content, without padding with repeated sentences.");
        }

        return issue.length() == 0 ? null : issue.toString();
    }

    private int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
