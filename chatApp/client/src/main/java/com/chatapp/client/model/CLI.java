package com.chatapp.client.model;

import java.util.Scanner;
import java.net.DatagramSocket;

import com.chatapp.common.Message;
import com.chatapp.common.Audio;
import com.chatapp.common.Call;
import com.chatapp.common.CallType;

public class CLI {

    private String currentUser;
    private MessageSender messageSender;
    private AudioRecorder audioRecorder;
    private AudioSender audioSender;
    private VoiceStreamer voiceStreamer;
    private VoiceReceiver voiceReceiver;
    private DatagramSocket udpSocket;
    private Scanner scanner = new Scanner(System.in);
    private boolean inCall = false;
    private Call currentCall = null;

    public CLI(String currentUser, MessageSender messageSender, AudioRecorder audioRecorder, 
               AudioSender audioSender, VoiceStreamer voiceStreamer, VoiceReceiver voiceReceiver,
               DatagramSocket udpSocket) {
        this.currentUser = currentUser;
        this.messageSender = messageSender;
        this.audioRecorder = audioRecorder;
        this.audioSender = audioSender;
        this.voiceStreamer = voiceStreamer;
        this.voiceReceiver = voiceReceiver;
        this.udpSocket = udpSocket;
    }

    public void displayCommands() {
        System.out.println("Available commands:");
        System.out.println("/msg <username> <message> - Send a direct message to a user");
        System.out.println("/msgg <groupname> <message> - Send a message to a group");
        System.out.println("/audiomsg <username> - Send an audio message to a user");
        System.out.println("/audiomsgg <groupname> - Send an audio message to a group");
        System.out.println("/call <username> - Start a voice call with a user");
        System.out.println("/callg <groupname> - Start a group voice call");
        System.out.println("/endcall - End the current call");
        System.out.println("/createg <groupname> <user1 , user2, user3...> - Create a new group");
        System.out.println("/joing <groupname> - Join an existing group");
        System.out.println("/leaveg <groupname> - Leave a group");
        System.out.println("/listg - List all groups you are a member of");
        System.out.println("/removeg <groupname> <user1 , user2, user3...> - Remove users from a group");
        System.out.println("/addg <groupname> <user1 , user2, user3...> - Add users to a group");
        System.out.println("/listu - List all users currently online");
        System.out.println("/exit - Exit the application");
    }

    public void executeCommand(String command) {
        // Implementation for executing commands

        if (command.startsWith("/msg") || command.startsWith("/msgg")) {
            msgExe(command);
        } else if (command.startsWith("/createg")) {
            createGroupExe(command);
        } else if (command.startsWith("/joing")) {
            joinGroupExe(command);
        } else if (command.startsWith("/audiomsg") || command.startsWith("/audiomsgg")) {
            audioMsgExe(command);
        } else if (command.startsWith("/call") && !command.startsWith("/callg")) {
            callExe(command);
        } else if (command.startsWith("/callg")) {
            callExe(command);
        } else if (command.equals("/endcall")) {
            endCallExe();
        }
    }

    private void msgExe(String command) {
        Message msg = Message.fromCommand(currentUser, command);
        if (msg != null) {
            messageSender.setMessageContent(msg.getFormattedMessage());
            messageSender.sendMsgToServer();
        }
    }

    private void createGroupExe(String command) {
        String[] commandParts = command.split(" ", 3);
        if (commandParts.length >= 2) {
            String groupName = commandParts[1];
            String members = commandParts.length > 2 ? commandParts[2] : "";
            String createGroupCommand = "CREATE_GROUP:" + groupName + ":" + currentUser + ":" + members;
            messageSender.setMessageContent(createGroupCommand);
            messageSender.sendMsgToServer();
        } else {
            System.out.println("Usage: /createg <groupname> <user1 , user2, user3...>");
        }
    }

    private void joinGroupExe(String command) {
        String[] commandParts = command.split(" ", 2);
        if (commandParts.length == 2) {
            String groupName = commandParts[1];
            String joinGroupCommand = "JOIN_GROUP:" + groupName + ":" + currentUser;
            messageSender.setMessageContent(joinGroupCommand); 
            messageSender.sendMsgToServer();
        } else {
            System.out.println("Usage: /joing <groupname>");
        }
    }

    // TODO leaveg, listg, removeg, addg, listu, exit

