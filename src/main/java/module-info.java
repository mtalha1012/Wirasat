module com.wirasat {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires com.zaxxer.hikari;
    requires org.slf4j;

    opens com.wirasat to javafx.fxml;
    exports com.wirasat;
}