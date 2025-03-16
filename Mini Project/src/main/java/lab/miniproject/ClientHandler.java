package lab.miniproject;

import lab.SECRET.ROT42069;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String userName;
    private String clientEmail;
    private Connection dbConnection;



    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.dbConnection = DatabaseConnection.getConnection();
        } catch(IOException | SQLException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        try {
            // Handle authentication first
            String initialMessage = bufferedReader.readLine();
            String[] parts = initialMessage.split(":");
            boolean authenticated = false;
            DataBase db = new DataBase();

            if (parts[0].equals("LOGIN")) {
                authenticated = handleLogin(parts[1], parts[2]);
            } else if (parts[0].equals("SIGNUP")) {
                authenticated = handleSignup(parts[1], parts[2], parts[3]);
            }

            if (!authenticated) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                return;
            }

            // After successful authentication, handle messages
            String messageFromClient;
            while(socket.isConnected()) {
                try {
                    messageFromClient = bufferedReader.readLine();
                    // analyze for private message // format /receiverEmail message //analyze for different functions
                    String startsWith = clientEmail + ": /";
                    if (messageFromClient.startsWith(startsWith)) {
                        int slashIndex = messageFromClient.indexOf("/");
                        int spaceAfterEmail = messageFromClient.indexOf(" ", slashIndex);

                        if (messageFromClient.startsWith(startsWith + "name")){
                            String newUserName = messageFromClient.substring(spaceAfterEmail + 1);
                            changeUserName(newUserName);
                        }
                        else {
                            if (spaceAfterEmail != -1) {
                                String recipientEmail = messageFromClient.substring(slashIndex + 1, spaceAfterEmail);
                                String messageContent = messageFromClient.substring(spaceAfterEmail + 1);

                                // Send direct message
                                sendDirectMessage(recipientEmail, messageContent); // save to db
                                db.sendMessage(clientEmail, recipientEmail,messageContent);
                                // changeUserName()
                            } else {
                                // Invalid format, broadcast as regular message
                                String messageContent = messageFromClient.substring(messageFromClient.indexOf(" ") + 1);
                                clientBroadcastMessage(messageContent); // save to db
                                db.sendMessage(clientEmail, "all",messageContent);
                            }
                        }
                    } else {
                        // Regular broadcast message
                        String messageContent = messageFromClient.substring(messageFromClient.indexOf(" ") + 1);
                        clientBroadcastMessage(messageContent); //save to db
                        db.sendMessage(clientEmail, "all",messageContent);
                    }

                } catch(IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void changeUserName(String username) throws IOException {
        DataBase db = new DataBase();
        ClientHandler clientHandler = this;
        try {
            db.updateUsername(clientEmail, username);
            this.userName = username;
            clientHandler.bufferedWriter.write("USERNAME_CHANGED");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendDirectMessage(String recipientEmail, String messageContent) {
        boolean recipientFound = false;

        // Try to find the recipient among online users
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.clientEmail.equals(recipientEmail)) {
                try {
                    // Send the message to the recipient
                    clientHandler.bufferedWriter.write("DM from " + userName + ": " + messageContent);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();

                    // Confirm to the sender
                    bufferedWriter.write("DM to " + recipientEmail + ": " + messageContent);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();

                    recipientFound = true;
//                    break;
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }

        // If the recipient is not found, notify the sender
        if (!recipientFound) {
            try {
                bufferedWriter.write("SERVER: User " + recipientEmail + " is not online or does not exist.");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void Load100Messages(String email){
        DataBase db = new DataBase();
        List<String> loadedMessages = db.getLast100Messages(email);
        for (String message : loadedMessages) {
            try {
                int colonIndex = message.indexOf(":");
                int secondColonIndex = message.indexOf(":", colonIndex + 1);
                if(message.startsWith(email)){
                    if (message.substring( colonIndex + 1).startsWith("all")) {
                        this.bufferedWriter.write(message.substring(secondColonIndex + 1));
                        this.bufferedWriter.newLine();
                        this.bufferedWriter.flush();
                    }
                    else {
                        this.bufferedWriter.write("DM to " + db.getUsername(message.substring(colonIndex + 1, secondColonIndex)) + ": " + message.substring(secondColonIndex + 1));
                        this.bufferedWriter.newLine();
                        this.bufferedWriter.flush();
                    }
                }
                else if(message.substring(colonIndex + 1).startsWith(email)){
                    this.bufferedWriter.write("DM from " + db.getUsername(message.substring(0, colonIndex)) + ": " + message.substring(secondColonIndex + 1));
                    this.bufferedWriter.newLine();
                    this.bufferedWriter.flush();
                }
                else if(message.substring(colonIndex + 1).startsWith("all")){
                    this.bufferedWriter.write(db.getUsername(message.substring(0, colonIndex)) + ": " + message.substring(secondColonIndex + 1));
                    this.bufferedWriter.newLine();
                    this.bufferedWriter.flush();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean handleLogin(String email, String password) throws IOException {
        String sql = "SELECT password FROM users WHERE email = ?";
        ROT42069 secret = new ROT42069();
        DataBase db = new DataBase();
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next() && rs.getString("password").equals(secret.rot42069(password))) {
                this.clientEmail = email;
                clientHandlers.add(this);
                bufferedWriter.write("SUCCESS");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                this.userName = db.getUsername(email);
                serverBroadcastMessage("SERVER: " + userName + " has joined the chat!"); // change email to username
                Load100Messages(clientEmail);
                return true;
            } else {
                bufferedWriter.write("Invalid email or password");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                return false;
            }
        } catch (SQLException e) {
            bufferedWriter.write("Login failed: Database error");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            return false;
        }
    }

    private boolean handleSignup(String email, String username, String password) throws IOException {
        if (userExists(email)) {
            bufferedWriter.write("Email already exists");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            return false;
        }

        DataBase db = new DataBase();
        if(db.createUser(email, username, password)){
            this.clientEmail = email;
            this.userName = username;
            clientHandlers.add(this);
            System.out.println("SUCCESS");
            bufferedWriter.write("SUCCESS");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            serverBroadcastMessage("SERVER: " + username + " has joined the chat!");
            return true;
        }
        else{
            System.out.println("Signup failed: Database error");
            bufferedWriter.write("Signup failed: Database error");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            return false;
        }
    }

    private boolean userExists(String email) {
        String sql = "SELECT email FROM users WHERE email = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public void clientBroadcastMessage(String messageToSend) {
        for(ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientEmail.equals(clientEmail)) {
                    String formattedMessage = userName + ": " + messageToSend;
                    clientHandler.bufferedWriter.write(formattedMessage);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }

            } catch(IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void serverBroadcastMessage(String messageToSend) {
        for(ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientEmail.equals(clientEmail)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }

            } catch(IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        serverBroadcastMessage("SERVER: " + userName + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if(bufferedReader != null) bufferedReader.close();
            if(bufferedWriter != null) bufferedWriter.close();
            if(socket != null) socket.close();
            if(dbConnection != null) dbConnection.close();
        } catch(IOException | SQLException e) {
            e.printStackTrace();
        }
    }
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return true;//pattern.matcher(email).matches();
    }
}