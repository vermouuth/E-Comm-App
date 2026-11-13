package com.ecomm.sb_ecomm.security;

import com.ecomm.sb_ecomm.auth.WebSecurityConfig;
import com.ecomm.sb_ecomm.auth.controllers.AuthController;
import com.ecomm.sb_ecomm.auth.jwt.AuthEntryPoint;
import com.ecomm.sb_ecomm.auth.jwt.JwtUtils;
import com.ecomm.sb_ecomm.auth.model.AppRole;
import com.ecomm.sb_ecomm.auth.model.Role;
import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.auth.repository.RoleRepository;
import com.ecomm.sb_ecomm.auth.repository.UserRepository;
import com.ecomm.sb_ecomm.auth.services.UserDetailsImpl;
import com.ecomm.sb_ecomm.auth.services.UserDetailsServicesImpl;
import com.ecomm.sb_ecomm.gateway.MockGatewayController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real security filter chain (WebSecurityConfig + JwtUtils + the JWT filter)
 * with the persistence layer mocked, so no database / Docker is needed.
 */
@WebMvcTest(
        controllers = {AuthController.class, MockGatewayController.class},
        properties = "mock.gateway.enabled=true")
@Import({WebSecurityConfig.class, JwtUtils.class, AuthEntryPoint.class})
class SecurityHardeningWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtils jwtUtils;

    @MockitoBean UserDetailsServicesImpl userDetailsServices;
    @MockitoBean UserRepository userRepository;
    @MockitoBean RoleRepository roleRepository;

    @Value("${spring.app.ecom.jwtCookeName}")
    String jwtCookieName;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void stubUsers() {
        when(userDetailsServices.loadUserByUsername("user1"))
                .thenReturn(principal("user1", "ROLE_USER"));
        when(userDetailsServices.loadUserByUsername("admin1"))
                .thenReturn(principal("admin1", "ROLE_ADMIN"));
    }

    private UserDetailsImpl principal(String username, String role) {
        return new UserDetailsImpl(1L, username, username + "@example.com",
                encoder.encode("password1"), List.of(new SimpleGrantedAuthority(role)));
    }

    private Cookie authCookieFor(String username) {
        return new Cookie(jwtCookieName, jwtUtils.generateTokenFromUsername(username));
    }

    // ---------------------------------------------------------------- signup (Bug 1)

    @Test
    void signup_ignoresRequestedRoles_andAlwaysCreatesPlainUser() throws Exception {
        when(roleRepository.findByRoleName(AppRole.ROLE_USER)).thenReturn(Optional.of(new Role(AppRole.ROLE_USER)));
        when(roleRepository.findByRoleName(AppRole.ROLE_ADMIN)).thenReturn(Optional.of(new Role(AppRole.ROLE_ADMIN)));
        when(roleRepository.findByRoleName(AppRole.ROLE_SELLER)).thenReturn(Optional.of(new Role(AppRole.ROLE_SELLER)));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"mallory","password":"password123",
                                 "email":"mallory@example.com","roles":["admin","seller"]}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Users> saved = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).save(saved.capture());
        Set<AppRole> granted = saved.getValue().getRoles().stream()
                .map(Role::getRoleName).collect(Collectors.toSet());
        assertThat(granted).containsExactly(AppRole.ROLE_USER);
    }

    @Test
    void signup_enforcesPasswordRules() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"mallory","password":"short","email":"mallory@example.com"}
                                """))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------ cookie hardening (Bugs 3, 7)

    @Test
    void signin_setsHardenedCookie_andDoesNotLeakTokenInBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"password\":\"password1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly").contains("Secure").contains("SameSite=Lax");

        String token = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        assertThat(token).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(token);
    }

    @Test
    void signout_expiresTheCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signout"))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("Max-Age=0").contains("HttpOnly");
    }

    // ------------------------------------------------------ route protection

    @Test
    void currentUserEndpoint_withoutCookie_is401_notAServerError() throws Exception {
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserEndpoint_withValidCookie_returnsTheUser() throws Exception {
        mockMvc.perform(get("/api/auth/user").cookie(authCookieFor("user1")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("user1")));
    }

    @Test
    void tamperedCookie_isTreatedAsAnonymous() throws Exception {
        Cookie tampered = authCookieFor("user1");
        tampered.setValue(tampered.getValue() + "x");

        mockMvc.perform(get("/api/auth/user").cookie(tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRole_isForbiddenFromAdminRoutes_withAJsonBody() throws Exception {
        mockMvc.perform(get("/api/admin/anything").cookie(authCookieFor("user1")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void anonymous_isRejectedFromAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openDevToolingPaths_areNoLongerPublic() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/test/anything")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------- mock gateway (Bug 2)

    @Test
    void mockGateway_rejectsAnonymousCallers() throws Exception {
        mockMvc.perform(post("/api/mock-gateway/complete/mock_ref_123").param("outcome", "CAPTURED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mockGateway_rejectsNonAdminUsers() throws Exception {
        mockMvc.perform(post("/api/mock-gateway/complete/mock_ref_123")
                        .param("outcome", "CAPTURED")
                        .cookie(authCookieFor("user1")))
                .andExpect(status().isForbidden());
    }
}
