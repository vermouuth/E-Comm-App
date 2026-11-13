package com.ecomm.sb_ecomm.security;

import com.ecomm.sb_ecomm.gateway.MockGatewayController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mock gateway can forge "payment captured" webhooks, so it must not exist
 * unless someone deliberately switches it on.
 */
class MockGatewayGatingTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(MockGatewayController.class)
            .withPropertyValues("mock.gateway.signing-secret=test-secret", "server.port=8080");

    @Test
    void controllerIsAbsentByDefault() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(MockGatewayController.class));
    }

    @Test
    void controllerIsAbsentWhenExplicitlyDisabled() {
        runner.withPropertyValues("mock.gateway.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(MockGatewayController.class));
    }

    @Test
    void controllerExistsOnlyWhenEnabled() {
        runner.withPropertyValues("mock.gateway.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(MockGatewayController.class));
    }
}
