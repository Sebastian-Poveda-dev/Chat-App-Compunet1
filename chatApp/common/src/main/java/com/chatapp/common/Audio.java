package com.chatapp.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Audio {
    private String sender;
    private String receiver;
    private AudioType type;
    private String audioId;
    private String timestamp;
    private String audioFileName; 

    public Audio() {
        this.audioId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Audio(String sender, String receiver, AudioType type) {
        this();
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public AudioType getType() {
        return type;
    }

    public void setType(AudioType type) {
        this.type = type;
    }

    public String getAudioId() {
        return audioId;
    }

    public void setAudioId(String audioId) {
        this.audioId = audioId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedAudio(int audioSize) {
        String typePrefix = (type == AudioType.PRIVATE) ? "PRIVATE_AUDIO" : "GROUP_AUDIO";
        return typePrefix + ":" + sender + ":" + receiver + ":" + audioFileName + ":" + audioSize;
    }

    public String getAudioFileName() {
        return audioFileName;
    }

    public void setAudioFileName(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    public static Audio fromCommand(String sender, String command) {
        if (command == null || (!command.startsWith("/"))) {
            return null;
        }

        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            return null;
        }
        String commandType = parts[0];
        String receiver = parts[1];

        AudioType type;
        if ("/audiomsg".equals(commandType)) {
            type = AudioType.PRIVATE;
        } else if ("/audiomsgg".equals(commandType)) {
            type = AudioType.GROUP;
        } else {
            return null; 
        }

        return new Audio(sender, receiver, type);
    }
    
}
