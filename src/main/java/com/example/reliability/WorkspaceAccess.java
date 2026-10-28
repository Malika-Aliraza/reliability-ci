package com.example.reliability;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceAccess {

    private final MembershipRepository memberships;

    public WorkspaceAccess(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    /** Returns the caller's membership, or throws 404 (not a member) / 403 (role too low). */
    public Membership require(Long workspaceId, Authentication auth, Role minimum) {
        Long userId = (Long) auth.getPrincipal();
        Membership m = memberships.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        if (!m.getRole().atLeast(minimum)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your role (" + m.getRole() + ") is not allowed to do this. " + minimum + " required");
        }
        return m;
    }
}