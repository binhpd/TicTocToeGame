import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public final class TicTacToeScreen extends JFrame implements Runnable {

    private final JTextField idField; 
    private final JTextArea displayArea;
    private final JPanel boardPanel;
    private final JPanel panel2;
    private final Square[][] board;
    private Square currentSquare;
    private String myMark;
    private boolean myTurn; 
    private final String X_MARK = "X";
    private final String O_MARK = "O";
    
    private SocketClient socketClient = SocketClient.getInstance();

    public TicTacToeScreen(String host, String mark) {
    	myMark = mark;
        displayArea = new JTextArea(4, 30); // set up JTextArea
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.SOUTH);

        boardPanel = new JPanel(); // set up panel for squares in board
        boardPanel.setLayout(new GridLayout(3, 3, 0, 0));
        board = new Square[3][3]; // create board

        // loop over the rows in the board
        for (int row = 0; row < board.length; row++) {
            // loop over the columns in the board
            for (int column = 0; column < board[row].length; column++) {
                // create square
                board[row][column] = new Square(" ", row * 3 + column);
                boardPanel.add(board[row][column]); // add square       
            }
        }

        idField = new JTextField();
        idField.setEditable(false);
        add(idField, BorderLayout.NORTH);

        panel2 = new JPanel(); 
        panel2.add(boardPanel, BorderLayout.CENTER); 
        add(panel2, BorderLayout.CENTER);
        setSize(300, 225); // set size of window
        setVisible(true); // show window

        startClient();
    }

 
    public void startClient() {
    	System.out.print("\nRun client");
        ExecutorService worker = Executors.newFixedThreadPool(1);
        worker.execute(this); // execute client
    }

    @Override
    public void run() {
        try {
        	SwingUtilities.invokeLater(() -> {
	            idField.setText("You are player \"" + myMark + "\"");
	        });
			
        	myTurn = (myMark.equals(X_MARK));
	        
        	String responseLine;
        	System.out.print("\nStart handle message");
        	while ((responseLine = socketClient.input.readLine()) != null) {
				processMessage(responseLine);
			}
	           
	       System.out.print("\nEnd handle message");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.print("\nError");
			e.printStackTrace();
		} 
    }

    // process messages sent to the client
    private void processMessage(String message) {
        // valid move occurred
    	
    	String[] terminal = MainScreen.getTerminal(message);
        switch (terminal[0]) {
        	case "message":
        		displayMessage(terminal[2]);
        		break;
            case "move":
                int location = Integer.valueOf(terminal[2]); // get move location
                int row = location / 3; // calculate row
                int column = location % 3; // calculate column
                setMark(board[row][column],
                        (myMark.equals(X_MARK) ? O_MARK : X_MARK)); // mark move  
                displayMessage("Opponent moved. Your turn.\n");
                myTurn = true; // now this client's turn
                break;
        }
    }

    // manipulate displayArea in event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(() -> {
            displayArea.append(messageToDisplay); // updates output
        });
    }

    // utility method to set mark on board in event-dispatch thread
    private void setMark(final Square squareToMark, final String mark) {
        SwingUtilities.invokeLater(() -> {
            squareToMark.setMark(mark); // set mark in square
        });
    }

    // send message to server indicating clicked square
    public void sendClickedSquare(int location) {
        // if it is my turn
        if (myTurn) {
        	setMark(currentSquare, myMark);
        	System.out.print("\nClick on " + location);
            socketClient.output.println("move " + location + " null");
            myTurn = false; // not my turn any more
        }
    }

    // set current Square
    public void setCurrentSquare(Square square) {
        currentSquare = square; // set current square to argument
    }

    // private inner class for the squares on the board
    private class Square extends JPanel {

        private String mark; // mark to be drawn in this square
        private final int location; // location of square

        public Square(String squareMark, int squareLocation) {
            mark = squareMark; // set mark for this square
            location = squareLocation; // set location of this square

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setCurrentSquare(Square.this); // set current square

                    // send location of this square
                    sendClickedSquare(getSquareLocation());
                }
            });
        }


        @Override
        public Dimension getPreferredSize() {
            return new Dimension(30, 30); // return preferred size
        }

        // return minimum size of Square
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize(); // return preferred size
        }


        public void setMark(String newMark) {
            mark = newMark; // set mark of square
            repaint(); // repaint square
        }


        public int getSquareLocation() {
            return location; // return location of square
        }


        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawRect(0, 0, 29, 29); // draw square
            g.drawString(mark, 11, 20); // draw mark   
        }
    }

}
