package com.chatapp.client.model;

import java.io.*;
import java.net.Socket;
import java.net.DatagramSocket;
import java.util.Scanner;

public class MessageReciever implements Runnable {

    private Socket socket;
    private DataInputStream dataInputStream;
    private CLI cli; // Reference to CLI for call handling
    private MessageSender messageSender; // For sending responses
    private DatagramSocket udpSocket; // For UDP port info
    private String username; // Current user's name
    private Scanner scanner = new Scanner(System.in);

    public MessageReciever(Socket tcpSocket) {
        this.socket = tcpSocket;
        initializeStreams();
    }

    /**
     * Set CLI reference for handling calls
     */
    public void setCli(CLI cli) {
        this.cli = cli;
    }

    /**
     * Set MessageSender reference for sending call responses
     */
    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    /**
     * Set UDP socket reference for port info
     */
    public void setUdpSocket(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    /**
     * Set username for call responses
     */
    public void setUsername(String username) {
        this.username = username;
    }

    private void initializeStreams() {
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Error initializing streams");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            String message = recieveMsgFromServer();
            if (message != null) {
                // Debug para mensajes de llamadas
                if (message.startsWith("CALL_") || message.startsWith("INCOMING_CALL")) {
                    System.out.println("🟢 CLIENT: Received call message: " + message);
                }
                
                // Detectar tipo de mensaje
                if (message.startsWith("AUDIO_FROM:")) {
                    handleIncomingAudio(message);
                } else if (message.startsWith("GROUP_AUDIO_FROM:")) {
                    handleIncomingGroupAudio(message);
                } else if (message.startsWith("INCOMING_CALL:")) {
                    handleIncomingCall(message);
                } else if (message.startsWith("CALL_ACCEPTED:")) {
                    handleCallAccepted(message);
                } else if (message.startsWith("CALL_REJECTED:")) {
                    handleCallRejected(message);
                } else if (message.startsWith("CALL_ENDED:")) {
                    handleCallEnded(message);
                } else if (message.startsWith("INCOMING_GROUP_CALL:")) {
                    handleIncomingGroupCall(message);
                } else if (message.startsWith("GROUP_CALL_ENDED:")) {
                    handleGroupCallEnded(message);
                } else {
                    System.out.println(message);
                }
            } else {
                System.out.println("Connection closed by server.");
                break;
            }
        }
    }

    public String recieveMsgFromServer() {
        try {
            // Read using DataInputStream.readUTF()
            String message = dataInputStream.readUTF();
            return message;
        } catch (IOException e) {
            System.out.println("Error receiving message from server");
            e.printStackTrace();
            return null;
        } 
    }

    private void handleIncomingAudio(String message) {
        try {
            // Parse: "AUDIO_FROM: juan: audio_123.wav: 50000"
            String[] parts = message.split(": ", 4);
            
            if (parts.length < 4) {
                System.out.println("Invalid audio message format");
                return;
            }
            
            String from = parts[1];
            String audioFileName = parts[2];
            int audioSize = Integer.parseInt(parts[3]);

            System.out.println("Receiving audio from " + from + "...");

            // Read audio bytes using DataInputStream
            byte[] audioData = new byte[audioSize];
            dataInputStream.readFully(audioData);

            // Save audio
            String savedPath = saveAudio(audioFileName, audioData, from);

            // Notify user
            System.out.println("Received audio from " + from + " (" + audioSize + " bytes)");
            System.out.println("Saved to: " + savedPath);
            System.out.println("Play the audio file with your media player");

        } catch (IOException | NumberFormatException e) {
            System.out.println("Error receiving audio");
            e.printStackTrace();
        }
    }

