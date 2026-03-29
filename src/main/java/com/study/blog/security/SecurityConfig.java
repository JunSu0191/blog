package com.study.blog.security;

import com.study.blog.config.AllowedOriginsProvider;
import com.study.blog.oauth.CustomOAuth2UserService;
import com.study.blog.oauth.OAuth2AuthenticationFailureHandler;
import com.study.blog.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            UserDetailsService userDetailsService,
            AllowedOriginsProvider allowedOriginsProvider) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.allowedOriginPatterns = allowedOriginsProvider.asList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomOAuth2UserService customOAuth2UserService,
                                                   OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
                                                   OAuth2AuthenticationFailureHandler oauth2FailureHandler) throws Exception {
        http.csrf().disable()
                .cors().and()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register",
                                "/api/auth/check-username", "/api/auth/check-nickname",
                                "/api/auth/find-id/**", "/api/auth/reset-password/**",
                                "/api/auth/me",
                                "/api/verifications/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/h2-console/**", "/actuator/**",
                                "/api/attach-files/uploads/**", "/api/attach-files/complete",
                                "/upload/**", "/uploads/**", "/ws", "/ws/**", "/ws-sockjs/**",
                                "/api/chat/**", "/api/chats/**", "/api/friends/**", "/api/notifications/**",
                                "/api/blogs/**",
                                "/oauth2/**", "/login/oauth2/**")
                        .permitAll()
                        .requestMatchers("/api/posts/drafts/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/*", "/api/posts/*/related")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/posts/*").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/posts/slug/check",
                                "/api/v1/tags", "/api/v1/tags/*", "/api/v1/tags/*/posts",
                                "/api/v1/categories/*", "/api/v1/categories/*/posts",
                                "/api/v1/search", "/api/v1/search/*",
                                "/api/v1/series", "/api/v1/series/*")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // allow h2 console frames
        http.headers().frameOptions().disable();

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // allow wildcard origin patterns for LAN/mobile development (e.g. 192.168.x.x)
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "HEAD", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-User-Id",
                "Location",
                "Tus-Resumable",
                "Tus-Version",
                "Tus-Extension",
                "Tus-Max-Size",
                "Upload-Offset",
                "Upload-Length",
                "Upload-Metadata"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
