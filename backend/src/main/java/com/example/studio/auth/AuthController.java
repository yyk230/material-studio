package com.example.studio.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.studio.config.AppProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String COOKIE_NAME = "SESSION";

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @Size(max = 32) String displayName) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    private final AuthService authService;
    private final AppProperties props;

    public AuthController(AuthService authService, AppProperties props) {
        this.authService = authService;
        this.props = props;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.Login login = authService.register(request.email(), request.password(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(login.token()).toString())
                .body(login.user());
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequest request) {
        AuthService.Login login = authService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(login.token()).toString())
                .body(login.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_NAME, required = false) String token) {
        authService.logout(token);
        ResponseCookie cleared = baseCookie("").maxAge(0).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }

    @GetMapping("/me")
    public UserDto me(@CurrentUserId long userId) {
        return authService.me(userId);
    }

    private ResponseCookie sessionCookie(String token) {
        return baseCookie(token).maxAge(props.auth().sessionTtl()).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .secure(props.auth().cookieSecure());
    }
}
