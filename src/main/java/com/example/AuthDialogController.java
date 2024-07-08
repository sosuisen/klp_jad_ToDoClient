package com.example;

import com.example.model.ToDoService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthDialogController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;

	private ToDoService model;
	
	public void initModel(ToDoService service) {
		if (this.model != null)
			throw new IllegalStateException("Model can only be initialized once");

		model = service;

		usernameField.textProperty().bindBidirectional(model.userName);
		passwordField.textProperty().bindBidirectional(model.password);
		errorLabel.textProperty().bind(model.authError);
	}
}