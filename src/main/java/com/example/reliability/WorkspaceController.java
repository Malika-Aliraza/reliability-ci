package com.example.reliability;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    public record WorkspaceView(Long id, String name, Role role) {}
    public record MemberView(Long userId, String email, Role role) {}
    public record CreateWorkspaceRequest(String name) {}
    public record AddMemberRequest(String email, Role role) {}
    public record RoleRequest(Role role) {}

    private final WorkspaceRepository workspaces;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final WorkspaceAccess access;

    public WorkspaceController(WorkspaceRepository workspaces, MembershipRepository memberships,
                               UserRepository users, WorkspaceAccess access) {
        this.workspaces = workspaces;
        this.memberships = memberships;
        this.users = users;
        this.access = access;
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    private MemberView view(Membership m) {
        String email = users.findById(m.getUserId()).map(AppUser::getEmail).orElse("unknown");
        return new MemberView(m.getUserId(), email, m.getRole());
    }

    private void guardLastAdmin(Membership m, Long workspaceId) {
        if (m.getRole() == Role.ADMIN
                && memberships.countByWorkspaceIdAndRole(workspaceId, Role.ADMIN) <= 1) {
            throw error(HttpStatus.CONFLICT, "A workspace must keep at least one admin");
        }
    }

    // ----- workspaces -----

    @GetMapping
    public List<WorkspaceView> mine(Authentication auth) {
        List<Membership> mine = memberships.findByUserId((Long) auth.getPrincipal());
        Map<Long, Workspace> byId = workspaces
                .findAllById(mine.stream().map(Membership::getWorkspaceId).toList())
                .stream().collect(Collectors.toMap(Workspace::getId, Function.identity()));
        return mine.stream()
                .filter(m -> byId.containsKey(m.getWorkspaceId()))
                .map(m -> new WorkspaceView(m.getWorkspaceId(), byId.get(m.getWorkspaceId()).getName(), m.getRole()))
                .sorted(Comparator.comparing(WorkspaceView::id))
                .toList();
    }

    @PostMapping
    public ResponseEntity<WorkspaceView> create(@RequestBody CreateWorkspaceRequest req, Authentication auth) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty() || name.length() > 60) {
            throw error(HttpStatus.BAD_REQUEST, "Workspace name must be 1-60 characters");
        }
        Workspace ws = workspaces.save(new Workspace(name));
        memberships.save(new Membership(ws.getId(), (Long) auth.getPrincipal(), Role.ADMIN));
        return ResponseEntity.status(HttpStatus.CREATED).body(new WorkspaceView(ws.getId(), ws.getName(), Role.ADMIN));
    }

    // ----- members -----

    @GetMapping("/{wid}/members")
    public List<MemberView> members(@PathVariable Long wid, Authentication auth) {
        access.require(wid, auth, Role.VIEWER);
        return memberships.findByWorkspaceId(wid).stream().map(this::view).toList();
    }

    @PostMapping("/{wid}/members")
    public ResponseEntity<MemberView> addMember(@PathVariable Long wid, @RequestBody AddMemberRequest req,
                                                Authentication auth) {
        access.require(wid, auth, Role.ADMIN);
        if (req.role() == null) throw error(HttpStatus.BAD_REQUEST, "A role is required");
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No registered user with that email"));
        if (memberships.existsByWorkspaceIdAndUserId(wid, user.getId())) {
            throw error(HttpStatus.CONFLICT, "That user is already a member");
        }
        Membership m = memberships.save(new Membership(wid, user.getId(), req.role()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view(m));
    }

    @PutMapping("/{wid}/members/{userId}")
    public MemberView changeRole(@PathVariable Long wid, @PathVariable Long userId,
                                 @RequestBody RoleRequest req, Authentication auth) {
        access.require(wid, auth, Role.ADMIN);
        if (req.role() == null) throw error(HttpStatus.BAD_REQUEST, "A role is required");
        Membership m = memberships.findByWorkspaceIdAndUserId(wid, userId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Member not found"));
        if (req.role() != Role.ADMIN) guardLastAdmin(m, wid);
        m.setRole(req.role());
        return view(memberships.save(m));
    }

    @DeleteMapping("/{wid}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long wid, @PathVariable Long userId,
                                             Authentication auth) {
        access.require(wid, auth, Role.ADMIN);
        Membership m = memberships.findByWorkspaceIdAndUserId(wid, userId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Member not found"));
        guardLastAdmin(m, wid);
        memberships.delete(m);
        return ResponseEntity.noContent().build();
    }
}