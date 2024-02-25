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

public class ChatSystemServer extends JFrame {
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private List<PrintWriter> clientOutputStreams;
    private ExecutorService executorService;
    private JLabel ipAddressLabel;

    public ChatSystemServer() {
        setTitle("Chat Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextPane();
        chatArea.setEditable(false);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessageToClients("Server: " + messageField.getText()));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        // inputPanel.add(messageField, BorderLayout.CENTER);
        // inputPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(chatArea), BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        ipAddressLabel = new JLabel("IP Address: " + getLocalHostAddress());
        getContentPane().add(ipAddressLabel, BorderLayout.NORTH);

        setVisible(true);

        startHttpServer(); // Start the HTTP server

        clientOutputStreams = new ArrayList<>();
        executorService = Executors.newCachedThreadPool();
    }

    private void startHttpServer() {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);
            httpServer.createContext("/", new ChatHandler());
            httpServer.setExecutor(null); // Use the default executor
            httpServer.start();
            appendToChatArea("HTTP Server started on port 8000");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            executorService.execute(new ClientHandler(exchange));
        }
    }

    private class ClientHandler implements Runnable {
        private final HttpExchange exchange;

        public ClientHandler(HttpExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void run() {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    serveFile(exchange, "client.html"); // Serve the HTML page to the client
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    handlePostRequest(exchange); // Handle incoming messages from clients
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // Read the message sent by the client
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();

            // Display the message in the server's chat area
            appendToChatArea("Client: " + message);

            // Send the message to all connected clients
            sendMessageToClients("Server: " + message);

            // Send response to the client
            String response = "Message received: " + message;
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private void sendMessageToClients(String message) {
        for (PrintWriter writer : clientOutputStreams) {
            writer.println(message);
            writer.flush();
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