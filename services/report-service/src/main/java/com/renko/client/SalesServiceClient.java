package com.renko.client;

import com.renko.payload.dto.OrderDto;
import com.renko.payload.dto.RefundDto;
import com.renko.payload.dto.ShiftReportDto;
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
public class SalesServiceClient
{
    @Qualifier("salesRestClient")
    private final RestClient salesRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;

    private String auth() { return authHeaderForwarder.currentAuthorizationHeader(); }

    public List<OrderDto> ordersByStore(Long storeId)
    {
        try
        {
            var req = salesRestClient.get().uri("/api/orders/store/{storeId}", storeId);
            String a = auth();
            if(a != null) req = req.header(HttpHeaders.AUTHORIZATION, a);
            List<OrderDto> body = req.retrieve().body(new ParameterizedTypeReference<>() {});
            return body != null ? body : Collections.emptyList();
        }
        catch(Exception e)
        {
            return Collections.emptyList();
        }
    }

    public List<RefundDto> refundsByStore(Long storeId)
    {
        try
        {
            var req = salesRestClient.get().uri("/api/refunds/store/{storeId}", storeId);
            String a = auth();
            if(a != null) req = req.header(HttpHeaders.AUTHORIZATION, a);
            List<RefundDto> body = req.retrieve().body(new ParameterizedTypeReference<>() {});
            return body != null ? body : Collections.emptyList();
        }
        catch(Exception e)
        {
            return Collections.emptyList();
        }
    }

    public List<ShiftReportDto> shiftsByStore(Long storeId)
    {
        try
        {
            var req = salesRestClient.get().uri("/api/shift-report/store/{storeId}", storeId);
            String a = auth();
            if(a != null) req = req.header(HttpHeaders.AUTHORIZATION, a);
            List<ShiftReportDto> body = req.retrieve().body(new ParameterizedTypeReference<>() {});
            return body != null ? body : Collections.emptyList();
        }
        catch(Exception e)
        {
            return Collections.emptyList();
        }
    }
}
