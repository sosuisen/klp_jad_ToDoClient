package com.example.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import org.hildan.fxgson.FxGsonBuilder;

import com.example.exceptions.InternalServerErrorException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ToDoService {
	private final String rootEndPoint;
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = new FxGsonBuilder().create();
	private final Logger logger = Logger.getLogger(ToDoService.class.getName());
	
	public record GetResult(List<ToDo> todos, String error) {}
	public record PostResult(ToDo todo, String error) {}
	public record PutResult(ToDo todo, String error) {}
	public record DeleteResult(int id, String error) {}

	private ToDoService(String rootEndPoint) {
		this.rootEndPoint = rootEndPoint;
	}

	private static class SingletonHolder {
		private static ToDoService singleton;
	}

	public static ToDoService getInstance(String rootEndPoint) {
		if (SingletonHolder.singleton == null) {
			SingletonHolder.singleton = new ToDoService(rootEndPoint);
		}
		return SingletonHolder.singleton;
	}

	public List<ToDo> getAll() throws IOException, InterruptedException, InternalServerErrorException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.build();
		HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			logger.severe("Failed to get all todos: " + res.body());
			throw new InternalServerErrorException();
		}
		List<ToDo> todos;
		try {
			todos = gson.fromJson(res.body(), GetResult.class).todos();
		} catch (JsonSyntaxException e) {
			logger.severe("Failed to parse the response: " + res.body());
			throw new InternalServerErrorException();
		}
		return todos;
	}

	public ToDo create(String title, LocalDate date, int priority, boolean completed) throws IOException, InterruptedException, InternalServerErrorException {
		record PostParams(String title, LocalDate date, int priority, boolean completed) {}
		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(new PostParams(title, date, priority, completed))))
				.build();
		HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 201) {
			logger.severe("Failed to create a new todo: " + res.body());
			throw new InternalServerErrorException();
		}
		ToDo todo;
		try {
			todo = gson.fromJson(res.body(), PostResult.class).todo();
		} catch (JsonSyntaxException e) {
			logger.severe("Failed to parse the response: " + res.body());
			throw new InternalServerErrorException();
		}
		return todo;
	}

	public void delete(int id) throws IOException, InterruptedException, InternalServerErrorException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos/" + id))
				.DELETE()
				.build();
		HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			logger.severe("Failed to delete a todo: " + res.body());
			throw new InternalServerErrorException();
		}
	}

	public void deleteAll() throws IOException, InterruptedException, InternalServerErrorException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.DELETE()
				.build();
		HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			logger.severe("Failed to delete all todos: " + res.body());
			throw new InternalServerErrorException();
		}
	}
	
	private void updateField(int id, String fieldName, String json) throws IOException, InterruptedException, InternalServerErrorException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(rootEndPoint + "/todos/" + id + "/" + fieldName))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			logger.severe("Failed to update a todo: " + res.body());
			throw new InternalServerErrorException();
		}
    }
	
	public void updateTitle(int id, String title) throws IOException, InterruptedException, InternalServerErrorException {
		record Param(String title) {}
		updateField(id, "title", gson.toJson(new Param(title)));
	}

	public void updateDate(int id, LocalDate date) throws IOException, InterruptedException, InternalServerErrorException {
		record Param(String date) {}
		updateField(id, "title", gson.toJson(new Param(date.toString())));
	}

	public void updatePriority(int id, int priority) throws IOException, InterruptedException, InternalServerErrorException {
		record Param(int priority) {}
		updateField(id, "title", gson.toJson(new Param(priority)));
	}

	public void updateCompleted(int id, boolean completed) throws IOException, InterruptedException, InternalServerErrorException {
		record Param(boolean completed) {}
		updateField(id, "title", gson.toJson(new Param(completed)));
	}
}
