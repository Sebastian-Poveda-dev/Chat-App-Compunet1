package com.chatapp.server.ui;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.chatapp.server.model.ClientHandler;



public class Server {


    public static void main(String[] args) {

        String ipAddress = "";
        final int port = 5000;

        ServerSocket serverSocket;
        DatagramSocket udpServerSocket; // For future UDP use
        try {
            InetAddress serverIP = InetAddress.getByName("192.168.1.8");
            serverSocket = new ServerSocket(port, 50, serverIP);
        } catch (IOException e) {
            System.out.println("Could not start server on port " + port);
            e.printStackTrace();
            return; // Stop server startup if port is unavailable   
        }

        Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();
        Map<String, ArrayList<String>> groups = new ConcurrentHashMap<>();

        while (true) {
            try {
                Socket newClient = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(newClient, onlineUsers, groups);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();

            } catch (IOException e) {
                System.out.println("Error accepting client connection");
                e.printStackTrace();
            }
            

            
            
        }
        
    }


}
