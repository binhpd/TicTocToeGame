import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SocketClient {
	
	private static final int PORT_NUMBER = 1111;

	private static Socket socket = null;

	public static PrintStream output = null;
	
	public static DataInputStream input = null;

    private static SocketClient socketClient;
    
    private static boolean isInitialize = false;
    
    public static SocketClient getInstance() {
    	if (socketClient == null) {
    		socketClient = new SocketClient();
    	}
    	
    	return socketClient;
    }
    
    
    // init all connect, call this method when start application
    public static final void initialize() {
    	if (isInitialize) {
    		return;
    	}
    	startClient();
    }
    
    
    // start the client thread
    public static void startClient() {
        // connect to server and get streams
    	try {
    		socket = new Socket("127.0.0.1", PORT_NUMBER);
			output = new PrintStream(socket.getOutputStream());
			input = new DataInputStream(socket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host ");
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host ");
		}
    }
    
}
