package com.renko.exceptions;

import jakarta.persistence.EntityNotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helpers for consistent, detailed exception messages (resource, ids, invalid fields).
 */
public final class ExceptionMessages
{
    private ExceptionMessages()
    {
    }

    public static EntityNotFoundException notFound(String resource, Object id)
    {
        return new EntityNotFoundException(resource + " not found with id: " + id);
    }

    public static EntityNotFoundException notFound(String resource, Object id, String action)
    {
        return new EntityNotFoundException(resource + " not found with id: " + id + "; cannot " + action);
    }

    public static EntityNotFoundException notFoundBy(String resource, String field, Object value)
    {
        return new EntityNotFoundException(resource + " not found with " + field + ": " + value);
    }

    public static UserException required(String field)
    {
        return UserException.withDetail(field + " is required", field, null);
    }

    public static UserException required(String field, String message)
    {
        return UserException.withDetail(message, field, null);
    }

    public static UserException mismatch(String message, Map<String, Object> details)
    {
        return UserException.withDetails(message, details);
    }

    public static UserException mismatch(String message, Object... keyValues)
    {
        return UserException.withDetails(message, ctx(keyValues));
    }

    public static Exception invalid(String message, String field, Object value)
    {
        return new Exception(message + " [invalid " + field + "=" + value + "]");
    }

    public static Exception detailed(String message, Map<String, Object> context)
    {
        StringBuilder sb = new StringBuilder(message);
        if(context != null && false == context.isEmpty())
        {
            sb.append(" | details: ");
            boolean first = true;
            for(Map.Entry<String, Object> entry : context.entrySet())
            {
                if(false == first)
                {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        return new Exception(sb.toString());
    }

    public static Map<String, Object> ctx(Object... keyValues)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        if(keyValues == null)
        {
            return map;
        }
        for(int i = 0; i + 1 < keyValues.length; i += 2)
        {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
