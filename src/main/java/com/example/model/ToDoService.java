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
import com.example.exceptions.ToDoServiceException;
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
	private final Logger logger = Logger.getLogger(ToDoService.class.getName());
	private final Gson gson = FxGson.coreBuilder()
			.registerTypeAdapter(LocalDate.class, new LocalDateAdapter()) //Gson needed an adapter to convert LocalDate
			.create();
	private final Dialog<Boolean> authDialog = new Dialog<>();

	// Properties for authDialog
	public StringProperty userName = new SimpleStringProperty();
	public StringProperty password = new SimpleStringProperty();
	public StringProperty authError = new SimpleStringProperty();

	private String getMessage(String key) {
		return I18n.getInstance().getMessage(key);
	}

	public ToDoService(String rootEndPoint) {
		this.rootEndPoint = rootEndPoint;

		// Config authDialog
		authDialog.setTitle(getMessage("authdialog.your_account"));
		authDialog.setHeaderText(getMessage("authdialog.enter_your_account"));
		// Buttons
		authDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		authDialog.setResultConverter(buttonType -> buttonType == ButtonType.OK);
		// Load inner FXML
		var loader = new FXMLLoader(getClass().getResource("/com/example/auth_dialog.fxml"));
		try {
			authDialog.getDialogPane().setContent(loader.load());
		} catch (IOException e) {
			logger.severe("auth_dialog.fxml not found");
		}

		// Initialize controller for auth dialog
		AuthDialogController controller = loader.getController();
		controller.initModel(this);
	}

	private boolean openAuthDialog(int statusCode) {
		if (statusCode == 401) {
			authError.set(getMessage("authdialog.invalid_account"));
		} else if (statusCode == 403) {
			authError.set(getMessage("authdialog.not_permitted"));
		}

		var result = authDialog.showAndWait();
		return result.orElse(false);
	}

	private String getBasicAuthHeader() {
		logger.info(userName.get() + ":" + password.get());
		return "Basic " + java.util.Base64.getEncoder()
				.encodeToString((userName.get() + ":" + password.get()).getBytes());
	}

	private HttpResponse<String> sendRequest(HttpRequest.Builder builder)
			throws ToDoServiceException {

		while (true) {
			var newBuilder = builder.copy();			
			var req = newBuilder.header("Authorization", getBasicAuthHeader()).build();

			HttpResponse<String> res;
			try {
				res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			} catch (IOException e) {
				logger.severe("sendRequest: " + e.getMessage());
				throw new ToDoServiceException(ToDoServiceException.Type.IO_ERROR, e);
			} catch (InterruptedException e) {
				logger.severe("sendRequest: " + e.getMessage());
				throw new ToDoServiceException(ToDoServiceException.Type.INTERRUPTED_ERROR, e);
			}
			logger.info("HTTP Response Status Code: " + res.statusCode());

			switch (res.statusCode()) {
				case 200, 201, 204:
					return res;
				case 401:
					if (openAuthDialog(res.statusCode()))
						continue;
					else
						throw new ToDoServiceException(ToDoServiceException.Type.AUTHENTICATION_ERROR);
				case 403:
					if (openAuthDialog(res.statusCode()))
						continue;
					else
						throw new ToDoServiceException(ToDoServiceException.Type.AUTHORIZATION_ERROR);
				default:
					logger.severe("sendRequest: receive unsupported status code");
					throw new ToDoServiceException(ToDoServiceException.Type.INTERNAL_SERVER_ERROR);
			}
		}
	}

	public List<ToDo> getAll() throws ToDoServiceException {
		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"));
		HttpResponse<String> res = sendRequest(builder);

		record GetResult(List<ToDo> todos, String error) {}
		try {
			return gson.fromJson(res.body(), GetResult.class).todos();
		} catch (JsonSyntaxException e) {
			logger.severe("getAll: " + e.getMessage());
			throw new ToDoServiceException(ToDoServiceException.Type.INTERNAL_SERVER_ERROR, e);
		}
	}

	public ToDo create(String title, LocalDate date, int priority, boolean completed) throws ToDoServiceException {
		record PostParams(String title, LocalDate date, int priority, boolean completed) {}
		var json = gson.toJson(new PostParams(title, date, priority, completed));

		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json));
		HttpResponse<String> res = sendRequest(builder);

		record PostResult(ToDo todo, String error) {}
		try {
			return gson.fromJson(res.body(), PostResult.class).todo();
		} catch (JsonSyntaxException e) {
			logger.severe("create: " + e.getMessage());
			throw new ToDoServiceException(ToDoServiceException.Type.INTERNAL_SERVER_ERROR, e);
		}
	}

	public void delete(int id) throws ToDoServiceException {
		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos/" + id))
				.DELETE();
		sendRequest(builder);
	}

	public void deleteAll() throws ToDoServiceException {
		var builder = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos"))
				.DELETE();
		sendRequest(builder);
	}

	private void updateField(int id, String fieldName, String json) throws ToDoServiceException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create(rootEndPoint + "/todos/" + id + "/" + fieldName))
				.header("Content-Type", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(json));
		sendRequest(request);
	}

	public void updateTitle(int id, String title) throws ToDoServiceException {
		record Param(String title) {}
		updateField(id, "title", gson.toJson(new Param(title)));
	}

	public void updateDate(int id, LocalDate date) throws ToDoServiceException {
		record Param(String date) {}
		updateField(id, "date", gson.toJson(new Param(date.toString())));
	}

	public void updatePriority(int id, int priority) throws ToDoServiceException {
		record Param(int priority) {}
		updateField(id, "priority", gson.toJson(new Param(priority)));
	}

	public void updateCompleted(int id, boolean completed) throws ToDoServiceException {
		record Param(boolean completed) {}
		updateField(id, "completed", gson.toJson(new Param(completed)));
	}
}
