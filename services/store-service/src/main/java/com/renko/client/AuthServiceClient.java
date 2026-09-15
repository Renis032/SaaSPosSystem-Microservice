package com.renko.client;

import com.renko.payload.dto.CreateEmployeeDto;
import com.renko.payload.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthServiceClient
{
    @Qualifier("authRestClient")
    private final RestClient authRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;

    private RestClient.RequestHeadersSpec<?> withAuth(RestClient.RequestHeadersSpec<?> spec)
    {
        String auth = authHeaderForwarder.currentAuthorizationHeader();
        if(auth != null)
        {
            return spec.header(HttpHeaders.AUTHORIZATION, auth);
        }
        return spec;
    }

    public UserDto createInternalUser(CreateEmployeeDto dto)
    {
        var req = authRestClient.post()
                .uri("/api/users/internal")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto);
        String auth = authHeaderForwarder.currentAuthorizationHeader();
        if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
        return req.retrieve().body(UserDto.class);
    }

    public UserDto linkStore(Long userId, Long storeId)
    {
        var req = authRestClient.patch()
                .uri("/api/users/{id}/store", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("storeId", storeId));
        String auth = authHeaderForwarder.currentAuthorizationHeader();
        if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
        return req.retrieve().body(UserDto.class);
    }

    public UserDto getUser(Long id)
    {
        var req = authRestClient.get().uri("/api/users/{id}", id);
        String auth = authHeaderForwarder.currentAuthorizationHeader();
        if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
        return req.retrieve().body(UserDto.class);
    }

    public List<UserDto> listByStore(Long storeId)
    {
        var req = authRestClient.get().uri("/api/users/store/{storeId}", storeId);
        String auth = authHeaderForwarder.currentAuthorizationHeader();
        if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
        return req.retrieve().body(new ParameterizedTypeReference<>() {});
    }

    public void deleteUser(Long id)
    {
        var req = authRestClient.delete().uri("/api/users/{id}", id);
        String auth = authHeaderForwarder.currentAuthorizationHeader();
        if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
        req.retrieve().toBodilessEntity();
    }
}
