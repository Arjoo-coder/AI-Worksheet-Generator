package com.university.worksheet.document;

/**
 * Centralized style/template configuration for the generated worksheet.
 * No formatting values are hard-coded inside DocxGeneratorService - they all
 * come from here.
 *
 * Font sizes follow the exact spec:
 *   - Main headings, student info, worksheet heading: 14pt bold
 *   - Body text: 12pt
 *   - Code: 12pt monospace
 *
 * Layout follows the reference worksheet structure: two logos in the header,
 * "Worksheet No - XX" heading, a borderless two-column student-info block,
 * then the six numbered experiment sections. The document is NOT paginated
 * with forced page breaks - content flows naturally across as many pages as
 * it needs; the selected page count only influences how much the AI writes
 * per section (see AiPromptProvider / worksheet-prompt.txt).
 */
public final class DocumentStyle {

    private DocumentStyle() {}

    // ---- Page (A4, twips) ----
    public static final int PAGE_WIDTH_TWIPS = 11906;
    public static final int PAGE_HEIGHT_TWIPS = 16838;
    public static final int MARGIN_TOP_TWIPS = 1296;    // 0.9"
    public static final int MARGIN_BOTTOM_TWIPS = 1296;
    public static final int MARGIN_LEFT_TWIPS = 1350;   // ~0.94"
    public static final int MARGIN_RIGHT_TWIPS = 1350;

    // ---- Fonts ----
    public static final String BODY_FONT = "Times New Roman";
    public static final String CODE_FONT = "Consolas";
    public static final String HEADER_FOOTER_FONT = "Calibri";

    // ---- Sizes (half-points, per POI convention: 24 = 12pt) ----
    public static final int TITLE_FONT_SIZE = 28;         // 14pt - "Worksheet No - XX"
    public static final int HEADING_FONT_SIZE = 28;       // 14pt - numbered section headings
    public static final int BODY_FONT_SIZE = 24;          // 12pt - normal body text
    public static final int CODE_FONT_SIZE = 24;          // 12pt - code
    public static final int INFO_FONT_SIZE = 28;           // 14pt bold - student information
    public static final int HEADER_FOOTER_FONT_SIZE = 20; // 10pt - header/footer chrome

    // ---- Spacing (twips) ----
    public static final int PARAGRAPH_SPACING_AFTER = 160;
    public static final int SECTION_SPACING_BEFORE = 220;
    public static final int INFO_ROW_SPACING_AFTER = 100;

    // ---- Colors (hex, no leading #) ----
    public static final String HEADING_COLOR = "1F3864";
    public static final String CODE_BORDER_COLOR = "BFBFBF";

    // ---- Header logos ----
    public static final String LOGO_UNIVERSITY_RESOURCE = "logos/university-logo.png";
    public static final String LOGO_NAAC_RESOURCE = "logos/naac-logo.png";
    /** Target rendered logo height, in EMUs (914400 EMU = 1 inch). Width is derived to preserve aspect ratio. */
    public static final int LOGO_TARGET_HEIGHT_EMU = 620000; // ~0.68"
    public static final String HEADER_INSTITUTION_LINE = "Chandigarh University";
    public static final String HEADER_NAAC_FALLBACK_LINE = "NAAC A+";

    // ---- Footer text ----
    public static final String FOOTER_LABEL_PREFIX = "Page ";
}
