package lab.miniproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class SignUpSceneController {
    @FXML
    TextField email;
    @FXML
    TextField username;
    @FXML
    PasswordField password;
    @FXML
    PasswordField confirmPassword;
    @FXML
    Text error;

    public void switchToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToMessages(ActionEvent event) throws IOException {
        if (!password.getText().equals(confirmPassword.getText())) {
            error.setText("Password and Confirm Password don't match");
            return;
        }

        Socket socket = new Socket("localhost", 6824);
        Client client = new Client(socket);

        String response = client.login(email.getText(), password.getText());
        error.setText(response);

        if (response.equals("SUCCESS")) {
            Parent root = FXMLLoader.load(getClass().getResource("messages.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }
    }
}
