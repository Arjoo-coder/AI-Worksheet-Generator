package com.university.worksheet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration for the LibreOffice-headless DOCX -> PDF conversion step. */
@Component
@ConfigurationProperties(prefix = "app.pdf")
public class PdfProperties {

    private String sofficePath = "soffice";
    private int conversionTimeoutSeconds = 60;

    public String getSofficePath() { return sofficePath; }
    public void setSofficePath(String sofficePath) { this.sofficePath = sofficePath; }
    public int getConversionTimeoutSeconds() { return conversionTimeoutSeconds; }
    public void setConversionTimeoutSeconds(int conversionTimeoutSeconds) { this.conversionTimeoutSeconds = conversionTimeoutSeconds; }
}
