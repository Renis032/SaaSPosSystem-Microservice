package com.renko.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BillingServiceClient
{
    @Qualifier("billingRestClient")
    private final RestClient billingRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;

    public void requireActiveSubscription(Long storeId)
    {
        var req = billingRestClient.get().uri("/api/billing/subscription/{storeId}/require-active", storeId);
        String header = authHeaderForwarder.currentAuthorizationHeader();
        if(header != null) req = req.header(HttpHeaders.AUTHORIZATION, header);
        req.retrieve().toBodilessEntity();
    }

    public boolean verifyPayment(String paymentIntentId)
    {
        var req = billingRestClient.post()
                .uri("/api/billing/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("paymentIntentId", paymentIntentId));
        String header = authHeaderForwarder.currentAuthorizationHeader();
        if(header != null) req = req.header(HttpHeaders.AUTHORIZATION, header);
        Map<String, Object> body = req.retrieve().body(new ParameterizedTypeReference<>() {});
        return body != null && Boolean.TRUE.equals(body.get("verified"));
    }
}
