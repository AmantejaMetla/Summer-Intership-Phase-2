package com.eshop.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Razorpay integration: create order (INR/paise), verify payment signature.
 * Supports Indian payments; test mode does not require company verification.
 */
@Service
public class RazorpayService {

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    private RazorpayClient client() throws Exception {
        return new RazorpayClient(keyId, keySecret);
    }

    /**
     * Create a Razorpay order. Amount in paise (e.g. 100 = 1 INR).
     * Returns Razorpay order id for use in checkout.
     */
    public String createOrder(long amountPaise, String receiptId) throws Exception {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountPaise);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receiptId != null ? receiptId : "rcpt_" + System.currentTimeMillis());
        Order order = client().orders.create(orderRequest);
        return order.get("id");
    }

    /**
     * Verify payment signature from Razorpay Checkout (frontend sends order_id, payment_id, signature).
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject params = new JSONObject();
            params.put("razorpay_order_id", orderId);
            params.put("razorpay_payment_id", paymentId);
            params.put("razorpay_signature", signature);
            Utils.verifyPaymentSignature(params, keySecret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
