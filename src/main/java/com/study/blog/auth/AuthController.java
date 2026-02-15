package com.study.blog.auth;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.security.JwtUtil;
import com.study.blog.user.User;
import com.study.blog.user.UserNamePolicy;
import com.study.blog.user.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseTemplate<Object>> register(@Valid @RequestBody RegisterRequest req) {
        log.info("회원가입 요청 :: {}", req);
        if (userRepository.existsByUsername(req.username())) {
            ApiResponseTemplate<Object> body = new ApiResponseTemplate<>(null, HttpStatus.BAD_REQUEST,
                    "이미 존재하는 아이디입니다.", false);
            return ResponseEntity.badRequest().body(body);
        }
        User user = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .name(UserNamePolicy.resolveName(req.name(), req.username()))
                .createdAt(LocalDateTime.now())
                .deletedYn("N")
                .build();
        userRepository.save(user);
        return ApiResponseFactory.ok(Map.of("success", true));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseTemplate<Object>> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (Exception e) {
            log.error("AUTH FAIL", e);
            ApiResponseTemplate<Object> body = new ApiResponseTemplate<>(null, HttpStatus.UNAUTHORIZED, "계정 정보가 없습니다.",
                    false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String token = jwtUtil.generateToken(user.getUsername());
        log.info("token :: {}", token);

        return ApiResponseFactory.ok(new AuthResponse.Login(token, toUserSummary(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseTemplate<Object>> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            ApiResponseTemplate<Object> body = new ApiResponseTemplate<>(
                    null,
                    HttpStatus.UNAUTHORIZED,
                    "인증 사용자 정보를 확인할 수 없습니다.",
                    false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return ApiResponseFactory.ok(toUserSummary(user));
    }

    private AuthResponse.UserSummary toUserSummary(User user) {
        return new AuthResponse.UserSummary(user.getId(), user.getUsername(),
                UserNamePolicy.resolveName(user.getName(), user.getUsername()));
    }
}
