package com.renko.client;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthHeaderForwarder
{
    public String currentAuthorizationHeader()
    {
        var attrs = RequestContextHolder.getRequestAttributes();
        if(attrs instanceof ServletRequestAttributes servletAttrs)
        {
            String header = servletAttrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if(header != null && !header.isBlank())
            {
                return header;
            }
        }
        return null;
    }
}
