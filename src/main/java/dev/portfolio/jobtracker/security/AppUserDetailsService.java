package dev.portfolio.jobtracker.security;

import dev.portfolio.jobtracker.user.AppUserRepository;
import dev.portfolio.jobtracker.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(UserService.normalizeEmail(email))
                .map(AuthUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("No such user"));
    }
}
