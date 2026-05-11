/*package com.smartworks.smartworks_api.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.smartworks.smartworks_api.dto.AuthRequest;
import com.smartworks.smartworks_api.dto.AuthResponse;
import com.smartworks.smartworks_api.entity.AppUser;
import com.smartworks.smartworks_api.entity.Role;
import com.smartworks.smartworks_api.repository.UserRepository;
import com.smartworks.smartworks_api.security.JwtService;

//@RestController
//@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepo,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          JwtService jwtService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest req) {
        if (req.username == null || req.username.isBlank()) throw new IllegalArgumentException("username required");
        if (req.password == null || req.password.isBlank()) throw new IllegalArgumentException("password required");

        if (userRepo.existsByUsername(req.username)) {
            throw new IllegalArgumentException("username already exists");
        }

        AppUser u = new AppUser();
        u.setUsername(req.username);
        u.setPasswordHash(encoder.encode(req.password));
        u.setRole(Role.USER);

        userRepo.save(u);

        String token = jwtService.generateToken(u.getUsername(), u.getRole().name());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username, req.password)
        );

        AppUser u = userRepo.findByUsername(req.username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        String token = jwtService.generateToken(u.getUsername(), u.getRole().name());
        return new AuthResponse(token);
    }
}
/* */