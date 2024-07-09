package com.example.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.hildan.fxgson.FxGson;

import com.example.MvcApp;
import com.google.gson.Gson;

public class Settings {
	private final String settingsPath = "./settings.json";
	private final Logger logger = Logger.getLogger(MvcApp.class.getName());
	private final Gson gson = FxGson.coreBuilder().setPrettyPrinting().create();
	// Please note that the key value is embedded in the code.
	// For a more secure approach, use the OS's secure storage
	// such as Windows Credential Manager or Mac's Keychain.
	private final SecretKeySpec key = new SecretKeySpec(getUTF8Bytes("bgkjyt78AyofjaBc"), "AES");
	private final IvParameterSpec iv = new IvParameterSpec(getUTF8Bytes("lhY'6D3assFfa1g*"));
	private Cipher cipher;
	private Locale locale;
	private String rootEndPoint;
	private String userName;
	private String password;

	record SettingsFile(String rootEndPoint, String language, String userName, String password) {}

	private Settings() {
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		} catch (Exception e) {
			logger.severe("Cipher init error: " + e.getMessage());
		}
		load();
	}

	private static class SingletonHolder {
		private static Settings singleton;
	}

	public static Settings getInstance() {
		if (SingletonHolder.singleton == null) {
			SingletonHolder.singleton = new Settings();
		}
		return SingletonHolder.singleton;
	}

	private static byte[] getUTF8Bytes(final String input) {
		return input.getBytes(StandardCharsets.UTF_8);
	}

	public void save() {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			byte[] encrypted = cipher.doFinal(password.getBytes());
			String encoded = Base64.getEncoder().encodeToString(encrypted);
			var json = gson.toJson(new SettingsFile(rootEndPoint, locale.getLanguage(), userName, encoded));
			Files.writeString(Path.of(settingsPath), json);
		} catch (Exception e) {
			logger.severe("Error in save: " + e.getMessage());
		}

	}

	public void load() {
		try {
			var settings = gson.fromJson(Files.readString(Path.of(settingsPath)), SettingsFile.class);

			setRootEndPoint(settings.rootEndPoint);

			// Get user default locale
			var userDefaultLocale = Locale.getDefault();
			// Use messages.properties as default resource bundle
			Locale.setDefault(Locale.of("en", "US"));
			if (settings.language == null || settings.language.isEmpty()) {
				setLocale(userDefaultLocale);
				logger.warning("Use default language: " + locale.getLanguage());
				save();
			} else {
				setLocale(Locale.of(settings.language));
				logger.warning("Use language: " + locale.getLanguage());
			}

			if (settings.userName == null || settings.userName.isEmpty()) {
				setUserName("");
			} else {
				setUserName(settings.userName);
			}

			if (settings.password == null || settings.password.isEmpty()) {
				setPassword("");
			}
			else {
				cipher.init(Cipher.DECRYPT_MODE, key, iv);
				byte[] decrpyted = cipher.doFinal(Base64.getDecoder().decode(settings.password));
				setPassword(new String(decrpyted));
			}
		} catch (Exception e) {
			logger.severe("Initializing Settings error: " + e.getMessage());
		}
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public String getRootEndPoint() {
		return rootEndPoint;
	}

	public void setRootEndPoint(String rootEndPoint) {
		this.rootEndPoint = rootEndPoint;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
