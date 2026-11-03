package com.university.worksheet.document;

import com.university.worksheet.dto.GenerateDocumentRequestDto;
import com.university.worksheet.dto.WorksheetContentDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxGeneratorServiceTest {

    @Test
    void generatesNonEmptyDocxForEachSupportedPageCount() {
        DocxGeneratorService service = new DocxGeneratorService();
        for (int pages = 2; pages <= 6; pages++) {
            GenerateDocumentRequestDto request = sampleRequest(pages);
            byte[] docx = service.generate(request);
            assertTrue(docx.length > 0, "DOCX should not be empty for pageCount=" + pages);
        }
    }

    private GenerateDocumentRequestDto sampleRequest(int pageCount) {
        WorksheetContentDto content = new WorksheetContentDto();
        content.setExperimentTitle("Signup Page using Node.js and Express");
        content.setAim("To design and implement a signup page using Node.js and Express.");
        content.setObjectives(List.of("Understand Express routing", "Implement form validation"));
        content.setInput(List.of("User name, email, password"));
        content.setApparatusUsed(List.of("Node.js", "Express", "VS Code"));
        content.setCodeImplementation(List.of(
                new WorksheetContentDto.CodeFileDto("app.js", "const express = require('express');\napp.listen(3000);")
        ));
        content.setOutput("Signup form renders and accepts user input successfully.");
        content.setLearningOutcomes(List.of("Learned Express routing", "Learned form handling"));

        GenerateDocumentRequestDto request = new GenerateDocumentRequestDto();
        request.setName("Test Student");
        request.setUid("UID001");
        request.setClassName("CSE-1");
        request.setSubject("Backend Technologies");
        request.setSubjectCode("CSE301");
        request.setDate("2026-09-15");
        request.setExperimentNumber("02");
        request.setPageCount(pageCount);
        request.setContent(content);
        return request;
    }
}
