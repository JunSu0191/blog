package com.study.blog.admin;

import com.study.blog.admin.dto.AdminDto;
import com.study.blog.config.AllowedOriginsProvider;
import com.study.blog.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import(AdminControllerSecurityTest.TestSecurityConfig.class)
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;
    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminShouldAccessAdminApi() throws Exception {
        AdminDto.MeResponse meResponse = new AdminDto.MeResponse();
        meResponse.setId(1L);
        meResponse.setUsername("admin");
        when(adminService.getMe(anyString())).thenReturn(meResponse);

        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userShouldBeForbiddenForAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        AllowedOriginsProvider allowedOriginsProvider() {
            return new AllowedOriginsProvider("http://localhost:5173");
        }

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}
