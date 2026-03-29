package com.study.blog.verification;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.verification.dto.VerificationDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verifications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponseTemplate<VerificationDto.SendResponse>> send(
            HttpServletRequest httpRequest,
            @Valid @RequestBody VerificationDto.SendRequest req) {
        VerificationService.SendResult result = verificationService.send(
                req.getPurpose(),
                req.getChannel(),
                req.getTarget(),
                httpRequest.getRemoteAddr(),
                true);

        VerificationDto.SendResponse response = new VerificationDto.SendResponse();
        response.setVerificationId(result.verificationCode().getId());
        response.setExpiresAt(result.verificationCode().getExpiresAt());
        response.setResendCount(result.verificationCode().getResendCount());
        response.setCooldownSeconds(result.cooldownSeconds());
        response.setDebugCode(result.debugCode());
        return ApiResponseFactory.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponseTemplate<VerificationDto.ConfirmResponse>> confirm(
            @Valid @RequestBody VerificationDto.ConfirmRequest req) {
        VerificationCode verificationCode = verificationService.confirm(req.getVerificationId(), req.getCode(), null);

        VerificationDto.ConfirmResponse response = new VerificationDto.ConfirmResponse();
        response.setVerificationId(verificationCode.getId());
        response.setPurpose(verificationCode.getPurpose());
        response.setChannel(verificationCode.getChannel());
        response.setTarget(verificationCode.getTarget());
        response.setVerifiedAt(verificationCode.getVerifiedAt());

        return ApiResponseFactory.ok(response);
    }
}
