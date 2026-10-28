package com.example.reliability;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long workspaceId;
    private Long authorId;
    private String authorEmail;
    @Column(length = 500)
    private String content;
    private String platform;
    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.DRAFT;
    private Instant scheduledAt;
    private Instant publishedAt;
    private Instant createdAt = Instant.now();
    private int impressions;
    private int likes;
    private int shares;

    public Post() {}
    public Post(Long workspaceId, Long authorId, String authorEmail, String content, String platform) {
        this.workspaceId = workspaceId;
        this.authorId = authorId;
        this.authorEmail = authorEmail;
        this.content = content;
        this.platform = platform;
    }

    public Long getId() { return id; }
    public Long getWorkspaceId() { return workspaceId; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorEmail() { return authorEmail; }
    public String getContent() { return content; }
    public String getPlatform() { return platform; }
    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public int getImpressions() { return impressions; }
    public void setImpressions(int impressions) { this.impressions = impressions; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }
}