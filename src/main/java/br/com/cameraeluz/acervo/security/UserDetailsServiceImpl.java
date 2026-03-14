package br.com.cameraeluz.acervo.security;

import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of UserDetailsService.
 * This class is used by Spring Security to load user data from our database during authentication.
 */
@Service
public class UserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Locates the user based on the username.
     *
     * @param username the username identifying the user whose data is required.
     * @return a fully populated UserDetails object (never null).
     * @throws UsernameNotFoundException if the user could not be found.
     */
    @Override
    @Transactional // ← mantém a sessão aberta para o carregamento LAZY
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        return UserDetailsImpl.build(user); // roles são carregados aqui, dentro da transação
    }
}