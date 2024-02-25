import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.Enumeration;
import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.table.DefaultTableModel;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;


public class ChatSystemServer extends JFrame {
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton historyButton;
    private JButton sendButton;
    private List<WebSocket> connectedClients; // Maintain a list of connected WebSocket clients
    private ExecutorService executorService;
    private JLabel ipAddressLabel;
    private WebSocketServer webSocketServer;
    private HttpServer httpServer;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;
    private JSplitPane splitPane;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private Connection connection;
    private JFrame loginFrame;

    public ChatSystemServer() {
        initializeLoginUI(); // Initialize the login UI

        // Initialize the database connection
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/chat";
        String dbUsername = "root";
        String dbPassword = "mysql";

        try {
            connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to the database", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeLoginUI() {
        loginFrame = new JFrame("Login");
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setText("admin");
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(passwordField, gbc);

        JButton loginButton = new JButton("Login");
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String password = new String(passwordField.getPassword());

                if (validateLogin(password)) {
                    // Proceed with opening the chat system
                    loginFrame.dispose(); // Close the login window
                    initializeChatUI(); // Initialize the chat UI
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Incorrect password!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        loginFrame.add(panel);
        loginFrame.setVisible(true);
    }

    private boolean validateLogin(String password) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT value FROM settings WHERE name = ? AND value = ?");
            statement.setString(1, "password");
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            boolean isValid = resultSet.next();
            statement.close(); // Close the PreparedStatement
            return isValid;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void initializeChatUI() {
        setTitle("Chat Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextPane();
        chatArea.setEditable(false);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> {
            String message = messageField.getText();
            appendToChatArea("Server: " + message);
            sendMessageToClients(message);
            messageField.setText("");
            updateHistoryTable();
        });

        // Create and position the history button
        historyButton = new JButton("History");
        historyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMessageHistory(); // Method to retrieve and display message history
            }
        });

        // Create buttons for clearing chat and deleting history
        JButton clearChatButton = new JButton("Clear Chat");
        clearChatButton.addActionListener(e -> {
            chatArea.setText(""); // Clear the chat area
        });

        JButton deleteHistoryButton = new JButton("Delete History");
        deleteHistoryButton.addActionListener(e -> {
            deleteAllMessages();
        });

        JButton changePasswordButton = new JButton("Change Password");
        changePasswordButton.addActionListener(e -> {
            // Create a dialog window
            JDialog dialog = new JDialog();
            dialog.setTitle("Change Password");
            dialog.setSize(300, 150);
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(null);

            // Create components for the dialog
            JLabel currentPasswordLabel = new JLabel(" Current Password:");
            JTextField currentPasswordField = new JTextField(20);
            JLabel newPasswordLabel = new JLabel(" New Password:");
            JTextField newPasswordField = new JTextField(20);
            JButton updatePasswordButton = new JButton("Update Password");

            // Add action listener to the save button
            updatePasswordButton.addActionListener(event -> {
                // Get the new password from the text field
                String currentPassword = currentPasswordField.getText();
                String newPassword = newPasswordField.getText();
                
                // Perform the password change
                if (validateLogin(currentPassword)) {
                    changePassword(newPassword);
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Incorrect Current Password!", "Error", JOptionPane.ERROR_MESSAGE);
                }
                
                // Close the dialog
                dialog.dispose();
            });

            // Set up layout for the dialog
            JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
            panel.add(currentPasswordLabel);
            panel.add(currentPasswordField);
            panel.add(newPasswordLabel);
            panel.add(newPasswordField);
            panel.add(new JPanel()); // Placeholder for alignment
            panel.add(updatePasswordButton);

            // Add the panel to the dialog
            dialog.add(panel);

            // Make the dialog visible
            dialog.setVisible(true);
        });

