package com.study.blog.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class OAuth2SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oauthAuthorizationEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void protectedEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/mypage"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/me/blog/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestShouldAccessPublicBlogReadEndpoints() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments/posts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void guestShouldAccessPublicBlogProfileEndpoint() throws Exception {
        mockMvc.perform(get("/api/blogs/unknown-user"))
                .andExpect(status().isNotFound());
    }

    @Test
    void authMeShouldBePublicAndReturnOkForGuest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }
}
