package com.university.worksheet.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Student profile. Persisted client-side (localStorage) in V1. This DTO
 * gives /api/profile a well-defined contract even though nothing is stored
 * server-side yet.
 */
public class ProfileDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "UID is required")
    private String uid;

    @NotBlank(message = "Class is required")
    private String className;

    public ProfileDto() {}

    public ProfileDto(String name, String uid, String className) {
        this.name = name;
        this.uid = uid;
        this.className = className;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}
