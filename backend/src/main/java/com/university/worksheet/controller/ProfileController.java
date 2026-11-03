package com.university.worksheet.controller;

import com.university.worksheet.dto.ProfileDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Profile is persisted client-side (localStorage) in V1 - see
 * frontend/js/app.js. These endpoints exist so the contract is in place for
 * a future server-side profile store, and simply validate + echo back.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @PostMapping
    public ProfileDto create(@Valid @RequestBody ProfileDto profile) {
        return profile;
    }

    @GetMapping
    public ProfileDto get() {
        // No server-side storage in V1; frontend reads from localStorage directly.
        return new ProfileDto("", "", "");
    }

    @PutMapping
    public ProfileDto update(@Valid @RequestBody ProfileDto profile) {
        return profile;
    }
}
