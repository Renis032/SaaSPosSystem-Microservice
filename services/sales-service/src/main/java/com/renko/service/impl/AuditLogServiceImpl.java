package com.renko.service.impl;

import com.renko.client.AuthHeaderForwarder;
import com.renko.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService
{
    @Qualifier("reportRestClient")
    private final RestClient reportRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;

    @Override
    public void record(Long storeId, String action, String entityType, String entityId, String details)
    {
        recordChange(storeId, action, entityType, entityId, null, null, details);
    }

    @Override
    public void recordChange(Long storeId, String action, String entityType, String entityId,
                             String beforeState, String afterState, String details)
    {
        try
        {
            var req = reportRestClient.post()
                    .uri("/api/audit-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "storeId", storeId,
                            "action", action,
                            "entityType", entityType,
                            "entityId", entityId != null ? entityId : "",
                            "details", details != null ? details : "",
                            "beforeState", beforeState != null ? beforeState : "",
                            "afterState", afterState != null ? afterState : ""
                    ));
            String auth = authHeaderForwarder.currentAuthorizationHeader();
            if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
            req.retrieve().toBodilessEntity();
        }
        catch(Exception e)
        {
            log.warn("Failed to forward audit log: {}", e.getMessage());
        }
    }

    @Override
    public List<?> listForStore(Long storeId) throws Exception
    {
        return Collections.emptyList();
    }
}
