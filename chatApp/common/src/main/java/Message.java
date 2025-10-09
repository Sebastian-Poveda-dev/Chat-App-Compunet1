public class Message {
    private String sender;
    private String reciever;
    private String content;


    public Message() {
        
    }

    private String getSender() {
        return sender;
    }

    private String getReciever() {
        return reciever;
    }

    private String getContent() {
        return content;
    }

    private void setSender(String sender) {
        this.sender = sender;
    }

    private void setReciever(String reciever) {
        this.reciever = reciever;
    }

    private void setContent(String content) {
        this.content = content;
    }
}