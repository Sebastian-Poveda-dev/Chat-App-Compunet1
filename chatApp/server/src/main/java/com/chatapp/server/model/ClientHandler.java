package com.chatapp.server.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;



public class ClientHandler implements Runnable {

    private Socket client;
    private Map<String, ClientHandler> onlineUsers;
    private String username;
    
    private BufferedReader reader;
    private PrintWriter writer;


    public ClientHandler(Socket client, Map<String, ClientHandler> onlineUsers) {
        this.client = client;
        this.onlineUsers = onlineUsers;
    }

    @Override
    public void run() {
        // Handle client communication

        
    }

    private void handleUserRegsitrarion() throws IOException {
        // This waits for a message like  "REGISTER:username"
        String registrationMessage = reader.readLine();
        String inputUsername = registrationMessage.substring(9);

        if (onlineUsers.containsKey(inputUsername)) {
            writer.println("ERROR: Username already taken");
            throw new IOException("Username already taken");
        }

        if (registrationMessage != null && registrationMessage.startsWith("REGISTER:")) {
            this.username = inputUsername;
            onlineUsers.put(username, this);
            writer.println("REGISTERED\n your username is " + username);
            System.out.println("User registered: " + username);
        } else {
            writer.println("REGISTERED SUCCESSFUL");
            throw new IOException("Invalid registration ");
        }
    }

    private void handleMessage(String message) {
        // Handle incoming messages from the client
        System.out.println("Received message from " + username + ": " + message);
        // Further processing logic here
    }
    
    public void sendMessage(String message) {
        writer.println(message);
    }

    private void cleanUp() {
        try {
            if (username != null) {
                onlineUsers.remove(username);
                System.out.println("User disconnected: " + username);
            }

            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (client != null) client.close();
        } catch (IOException e) {
            System.out.println("Error during cleanup");
            e.printStackTrace();
        }
    }
}