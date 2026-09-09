package com.team2.paymentservice.payment.service;

import com.team2.paymentservice.common.exception.CustomException;
import com.team2.paymentservice.common.exception.ErrorCode;
import com.team2.paymentservice.payment.client.FixDealStatusResponse;
import com.team2.paymentservice.payment.client.PostInfoResponse;
import com.team2.paymentservice.payment.client.PostServiceClient;
import com.team2.paymentservice.payment.dto.PaymentRequestDto;
import com.team2.paymentservice.payment.dto.PaymentResponseDto;
import com.team2.paymentservice.payment.entity.Payment;
import com.team2.paymentservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PostServiceClient postServiceClient;

    @Transactional
    public Long createPayment(PaymentRequestDto.Create request, String payerEmail) {
        PostInfoResponse post = postServiceClient.getPost(request.postId());

        if (!post.authorEmail().equals(payerEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_CREATE);
        }

        FixDealStatusResponse fixDeal = postServiceClient.getFixDealStatus(request.postId());
        if (!"REPAIR_DONE".equals(fixDeal.status())) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        if (paymentRepository.existsByPostId(request.postId())) {
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
        }

        Payment payment = Payment.builder()
                .postId(request.postId())
                .payerEmail(payerEmail)
                .payeeEmail(request.payeeEmail())
                .amount(request.amount())
                .build();

        paymentRepository.save(payment);
        return payment.getId();
    }

    public PaymentResponseDto getPayment(Long paymentId, String requesterEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getPayerEmail().equals(requesterEmail) && !payment.getPayeeEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        return PaymentResponseDto.from(payment);
    }

    public PaymentResponseDto getPaymentByPostId(Long postId, String requesterEmail) {
        Payment payment = paymentRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getPayerEmail().equals(requesterEmail) && !payment.getPayeeEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        return PaymentResponseDto.from(payment);
    }
}
