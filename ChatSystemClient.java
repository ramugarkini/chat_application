import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class ChatSystemClient extends JFrame implements ActionListener {
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;

    private Socket client;
    private BufferedReader inputFromServer;
    private PrintWriter outputStream;

    public ChatSystemClient() {
        setTitle("Chat Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextPane();
        chatArea.setEditable(false);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(this);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(chatArea), BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);

        try {
            InetAddress addressOfTheServer = InetAddress.getByName("127.0.0.1");
            client = new Socket(addressOfTheServer, 3000);
            inputFromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outputStream = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
            new Thread(new MessageReader()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                outputStream.println(message);
                outputStream.flush();
                appendToChatArea(message, false); // Message from client, right-aligned
                messageField.setText("");
            }
        }
    }

    private void appendToChatArea(String message, boolean isServerMessage) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatArea.getStyledDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setAlignment(style, isServerMessage ? StyleConstants.ALIGN_LEFT : StyleConstants.ALIGN_RIGHT);
            try {
                doc.insertString(doc.getLength(), (isServerMessage ? "Server: " : "Client: ") + message + "\n", style);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
    }

    private class MessageReader implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = inputFromServer.readLine()) != null) {
                    appendToChatArea(message, true); // Message from server, left-aligned
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatSystemClient::new);
    }
}
