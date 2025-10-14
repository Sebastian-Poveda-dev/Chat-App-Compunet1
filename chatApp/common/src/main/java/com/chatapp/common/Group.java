package com.chatapp.common;

import java.util.ArrayList;
import java.util.UUID;

public class Group {

    private String groupName;
    private String groupId;
    private ArrayList<String> members;
    private String admin;

    public Group(String groupName) {
        this.groupName = groupName;
        this.members = new ArrayList<>();
        this.groupId = UUID.randomUUID().toString();
    }

    public Group() {
        this.members = new ArrayList<>();
        this.groupId = UUID.randomUUID().toString();
    }

    public Group(String groupName, String admin) {
        this(groupName);
        this.admin = admin;
        this.members = new ArrayList<>();
        this.groupId = UUID.randomUUID().toString();
        addMember(admin); 
    }

    public boolean addMember(String username) {
        if (!isMember(username)) {
            members.add(username);
            return true;
        } 
        System.out.println("User " + username + " is already a member of the group " + groupName);
        return false;
    }

    public boolean removeMember(String username) {
        if (isMember(username)) {
            members.remove(username);
            return true;
        } 
        System.out.println("User " + username + " is not a member of the group " + groupName);
        return false;
    }

    public boolean isMember(String username) {
        return members.contains(username);
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public boolean isAdmin(String username) {
        return admin != null && admin.equals(username);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
  

}
