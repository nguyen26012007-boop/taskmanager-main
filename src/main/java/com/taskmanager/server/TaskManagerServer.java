package com.taskmanager.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP Socket Server lắng nghe kết nối từ Client.
 */
public class TaskManagerServer {

    private final int port;
    private final RequestRouter router;
    private final List<ClientHandler> handlers = new ArrayList<>();
    private ServerSocket serverSocket;
    private boolean running = false;

    public TaskManagerServer(int port) {
        this.port = port;
        this.router = new RequestRouter();
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("[Server] Server đã khởi động thành công trên cổng " + port);

                while (running) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket, router);
                    synchronized (handlers) {
                        handlers.add(handler);
                    }
                    new Thread(handler, "ClientHandler-" + socket.getRemoteSocketAddress()).start();
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Server] Lỗi ServerSocket: " + e.getMessage());
                }
            }
        }, "TaskManagerServer-Listener").start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            synchronized (handlers) {
                for (ClientHandler handler : handlers) {
                    handler.stop();
                }
                handlers.clear();
            }
            System.out.println("[Server] Server đã dừng hoạt động");
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khi dừng server: " + e.getMessage());
        }
    }
}
