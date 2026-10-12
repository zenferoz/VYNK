package com.vynk;

import com.vynk.college.entity.College;
import com.vynk.post.dto.CreatePostRequest;
import com.vynk.post.entity.Post;
import com.vynk.post.repository.PostRepository;
import com.vynk.post.service.PostService;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostServiceTest {

    private PostRepository postRepository;
    private StudentProfileRepository profileRepository;
    private SafetyService safetyService;
    private PostService postService;

    private College college;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() throws Exception {
        postRepository = mock(PostRepository.class);
        profileRepository = mock(StudentProfileRepository.class);
        safetyService = mock(SafetyService.class);

        postService = new PostService(postRepository, profileRepository, safetyService);

        college = new College();
        setField(college, "id", 1L);

        user1 = new User();
        setField(user1, "id", 10L);
        user1.setCollege(college);

        user2 = new User();
        setField(user2, "id", 20L);
        user2.setCollege(college);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void createsPostSuccessfully() throws Exception {
        when(postRepository.save(any(Post.class))).thenAnswer(i -> {
            Post p = i.getArgument(0);
            setField(p, "id", 101L);
            return p;
        });

        CreatePostRequest req = new CreatePostRequest("Looking for hackathon teammates!");
        var res = postService.createPost(user1, req);

        assertNotNull(res);
        assertEquals(101L, res.getId());
        assertEquals("Looking for hackathon teammates!", res.getContent());
        assertTrue(res.isMine());
    }

    @Test
    void allowsAuthorToDeleteOwnPost() throws Exception {
        Post p = new Post(user1, college, "Hello");
        setField(p, "id", 55L);
        when(postRepository.findById(55L)).thenReturn(Optional.of(p));

        assertDoesNotThrow(() -> postService.deletePost(user1, 55L));
        verify(postRepository, times(1)).delete(p);
    }

    @Test
    void rejectsNonAuthorPostDeletion() throws Exception {
        Post p = new Post(user1, college, "Hello");
        setField(p, "id", 55L);
        when(postRepository.findById(55L)).thenReturn(Optional.of(p));

        // user2 is not the author
        assertThrows(AccessDeniedException.class, () ->
                postService.deletePost(user2, 55L)
        );
    }

    @Test
    void allowsAuthorToUpdateOwnPost() throws Exception {
        Post p = new Post(user1, college, "Initial content");
        setField(p, "id", 66L);
        when(postRepository.findById(66L)).thenReturn(Optional.of(p));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));

        CreatePostRequest updateReq = new CreatePostRequest("Updated post content");
        var res = postService.updatePost(user1, 66L, updateReq);

        assertNotNull(res);
        assertEquals("Updated post content", res.getContent());
        assertEquals(66L, res.getId());
        verify(postRepository, times(1)).save(p);
    }

    @Test
    void rejectsNonAuthorPostUpdate() throws Exception {
        Post p = new Post(user1, college, "Initial content");
        setField(p, "id", 66L);
        when(postRepository.findById(66L)).thenReturn(Optional.of(p));

        CreatePostRequest updateReq = new CreatePostRequest("Malicious update");
        assertThrows(AccessDeniedException.class, () ->
                postService.updatePost(user2, 66L, updateReq)
        );
        verify(postRepository, never()).save(any());
    }

    @Test
    void rejectsBlankContentOnPostUpdate() throws Exception {
        Post p = new Post(user1, college, "Initial content");
        setField(p, "id", 66L);
        when(postRepository.findById(66L)).thenReturn(Optional.of(p));

        CreatePostRequest updateReq = new CreatePostRequest("   ");
        assertThrows(IllegalArgumentException.class, () ->
                postService.updatePost(user1, 66L, updateReq)
        );
    }
}
