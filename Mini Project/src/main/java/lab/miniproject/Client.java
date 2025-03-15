package lab.miniproject;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import lab.SECRET.ROT42069;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String email;
    private String username;

    public Client(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch(IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendMessage() {
        try {
            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                bufferedWriter.write(email + ": " + messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch(IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;
                while(socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println(msgFromGroupChat);
                    } catch(IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                        break;
                    }
                }
            }
        }).start();
    }

    public boolean login(String email, String password) throws IOException {
        this.email = email;

        // Format: LOGIN:email:password
        bufferedWriter.write("LOGIN:" + email + ":" + password);
        bufferedWriter.newLine();
        bufferedWriter.flush();

        String response = bufferedReader.readLine();
        return response.equals("SUCCESS");
    }

    public boolean signup(String email, String username, String password) throws IOException {
        this.email = email;

        // Format: SIGNUP:email:username:password
        bufferedWriter.write("SIGNUP:" + email + ":" + username + ":" + password);
        bufferedWriter.newLine();
        bufferedWriter.flush();

        String response = bufferedReader.readLine();
        return response.equals("SUCCESS");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if(bufferedReader != null) bufferedReader.close();
            if(bufferedWriter != null) bufferedWriter.close();
            if(socket != null) socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("1. Login\n2. Signup");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            Socket socket = new Socket("localhost", 6824);
            Client client = new Client(socket);

            boolean authenticated = false;
            String responseMessage = "";

            switch (choice) {
                case 1:
                    System.out.println("Enter email: ");
                    String loginEmail = scanner.nextLine();
                    System.out.println("Enter password: ");
                    String loginPassword = scanner.nextLine();

                    try {
                        authenticated = client.login(loginEmail, loginPassword);
                        if (!authenticated) {
                            responseMessage = client.bufferedReader.readLine();
                        }
                    } catch (IOException e) {
                        System.out.println("Error during login: " + e.getMessage());
                    }
                    break;

                case 2:
                    System.out.println("Enter email: ");
                    String signupEmail = scanner.nextLine();
                    System.out.println("Enter username: ");
                    String username = scanner.nextLine();
                    System.out.println("Enter password: ");
                    String signupPassword = scanner.nextLine();

                    try {
                        authenticated = client.signup(signupEmail, username, signupPassword);
                        if (!authenticated) {
                            responseMessage = client.bufferedReader.readLine();
                        }
                    } catch (IOException e) {
                        System.out.println("Error during signup: " + e.getMessage());
                    }
                    break;

                default:
                    System.out.println("Invalid choice");
                    client.closeEverything(socket, client.bufferedReader, client.bufferedWriter);
                    return;
            }

            if (authenticated) {
                System.out.println("Connected to server!");
                //loadLast100Messages()
                client.listenForMessage();
                client.sendMessage();
            } else {
                System.out.println("Authentication failed: " + responseMessage);
                client.closeEverything(socket, client.bufferedReader, client.bufferedWriter);
            }
        } catch (SocketException e) {
            System.out.println("Lost connection to server. Please try again.");
        } catch (IOException e) {
            System.out.println("Cannot connect to server. Please check if server is running.");
            e.printStackTrace();
        }
    }
}
