module lab.miniproject {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens lab.miniproject to javafx.fxml;
    exports lab.miniproject;
}