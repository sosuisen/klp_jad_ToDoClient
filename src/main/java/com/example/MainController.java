package com.example;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.example.exceptions.ToDoServiceException;
import com.example.model.I18n;
import com.example.model.ToDo;
import com.example.model.ToDoManager;
import com.example.model.ToDoService;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MainController {
	private final String TODO_ID_PREFIX = "todo-";

	@FXML
	private Menu fileMenu;

	@FXML
	private Menu helpMenu;

	@FXML
	private MenuItem menuItemAbout;

	@FXML
	private MenuItem menuItemAccountSettings;

	@FXML
	private MenuItem menuItemClear;

	@FXML
	private MenuItem menuItemClose;

	@FXML
	private Label dateLabel;

	@FXML
	private Button addBtn;

	@FXML
	private DatePicker headerDatePicker;

	@FXML
	private TextField headerTitleField;

	@FXML
	private VBox todoListVBox;

	@FXML
	private ChoiceBox<Integer> headerPriorityChoiceBox;

	private String getMessage(String key) {
		return I18n.getInstance().getMessage(key);
	}

	private void showInfo(String txt) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(getMessage("main.app_info"));
		alert.setHeaderText(null);
		alert.setContentText(txt);
		alert.showAndWait();
	}

	private void showError(Exception e) {
		if (e instanceof ToDoServiceException tdse) {
			String txt = switch (tdse.getType()) {
				case ToDoServiceException.Type.IO_ERROR -> getMessage("main.io_error");
				case ToDoServiceException.Type.INTERRUPTED_ERROR -> getMessage("main.interrupted_error");
				case ToDoServiceException.Type.INTERNAL_SERVER_ERROR -> getMessage("main.internal_server_error");
				case ToDoServiceException.Type.AUTHENTICATION_ERROR -> getMessage("main.authentication_error");
				case ToDoServiceException.Type.AUTHORIZATION_ERROR -> getMessage("main.authorization_error");
				default -> getMessage("main.unknown_error");
			};

			var dialog = new Alert(AlertType.ERROR);
			dialog.setTitle(getMessage("main.error"));
			dialog.setHeaderText(null);
			dialog.setContentText(txt);
			dialog.showAndWait();
		}
	}

	private HBox createToDoHBox(ToDo todo) {
		// Create View Items
		var completedCheckBox = new CheckBox();
		completedCheckBox.setSelected(todo.isCompleted());
		completedCheckBox.getStyleClass().add("todo-completed");

		var titleField = new TextField(todo.getTitle());
		titleField.getStyleClass().add("todo-title");
		HBox.setHgrow(titleField, Priority.ALWAYS);

		var datePicker = new DatePicker(todo.getDate());
		datePicker.getStyleClass().add("todo-date");
		datePicker.setPrefWidth(105);
		HBox.setHgrow(datePicker, Priority.NEVER);

		var priorityChoiceBox = new ChoiceBox<Integer>();
		priorityChoiceBox.getItems().addAll(1, 2, 3, 4, 5);
		priorityChoiceBox.setValue(todo.getPriority());
		HBox.setHgrow(priorityChoiceBox, Priority.NEVER);

		var deleteBtn = new Button(getMessage("main.delete_button"));
		deleteBtn.getStyleClass().add("todo-delete");

		var todoItem = new HBox(completedCheckBox, titleField, datePicker, priorityChoiceBox, deleteBtn);
		todoItem.getStyleClass().add("todo-item");

		todoItem.setId(TODO_ID_PREFIX + todo.getId());

		// Bind Model to View
		completedCheckBox.selectedProperty().bindBidirectional(todo.completedProperty());
		titleField.textProperty().bindBidirectional(todo.titleProperty());
		datePicker.valueProperty().bindBidirectional(todo.dateProperty());
		priorityChoiceBox.valueProperty().bindBidirectional(todo.priorityProperty());

		// Event Handler for sorting
		ChangeListener<Object> listener = (observable, newValue, oldValue) -> sortByCompletedAndDate();
		completedCheckBox.selectedProperty().addListener(listener);
		datePicker.valueProperty().addListener(listener);

		// Event Handler
		deleteBtn.setOnAction(e -> {
			try {
				ToDoManager.getInstance().remove(todo);
			} catch (Exception ex) {
				showError(ex);
			}
		});

		return todoItem;
	}

	public void initModel() {
		ObservableList<Node> todoListItems = todoListVBox.getChildren();

		// Event Handler
		addBtn.setOnAction(e -> {
			try {
				ToDoManager.getInstance().create(headerTitleField.getText(), headerDatePicker.getValue(),
						headerPriorityChoiceBox.getValue(), false);
				headerTitleField.clear();
			} catch (Exception ex) {
				showError(ex);
			}
		});

		// Observe Model to update View
		ToDoManager.getInstance().todosProperty().addListener((ListChangeListener<ToDo>) change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					change.getAddedSubList().forEach(todo -> todoListItems.add(createToDoHBox(todo)));
				}
				if (change.wasRemoved()) {
					List<String> ids = change.getRemoved().stream().map(todo -> TODO_ID_PREFIX + todo.getId()).toList();
					todoListItems.removeIf(node -> ids.contains(node.getId()));
				}
			}
			sortByCompletedAndDate();
		});

		menuItemClear.setOnAction(e -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle(getMessage("main.confirm"));
			alert.setHeaderText("");
			alert.setContentText(getMessage("main.clear_confirm"));
			alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					try {
						ToDoManager.getInstance().clear();
					} catch (Exception ex) {
						showError(ex);
					}
				}
			});
		});

		menuItemAbout.setOnAction(e -> showInfo(getMessage("main.app_name")));
		menuItemClose.setOnAction(e -> Platform.exit());

		try {
			ToDoManager.getInstance().loadInitialData();
		} catch (Exception e) {
			showError(e);
		}

		menuItemAccountSettings.setOnAction(e -> {
			if (ToDoService.getInstance().openAuthDialog(0)) {
				try {
					ToDoManager.getInstance().loadInitialData();
				} catch (ToDoServiceException ex) {
					showError(ex);
				}
			}
		});

		sortByCompletedAndDate();
	}

	private void sortByCompletedAndDate() {
		FXCollections.sort(todoListVBox.getChildren(),
				Comparator.comparing(node -> ((CheckBox) ((HBox) node).getChildren().get(0)).isSelected())
						.thenComparing(node -> ((DatePicker) ((HBox) node).getChildren().get(2)).getValue()));
	}

	public void initialize() {
		// Set today
		headerDatePicker.setValue(LocalDate.now());

		headerPriorityChoiceBox.getItems().addAll(1, 2, 3, 4, 5);
		headerPriorityChoiceBox.setValue(3);

		fileMenu.setText(getMessage("main.file_menu"));
		helpMenu.setText(getMessage("main.help_menu"));

		addBtn.setText(getMessage("main.add_button"));
		dateLabel.setText(getMessage("main.date"));
		menuItemAbout.setText(getMessage("main.about_menu"));
		menuItemAccountSettings.setText(getMessage("main.account_settings_menu"));
		menuItemClear.setText(getMessage("main.clear_menu"));
		menuItemClose.setText(getMessage("main.close_menu"));
	}
}