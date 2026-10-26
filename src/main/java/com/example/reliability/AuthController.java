package com.example.reliability;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record AuthRequest(String email, String password) {}
    public record AuthResponse(String token, String email) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest req) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (!email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        AppUser user = users.save(new AppUser(email, encoder.encode(req.password())));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(jwt.generateToken(user.getId(), user.getEmail()), user.getEmail()));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        AppUser user = users.findByEmail(email)
                .filter(u -> req.password() != null && encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generateToken(user.getId(), user.getEmail()), user.getEmail());
    }
}