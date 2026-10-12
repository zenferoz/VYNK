package com.vynk.post.service;

import com.vynk.college.entity.College;
import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.post.dto.CreatePostRequest;
import com.vynk.post.dto.PostResponse;
import com.vynk.post.entity.Post;
import com.vynk.post.repository.PostRepository;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final StudentProfileRepository profileRepository;
    private final SafetyService safetyService;

    public PostService(
            PostRepository postRepository,
            StudentProfileRepository profileRepository,
            SafetyService safetyService) {
        this.postRepository = postRepository;
        this.profileRepository = profileRepository;
        this.safetyService = safetyService;
    }

    @Transactional
    public PostResponse createPost(User currentUser, CreatePostRequest request) {
        College college = currentUser.getCollege();
        if (college == null) {
            throw new IllegalArgumentException("User must be affiliated with a college to post.");
        }

        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Post content cannot be blank.");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Post content cannot exceed 2000 characters.");
        }

        Post post = new Post(currentUser, college, content);
        Post saved = postRepository.save(post);

        StudentProfile profile = profileRepository.findByUserId(currentUser.getId()).orElse(null);
        return mapToResponse(saved, profile, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(User currentUser, int page, int size) {
        College college = currentUser.getCollege();
        if (college == null) {
            return Page.empty();
        }

        Set<Long> blockedIds = safetyService.getAllBlockedUserIdsFor(currentUser.getId());
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(50, Math.max(1, size));

        Page<Post> posts;
        if (blockedIds.isEmpty()) {
            posts = postRepository.findByCollegeIdOrderByCreatedAtDesc(
                    college.getId(),
                    PageRequest.of(boundedPage, boundedSize)
            );
        } else {
            posts = postRepository.findFeedPosts(
                    college.getId(),
                    blockedIds,
                    PageRequest.of(boundedPage, boundedSize)
            );
        }

        return posts.map(p -> {
            StudentProfile profile = profileRepository.findByUserId(p.getAuthor().getId()).orElse(null);
            return mapToResponse(p, profile, currentUser.getId());
        });
    }

    @Transactional
    public void deletePost(User currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this post.");
        }

        postRepository.delete(post);
    }

    @Transactional
    public PostResponse updatePost(User currentUser, Long postId, CreatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to edit this post.");
        }

        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Post content cannot be blank.");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Post content cannot exceed 2000 characters.");
        }

        post.setContent(content);
        Post saved = postRepository.save(post);

        StudentProfile profile = profileRepository.findByUserId(currentUser.getId()).orElse(null);
        return mapToResponse(saved, profile, currentUser.getId());
    }

    private PostResponse mapToResponse(Post post, StudentProfile profile, Long currentUserId) {
        String name = profile != null ? profile.getFullName() : "Student #" + post.getAuthor().getId();
        String dept = profile != null ? profile.getDepartment() : null;
        String course = profile != null ? profile.getCourse() : null;
        Integer year = profile != null ? profile.getCurrentYear() : null;
        return new PostResponse(post, name, dept, course, year, currentUserId);
    }
}
