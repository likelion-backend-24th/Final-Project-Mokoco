package com.team2.postservice.client;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UserClientConfigTest {
    @Test
    void attachesInternalKey() {
        var template = new RequestTemplate();
        new UserClientConfig().internalServiceAuthentication("test-key").apply(template);
        assertThat(template.headers().get("X-Internal-Service-Key")).containsExactly("test-key");
    }

    @Test
    void refusesBlankKey() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new UserClientConfig().internalServiceAuthentication(" "));
    }
}
