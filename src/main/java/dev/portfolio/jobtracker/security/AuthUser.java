package dev.portfolio.jobtracker.security;

import dev.portfolio.jobtracker.user.AppUser;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The signed-in user as stored in the session. Carries the id so queries can be scoped to it. */
public final class AuthUser implements UserDetails, CredentialsContainer {

    private final long id;
    private final String email;
    private final String displayName;
    private String passwordHash;

    public AuthUser(long id, String email, String displayName, String passwordHash) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    public static AuthUser from(AppUser user) {
        return new AuthUser(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash());
    }

    public long id() { return id; }
    public String email() { return email; }
    public String displayName() { return displayName; }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /** Called by Spring Security after login so the hash isn't kept in the session. */
    @Override public void eraseCredentials() { passwordHash = null; }
}
