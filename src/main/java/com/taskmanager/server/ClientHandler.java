package com.taskmanager.server;

import com.taskmanager.model.User;
import com.taskmanager.network.Request;
import com.taskmanager.network.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Xử lý kết nối từ mỗi client trên một Thread riêng.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestRouter router;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User loggedInUser;
    private boolean running = true;

    public ClientHandler(Socket socket, RequestRouter router) {
        this.socket = socket;
        this.router = router;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server] Client kết nối từ: " + socket.getRemoteSocketAddress());

            while (running) {
                Object obj = in.readObject();
                if (obj instanceof Request request) {
                    Response response = router.route(request, this);
                    out.writeObject(response);
                    out.flush();
                    out.reset(); // clear cache
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] Client ngắt kết nối: " + socket.getRemoteSocketAddress() + " (" + e.getMessage() + ")");
        } finally {
            closeConnection();
        }
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void stop() {
        this.running = false;
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[Server] Lỗi đóng kết nối client: " + e.getMessage());
        }
    }
}
