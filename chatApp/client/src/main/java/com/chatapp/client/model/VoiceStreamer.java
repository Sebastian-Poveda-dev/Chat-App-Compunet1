package com.chatapp.client.model;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Captures audio from microphone and sends it via UDP in real-time
 */
public class VoiceStreamer {
    
    private AudioFormat audioFormat;
    private TargetDataLine microphoneLine;
    private Thread streamingThread;
    private boolean isStreaming;
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private int serverPort;
    private double gain; // Volume control (0.0 to 1.0)

    public VoiceStreamer(DatagramSocket udpSocket) {
        // Audio format: 8000 Hz, 16-bit, mono (optimized for voice over network)
        this.audioFormat = new AudioFormat(8000, 16, 1, true, true);
        this.udpSocket = udpSocket;
        this.isStreaming = false;
        this.gain = 0.07; // Same as AudioRecorder for consistency
    }

    /**
     * Start streaming audio to the specified destination
     * @param address Server/peer IP address
     * @param port Server/peer UDP port
     */
    public void startStreaming(InetAddress address, int port) {
        this.serverAddress = address;
        this.serverPort = port;

        try {
            microphoneLine = AudioSystem.getTargetDataLine(audioFormat);
            microphoneLine.open(audioFormat);
            microphoneLine.start();

            isStreaming = true;

            streamingThread = new Thread(() -> {
                System.out.println("🎤 VoiceStreamer thread started - Capturing from microphone...");
                byte[] buffer = new byte[512]; // Small buffer for low latency
                int packetsSent = 0;
                long lastReportTime = System.currentTimeMillis();
                
                while (isStreaming) {
                    int bytesRead = microphoneLine.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        // Apply gain reduction to prevent saturation
                        applyGainReduction(buffer, bytesRead, gain);
                        
                        try {
                            // Send audio packet via UDP
                            DatagramPacket packet = new DatagramPacket(
                                buffer, bytesRead, serverAddress, serverPort
                            );
                            udpSocket.send(packet);
                            packetsSent++;
                            
                            // Report every 2 seconds
                            long now = System.currentTimeMillis();
                            if (now - lastReportTime > 2000) {
                                System.out.println("📤 Sent " + packetsSent + " audio packets");
                                lastReportTime = now;
                            }
                            
                        } catch (IOException e) {
                            if (isStreaming) {
                                System.out.println("Error sending voice packet: " + e.getMessage());
                            }
                        }
                    }
                }
                
                System.out.println("Total packets sent: " + packetsSent);
            });
            
            streamingThread.start();
            System.out.println("Voice streaming started to " + address + ":" + port);

        } catch (LineUnavailableException e) {
            System.out.println("Microphone not available for call.");
            e.printStackTrace();
        }
    }

    /**
     * Stop streaming audio
     */
    public void stopStreaming() {
        isStreaming = false;
        
        if (microphoneLine != null) {
            microphoneLine.stop();
            microphoneLine.close();
        }
        
        if (streamingThread != null) {
            try {
                streamingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("Voice streaming stopped");
    }

    /**
     * Applies gain reduction to audio samples to prevent saturation
     * @param buffer The audio buffer (16-bit big-endian samples)
     * @param length Number of valid bytes in the buffer
     * @param gain Gain multiplier (0.0 to 1.0)
     */
    private void applyGainReduction(byte[] buffer, int length, double gain) {
        // Process samples in pairs (16-bit = 2 bytes)
        for (int i = 0; i < length - 1; i += 2) {
            // Read 16-bit sample (big-endian format)
            int sample = (buffer[i] << 8) | (buffer[i + 1] & 0xFF);
            
            // Apply gain
            sample = (int)(sample * gain);
            
            // Clamp to prevent overflow
            if (sample > 32767) sample = 32767;
            if (sample < -32768) sample = -32768;
            
            // Write back to buffer
            buffer[i] = (byte)((sample >> 8) & 0xFF);
            buffer[i + 1] = (byte)(sample & 0xFF);
        }
    }

    /**
     * Check if currently streaming
     */
    public boolean isStreaming() {
        return isStreaming;
    }

    /**
     * Set the gain (volume) level
     * @param gain Gain multiplier (0.0 to 1.0), typical values: 0.05 - 0.15
     */
    public void setGain(double gain) {
        this.gain = Math.max(0.0, Math.min(1.0, gain));
        System.out.println("Voice gain set to: " + this.gain);
    }
}
