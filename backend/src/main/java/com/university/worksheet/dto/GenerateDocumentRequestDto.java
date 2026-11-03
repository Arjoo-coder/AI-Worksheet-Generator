package com.university.worksheet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Everything needed to render the final worksheet document. The frontend
 * builds this from: saved profile + worksheet detail fields + the
 * (possibly user-edited) structured content returned by /analyze.
 */
public class GenerateDocumentRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "UID is required")
    private String uid;

    @NotBlank(message = "Class is required")
    private String className;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Subject code is required")
    private String subjectCode;

    @NotBlank(message = "Date is required")
    private String date;

    /** Experiment number, e.g. "02". Auto-generated client-side, editable. */
    private String experimentNumber;

    /**
     * Optional - NOT part of the current required frontend inputs (only
     * Subject/Subject Code/Date/Pages/Content are required). Left null/blank
     * unless a future frontend change starts collecting them; the student
     * info block renders sensibly either way (falls back to className / a
     * blank line rather than breaking layout).
     */
    private String branch;
    private String sectionGroup;
    private String semester;

    /** Target page count, carried through from the analyze step so DOCX layout matches. */
    @Min(value = 2, message = "Number of pages must be between 2 and 6")
    @Max(value = 6, message = "Number of pages must be between 2 and 6")
    private int pageCount = 4;

    @NotNull(message = "Structured worksheet content is required")
    @Valid
    private WorksheetContentDto content;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getExperimentNumber() { return experimentNumber; }
    public void setExperimentNumber(String experimentNumber) { this.experimentNumber = experimentNumber; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getSectionGroup() { return sectionGroup; }
    public void setSectionGroup(String sectionGroup) { this.sectionGroup = sectionGroup; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public WorksheetContentDto getContent() { return content; }
    public void setContent(WorksheetContentDto content) { this.content = content; }
}
