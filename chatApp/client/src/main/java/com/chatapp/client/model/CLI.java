package com.chatapp.client.model;

public class CLI {

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

    public void executeCommand() {
        // Implementation for executing commands
        
    }
    

}
