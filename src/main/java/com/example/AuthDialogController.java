package com.example;

import com.example.model.I18n;
import com.example.model.ToDoService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthDialogController {
	@FXML
	private Label userNameLabel;
	
	@FXML
	private Label passwordLabel;
	
    @FXML
    private TextField userNameField;

    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;

	private ToDoService model;
	
	private String getMessage(String key) {
		return I18n.getInstance().getMessage(key);
	}
	
	public void initModel(ToDoService service) {
		if (this.model != null)
			throw new IllegalStateException("Model can only be initialized once");

		model = service;

		userNameField.textProperty().bindBidirectional(model.userName);
		passwordField.textProperty().bindBidirectional(model.password);
		errorLabel.textProperty().bind(model.authError);
		
		userNameLabel.setText(getMessage("authdialog.username"));
		userNameField.setPromptText(getMessage("authdialog.username"));
		passwordLabel.setText(getMessage("authdialog.password"));
		passwordField.setPromptText(getMessage("authdialog.password"));		
	}
}