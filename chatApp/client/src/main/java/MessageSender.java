

public class MessageSender {

    private Socket socket;
    private String messageContent;

    public MessageSender(Socket tcpSocket) {
        this.socket = tcpSocket;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public void sendMsgToServer() {
        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);

        bw.write(messageContent);
        bw.flush();

    }


    
}
