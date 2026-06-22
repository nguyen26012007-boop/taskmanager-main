package com.taskmanager.network;

import java.io.Serializable;

/**
 * Đối tượng phản hồi từ Server trả về Client qua TCP Socket.
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final Object data;
    private final String errorMessage;

    private Response(boolean success, Object data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static Response ok(Object data) {
        return new Response(true, data, null);
    }

    public static Response error(String message) {
        return new Response(false, null, message);
    }

    public boolean isSuccess() { return success; }
    public Object getData() { return data; }
    public String getErrorMessage() { return errorMessage; }

    @SuppressWarnings("unchecked")
    public <T> T getDataAs() { return (T) data; }

    @Override
    public String toString() {
        return success ? "Response{OK, data=" + data + "}" : "Response{ERROR: " + errorMessage + "}";
    }
}
