package com.vynk.goal.service;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.goal.dto.NetworkingGoalRequest;
import com.vynk.goal.dto.NetworkingGoalResponse;
import com.vynk.goal.entity.NetworkingGoal;
import com.vynk.goal.repository.NetworkingGoalRepository;
import com.vynk.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NetworkingGoalService {

    private final NetworkingGoalRepository goalRepository;

    public NetworkingGoalService(NetworkingGoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Transactional(readOnly = true)
    public List<NetworkingGoalResponse> getGoalsForUser(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NetworkingGoalResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NetworkingGoal> getActiveGoalsEntity(Long userId) {
        return goalRepository.findActiveGoalsByUserId(userId);
    }

    @Transactional
    public NetworkingGoalResponse createGoal(User user, NetworkingGoalRequest request) {
        NetworkingGoal goal = new NetworkingGoal();
        goal.setUser(user);
        goal.setPurpose(request.getPurpose());
        goal.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        goal.setTargetYear(request.getTargetYear());
        goal.setActive(request.getActive() != null ? request.getActive() : true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setExpiresAt(request.getExpiresAt());

        // Normalize skills
        Set<String> cleanSkills = new LinkedHashSet<>();
        if (request.getSkills() != null) {
            for (String s : request.getSkills()) {
                if (s != null && !s.isBlank() && cleanSkills.size() < 15) {
                    String norm = s.trim();
                    if (norm.length() > 50) norm = norm.substring(0, 50);
                    cleanSkills.add(norm);
                }
            }
        }
        goal.setSkills(cleanSkills);

        NetworkingGoal saved = goalRepository.save(goal);
        return new NetworkingGoalResponse(saved);
    }

    @Transactional
    public NetworkingGoalResponse updateGoal(User user, Long goalId, NetworkingGoalRequest request) {
        NetworkingGoal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Networking goal not found or not owned by you."));

        if (request.getPurpose() != null) {
            goal.setPurpose(request.getPurpose());
        }
        if (request.getDescription() != null) {
            goal.setDescription(request.getDescription().trim());
        }
        if (request.getTargetYear() != null) {
            goal.setTargetYear(request.getTargetYear());
        }
        if (request.getActive() != null) {
            goal.setActive(request.getActive());
        }
        if (request.getExpiresAt() != null) {
            goal.setExpiresAt(request.getExpiresAt());
        }

        if (request.getSkills() != null) {
            Set<String> cleanSkills = new LinkedHashSet<>();
            for (String s : request.getSkills()) {
                if (s != null && !s.isBlank() && cleanSkills.size() < 15) {
                    String norm = s.trim();
                    if (norm.length() > 50) norm = norm.substring(0, 50);
                    cleanSkills.add(norm);
                }
            }
            goal.setSkills(cleanSkills);
        }

        NetworkingGoal updated = goalRepository.save(goal);
        return new NetworkingGoalResponse(updated);
    }

    @Transactional
    public void deactivateGoal(User user, Long goalId) {
        NetworkingGoal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Networking goal not found or not owned by you."));
        goal.setActive(false);
        goalRepository.save(goal);
    }
}
