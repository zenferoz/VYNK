package com.vynk;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.profile.dto.StudentProfileResponse;
import com.vynk.profile.dto.UpdateStudentProfileRequest;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.profile.service.StudentProfileService;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudentProfileServiceTest {

    private StudentProfileRepository profileRepository;
    private SafetyService safetyService;
    private StudentProfileService profileService;

    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        profileRepository = Mockito.mock(StudentProfileRepository.class);
        safetyService = Mockito.mock(SafetyService.class);
        profileService = new StudentProfileService(profileRepository, safetyService);

        testUser = new User();
        setField(testUser, "id", 101L);
        testUser.setEmail("student@anurag.edu.in");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void getProfileResponseForUser_WhenProfileExists_ReturnsResponse() throws Exception {
        StudentProfile profile = new StudentProfile();
        setField(profile, "id", 1L);
        profile.setUser(testUser);
        profile.setFullName("Aditya Sharma");
        profile.setDepartment("Computer Science");
        profile.setCourse("B.Tech CSE");
        profile.setCurrentYear(3);
        profile.setBio("Passionate about distributed systems.");
        profile.setDiscoverable(true);
        profile.setOnboardingCompleted(true);
        profile.setAvailability("10-15 hrs/week");

        when(profileRepository.findByUserId(101L)).thenReturn(Optional.of(profile));

        StudentProfileResponse response = profileService.getProfileResponseForUser(101L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(101L, response.getUserId());
        assertEquals("Aditya Sharma", response.getFullName());
        assertEquals("Computer Science", response.getDepartment());
        assertEquals("B.Tech CSE", response.getCourse());
        assertEquals(3, response.getCurrentYear());
        assertTrue(response.isOnboardingCompleted());
        assertTrue(response.isDiscoverable());
    }

    @Test
    void getProfileResponseForUser_WhenProfileDoesNotExist_ThrowsResourceNotFoundException() {
        when(profileRepository.findByUserId(101L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                profileService.getProfileResponseForUser(101L));

        assertTrue(ex.getMessage().contains("Student profile not found. Please complete onboarding."));
    }

    @Test
    void saveOrUpdateProfile_WhenCreatingNewProfile_SetsOnboardingCompletedTrue() {
        when(profileRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStudentProfileRequest request = new UpdateStudentProfileRequest();
        request.setFullName("Priya Patel");
        request.setDepartment("Information Technology");
        request.setCourse("B.Tech IT");
        request.setCurrentYear(2);
        request.setBio("Interested in cloud computing and open source.");
        request.setDiscoverable(true);
        request.setAvailability("Weekends only");
        request.setSkills(List.of("Java", "Spring Boot", "Docker"));
        request.setInterests(List.of("AI", "Competitive Programming"));

        StudentProfileResponse response = profileService.saveOrUpdateProfile(testUser, request);

        assertNotNull(response);
        assertEquals("Priya Patel", response.getFullName());
        assertEquals("Information Technology", response.getDepartment());
        assertEquals("B.Tech IT", response.getCourse());
        assertEquals(2, response.getCurrentYear());
        assertTrue(response.isOnboardingCompleted());
        assertEquals(3, response.getSkills().size());
        assertEquals(2, response.getInterests().size());
        verify(profileRepository, times(1)).save(any(StudentProfile.class));
    }

    @Test
    void saveOrUpdateProfile_WhenUpdatingExistingProfile_PreservesUserAndUpdatesFields() throws Exception {
        StudentProfile existing = new StudentProfile();
        setField(existing, "id", 50L);
        existing.setUser(testUser);
        existing.setFullName("Old Name");
        existing.setDepartment("Old Dept");
        existing.setCourse("Old Course");
        existing.setCurrentYear(1);

        when(profileRepository.findByUserId(101L)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStudentProfileRequest request = new UpdateStudentProfileRequest();
        request.setFullName("New Name");
        request.setDepartment("Electronics");
        request.setCourse("B.Tech ECE");
        request.setCurrentYear(4);
        request.setBio("Updated bio");
        request.setDiscoverable(false);
        request.setAvailability("5 hrs/week");

        StudentProfileResponse response = profileService.saveOrUpdateProfile(testUser, request);

        assertNotNull(response);
        assertEquals("New Name", response.getFullName());
        assertEquals("Electronics", response.getDepartment());
        assertEquals("B.Tech ECE", response.getCourse());
        assertEquals(4, response.getCurrentYear());
        assertFalse(response.isDiscoverable());
        assertTrue(response.isOnboardingCompleted());
        assertEquals(50L, response.getId());
    }

    @Test
    void saveOrUpdateProfile_NormalizesSkillsAndInterests() {
        when(profileRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStudentProfileRequest request = new UpdateStudentProfileRequest();
        request.setFullName("Test Student");
        request.setDepartment("CSE");
        request.setCourse("B.Tech");
        request.setCurrentYear(1);
        // Includes duplicate, whitespace, and long string
        request.setSkills(List.of(" Java ", "Java", "   ", "A".repeat(60)));

        StudentProfileResponse response = profileService.saveOrUpdateProfile(testUser, request);

        assertNotNull(response);
        // " Java " and "Java" deduplicate to "Java", "   " is omitted, long string is capped to 50
        assertEquals(2, response.getSkills().size());
        assertTrue(response.getSkills().contains("Java"));
        assertTrue(response.getSkills().contains("A".repeat(50)));
    }
}
