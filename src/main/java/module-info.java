module dev.ritterackr.bitterlog {
    requires javafx.controls;
    requires javafx.fxml;


    opens dev.ritterackr.bitterlog to javafx.fxml;
    exports dev.ritterackr.bitterlog;
}