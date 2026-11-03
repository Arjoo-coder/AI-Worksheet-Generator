package com.university.worksheet.document;

import com.university.worksheet.config.PdfProperties;
import com.university.worksheet.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Converts a generated DOCX to PDF via headless LibreOffice
 * ("soffice --headless --convert-to pdf"). This is the most reliable way to
 * keep the PDF visually identical to the DOCX, since it's the exact same
 * rendering engine that opens the DOCX, rather than a second independent
 * PDF-layout implementation.
 *
 * Requires LibreOffice installed and the "soffice" (or soffice.exe) command
 * reachable on PATH, or app.pdf.soffice-path pointing directly at it.
 */
@Service
public class PdfConversionService {

    private static final Logger log = LoggerFactory.getLogger(PdfConversionService.class);

    private final PdfProperties pdfProperties;

    public PdfConversionService(PdfProperties pdfProperties) {
        this.pdfProperties = pdfProperties;
    }

    public byte[] convertDocxToPdf(byte[] docxBytes, String baseName) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("worksheet-pdf-");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to prepare temporary files for PDF conversion.", e);
        }

        Path docxPath = tempDir.resolve(baseName + ".docx");
        Path pdfPath = tempDir.resolve(baseName + ".pdf");

        try {
            Files.write(docxPath, docxBytes);

            List<String> command = List.of(
                    pdfProperties.getSofficePath(),
                    "--headless",
                    "--norestore",
                    "--convert-to", "pdf",
                    "--outdir", tempDir.toString(),
                    docxPath.toString()
            );

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(pdfProperties.getConversionTimeoutSeconds(), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes());

            if (!finished) {
                process.destroyForcibly();
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                        "PDF conversion timed out. Please try again.");
            }
            if (process.exitValue() != 0 || !Files.exists(pdfPath)) {
                log.error("LibreOffice conversion failed. Exit={}, output={}", process.exitValue(), output);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "PDF conversion failed. Make sure LibreOffice (soffice) is installed and reachable on PATH.");
            }

            return Files.readAllBytes(pdfPath);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not run LibreOffice for PDF conversion. Make sure it is installed and on PATH.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF conversion was interrupted.", e);
        } finally {
            cleanupQuietly(tempDir, docxPath, pdfPath);
        }
    }

    private void cleanupQuietly(Path tempDir, Path... files) {
        for (Path f : files) {
            try {
                Files.deleteIfExists(f);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }
        try {
            Files.deleteIfExists(tempDir);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
