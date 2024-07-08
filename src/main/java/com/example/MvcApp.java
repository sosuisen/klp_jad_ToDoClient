package com.example;

import java.io.IOException;

import com.example.model.ToDoManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MvcApp extends Application {

    @Override
    public void start(Stage stage) {
		try {
			// Create Loader for .fxml
			var mainViewLoader = new FXMLLoader(getClass().getResource("main.fxml"));

			// Get View
			Parent root = mainViewLoader.load();

			// Get Controller
			MainController mainController = mainViewLoader.getController(); 
			
			// Create and set Model to Controller
			var model = new ToDoManager();
			mainController.initModel(model);

			// Build scene and stage to show View on the screen
			var scene = new Scene(root);
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