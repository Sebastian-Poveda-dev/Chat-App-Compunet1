package com.chatapp.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.Socket;

public class MessageReciever implements Runnable {

    private Socket socket;

    public MessageReciever(Socket tcpSocket) {
        this.socket = tcpSocket;
    }

    @Override
    public void run() {
        while (true) { // MAYBE OTHER CONDITION TO STOP
            String message = recieveMsgFromServer();
            if (message != null) {
                System.out.println("New message: " + message);
            } else {
                System.out.println("Connection closed by server.");
                break;
            }
            
        }
    }

    public String recieveMsgFromServer() {

        try {
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String message = br.readLine();
            return message;
        } catch (IOException e) {
            System.out.println("Error receiving message from server");
            e.printStackTrace();
            return null;
        } 
    }
}
