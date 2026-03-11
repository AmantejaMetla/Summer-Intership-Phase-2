package com.eshop.payment.controller;

import com.eshop.payment.entity.Payment;
import com.eshop.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> listByUser(@RequestHeader("X-User-Id") Long userId) {
        return paymentService.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> get(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return paymentService.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a Razorpay order for an order. Stores PENDING payment; returns razorpayOrderId and keyId for frontend checkout.
     * Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET (Test mode keys from Dashboard - no company verification needed).
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> body) {
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        BigDecimal amount = body.get("amount") != null ? new BigDecimal(body.get("amount").toString()) : null;
        if (orderId == null || amount == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orderId and amount required"));
        }
        return paymentService.createOrderForPayment(orderId, userId, amount)
                .map(r -> ResponseEntity.ok(Map.of(
                        "paymentId", r.paymentId(),
                        "razorpayOrderId", r.razorpayOrderId(),
                        "razorpayKeyId", r.razorpayKeyId())))
                .orElse(ResponseEntity.status(503).body(
                        Map.of("error", "Razorpay not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET (Dashboard -> API Keys -> Test mode).")));
    }

    /**
     * Verify Razorpay payment (after checkout). Pass order_id, payment_id, signature from Razorpay callback.
     * On success, payment is COMPLETED and receipt link stored in DB.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> body) {
        Long paymentId = body.get("paymentId") != null ? Long.valueOf(body.get("paymentId").toString()) : null;
        String razorpayOrderId = body.get("razorpayOrderId") != null ? body.get("razorpayOrderId").toString() : null;
        String razorpayPaymentId = body.get("razorpayPaymentId") != null ? body.get("razorpayPaymentId").toString() : null;
        String razorpaySignature = body.get("razorpaySignature") != null ? body.get("razorpaySignature").toString() : null;
        if (paymentId == null || razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "paymentId, razorpayOrderId, razorpayPaymentId, razorpaySignature required"));
        }
        return paymentService.verifyAndCompletePayment(paymentId, userId, razorpayOrderId, razorpayPaymentId, razorpaySignature)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Verification failed or payment not found.")));
    }

    @PostMapping
    public Payment create(@RequestBody Payment payment, @RequestHeader("X-User-Id") Long userId) {
        payment.setUserId(userId);
        return paymentService.create(payment);
    }
}
