package com.renko.client;

import com.renko.payload.dto.InventoryDto;
import com.renko.payload.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CatalogServiceClient
{
    @Qualifier("catalogRestClient")
    private final RestClient catalogRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;

    private <S extends RestClient.RequestHeadersSpec<?>> S auth(S spec)
    {
        String header = authHeaderForwarder.currentAuthorizationHeader();
        if(header != null) spec.header(HttpHeaders.AUTHORIZATION, header);
        return spec;
    }

    public ProductDto getProduct(Long productId)
    {
        var req = catalogRestClient.get().uri("/api/products/{id}", productId);
        String header = authHeaderForwarder.currentAuthorizationHeader();
        if(header != null) req = req.header(HttpHeaders.AUTHORIZATION, header);
        return req.retrieve().body(ProductDto.class);
    }

    public InventoryDto deduct(Long storeId, Long productId, int quantity)
    {
        var req = catalogRestClient.post()
                .uri("/api/inventories/store/{storeId}/product/{productId}/deduct?quantity={q}",
                        storeId, productId, quantity);
        String header = authHeaderForwarder.currentAuthorizationHeader();
        if(header != null) req = req.header(HttpHeaders.AUTHORIZATION, header);
        return req.retrieve().body(InventoryDto.class);
    }
}
