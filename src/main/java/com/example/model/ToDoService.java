package com.example.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import org.hildan.fxgson.FxGson;

import com.example.AuthDialogController;
import com.example.exceptions.InternalServerErrorException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.AnchorPane;
import javafx.util.Pair;

public class ToDoService {
	private final String rootEndPoint;
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = FxGson.coreBuilder()
			.registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
			.create();

	private final Logger logger = Logger.getLogger(ToDoService.class.getName());

	public StringProperty userName = new SimpleStringProperty();
	public StringProperty password = new SimpleStringProperty();

	private Dialog<Pair<String, String>> authDialog = new Dialog<>();
	private AuthDialogController controller;
	
	public ToDoService(String rootEndPoint) throws IOException {
		this.rootEndPoint = rootEndPoint;
		
		// 認証ダイアログを作成
		var loader = new FXMLLoader(getClass().getResource("/com/example/auth_dialog.fxml"));
		AnchorPane anchorPane = loader.load(); 

		// コントローラを取得
		controller = loader.getController();

		// ダイアログの作成
		authDialog.setTitle("Your account");
		authDialog.setHeaderText("Please enter your username and password");

		// OKおよびCancelボタンを追加
		authDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		// ダイアログにFXMLの内容をセット
		authDialog.getDialogPane().setContent(anchorPane);

		// ダイアログの結果を処理
		authDialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.OK) {
				return new Pair<>(controller.getUsername(), controller.getPassword());
			}
			return null;
		});
	}

	private boolean openAuthDialog(int statusCode) {
		if (statusCode == 401) {
			controller.setError("Invalid username or password");
		} else if (statusCode == 403) {
			controller.setError("Access denied");
		}

		// 認証ダイアログを表示して結果を取得
		var result = authDialog.showAndWait();
		if (result.isEmpty()) {
			return false;
		}
		userName.set(result.get().getKey());
		password.set(result.get().getValue());
		return true;
	}

	private String getBasicAuthHeader() {
		return "Basic " + java.util.Base64.getEncoder()
				.encodeToString((userName.get() + ":" + password.get()).getBytes());
	}

	public List<ToDo> getAll() throws IOException, InterruptedException, InternalServerErrorException {
		HttpResponse<String> res;
		do {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(rootEndPoint + "/todos"))
					.header("Authorization", getBasicAuthHeader()) // Basic認証
					.build();

			res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			var authExists = true;
			if (res.statusCode() == 401 || res.statusCode() == 403) { 
				authExists = openAuthDialog(res.statusCode());
			} else if (res.statusCode() != 200) {
				logger.severe("Failed to get all todos: " + res.body());
				throw new InternalServerErrorException();
			}
			
			if (!authExists) {
				return List.of();
			}
		} while (res.statusCode() != 200);

		record GetResult(List<ToDo> todos, String error) {
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

	public ToDo create(String title, LocalDate date, int priority, boolean completed)
			throws IOException, InterruptedException, InternalServerErrorException {
		record PostParams(String title, LocalDate date, int priority, boolean completed) {
		}
		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.header("Content-Type", "application/json")
				.header("Authorization", getBasicAuthHeader())
				.POST(HttpRequest.BodyPublishers
						.ofString(gson.toJson(new PostParams(title, date, priority, completed))))
				.build();
		HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 201) {
			logger.severe("Failed to create a new todo: " + res.body());
			throw new InternalServerErrorException();
		}
		record PostResult(ToDo todo, String error) {
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
				.header("Authorization", getBasicAuthHeader())
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
				.header("Authorization", getBasicAuthHeader())
				.DELETE()
				.build();
		HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			logger.severe("Failed to delete all todos: " + res.body());
			throw new InternalServerErrorException();
		}
	}

	private void updateField(int id, String fieldName, String json)
			throws IOException, InterruptedException, InternalServerErrorException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos/" + id + "/" + fieldName))
				.header("Content-Type", "application/json")
				.header("Authorization", getBasicAuthHeader())
				.PUT(HttpRequest.BodyPublishers.ofString(json))
				.build();
		HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			logger.severe("Failed to update a todo: " + res.body());
			throw new InternalServerErrorException();
		}
	}

	public void updateTitle(int id, String title)
			throws IOException, InterruptedException, InternalServerErrorException {
		record Param(String title) {
		}
		updateField(id, "title", gson.toJson(new Param(title)));
	}

	public void updateDate(int id, LocalDate date)
			throws IOException, InterruptedException, InternalServerErrorException {
		record Param(String date) {
		}
		updateField(id, "date", gson.toJson(new Param(date.toString())));
	}

	public void updatePriority(int id, int priority)
			throws IOException, InterruptedException, InternalServerErrorException {
		record Param(int priority) {
		}
		updateField(id, "priority", gson.toJson(new Param(priority)));
	}

	public void updateCompleted(int id, boolean completed)
			throws IOException, InterruptedException, InternalServerErrorException {
		record Param(boolean completed) {
		}
		updateField(id, "completed", gson.toJson(new Param(completed)));
	}
}
