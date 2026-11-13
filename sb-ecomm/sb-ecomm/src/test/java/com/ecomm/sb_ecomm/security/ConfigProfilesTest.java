package com.ecomm.sb_ecomm.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Loads the REAL src/main/resources config (not the test copy) to prove which secrets and
 * dangerous features each profile gets.
 */
class ConfigProfilesTest {

    // The old hand-typed dev secret started with this; the replacement is random.
    private static final String OLD_SECRET_PREFIX = "mySecretKey";

    // application*.properties are gitignored, so a fresh clone has nothing to check: skip, don't fail.
    @BeforeAll
    static void configFilesArePresent() {
        assumeTrue(Files.exists(Path.of("src/main/resources/application.properties"))
                        && Files.exists(Path.of("src/main/resources/application-dev.properties")),
                "local application*.properties not present");
    }

    @Configuration
    static class Empty {}

    private ConfigurableApplicationContext start(String... args) {
        String location = Path.of("src/main/resources").toAbsolutePath().toUri().toString();
        String[] all = Arrays.copyOf(args, args.length + 1);
        all[args.length] = "--spring.config.location=" + location;
        return new SpringApplicationBuilder(Empty.class)
                .web(WebApplicationType.NONE)
                .run(all);
    }

    @Test
    void defaultRun_usesTheDevProfile_withItsOwnThrowawaySecret() {
        try (ConfigurableApplicationContext ctx = start()) {
            Environment env = ctx.getEnvironment();

            assertThat(env.getDefaultProfiles()).contains("dev");
            String secret = env.getProperty("spring.app.jwtSecret");
            assertThat(secret).hasSizeGreaterThanOrEqualTo(43).doesNotStartWith(OLD_SECRET_PREFIX);
            assertThat(env.getProperty("spring.app.ecom.cookieSecure")).isEqualTo("false");
            assertThat(env.getProperty("mock.gateway.enabled")).isEqualTo("true");
            assertThat(env.getProperty("app.seed-data.enabled")).isEqualTo("true");
        }
    }

    @Test
    void otherProfiles_getNoSecretsFromTheRepo_andStartLocked() {
        try (ConfigurableApplicationContext ctx = start("--spring.profiles.active=prod")) {
            Environment env = ctx.getEnvironment();

            assertThatThrownBy(() -> env.getProperty("spring.app.jwtSecret"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JWT_SECRET");
            assertThatThrownBy(() -> env.getProperty("spring.datasource.password"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("DB_PASSWORD");
            assertThatThrownBy(() -> env.getProperty("mock.gateway.signing-secret"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(env.getProperty("spring.app.ecom.cookieSecure")).isEqualTo("true");
            assertThat(env.getProperty("mock.gateway.enabled")).isEqualTo("false");
            assertThat(env.getProperty("app.seed-data.enabled")).isEqualTo("false");
            assertThat(env.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
            assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        }
    }

    @Test
    void otherProfiles_takeSecretsFromTheEnvironment() {
        try (ConfigurableApplicationContext ctx = start(
                "--spring.profiles.active=prod", "--JWT_SECRET=from-the-environment")) {
            assertThat(ctx.getEnvironment().getProperty("spring.app.jwtSecret"))
                    .isEqualTo("from-the-environment");
        }
    }
}
