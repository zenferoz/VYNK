package com.vynk.profile.service;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.profile.dto.StudentDiscoveryResponse;
import com.vynk.profile.dto.StudentProfileResponse;
import com.vynk.profile.dto.UpdateStudentProfileRequest;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final com.vynk.safety.service.SafetyService safetyService;

    public StudentProfileService(
            StudentProfileRepository studentProfileRepository,
            com.vynk.safety.service.SafetyService safetyService) {
        this.studentProfileRepository = studentProfileRepository;
        this.safetyService = safetyService;
    }

    public StudentProfile createProfile(StudentProfile profile) {
        return studentProfileRepository.save(profile);
    }

    public List<StudentProfile> getAllProfiles() {
        return studentProfileRepository.findAll();
    }

    public Optional<StudentProfile> getProfileById(Long id) {
        return studentProfileRepository.findById(id);
    }

    public Optional<StudentProfile> getProfileByUserId(Long userId) {
        return studentProfileRepository.findByUserId(userId);
    }

    public boolean profileExistsForUser(Long userId) {
        return studentProfileRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileResponseForUser(Long userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found. Please complete onboarding."));
        return toResponse(profile);
    }

    @Transactional
    public StudentProfileResponse saveOrUpdateProfile(User user, UpdateStudentProfileRequest request) {
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    StudentProfile newProfile = new StudentProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        profile.setFullName(request.getFullName().trim());
        profile.setDepartment(request.getDepartment().trim());
        profile.setCourse(request.getCourse().trim());
        profile.setCurrentYear(request.getCurrentYear());
        profile.setBio(request.getBio() != null ? request.getBio().trim() : null);
        profile.setDiscoverable(request.getDiscoverable() != null ? request.getDiscoverable() : true);
        profile.setAvailability(request.getAvailability() != null && !request.getAvailability().isBlank()
                ? request.getAvailability().trim() : null);

        // Normalize skills
        Set<String> cleanSkills = new LinkedHashSet<>();
        if (request.getSkills() != null) {
            for (String s : request.getSkills()) {
                if (s != null && !s.isBlank() && cleanSkills.size() < 25) {
                    String norm = s.trim();
                    if (norm.length() > 50) norm = norm.substring(0, 50);
                    cleanSkills.add(norm);
                }
            }
        }
        profile.setSkills(cleanSkills);

        // Normalize interests
        Set<String> cleanInterests = new LinkedHashSet<>();
        if (request.getInterests() != null) {
            for (String in : request.getInterests()) {
                if (in != null && !in.isBlank() && cleanInterests.size() < 25) {
                    String norm = in.trim();
                    if (norm.length() > 50) norm = norm.substring(0, 50);
                    cleanInterests.add(norm);
                }
            }
        }
        profile.setInterests(cleanInterests);

        boolean onboardingValid = !profile.getFullName().isBlank()
                && !profile.getDepartment().isBlank()
                && !profile.getCourse().isBlank()
                && profile.getCurrentYear() != null
                && profile.getCurrentYear() >= 1
                && profile.getCurrentYear() <= 6;

        profile.setOnboardingCompleted(onboardingValid);

        StudentProfile saved = studentProfileRepository.save(profile);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentDiscoveryResponse> getDiscoverableStudentsInCollege(Long collegeId, Long currentUserId) {
        Set<Long> blockedIds = safetyService.getAllBlockedUserIdsFor(currentUserId);
        return studentProfileRepository.findDiscoverableStudentsInCollege(collegeId, currentUserId).stream()
                .filter(p -> !blockedIds.contains(p.getUser().getId()))
                .map(this::toDiscoveryResponse)
                .toList();
    }

    public StudentProfileResponse toResponse(StudentProfile profile) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getFullName(),
                profile.getDepartment(),
                profile.getCourse(),
                profile.getCurrentYear(),
                profile.getBio(),
                profile.isDiscoverable(),
                profile.isOnboardingCompleted(),
                profile.getAvailability(),
                new ArrayList<>(profile.getSkills()),
                new ArrayList<>(profile.getInterests())
        );
    }

    public StudentDiscoveryResponse toDiscoveryResponse(StudentProfile profile) {
        return new StudentDiscoveryResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getFullName(),
                profile.getDepartment(),
                profile.getCourse(),
                profile.getCurrentYear(),
                profile.getBio(),
                profile.getUser().getCollege().getName(),
                profile.getAvailability(),
                new ArrayList<>(profile.getSkills()),
                new ArrayList<>(profile.getInterests())
        );
    }
}