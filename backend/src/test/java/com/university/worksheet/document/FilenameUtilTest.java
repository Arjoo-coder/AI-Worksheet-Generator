package com.university.worksheet.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilenameUtilTest {

    @Test
    void buildsMeaningfulSanitizedName() {
        String name = FilenameUtil.buildBaseName("Backend Technologies", "02");
        assertEquals("Backend_Technologies_Experiment_02", name);
    }

    @Test
    void fallsBackWhenSubjectBlank() {
        String name = FilenameUtil.buildBaseName("   ", null);
        assertEquals("Worksheet", name);
    }

    @Test
    void stripsInvalidCharacters() {
        String sanitized = FilenameUtil.sanitize("Node.js / Express!! Signup");
        assertEquals("Nodejs_Express_Signup", sanitized);
    }
}
