
import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.net.ServerSocket;

public class GameServerSync {
	
	public static final String CREATE_ROOM = "create_room";
	public static final String CLOSE = "close";
	public static final String JOIN_ROOM = "join_room";
	public static final String PLAY = "play";
	public static final String GET_ROOMS = "get_rooms";
	public static final String MESSAGE = "message";
	public static final String MOVE = "move";
	
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;

	private static final int PORT_NUMBER = 1111;
	private static final int maxClientsCount = 10;
	private static final ClientThread[] threads = new ClientThread[maxClientsCount];
	public static final ArrayList<Room> rooms = new ArrayList();


	public static void main(String args[]) {
		int portNumber = PORT_NUMBER;
		if (args.length == 1) {
			portNumber = Integer.valueOf(args[0]).intValue();
		}

		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		while (true) {
			try {
				clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new ClientThread(clientSocket, threads)).start();
						break;
					}
				}
				if (i == maxClientsCount) {
					PrintStream os = new PrintStream(clientSocket.getOutputStream());
					os.println("Server too busy. Try later.");
					os.close();
					clientSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

class ClientThread extends Thread {

	private String clientName = null;
	private DataInputStream is = null;
	private PrintStream os = null;
	private Socket clientSocket = null;
	private final ClientThread[] threads;
	private int maxClientsCount;

	private static final String END_TERMINAL = "@EXIT";

	public ClientThread(Socket clientSocket, ClientThread[] threads) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxClientsCount = threads.length;
	}

	public void run() {
		int maxClientsCount = this.maxClientsCount;
		ClientThread[] threads = this.threads;

		try {
			is = new DataInputStream(clientSocket.getInputStream());
			os = new PrintStream(clientSocket.getOutputStream());
			String name;
			while (true) {
				name = is.readLine().trim();
				if (name.indexOf('@') == -1 && name.indexOf(" ") == -1) {
					break;
				} else {
					os.println("message null The name should not contain space or '@' character.");
				}
			}

			System.out.print(name + " logined\n");
			
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = "" + name;
						break;
					}
				}
				
//				dispatcherNewLoginToAllUsers(maxClientsCount, threads, name);
			}
			
