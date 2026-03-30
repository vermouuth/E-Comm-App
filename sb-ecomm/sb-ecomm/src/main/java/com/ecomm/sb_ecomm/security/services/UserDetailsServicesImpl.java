package com.ecomm.sb_ecomm.security.services;

import com.ecomm.sb_ecomm.models.User;
import com.ecomm.sb_ecomm.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServicesImpl  implements UserDetailsService {


    private final UserRepository userRepository;

    public UserDetailsServicesImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return UserDetailsImpl.build(user);
    }
}
