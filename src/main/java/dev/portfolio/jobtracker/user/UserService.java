package dev.portfolio.jobtracker.user;

import dev.portfolio.jobtracker.web.dto.RegisterRequest;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Emails are compared case-insensitively, so store them in one canonical form. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        try {
            return users.saveAndFlush(new AppUser(
                    email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent sign-ups with the same email: the unique constraint is the real guard.
            throw new EmailAlreadyRegisteredException();
        }
    }
}
