package com.ecomm.sb_ecomm.auth;

import com.ecomm.sb_ecomm.auth.model.AppRole;
import com.ecomm.sb_ecomm.auth.model.Role;
import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.auth.repository.RoleRepository;
import com.ecomm.sb_ecomm.auth.repository.UserRepository;
import com.ecomm.sb_ecomm.auth.jwt.AuthEntryPoint;
import com.ecomm.sb_ecomm.auth.jwt.AuthTokenFilter;
import com.ecomm.sb_ecomm.auth.services.UserDetailsServicesImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    private  UserDetailsServicesImpl userDetailsServices;

    @Autowired
    private  AuthEntryPoint unauthorizedHandler;

    @Bean
    public AuthTokenFilter authTokenFilter() {return new AuthTokenFilter();}

    // The JWT filter belongs only in the security chain. Without this, Spring Boot would also
    // register it as a plain servlet filter because it is a Filter bean.
    @Bean
    public FilterRegistrationBean<AuthTokenFilter> authTokenFilterRegistration(AuthTokenFilter filter) {
        FilterRegistrationBean<AuthTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // Authenticated but not allowed (e.g. a USER hitting /api/admin/**): same JSON shape as the 401.
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), Map.of(
                    "status", HttpServletResponse.SC_FORBIDDEN,
                    "error", "Forbidden",
                    "message", "You do not have permission to access this resource",
                    "path", request.getRequestURI()));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsServices);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration builder) throws Exception {
        return builder.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                                 auth
                                .requestMatchers("/v3/api-docs/**").permitAll()
                                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/user/**").hasAnyRole("ADMIN" ,"USER")
                                .requestMatchers("/api/public/**").permitAll()
                                .requestMatchers("/api/auth/user").authenticated()
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/images/**").permitAll()
                                // Authenticated by an HMAC signature in the controller, not by a login.
                                .requestMatchers("/api/webhooks/**").permitAll()
                                // Can forge "payment captured" webhooks: staff only (and off unless enabled).
                                .requestMatchers("/api/mock-gateway/**").hasRole("ADMIN")
                                .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Demo accounts with well-known passwords: created only when app.seed-data.enabled=true (dev profile).
    @Bean
    @ConditionalOnProperty(name = "app.seed-data.enabled", havingValue = "true")
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Retrieve or create roles
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseGet(() -> {
                        Role newUserRole = new Role(AppRole.ROLE_USER);
                        return roleRepository.save(newUserRole);
                    });

            Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                    .orElseGet(() -> {
                        Role newSellerRole = new Role(AppRole.ROLE_SELLER);
                        return roleRepository.save(newSellerRole);
                    });

            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                    .orElseGet(() -> {
                        Role newAdminRole = new Role(AppRole.ROLE_ADMIN);
                        return roleRepository.save(newAdminRole);
                    });

            Set<Role> userRoles = Set.of(userRole);
            Set<Role> sellerRoles = Set.of(sellerRole);
            Set<Role> adminRoles = Set.of(userRole, sellerRole, adminRole);


            // Create users if not already present
            if (!userRepository.existsByUsername("user1")) {
                Users user1 = new Users("user1",passwordEncoder.encode("password1") ,"user1@example.com" );
                userRepository.save(user1);
            }

            if (!userRepository.existsByUsername("seller1")) {
                Users seller1 = new Users("seller1", passwordEncoder.encode("password2"),"seller1@example.com" );
                userRepository.save(seller1);
            }

            if (!userRepository.existsByUsername("admin")) {
                Users admin = new Users("admin",  passwordEncoder.encode("adminPass") ,"admin@example.com");
                userRepository.save(admin);
            }

            // Update roles for existing users
            userRepository.findByUsername(("user1")).ifPresent(user -> {
                user.setRoles(userRoles);
                userRepository.save(user);
            });

            userRepository.findByUsername("seller1").ifPresent(seller -> {
                seller.setRoles(sellerRoles);
                userRepository.save(seller);
            });

            userRepository.findByUsername("admin").ifPresent(admin -> {
                admin.setRoles(adminRoles);
                userRepository.save(admin);
            });
        };
    }

}
