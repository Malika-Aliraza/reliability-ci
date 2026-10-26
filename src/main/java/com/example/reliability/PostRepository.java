package com.example.reliability;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Post> findByUserId(Long userId);
    Optional<Post> findByIdAndUserId(Long id, Long userId);
    List<Post> findByStatusAndScheduledAtLessThanEqual(PostStatus status, Instant time);
}