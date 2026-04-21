module com.wirasat {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.wirasat to javafx.fxml;
    exports com.wirasat;
}