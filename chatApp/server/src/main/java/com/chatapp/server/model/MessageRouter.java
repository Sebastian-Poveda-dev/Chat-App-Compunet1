package com.chatapp.server.model;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class MessageRouter {

    private Map<String, ClientHandler> onlineUsers;
    private Map<String, Set<String>> groups;

    public MessageRouter() {
        onlineUsers = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
    }

    public void addUser(String username, ClientHandler handler) {
        if (onlineUsers.containsKey(username)) {
            System.out.println("User " + username + " already exists.");
            return;
        }
        
        onlineUsers.put(username, handler);

    }

    public void removeUser(String username) {
        if (!onlineUsers.containsKey(username)) {
            System.out.println("User " + username + " does not exist.");
            return;
        }
        
        onlineUsers.remove(username);
    }

    public void sendDirectMessage(String sender, String receiver, String content) {
        if (!onlineUsers.containsKey(receiver)) {
            System.out.println("Receiver " + receiver + " is not online.");
            return;
        }

        //TO DO: sending logic 
    }


}