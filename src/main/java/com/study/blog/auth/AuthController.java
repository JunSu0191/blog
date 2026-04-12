package com.study.blog.auth;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.oauth.OAuthAccountService;
import com.study.blog.security.JwtUtil;
import com.study.blog.user.UserAvatarService;
import com.study.blog.user.User;
import com.study.blog.user.UserNamePolicy;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import com.study.blog.user.UserRepository;
import com.study.blog.verification.VerificationChannel;
import com.study.blog.verification.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final VerificationService verificationService;
    private final AccountRecoveryService accountRecoveryService;
    private final OAuthAccountService oauthAccountService;
    private final UserAvatarService userAvatarService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtUtil jwtUtil,
            VerificationService verificationService,
            AccountRecoveryService accountRecoveryService,
            OAuthAccountService oauthAccountService,
            UserAvatarService userAvatarService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.verificationService = verificationService;
        this.accountRecoveryService = accountRecoveryService;
        this.oauthAccountService = oauthAccountService;
        this.userAvatarService = userAvatarService;
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> checkUsername(@RequestParam String username) {
        String normalized = UserNamePolicy.validatePublicUsername(username);
        boolean available = !userRepository.existsByUsername(normalized);
        return ApiResponseFactory.ok(Map.of("available", available));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> checkNickname(@RequestParam String nickname) {
        String normalized = normalizeNullable(nickname);
        if (normalized == null) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }
        return ApiResponseFactory.ok(Map.of("available", true));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseTemplate<Object>> register(@Valid @RequestBody RegisterRequest req) {
        String username = UserNamePolicy.validatePublicUsername(req.username());
        String nickname = normalizeNullable(req.nickname());
        String phoneNumber = normalizePhoneNumber(req.phoneNumber());
        String emailRaw = normalizeNullable(req.email());
        String email = emailRaw == null ? null : verificationService.normalizeTarget(VerificationChannel.EMAIL, emailRaw);

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("아이디를 입력해 주세요.");
        }
        if (nickname == null) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }
        if (userRepository.existsByUsername(username)) {
            ApiResponseTemplate<Object> body = new ApiResponseTemplate<>(null, HttpStatus.BAD_REQUEST,
                    "이미 존재하는 아이디입니다.", false);
            return ResponseEntity.badRequest().body(body);
        }
        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("이미 존재하는 휴대폰 번호입니다.");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(req.password()))
                .name(UserNamePolicy.resolveName(req.name(), username))
                .nickname(nickname)
                .email(email)
                .phoneNumber(phoneNumber)
                .phoneVerifiedAt(null)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .createdAt(LocalDateTime.now())
                .deletedYn("N")
                .build();
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("동일 정보의 계정이 이미 존재합니다. 다시 확인해 주세요.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("user", toUserSummary(user));
        return ApiResponseFactory.ok(payload);
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

        User user = userRepository.findByUsernameAndDeletedYn(req.username(), "N")
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String token = jwtUtil.generateToken(user.getUsername());

        return ApiResponseFactory.ok(new AuthResponse.Login(token, toUserSummary(user)));
    }

    @GetMapping("/oauth/signup/pending")
    public ResponseEntity<ApiResponseTemplate<AuthResponse.PendingOAuthSignup>> getPendingOAuthSignup(
            @RequestParam String signupToken) {
        OAuthAccountService.PendingSignupProfile pendingSignup = oauthAccountService.getPendingSignupProfile(signupToken);
        return ApiResponseFactory.ok(new AuthResponse.PendingOAuthSignup(
                pendingSignup.signupToken(),
                pendingSignup.provider(),
                pendingSignup.email(),
                pendingSignup.name(),
                pendingSignup.suggestedUsername(),
                pendingSignup.suggestedNickname()));
    }

    @PostMapping("/oauth/signup/complete")
    public ResponseEntity<ApiResponseTemplate<Object>> completeOAuthSignup(
            @Valid @RequestBody OAuthSignupCompleteRequest req) {
        User user = oauthAccountService.completeSignup(req.signupToken(), req.username(), req.nickname());
        String token = jwtUtil.generateToken(user.getUsername());
        return ApiResponseFactory.ok(new AuthResponse.Login(token, toUserSummary(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseTemplate<Object>> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return ApiResponseFactory.ok((Object) null);
        }

        User user = userRepository.findByUsernameAndDeletedYn(authentication.getName(), "N")
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return ApiResponseFactory.ok(toUserSummary(user));
    }

    @PostMapping("/find-id/request")
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> requestFindId(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody AccountRecoveryDto.RequestVerification req) {
        VerificationService.SendResult result = accountRecoveryService.requestFindId(
                req.getChannel(),
                req.getTarget(),
                httpServletRequest.getRemoteAddr());
        return ApiResponseFactory.ok(toRecoveryRequestPayload(result));
    }

    @PostMapping("/find-id/confirm")
    public ResponseEntity<ApiResponseTemplate<AccountRecoveryDto.FindIdResponse>> confirmFindId(
            @Valid @RequestBody AccountRecoveryDto.ConfirmVerification req) {
        String maskedUsername = accountRecoveryService.confirmFindId(req.getVerificationId(), req.getCode());
        AccountRecoveryDto.FindIdResponse response = new AccountRecoveryDto.FindIdResponse();
        response.setMaskedUsername(maskedUsername);
        return ApiResponseFactory.ok(response);
    }

    @PostMapping("/reset-password/request")
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> requestResetPassword(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody AccountRecoveryDto.RequestVerification req) {
        VerificationService.SendResult result = accountRecoveryService.requestResetPassword(
                req.getChannel(),
                req.getTarget(),
                httpServletRequest.getRemoteAddr());
        return ApiResponseFactory.ok(toRecoveryRequestPayload(result));
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<ApiResponseTemplate<AccountRecoveryDto.ResetPasswordConfirmResponse>> confirmResetPassword(
            @Valid @RequestBody AccountRecoveryDto.ConfirmVerification req) {
        AccountRecoveryService.ResetTokenResult result = accountRecoveryService.confirmResetPassword(
                req.getVerificationId(),
                req.getCode());
        AccountRecoveryDto.ResetPasswordConfirmResponse response = new AccountRecoveryDto.ResetPasswordConfirmResponse();
        response.setResetToken(result.resetToken());
        response.setExpiresAt(result.expiresAt());
        return ApiResponseFactory.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> resetPassword(
            @Valid @RequestBody AccountRecoveryDto.ResetPasswordRequest req) {
        accountRecoveryService.resetPassword(req.getResetToken(), req.getNewPassword());
        return ApiResponseFactory.ok(Map.of("success", true));
    }

    private AuthResponse.UserSummary toUserSummary(User user) {
        return new AuthResponse.UserSummary(user.getId(), user.getUsername(),
                UserNamePolicy.resolveName(user.getName(), user.getUsername()),
                user.getNickname(),
                user.getEmail(),
                user.getPhoneNumber(),
                userAvatarService.getAvatarUrl(user.getId()),
                user.getRole(),
                user.getStatus(),
                user.getMustChangePassword());
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePhoneNumber(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        return verificationService.normalizeTarget(VerificationChannel.SMS, normalized);
    }

    private Map<String, Object> toRecoveryRequestPayload(VerificationService.SendResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verificationId", result.verificationCode().getId());
        payload.put("expiresAt", result.verificationCode().getExpiresAt());
        payload.put("resendCount", result.verificationCode().getResendCount());
        payload.put("cooldownSeconds", result.cooldownSeconds());
        if (result.debugCode() != null) {
            payload.put("debugCode", result.debugCode());
        }
        payload.put("message", "인증번호가 전송되었습니다.");
        return payload;
    }
}
