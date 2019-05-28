
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
 
public class MainScreen extends JFrame implements Runnable {
    
	private static final int PORT_NUMBER = 1111;
	private SocketClient socketClient = null;
	private boolean stop = false;
	
    // login
    private JTextField inputAccount;
	private JButton login;
	
	// home form
	private JLabel accountName = new JLabel();
	JButton createRoomBtn = new JButton("Create Room");
    private JList<String> activeList;
	private JLabel activeField;
	
    
    public MainScreen() {
    	SocketClient.initialize();
    	socketClient = SocketClient.getInstance();
    	
    	setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
    	
    	initLoginForm();
    	initMainForm();
    	
    	//---------------
    	showLogin();
         
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("TicTocToe Client");       
        this.setSize(400,600);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
       
        int portNumber = PORT_NUMBER;
		String host = "localhost";
		 // create and start worker thread for this client
        ExecutorService worker = Executors.newFixedThreadPool(1);
        worker.execute(this); // execute client
    }
    
    @Override
    public void run() {
    	String responseLine;
    	System.out.println("\nHandle message from server");
		try {
			while (!stop && (responseLine = socketClient.input.readLine()) != null) {
				System.out.println(responseLine);
				processMessage(responseLine);
			}
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
    	System.out.println("\nEnd");
	}
    
    private void showLogin() {
    	showLoginForm(true);
    	showMainForm(false);
    }
    
    private void showHomeScreen() {
    	showLoginForm(false);
    	showMainForm(true);
    }
    
    private void initLoginForm() {
    	inputAccount = new JTextField();
    	login = new JButton("Login");
    	login.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (inputAccount.getText() != null && !inputAccount.getText().equals("")) {
					login();				
				}
			}
		});
    	add(inputAccount);
    	add(login);
    }
    
    private void initMainForm() {
    	
    	add(accountName);
    	
    	createRoomBtn = new JButton("Create Room");
    	createRoomBtn.setBounds(50, 150, 100, 30);
    	createRoomBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				stop = true;
				socketClient.output.println("create_room " + inputAccount.getText() + " null");
				TicTacToeScreen appllication = new TicTacToeScreen("127.0.0.1", "X");
				appllication.setDefaultCloseOperation(EXIT_ON_CLOSE);
				
			}
		});
    	
    	//add button to the frame
    	add(createRoomBtn);
//    	add(joinRoomBtn);
    	
    	activeField = new JLabel("Active Room");
    	add(activeField);
    
        //create the list
        activeList = new JList<>();
        add(activeList);
    }
    
    private void showLoginForm(boolean visiable) {
    	inputAccount.setVisible(visiable);
    	login.setVisible(visiable);
    }
    
    private void showMainForm(boolean visiable) {
    	activeField.setVisible(visiable);
    	createRoomBtn.setVisible(visiable);
    	activeList.setVisible(visiable); 
    }
    
    private void login() {
    	System.out.print("\nHello: " + inputAccount.getText());
    	socketClient.output.println(inputAccount.getText());
    	accountName.setText("Account Name: " + inputAccount.getText());
    	showHomeScreen();
    }
    
    private void logout() {
    	showLoginForm(true);
    	showMainForm(false);    	
    }
    
    private void processMessage(String message) {
    	String [] terminal = getTerminal(message);
 
        // valid move occurred
        switch (terminal[0]) {
            case "get_rooms":
        		String[] rooms = terminal[2].split(" ");
        		System.out.print("\nRoom:" + rooms.length);
        		bindActiveList(rooms);
                break;
        }
    }
    
    private void bindActiveList(String[] rooms) {
    	DefaultListModel models = new DefaultListModel();
    	for (String room: rooms) {
        	models.addElement(room);
    	}
    	activeList.setModel(models);
    	
    	MouseListener mouseListener = new MouseAdapter() {
    		public void mouseClicked(MouseEvent mouseEvent) {
    	        JList<String> theList = (JList) mouseEvent.getSource();
    	        int index = theList.locationToIndex(mouseEvent.getPoint());
	  	        if (index >= 0) {
	  	            String o = theList.getModel().getElementAt(index);
	  	            if (o.indexOf("-") > 0) {
	  	            	return;
	  	            }
	  	            stop = true;
	  	            String roomId = o.substring(0, o.indexOf("("));
					socketClient.output.println("join_room " + roomId + " null");
					TicTacToeScreen appllication = new TicTacToeScreen("127.0.0.1", "O");
					appllication.setDefaultCloseOperation(EXIT_ON_CLOSE);      
	  	        }
    	     }
    	 };
    	 activeList.addMouseListener(mouseListener);
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
		System.out.print("\nTerminal: " + terminals[0] + "|" + terminals[1] + "|" + terminals[2]);
		return terminals;
	}
     
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainScreen();
            }
        });
    }   
}