    private void handleIncomingGroupAudio(String message) {
        try {
            // Parse: "GROUP_AUDIO_FROM: juan@amigos: audio_123.wav: 50000"
            String[] parts = message.split(": ", 4);
            
            if (parts.length < 4) {
                System.out.println("Invalid group audio format");
                return;
            }
            
            String fromAndGroup = parts[1];
            String audioFileName = parts[2];
            int audioSize = Integer.parseInt(parts[3]);
            
            // Separate sender and group
            String[] senderGroup = fromAndGroup.split("@", 2);
            String from = senderGroup[0];
            String groupName = senderGroup.length > 1 ? senderGroup[1] : "unknown";

            System.out.println("Receiving group audio from " + from + " in group " + groupName + "...");

            // Read audio bytes using DataInputStream
            byte[] audioData = new byte[audioSize];
            dataInputStream.readFully(audioData);

            // Save audio with group prefix
            String savedPath = saveGroupAudio(audioFileName, audioData, from, groupName);

            // Notify user
            System.out.println("Received group audio from " + from + " in " + groupName + 
                             " (" + audioSize + " bytes)");
            System.out.println("Saved to: " + savedPath);
            System.out.println("Play the audio file with your media player");

        } catch (IOException | NumberFormatException e) {
            System.out.println("Error receiving group audio");
            e.printStackTrace();
        }
    }

    private String saveAudio(String originalFileName, byte[] audioData, String from) {
        try {
            // Create folder
            File receivedDir = new File("audios/received");
            receivedDir.mkdirs();

            // Unique name
            String fileName = from + "_" + originalFileName;
            File audioFile = new File(receivedDir, fileName);

            // Validate WAV header
            if (audioData.length < 44) {
                System.out.println("Error: Audio data too small, expected at least 44 bytes for WAV header");
                return null;
            }

            // Check if it's a valid WAV file
            if (audioData[0] != 'R' || audioData[1] != 'I' || 
                audioData[2] != 'F' || audioData[3] != 'F') {
                System.out.println("Warning: Not a valid WAV file (missing RIFF header)");
            }

            if (audioData[8] != 'W' || audioData[9] != 'A' || 
                audioData[10] != 'V' || audioData[11] != 'E') {
                System.out.println("Warning: Not a valid WAV file (missing WAVE header)");
            }

            // Write file
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(audioData);
            fos.close();

            System.out.println("Audio file saved: " + audioFile.getAbsolutePath());
            System.out.println("File size: " + audioFile.length() + " bytes");

            return audioFile.getAbsolutePath();

        } catch (IOException e) {
            System.out.println("Error saving audio");
            e.printStackTrace();
            return null;
        }
    }

    private String saveGroupAudio(String originalFileName, byte[] audioData, String from, String groupName) {
        try {
            // Create folder for groups
            File receivedDir = new File("audios/received/groups");
            receivedDir.mkdirs();

            // Unique name: group_sender_file
            String fileName = groupName + "_" + from + "_" + originalFileName;
            File audioFile = new File(receivedDir, fileName);

            // Validate WAV header
            if (audioData.length < 44) {
                System.out.println("Error: Audio data too small, expected at least 44 bytes for WAV header");
                return null;
            }

            // Check if it's a valid WAV file
            if (audioData[0] != 'R' || audioData[1] != 'I' || 
                audioData[2] != 'F' || audioData[3] != 'F') {
                System.out.println("Warning: Not a valid WAV file (missing RIFF header)");
            }

            // Write file
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(audioData);
            fos.close();

            System.out.println("Group audio file saved: " + audioFile.getAbsolutePath());
            System.out.println("File size: " + audioFile.length() + " bytes");

            return audioFile.getAbsolutePath();

        } catch (IOException e) {
            System.out.println("Error saving group audio");
            e.printStackTrace();
            return null;
        }
    }
    
    // ============ CALL HANDLING METHODS ============
    
