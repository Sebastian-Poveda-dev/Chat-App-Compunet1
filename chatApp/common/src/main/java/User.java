public class User {

    private String userId;
    private String userName;

    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;    
    }

    public User(String userId) {
        this.userId = userId;
        this.userName = "Unknown"; 
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
