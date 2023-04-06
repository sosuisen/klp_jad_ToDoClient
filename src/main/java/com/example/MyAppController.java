package com.example;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MyAppController {
	/**
	 * プロジェクトフォルダに config.json が必要。
	 * ToDoServer側の認証に必要なユーザ名とパスワードを指定します。
	 * ﻿{
	 * "user": "admin",
  	 * "pass": "foobar"
  	 * }
	 */
	private String path = "config.json";

	private DAO dao;

	private final String TODO_COMPLETED = "完了";
	private final String TODO_TITLE = "タイトル";
	private final String TODO_DATE = "日付";
	private final LinkedHashMap<String, Integer> MENU = new LinkedHashMap<String, Integer>() {
		{
			put(TODO_COMPLETED, 0);
			put(TODO_TITLE, 1);
			put(TODO_DATE, 2);
		}
	};
	private final String SORT_ASCENDANT = "昇順";
	private final String SORT_DESCENDANT = "降順";

	private ArrayList<ToDo> todos;

	@FXML
	private Button addBtn;

	@FXML
	private DatePicker headerDatePicker;

	@FXML
	private MenuItem menuItemAbout;

	@FXML
	private MenuItem menuItemClose;

	@FXML
	private ChoiceBox<String> sortOrderMenu;

	@FXML
	private ChoiceBox<String> sortTypeMenu;

	@FXML
	private TextField headerTitleField;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private VBox todoListVBox;

	private ObservableList<Node> todoListItems;

	private HBox createToDoHBox(ToDo todo) {
		var completedCheckBox = new CheckBox();
		completedCheckBox.setSelected(todo.isCompleted());
		completedCheckBox.getStyleClass().add("todo-completed");
		completedCheckBox.setOnAction(e -> {
			System.out.println("チェック更新[" + todo.getId() + "] " + completedCheckBox.isSelected());
			todo.setCompleted(completedCheckBox.isSelected());
			dao.updateCompleted(todo.getId(), todo.isCompleted());
		});

		var titleField = new TextField(todo.getTitle());
		titleField.getStyleClass().add("todo-title");
		HBox.setHgrow(titleField, Priority.ALWAYS);
		titleField.setOnAction(e -> {
			System.out.println("タイトル更新[" + todo.getId() + "] " + titleField.getText());
			todo.setTitle(titleField.getText());
			dao.updateTitle(todo.getId(), todo.getTitle());
		});
		titleField.focusedProperty().addListener((observable, oldProperty, newProperty) -> {
			if (!newProperty) {
				System.out.println("タイトル更新[" + todo.getId() + "] " + titleField.getText());
				todo.setTitle(titleField.getText());
				dao.updateTitle(todo.getId(), todo.getTitle());
			}
		});

		var datePicker = new DatePicker(todo.getLocalDate());
		datePicker.getStyleClass().add("todo-date");
		datePicker.setPrefWidth(105);
		HBox.setHgrow(datePicker, Priority.NEVER);
		datePicker.setOnAction(e -> {
			System.out.println("日付更新[" + todo.getId() + "] " + datePicker.getValue().toString());
			todo.setDate(datePicker.getValue().toString());
			dao.updateDate(todo.getId(), todo.getDate());
		});

		var deleteBtn = new Button("削除");
		deleteBtn.getStyleClass().add("todo-delete");

		var todoItem = new HBox(completedCheckBox, titleField, datePicker, deleteBtn);
		todoItem.getStyleClass().add("todo-item");

		deleteBtn.setOnAction(e -> {
			System.out.println("削除[" + todo.getId() + "]");
			todos.remove(todo);
			todoListItems.remove(todoItem);
			dao.delete(todo.getId());
		});

		return todoItem;
	}

	private void showInfo(String txt) {
		Alert dialog = new Alert(AlertType.INFORMATION);
		dialog.setTitle("アプリの情報");
		dialog.setHeaderText(null);
		dialog.setContentText(txt);
		dialog.showAndWait();
	}

	private void showError(String txt) {
		Alert dialog = new Alert(AlertType.ERROR);
		dialog.setHeaderText(null);
		dialog.setContentText(txt);
		dialog.showAndWait();
	}

	private void sort(String type, String order) {
		Comparator<Node> comp = null;
		switch (type) {
		case TODO_COMPLETED:
			comp = Comparator.comparing(
					node -> ((CheckBox) ((HBox) node).getChildren().get(MENU.get(TODO_COMPLETED))).isSelected());
			break;
		case TODO_TITLE:
			comp = Comparator
					.comparing(node -> ((TextField) ((HBox) node).getChildren().get(MENU.get(TODO_TITLE))).getText());
			break;
		case TODO_DATE:
		default:
			comp = Comparator
					.comparing(node -> ((DatePicker) ((HBox) node).getChildren().get(MENU.get(TODO_DATE))).getValue());
			break;
		}
		if (order.equals(SORT_DESCENDANT)) {
			comp = comp.reversed();
		}
		FXCollections.sort(todoListItems, comp);
	}

	public void initialize() {
		try {
			var configJson = Files.readString(Path.of(path));
			// ここでの変換のためにしか使わない単純なクラスなので、Recordで定義しています。
			record Config(String user, String pass) {};
			var configObj = new Gson().fromJson(configJson, Config.class);
			if (configObj.user() == null) {
				showError("設定ファイルにuserプロパティがありません");
				Platform.exit();
			}
			if (configObj.pass() == null) {
				showError("設定ファイルにpassプロパティがありません");
				Platform.exit();
			}
			dao = new DAO("http://localhost:8080/ToDoServer", configObj.user(), configObj.pass());
		} catch (NoSuchFileException err) {
			err.printStackTrace();
			showError("設定ファイル%sが必要です。".formatted(Path.of(path).toAbsolutePath()));
			Platform.exit();
		} catch (Exception err) {
			err.printStackTrace();
			showError("設定ファイルのJSON形式が不正です。");
			Platform.exit();
		}

		sortTypeMenu.getItems().addAll(MENU.keySet());
		sortTypeMenu.setValue(TODO_DATE);
		sortTypeMenu.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> sort(newValue, sortOrderMenu.getValue()));

		sortOrderMenu.getItems().addAll(SORT_ASCENDANT, SORT_DESCENDANT);
		sortOrderMenu.setValue(SORT_ASCENDANT);
		sortOrderMenu.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> sort(sortTypeMenu.getValue(), newValue));

		// Set today
		headerDatePicker.setValue(LocalDate.now());

		todoListItems = todoListVBox.getChildren();

		todos = dao.getAll();
		todos.stream()
				.sorted(Comparator.comparing(ToDo::getDate))
				.forEach(todo -> {
					todoListItems.add(createToDoHBox(todo));
				});

		EventHandler<ActionEvent> handler = e -> {
			var title = headerTitleField.getText();
			if (title.equals(""))
				return;
			LocalDate localDate = headerDatePicker.getValue(); // 2022-12-01
			ToDo newToDo = dao.create(title, localDate.toString(), false);
			todos.add(newToDo);
			todoListItems.add(createToDoHBox(newToDo));
			sort(sortTypeMenu.getValue(), sortOrderMenu.getValue());
			headerTitleField.clear();
			System.out.println("追加[" + newToDo.getId() + "] " + title);
		};
		headerTitleField.setOnAction(handler);
		addBtn.setOnAction(handler);

		menuItemAbout.setOnAction(e -> showInfo("ToDo App"));
		menuItemClose.setOnAction(e -> Platform.exit());

		// VBoxの末尾にあるScrollPaneを、ウィンドウサイズに応じて高さ最大化
		VBox.setVgrow(scrollPane, Priority.ALWAYS);
	}
}
