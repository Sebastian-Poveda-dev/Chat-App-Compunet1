package com.chatapp.server.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;



public class ClientHandler implements Runnable {

    private Socket client;
    private Map<String, ClientHandler> onlineUsers;
    private Map<String, ArrayList<String>> groups;
    private String username;
    
    private BufferedReader reader;
    private PrintWriter writer;


    public ClientHandler(Socket client, Map<String, ClientHandler> onlineUsers, Map<String, ArrayList<String>> groups) {
        this.client = client;
        this.onlineUsers = onlineUsers;
        this.groups = groups;
        initializeStreams();
    
    }

    @Override
    public void run() {
        
        
        try {
            // 1 STEP: Register user
            handleUserRegistration();

            // 2 STEP: Listen for messages from the client
            String message;
            while ((message = reader.readLine()) != null) {
                handleMessage(message);
            }

        } catch (IOException e) {
            System.out.println("Client " + username + " disconnected: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanUp();
        }

        
    }

    private void handleUserRegistration() throws IOException {
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
            System.out.println("User registered: " + username);
        } else {
            throw new IOException("Invalid registration ");
        }
    }

    private void handleMessage(String message) {
        // Handle incoming messages from the client
        if (message.startsWith("PRIVATE_MSG:")) {
            handleSendPrivateMessage(message);
        } else if (message.startsWith("GROUP_MSG:")) {
            handleSendGroupMessage(message);
        } else if (message.startsWith("CREATE_GROUP:")) {
            handleCreateGroup(message);
        } else if (message.startsWith("JOIN_GROUP:")) {
            handleJoinGroup(message);
        } else {
            writer.println(message);
        }
    }

    private void handleSendPrivateMessage(String message) {
        String[] msgparts = message.split(":");
        String from = msgparts[1];
        String to = msgparts[2];
        String msgContent = msgparts[3];

        ClientHandler recipientHandler = onlineUsers.get(to);
        if (recipientHandler != null) {
            recipientHandler.sendMessage("MESSAGE_FROM: " + from + ": " + msgContent);
            System.out.println("Private message from " + from + " to " + to + ": " + msgContent);
            
        } else {
            writer.println("ERROR: User " + to + " not found");
        }
    }

    private void handleSendGroupMessage(String message) {
        String [] msgParts = message.split(":");
        String from = msgParts[1];
        String groupName = msgParts[2];
        String msgContent = msgParts[3];

        ArrayList<String> members = groups.get(groupName);
        if (members != null) {
            // VERIFICAR QUE EL EMISOR PERTENECE AL GRUPO
            if (!members.contains(from)) {
                writer.println("ERROR: You are not a member of group " + groupName);
                return;
            }
            
            // Enviar mensaje a todos los miembros del grupo
            for (String member : members) {
                ClientHandler memberHandler = onlineUsers.get(member);
                if (memberHandler != null && !member.equals(from)) {
                    memberHandler.sendMessage("GROUP_MESSAGE_FROM: " + from + "@" + groupName + ": " + msgContent);
                    System.out.println("Group message from " + from + " to group " + groupName + ": " + msgContent);
                }
            }
            
            // Confirmar al emisor que el mensaje se envió
            writer.println("GROUP_MSG_SENT: " + groupName);
        } else {
            writer.println("ERROR: Group " + groupName + " not found");
        }
    }

    private void handleCreateGroup(String message) {
        String[] msgParts = message.split(":");
        String groupName = msgParts[1];
        String admin = username;
        ArrayList<String> members = new ArrayList<>();
            members.add(admin);  // El admin siempre se añade primero

        // msgParts[2] es el admin, msgParts[3] son los miembros adicionales
        if (msgParts.length > 3 && !msgParts[3].isEmpty()) {
            String[] memberArray = msgParts[3].split(",");
            for (String member : memberArray) {
                String trimmedMember = member.trim();
                if (!trimmedMember.equals(admin)) {  // Evitar duplicar al admin
                    members.add(trimmedMember);
                }
            }
        }

        if (groups.containsKey(groupName)) {
            writer.println("ERROR: Group " + groupName + " already exists");
            return;
        }

        groups.put(groupName, members);

        writer.println("GROUP_CREATED: " + groupName);
        
        // Notificar a todos los miembros (excepto al admin que lo creó)
        System.out.println("Notifying members of group " + groupName + ": " + members);
        for (String member: members) {
            System.out.println("Checking member: " + member + ", admin: " + admin);
            ClientHandler memberHandler = onlineUsers.get(member);
            
            if (memberHandler != null) {
                System.out.println("Member " + member + " is online");
                if (!member.equals(admin)) {
                    memberHandler.sendMessage("ADDED_TO_GROUP: " + groupName + " by " + admin);
                    System.out.println("Notification sent to " + member);
                } else {
                    System.out.println("Skipping notification to admin: " + member);
                }
            } else {
                System.out.println("Member " + member + " is not online");
            }
        }
    }

    private void handleJoinGroup(String message) {
        String[] msgParts = message.split(":");
        String groupName = msgParts[1];
        String user = username; 
        ArrayList<String> members = groups.get(groupName);
        if (members != null) {
            if (!members.contains(user)) {
                members.add(user);
                groups.put(groupName, members);
                writer.println("JOINED_GROUP: " + groupName);
            } else {
                writer.println("ERROR: You are already a member of group " + groupName);
            }
        } else {
            writer.println("ERROR: Group " + groupName + " does not exist");
        }
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

    private void initializeStreams() {

        try {
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error initializing I/O streams");
            e.printStackTrace();
            cleanUp();
            return; 
        }
        
    }
}