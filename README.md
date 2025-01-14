# chat_application
features:

User Authentication:

A login window is displayed upon starting the server.
The password is validated against a database before proceeding to the chat interface.
Chat Server UI:

The server UI includes a JTextPane for displaying messages, a text field for composing messages, and a button to send them.
The server listens for incoming messages and sends them to all connected clients.
It allows the server to display and send messages to clients in the chat area.
Client Management:

The server maintains a list of connected clients and sends messages to them using WebSocket.
Message History:

The server can show the history of messages sent in the chat.
Users can clear the chat area or delete the message history.
Password Management:

The server allows the password to be changed via a dialog window.
The current password must be validated before allowing the password change.
HTTP Port Management:

The server allows the HTTP port to be changed through a dialog window.
The current port is retrieved from the settings and can be modified.
Summary of Features for the Chat Application:
Client-Side:
A graphical interface for chatting, including message sending, display, and client-server communication.
Client sends messages to the server and receives incoming messages asynchronously.
Server-Side:
WebSocket-based communication to handle multiple clients.
A login system with password validation.
Message history management, chat clearing, and history deletion.
Password and HTTP port management through UI dialogs.
This project is a basic chat application that allows multiple clients to connect to a server, send and receive messages, and manage features such as message history, login, and server configuration (e.g., changing passwords and HTTP ports). The application uses both graphical user interfaces (GUIs) and WebSockets for communication. 
