package com.vynk.profile.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.profile.dto.StudentProfileResponse;
import com.vynk.profile.dto.UpdateStudentProfileRequest;
import com.vynk.profile.service.StudentProfileService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/me")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final UserRepository userRepository;

    public StudentProfileController(
            StudentProfileService studentProfileService,
            UserRepository userRepository) {
        this.studentProfileService = studentProfileService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<StudentProfileResponse> getMyProfile(Authentication authentication) {
        User user = getCurrentAuthenticatedUser(authentication);
        StudentProfileResponse response = studentProfileService.getProfileResponseForUser(user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<StudentProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateStudentProfileRequest request,
            Authentication authentication) {
        User user = getCurrentAuthenticatedUser(authentication);
        StudentProfileResponse response = studentProfileService.saveOrUpdateProfile(user, request);
        return ResponseEntity.ok(response);
    }

    private User getCurrentAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Unauthenticated user session.");
        }
        return userRepository.findByEmail(authentication.getName())
                .or(() -> userRepository.findByEmailIgnoreCase(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }
}
