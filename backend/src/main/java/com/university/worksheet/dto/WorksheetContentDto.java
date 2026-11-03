package com.university.worksheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Strict schema for the AI-structured worksheet content. This is what the
 * AI service must return as JSON, and what the document generator consumes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorksheetContentDto {

    private String experimentTitle = "";
    private String aim = "";
    private List<String> objectives = new ArrayList<>();
    private List<String> input = new ArrayList<>();
    private List<String> apparatusUsed = new ArrayList<>();
    private List<CodeFileDto> codeImplementation = new ArrayList<>();
    private String output = "";
    private List<String> learningOutcomes = new ArrayList<>();

    public String getExperimentTitle() { return experimentTitle; }
    public void setExperimentTitle(String experimentTitle) { this.experimentTitle = experimentTitle; }
    public String getAim() { return aim; }
    public void setAim(String aim) { this.aim = aim; }
    public List<String> getObjectives() { return objectives; }
    public void setObjectives(List<String> objectives) { this.objectives = objectives; }
    public List<String> getInput() { return input; }
    public void setInput(List<String> input) { this.input = input; }
    public List<String> getApparatusUsed() { return apparatusUsed; }
    public void setApparatusUsed(List<String> apparatusUsed) { this.apparatusUsed = apparatusUsed; }
    public List<CodeFileDto> getCodeImplementation() { return codeImplementation; }
    public void setCodeImplementation(List<CodeFileDto> codeImplementation) { this.codeImplementation = codeImplementation; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public List<String> getLearningOutcomes() { return learningOutcomes; }
    public void setLearningOutcomes(List<String> learningOutcomes) { this.learningOutcomes = learningOutcomes; }

    /** One named code file/snippet preserved verbatim from the source content. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodeFileDto {
        private String fileName = "";
        private String code = "";

        public CodeFileDto() {}
        public CodeFileDto(String fileName, String code) {
            this.fileName = fileName;
            this.code = code;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
