package com.chatapp.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Message {
    
    private String sender;
    private String reciever;
    private String content;
    private MessageType type;
    private String timestamp;
    private String messageId;


    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public Message(String sender, String receiver, String content, MessageType type) {
        this();
        this.sender = sender;
        this.reciever = receiver;
        this.content = content;
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public String getReciever() {
        return reciever;
    }

    public String getContent() {
        return content;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReciever(String reciever) {
        this.reciever = reciever;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormattedMessage() {
        // Formato para comunicación servidor: TIPO:sender:receiver:content
        String typePrefix = (type == MessageType.PRIVATE) ? "PRIVATE_MSG" : "GROUP_MSG";
        return typePrefix + ":" + sender + ":" + reciever + ":" + content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    /**
     * Crea un Message a partir de un comando CLI
     * @param sender El usuario que envía
     * @param command El comando completo (ej: "/msg juan hola" o "/msgg grupo1 hola todos")
     * @return Message object o null si el comando es inválido
     */
    public static Message fromCommand(String sender, String command) {
        if (command == null || !command.startsWith("/")) {
            return null;
        }
        
        String[] parts = command.split(" ", 3); // Divide en máximo 3 partes
        
        if (parts.length < 3) {
            return null; // Comando incompleto
        }
        
        String commandType = parts[0];
        String receiver = parts[1];
        String content = parts[2];
        
        MessageType type;
        if ("/msg".equals(commandType)) {
            type = MessageType.PRIVATE;
        } else if ("/msgg".equals(commandType)) {
            type = MessageType.GROUP;
        } else {
            return null; // Comando no reconocido
        }
        
        return new Message(sender, receiver, content, type);
    }

}