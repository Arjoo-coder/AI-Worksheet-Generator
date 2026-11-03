package com.university.worksheet.document;

import com.university.worksheet.dto.GenerateDocumentRequestDto;
import com.university.worksheet.dto.WorksheetContentDto;
import com.university.worksheet.exception.ApiException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import javax.imageio.ImageIO;

@Service
public class DocxGeneratorService {

    private static final Logger log =
            LoggerFactory.getLogger(DocxGeneratorService.class);

    /**
     * Generates the final DOCX worksheet.
     *
     * Formatting:
     * - A4
     * - No outer page border
     * - Header with two logos
     * - Worksheet number
     * - Borderless 2-column student information
     * - 14 pt bold headings
     * - 14 pt bold student information
     * - 12 pt body
     * - 12 pt code
     * - Natural document flow
     */
    public byte[] generate(GenerateDocumentRequestDto request) {

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Generate request must not be null."
            );
        }

        try (XWPFDocument doc = new XWPFDocument()) {

            // ---------------------------------------------
            // PAGE SETUP
            // ---------------------------------------------
            setPageAndMargins(doc);

            // ---------------------------------------------
            // HEADER
            // ---------------------------------------------
            addHeaderWithLogos(doc);

            // ---------------------------------------------
            // FOOTER
            // ---------------------------------------------
            addFooterWithPageNumbers(doc);

            // ---------------------------------------------
            // WORKSHEET NUMBER
            // ---------------------------------------------
            addWorksheetNoHeading(doc, request);

            // ---------------------------------------------
            // STUDENT INFORMATION
            // ---------------------------------------------
            addStudentInfoBlock(doc, request);

            // ---------------------------------------------
            // AI GENERATED CONTENT
            // ---------------------------------------------
            WorksheetContentDto content = request.getContent();

            if (content == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Worksheet content must not be empty."
                );
            }

            // 1. AIM
            addNumberedHeading(
                    doc,
                    "1. Aim/Overview of the practical:"
            );

            addBodyParagraph(
                    doc,
                    emptyFallback(
                            content.getAim(),
                            "Not specified."
                    )
            );

            // OBJECTIVES
            if (content.getObjectives() != null
                    && !content.getObjectives().isEmpty()) {

                addSubLabel(doc, "Objectives:");
                addBulletList(
                        doc,
                        content.getObjectives()
                );
            }

            // 2. APPARATUS
            addNumberedHeading(
                    doc,
                    "2. Apparatus"
            );

            addBulletList(
                    doc,
                    content.getApparatusUsed()
            );

            // 3. PROCEDURE / INPUT
            addNumberedHeading(
                    doc,
                    "3. Procedure / Input"
            );

            addBulletList(
                    doc,
                    content.getInput()
            );

            // 4. CODING
            addNumberedHeading(
                    doc,
                    "4. Coding:"
            );

            addCodeSections(
                    doc,
                    content.getCodeImplementation()
            );

            // 5. OUTPUT
            addNumberedHeading(
                    doc,
                    "5. Output:"
            );

            addBodyParagraph(
                    doc,
                    emptyFallback(
                            content.getOutput(),
                            "Not specified."
                    )
            );

            // 6. LEARNING OUTCOMES
            addNumberedHeading(
                    doc,
                    "6. Learning Outcomes:"
            );

            addNumberedList(
                    doc,
                    content.getLearningOutcomes()
            );

            // ---------------------------------------------
            // WRITE DOCX
            // ---------------------------------------------
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            doc.write(out);

            return out.toByteArray();

        } catch (ApiException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "DOCX generation failed",
                    e
            );

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate Word document.",
                    e
            );
        }
    }

    // =====================================================
    // PAGE SETUP
    // =====================================================

    private void setPageAndMargins(XWPFDocument doc) {

        CTSectPr sectPr =
                doc.getDocument()
                        .getBody()
                        .isSetSectPr()
                        ? doc.getDocument()
                                .getBody()
                                .getSectPr()
                        : doc.getDocument()
                                .getBody()
                                .addNewSectPr();

        // A4
        CTPageSz pageSz =
                sectPr.isSetPgSz()
                        ? sectPr.getPgSz()
                        : sectPr.addNewPgSz();

        pageSz.setW(
                BigInteger.valueOf(
                        DocumentStyle.PAGE_WIDTH_TWIPS
                )
        );

        pageSz.setH(
                BigInteger.valueOf(
                        DocumentStyle.PAGE_HEIGHT_TWIPS
                )
        );

        pageSz.setOrient(
                STPageOrientation.PORTRAIT
        );

        // Margins
        CTPageMar pageMar =
                sectPr.isSetPgMar()
                        ? sectPr.getPgMar()
                        : sectPr.addNewPgMar();

        pageMar.setTop(
                BigInteger.valueOf(
                        DocumentStyle.MARGIN_TOP_TWIPS
                )
        );

        pageMar.setBottom(
                BigInteger.valueOf(
                        DocumentStyle.MARGIN_BOTTOM_TWIPS
                )
        );

        pageMar.setLeft(
                BigInteger.valueOf(
                        DocumentStyle.MARGIN_LEFT_TWIPS
                )
        );

        pageMar.setRight(
                BigInteger.valueOf(
                        DocumentStyle.MARGIN_RIGHT_TWIPS
                )
        );

        // IMPORTANT:
        // No page border is added.
    }

    // =====================================================
    // HEADER
    // =====================================================

    private void addHeaderWithLogos(XWPFDocument doc) {

        /*
         * IMPORTANT:
         *
         * Apache POI 5.3.0 compatible implementation.
         *
         * Do NOT use:
         * HeaderFooterType.DEFAULT
         *
         * We use:
         * STHdrFtr.DEFAULT
         */

        XWPFHeaderFooterPolicy policy =
                new XWPFHeaderFooterPolicy(doc);

        XWPFHeader header =
                policy.createHeader(
                        STHdrFtr.DEFAULT
                );

        // Create one borderless row with two cells.
        XWPFTable logoTable =
                header.createTable(1, 2);

        logoTable.setWidth("100%");

        styleTableBordersNone(logoTable);

        // ---------------------------------------------
        // LEFT LOGO
        // ---------------------------------------------

        XWPFTableCell leftCell =
                logoTable
                        .getRow(0)
                        .getCell(0);

        removeAllCellParagraphs(leftCell);

        XWPFParagraph leftParagraph =
                leftCell.addParagraph();

        leftParagraph.setAlignment(
                ParagraphAlignment.LEFT
        );

        leftParagraph.setSpacingBefore(0);
        leftParagraph.setSpacingAfter(0);

        boolean universityLogoInserted =
                insertLogo(
                        leftParagraph,
                        DocumentStyle.LOGO_UNIVERSITY_RESOURCE
                );

        if (!universityLogoInserted) {

            XWPFRun fallback =
                    leftParagraph.createRun();

            styleRun(
                    fallback,
                    DocumentStyle.HEADER_INSTITUTION_LINE,
                    DocumentStyle.HEADER_FOOTER_FONT,
                    DocumentStyle.HEADER_FOOTER_FONT_SIZE,
                    true,
                    false
            );
        }

        // ---------------------------------------------
        // RIGHT LOGO
        // ---------------------------------------------

        XWPFTableCell rightCell =
                logoTable
                        .getRow(0)
                        .getCell(1);

        removeAllCellParagraphs(rightCell);

        XWPFParagraph rightParagraph =
                rightCell.addParagraph();

        rightParagraph.setAlignment(
                ParagraphAlignment.RIGHT
        );

        rightParagraph.setSpacingBefore(0);
        rightParagraph.setSpacingAfter(0);

        boolean naacLogoInserted =
                insertLogo(
                        rightParagraph,
                        DocumentStyle.LOGO_NAAC_RESOURCE
                );

        if (!naacLogoInserted) {

            XWPFRun fallback =
                    rightParagraph.createRun();

            styleRun(
                    fallback,
                    DocumentStyle.HEADER_NAAC_FALLBACK_LINE,
                    DocumentStyle.HEADER_FOOTER_FONT,
                    DocumentStyle.HEADER_FOOTER_FONT_SIZE,
                    true,
                    false
            );
        }
    }

    // =====================================================
    // INSERT LOGO
    // =====================================================

    private boolean insertLogo(
            XWPFParagraph paragraph,
            String classpathResource) {

        try (
                InputStream inputStream =
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(
                                        classpathResource
                                )
        ) {

            if (inputStream == null) {

                log.warn(
                        "Logo resource not found: {}",
                        classpathResource
                );

                return false;
            }

            byte[] imageBytes =
                    inputStream.readAllBytes();

            BufferedImage image =
                    ImageIO.read(
                            new ByteArrayInputStream(
                                    imageBytes
                            )
                    );

            if (image == null) {

                log.warn(
                        "Unable to decode logo: {}",
                        classpathResource
                );

                return false;
            }

            int targetHeightEmu =
                    DocumentStyle.LOGO_TARGET_HEIGHT_EMU;

            int targetWidthEmu =
                    (int) Math.round(
                            targetHeightEmu
                                    * (
                                    image.getWidth()
                                            / (double)
                                    image.getHeight()
                            )
                    );

            XWPFRun run =
                    paragraph.createRun();

            run.addPicture(
                    new ByteArrayInputStream(
                            imageBytes
                    ),
                    Document.PICTURE_TYPE_PNG,
                    classpathResource,
                    targetWidthEmu,
                    targetHeightEmu
            );

            return true;

        } catch (
                InvalidFormatException |
                IOException e
        ) {

            log.warn(
                    "Could not insert logo {}: {}",
                    classpathResource,
                    e.getMessage()
            );

            return false;
        }
    }

    // =====================================================
    // FOOTER
    // =====================================================

    private void addFooterWithPageNumbers(
            XWPFDocument doc) {

        /*
         * Same POI 5.3.0 compatible policy.
         */

        XWPFHeaderFooterPolicy policy =
                new XWPFHeaderFooterPolicy(doc);

        XWPFFooter footer =
                policy.createFooter(
                        STHdrFtr.DEFAULT
                );

        XWPFParagraph paragraph =
                footer.getParagraphArray(0);

        if (paragraph == null) {
            paragraph = footer.createParagraph();
        }

        paragraph.setAlignment(
                ParagraphAlignment.CENTER
        );

        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);

        XWPFRun prefix =
                paragraph.createRun();

        styleRun(
                prefix,
                DocumentStyle.FOOTER_LABEL_PREFIX,
                DocumentStyle.HEADER_FOOTER_FONT,
                DocumentStyle.HEADER_FOOTER_FONT_SIZE,
                false,
                false
        );

        addField(
                paragraph,
                "PAGE"
        );

        XWPFRun middle =
                paragraph.createRun();

        styleRun(
                middle,
                " of ",
                DocumentStyle.HEADER_FOOTER_FONT,
                DocumentStyle.HEADER_FOOTER_FONT_SIZE,
                false,
                false
        );

        addField(
                paragraph,
                "NUMPAGES"
        );
    }

    // =====================================================
    // WORD FIELD
    // =====================================================

    private void addField(
            XWPFParagraph paragraph,
            String fieldName) {

        CTP ctp =
                paragraph.getCTP();

        // BEGIN
        CTR beginRun =
                ctp.addNewR();

        beginRun
                .addNewFldChar()
                .setFldCharType(
                        STFldCharType.BEGIN
                );

        // INSTRUCTION
        CTR instructionRun =
                ctp.addNewR();

        instructionRun
                .addNewInstrText()
                .setStringValue(
                        " " + fieldName + " "
                );

        // SEPARATE
        CTR separateRun =
                ctp.addNewR();

        separateRun
                .addNewFldChar()
                .setFldCharType(
                        STFldCharType.SEPARATE
                );

        // RESULT
        CTR resultRun =
                ctp.addNewR();

        resultRun
                .addNewT()
                .setStringValue("1");

        // END
        CTR endRun =
                ctp.addNewR();

        endRun
                .addNewFldChar()
                .setFldCharType(
                        STFldCharType.END
                );
    }

    // =====================================================
    // WORKSHEET NUMBER
    // =====================================================

    private void addWorksheetNoHeading(
            XWPFDocument doc,
            GenerateDocumentRequestDto request) {

        String number =
                request.getExperimentNumber();

        if (number == null) {
            number = "";
        }

        number = number.trim();

        XWPFParagraph paragraph =
                doc.createParagraph();

        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(
                DocumentStyle.PARAGRAPH_SPACING_AFTER
        );

        XWPFRun run =
                paragraph.createRun();

        String text;

        if (number.isBlank()) {
            text = "Worksheet";
        } else {
            text = "Worksheet No - " + number;
        }

        run.setText(text);

        run.setBold(true);

        run.setFontFamily(
                DocumentStyle.BODY_FONT
        );

        /*
         * DocumentStyle values appear to be stored
         * in half-points, therefore /2 is retained.
         */
        run.setFontSize(
                DocumentStyle.TITLE_FONT_SIZE / 2
        );
    }

    // =====================================================
    // STUDENT INFORMATION
    // =====================================================

    private void addStudentInfoBlock(
            XWPFDocument doc,
            GenerateDocumentRequestDto request) {

        /*
         * 4 rows x 2 columns
         *
         * Borderless table is used only to guarantee
         * perfect horizontal alignment.
         */

        XWPFTable table =
                doc.createTable(4, 2);

        table.setWidth("100%");

        styleTableBordersNone(table);

        // Fixed column widths without calling
        // isSetTblGrid() on the XWPF table object.
        setColumnWidths(
                table,
                50,
                50
        );

        // Row 1
        setInfoCell(
                table,
                0,
                0,
                "Student Name",
                request.getName()
        );

        setInfoCell(
                table,
                0,
                1,
                "UID",
                request.getUid()
        );

        // Row 2
        String branch =
                valueOrFallback(
                        request.getBranch(),
                        request.getClassName()
                );

        setInfoCell(
                table,
                1,
                0,
                "Branch",
                branch
        );

        setInfoCell(
                table,
                1,
                1,
                "Section/Group",
                request.getSectionGroup()
        );

        // Row 3
        setInfoCell(
                table,
                2,
                0,
                "Semester",
                request.getSemester()
        );

        setInfoCell(
                table,
                2,
                1,
                "Date of Performance",
                request.getDate()
        );

        // Row 4
        setInfoCell(
                table,
                3,
                0,
                "Subject Name",
                request.getSubject()
        );

        setInfoCell(
                table,
                3,
                1,
                "Subject Code",
                request.getSubjectCode()
        );

        // Space after student information.
        XWPFParagraph spacer =
                doc.createParagraph();

        spacer.setSpacingBefore(0);

        spacer.setSpacingAfter(
                DocumentStyle.SECTION_SPACING_BEFORE
        );
    }

    // =====================================================
    // STUDENT INFO CELL
    // =====================================================

    private void setInfoCell(
            XWPFTable table,
            int row,
            int column,
            String label,
            String value) {

        XWPFTableCell cell =
                table
                        .getRow(row)
                        .getCell(column);

        removeAllCellParagraphs(cell);

        XWPFParagraph paragraph =
                cell.addParagraph();

        paragraph.setAlignment(
                ParagraphAlignment.LEFT
        );

        paragraph.setSpacingBefore(0);

        paragraph.setSpacingAfter(
                DocumentStyle.INFO_ROW_SPACING_AFTER
        );

        paragraph.setSpacingBetween(1.0);

        XWPFRun run =
                paragraph.createRun();

        String finalValue =
                value == null || value.isBlank()
                        ? ""
                        : value.trim();

        run.setText(
                label + ": " + finalValue
        );

        run.setBold(true);

        run.setFontFamily(
                DocumentStyle.BODY_FONT
        );

        /*
         * User information = 14 pt.
         */
        run.setFontSize(
                DocumentStyle.INFO_FONT_SIZE / 2
        );
    }

    // =====================================================
    // COLUMN WIDTHS
    // =====================================================

    private void setColumnWidths(
            XWPFTable table,
            int leftPercent,
            int rightPercent) {

        int totalTwips =
                DocumentStyle.PAGE_WIDTH_TWIPS
                        - DocumentStyle.MARGIN_LEFT_TWIPS
                        - DocumentStyle.MARGIN_RIGHT_TWIPS;

        int leftWidth =
                totalTwips * leftPercent / 100;

        int rightWidth =
                totalTwips * rightPercent / 100;

        /*
         * Directly create a table grid.
         *
         * This avoids the previous isSetTblGrid()
         * compatibility issue.
         */

        CTTbl ctTbl =
                table.getCTTbl();

        CTTblGrid grid =
                ctTbl.addNewTblGrid();

        CTTblGridCol leftColumn =
                grid.addNewGridCol();

        leftColumn.setW(
                BigInteger.valueOf(
                        leftWidth
                )
        );

        CTTblGridCol rightColumn =
                grid.addNewGridCol();

        rightColumn.setW(
                BigInteger.valueOf(
                        rightWidth
                )
        );
    }

    // =====================================================
    // REMOVE CELL PARAGRAPHS
    // =====================================================

    private void removeAllCellParagraphs(
            XWPFTableCell cell) {

        while (
                cell.getParagraphs().size() > 0
        ) {

            cell.removeParagraph(0);
        }
    }

    // =====================================================
    // REMOVE TABLE BORDERS
    // =====================================================

    private void styleTableBordersNone(
            XWPFTable table) {

        table.setInsideHBorder(
                XWPFTable.XWPFBorderType.NONE,
                0,
                0,
                "auto"
        );

        table.setInsideVBorder(
                XWPFTable.XWPFBorderType.NONE,
                0,
                0,
                "auto"
        );

        table.setTopBorder(
                XWPFTable.XWPFBorderType.NONE,
                0,
                0,
                "auto"
        );

        table.setBottomBorder(
                XWPFTable.XWPFBorderType.NONE,
                0,
                0,
                "auto"
        );

        table.setLeftBorder(
                XWPFTable.XWPFBorderType.NONE,
                0,
                0,
                "auto"
        );

        table.setRightBorder(
                XWPFTable.XWPFBorderType.NONE,
                0,
                0,
                "auto"
        );
    }

    // =====================================================
    // MAIN SECTION HEADING
    // =====================================================

    private void addNumberedHeading(
            XWPFDocument doc,
            String text) {

        XWPFParagraph paragraph =
                doc.createParagraph();

        paragraph.setSpacingBefore(
                DocumentStyle.SECTION_SPACING_BEFORE
        );

        paragraph.setSpacingAfter(
                DocumentStyle.PARAGRAPH_SPACING_AFTER
        );

        XWPFRun run =
                paragraph.createRun();

        run.setText(text);

        run.setBold(true);

        run.setFontFamily(
                DocumentStyle.BODY_FONT
        );

        /*
         * 14 pt heading.
         */
        run.setFontSize(
                DocumentStyle.HEADING_FONT_SIZE / 2
        );

        if (DocumentStyle.HEADING_COLOR != null
                && !DocumentStyle.HEADING_COLOR.isBlank()) {

            run.setColor(
                    DocumentStyle.HEADING_COLOR
            );
        }
    }

    // =====================================================
    // SUB LABEL
    // =====================================================

    private void addSubLabel(
            XWPFDocument doc,
            String text) {

        XWPFParagraph paragraph =
                doc.createParagraph();

        paragraph.setSpacingBefore(0);

        paragraph.setSpacingAfter(80);

        XWPFRun run =
                paragraph.createRun();

        run.setText(text);

        run.setBold(true);

        run.setFontFamily(
                DocumentStyle.BODY_FONT
        );

        run.setFontSize(
                DocumentStyle.BODY_FONT_SIZE / 2
        );
    }

    // =====================================================
    // BODY PARAGRAPH
    // =====================================================

    private void addBodyParagraph(
            XWPFDocument doc,
            String text) {

        if (text == null || text.isBlank()) {
            return;
        }

        String normalized =
                text.replace("\r\n", "\n")
                        .replace("\r", "\n");

        String[] blocks =
                normalized.split("\n\\s*\n");

        for (String block : blocks) {

            if (block == null || block.isBlank()) {
                continue;
            }

            String[] lines =
                    block.split("\n");

            XWPFParagraph paragraph =
                    doc.createParagraph();

            paragraph.setAlignment(
                    ParagraphAlignment.BOTH
            );

            paragraph.setSpacingBefore(0);

            paragraph.setSpacingAfter(
                    DocumentStyle.PARAGRAPH_SPACING_AFTER
            );

            paragraph.setSpacingBetween(1.15);

            for (int i = 0; i < lines.length; i++) {

                XWPFRun run =
                        paragraph.createRun();

                run.setText(
                        lines[i].trim()
                );

                run.setFontFamily(
                        DocumentStyle.BODY_FONT
                );

                run.setFontSize(
                        DocumentStyle.BODY_FONT_SIZE / 2
                );

                run.setBold(false);

                if (i < lines.length - 1) {
                    run.addBreak();
                }
            }
        }
    }

    // =====================================================
    // BULLET LIST
    // =====================================================

    private void addBulletList(
            XWPFDocument doc,
            List<String> items) {

        if (items == null || items.isEmpty()) {

            addBodyParagraph(
                    doc,
                    "Not specified."
            );

            return;
        }

        for (String item : items) {

            if (item == null || item.isBlank()) {
                continue;
            }

            XWPFParagraph paragraph =
                    doc.createParagraph();

            paragraph.setIndentationLeft(360);

            paragraph.setIndentationHanging(180);

            paragraph.setSpacingBefore(0);

            paragraph.setSpacingAfter(80);

            XWPFRun run =
                    paragraph.createRun();

            run.setText(
                    "\u2022 " + item.trim()
            );

            run.setFontFamily(
                    DocumentStyle.BODY_FONT
            );

            run.setFontSize(
                    DocumentStyle.BODY_FONT_SIZE / 2
            );
        }
    }

    // =====================================================
    // NUMBERED LIST
    // =====================================================

    private void addNumberedList(
            XWPFDocument doc,
            List<String> items) {

        if (items == null || items.isEmpty()) {

            addBodyParagraph(
                    doc,
                    "Not specified."
            );

            return;
        }

        int index = 1;

        for (String item : items) {

            if (item == null || item.isBlank()) {
                continue;
            }

            XWPFParagraph paragraph =
                    doc.createParagraph();

            paragraph.setIndentationLeft(360);

            paragraph.setIndentationHanging(180);

            paragraph.setSpacingBefore(0);

            paragraph.setSpacingAfter(80);

            XWPFRun run =
                    paragraph.createRun();

            run.setText(
                    index + ". " + item.trim()
            );

            run.setFontFamily(
                    DocumentStyle.BODY_FONT
            );

            run.setFontSize(
                    DocumentStyle.BODY_FONT_SIZE / 2
            );

            index++;
        }
    }

    // =====================================================
    // CODE SECTIONS
    // =====================================================

    private void addCodeSections(
            XWPFDocument doc,
            List<WorksheetContentDto.CodeFileDto> codeFiles) {

        if (codeFiles == null || codeFiles.isEmpty()) {

            addBodyParagraph(
                    doc,
                    "No source code identified in the supplied content."
            );

            return;
        }

        for (
                WorksheetContentDto.CodeFileDto file
                : codeFiles
        ) {

            if (file == null) {
                continue;
            }

            // File name
            if (
                    file.getFileName() != null
                            && !file.getFileName().isBlank()
            ) {

                XWPFParagraph fileNameParagraph =
                        doc.createParagraph();

                fileNameParagraph.setSpacingBefore(0);

                fileNameParagraph.setSpacingAfter(60);

                XWPFRun fileNameRun =
                        fileNameParagraph.createRun();

                fileNameRun.setText(
                        file.getFileName()
                );

                fileNameRun.setBold(true);

                fileNameRun.setItalic(true);

                fileNameRun.setFontFamily(
                        DocumentStyle.BODY_FONT
                );

                fileNameRun.setFontSize(
                        DocumentStyle.BODY_FONT_SIZE / 2
                );
            }

            // Code table
            XWPFTable codeTable =
                    doc.createTable(1, 1);

            codeTable.setWidth("100%");

            // Code block border
            codeTable.setTopBorder(
                    XWPFTable.XWPFBorderType.SINGLE,
                    4,
                    0,
                    DocumentStyle.CODE_BORDER_COLOR
            );

            codeTable.setBottomBorder(
                    XWPFTable.XWPFBorderType.SINGLE,
                    4,
                    0,
                    DocumentStyle.CODE_BORDER_COLOR
            );

            codeTable.setLeftBorder(
                    XWPFTable.XWPFBorderType.SINGLE,
                    4,
                    0,
                    DocumentStyle.CODE_BORDER_COLOR
            );

            codeTable.setRightBorder(
                    XWPFTable.XWPFBorderType.SINGLE,
                    4,
                    0,
                    DocumentStyle.CODE_BORDER_COLOR
            );

            XWPFTableCell cell =
                    codeTable
                            .getRow(0)
                            .getCell(0);

            // Light background
            cell.setColor("F5F5F5");

            removeAllCellParagraphs(cell);

            String code =
                    file.getCode() == null
                            ? ""
                            : file.getCode();

            String[] lines =
                    code.split(
                            "\\R",
                            -1
                    );

            XWPFParagraph codeParagraph =
                    cell.addParagraph();

            codeParagraph.setSpacingBefore(0);
            codeParagraph.setSpacingAfter(0);
            codeParagraph.setSpacingBetween(1.0);

            for (int i = 0; i < lines.length; i++) {

                XWPFRun run =
                        codeParagraph.createRun();

                run.setText(
                        lines[i]
                                .replace(
                                        "\t",
                                        "    "
                                )
                );

                run.setFontFamily(
                        DocumentStyle.CODE_FONT
                );

                run.setFontSize(
                        DocumentStyle.CODE_FONT_SIZE / 2
                );

                if (i < lines.length - 1) {
                    run.addBreak();
                }
            }

            XWPFParagraph spacer =
                    doc.createParagraph();

            spacer.setSpacingBefore(0);

            spacer.setSpacingAfter(
                    DocumentStyle.PARAGRAPH_SPACING_AFTER
            );
        }
    }

    // =====================================================
    // GENERIC RUN FORMATTER
    // =====================================================

    private void styleRun(
            XWPFRun run,
            String text,
            String font,
            int halfPoints,
            boolean bold,
            boolean italic) {

        run.setText(
                text == null ? "" : text
        );

        run.setFontFamily(font);

        /*
         * Existing DocumentStyle stores sizes
         * as half-points.
         */
        run.setFontSize(
                halfPoints / 2
        );

        run.setBold(bold);

        run.setItalic(italic);
    }

    // =====================================================
    // VALUE FALLBACK
    // =====================================================

    private String valueOrFallback(
            String preferred,
            String fallback) {

        if (
                preferred != null
                        && !preferred.isBlank()
        ) {
            return preferred;
        }

        return fallback == null
                ? ""
                : fallback;
    }

    // =====================================================
    // EMPTY FALLBACK
    // =====================================================

    private String emptyFallback(
            String value,
            String fallback) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return fallback;
        }

        return value;
    }
}