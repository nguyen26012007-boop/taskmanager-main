package com.taskmanager.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Đối tượng yêu cầu gửi từ Client đến Server qua TCP Socket.
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String action;
    private final Map<String, Object> params;

    public Request(String action) {
        this.action = action;
        this.params = new HashMap<>();
    }

    public String getAction() { return action; }
    public Map<String, Object> getParams() { return params; }

    public Request put(String key, Object value) {
        params.put(key, value);
        return this;
    }

    public Object get(String key) { return params.get(key); }
    public String getString(String key) { return (String) params.get(key); }
    public int getInt(String key) { return params.get(key) != null ? ((Number) params.get(key)).intValue() : 0; }
    public Integer getInteger(String key) { return params.get(key) != null ? ((Number) params.get(key)).intValue() : null; }
    public boolean getBool(String key) { return Boolean.TRUE.equals(params.get(key)); }

    @Override
    public String toString() { return "Request{action='" + action + "', params=" + params + "}"; }
}
