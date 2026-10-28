package com.example.reliability;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
public class AnalyticsController {

    public record Analytics(long totalPosts, Map<String, Long> byStatus, Map<String, Long> byPlatform,
                            long impressions, long likes, long shares, double engagementRate) {}

    private final PostRepository repo;
    private final WorkspaceAccess access;

    public AnalyticsController(PostRepository repo, WorkspaceAccess access) {
        this.repo = repo;
        this.access = access;
    }

    @GetMapping("/api/workspaces/{wid}/analytics")
    public Analytics analytics(@PathVariable Long wid, Authentication auth) {
        access.require(wid, auth, Role.VIEWER);
        List<Post> posts = repo.findByWorkspaceId(wid);
        long impressions = posts.stream().mapToLong(Post::getImpressions).sum();
        long likes = posts.stream().mapToLong(Post::getLikes).sum();
        long shares = posts.stream().mapToLong(Post::getShares).sum();
        double rate = impressions == 0 ? 0 : Math.round((likes + shares) * 10000.0 / impressions) / 100.0;

        Map<String, Long> byStatus = posts.stream().collect(
                Collectors.groupingBy(p -> p.getStatus().name(), TreeMap::new, Collectors.counting()));
        Map<String, Long> byPlatform = posts.stream().collect(
                Collectors.groupingBy(Post::getPlatform, TreeMap::new, Collectors.counting()));

        return new Analytics(posts.size(), byStatus, byPlatform, impressions, likes, shares, rate);
    }
}