package com.renko.service;

import com.renko.domain.SubscriptionPlan;
import com.stripe.exception.StripeException;

import java.util.Map;

public interface BillingService
{
    String createPaymentIntent(long amountCents) throws IllegalAccessException, StripeException;

    boolean verifyPayment(String paymentIntentId) throws IllegalAccessException, StripeException;

    void refundCardPayment(String paymentIntentId, long amountCents, String reason) throws Exception;

    Map<String, String> createSubscriptionCheckoutSession(Long storeId,
                                                          SubscriptionPlan plan,
                                                          String successUrl,
                                                          String cancelUrl) throws Exception;

    void handleStripeWebhook(String payload, String signatureHeader) throws Exception;
}
