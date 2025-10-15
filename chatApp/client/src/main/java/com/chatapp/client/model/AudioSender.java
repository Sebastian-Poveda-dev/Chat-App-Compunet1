package com.chatapp.client.model;

import java.io.*;
import java.net.Socket;

public class AudioSender {

    private DataOutputStream dataOutputStream;

    // Constructor that accepts an existing DataOutputStream
    public AudioSender(DataOutputStream dataOutputStream) {
        this.dataOutputStream = dataOutputStream;
    }

    // Legacy constructor for backward compatibility
    public AudioSender(Socket tcpSocket) {
        try {
            dataOutputStream = new DataOutputStream(tcpSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error initializing DataOutputStream for audio");
            e.printStackTrace();
        }
    }

    /**
     * Sends audio bytes to the server (raw bytes only)
     * The size is already sent in the metadata
     */
    public void sendAudioBytes(byte[] audioData) {
        try {
            // Send raw bytes directly
            dataOutputStream.write(audioData);
            dataOutputStream.flush();
            System.out.println("Audio sent: " + audioData.length + " bytes");
        } catch (IOException e) {
            System.out.println("Error sending audio bytes");
            e.printStackTrace();
        }
    }
}