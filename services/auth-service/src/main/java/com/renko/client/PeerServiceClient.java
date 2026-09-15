package com.renko.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PeerServiceClient
{
    @Qualifier("storeRestClient")
    private final RestClient storeRestClient;
    @Qualifier("catalogRestClient")
    private final RestClient catalogRestClient;
    @Qualifier("salesRestClient")
    private final RestClient salesRestClient;
    @Qualifier("billingRestClient")
    private final RestClient billingRestClient;
    @Qualifier("reportRestClient")
    private final RestClient reportRestClient;

    public void clearAllPeers()
    {
        for(RestClient client : List.of(storeRestClient, catalogRestClient, salesRestClient, billingRestClient, reportRestClient))
        {
            try
            {
                client.delete().uri("/api/dev/clear-db").retrieve().toBodilessEntity();
            }
            catch(Exception e)
            {
                System.out.println("Peer clear-db failed: " + e.getMessage());
            }
        }
    }

    public <T> T post(RestClient client, String path, Object body, String authorization, Class<T> type)
    {
        RestClient.RequestBodySpec spec = client.post().uri(path).contentType(MediaType.APPLICATION_JSON);
        if(authorization != null)
        {
            spec = spec.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return spec.body(body).retrieve().body(type);
    }

    public RestClient store() { return storeRestClient; }
    public RestClient catalog() { return catalogRestClient; }
    public RestClient sales() { return salesRestClient; }
    public RestClient billing() { return billingRestClient; }
    public RestClient report() { return reportRestClient; }
}
