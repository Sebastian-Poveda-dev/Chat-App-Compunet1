public class MessageReciever {

    private Socket socket;

    public MessageReciever(Socket tcpSocket) {
        this.socket = tcpSocket;
    }

    public String recieveMsgFromServer() {
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String message = br.readLine();
        return message;
    }
    
}
