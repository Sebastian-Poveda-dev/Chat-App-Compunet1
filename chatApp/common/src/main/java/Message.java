public class Message {
    private String sender;
    private String reciever;
    private String content;


    public Message() {
        
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
        return sender + ':' + content + ':' + reciever;
    }

}