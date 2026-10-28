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
@RequestMapping("/api/workspaces/{wid}/posts")
public class PostController {

    public record PostRequest(String content, String platform, Instant scheduledAt) {}
    public record ScheduleRequest(Instant scheduledAt) {}

    private static final int MAX_LENGTH = 280;
    private static final Set<String> PLATFORMS = Set.of("TWITTER", "LINKEDIN", "INSTAGRAM", "FACEBOOK");

    private final PostRepository repo;
    private final UserRepository users;
    private final WorkspaceAccess access;

    public PostController(PostRepository repo, UserRepository users, WorkspaceAccess access) {
        this.repo = repo;
        this.users = users;
        this.access = access;
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
    }

    private void requireFuture(Instant when) {
        if (!when.isAfter(Instant.now())) throw bad("Scheduled time must be in the future");
    }

    @GetMapping
    public List<Post> list(@PathVariable Long wid, Authentication auth) {
        access.require(wid, auth, Role.VIEWER);
        return repo.findByWorkspaceIdOrderByCreatedAtDesc(wid);
    }

    @PostMapping
    public ResponseEntity<Post> create(@PathVariable Long wid, @RequestBody PostRequest req, Authentication auth) {
        access.require(wid, auth, Role.EDITOR);
        String content = req.content() == null ? "" : req.content().trim();
        if (content.isEmpty()) throw bad("Post content cannot be empty");
        if (content.length() > MAX_LENGTH) throw bad("Post cannot exceed " + MAX_LENGTH + " characters");
        String platform = req.platform() == null ? "" : req.platform().toUpperCase();
        if (!PLATFORMS.contains(platform)) throw bad("Unsupported platform: " + req.platform());

        Long userId = (Long) auth.getPrincipal();
        String email = users.findById(userId).map(AppUser::getEmail).orElse("unknown");
        Post post = new Post(wid, userId, email, content, platform);
        if (req.scheduledAt() != null) {
            requireFuture(req.scheduledAt());
            post.setScheduledAt(req.scheduledAt());
            post.setStatus(PostStatus.SCHEDULED);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(post));
    }

    @PutMapping("/{id}/schedule")
    public Post schedule(@PathVariable Long wid, @PathVariable Long id,
                         @RequestBody ScheduleRequest req, Authentication auth) {
        access.require(wid, auth, Role.EDITOR);
        Post post = repo.findByIdAndWorkspaceId(id, wid).orElseThrow(this::notFound);
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
    public ResponseEntity<Void> delete(@PathVariable Long wid, @PathVariable Long id, Authentication auth) {
        Membership caller = access.require(wid, auth, Role.EDITOR);
        Post post = repo.findByIdAndWorkspaceId(id, wid).orElseThrow(this::notFound);
        if (caller.getRole() != Role.ADMIN && !post.getAuthorId().equals(caller.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Editors can only delete their own posts");
        }
        repo.delete(post);
        return ResponseEntity.noContent().build();
    }
}