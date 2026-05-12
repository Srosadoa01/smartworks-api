package com.smartworks.smartworks_api.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.dto.AuthRequest;
import com.smartworks.smartworks_api.dto.AuthResponse;
import com.smartworks.smartworks_api.entity.AppUser;
import com.smartworks.smartworks_api.entity.Role;
import com.smartworks.smartworks_api.repository.UserRepository;
import com.smartworks.smartworks_api.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepo,
            PasswordEncoder encoder,
            AuthenticationManager authManager,
            JwtService jwtService
    ) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest req) {
        if (req.username == null || req.username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }

        if (req.password == null || req.password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        if (req.password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }

        String username = req.username.trim().toLowerCase();

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }

        Role role = parseRole(req.role);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(req.password));
        user.setRole(role);

        userRepo.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name()
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        if (req.username == null || req.username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }

        if (req.password == null || req.password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        String username = req.username.trim().toLowerCase();

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, req.password)
        );

        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name()
        );
    }

    private Role parseRole(String roleText) {
        if (roleText == null || roleText.isBlank()) {
            return Role.USER;
        }

        try {
            return Role.valueOf(roleText.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no válido. Usa ADMIN o USER.");
        }
    }
}