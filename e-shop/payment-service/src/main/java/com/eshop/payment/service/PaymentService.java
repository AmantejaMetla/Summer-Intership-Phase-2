package com.eshop.payment.service;

import com.eshop.payment.config.RazorpayConfig;
import com.eshop.payment.entity.Payment;
import com.eshop.payment.entity.Payment.PaymentStatus;
import com.eshop.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final RazorpayConfig razorpayConfig;

    public PaymentService(PaymentRepository paymentRepository, RazorpayService razorpayService, RazorpayConfig razorpayConfig) {
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
        this.razorpayConfig = razorpayConfig;
    }

    public List<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> findByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment create(Payment payment) {
        return paymentRepository.save(payment);
    }

    /**
     * Create a Razorpay order for an order and store a PENDING payment.
     * Amount in INR; converted to paise for Razorpay. Returns Razorpay order_id for frontend checkout.
     */
    @Transactional
    public Optional<CreateOrderResult> createOrderForPayment(Long orderId, Long userId, BigDecimal amountInr) {
        if (!razorpayConfig.isConfigured()) {
            return Optional.empty();
        }
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amountInr);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod("RAZORPAY");
        payment = paymentRepository.save(payment);
        try {
            long amountPaise = amountInr.multiply(BigDecimal.valueOf(100)).longValue();
            if (amountPaise <= 0) amountPaise = 100;
            String receiptId = "pay_" + payment.getId();
            String razorpayOrderId = razorpayService.createOrder(amountPaise, receiptId);
            payment.setGatewayOrderId(razorpayOrderId);
            paymentRepository.save(payment);
            return Optional.of(new CreateOrderResult(payment.getId(), razorpayOrderId, razorpayConfig.getKeyId()));
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return Optional.empty();
        }
    }

    /**
     * Verify Razorpay signature and mark payment COMPLETED. Store gateway_payment_id and optional receipt.
     */
    @Transactional
    public Optional<Payment> verifyAndCompletePayment(Long paymentId, Long userId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Optional<Payment> opt = paymentRepository.findById(paymentId);
        if (opt.isEmpty() || !opt.get().getUserId().equals(userId) || opt.get().getStatus() != PaymentStatus.PENDING) {
            return Optional.empty();
        }
        Payment payment = opt.get();
        if (!razorpayOrderId.equals(payment.getGatewayOrderId())) {
            return Optional.empty();
        }
        if (!razorpayService.verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return Optional.empty();
        }
        payment.setGatewayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setReceiptUrl("https://dashboard.razorpay.com/app/payments/" + razorpayPaymentId);
        return Optional.of(paymentRepository.save(payment));
    }

    public boolean isRazorpayConfigured() {
        return razorpayConfig.isConfigured();
    }

    public record CreateOrderResult(long paymentId, String razorpayOrderId, String razorpayKeyId) {}
}
