import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.Enumeration;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

public class ChatSystemServer extends JFrame {
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private List<WebSocket> connectedClients; // Maintain a list of connected WebSocket clients
    private ExecutorService executorService;
    private JLabel ipAddressLabel;
    private WebSocketServer webSocketServer;

    public ChatSystemServer() {
        setTitle("Chat Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextPane();
        chatArea.setEditable(false);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> {
            String message = "Server: " + messageField.getText();
            appendToChatArea(message);
            sendMessageToClients(message);
            messageField.setText("");
        });

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(chatArea), BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        ipAddressLabel = new JLabel("IP Address: " + getLocalHostAddress());
        getContentPane().add(ipAddressLabel, BorderLayout.NORTH);

        setVisible(true);

        startHttpServer(); // Start the HTTP server
        startWebSocketServer(); // Start the WebSocket server

        connectedClients = new ArrayList<>();
        executorService = Executors.newCachedThreadPool();
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
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);
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

    private void serveFile(HttpExchange exchange, String filename) throws IOException {
        try (InputStream fileStream = ChatSystemServer.class.getResourceAsStream(filename)) {
            if (fileStream == null) {
                String response = "File not found: " + filename;
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileStream.read(buffer)) != -1) {
                        responseBody.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    private void sendMessageToClients(String message) {
        for (WebSocket client : connectedClients) {
            client.send(message); // Send the message to each connected client
        }
    }

    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatArea.getStyledDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
            try {
                doc.insertString(doc.getLength(), message + "\n", style);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
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
       return "Unknown";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatSystemServer::new);
    }
}
