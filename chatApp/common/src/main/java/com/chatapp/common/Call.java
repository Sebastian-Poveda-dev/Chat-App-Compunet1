package com.chatapp.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Call {
    private String caller;
    private String receiver; // username or group name
    private CallType type;
    private String callId;
    private String timestamp;
    private int udpPort; // Puerto UDP del cliente para recibir audio

    public Call() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Call(String caller, String receiver, CallType type, int udpPort) {
        this();
        this.caller = caller;
        this.receiver = receiver;
        this.type = type;
        this.udpPort = udpPort;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public CallType getType() {
        return type;
    }

    public void setType(CallType type) {
        this.type = type;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    /**
     * Format call request to send to server
     * @return Formatted string like "CALL_REQUEST:juan:ana:5500" or "GROUP_CALL_REQUEST:juan:amigos:5500"
     */
    public String getFormattedCallRequest() {
        String typePrefix = (type == CallType.PRIVATE) ? "CALL_REQUEST" : "GROUP_CALL_REQUEST";
        return typePrefix + ":" + caller + ":" + receiver + ":" + udpPort;
    }

    /**
     * Format call acceptance
     * @return Formatted string like "CALL_ACCEPT:ana:juan:5501"
     */
    public String getFormattedCallAccept() {
        return "CALL_ACCEPT:" + receiver + ":" + caller + ":" + udpPort;
    }

    /**
     * Format call rejection
     */
    public String getFormattedCallReject() {
        return "CALL_REJECT:" + receiver + ":" + caller;
    }

    /**
     * Format call end
     */
    public String getFormattedCallEnd() {
        String typePrefix = (type == CallType.PRIVATE) ? "CALL_END" : "GROUP_CALL_END";
        return typePrefix + ":" + caller + ":" + receiver;
    }

    /**
     * Parse /call or /callg command
     * @param caller Username of person making the call
     * @param command Command string like "/call ana" or "/callg amigos"
     * @param udpPort UDP port for receiving audio
     * @return Call object or null if invalid
     */
    public static Call fromCommand(String caller, String command, int udpPort) {
        if (command == null || !command.startsWith("/")) {
            return null;
        }

        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            return null;
        }

        String commandType = parts[0];
        String receiver = parts[1].trim();

        CallType type;
        if ("/call".equals(commandType)) {
            type = CallType.PRIVATE;
        } else if ("/callg".equals(commandType)) {
            type = CallType.GROUP;
        } else {
            return null;
        }

        return new Call(caller, receiver, type, udpPort);
    }
}
