package com.chatapp.client.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MessageSender {

    private Socket socket; //remote socket
    private String messageContent;

    public MessageSender(Socket tcpSocket) {
        this.socket = tcpSocket;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public void sendMsgToServer() {

        try {
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);

            bw.write(messageContent + "\n");
            bw.flush();


        } catch (IOException e) {
            System.out.println("Error sending message to server");
            e.printStackTrace();
        }
        

    }


    
}
