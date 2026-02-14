package com.study.blog.auth;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.security.JwtUtil;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                .name(req.name())
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

        String token = jwtUtil.generateToken(req.username());
        log.info("token :: {}", token);

        return ApiResponseFactory.ok(Map.of("token", token));
    }
}
