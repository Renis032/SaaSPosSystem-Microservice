package com.renko.service.impl;

import com.renko.domain.SubscriptionPlan;
import com.renko.service.BillingService;
import com.renko.service.SubscriptionService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundCreateParams.Reason;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService
{
    private final SubscriptionService subscriptionService;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.currency:usd}")
    private String currency;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${stripe.price.starter:}")
    private String starterPriceId;

    @Value("${stripe.price.pro:}")
    private String proPriceId;

    @PostConstruct
    public void init()
    {
        if(stripeApiKey != null && false == stripeApiKey.isBlank())
        {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe API key configured successfully");
        }
        else
        {
            log.warn("Stripe API key is not configured");
        }
    }

    @Override
    public String createPaymentIntent(long amountCents) throws IllegalAccessException, StripeException
    {
        ensureStripeConfigured();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }

    @Override
    public boolean verifyPayment(String paymentIntentId) throws IllegalAccessException, StripeException
    {
        if(paymentIntentId == null || paymentIntentId.isBlank())
        {
            return false;
        }

        ensureStripeConfigured();
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        return intent.getStatus().equals("succeeded");
    }

    @Override
    public void refundCardPayment(String paymentIntentId, long amountCents, String reason) throws Exception
    {
        ensureStripeConfigured();

        RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(amountCents);

        if(reason != null && false == reason.isBlank())
        {
            Reason r = "duplicate".equalsIgnoreCase(reason) ? Reason.DUPLICATE :
                       "fraudulent".equalsIgnoreCase(reason) ? Reason.FRAUDULENT : Reason.REQUESTED_BY_CUSTOMER;
            paramsBuilder.setReason(r);
        }

        try
        {
            Refund.create(paramsBuilder.build());
        }
        catch(StripeException e)
        {
            log.error("Stripe refund failed: {}", e.getMessage());
            throw new Exception("Stripe refund failed for paymentIntentId=" + paymentIntentId
                    + ", amountCents=" + amountCents
                    + ", reason=" + reason
                    + ": " + e.getMessage());
        }
    }

    @Override
    public Map<String, String> createSubscriptionCheckoutSession(Long storeId,
                                                                 SubscriptionPlan plan,
                                                                 String successUrl,
                                                                 String cancelUrl) throws Exception
    {
        SubscriptionPlan resolved = plan != null ? plan : SubscriptionPlan.STARTER;

        if(stripeApiKey == null || stripeApiKey.isBlank())
        {
            // Local/dev fallback when Stripe is not configured
            subscriptionService.markActive(storeId, "local_customer_" + storeId, "local_sub_" + storeId);
            Map<String, String> local = new LinkedHashMap<>();
            local.put("mode", "local");
            local.put("message", "Stripe not configured; subscription marked ACTIVE locally");
            local.put("storeId", String.valueOf(storeId));
            local.put("plan", resolved.name());
            return local;
        }

        String priceId = resolved == SubscriptionPlan.PRO ? proPriceId : starterPriceId;
        if(priceId == null || priceId.isBlank())
        {
            throw new IllegalAccessException(
                    "Stripe price id missing. Set stripe.price.starter / stripe.price.pro"
            );
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl != null ? successUrl : "http://localhost:5173/admin/billing?success=1")
                .setCancelUrl(cancelUrl != null ? cancelUrl : "http://localhost:5173/admin/billing?canceled=1")
                .putMetadata("storeId", String.valueOf(storeId))
                .putMetadata("plan", resolved.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(priceId)
                        .build())
                .build();

        Session session = Session.create(params);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("mode", "stripe");
        body.put("sessionId", session.getId());
        body.put("url", session.getUrl());
        return body;
    }

    @Override
    public void handleStripeWebhook(String payload, String signatureHeader) throws Exception
    {
        if(webhookSecret == null || webhookSecret.isBlank())
        {
            log.warn("Stripe webhook secret not configured; ignoring webhook");
            return;
        }

        Event event;
        try
        {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        }
        catch(SignatureVerificationException e)
        {
            throw new IllegalAccessException("Invalid Stripe webhook signature");
        }

        if("checkout.session.completed".equals(event.getType()))
        {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);
            if(session == null)
            {
                return;
            }

            String storeIdRaw = session.getMetadata() != null ? session.getMetadata().get("storeId") : null;
            if(storeIdRaw == null)
            {
                return;
            }

            Long storeId = Long.valueOf(storeIdRaw);
            String customerId = session.getCustomer();
            String subscriptionId = session.getSubscription();
            subscriptionService.activateFromStripeWebhook(storeId, customerId, subscriptionId);
            log.info("Activated subscription for storeId={} via Stripe checkout", storeId);
        }
    }

    private void ensureStripeConfigured() throws IllegalAccessException
    {
        if(stripeApiKey == null || stripeApiKey.isBlank())
        {
            throw new IllegalAccessException(
                    "Stripe secret key is not configured. Set stripe.api.key before using billing endpoints."
            );
        }
    }
}