    private void audioMsgExe(String command) {
        // Parse command and create audio from command
        Audio audio = Audio.fromCommand(currentUser, command);

        if (audio == null) {
            System.out.println("Invalid audio command. Use /audiomsg <username> or /audiomsgg <groupname>");
            return;
        }

        // Determine if it's private or group
        String audioType = (audio.getType() == com.chatapp.common.AudioType.PRIVATE) ? "private" : "group";
        String recipient = audio.getReceiver();

        // Start recording
        audioRecorder.startRecording();
        System.out.println("Recording " + audioType + " audio for " + recipient + "... Press ENTER to stop.");

        // Wait for user to press ENTER to stop recording
        scanner.nextLine();

        // Stop recording
        audioRecorder.stopRecording();

        // Save audio to file and get filename
        String audioFileName = audioRecorder.saveAudio();
        audio.setAudioFileName(audioFileName);

        // Get audio bytes
        byte[] audioData = audioRecorder.getAudioBytes();

        // Send audio metadata to server (includes size)
        String audioMetadata = audio.getFormattedAudio(audioData.length);
        System.out.println("Sending " + audioType + " audio to " + recipient + "...");
        System.out.println("Audio size: " + audioData.length + " bytes");
        
        messageSender.setMessageContent(audioMetadata);
        messageSender.sendMsgToServer();

        // Send audio bytes to server
        audioSender.sendAudioBytes(audioData);
        System.out.println("✓ Audio sent successfully!");
        
        // Reset recording buffer for next audio
        audioRecorder.resetRecording();
    }

    private void callExe(String command) {
        if (inCall) {
            System.out.println("You are already in a call. Use /endcall to end it first.");
            return;
        }

        // Get UDP port
        int udpPort = udpSocket.getLocalPort();
        
        // Create call object from command
        Call call = Call.fromCommand(currentUser, command, udpPort);
        
        if (call == null) {
            System.out.println("Invalid call command. Use /call <username> or /callg <groupname>");
            return;
        }

        // Save current call
        currentCall = call;
        
        // Send call request to server
        String callRequest = call.getFormattedCallRequest();
        messageSender.setMessageContent(callRequest);
        messageSender.sendMsgToServer();
        
        String callType = (call.getType() == com.chatapp.common.CallType.PRIVATE) ? "private" : "group";
        System.out.println("📞 Calling " + call.getReceiver() + " (" + callType + ")...");
        System.out.println("Waiting for response...");
    }

    private void endCallExe() {
        if (!inCall) {
            System.out.println("You are not in a call.");
            return;
        }

        System.out.println("📵 Ending call...");

        // Stop voice streaming and receiving
        if (voiceStreamer != null && voiceStreamer.isStreaming()) {
            voiceStreamer.stopStreaming();
        }
        if (voiceReceiver != null && voiceReceiver.isReceiving()) {
            voiceReceiver.stopReceiving();
        }

        // Notify server
        if (currentCall != null) {
            String callEnd = currentCall.getFormattedCallEnd();
            messageSender.setMessageContent(callEnd);
            messageSender.sendMsgToServer();
        }

        inCall = false;
        currentCall = null;
        System.out.println("📵 Call ended. You can now send messages or make another call.");
        System.out.print(currentUser + "> "); // Show prompt immediately
        System.out.flush();
    }

    /**
     * Called by MessageReceiver when call is accepted
     */
    public void startCall(String peerAddress, int peerPort) {
        try {
            System.out.println("=== STARTING CALL ===");
            System.out.println("Peer Address: " + peerAddress);
            System.out.println("Peer UDP Port: " + peerPort);
            System.out.println("My UDP Port: " + udpSocket.getLocalPort());
            
            inCall = true;
            
            // Start receiving audio
            System.out.println("Starting voice receiver...");
            voiceReceiver.startReceiving();
            
            // Start streaming audio to peer
            System.out.println("Starting voice streamer to " + peerAddress + ":" + peerPort);
            java.net.InetAddress address = java.net.InetAddress.getByName(peerAddress);
            voiceStreamer.startStreaming(address, peerPort);
            
            System.out.println("📞 Call connected! Speak now. Type /endcall to hang up.");
            System.out.println("=====================");
            
        } catch (java.net.UnknownHostException e) {
            System.out.println("Error: Cannot resolve peer address: " + peerAddress);
            e.printStackTrace();
            inCall = false;
        }
    }

    /**
     * Called by MessageReceiver when call ends
     */
    /**
     * Called by MessageReceiver when the other user ends the call
     */
    public void endCall() {
        if (voiceStreamer != null && voiceStreamer.isStreaming()) {
            voiceStreamer.stopStreaming();
        }
        if (voiceReceiver != null && voiceReceiver.isReceiving()) {
            voiceReceiver.stopReceiving();
        }
        inCall = false;
        currentCall = null;
        System.out.println("\n📵 Call ended by other user.");
        System.out.print(currentUser + "> ");
        System.out.flush();
    }

    /**
     * Check if currently in a call
     */
    public boolean isInCall() {
        return inCall;
    }
}
        
    
    



    
    


