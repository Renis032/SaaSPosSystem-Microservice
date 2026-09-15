package com.renko.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig
{
    @Bean
    public RestClient authRestClient(@Value("${services.auth.url:http://localhost:5001}") String baseUrl)
    {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient storeRestClient(@Value("${services.store.url:http://localhost:5002}") String baseUrl)
    {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient catalogRestClient(@Value("${services.catalog.url:http://localhost:5003}") String baseUrl)
    {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient salesRestClient(@Value("${services.sales.url:http://localhost:5004}") String baseUrl)
    {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient billingRestClient(@Value("${services.billing.url:http://localhost:5005}") String baseUrl)
    {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient reportRestClient(@Value("${services.report.url:http://localhost:5006}") String baseUrl)
    {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
