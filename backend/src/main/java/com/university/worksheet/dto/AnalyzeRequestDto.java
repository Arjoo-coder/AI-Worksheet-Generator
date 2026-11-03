package com.university.worksheet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Input to POST /api/worksheet/analyze - the raw pasted LMS content. */
public class AnalyzeRequestDto {

    @NotBlank(message = "Raw content must not be empty")
    private String rawContent;

    /** Optional hint, e.g. subject name, to help the AI disambiguate the topic. */
    private String subjectHint;

    /**
     * Target worksheet length in pages. Guides how much content the AI
     * produces per section so the DOCX ends up roughly this many pages,
     * without padding with filler or cutting essential content.
     */
    @Min(value = 2, message = "Number of pages must be between 2 and 6")
    @Max(value = 6, message = "Number of pages must be between 2 and 6")
    private int pageCount = 4;

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }
    public String getSubjectHint() { return subjectHint; }
    public void setSubjectHint(String subjectHint) { this.subjectHint = subjectHint; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
}
