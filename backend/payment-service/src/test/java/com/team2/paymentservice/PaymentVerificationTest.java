package com.team2.paymentservice;

import com.team2.common.payment.PaymentContext;
import com.team2.common.security.LoginUser;
import com.team2.paymentservice.common.exception.CustomException;
import com.team2.paymentservice.payment.client.*;
import com.team2.paymentservice.payment.dto.PaymentRequestDto;
import com.team2.paymentservice.payment.entity.PaymentOrder;
import com.team2.paymentservice.payment.repository.*;
import com.team2.paymentservice.payment.service.PaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import(PaymentService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentVerificationTest extends FlywaySchemaTest {
    @Autowired PaymentService service;
    @Autowired PaymentRepository payments;
    @Autowired PaymentOrderRepository orders;
    @MockitoBean PostServiceClient posts;
    @MockitoBean PortOnePaymentClient portOne;
    static final AtomicLong IDS = new AtomicLong(100);
    final LoginUser owner = new LoginUser(10L, com.team2.common.security.Role.USER);
    PaymentContext context;

    @BeforeEach void setup() {
        Long postId = IDS.incrementAndGet();
        context = new PaymentContext(postId, postId + 1000, 10L, 20L, 10000, "REPAIR_DONE");
        when(posts.getPaymentContext(postId)).thenReturn(context);
    }
    PaymentRequestDto.Create request(PaymentOrder order) {
        return new PaymentRequestDto.Create(order.getPostId(), null, null, null, order.getPaymentId());
    }
    PortOnePaymentResponse paid(PaymentOrder order) {
        return new PortOnePaymentResponse(order.getPaymentId(), "PAID", new PortOnePaymentResponse.Amount(11000), "KRW", "forged customData is ignored");
    }

    @Test void serverOrderDeterminesMoneyAndRecipientAndRetryReturnsSamePayment() {
        PaymentOrder order = service.prepare(context.postId(), owner.userId());
        assertThat(service.prepare(context.postId(), owner.userId()).getPaymentId()).isEqualTo(order.getPaymentId());
        when(portOne.getPayment(order.getPaymentId())).thenReturn(paid(order));
        Long id = service.createPayment(request(order), owner.userId());
        assertThat(service.createPayment(request(order), owner.userId())).isEqualTo(id);
        assertThat(payments.findById(id).orElseThrow().getPayeeId()).isEqualTo(20L);
        assertThat(payments.findById(id).orElseThrow().getAmount()).isEqualTo(11000);
        assertThat(payments.findById(id).orElseThrow().getNetAmount()).isEqualTo(10000);
        verify(portOne, times(1)).getPayment(order.getPaymentId());
    }

    @Test void rejectsImpersonationAndTamperedAmountRecipientOrPost() {
        assertThatThrownBy(() -> service.prepare(context.postId(), 99L)).isInstanceOf(CustomException.class);
        PaymentOrder order = service.prepare(context.postId(), owner.userId());
        assertThatThrownBy(() -> service.createPayment(request(order), 99L)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.createPayment(new PaymentRequestDto.Create(context.postId(), 99L, 11000, 10000, order.getPaymentId()), owner.userId())).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.createPayment(new PaymentRequestDto.Create(context.postId(), null, 1, 1, order.getPaymentId()), owner.userId())).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.createPayment(new PaymentRequestDto.Create(999L, null, null, null, order.getPaymentId()), owner.userId())).isInstanceOf(CustomException.class);
        verifyNoInteractions(portOne);
        assertThat(payments.findByPostId(context.postId())).isEmpty();
    }

    @Test void webhookUsesSameProviderAmountCurrencyAndOrderValidation() {
        PaymentOrder order = service.prepare(context.postId(), owner.userId());
        when(portOne.getPayment(order.getPaymentId())).thenReturn(new PortOnePaymentResponse(order.getPaymentId(), "PAID", new PortOnePaymentResponse.Amount(1), "KRW", "{}"));
        assertThatThrownBy(() -> service.handleWebhookPayment(order.getPaymentId())).isInstanceOf(CustomException.class);
        when(portOne.getPayment(order.getPaymentId())).thenReturn(new PortOnePaymentResponse(order.getPaymentId(), "PAID", new PortOnePaymentResponse.Amount(11000), "USD", "{}"));
        assertThatThrownBy(() -> service.handleWebhookPayment(order.getPaymentId())).isInstanceOf(CustomException.class);
        when(portOne.getPayment(order.getPaymentId())).thenReturn(new PortOnePaymentResponse("other-id", "PAID", new PortOnePaymentResponse.Amount(11000), "KRW", "{}"));
        assertThatThrownBy(() -> service.handleWebhookPayment(order.getPaymentId())).isInstanceOf(CustomException.class);
        assertThat(payments.findByPostId(context.postId())).isEmpty();
        when(portOne.getPayment(order.getPaymentId())).thenReturn(paid(order));
        service.handleWebhookPayment(order.getPaymentId());
        Long id = service.createPayment(request(order), owner.userId());
        service.handleWebhookPayment(order.getPaymentId());
        assertThat(payments.findByPostId(context.postId()).orElseThrow().getId()).isEqualTo(id);
    }

    @Test void ignoresUnknownWebhookInsteadOfTrustingItsCustomData() {
        service.handleWebhookPayment("unprepared-payment");
        verifyNoInteractions(portOne);
        assertThat(payments.findByPostId(context.postId())).isEmpty();
    }

    @Test void rejectsChangedDealAndUnpaidProviderState() {
        PaymentOrder order = service.prepare(context.postId(), owner.userId());
        when(portOne.getPayment(order.getPaymentId())).thenReturn(new PortOnePaymentResponse(order.getPaymentId(), "READY", new PortOnePaymentResponse.Amount(11000), "KRW", null));
        assertThatThrownBy(() -> service.createPayment(request(order), owner.userId())).isInstanceOf(CustomException.class);
        when(portOne.getPayment(order.getPaymentId())).thenReturn(paid(order));
        when(posts.getPaymentContext(context.postId())).thenReturn(new PaymentContext(context.postId(), 999L, 10L, 20L, 10000, "REPAIR_DONE"));
        assertThatThrownBy(() -> service.createPayment(request(order), owner.userId())).isInstanceOf(CustomException.class);
        assertThat(payments.findByPostId(context.postId())).isEmpty();
    }

    @Test void frontendConfirmationRacingWebhookCreatesOnePayment() throws Exception {
        PaymentOrder order = service.prepare(context.postId(), owner.userId());
        CyclicBarrier providerBarrier = new CyclicBarrier(2);
        when(portOne.getPayment(order.getPaymentId())).thenAnswer(invocation -> {
            providerBarrier.await(10, TimeUnit.SECONDS); return paid(order);
        });
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Long> frontend = pool.submit(() -> service.createPayment(request(order), owner.userId()));
            Future<?> webhook = pool.submit(() -> service.handleWebhookPayment(order.getPaymentId()));
            Long id = frontend.get(20, TimeUnit.SECONDS);
            webhook.get(20, TimeUnit.SECONDS);
            assertThat(payments.findByPostId(context.postId()).orElseThrow().getId()).isEqualTo(id);
            assertThat(payments.findByPortonePaymentId(order.getPaymentId())).isPresent();
        }
    }

    @Test void concurrentPreparationReturnsOneServerPaymentId() throws Exception {
        CyclicBarrier ready = new CyclicBarrier(2);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<String> prepare = () -> { ready.await(10, TimeUnit.SECONDS); return service.prepare(context.postId(), owner.userId()).getPaymentId(); };
            Future<String> first = pool.submit(prepare);
            Future<String> second = pool.submit(prepare);
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(second.get(20, TimeUnit.SECONDS));
        }
    }
    @Test void feeRoundingIsExactAndOverflowCannotProduceACharge() {
        assertThat(com.team2.paymentservice.payment.entity.Payment.calculateTotalAmount(15)).isEqualTo(17);
        assertThatThrownBy(() -> com.team2.paymentservice.payment.entity.Payment.calculateTotalAmount(Integer.MAX_VALUE))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> com.team2.paymentservice.payment.entity.Payment.calculateTotalAmount(0))
                .isInstanceOf(CustomException.class);
    }

}
