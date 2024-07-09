package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Logger;

import org.hildan.fxgson.FxGson;

import com.example.model.I18n;
import com.example.model.Settings;
import com.example.model.ToDoManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MvcApp extends Application {
	private final String settingsPath = "./settings.json";
	private final Logger logger = Logger.getLogger(MvcApp.class.getName());

	@Override
	public void start(Stage stage) {
		try {
			var gson = FxGson.coreBuilder().create();
			record SettingsFromFile(String rootEndPoint, String language) {}
			var settings = gson.fromJson(Files.readString(Path.of(settingsPath)), SettingsFromFile.class);

			// Get user default locale
			var userDefaultLocale = Locale.getDefault();
			// Use messages.properties as default resource bundle
			Locale.setDefault(Locale.of("en", "US"));
			Locale locale;
			if (settings.language == null || settings.language.isEmpty()) {
				locale = userDefaultLocale;
				logger.warning("Use default language: " + locale.getLanguage());
				Files.writeString(Path.of(settingsPath),
						gson.toJson(new SettingsFromFile(settings.rootEndPoint, locale.getLanguage())));
			} else {
				locale = Locale.of(settings.language);
				logger.warning("Use language: " + locale.getLanguage());
			}
			I18n.getInstance().setResource("messages", locale);

			Settings.getInstance()
					.setRootEndPoint(settings.rootEndPoint)
					.setLocale(locale);

			// Create Loader for .fxml
			var mainViewLoader = new FXMLLoader(getClass().getResource("main.fxml"));

			// Get View
			Parent root = mainViewLoader.load();

			// Get Controller
			MainController mainController = mainViewLoader.getController();

			// Create and set Model to Controller
			var model = new ToDoManager();
			mainController.initModel(model);

			// Build scene and stage to show View on the screen
			var scene = new Scene(root);
			stage.setTitle(I18n.getInstance().getMessage("main.app_name"));
			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch();
	}

}