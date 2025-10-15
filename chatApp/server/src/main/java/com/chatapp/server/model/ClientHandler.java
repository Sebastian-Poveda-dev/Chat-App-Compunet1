package com.chatapp.server.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;



public class ClientHandler implements Runnable {

    private Socket client;
    private Map<String, ClientHandler> onlineUsers;
    private Map<String, ArrayList<String>> groups;
    private String username;
    
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;



    public ClientHandler(Socket client, Map<String, ClientHandler> onlineUsers, Map<String, ArrayList<String>> groups) {
        this.client = client;
        this.onlineUsers = onlineUsers;
        this.groups = groups;
    
    }

    @Override
    public void run() {
        
        
        try {
            // Initialize streams
            dataInputStream = new DataInputStream(client.getInputStream());
            dataOutputStream = new DataOutputStream(client.getOutputStream());
            
            // 1 STEP: Register user
            handleUserRegistration();

            // 2 STEP: Listen for messages from the client
            String message;
            while ((message = dataInputStream.readUTF()) != null) {
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
        String registrationMessage = dataInputStream.readUTF();
        String inputUsername = registrationMessage.substring(9);

        if (onlineUsers.containsKey(inputUsername)) {
            dataOutputStream.writeUTF("ERROR: Username already taken");
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
        System.out.println("Received message from " + username + ": " + message);
        
        if (message.startsWith("PRIVATE_MSG:")) {
            handleSendPrivateMessage(message);
        } else if (message.startsWith("GROUP_MSG:")) {
            handleSendGroupMessage(message);
        } else if (message.startsWith("CREATE_GROUP:")) {
            handleCreateGroup(message);
        } else if (message.startsWith("JOIN_GROUP:")) {
            handleJoinGroup(message);
        } else if (message.startsWith("PRIVATE_AUDIO:")) {
            System.out.println("Detected PRIVATE_AUDIO message");
            handleSendAudioPrivate(message);
        } else if (message.startsWith("GROUP_AUDIO:")) {
            System.out.println("Detected GROUP_AUDIO message");
            handleSendAudioGroup(message);
        } else if (message.startsWith("CALL_REQUEST:")) {
            handleCallRequest(message);
        } else if (message.startsWith("GROUP_CALL_REQUEST:")) {
            handleGroupCallRequest(message);
        } else if (message.startsWith("CALL_ACCEPT:")) {
            handleCallAccept(message);
        } else if (message.startsWith("CALL_REJECT:")) {
            handleCallReject(message);
        } else if (message.startsWith("CALL_END:")) {
            handleCallEnd(message);
        } else if (message.startsWith("GROUP_CALL_END:")) {
            handleGroupCallEnd(message);
        } else {
            try {
                dataOutputStream.writeUTF(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            try {
                dataOutputStream.writeUTF("ERROR: User " + to + " not found");
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                try {
                    dataOutputStream.writeUTF("ERROR: You are not a member of group " + groupName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            try {
                dataOutputStream.writeUTF("GROUP_MSG_SENT: " + groupName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                dataOutputStream.writeUTF("ERROR: Group " + groupName + " not found");
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            try {
                dataOutputStream.writeUTF("ERROR: Group " + groupName + " already exists");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        groups.put(groupName, members);

        try {
            dataOutputStream.writeUTF("GROUP_CREATED: " + groupName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
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
                try {
                    dataOutputStream.writeUTF("JOINED_GROUP: " + groupName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    dataOutputStream.writeUTF("ERROR: You are already a member of group " + groupName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                dataOutputStream.writeUTF("ERROR: Group " + groupName + " does not exist");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSendAudioPrivate(String message) {
        System.out.println("handleSendAudioPrivate called with: " + message);
        
        // Parse: "PRIVATE_AUDIO:juan:pedro:audio_123.wav:50000"
        String[] msgParts = message.split(":");
        
        if (msgParts.length < 5) {
            System.out.println("ERROR: Invalid audio message format. Expected 5 parts, got " + msgParts.length);
            try {
                dataOutputStream.writeUTF("ERROR: Invalid audio message format");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        String from = msgParts[1];
        String to = msgParts[2];
        String audioFileName = msgParts[3];
        int audioSize = Integer.parseInt(msgParts[4]);
        
        System.out.println("From: " + from + ", To: " + to + ", File: " + audioFileName + ", Size: " + audioSize);
        System.out.println("Online users: " + onlineUsers.keySet());

        try {
            // Read audio bytes from data input stream
            byte[] audioData = new byte[audioSize];
            dataInputStream.readFully(audioData);
            
            System.out.println("Audio received from " + from + " for " + to + " (" + audioSize + " bytes)");

            ClientHandler recipientHandler = onlineUsers.get(to);

            if (recipientHandler != null) {
                recipientHandler.sendAudio(from, audioFileName, audioData);
                dataOutputStream.writeUTF("AUDIO_SENT: " + to);
                System.out.println("Audio forwarded from " + from + " to " + to);
            } else {
                dataOutputStream.writeUTF("ERROR: User " + to + " not found or offline");
                System.out.println("User " + to + " not found");
            }

        } catch (IOException | NumberFormatException e) {
            System.out.println("Error receiving audio from " + from);
            e.printStackTrace();
            try {
                dataOutputStream.writeUTF("ERROR: Failed to send audio");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }    
    }

    private void handleSendAudioGroup(String message) {
        // Parse: "GROUP_AUDIO:juan:amigos:audio_123.wav:50000"
        String[] msgParts = message.split(":");
        String from = msgParts[1];
        String groupName = msgParts[2];
        String audioFileName = msgParts[3];
        int audioSize = Integer.parseInt(msgParts[4]);

        try {
            // Read audio bytes from data input stream
            byte[] audioData = new byte[audioSize];
            dataInputStream.readFully(audioData);

            System.out.println("Group audio received from: " + from + " for group " + groupName + 
                            " (" + audioSize + " bytes, file: " + audioFileName + ")");

            // Find the group
            ArrayList<String> members = groups.get(groupName);
            
            if (members != null) {
                // Verify that sender is member of the group
                if (!members.contains(from)) {
                    dataOutputStream.writeUTF("ERROR: You are not a member of group " + groupName);
                    System.out.println("User " + from + " is not a member of " + groupName);
                    return;
                }
                
                // Send audio to all members (except sender)
                int sentCount = 0;
                for (String member : members) {
                    if (!member.equals(from)) {
                        ClientHandler memberHandler = onlineUsers.get(member);
                        
                        if (memberHandler != null) {
                            memberHandler.sendGroupAudio(from, groupName, audioFileName, audioData);
                            sentCount++;
                            System.out.println("Audio sent to " + member);
                        } else {
                            System.out.println("Member " + member + " is not online");
                        }
                    }
                }
                
                // Confirm to sender
                dataOutputStream.writeUTF("GROUP_AUDIO_SENT: " + groupName + " (" + sentCount + " members)");
                System.out.println("Group audio from " + from + " sent to " + sentCount + " members of " + groupName);
                
            } else {
                dataOutputStream.writeUTF("ERROR: Group " + groupName + " not found");
                System.out.println("Group " + groupName + " not found");
            }

        } catch (IOException | NumberFormatException e) {
            System.out.println("Error receiving group audio from " + from);
            e.printStackTrace();
            try {
                dataOutputStream.writeUTF("ERROR: Failed to send group audio");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void sendMessage(String message) {
        try {
            dataOutputStream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAudio(String from, String audioFileName, byte[] audioData) {
        try {
            // Send metadata with size
            dataOutputStream.writeUTF("AUDIO_FROM: " + from + ": " + audioFileName + ": " + audioData.length);

            // Send audio bytes
            dataOutputStream.write(audioData);
            dataOutputStream.flush();
            
            System.out.println("Audio sent to " + username + " (" + audioData.length + " bytes)");
            
        } catch (IOException e) {
            System.out.println("Error sending audio to " + username);
            e.printStackTrace();
        }
    }

    public void sendGroupAudio(String from, String groupName, String audioFileName, byte[] audioData) {
        try {
            // Send metadata with group format and size
            dataOutputStream.writeUTF("GROUP_AUDIO_FROM: " + from + "@" + groupName + ": " + audioFileName + ": " + audioData.length);
            
            // Send audio bytes
            dataOutputStream.write(audioData);
            dataOutputStream.flush();
            
            System.out.println("Group audio sent to " + username + " (" + audioData.length + " bytes)");
            
        } catch (IOException e) {
            System.out.println(" Error sending group audio to " + username);
            e.printStackTrace();
        }
    }

    // ============ CALL HANDLING METHODS ============

    private void handleCallRequest(String message) {
        // Parse: "CALL_REQUEST:juan:ana:5500"
        System.out.println("🔵 SERVER: Received CALL_REQUEST: " + message);
        String[] parts = message.split(":");
        String caller = parts[1];
        String callee = parts[2];
        String callerUdpPort = parts[3];

        System.out.println("🔵 SERVER: Call request from " + caller + " to " + callee);
        System.out.println("🔵 SERVER: Looking for callee handler: " + callee);

        ClientHandler calleeHandler = onlineUsers.get(callee);
        if (calleeHandler != null) {
            try {
                // Forward call request to callee with caller's UDP port and IP
                String callerIP = client.getInetAddress().getHostAddress();
                String incomingCallMsg = "INCOMING_CALL:" + caller + ":" + callerIP + ":" + callerUdpPort;
                System.out.println("🔵 SERVER: Sending to " + callee + ": " + incomingCallMsg);
                calleeHandler.dataOutputStream.writeUTF(incomingCallMsg);
                calleeHandler.dataOutputStream.flush(); // Ensure it's sent
                System.out.println("🔵 SERVER: ✓ Call request forwarded to " + callee);
            } catch (IOException e) {
                System.out.println("🔵 SERVER: ✗ Error forwarding call request to " + callee);
                e.printStackTrace();
                try {
                    dataOutputStream.writeUTF("ERROR: Failed to reach " + callee);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            System.out.println("🔵 SERVER: ✗ ERROR: User " + callee + " not found or offline");
            try {
                dataOutputStream.writeUTF("ERROR: User " + callee + " not found or offline");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCallAccept(String message) {
        // Parse: "CALL_ACCEPT:ana:juan:5501"
        System.out.println("🔵 SERVER: Received CALL_ACCEPT: " + message);
        String[] parts = message.split(":");
        String accepter = parts[1];
        String caller = parts[2];
        String accepterUdpPort = parts[3];

        System.out.println("🔵 SERVER: " + accepter + " accepted call from " + caller);
        System.out.println("🔵 SERVER: Looking for caller handler: " + caller);

        ClientHandler callerHandler = onlineUsers.get(caller);
        if (callerHandler != null) {
            try {
                // Send accept notification to caller with accepter's UDP info
                String accepterIP = client.getInetAddress().getHostAddress();
                String responseMessage = "CALL_ACCEPTED:" + accepter + ":" + accepterIP + ":" + accepterUdpPort;
                System.out.println("🔵 SERVER: Sending to " + caller + ": " + responseMessage);
                callerHandler.dataOutputStream.writeUTF(responseMessage);
                callerHandler.dataOutputStream.flush(); // Ensure it's sent
                System.out.println("🔵 SERVER: ✓ Call accepted notification sent to " + caller);
            } catch (IOException e) {
                System.out.println("🔵 SERVER: ✗ Error sending call accepted to " + caller);
                e.printStackTrace();
            }
        } else {
            System.out.println("🔵 SERVER: ✗ ERROR: Caller handler not found for: " + caller);
        }
    }

    private void handleCallReject(String message) {
        // Parse: "CALL_REJECT:ana:juan"
        String[] parts = message.split(":");
        String rejecter = parts[1];
        String caller = parts[2];

        System.out.println(rejecter + " rejected call from " + caller);

        ClientHandler callerHandler = onlineUsers.get(caller);
        if (callerHandler != null) {
            try {
                callerHandler.dataOutputStream.writeUTF("CALL_REJECTED:" + rejecter);
                System.out.println("Call rejected notification sent to " + caller);
            } catch (IOException e) {
                System.out.println("Error sending call rejected to " + caller);
                e.printStackTrace();
            }
        }
    }

    private void handleCallEnd(String message) {
        // Parse: "CALL_END:juan:ana"
        String[] parts = message.split(":");
        String ender = parts[1];
        String other = parts[2];

        System.out.println(ender + " ended call with " + other);

        ClientHandler otherHandler = onlineUsers.get(other);
        if (otherHandler != null) {
            try {
                otherHandler.dataOutputStream.writeUTF("CALL_ENDED:" + ender);
                System.out.println("Call ended notification sent to " + other);
            } catch (IOException e) {
                System.out.println("Error sending call ended to " + other);
                e.printStackTrace();
            }
        }
    }

    private void handleGroupCallRequest(String message) {
        // Parse: "GROUP_CALL_REQUEST:juan:amigos:5500"
        String[] parts = message.split(":");
        String caller = parts[1];
        String groupName = parts[2];
        String callerUdpPort = parts[3];

        System.out.println("Group call request from " + caller + " to group " + groupName);

        ArrayList<String> members = groups.get(groupName);
        if (members != null && members.contains(caller)) {
            String callerIP = client.getInetAddress().getHostAddress();
            
            // Notify all members except caller
            for (String member : members) {
                if (!member.equals(caller)) {
                    ClientHandler memberHandler = onlineUsers.get(member);
                    if (memberHandler != null) {
                        try {
                            memberHandler.dataOutputStream.writeUTF(
                                "INCOMING_GROUP_CALL:" + caller + "@" + groupName + ":" + callerIP + ":" + callerUdpPort
                            );
                        } catch (IOException e) {
                            System.out.println("Error notifying " + member + " of group call");
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            try {
                dataOutputStream.writeUTF("ERROR: Group " + groupName + " not found or you're not a member");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleGroupCallEnd(String message) {
        // Parse: "GROUP_CALL_END:juan:amigos"
        String[] parts = message.split(":");
        String ender = parts[1];
        String groupName = parts[2];

        System.out.println(ender + " ended group call in " + groupName);

        ArrayList<String> members = groups.get(groupName);
        if (members != null) {
            for (String member : members) {
                if (!member.equals(ender)) {
                    ClientHandler memberHandler = onlineUsers.get(member);
                    if (memberHandler != null) {
                        try {
                            memberHandler.dataOutputStream.writeUTF("GROUP_CALL_ENDED:" + ender + "@" + groupName);
                        } catch (IOException e) {
                            System.out.println("Error notifying " + member + " of group call end");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    
    private void cleanUp() {
        try {
            if (username != null) {
                onlineUsers.remove(username);
                System.out.println("User disconnected: " + username);
            }

            if (dataInputStream != null) dataInputStream.close();
            if (dataOutputStream != null) dataOutputStream.close();
            if (client != null) client.close();
        } catch (IOException e) {
            System.out.println("Error during cleanup");
            e.printStackTrace();
        }
    }
}