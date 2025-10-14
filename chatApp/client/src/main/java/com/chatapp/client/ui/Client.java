package com.chatapp.client.ui;

import java.net.Socket;
import java.util.Scanner;

import com.chatapp.client.model.CLI;
import com.chatapp.client.model.MessageReciever;
import com.chatapp.client.model.MessageSender;

import java.io.IOException;
import java.net.DatagramSocket;

public class Client {


    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        
        try {
            String username;

            System.out.print("Enter your username: ");
            username = sc.nextLine();

            Socket tcpSocket = new Socket("localhost", 5000);
            DatagramSocket udpSocket = new DatagramSocket(); // FOR LATER UDP IMPLEMENTATION
            
            MessageReciever messageReciever = new MessageReciever(tcpSocket);
            MessageSender messageSender = new MessageSender(tcpSocket); 

            // SEND USERNAME TO SERVER FOR REGISTRATION
            messageSender.setMessageContent("REGISTER:" + username);
            messageSender.sendMsgToServer();

            //CONSTANTLY LISTEN FOR MESSAGES FROM SERVER
            Thread receiverThread = new Thread(messageReciever);
            receiverThread.start();

            CLI cli = new CLI(username, messageSender);
            String command = "";

            while (!command.equals("/exit")) {
                cli.displayCommands();
                System.out.print("Enter command: ");
                command = sc.nextLine();
                cli.executeCommand(command);
            }
            
            

                
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
        
    }
}