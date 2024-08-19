import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.*;

public class ConnectFourServer {
    private ServerSocket serverSocket;
    private Color[][] grid = new Color[6][7];
    private int currentPlayer = 1;
    private boolean gameWon = false;
    private final Lock lock = new ReentrantLock();
    private final List<HandleClient> clients = new ArrayList<>();

    public ConnectFourServer() {
        try {
            serverSocket = new ServerSocket(8000); // O(1)
            System.out.println("Server started...");
            initializeGrid(); // O(m * n)

            for (int i = 1; i <= 2; i++) {
                Socket socket = serverSocket.accept(); // O(1)
                HandleClient client = new HandleClient(socket, i); // O(1)
                clients.add(client); // O(1)
                new Thread(client).start(); // O(1)
            }

        } catch (IOException ex) {
            ex.printStackTrace(); // O(1)
        }
    }

    private void initializeGrid() { // O(m * n)
        for (int i = 0; i < 6; i++) { // O(m)
            for (int j = 0; j < 7; j++) { // O(n)
                grid[i][j] = Color.WHITE;
            }
        }
    }

    private class HandleClient implements Runnable {
        private final Socket socket;
        private final DataInputStream fromClient;
        private final DataOutputStream toClient;
        private final int playerNumber;
        private final Color playerColor;

        public HandleClient(Socket socket, int playerNumber) throws IOException {
            this.socket = socket;
            this.playerNumber = playerNumber;
            this.playerColor = (playerNumber == 1) ? Color.RED : Color.YELLOW;
            this.fromClient = new DataInputStream(socket.getInputStream()); // O(1)
            this.toClient = new DataOutputStream(socket.getOutputStream()); // O(1)
            toClient.writeInt(playerNumber); // O(1)
            toClient.flush(); // O(1)
        }

        @Override
        public void run() { // O(1) per iteration
            try {
                while (!gameWon) { // O(1) per iteration
                    int messageType = fromClient.readInt(); // O(1)
                    System.out.println("Received message type: " + messageType); // O(1)

                    if (messageType == 0) { // O(1)
                        String message = fromClient.readUTF(); // O(1)
                        System.out.println("Received chat message from player " + playerNumber + ": " + message); // O(1)
                        broadcastMessage(message); // O(c)
                    } else if (messageType == 1) { // O(1)
                        int column = fromClient.readInt(); // O(1)
                        lock.lock(); // O(1)
                        try {
                            processMove(column, playerColor); // O(n * m) in worst case
                        } finally {
                            lock.unlock(); // O(1)
                        }
                    } else {
                        System.err.println("Unknown message type: " + messageType); // O(1)
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace(); // O(1)
            } finally {
                try {
                    socket.close(); // O(1)
                } catch (IOException e) {
                    e.printStackTrace(); // O(1)
                }
            }
        }

        private void broadcastMessage(String message) throws IOException { // O(c)
            for (HandleClient client : clients) { // O(c)
                client.toClient.writeInt(0); // O(1)
                client.toClient.writeUTF(message); // O(1)
                client.toClient.flush(); // O(1)
            }
        }

        private void broadcastMove(int column, int row) throws IOException { // O(c)
            for (HandleClient client : clients) { // O(c)
                client.toClient.writeInt(1); // O(1)
                client.toClient.writeInt(column); // O(1)
                client.toClient.writeInt(row); // O(1)
                client.toClient.flush(); // O(1)
            }
        }

        private void notifyGameOver(boolean gameWon) throws IOException { // O(c)
            for (HandleClient client : clients) { // O(c)
                client.toClient.writeInt(2); // O(1)
                client.toClient.writeBoolean(gameWon); // O(1)
                client.toClient.flush(); // O(1)
            }
        }

        private void switchTurn() { // O(1)
            lock.lock(); // O(1)
            try {
                currentPlayer = (currentPlayer == 1) ? 2 : 1; // O(1)
                System.out.println("Switched turn to player " + currentPlayer); // O(1)
            } finally {
                lock.unlock(); // O(1)
            }
        }

        private boolean checkForWinner(int column, int row, Color color) { // O(n * m) in worst case
            return (checkDirection(column, row, color, 1, 0) + checkDirection(column, row, color, -1, 0) >= 3) ||
                    (checkDirection(column, row, color, 0, 1) + checkDirection(column, row, color, 0, -1) >= 3) ||
                    (checkDirection(column, row, color, 1, 1) + checkDirection(column, row, color, -1, -1) >= 3) ||
                    (checkDirection(column, row, color, 1, -1) + checkDirection(column, row, color, -1, 1) >= 3);
        }

        private int checkDirection(int column, int row, Color color, int colOffset, int rowOffset) { // O(m)
            int count = 0;
            int c = column + colOffset;
            int r = row + rowOffset;

            while (c >= 0 && c < 7 && r >= 0 && r < 6 && grid[r][c] == color) { // O(m)
                count++;
                c += colOffset; // O(1)
                r += rowOffset; // O(1)
            }

            return count; // O(1)
        }

        private int dropDisc(int column, Color color) { // O(m)
            for (int row = grid.length - 1; row >= 0; row--) { // O(m)
                if (grid[row][column] == Color.WHITE) { // O(1)
                    grid[row][column] = color; // O(1)
                    return row; // O(1)
                }
            }
            return -1; // O(1)
        }

        private void processMove(int column, Color color) throws IOException { // O(n * m) in worst case
            int row = dropDisc(column, color); // O(m)
            if (row != -1) { // O(1)
                broadcastMove(column, row); // O(c)
                if (checkForWinner(column, row, color)) { // O(n * m) in worst case
                    gameWon = true; // O(1)
                    notifyGameOver(true); // O(c)
                } else {
                    switchTurn(); // O(1)
                    notifyGameOver(false); // O(c)
                }
            } else {
                System.err.println("Column " + column + " is full."); // O(1)
                toClient.writeInt(-1); // O(1)
                toClient.flush(); // O(1)
            }
        }
    }

    public static void main(String[] args) { // O(1)
        new ConnectFourServer(); // O(1)
    }
}
