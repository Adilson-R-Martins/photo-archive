package br.com.cameraeluz.acervo.security;

import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of {@link org.springframework.security.core.userdetails.UserDetailsService}.
 *
 * <p>Loads user data from the database during Spring Security's authentication process.
 * The method is annotated with {@link Transactional} to keep the JPA session open long
 * enough for the lazy-loaded {@code roles} collection to be initialised inside
 * {@link UserDetailsImpl#build(User)}.</p>
 */
@Service
public class UserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Locates the user by username and builds a fully populated {@link UserDetails} object.
     *
     * <p>The {@link Transactional} boundary ensures that the lazy {@code roles}
     * association is loaded within the same session before the entity is detached.</p>
     *
     * @param username the username identifying the user whose data is required.
     * @return a fully populated {@link UserDetails} object (never {@code null}).
     * @throws UsernameNotFoundException if no user with the given username exists.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        return UserDetailsImpl.build(user);
    }
}
