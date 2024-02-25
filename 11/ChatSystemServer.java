import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatSystemServer extends JFrame {
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader inputFromClient;
    private PrintWriter outputStream;

    public ChatSystemServer() {
        setTitle("Chat Server");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextPane();
        chatArea.setEditable(false);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(chatArea), BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);

        try {
            serverSocket = new ServerSocket(3000);
            appendToChatArea("Server started. Waiting for clients...");
            clientSocket = serverSocket.accept();
            appendToChatArea("Client connected: " + clientSocket.getInetAddress());
            inputFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputStream = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            new Thread(new MessageReader()).start();
        } catch (IOException e) {
            e.printStackTrace();
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

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            appendToChatArea("Server: " + message);
            outputStream.println(message);
            outputStream.flush();
            messageField.setText("");
        }
    }

    private class MessageReader implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = inputFromClient.readLine()) != null) {
                    appendToChatArea("Client: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatSystemServer::new);
    }
}
