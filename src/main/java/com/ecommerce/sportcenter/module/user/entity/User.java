package com.ecommerce.sportcenter.module.user.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.role.entity.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application user. A user has one or more {@link Role}s,
 * each role aggregates Permission.
 * Spring Security authorities = ROLE_* + permission names.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "Username"),
        @Index(name = "idx_user_email", columnList = "Email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity implements UserDetails {

    @Column(name = "Username", unique = true, nullable = false)
    private String username;

    @Column(name = "Email", unique = true, nullable = false)
    private String email;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "FullName")
    private String fullName;

    @Column(name = "Enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "roleId")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ---------- UserDetails ----------

    @Override
    @Transient
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (roles == null) {
            return authorities;
        }
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            if (role.getPermissions() != null) {
                authorities.addAll(role.getPermissions().stream()
                        .map(p -> new SimpleGrantedAuthority(p.getName()))
                        .collect(Collectors.toSet()));
            }
        }
        return authorities;
    }

    @Override
    @Transient
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @Transient
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
