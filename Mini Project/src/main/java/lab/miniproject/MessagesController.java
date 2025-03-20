package lab.miniproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessagesController {
    @FXML private Label userEmailLabel;
    @FXML private Button logoutButton;
    @FXML private ListView<String> usersListView;
    @FXML private VBox chatMessagesContainer;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button attachButton;

    private Client client;
    private String userEmail;

    public void initialize() {
        chatMessagesContainer.getChildren().clear();
        messageField.setOnAction(event -> sendMessage());
    }


    // This method will be called after login to set up the client
    public void setClient(Client client, String email) {
        this.client = client;
        this.userEmail = email;
        userEmailLabel.setText(email);

        // Now that we have a client, set up the message listener
        setupMessageListener();
    }


    private void setupMessageListener() {
        // Create a new thread to listen for incoming messages from the server
        new Thread(() -> {
            try {
                // Keep checking for new messages as long as the client is connected
                while (client.isConnected()) {
                    // This will block until a message is received
                    String messageFromGroupChat = client.getBufferedReader().readLine();

                    // If we received a message, update the UI on the JavaFX thread
                    if (messageFromGroupChat != null) {
                        String finalMessage = messageFromGroupChat;
                        Platform.runLater(() -> {
                            addMessageToChat(finalMessage);
                        });
                    }
                }
            } catch (IOException e) {
                // If there's an error, show it in the UI
                Platform.runLater(() -> {
                    showAlert("Connection Error", "Lost connection to server: " + e.getMessage());
                    try {
                        // Try to navigate back to login screen
                        handleLogout();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }).start();
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                // Use the bufferedWriter directly from the client to send the message
                client.getBufferedWriter().write(userEmail + ": " + message);
                client.getBufferedWriter().newLine();
                client.getBufferedWriter().flush();

                // Add the message to the chat UI immediately (optional - for responsive UI)
                addMessageToChat(userEmail + ": " + message);

                // Clear the message field after sending
                messageField.clear();
            } catch (IOException e) {
                showAlert("Error", "Could not send message: " + e.getMessage());
            }
        }
    }

//    @FXML
//    private void sendMessage() {
//        String message = messageField.getText().trim();
//        if (!message.isEmpty()) {
//            try {
//                client.sendMessage(message);
//                messageField.clear();
//            } catch (IOException e) {
//                showAlert("Error", "Could not send message: " + e.getMessage());
//            }
//        }
//    }

    @FXML
    private void attachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Attach");
        File selectedFile = fileChooser.showOpenDialog(attachButton.getScene().getWindow());

        if (selectedFile != null) {
            // Implement file sending logic here
            showAlert("Feature Coming Soon", "File attachment will be available in a future update.");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // Close the client connection
            if (client != null) {
                client.closeEverything();
            }

            // Navigate back to login screen
            Parent root = FXMLLoader.load(getClass().getResource("/lab/miniproject/login.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not logout properly: " + e.getMessage());
        }
    }

    private void addMessageToChat(String message) {
        // Parse the message to determine if it's from the current user
        boolean isMyMessage = message.startsWith(userEmail);

        // Create message container
        HBox messageBox = new HBox();
        messageBox.setSpacing(5);

        if (isMyMessage) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }

        // Create the message bubble
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("message-bubble");
        messageLabel.getStyleClass().add(isMyMessage ? "my-message" : "other-message");
        messageLabel.setWrapText(true);

        // Add timestamp
        Label timeLabel = new Label(getCurrentTime());
        timeLabel.getStyleClass().add("message-time");

        VBox messageContainer = new VBox(messageLabel, timeLabel);
        messageContainer.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        messageBox.getChildren().add(messageContainer);
        chatMessagesContainer.getChildren().add(messageBox);
    }

    private String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return now.format(formatter);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // You'll need to add a method to receive messages from the client
    public String receiveMessage() throws IOException {
        // This would be implemented to receive messages from your Client class
        return null;
    }
}


