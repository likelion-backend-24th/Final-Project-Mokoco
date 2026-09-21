package com.team2.paymentservice;

import com.team2.common.security.LoginUser;
import com.team2.paymentservice.config.TokenAuthenticationFilter;
import com.team2.paymentservice.payment.controller.PaymentController;
import com.team2.paymentservice.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PaymentAuthenticationTest {
    @Test void identityHeaderCannotAuthenticateAndVerifiedPrincipalOverridesSpoofing() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://users").defaultHeader("X-Internal-Service-Key", "test-key");
        MockRestServiceServer upstream = MockRestServiceServer.bindTo(builder).build();
        PaymentService service = mock(PaymentService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PaymentController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(new TokenAuthenticationFilter(builder.build())).build();
        mvc.perform(get("/payments/post/1").header("X-User-Email", "victim@test")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        upstream.expect(requestTo("http://users/api/internal/users/verify-token"))
                .andExpect(header("X-Internal-Service-Key", "test-key"))
                .andExpect(content().string("valid-token"))
                .andRespond(withSuccess("{\"id\":10,\"email\":\"actual@test\"}", MediaType.APPLICATION_JSON));
        mvc.perform(get("/payments/post/1").header("Authorization", "Bearer valid-token")
                .header("X-User-Email", "victim@test")).andExpect(status().isOk());
        verify(service).getPaymentByPostId(1L, "actual@test");
        upstream.verify();
    }
    @Test void invalidTokenIs401AndInternalKeyFailureIs502() throws Exception {
        for (boolean internal : new boolean[]{false, true}) {
            RestClient.Builder builder = RestClient.builder().baseUrl("http://users");
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            org.springframework.test.web.client.response.DefaultResponseCreator rejection =
                    withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED);
            if (internal) rejection.header("X-Internal-Auth-Error", "UNAUTHORIZED_INTERNAL_SERVICE");
            server.expect(requestTo("http://users/api/internal/users/verify-token")).andRespond(rejection);
            org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer rejected");
            org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
            new TokenAuthenticationFilter(builder.build()).doFilter(request, response, (incoming, outgoing) -> {
                throw new AssertionError("Invalid authentication reached controller");
            });
            org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(internal ? 502 : 401);
            server.verify();
        }
    }

}
