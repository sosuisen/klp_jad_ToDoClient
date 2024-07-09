package com.example.model;

import java.time.LocalDate;

import com.example.exceptions.ToDoServiceException;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class ToDoManager {
	private final ToDoService service;
	private final ListProperty<ToDo> todos = new SimpleListProperty<>(FXCollections.observableArrayList());

	public ListProperty<ToDo> todosProperty() {
		return todos;
	}

	public ToDoManager() {
		service = new ToDoService();
	}

	public void remove(ToDo todo) throws ToDoServiceException {
		service.delete(todo.getId());
		todos.remove(todo);
	}

	public void clear() throws ToDoServiceException {
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

	public void create(String title, LocalDate date, int priority, boolean completed) throws ToDoServiceException {
		var todo = service.create(title, date, priority, completed);
		addNewToDo(todo);
	}

	private void addNewToDo(ToDo todo) {
		addListener(todo);
		todos.add(todo);
	}

	public void loadInitialData() throws ToDoServiceException {
		service.getAll().forEach(todo -> addNewToDo(todo));
	}
}
