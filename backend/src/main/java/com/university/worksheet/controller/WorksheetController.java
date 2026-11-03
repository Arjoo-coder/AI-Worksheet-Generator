package com.university.worksheet.controller;

import com.university.worksheet.ai.WorksheetService;
import com.university.worksheet.dto.AnalyzeRequestDto;
import com.university.worksheet.dto.WorksheetContentDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worksheet")
public class WorksheetController {

    private final WorksheetService worksheetService;

    public WorksheetController(WorksheetService worksheetService) {
        this.worksheetService = worksheetService;
    }

    /** Raw LMS content -> AI -> structured worksheet JSON (pageCount guides section depth). */
    @PostMapping("/analyze")
    public WorksheetContentDto analyze(@Valid @RequestBody AnalyzeRequestDto request) {
        return worksheetService.analyze(request.getRawContent(), request.getSubjectHint(), request.getPageCount());
    }
}
