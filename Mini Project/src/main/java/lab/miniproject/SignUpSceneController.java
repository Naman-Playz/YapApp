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

    public void submitForm(ActionEvent event) throws IOException {
        if (!password.getText().equals(confirmPassword.getText())) {
            error.setText("Password and Confirm Password don't match");
            return;
        }

        switchToMessages(event);
    }

    public void switchToMessages(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("messages.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
