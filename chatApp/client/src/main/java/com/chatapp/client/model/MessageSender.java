package com.chatapp.client.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MessageSender {

    private String messageContent;
    private DataOutputStream dataOutputStream;

    // Constructor that accepts an existing DataOutputStream
    public MessageSender(DataOutputStream dataOutputStream) {
        this.dataOutputStream = dataOutputStream;
    }

    // Legacy constructor for backward compatibility
    public MessageSender(Socket tcpSocket) {
        try {
            dataOutputStream = new DataOutputStream(tcpSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error initializing DataOutputStream");
            e.printStackTrace();
        }
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public void sendMsgToServer() {
        try {
            // Use writeUTF to send text messages with length prefix
            dataOutputStream.writeUTF(messageContent);
            dataOutputStream.flush();
        } catch (IOException e) {
            System.out.println("Error sending message to server");
            e.printStackTrace();
        }
    }
}
