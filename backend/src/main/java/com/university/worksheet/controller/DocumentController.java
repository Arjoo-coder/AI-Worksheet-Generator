package com.university.worksheet.controller;

import com.university.worksheet.document.DocxGeneratorService;
import com.university.worksheet.document.FilenameUtil;
import com.university.worksheet.document.PdfConversionService;
import com.university.worksheet.dto.GenerateDocumentRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    private static final MediaType DOCX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocxGeneratorService docxGeneratorService;
    private final PdfConversionService pdfConversionService;

    public DocumentController(DocxGeneratorService docxGeneratorService, PdfConversionService pdfConversionService) {
        this.docxGeneratorService = docxGeneratorService;
        this.pdfConversionService = pdfConversionService;
    }

    @PostMapping(value = "/docx", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> generateDocx(@Valid @RequestBody GenerateDocumentRequestDto request) {
        byte[] docx = docxGeneratorService.generate(request);
        String filename = FilenameUtil.buildBaseName(request.getSubject(), request.getExperimentNumber()) + ".docx";
        return fileResponse(docx, filename, DOCX_MEDIA_TYPE);
    }

    @PostMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdf(@Valid @RequestBody GenerateDocumentRequestDto request) {
        byte[] docx = docxGeneratorService.generate(request);
        String baseName = FilenameUtil.buildBaseName(request.getSubject(), request.getExperimentNumber());
        byte[] pdf = pdfConversionService.convertDocxToPdf(docx, baseName);
        return fileResponse(pdf, baseName + ".pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> fileResponse(byte[] bytes, String filename, MediaType mediaType) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(bytes);
    }
}
