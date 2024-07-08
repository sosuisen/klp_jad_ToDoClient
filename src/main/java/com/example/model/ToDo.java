package com.example.model;

import java.time.LocalDate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ToDo {
	private int id;
	private StringProperty title = new SimpleStringProperty();
	private ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
	private BooleanProperty completed = new SimpleBooleanProperty();
	private ObjectProperty<Integer> priority = new SimpleObjectProperty<>();

	// Constructor
	public ToDo(int id, String title, LocalDate date, int priority, boolean completed) {
		this.id = id;
		setTitle(title);
		setDate(date);
		setPriority(priority);
		setCompleted(completed);
	}

	// id is read-only
	public int getId() {
		return id;
	}

	// Title
	public StringProperty titleProperty() {
		return title;
	}

	public String getTitle() {
		return title.get();
	}

	public void setTitle(String title) {
		this.title.set(title);
	}

	// Date
	public ObjectProperty<LocalDate> dateProperty() {
		return date;
	}

	public LocalDate getDate() {
		return date.get();
	}

	public void setDate(LocalDate localDate) {
		this.date.set(localDate);
	}
	
	// Priority
	public ObjectProperty<Integer> priorityProperty() {
		return priority;
	}

	public int getPriority() {
		return priority.get();
	}
	
	public void setPriority(int priority) {
		this.priority.set(priority);
	}
	
	// Completed
	public BooleanProperty completedProperty() {
		return completed;
	}

	public boolean isCompleted() {
		return completed.get();
	}

	public void setCompleted(boolean completed) {
		this.completed.set(completed);
	}
}
