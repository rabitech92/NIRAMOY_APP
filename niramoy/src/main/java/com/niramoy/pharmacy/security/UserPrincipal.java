package com.niramoy.pharmacy.security;

import com.niramoy.pharmacy.entity.AppUser;
import com.niramoy.pharmacy.entity.Role;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Role role;
    private final Long branchId;
    private final String branchName;
    private final boolean enabled;

    public UserPrincipal(AppUser u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.password = u.getPassword();
        this.fullName = u.getFullName();
        this.role = u.getRole();
        this.branchId = u.getBranch() != null ? u.getBranch().getId() : null;
        this.branchName = u.getBranch() != null ? u.getBranch().getName() : null;
        this.enabled = u.isActive();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    /** সব শাখা দেখতে পারেন কি না */
    public boolean isGlobal() { return role.isGlobal(); }

    public static UserPrincipal current() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof UserPrincipal p ? p : null;
    }
}
