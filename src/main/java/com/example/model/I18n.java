package com.example.model;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
	private ResourceBundle messages;

	private I18n() {}

	private static class SingletonHolder {
		private static I18n singleton;
	}

	public static I18n getInstance() {
		if (SingletonHolder.singleton == null) {
			SingletonHolder.singleton = new I18n();
		}
		return SingletonHolder.singleton;
	}

	public void setResource(String resourceName, Locale locale) {
		this.messages = ResourceBundle.getBundle(resourceName, locale);
	}

	public String getMessage(String key) {
		return messages.getString(key);
	}
}
