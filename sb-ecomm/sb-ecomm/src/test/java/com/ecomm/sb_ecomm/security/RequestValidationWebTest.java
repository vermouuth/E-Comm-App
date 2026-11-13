package com.ecomm.sb_ecomm.security;

import com.ecomm.sb_ecomm.auth.controllers.AuthController;
import com.ecomm.sb_ecomm.auth.jwt.AuthEntryPoint;
import com.ecomm.sb_ecomm.auth.jwt.JwtUtils;
import com.ecomm.sb_ecomm.auth.model.AppRole;
import com.ecomm.sb_ecomm.auth.model.Role;
import com.ecomm.sb_ecomm.auth.repository.RoleRepository;
import com.ecomm.sb_ecomm.auth.repository.UserRepository;
import com.ecomm.sb_ecomm.auth.services.UserDetailsServicesImpl;
import com.ecomm.sb_ecomm.auth.WebSecurityConfig;
import com.ecomm.sb_ecomm.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        properties = "spring.jackson.deserialization.fail-on-unknown-properties=true")
@Import({WebSecurityConfig.class, JwtUtils.class, AuthEntryPoint.class, GlobalExceptionHandler.class})
class RequestValidationWebTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserDetailsServicesImpl userDetailsServices;
    @MockitoBean UserRepository userRepository;
    @MockitoBean RoleRepository roleRepository;

    private ResultActions signup(String json) throws Exception {
        return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void missingField_isListedUnderMissingFields() throws Exception {
        signup("{\"username\":\"mallory\",\"email\":\"mallory@example.com\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is invalid, please fix and resend"))
                .andExpect(jsonPath("$.missingFields", contains("password")))
                .andExpect(jsonPath("$.path").value("/api/auth/signup"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void ruleViolation_isListedUnderInvalidFields() throws Exception {
        signup("{\"username\":\"mallory\",\"password\":\"short\",\"email\":\"mallory@example.com\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields[0].field").value("password"))
                .andExpect(jsonPath("$.invalidFields[0].received").value("short"))
                .andExpect(jsonPath("$.invalidFields[0].problem").value("password must be 8-50 characters"));
    }

    @Test
    void wrongType_namesFieldAndExpectedType() throws Exception {
        signup("{\"username\":123,\"password\":[1],\"email\":\"mallory@example.com\"}")
                .andExpect(status().isBadRequest());
        signup("{\"username\":{\"a\":1},\"password\":\"password123\",\"email\":\"m@example.com\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Wrong data type"))
                .andExpect(jsonPath("$.invalidFields[0].field").value("username"));
    }

    @Test
    void unknownField_listsAllowedFields() throws Exception {
        signup("{\"username\":\"mallory\",\"password\":\"password123\",\"email\":\"mallory@example.com\",\"nickname\":\"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown field 'nickname'"))
                .andExpect(jsonPath("$.allowedFields", hasItems("username", "password", "email")));
    }

    @Test
    void malformedJson_isReportedAsSuch() throws Exception {
        signup("{\"username\":\"mallory\",")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON, check commas, quotes and brackets"));
    }

    @Test
    void validRequest_succeeds_andResponseHasNoPassword() throws Exception {
        when(roleRepository.findByRoleName(AppRole.ROLE_USER)).thenReturn(Optional.of(new Role(AppRole.ROLE_USER)));
        signup("{\"username\":\" mallory \",\"password\":\"password123\",\"email\":\"Mallory@Example.com\"}")
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("password"))));
    }
}