        // Create a panel to hold clear chat and delete history buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        buttonPanel.add(deleteHistoryButton);
        buttonPanel.add(changePasswordButton);
        buttonPanel.add(clearChatButton);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(historyButton, BorderLayout.WEST);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH); // Add the button panel containing clear chat and delete history buttons

        // Create the message history table and its model
        historyTableModel = new DefaultTableModel();
        historyTableModel.addColumn("Timestamp");
        historyTableModel.addColumn("Sender");
        historyTableModel.addColumn("Message");
        historyTable = new JTable(historyTableModel);
        
        // Create a scroll pane for the message history table
        JScrollPane historyScrollPane = new JScrollPane(historyTable);
        
        // Create a split pane to split the chat area and message history
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(chatArea), null);
        splitPane.setDividerLocation(0.5); // Set initial divider location closer to the top
        splitPane.setResizeWeight(0.5); // Set resize weight to 1.0 to keep the bottom component (message history) at the bottom
        
        // Set up the layout and add components
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        ipAddressLabel = new JLabel("IP Address: " + getLocalHostAddress());
        getContentPane().add(ipAddressLabel, BorderLayout.NORTH);

        setVisible(true);

        startServers();

        connectedClients = new ArrayList<>();
        executorService = Executors.newCachedThreadPool();
    }


    private void startServers() {
        startHttpServer();
        startWebSocketServer();
    }

    private String getClientHTML(String ipAddress) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>WebSocket Chat Client</title>\n" +
                "    <style>\n" +
                "        /* Style for sent and received messages */\n" +
                "        .message-container {\n" +
                "            display: flex;\n" +
                "            flex-direction: column;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .message {\n" +
                "            max-width: 70%;\n" +
                "            padding: 10px;\n" +
                "            border-radius: 10px;\n" +
                "            margin-bottom: 5px;\n" +
                "            word-wrap: break-word;\n" +
                "        }\n" +
                "        .sent-message {\n" +
                "            background-color: #DCF8C6;\n" +
                "            align-self: flex-end;\n" +
                "        }\n" +
                "        .received-message {\n" +
                "            background-color: #ADD8E6;\n" +
                "            align-self: flex-start;\n" +
                "        }\n" +
                "        .server-message {\n" +
                "            background-color: #ADD8E6;\n" +
                "            align-self: flex-start;\n" +
                "        }\n" +
                "        /* Style for text input and send button */\n" +
                "        .input-container {\n" +
                "            position: fixed;\n" +
                "            bottom: 0;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            padding: 10px;\n" +
                "            background-color: #F0F0F0;\n" +
                "            left: 50%;\n" +
                "            transform: translateX(-50%);\n" +
                "            width: 95%;\n" +
                "        }\n" +
                "        #messageInput {\n" +
                "            flex: 1;\n" +
                "            padding: 10px;\n" +
                "            border-radius: 20px;\n" +
                "            border: none;\n" +
                "            margin-right: 10px;\n" +
                "            outline: none;\n" +
                "        }\n" +
                "        #sendButton {\n" +
                "            padding: 10px 20px;\n" +
                "            border-radius: 20px;\n" +
                "            border: none;\n" +
                "            background-color: #4CAF50;\n" +
                "            color: white;\n" +
                "            font-weight: bold;\n" +
                "            cursor: pointer;\n" +
                "            outline: none;\n" +
                "        }\n" +
                "        #sendButton:hover {\n" +
                "            background-color: #45a049;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"receivedMessages\"></div> <!-- Add a div to display received messages -->\n" +
                "\n" +
                "    <div class=\"input-container\">\n" +
                "        <input type=\"text\" id=\"messageInput\" placeholder=\"Type your message...\">\n" +
                "        <button id=\"sendButton\" onclick=\"sendMessage()\">Send</button>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        var ipAddress = '" + ipAddress + "';\n" +
                "        var webSocket = new WebSocket(\"ws://\" + ipAddress + \":8001/ws\"); // Connect to the WebSocket server\n" +
                "\n" +
                "        // Event listener for WebSocket connection opening\n" +
                "        webSocket.onopen = function(event) {\n" +
                "            console.log(\"WebSocket connection opened\");\n" +
                "        };\n" +
                "\n" +
                "        // Event listener for WebSocket messages\n" +
                "        webSocket.onmessage = function(event) {\n" +
                "            var message = event.data;\n" +
                "            // Check if the message is from the server or another client\n" +
                "            if (message.startsWith('[Server]')) {\n" +
                "                appendMessage(message.substring(8), 'server-message');\n" +
                "            } else {\n" +
                "                appendMessage(message, 'received-message');\n" +
                "            }\n" +
                "        };\n" +
                "\n" +
                "        // Event listener for WebSocket connection closing\n" +
                "        webSocket.onclose = function(event) {\n" +
                "            console.log(\"WebSocket connection closed\");\n" +
                "        };\n" +
                "\n" +
                "        // Event listener for WebSocket errors\n" +
                "        webSocket.onerror = function(event) {\n" +
                "            console.error(\"WebSocket error:\", event);\n" +
                "        };\n" +
                "\n" +
                "        function sendMessage() {\n" +
                "            var messageInput = document.getElementById(\"messageInput\");\n" +
                "            var message = messageInput.value.trim();\n" +
                "            if (message !== \"\") {\n" +
                "                // Send the message via WebSocket\n" +
                "                webSocket.send(message);\n" +
                "                messageInput.value = \"\";\n" +
                "                // Append the sent message to the receivedMessages div with a class for styling\n" +
                "                appendMessage(message, 'sent-message');\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        // Function to append a message to the receivedMessages div\n" +
                "        function appendMessage(message, messageClass) {\n" +
                "            var receivedMessagesDiv = document.getElementById(\"receivedMessages\");\n" +
                "            var messageContainer = document.createElement('div');\n" +
                "            messageContainer.classList.add('message-container');\n" +
                "            var messageElement = document.createElement('div');\n" +
                "            messageElement.textContent = message;\n" +
                "            messageElement.classList.add('message', messageClass);\n" +
                "            messageContainer.appendChild(messageElement);\n" +
                "            receivedMessagesDiv.appendChild(messageContainer);\n" +
                "            // Automatically scroll to the bottom of the div to show the latest message\n" +
                "            receivedMessagesDiv.scrollTop = receivedMessagesDiv.scrollHeight;\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private void startHttpServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);
            httpServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String ipAddressLabelText = ipAddressLabel.getText();
                    String ipAddress = ipAddressLabelText.substring(ipAddressLabelText.indexOf(":") + 2);
                    String response = getClientHTML(ipAddress);
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            });
            httpServer.setExecutor(null); // Use the default executor
            httpServer.start();
            appendToChatArea("HTTP Server started on port 8000");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startWebSocketServer() {
        webSocketServer = new WebSocketServer(new InetSocketAddress(8001)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                // Handle WebSocket connection opening
                appendToChatArea("WebSocket connection opened: " + conn.getRemoteSocketAddress());
                connectedClients.add(conn); // Add the newly connected client to the list
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                // Handle WebSocket connection closing
                appendToChatArea("WebSocket connection closed: " + conn.getRemoteSocketAddress());
                connectedClients.remove(conn); // Remove the closed client from the list
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                // Handle incoming messages from WebSocket clients
                appendToChatArea("Client: " + message);
                // sendMessageToClients("Server: " + message); // No need to echo messages back to clients
                saveMessageToDatabase(conn.getRemoteSocketAddress().toString(), message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                // Handle WebSocket errors
                ex.printStackTrace();
            }

            // @Override
            public void onStart() {
                // Handle WebSocket server startup
                appendToChatArea("WebSocket server started on port 8001");
            }
        };
        webSocketServer.start();
    }

    private void sendMessageToClients(String message) {
        // Save the server message to the database
        saveMessageToDatabase("Server", message);

        // Send the message to each connected client
        for (WebSocket client : connectedClients) {
            client.send(message); // Send the message to each connected client
        }
    }

    private void saveMessageToDatabase(String clientAddress, String message) {
        try {
            // Prepare the SQL statement to insert the message into the database
            PreparedStatement statement = connection.prepareStatement("INSERT INTO messages (sender, message, timestamp) VALUES (?, ?, ?)");
            statement.setString(1, clientAddress);
            statement.setString(2, message);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            // Execute the SQL statement
            statement.executeUpdate();
            statement.close(); // Close the PreparedStatement
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private void appendToChatArea(String message) {
        StyledDocument doc = chatArea.getStyledDocument();
        Style style = chatArea.addStyle("ChatStyle", null);
        if (message.startsWith("Server:")) {
            StyleConstants.setForeground(style, Color.RED);
        } else if (message.startsWith("Client:")) {
            StyleConstants.setForeground(style, Color.BLACK);
        } else {
            StyleConstants.setForeground(style, Color.BLUE);
        }
        
        // Get the current timestamp
        String timestamp = dateFormat.format(new java.util.Date());
        
        try {
            // Append the timestamp and message to the chat area
            doc.insertString(doc.getLength(), "[" + timestamp + "] " + message + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private String getLocalHostAddress() {
        try {
           Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
           while (networkInterfaces.hasMoreElements()) {
               NetworkInterface networkInterface = networkInterfaces.nextElement();
               if (networkInterface.getName().startsWith("Wi-Fi") || networkInterface.getName().startsWith("wlan")) {
                   Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                   while (inetAddresses.hasMoreElements()) {
                       InetAddress inetAddress = inetAddresses.nextElement();
                       if (inetAddress instanceof Inet4Address) {
                           return inetAddress.getHostAddress();
                       }
                   }
               }
           }
       } catch (SocketException e) {
           e.printStackTrace();
       }
       return "127.0.0.1";
    }

    private void showMessageHistory() {
        try {
            // Fetch message history from the database
            PreparedStatement statement = connection.prepareStatement("SELECT sender, message, timestamp FROM messages ORDER BY timestamp");
            ResultSet resultSet = statement.executeQuery();
            
            // Clear existing data in the history table
            historyTableModel.setRowCount(0);

            // Populate the table model with data from the result set
            while (resultSet.next()) {
                String sender = resultSet.getString("sender");
                String message = resultSet.getString("message");
                String timestamp = resultSet.getString("timestamp");
                historyTableModel.addRow(new Object[]{timestamp, sender, message});
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        splitPane.setBottomComponent(splitPane.getBottomComponent() == null ? new JScrollPane(historyTable) : null);
    }

    private void updateHistoryTable() {
        try {
            // Clear the existing data in the history table model
            historyTableModel.setRowCount(0);
            
            // Fetch message history from the database
            PreparedStatement statement = connection.prepareStatement("SELECT sender, message, timestamp FROM messages ORDER BY timestamp");
            ResultSet resultSet = statement.executeQuery();

            // Populate the table model with data from the result set
            while (resultSet.next()) {
                String sender = resultSet.getString("sender");
                String message = resultSet.getString("message");
                String timestamp = resultSet.getString("timestamp");
                historyTableModel.addRow(new Object[]{timestamp, sender, message});
            }
            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Handle SQLException if needed
        }
    }

    private void deleteAllMessages() {
        try {
            // Prepare the SQL statement to delete all messages
            PreparedStatement statement = connection.prepareStatement("DELETE FROM messages");
            // Execute the SQL statement
            int rowsAffected = statement.executeUpdate();
            statement.close(); // Close the PreparedStatement
            
            // Notify the user about the deletion
            JOptionPane.showMessageDialog(this, rowsAffected + " messages deleted from history.", "Delete History", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear the message history table
            historyTableModel.setRowCount(0);
            updateHistoryTable();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting messages from history.", "Delete History", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to change the password of a user
    private void changePassword(String newPassword) {
        try {
            // Update the password for the given username
            PreparedStatement statement = connection.prepareStatement("UPDATE settings SET value = ? WHERE name = ?");
            statement.setString(1, newPassword);
            statement.setString(2, "password");
            int rowsAffected = statement.executeUpdate();
            statement.close();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Password changed successfully.", "Change Password", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to change password.", "Change Password", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error changing password.", "Change Password", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatSystemServer::new);
    }
}