			handleSendMessage(maxClientsCount, threads, name);
			
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this && threads[i].clientName != null) {
						threads[i].os.println("*** The user " + name + " is leaving the chat room ***");
					}
				}
			}
			os.println("*** Bye " + name + " ***");

			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			is.close();
			os.close();
			clientSocket.close();
		} catch (IOException e) {
		}
	}
	
	synchronized private void dispatcherActiveRoom() {
		System.out.print("\nNumber active: " + GameServerSync.rooms.size());
		if (GameServerSync.rooms.size() == 0) {
			return;
		}
		
		for (int i = 0; i < maxClientsCount; i++) {			
			if (threads[i] != null) {
				String activesRoom = getActiveRooms(threads[i].clientName);
				threads[i].os.println(GameServerSync.GET_ROOMS + " null " + activesRoom);
			}
		}
	}
	
	private String getActiveRooms(String clientName) {
		StringBuilder builder = new StringBuilder();
		boolean isAddSpace = false;
		System.out.print("\nClient name: "  + clientName);
		for (int i = 0; i < GameServerSync.rooms.size(); i++) {
			
			Room room = GameServerSync.rooms.get(i);
			if (!clientName.equals(room.owner)) {
				if (isAddSpace) {
					builder.append(" ");
				}
				builder.append(room.owner + (room.player.equals("") ? "(waiting)" : "-" + room.player + "(playing)"));
				isAddSpace = true;
			}
		}
		
		return builder.toString();
	}

	private void handleSendMessage(int maxClientsCount, ClientThread[] threads, String name) throws IOException {
		while (true) {
			String line = is.readLine();
			System.out.print("\nTerminal: " + line);
			if (line.startsWith(END_TERMINAL)) {
				break;
			}
			
			String[] terminals = getTerminal(line);
			
			if (terminals[0].equals(GameServerSync.CREATE_ROOM)) {
				System.out.print("\nHandle create room");
				Room room = new Room();
				room.owner = terminals[1];
				room.ownerThread = this;
				GameServerSync.rooms.add(room);
//				
				dispatcherActiveRoom();
			} else if (terminals[0].equals(GameServerSync.JOIN_ROOM)) {
				System.out.print("\nHandle join room");
				for (Room room: GameServerSync.rooms) {
					if (room.owner.equals(terminals[1])) {
						room.player = clientName;
						room.playerThread = this;
					}
				}
				dispatcherActiveRoom();
				
				notiReadyForPlay(terminals[1]);
		
			} else if (terminals[0].equals(GameServerSync.MOVE)) {
				System.out.print("\nHandle play");
				sendMove(clientName, terminals[1]);
			}
			
		}
	}
	
	private void sendMove(String roomId, String location) {
		Room room = getRoomByID(roomId);		
		if (room == null) {
			return;
		}
		
		ClientThread player = getPlayer(room);
		player.os.println(GameServerSync.MOVE + " null " + location);
	}
	
	private ClientThread getPlayer(Room room) {
		return clientName.equals(room.owner) ? room.playerThread : room.ownerThread;
	}
	private void notiReadyForPlay(String roomId) {
		Room room = getRoomByID(roomId);		
		if (room == null) {
			return;
		}
		System.out.print("\nFound room");
		room.ownerThread.os.println(GameServerSync.MESSAGE + " null " + clientName + " joined game");
		
	}
	
	private Room getRoomByID(String roomId) {
		for (Room room : GameServerSync.rooms) {
			if (room.owner.equals(roomId) || (room.player != null && room.player.equals(roomId))) {
				return room;
			}
		}
		return null;
	}
	
	public static final String[] getTerminal(String line) {
		String[] terminals = new String[3]; 
		int index = 0;
		index = line.indexOf(" ");
		String prams1 = line.substring(0, index);
		terminals[0] = prams1;
		
		int index2 = line.indexOf(" ", index + 1);
		String prams2 = index2 != -1 ? line.substring(index + 1, index2) : line.substring(index+1);
		terminals[1] = prams2;
		
		int index3 = line.indexOf(" ", index2 + 1);
		String prams3 = line.substring(index2+1);
		terminals[2] = prams3;
		System.out.print("Terminal " + terminals[0] + "|" + terminals[1] + "|" + terminals[2]);
		return terminals;
	}
	
	private void dispatcherNewRoomToAllUsers(int maxClientsCount, ClientThread[] threads, String name) {
		for (int i = 0; i < maxClientsCount; i++) {
			if (threads[i] != null && threads[i] != this) {
				threads[i].os.println("*** User '" + name + "' created new game room !!! ***");
			}
		}
	}

	private void sendPublicMessage(int maxClientsCount, ClientThread[] threads, String name, String line) {
		synchronized (this) {
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null && threads[i].clientName != null) {
					threads[i].os.println("<" + name + "> " + line);
				}
			}
		}
	}

	private void sendPrivateMessage(int maxClientsCount, ClientThread[] threads, String name, String line) {
		String[] words = line.split("\\s", 2);
		if (words.length > 1 && words[1] != null) {
			words[1] = words[1].trim();
			if (!words[1].isEmpty()) {
				synchronized (this) {
					for (int i = 0; i < maxClientsCount; i++) {
						if (threads[i] != null && threads[i] != this && threads[i].clientName != null
								&& threads[i].clientName.equals(words[0])) {
							threads[i].os.println("<" + name + "> " + words[1]);
							this.os.println(">" + name + "> " + words[1]);
							break;
						}
					}
				}
			}
		}
	}

	private boolean isPrivateMessage(String line) {
		return line.startsWith("@");
	}
}