/*
 * package com.smartworks.smartworks_api.security;
 * 
 * import java.nio.charset.StandardCharsets;
 * import java.util.Date;
 * 
 * import javax.crypto.SecretKey;
 * 
 * import org.springframework.beans.factory.annotation.Value;
 * import org.springframework.stereotype.Service;
 * 
 * import io.jsonwebtoken.Claims;
 * import io.jsonwebtoken.Jwts;
 * import io.jsonwebtoken.SignatureAlgorithm;
 * import io.jsonwebtoken.security.Keys;
 * 
 * @Service
 * public class JwtService {
 * 
 * private final SecretKey key;
 * private final long expirationMs;
 * 
 * public JwtService(
 * 
 * @Value("${app.jwt.secret}") String secret,
 * 
 * @Value("${app.jwt.expiration-ms}") long expirationMs
 * ) {
 * this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
 * this.expirationMs = expirationMs;
 * }
 * 
 * public String generateToken(String username, String role) {
 * Date now = new Date();
 * Date exp = new Date(now.getTime() + expirationMs);
 * 
 * return Jwts.builder()
 * .setSubject(username)
 * .claim("role", role)
 * .setIssuedAt(now)
 * .setExpiration(exp)
 * .signWith(key, SignatureAlgorithm.HS256)
 * .compact();
 * }
 * 
 * public String extractUsername(String token) {
 * return getClaims(token).getSubject();
 * }
 * 
 * public String extractRole(String token) {
 * Object role = getClaims(token).get("role");
 * return role == null ? null : role.toString();
 * }
 * 
 * public boolean isTokenValid(String token) {
 * try {
 * getClaims(token);
 * return true;
 * } catch (Exception e) {
 * return false;
 * }
 * }
 * 
 * private Claims getClaims(String token) {
 * return Jwts.parserBuilder()
 * .setSigningKey(key)
 * .build()
 * .parseClaimsJws(token)
 * .getBody();
 * }
 * }
 */