    private void handleIncomingCall(String message) {
        // Parse: "INCOMING_CALL:juan:192.168.1.8:5500"
        String[] parts = message.split(":", 4);
        String caller = parts[1];
        String callerIP = parts[2];
        int callerPort = Integer.parseInt(parts[3]);
        
        System.out.println("\n📞 Incoming call from " + caller);
        System.out.println("Caller IP: " + callerIP + ", Port: " + callerPort);
        System.out.println("Accept call? (yes/no): ");
        
        String response = scanner.nextLine().trim().toLowerCase();
        
        System.out.println("🟢 CLIENT DEBUG: Response = '" + response + "'");
        System.out.println("🟢 CLIENT DEBUG: messageSender = " + (messageSender != null ? "OK" : "NULL"));
        System.out.println("🟢 CLIENT DEBUG: udpSocket = " + (udpSocket != null ? "OK" : "NULL"));
        System.out.println("🟢 CLIENT DEBUG: username = " + (username != null ? username : "NULL"));
        
        if ("yes".equals(response) || "y".equals(response)) {
            System.out.println("🟢 CLIENT: User said YES");
            // Send acceptance to server with our UDP port
            // Format: "CALL_ACCEPT:accepter:caller:accepterPort"
            if (messageSender != null && udpSocket != null && username != null) {
                int myUdpPort = udpSocket.getLocalPort();
                String acceptMessage = "CALL_ACCEPT:" + username + ":" + caller + ":" + myUdpPort;
                System.out.println("🟢 CLIENT: Sending accept message: " + acceptMessage);
                messageSender.setMessageContent(acceptMessage);
                messageSender.sendMsgToServer();
                System.out.println("🟢 CLIENT: ✓ Accept message sent to server");
            } else {
                System.out.println("🔴 CLIENT ERROR: Cannot send CALL_ACCEPT - missing dependencies!");
            }
            
            // Start call - begin receiving and streaming
            if (cli != null) {
                System.out.println("Starting call with IP: " + callerIP + ", Port: " + callerPort);
                cli.startCall(callerIP, callerPort);
            } else {
                System.out.println("🔴 CLIENT ERROR: CLI is null!");
            }
            System.out.println("✓ Call accepted - streaming started");
        } else {
            System.out.println("🟢 CLIENT: User said NO (response was: '" + response + "')");
            // Send rejection to server
            // Format: "CALL_REJECT:rejecter:caller"
            if (messageSender != null && username != null) {
                String rejectMessage = "CALL_REJECT:" + username + ":" + caller;
                messageSender.setMessageContent(rejectMessage);
                messageSender.sendMsgToServer();
            }
            System.out.println("✗ Call rejected");
        }
    }
    
    private void handleCallAccepted(String message) {
        // Parse: "CALL_ACCEPTED:ana:192.168.1.9:5501"
        String[] parts = message.split(":", 4);
        String accepter = parts[1];
        String accepterIP = parts[2];
        int accepterPort = Integer.parseInt(parts[3]);
        
        System.out.println("\n✓ " + accepter + " accepted your call!");
        System.out.println("Accepter IP: " + accepterIP + ", Port: " + accepterPort);
        
        if (cli != null) {
            System.out.println("Starting call with IP: " + accepterIP + ", Port: " + accepterPort);
            cli.startCall(accepterIP, accepterPort);
        } else {
            System.out.println("ERROR: CLI is null, cannot start call");
        }
    }
    
    private void handleCallRejected(String message) {
        String[] parts = message.split(":", 2);
        String rejecter = parts[1];
        
        System.out.println("\n " + rejecter + " rejected your call.");
    }
    
    private void handleCallEnded(String message) {
        String[] parts = message.split(":", 2);
        String ender = parts[1];
        
        System.out.println("\n " + ender + " ended the call.");
        
        if (cli != null) {
            cli.endCall();
        }
    }
    
    private void handleIncomingGroupCall(String message) {
        String[] parts = message.split(":", 4);
        String callerAndGroup = parts[1];
        String callerIP = parts[2];
        int callerPort = Integer.parseInt(parts[3]);
        
        String[] cg = callerAndGroup.split("@");
        String caller = cg[0];
        String groupName = cg.length > 1 ? cg[1] : "unknown";
        
        System.out.println("\n " + caller + " started a group call in " + groupName);
        System.out.println("Join call? (yes/no): ");
        
        String response = scanner.nextLine().trim().toLowerCase();
        
        if ("yes".equals(response) || "y".equals(response)) {
            if (cli != null) {
                cli.startCall(callerIP, callerPort);
            }
            System.out.println(" Joined group call");
        } else {
            System.out.println(" Declined group call");
        }
    }
    
    private void handleGroupCallEnded(String message) {
        String[] parts = message.split(":", 2);
        String enderAndGroup = parts[1];
        
        String[] eg = enderAndGroup.split("@");
        String ender = eg[0];
        String groupName = eg.length > 1 ? eg[1] : "unknown";
        
        System.out.println("\n " + ender + " ended the group call in " + groupName);
        
        if (cli != null) {
            cli.endCall();
        }
    }
}
