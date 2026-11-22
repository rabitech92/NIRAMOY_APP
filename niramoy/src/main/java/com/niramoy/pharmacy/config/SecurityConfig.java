package com.niramoy.pharmacy.config;

import org.springframework.context.annotation.Bean;
import com.niramoy.pharmacy.repository.AppUserRepository;
import com.niramoy.pharmacy.security.JwtAuthFilter;
import com.niramoy.pharmacy.security.JwtService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String SA = "SUPER_ADMIN", OWNER = "OWNER", BM = "BRANCH_MANAGER",
            PH = "PHARMACIST", CASH = "CASHIER", INV = "INVENTORY_MANAGER";

    /** মোবাইল অ্যাপের REST API: stateless, JWT, CSRF নেই */
    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http, JwtService jwt, AppUserRepository users) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/medicines/**").hasAnyRole(SA, OWNER, BM, PH, INV)
                .requestMatchers("/api/v1/pos/**", "/api/v1/sales/**", "/api/v1/refills/**").hasAnyRole(SA, OWNER, BM, PH, CASH)
                .requestMatchers("/api/v1/dues/**").hasAnyRole(SA, OWNER, BM, INV)
                .requestMatchers("/api/v1/customers/**", "/api/v1/business/**").hasAnyRole(SA, OWNER, BM)
                .requestMatchers("/api/v1/best-sellers/mark", "/api/v1/best-sellers/mark-top", "/api/v1/best-sellers/unmark")
                        .hasAnyRole(SA, OWNER, BM, INV)
                .requestMatchers("/api/v1/best-sellers/**").hasAnyRole(SA, OWNER, BM, PH, INV)
                .requestMatchers("/api/v1/expenses/*/void").hasAnyRole(SA, OWNER)
                .requestMatchers("/api/v1/expenses/**").hasAnyRole(SA, OWNER, BM)
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> json(res, HttpStatus.UNAUTHORIZED, "লগইন প্রয়োজন বা সেশনের মেয়াদ শেষ হয়েছে।"))
                .accessDeniedHandler((req, res, ex) -> json(res, HttpStatus.FORBIDDEN, "এই কাজের অনুমতি আপনার নেই।")))
            .addFilterBefore(new JwtAuthFilter(jwt, users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void json(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String message)
            throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
                .requestMatchers("/subscription/expired").authenticated()
                .requestMatchers("/subscription/manage", "/subscription/grant", "/subscription/plans/**",
                        "/subscription/*/approve", "/subscription/*/reject").hasRole(SA)
                .requestMatchers("/subscription/**").hasAnyRole(SA, OWNER)
                .requestMatchers("/business/purchases").hasAnyRole(SA, OWNER)
                .requestMatchers("/business/**").hasAnyRole(SA, OWNER, BM)
                .requestMatchers("/customers/**").hasAnyRole(SA, OWNER, BM)
                .requestMatchers("/best-sellers/mark", "/best-sellers/mark-top", "/best-sellers/unmark").hasAnyRole(SA, OWNER, BM, INV)
                .requestMatchers("/best-sellers/**").hasAnyRole(SA, OWNER, BM, PH, INV)
                .requestMatchers("/expenses/categories/**", "/expenses/*/void").hasAnyRole(SA, OWNER)
                .requestMatchers("/expenses/**").hasAnyRole(SA, OWNER, BM)
                .requestMatchers("/admin/**").hasAnyRole(SA, OWNER)
                .requestMatchers("/reports/**").hasAnyRole(SA, OWNER, BM)
                .requestMatchers("/dues/**", "/purchases/suppliers/**", "/purchases/orders/*/return").hasAnyRole(SA, OWNER, BM, INV)
                .requestMatchers("/purchases/**").hasAnyRole(SA, OWNER, BM, PH, INV)
                .requestMatchers("/inventory/new", "/inventory/save", "/inventory/*/adjust").hasAnyRole(SA, OWNER, BM, INV)
                .requestMatchers("/inventory/**").hasAnyRole(SA, OWNER, BM, PH, INV)
                .requestMatchers("/pos/**", "/sales/**", "/refills/**").hasAnyRole(SA, OWNER, BM, PH, CASH)
                .requestMatchers("/returns/**").hasAnyRole(SA, OWNER, BM, PH)
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .exceptionHandling(e -> e.accessDeniedPage("/access-denied"))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
