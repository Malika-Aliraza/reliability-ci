package com.example.reliability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    List<Post> findByWorkspaceId(Long workspaceId);
    Optional<Post> findByIdAndWorkspaceId(Long id, Long workspaceId);
    List<Post> findByStatusAndScheduledAtLessThanEqual(PostStatus status, Instant time);

    /** Atomically moves a post from one status to another. Returns 1 only for the instance that wins. */
    @Transactional
    @Modifying
    @Query("update Post p set p.status = :to where p.id = :id and p.status = :from")
    int claim(@Param("id") Long id, @Param("from") PostStatus from, @Param("to") PostStatus to);
}