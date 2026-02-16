package com.study.blog.admin;

import com.study.blog.admin.dto.AdminDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseTemplate<AdminDto.MeResponse>> me(Authentication authentication) {
        return ApiResponseFactory.ok(adminService.getMe(authentication.getName()));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponseTemplate<AdminDto.DashboardSummaryResponse>> dashboardSummary() {
        return ApiResponseFactory.ok(adminService.getDashboardSummary());
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword) {
        return ApiResponseFactory.ok(adminService.listUsers(pageable, keyword));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponseTemplate<AdminDto.UserSummaryResponse>> updateUserRole(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminDto.UpdateUserRoleRequest req) {
        return ApiResponseFactory.ok(adminService.updateUserRole(authentication.getName(), id, req.getRole()));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponseTemplate<AdminDto.UserSummaryResponse>> updateUserStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminDto.UpdateUserStatusRequest req) {
        return ApiResponseFactory.ok(adminService.updateUserStatus(authentication.getName(), id, req.getStatus()));
    }

    @GetMapping("/posts")
    public ResponseEntity<?> listPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean deleted) {
        return ApiResponseFactory.ok(adminService.listPosts(pageable, keyword, deleted));
    }

    @PatchMapping("/posts/{id}/hide")
    public ResponseEntity<ApiResponseTemplate<AdminDto.PostSummaryResponse>> hidePost(
            Authentication authentication,
            @PathVariable Long id) {
        return ApiResponseFactory.ok(adminService.hidePost(authentication.getName(), id));
    }

    @PatchMapping("/posts/{id}/restore")
    public ResponseEntity<ApiResponseTemplate<AdminDto.PostSummaryResponse>> restorePost(
            Authentication authentication,
            @PathVariable Long id) {
        return ApiResponseFactory.ok(adminService.restorePost(authentication.getName(), id));
    }

    @GetMapping("/comments")
    public ResponseEntity<?> listComments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean deleted) {
        return ApiResponseFactory.ok(adminService.listComments(pageable, keyword, deleted));
    }

    @PatchMapping("/comments/{id}/hide")
    public ResponseEntity<ApiResponseTemplate<AdminDto.CommentSummaryResponse>> hideComment(
            Authentication authentication,
            @PathVariable Long id) {
        return ApiResponseFactory.ok(adminService.hideComment(authentication.getName(), id));
    }

    @PatchMapping("/comments/{id}/restore")
    public ResponseEntity<ApiResponseTemplate<AdminDto.CommentSummaryResponse>> restoreComment(
            Authentication authentication,
            @PathVariable Long id) {
        return ApiResponseFactory.ok(adminService.restoreComment(authentication.getName(), id));
    }
}
