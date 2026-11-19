package dev.portfolio.jobtracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Session-cookie auth for a same-origin browser app.
 *
 * <p>Why sessions instead of JWTs: the session id lives in an HttpOnly cookie that page
 * scripts can't read, so an XSS bug can't exfiltrate it, and logout truly revokes access.
 * Cookies need CSRF protection, which {@code csrf.spa()} provides via an XSRF-TOKEN cookie
 * that the frontend echoes back in a header.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/*.css", "/*.js", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/csrf", "/api/demo").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.spa())
                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("email")
                        .successHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                // API clients get a 401, not a redirect to a login page.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
