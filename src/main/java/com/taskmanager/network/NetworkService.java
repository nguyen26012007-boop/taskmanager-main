package com.taskmanager.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Client-side singleton quản lý kết nối TCP đến Server.
 * Gửi Request và nhận Response qua ObjectStream.
 */
public class NetworkService {

    private static NetworkService instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String host;
    private int port;

    private NetworkService() {}

    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }

    /**
     * Kết nối đến Server.
     */
    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        System.out.println("[Client] Đã kết nối đến server " + host + ":" + port);
    }

    /**
     * Gửi Request đến Server và nhận Response.
     * Synchronized để đảm bảo thread-safe khi nhiều thread cùng gọi.
     */
    public synchronized Response send(Request request) {
        try {
            ensureConnected();
            out.writeObject(request);
            out.flush();
            out.reset(); // tránh cache object cũ
            return (Response) in.readObject();
        } catch (Exception e) {
            System.err.println("[Client] Lỗi gửi request: " + e.getMessage());
            return Response.error("Mất kết nối đến server: " + e.getMessage());
        }
    }

    private void ensureConnected() throws IOException {
        if (socket == null || socket.isClosed()) {
            connect(host, port);
        }
    }

    /**
     * Ngắt kết nối.
     */
    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            System.out.println("[Client] Đã ngắt kết nối server");
        } catch (IOException e) {
            System.err.println("[Client] Lỗi ngắt kết nối: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
