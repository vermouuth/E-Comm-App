package com.ecomm.sb_ecomm.security;

import com.ecomm.sb_ecomm.auth.payload.request.SignupUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppConfigs registers a plain ObjectMapper (FAIL_ON_UNKNOWN_PROPERTIES = true), which replaces
 * Spring Boot's lenient one. Removing "roles" from the DTO must not turn old clients that still
 * send it into 400s - the field should simply be ignored.
 */
class SignupUserJsonTest {

    @Test
    void legacyRolesField_isIgnored_evenWithAStrictObjectMapper() throws Exception {
        ObjectMapper strict = new ObjectMapper();

        SignupUser parsed = strict.readValue("""
                {"username":"mallory","password":"password123",
                 "email":"mallory@example.com","roles":["admin"]}
                """, SignupUser.class);

        assertThat(parsed.getUsername()).isEqualTo("mallory");
    }
}
