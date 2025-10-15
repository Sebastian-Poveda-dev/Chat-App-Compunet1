package com.chatapp.client.model;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Receives audio packets via UDP and plays them in real-time
 */
public class VoiceReceiver {
    
    private AudioFormat audioFormat;
    private SourceDataLine speakerLine;
    private Thread receivingThread;
    private boolean isReceiving;
    private DatagramSocket udpSocket;

    public VoiceReceiver(DatagramSocket udpSocket) {
        // Same format as VoiceStreamer: 8000 Hz, 16-bit, mono
        this.audioFormat = new AudioFormat(8000, 16, 1, true, true);
        this.udpSocket = udpSocket;
        this.isReceiving = false;
    }

    /**
     * Start receiving and playing audio
     */
    public void startReceiving() {
        try {
            speakerLine = AudioSystem.getSourceDataLine(audioFormat);
            speakerLine.open(audioFormat);
            speakerLine.start();

            isReceiving = true;

            receivingThread = new Thread(() -> {
                System.out.println("🔊 VoiceReceiver thread started - Listening for UDP packets...");
                byte[] buffer = new byte[512]; // Match VoiceStreamer buffer size
                int packetsReceived = 0;
                long lastReportTime = System.currentTimeMillis();
                
                while (isReceiving) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(packet);
                        
                        packetsReceived++;
                        
                        // Report every 2 seconds
                        long now = System.currentTimeMillis();
                        if (now - lastReportTime > 2000) {
                            System.out.println("📦 Received " + packetsReceived + " audio packets");
                            lastReportTime = now;
                        }
                        
                        // Play received audio immediately
                        speakerLine.write(packet.getData(), 0, packet.getLength());
                        
                    } catch (IOException e) {
                        if (isReceiving) {
                            System.out.println("Error receiving voice packet: " + e.getMessage());
                        }
                    }
                }
                
                System.out.println("Total packets received: " + packetsReceived);
            });
            
            receivingThread.start();
            System.out.println("Voice receiving started on port " + udpSocket.getLocalPort());

        } catch (LineUnavailableException e) {
            System.out.println("Speaker not available for call.");
            e.printStackTrace();
        }
    }

    /**
     * Stop receiving audio
     */
    public void stopReceiving() {
        isReceiving = false;
        
        if (speakerLine != null) {
            speakerLine.drain(); // Play remaining buffered audio
            speakerLine.stop();
            speakerLine.close();
        }
        
        if (receivingThread != null) {
            try {
                receivingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("Voice receiving stopped");
    }

    /**
     * Check if currently receiving
     */
    public boolean isReceiving() {
        return isReceiving;
    }
}
