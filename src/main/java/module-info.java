// 'mvcapp': Your module name
// 'com.example': Your package name
module mvcapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires java.net.http;
    requires org.hildan.fxgson;
	requires java.logging;
    opens com.example to javafx.graphics, javafx.fxml, com.google.gson;
}
