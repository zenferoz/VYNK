package com.vynk.post.repository;

import com.vynk.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p " +
           "WHERE p.college.id = :collegeId " +
           "  AND (COALESCE(:excludedUserIds, NULL) IS NULL OR p.author.id NOT IN :excludedUserIds) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findFeedPosts(
            @Param("collegeId") Long collegeId,
            @Param("excludedUserIds") Collection<Long> excludedUserIds,
            Pageable pageable
    );

    Page<Post> findByCollegeIdOrderByCreatedAtDesc(Long collegeId, Pageable pageable);
}
