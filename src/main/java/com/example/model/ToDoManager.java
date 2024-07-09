package com.example.model;

import java.time.LocalDate;

import com.example.exceptions.ToDoServiceException;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class ToDoManager {
	private final ListProperty<ToDo> todos = new SimpleListProperty<>(FXCollections.observableArrayList());

	public ListProperty<ToDo> todosProperty() {
		return todos;
	}

	private ToDoManager() {
	}

	private static class SingletonHolder {
		private static ToDoManager singleton;
	}

	public static ToDoManager getInstance() {
		if (SingletonHolder.singleton == null) {
			SingletonHolder.singleton = new ToDoManager();
		}
		return SingletonHolder.singleton;
	}

	public void remove(ToDo todo) throws ToDoServiceException {
		ToDoService.getInstance().delete(todo.getId());
		todos.remove(todo);
	}

	public void clear() throws ToDoServiceException {
		ToDoService.getInstance().deleteAll();
		todos.clear();
	}

	private void addListener(ToDo todo) {
		todo.titleProperty().addListener((observable, oldValue, newValue) -> {
			try {
				ToDoService.getInstance().updateTitle(todo.getId(), newValue);
			} catch (Exception e) {
				try {
					// Reset to the oldValue
					// This is a workaround because reset binded property is difficult.
					ToDoManager.getInstance().loadInitialData();
				} catch (ToDoServiceException ex) {
					ex.printStackTrace();
				}
			}
		});

		todo.dateProperty().addListener((observable, oldValue, newValue) -> {
			try {
				ToDoService.getInstance().updateDate(todo.getId(), newValue);
			} catch (Exception e) {				
				try {
					ToDoManager.getInstance().loadInitialData();					
				} catch (ToDoServiceException ex) {
					ex.printStackTrace();
				}
			}
		});

		todo.priorityProperty().addListener((observable, oldValue, newValue) -> {
			try {
				ToDoService.getInstance().updatePriority(todo.getId(), newValue);
			} catch (Exception e) {
				try {
					ToDoManager.getInstance().loadInitialData();
				} catch (ToDoServiceException ex) {
					ex.printStackTrace();
				}
			}
		});

		todo.completedProperty().addListener((observable, oldValue, newValue) -> {
			try {
				ToDoService.getInstance().updateCompleted(todo.getId(), newValue);
			} catch (Exception e) {
				try {
					ToDoManager.getInstance().loadInitialData();
				} catch (ToDoServiceException ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	public void create(String title, LocalDate date, int priority, boolean completed) throws ToDoServiceException {
		var todo = ToDoService.getInstance().create(title, date, priority, completed);
		addNewToDo(todo);
	}

	private void addNewToDo(ToDo todo) {
		addListener(todo);
		todos.add(todo);
	}

	public void loadInitialData() throws ToDoServiceException {
		todos.clear();
		ToDoService.getInstance().getAll().forEach(todo -> addNewToDo(todo));
	}
}
