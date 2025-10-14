package com.chatapp.client.model;

import com.chatapp.common.Message;

public class CLI {

    private String currentUser;
    private MessageSender messageSender;

    public CLI(String currentUser, MessageSender messageSender) {
        this.currentUser = currentUser;
        this.messageSender = messageSender;
    }

    public void displayCommands() {
        System.out.println("Available commands:");
        System.out.println("/msg <username> <message> - Send a direct message to a user");
        System.out.println("/msgg <groupname> <message> - Send a message to a group");
        System.out.println("/createg <groupname> <user1 , user2, user3...> - Create a new group");
        System.out.println("/joing <groupname> - Join an existing group");
        System.out.println("/leaveg <groupname> - Leave a group");
        System.out.println("/listg - List all groups you are a member of");
        System.out.println("/removeg <groupname> <user1 , user2, user3...> - Remove users from a group");
        System.out.println("/addg <groupname> <user1 , user2, user3...> - Add users to a group");
        System.out.println("/listu - List all users currently online");
        System.out.println("/exit - Exit the application");
    }

    public void executeCommand(String command) {
        // Implementation for executing commands

        if (command.startsWith("/msg") || command.startsWith("/msgg")) {
            msgExe(command);
        } else if (command.startsWith("/createg")) {
            createGroupExe(command);
        } else if (command.startsWith("/joing")) {
            joinGroupExe(command);
        }
    }

    private void msgExe(String command) {
        Message msg = Message.fromCommand(currentUser, command);
        if (msg != null) {
            messageSender.setMessageContent(msg.getFormattedMessage());
            messageSender.sendMsgToServer();
        }
    }

    private void createGroupExe(String command) {
        String[] commandParts = command.split(" ", 3);
        if (commandParts.length >= 2) {
            String groupName = commandParts[1];
            String members = commandParts.length > 2 ? commandParts[2] : "";
            String createGroupCommand = "CREATE_GROUP:" + groupName + ":" + currentUser + ":" + members;
            messageSender.setMessageContent(createGroupCommand);
            messageSender.sendMsgToServer();
        } else {
            System.out.println("Usage: /createg <groupname> <user1 , user2, user3...>");
        }
    }

    private void joinGroupExe(String command) {
        String[] commandParts = command.split(" ", 2);
        if (commandParts.length == 2) {
            String groupName = commandParts[1];
            String joinGroupCommand = "JOIN_GROUP:" + groupName + ":" + currentUser;
            messageSender.setMessageContent(joinGroupCommand); 
            messageSender.sendMsgToServer();
        } else {
            System.out.println("Usage: /joing <groupname>");
        }
    }
}
        
    
    


