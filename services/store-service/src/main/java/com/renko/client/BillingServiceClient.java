package com.renko.client;

import com.renko.domain.SubscriptionPlan;
import com.renko.payload.dto.SubscriptionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class BillingServiceClient
{
    @Qualifier("billingRestClient")
    private final RestClient billingRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;

    public SubscriptionDto createTrial(Long storeId, SubscriptionPlan plan)
    {
        var req = billingRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/billing/subscription/{storeId}/trial")
                        .queryParam("plan", plan != null ? plan.name() : SubscriptionPlan.STARTER.name())
                        .build(storeId));
        String auth = authHeaderForwarder.currentAuthorizationHeader();
        if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
        return req.retrieve().body(SubscriptionDto.class);
    }
}
