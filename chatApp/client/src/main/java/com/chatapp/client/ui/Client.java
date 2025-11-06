package com.chatapp.client.ui;

import java.net.Socket;
import java.util.Scanner;

import com.chatapp.client.model.CLI;
import com.chatapp.client.model.MessageReciever;
import com.chatapp.client.model.MessageSender;
import com.chatapp.client.model.AudioRecorder;
import com.chatapp.client.model.AudioSender;
import com.chatapp.client.model.VoiceStreamer;
import com.chatapp.client.model.VoiceReceiver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;

public class Client {


    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        
        try {
            String username;

            System.out.print("Enter your username: ");
            username = sc.nextLine();

            Socket tcpSocket = new Socket("192.168.1.8", 5000);
            DatagramSocket udpSocket = new DatagramSocket(); // UDP for voice calls
            
            System.out.println("UDP port: " + udpSocket.getLocalPort());
            
            // Create a single DataOutputStream to be shared
            DataOutputStream sharedOutputStream = new DataOutputStream(tcpSocket.getOutputStream());
            
            MessageReciever messageReciever = new MessageReciever(tcpSocket);
            MessageSender messageSender = new MessageSender(sharedOutputStream); 

            AudioRecorder audioRecorder = new AudioRecorder();
            AudioSender audioSender = new AudioSender(sharedOutputStream);
            
            // Voice call components
            VoiceStreamer voiceStreamer = new VoiceStreamer(udpSocket);
            VoiceReceiver voiceReceiver = new VoiceReceiver(udpSocket);

            // SEND USERNAME TO SERVER FOR REGISTRATION
            messageSender.setMessageContent("REGISTER:" + username);
            messageSender.sendMsgToServer();

            // Create CLI
            CLI cli = new CLI(username, messageSender, audioRecorder, audioSender, 
                            voiceStreamer, voiceReceiver, udpSocket);
            
            // Connect CLI to MessageReceiver for call handling
            messageReciever.setCli(cli);
            messageReciever.setMessageSender(messageSender);
            messageReciever.setUdpSocket(udpSocket);
            messageReciever.setUsername(username);

            //CONSTANTLY LISTEN FOR MESSAGES FROM SERVER
            Thread receiverThread = new Thread(messageReciever);
            receiverThread.start();

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
