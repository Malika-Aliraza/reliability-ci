package com.example.reliability;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    public record PostRequest(String content, String platform, Instant scheduledAt) {}
    public record ScheduleRequest(Instant scheduledAt) {}

    private static final int MAX_LENGTH = 280;
    private static final Set<String> PLATFORMS = Set.of("TWITTER", "LINKEDIN", "INSTAGRAM", "FACEBOOK");

    private final PostRepository repo;

    public PostController(PostRepository repo) {
        this.repo = repo;
    }

    private Long uid(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private void requireFuture(Instant when) {
        if (!when.isAfter(Instant.now())) throw bad("Scheduled time must be in the future");
    }

    @GetMapping
    public List<Post> list(Authentication auth) {
        return repo.findByUserIdOrderByCreatedAtDesc(uid(auth));
    }

    @PostMapping
    public ResponseEntity<Post> create(@RequestBody PostRequest req, Authentication auth) {
        String content = req.content() == null ? "" : req.content().trim();
        if (content.isEmpty()) throw bad("Post content cannot be empty");
        if (content.length() > MAX_LENGTH) throw bad("Post cannot exceed " + MAX_LENGTH + " characters");
        String platform = req.platform() == null ? "" : req.platform().toUpperCase();
        if (!PLATFORMS.contains(platform)) throw bad("Unsupported platform: " + req.platform());

        Post post = new Post(uid(auth), content, platform);
        if (req.scheduledAt() != null) {
            requireFuture(req.scheduledAt());
            post.setScheduledAt(req.scheduledAt());
            post.setStatus(PostStatus.SCHEDULED);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(post));
    }

    @PutMapping("/{id}/schedule")
    public Post schedule(@PathVariable Long id, @RequestBody ScheduleRequest req, Authentication auth) {
        Post post = repo.findByIdAndUserId(id, uid(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only drafts or scheduled posts can be scheduled");
        }
        if (req.scheduledAt() == null) throw bad("scheduledAt is required");
        requireFuture(req.scheduledAt());
        post.setScheduledAt(req.scheduledAt());
        post.setStatus(PostStatus.SCHEDULED);
        return repo.save(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Post post = repo.findByIdAndUserId(id, uid(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        repo.delete(post);
        return ResponseEntity.noContent().build();
    }
}