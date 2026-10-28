package com.example.reliability;

import jakarta.persistence.*;

@Entity
@Table(name = "memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public Membership() {}
    public Membership(Long workspaceId, Long userId, Role role) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
    }

    public Long getId() { return id; }
    public Long getWorkspaceId() { return workspaceId; }
    public Long getUserId() { return userId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}