import java.net.Socket;

public class Room {
	public String roomId = "";
	public String owner = "";
	public ClientThread ownerThread;
	public String player = "";
	public ClientThread playerThread;	
	
	public Room(){}
	
	public boolean isWaiting() {
		return player.equals("");
	}
}
