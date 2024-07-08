package com.example.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.hildan.fxgson.FxGsonBuilder;

import com.example.exceptions.InternalServerErrorException;
import com.google.gson.JsonSyntaxException;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class ToDoManager {
	private final String configPath = "./config.json";
	
	private final ToDoService service;
	
	private ListProperty<ToDo> todos = new SimpleListProperty<>(FXCollections.observableArrayList());

	public ListProperty<ToDo> todosProperty() {
		return todos;
	}
	
	public ToDoManager() throws JsonSyntaxException, IOException {
		var gson = new FxGsonBuilder().create();
		record Config(String rootEndPoint) {}
		var config = gson.fromJson(Files.readString(Path.of(configPath)), Config.class);
		service = new ToDoService(config.rootEndPoint);
	}

	public void remove(ToDo todo) throws IOException, InterruptedException, InternalServerErrorException {
		service.delete(todo.getId());
		todos.remove(todo);
	}

	public void clear() throws IOException, InterruptedException, InternalServerErrorException {
		service.deleteAll();
		todos.clear();
	}

	private void addListener(ToDo todo) {
		todo.titleProperty().addListener((observable, oldValue, newValue) -> {
			try {
				service.updateTitle(todo.getId(), newValue);
			} catch (Exception e) {
				todo.setTitle(oldValue);
				// イベントハンドラから投げる例外は、
				// RuntimeExceptionでラップする必要があります。
				throw new RuntimeException(e);
			}
		});
		
		todo.dateProperty().addListener((observable, oldValue, newValue) -> { 
			try {
				service.updateDate(todo.getId(), newValue);
			} catch (Exception e) {
				todo.setDate(oldValue);
                throw new RuntimeException(e);
            }
		});
		
		todo.priorityProperty().addListener((observable, oldValue, newValue) -> {
			try {
				service.updatePriority(todo.getId(), newValue);
			} catch (Exception e) {
				todo.setPriority(oldValue);
				throw new RuntimeException(e);
			}
		});

		todo.completedProperty().addListener((observable, oldValue, newValue) -> {
			try {
				service.updateCompleted(todo.getId(), newValue);
			} catch (Exception e) {
				todo.setCompleted(oldValue);
				throw new RuntimeException(e);
			}
		});
	}

	public void create(String title, LocalDate date, int priority, boolean completed) throws JsonSyntaxException, IOException, InterruptedException, InternalServerErrorException {
		int newId = 0;
		if (todos.size() > 0)
			newId = todos.stream().max((todo1, todo2) -> todo1.getId() - todo2.getId()).get().getId() + 1;

		addNewToDo(newId, title, date, priority, completed);
	}

	private void addNewToDo(int id, String title, LocalDate date, int priority, boolean completed) throws JsonSyntaxException, IOException, InterruptedException, InternalServerErrorException {
		var todo = service.create(title, date, priority, completed);
		addListener(todo);
		todos.add(todo);
	}

	public void loadInitialData() throws JsonSyntaxException, IOException, InterruptedException, InternalServerErrorException {
		todos.addAll(service.getAll());
	}
}
