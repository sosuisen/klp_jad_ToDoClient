package com.example.model;

import java.util.ResourceBundle;

public class I18n {
	private ResourceBundle messages;

	private static String resourceName = "messages";
	
	private I18n() {
		this.messages = ResourceBundle.getBundle(resourceName, Settings.getInstance().getLocale());
	}

	private static class SingletonHolder {
		private static I18n singleton;
	}

	public static I18n getInstance() {
		if (SingletonHolder.singleton == null) {
			SingletonHolder.singleton = new I18n();
		}
		return SingletonHolder.singleton;
	}

	public String getMessage(String key) {
		return messages.getString(key);
	}
}
