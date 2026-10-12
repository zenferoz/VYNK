package com.vynk.matching.service;

import com.vynk.goal.entity.NetworkingGoal;
import com.vynk.goal.entity.NetworkingPurpose;
import com.vynk.goal.repository.NetworkingGoalRepository;
import com.vynk.matching.dto.StudentMatchResponse;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurposeMatchingService {

    private static final int WEIGHT_PURPOSE = 40;
    private static final int WEIGHT_SKILLS = 30;
    private static final int WEIGHT_ACADEMIC = 20;
    private static final int WEIGHT_AVAILABILITY = 10;
    private static final int MAX_RECOMMENDATIONS = 10;

    private final StudentProfileRepository profileRepository;
    private final NetworkingGoalRepository goalRepository;
    private final SafetyService safetyService;
    private final SemanticInferenceClient semanticInferenceClient;

    public PurposeMatchingService(
            StudentProfileRepository profileRepository,
            NetworkingGoalRepository goalRepository,
            SafetyService safetyService
    ) {
        this(profileRepository, goalRepository, safetyService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PurposeMatchingService(
            StudentProfileRepository profileRepository,
            NetworkingGoalRepository goalRepository,
            SafetyService safetyService,
            SemanticInferenceClient semanticInferenceClient
    ) {
        this.profileRepository = profileRepository;
        this.goalRepository = goalRepository;
        this.safetyService = safetyService;
        this.semanticInferenceClient = semanticInferenceClient;
    }

    @Transactional(readOnly = true)
    public List<StudentMatchResponse> getTopMatches(User currentUser) {
        if (currentUser == null || currentUser.getCollege() == null) {
            return Collections.emptyList();
        }

        // Current user must have completed onboarding and profile
        StudentProfile userProfile = profileRepository.findByUserId(currentUser.getId()).orElse(null);
        if (userProfile == null || !userProfile.isOnboardingCompleted()) {
            return Collections.emptyList();
        }

        // Two-way blocked users
        Set<Long> blockedUserIds = safetyService.getAllBlockedUserIdsFor(currentUser.getId());

        // Eligible candidates: same college, not self, active, email verified, discoverable, onboarding completed
        List<StudentProfile> candidateProfiles = profileRepository.findDiscoverableStudentsInCollege(
                currentUser.getCollege().getId(), currentUser.getId());

        List<StudentProfile> filteredCandidates = candidateProfiles.stream()
                .filter(p -> !blockedUserIds.contains(p.getUser().getId()))
                .collect(Collectors.toList());

        if (filteredCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Load active goals for current user
        List<NetworkingGoal> userGoals = goalRepository.findActiveGoalsByUserId(currentUser.getId());
        Set<NetworkingPurpose> userPurposes = userGoals.stream()
                .filter(g -> g != null && g.getPurpose() != null)
                .map(g -> g.getPurpose())
                .collect(Collectors.toSet());

        // Load active goals for all candidates in bulk (prevents N+1)
        List<Long> candidateUserIds = filteredCandidates.stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toList());

        List<NetworkingGoal> allCandidateGoals = goalRepository.findActiveGoalsByUserIds(candidateUserIds);
        Map<Long, List<NetworkingGoal>> candidateGoalsMap = allCandidateGoals.stream()
                .collect(Collectors.groupingBy(g -> g.getUser().getId()));

        // Current user tokens for matching
        Set<String> userSkillInterestTokens = extractAllSkillsAndInterests(userProfile, userGoals);

        // Build candidate semantic texts for hybrid inference
        List<String> candidateSemanticTexts = new ArrayList<>();
        for (StudentProfile candidate : filteredCandidates) {
            List<NetworkingGoal> candGoals = candidateGoalsMap.getOrDefault(candidate.getUser().getId(), Collections.emptyList());
            candidateSemanticTexts.add(buildSemanticProfileText(candidate, candGoals));
        }

        String userSemanticText = buildSemanticProfileText(userProfile, userGoals);
        Optional<List<Double>> semanticScoresOpt = Optional.empty();
        if (semanticInferenceClient != null) {
            semanticScoresOpt = semanticInferenceClient.computeSimilarities(userSemanticText, candidateSemanticTexts);
        }

        List<StudentMatchResponse> matches = new ArrayList<>();

        for (int i = 0; i < filteredCandidates.size(); i++) {
            StudentProfile candidate = filteredCandidates.get(i);
            Long candidateUserId = candidate.getUser().getId();
            List<NetworkingGoal> candGoals = candidateGoalsMap.getOrDefault(candidateUserId, Collections.emptyList());

            int score = 0;
            List<String> matchReasons = new ArrayList<>();

            // 1. Purpose match (Max 40 points)
            Set<NetworkingPurpose> candPurposes = candGoals.stream()
                    .filter(g -> g != null && g.getPurpose() != null)
                    .map(g -> g.getPurpose())
                    .collect(Collectors.toSet());

            Set<NetworkingPurpose> sharedPurposes = new HashSet<>(userPurposes);
            sharedPurposes.retainAll(candPurposes);

            if (!sharedPurposes.isEmpty()) {
                score += WEIGHT_PURPOSE;
                String purposesStr = sharedPurposes.stream()
                        .map(this::formatPurposeName)
                        .collect(Collectors.joining(", "));
                matchReasons.add("Shared purpose: " + purposesStr);
            }

            // 2. Overlapping skills and interests (Max 30 points)
            Set<String> candSkillInterestTokens = extractAllSkillsAndInterests(candidate, candGoals);
            Set<String> sharedSkills = new HashSet<>(userSkillInterestTokens);
            sharedSkills.retainAll(candSkillInterestTokens);

            if (!sharedSkills.isEmpty()) {
                int skillPoints = Math.min(WEIGHT_SKILLS, sharedSkills.size() * 10);
                score += skillPoints;
                // Format up to 3 shared skills
                String skillsStr = sharedSkills.stream().limit(3).collect(Collectors.joining(", "));
                matchReasons.add("Shared skills/interests: " + skillsStr);
            }

            // 3. Academic department/year affinity (Max 20 points)
            int academicScore = 0;
            if (userProfile.getDepartment() != null && candidate.getDepartment() != null &&
                    userProfile.getDepartment().equalsIgnoreCase(candidate.getDepartment())) {
                academicScore += 12;
                matchReasons.add("Same department: " + candidate.getDepartment());
            }

            if (userProfile.getCurrentYear() != null && candidate.getCurrentYear() != null) {
                int diff = Math.abs(userProfile.getCurrentYear() - candidate.getCurrentYear());
                if (diff == 0) {
                    academicScore += 8;
                    matchReasons.add("Same academic year (" + candidate.getCurrentYear() + ")");
                } else if (diff == 1) {
                    academicScore += 4;
                }
            }
            score += Math.min(WEIGHT_ACADEMIC, academicScore);

            // 4. Availability compatibility (Max 10 points)
            if (userProfile.getAvailability() != null && candidate.getAvailability() != null) {
                if (checkAvailabilityOverlap(userProfile.getAvailability(), candidate.getAvailability())) {
                    score += WEIGHT_AVAILABILITY;
                    matchReasons.add("Compatible schedule: " + candidate.getAvailability().trim());
                }
            }

            // 5. Semantic similarity integration (Bounded hybrid contribution)
            boolean isSemanticAssisted = false;
            if (semanticScoresOpt.isPresent() && semanticScoresOpt.get().size() > i) {
                double sim = semanticScoresOpt.get().get(i);
                if (sim >= 0.15) {
                    int semanticBonus = (int) Math.round(sim * 20.0);
                    score = Math.min(100, score + semanticBonus);
                    matchReasons.add("Semantic synergy: Shared networking intent (" + Math.round(sim * 100) + "% match)");
                    isSemanticAssisted = true;
                }
            }

            // Normalize strictly between 0 and 100
            score = Math.max(0, Math.min(100, score));

            if (score > 0) {
                List<String> activePurposeStrings = candPurposes.stream()
                        .map(this::formatPurposeName)
                        .collect(Collectors.toList());

                StudentMatchResponse resp = new StudentMatchResponse(
                        candidate.getUser().getId(),
                        candidate.getFullName(),
                        candidate.getDepartment(),
                        candidate.getCourse(),
                        candidate.getCurrentYear(),
                        candidate.getBio(),
                        new ArrayList<>(candidate.getSkills()),
                        new ArrayList<>(candidate.getInterests()),
                        candidate.getAvailability(),
                        activePurposeStrings,
                        score,
                        matchReasons
                );
                resp.setSemanticAssisted(isSemanticAssisted);
                matches.add(resp);
            }
        }

        // Sort deterministically: score DESC, then candidate userId ASC
        matches.sort(Comparator.<StudentMatchResponse>comparingInt(r -> r != null ? r.getMatchScore() : 0).reversed()
                .thenComparingLong(r -> r != null && r.getUserId() != null ? r.getUserId() : 0L));

        // Return top 10
        if (matches.size() > MAX_RECOMMENDATIONS) {
            return matches.subList(0, MAX_RECOMMENDATIONS);
        }
        return matches;
    }

    private Set<String> extractAllSkillsAndInterests(StudentProfile profile, List<NetworkingGoal> goals) {
        Set<String> set = new HashSet<>();
        if (profile.getSkills() != null) {
            for (String s : profile.getSkills()) {
                if (s != null && !s.trim().isEmpty()) {
                    set.add(s.trim().toLowerCase());
                }
            }
        }
        if (profile.getInterests() != null) {
            for (String i : profile.getInterests()) {
                if (i != null && !i.trim().isEmpty()) {
                    set.add(i.trim().toLowerCase());
                }
            }
        }
        if (goals != null) {
            for (NetworkingGoal g : goals) {
                if (g.getSkills() != null) {
                    for (String gs : g.getSkills()) {
                        if (gs != null && !gs.trim().isEmpty()) {
                            set.add(gs.trim().toLowerCase());
                        }
                    }
                }
            }
        }
        return set;
    }

    private boolean checkAvailabilityOverlap(String a1, String a2) {
        if (a1 == null || a2 == null) return false;
        Set<String> words1 = extractMeaningfulTokens(a1);
        Set<String> words2 = extractMeaningfulTokens(a2);
        words1.retainAll(words2);
        return !words1.isEmpty();
    }

    private Set<String> extractMeaningfulTokens(String text) {
        Set<String> tokens = new HashSet<>();
        String[] parts = text.toLowerCase().split("[^a-z0-9]+");
        for (String p : parts) {
            if (p.length() >= 4) {
                tokens.add(p);
            }
        }
        return tokens;
    }

    private String formatPurposeName(NetworkingPurpose purpose) {
        switch (purpose) {
            case PROJECT_COLLABORATION: return "Project Collaboration";
            case HACKATHON_TEAM: return "Hackathon Team";
            case STUDY_PARTNER: return "Study Partner";
            case RESEARCH: return "Research";
            case PLACEMENT_PREP: return "Placement Prep";
            case CASUAL_NETWORKING: return "Casual Networking";
            default: return purpose.name();
        }
    }

    private String buildSemanticProfileText(StudentProfile profile, List<NetworkingGoal> goals) {
        StringBuilder sb = new StringBuilder();
        if (profile.getDepartment() != null) sb.append(profile.getDepartment()).append(" ");
        if (profile.getCourse() != null) sb.append(profile.getCourse()).append(" ");
        if (profile.getBio() != null) sb.append(profile.getBio()).append(" ");
        if (profile.getSkills() != null) sb.append(String.join(" ", profile.getSkills())).append(" ");
        if (profile.getInterests() != null) sb.append(String.join(" ", profile.getInterests())).append(" ");
        if (goals != null) {
            for (NetworkingGoal g : goals) {
                if (g.getPurpose() != null) sb.append(formatPurposeName(g.getPurpose())).append(" ");
                if (g.getDescription() != null) sb.append(g.getDescription()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
