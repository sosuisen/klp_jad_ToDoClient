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
import com.example.exceptions.AuthenticationFailedException;
import com.example.exceptions.AuthorizationFailedException;
import com.example.exceptions.InternalServerErrorException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class ToDoService {
	private final String rootEndPoint;
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = FxGson.coreBuilder()
			.registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
			.create();

	private final Logger logger = Logger.getLogger(ToDoService.class.getName());

	public StringProperty userName = new SimpleStringProperty();
	public StringProperty password = new SimpleStringProperty();
	public StringProperty authError = new SimpleStringProperty();

	private Dialog<Boolean> authDialog = new Dialog<>();
	private AuthDialogController controller;

	public ToDoService(String rootEndPoint) throws IOException {
		this.rootEndPoint = rootEndPoint;

		// Config auth dialog
		authDialog.setTitle("Your account");
		authDialog.setHeaderText("Please enter your username and password");
		// Buttons
		authDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		authDialog.setResultConverter(buttonType -> buttonType == ButtonType.OK);

		// Load inner FXML
		var loader = new FXMLLoader(getClass().getResource("/com/example/auth_dialog.fxml"));
		authDialog.getDialogPane().setContent(loader.load());

		// Initialize controller for auth dialog
		controller = loader.getController();
		controller.initModel(this);
	}

	private boolean openAuthDialog(int statusCode) {
		if (statusCode == 401) {
			authError.set("Invalid username or password");
		} else if (statusCode == 403) {
			authError.set("Access denied");
		}

		var result = authDialog.showAndWait();
		return result.get();
	}

	private String getBasicAuthHeader() {
		return "Basic " + java.util.Base64.getEncoder()
				.encodeToString((userName.get() + ":" + password.get()).getBytes());
	}

	private HttpResponse<String> sendRequest(int successCode, HttpRequest.Builder builder)
			throws InternalServerErrorException, IOException, InterruptedException, AuthorizationFailedException, AuthenticationFailedException {
		while (true) {
			var req = builder.header("Authorization", getBasicAuthHeader()).build();

			HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

			if (res.statusCode() == 401) {
				if (!openAuthDialog(res.statusCode())) {
					throw new AuthenticationFailedException();
				}
			}
			else if (res.statusCode() == 403) {
				if (!openAuthDialog(res.statusCode())) {
					throw new AuthorizationFailedException();
				}
			} else if (res.statusCode() != successCode) {
				logger.severe("Failed to get all todos: " + res.body());
				throw new InternalServerErrorException();
			} else {
				return res;
			}
		}
	}

	public List<ToDo> getAll()
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"));
		HttpResponse<String> res = sendRequest(200, builder);
		
		record GetResult(List<ToDo> todos, String error) {}
		try {
			return gson.fromJson(res.body(), GetResult.class).todos();
		} catch (JsonSyntaxException e) {
			logger.severe("Failed to parse the response: " + res.body());
			throw new InternalServerErrorException();
		}
	}

	public ToDo create(String title, LocalDate date, int priority, boolean completed)
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		record PostParams(String title, LocalDate date, int priority, boolean completed) {}
		var json = gson.toJson(new PostParams(title, date, priority, completed));
		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json));
		HttpResponse<String> res = sendRequest(201, builder);

		record PostResult(ToDo todo, String error) {}
		try {
			return gson.fromJson(res.body(), PostResult.class).todo();
		} catch (JsonSyntaxException e) {
			logger.severe("Failed to parse the response: " + res.body());
			throw new InternalServerErrorException();
		}
	}

	public void delete(int id)
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos/" + id))
				.DELETE();
		sendRequest(200, builder);
	}

	public void deleteAll() 
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.DELETE();
		sendRequest(200, builder);
	}

	private void updateField(int id, String fieldName, String json)
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos/" + id + "/" + fieldName))
				.header("Content-Type", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(json));
		sendRequest(200, request);		
	}

	public void updateTitle(int id, String title)
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {
		record Param(String title) {}
		updateField(id, "title", gson.toJson(new Param(title)));
	}

	public void updateDate(int id, LocalDate date)
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		record Param(String date) {}
		updateField(id, "date", gson.toJson(new Param(date.toString())));
	}

	public void updatePriority(int id, int priority)
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		record Param(int priority) {}
		updateField(id, "priority", gson.toJson(new Param(priority)));
	}

	public void updateCompleted(int id, boolean completed)
			throws IOException, InterruptedException, InternalServerErrorException,
			AuthorizationFailedException, AuthenticationFailedException {

		record Param(boolean completed) {}
		updateField(id, "completed", gson.toJson(new Param(completed)));
	}
}
