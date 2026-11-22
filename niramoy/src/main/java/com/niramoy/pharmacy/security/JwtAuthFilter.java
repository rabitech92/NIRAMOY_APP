package com.niramoy.pharmacy.security;

import com.niramoy.pharmacy.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** "Authorization: Bearer <jwt>" থেকে ব্যবহারকারী শনাক্ত করে (শুধু /api/** চেইনে বসানো হয়) */
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AppUserRepository users;

    public JwtAuthFilter(JwtService jwt, AppUserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long userId = jwt.verify(header.substring(7).trim());
            if (userId != null) {
                users.findById(userId).filter(u -> u.isActive()).ifPresent(u -> {
                    UserPrincipal p = new UserPrincipal(u);
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()));
                });
            }
        }
        chain.doFilter(req, res);
    }
}
