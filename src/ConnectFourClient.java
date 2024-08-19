import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ConnectFourClient extends JFrame implements Runnable {
    private Socket socket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private int playerNumber;
    private boolean myTurn;
    private boolean gameOver = false;
    private Color[][] grid = new Color[6][7];

    private JPanel boardPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JLabel titleLabel;
    private JLabel turnLabel;

    public ConnectFourClient(String serverAddress) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            socket = new Socket(serverAddress, 8000);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());

            playerNumber = fromServer.readInt();
            myTurn = (playerNumber == 1);

            initializeGrid(); // O(m * n)

            setupFrame();

            new Thread(this).start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setupFrame() {
        setTitle("Connect Four - Player " + (playerNumber == 1 ? "Red" : "Yellow"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane); // O(1)

        titleLabel = new JLabel("Connect Four", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(new Color(52, 152, 219));

        turnLabel = new JLabel(getTurnText(), SwingConstants.CENTER);
        turnLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        turnLabel.setForeground(new Color(39, 174, 96));

        boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) { // O(m * n)
                super.paintComponent(g);
                drawGrid(g); // O(m * n)
            }
        };
        boardPanel.setPreferredSize(new Dimension(420, 360));
        boardPanel.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true)); // O(1)
        boardPanel.setBackground(Color.WHITE); // O(1)

        boardPanel.addMouseListener(new MouseAdapter() { // O(1)
            @Override
            public void mouseClicked(MouseEvent e) { // O(1)
                if (myTurn && !gameOver) { // O(1)
                    int column = e.getX() / 60; // O(1)
                    if (column >= 0 && column < 7) { // O(1)
                        sendMove(column); // O(1)
                    }
                }
            }
        });

        chatArea = new JTextArea(); // O(1)
        chatArea.setEditable(false); // O(1)
        chatArea.setLineWrap(true); // O(1)
        chatArea.setWrapStyleWord(true); // O(1)
        chatArea.setBackground(new Color(236, 240, 241)); // O(1)
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14)); // O(1)

        chatInput = new JTextField(); // O(1)
        chatInput.setFont(new Font("SansSerif", Font.PLAIN, 14)); // O(1)
        chatInput.setBackground(new Color(236, 240, 241)); // O(1)
        chatInput.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true)); // O(1)

        chatInput.addActionListener(new ActionListener() { // O(1)
            @Override
            public void actionPerformed(ActionEvent e) { // O(1)
                sendChatMessage(); // O(1)
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout()); // O(1)
        topPanel.add(titleLabel, BorderLayout.NORTH); // O(1)
        topPanel.add(turnLabel, BorderLayout.SOUTH); // O(1)

        JPanel boardContainer = new JPanel(new GridBagLayout()); // O(1)
        boardContainer.add(boardPanel, new GridBagConstraints()); // O(1)

        chatArea.setPreferredSize(new Dimension(400, 150)); // O(1)

        JPanel chatPanel = new JPanel(new BorderLayout()); // O(1)
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER); // O(1)
        chatPanel.add(chatInput, BorderLayout.SOUTH); // O(1)

        contentPane.add(topPanel, BorderLayout.NORTH); // O(1)
        contentPane.add(boardContainer, BorderLayout.CENTER); // O(1)
        contentPane.add(chatPanel, BorderLayout.SOUTH); // O(1)
    }

    private void initializeGrid() { // O(m * n)
        for (int i = 0; i < 6; i++) { // O(m)
            for (int j = 0; j < 7; j++) { // O(n)
                grid[i][j] = Color.WHITE; // O(1)
            }
        }
    }

    private void drawGrid(Graphics g) { // O(m * n)
        for (int i = 0; i < 6; i++) { // O(m)
            for (int j = 0; j < 7; j++) { // O(n)
                g.setColor(grid[i][j]);
                g.fillRect(j * 60, i * 60, 60, 60);
                g.setColor(Color.BLACK);
                g.drawRect(j * 60, i * 60, 60, 60);
            }
        }
    }

    private String getTurnText() {
        return myTurn ? "Your turn (Player " + (playerNumber == 1 ? "Red" : "Yellow") + ")"
                : "Opponent's turn (Player " + (playerNumber == 1 ? "Yellow" : "Red") + ")";
    }

    private void sendMove(int column) {
        try {
            toServer.writeInt(1);
            toServer.writeInt(column);
            toServer.flush();
            myTurn = false;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() { // O(1) per iteration
        try {
            while (!gameOver) { // O(1) per iteration
                int messageType = fromServer.readInt();
                if (messageType == 1) { // O(1)
                    int column = fromServer.readInt();
                    int row = fromServer.readInt();
                    grid[row][column] = (playerNumber == 1) ? Color.YELLOW : Color.RED; // O(1)

                    SwingUtilities.invokeLater(() -> {
                        boardPanel.repaint(); // O(m * n)
                        turnLabel.setText(getTurnText());
                    });

                    boolean opponentWon = fromServer.readBoolean();
                    if (opponentWon) {
                        gameOver = true;
                        JOptionPane.showMessageDialog(this, "You lost!"); // O(1)
                    } else { // O(1)
                        myTurn = true;
                        SwingUtilities.invokeLater(() -> turnLabel.setText(getTurnText())); // O(1)
                    }
                } else if (messageType == 0) {
                    String message = fromServer.readUTF();
                    SwingUtilities.invokeLater(() -> chatArea.append(message.substring(6) + "\n"));
                } else {
                    System.err.println("Unknown message type: " + messageType);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendChatMessage() {
        String message = chatInput.getText();
        if (!message.trim().isEmpty()) {
            try {
                toServer.writeInt(0);
                toServer.writeUTF("/chat " + "Player " + playerNumber + ": " + message);
                toServer.flush();
                chatInput.setText("");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConnectFourClient("localhost").setVisible(true));
    }
}
