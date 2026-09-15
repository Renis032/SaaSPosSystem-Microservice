package com.renko.client;

import com.renko.payload.dto.InventoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogServiceClient
{
    @Qualifier("catalogRestClient")
    private final RestClient catalogRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;

    public List<InventoryDto> lowStock(Long storeId)
    {
        try
        {
            var req = catalogRestClient.get().uri("/api/inventories/store/{storeId}/low-stock", storeId);
            String a = authHeaderForwarder.currentAuthorizationHeader();
            if(a != null) req = req.header(HttpHeaders.AUTHORIZATION, a);
            List<InventoryDto> body = req.retrieve().body(new ParameterizedTypeReference<>() {});
            return body != null ? body : Collections.emptyList();
        }
        catch(Exception e)
        {
            return Collections.emptyList();
        }
    }
}
