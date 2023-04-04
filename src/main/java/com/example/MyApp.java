package com.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MyApp extends Application {

    @Override
    public void start(Stage stage) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("myapp.fxml"));
	        var scene = new Scene(root);
	        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
	        
	        stage.setTitle("ToDo App");
	        stage.setScene(scene);
	        stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public static void main(String[] args) {
        launch();
    }

}