package com.renko.controller;

import com.renko.domain.SubscriptionPlan;
import com.renko.payload.dto.SubscriptionDto;
import com.renko.service.BillingService;
import com.renko.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billing")
public class BillingController
{
    private final BillingService billingService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody Map<String, Long> body)
    {
        Long amountCents = body != null ? body.get("amountCents") : null;
        if(null == amountCents || amountCents <= 0)
        {
            return ResponseEntity.badRequest().build();
        }

        try
        {
            String clientSecret = billingService.createPaymentIntent(amountCents);
            return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
        }
        catch(Exception e)
        {
            return ResponseEntity.internalServerError().body(Map.of(
                    "Error",
                    e.getMessage() != null ? e.getMessage() : "Failed to create payment intent"
            ));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, String> body)
    {
        String paymentIntentId = body != null ? body.get("paymentIntentId") : null;
        if(paymentIntentId == null || paymentIntentId.isBlank())
        {
            return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId is required"));
        }

        try
        {
            boolean verified = billingService.verifyPayment(paymentIntentId);
            return ResponseEntity.ok(Map.of("paymentIntentId", paymentIntentId, "verified", verified));
        }
        catch(Exception e)
        {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Payment verification failed"
            ));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<Map<String, String>> refund(@RequestBody Map<String, Object> body)
    {
        String paymentIntentId = body != null && body.get("paymentIntentId") != null
                ? body.get("paymentIntentId").toString()
                : null;
        Number amount = body != null && body.get("amountCents") != null ? (Number) body.get("amountCents") : null;
        String reason = body != null && body.get("reason") != null ? body.get("reason").toString() : null;

        if(paymentIntentId == null || paymentIntentId.isBlank() || amount == null || amount.longValue() <= 0)
        {
            return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId and amountCents are required"));
        }

        long amountCents = amount.longValue();
        try
        {
            billingService.refundCardPayment(paymentIntentId, amountCents, reason);
            return ResponseEntity.ok(Map.of("message", "Refund initiated successfully"));
        }
        catch(Exception e)
        {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error",
                    e.getMessage() != null ? e.getMessage() : "Refund failed"
            ));
        }
    }

    @GetMapping("/subscription/{storeId}")
    @PreAuthorize("hasAnyRole('OWNER','STORE_MANAGER','ADMIN','CASHIER','BRANCH_MANAGER')")
    public ResponseEntity<SubscriptionDto> getSubscription(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(subscriptionService.getByStoreId(storeId));
    }

    @PostMapping("/subscription/{storeId}/trial")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STORE_MANAGER')")
    public ResponseEntity<SubscriptionDto> createTrial(@PathVariable Long storeId,
                                                       @RequestParam(required = false) SubscriptionPlan plan) throws Exception
    {
        return ResponseEntity.ok(subscriptionService.createTrialForNewStore(storeId, plan));
    }

    @GetMapping("/subscription/{storeId}/require-active")
    public ResponseEntity<Map<String, Object>> requireActive(@PathVariable Long storeId) throws Exception
    {
        subscriptionService.requireActiveSubscription(storeId);
        return ResponseEntity.ok(Map.of("storeId", storeId, "active", true));
    }

    @PostMapping("/subscription/activate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<SubscriptionDto> activateSubscription(@RequestBody Map<String, Object> body) throws Exception
    {
        if(body == null || body.get("storeId") == null)
        {
            return ResponseEntity.badRequest().build();
        }

        Long storeId = ((Number) body.get("storeId")).longValue();
        SubscriptionPlan plan = SubscriptionPlan.STARTER;
        if(body.get("plan") != null)
        {
            plan = SubscriptionPlan.valueOf(body.get("plan").toString());
        }

        return ResponseEntity.ok(subscriptionService.activateTrial(storeId, plan));
    }

    @PostMapping("/subscription/checkout")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Map<String, String>> checkoutSubscription(@RequestBody Map<String, Object> body) throws Exception
    {
        if(body == null || body.get("storeId") == null)
        {
            return ResponseEntity.badRequest().body(Map.of("error", "storeId is required"));
        }

        Long storeId = ((Number) body.get("storeId")).longValue();
        SubscriptionPlan plan = SubscriptionPlan.STARTER;
        if(body.get("plan") != null)
        {
            plan = SubscriptionPlan.valueOf(body.get("plan").toString());
        }
        String successUrl = body.get("successUrl") != null ? body.get("successUrl").toString() : null;
        String cancelUrl = body.get("cancelUrl") != null ? body.get("cancelUrl").toString() : null;

        return ResponseEntity.ok(billingService.createSubscriptionCheckoutSession(storeId, plan, successUrl, cancelUrl));
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Map<String, String>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) throws Exception
    {
        billingService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("received", "true"));
    }
}
