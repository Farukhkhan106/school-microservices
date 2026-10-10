package com.successacademy.authservice.service;

import com.successacademy.authservice.model.LoginRequest;
import com.successacademy.authservice.model.LoginResponse;
import com.successacademy.authservice.model.User;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Successful login returns JWT token and user details including roles")
    void testLogin_Success() {
        User user = new User();
        user.setId(10L);
        user.setUsername("teacher_john");
        user.setPassword("hashed_secret");
        user.setRole("TEACHER");
        user.setEmail("john@successacademy.com");
        user.setTeacherId(101L);

        when(userRepository.findByUsername("teacher_john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed_secret")).thenReturn(true);
        when(jwtUtil.generateToken(eq("teacher_john"), eq("TEACHER"), eq(10L), isNull(), eq(101L), isNull()))
                .thenReturn("mock.jwt.token");

        LoginRequest req = new LoginRequest("teacher_john", "secret123");
        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("mock.jwt.token", resp.getToken());
        assertEquals("TEACHER", resp.getRole());
        assertEquals(101L, resp.getTeacherId());
        assertEquals("teacher_john", resp.getUsername());
    }

    @Test
    @DisplayName("Invalid password throws RuntimeException")
    void testLogin_InvalidPassword_ThrowsException() {
        User user = new User();
        user.setId(10L);
        user.setUsername("admin");
        user.setPassword("hashed_secret");
        user.setRole("ADMIN");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_secret")).thenReturn(false);

        LoginRequest req = new LoginRequest("admin", "wrong_password");
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("User not found throws RuntimeException")
    void testLogin_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest("unknown", "secret");
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }
}
