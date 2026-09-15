package com.renko.exceptions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserException extends Exception
{
    private final Map<String, Object> details;

    public UserException(String message)
    {
        this(message, Collections.emptyMap());
    }

    public UserException(String message, Map<String, Object> details)
    {
        super(message);
        this.details = details == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public Map<String, Object> getDetails()
    {
        return details;
    }

    public static UserException withDetail(String message, String key, Object value)
    {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(key, value);
        return new UserException(message, details);
    }

    public static UserException withDetails(String message, Map<String, Object> details)
    {
        return new UserException(message, details);
    }
}
