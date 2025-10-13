package com.chatapp.server.model;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Server {


    public static void main(String[] args) {

        String ipAddress = "";
        final int port = 5000;

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Could not start server on port " + port);
            e.printStackTrace();
            return; // Stop server startup if port is unavailable   
        }

        Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

        while (true) {
            try {
                Socket newClient = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("Error accepting client connection");
                e.printStackTrace();
            }
            

            ClientHandler clientHandler; // TODO = new ClientHandler(newClient, onlineUsers);
            
        }
        
    }

    public void registerUser(String username, ClientHandler handler, Map<String, ClientHandler> onlineUsers) {
        onlineUsers.put(username, handler);
        System.out.println("User registered: " + username);
    }
}