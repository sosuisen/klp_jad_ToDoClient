package com.example.model;

import java.util.Locale;

public class Settings {
	private Locale locale;
	private String rootEndPoint;

	private Settings() {}

	private static class SingletonHolder {
		private static Settings singleton;
	}

	public static Settings getInstance() {
		if (SingletonHolder.singleton == null) {
			SingletonHolder.singleton = new Settings();
		}
		return SingletonHolder.singleton;
	}

	
	public Locale getLocale() {
		return locale;
	}

	public Settings setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}

	public String getRootEndPoint() {
		return rootEndPoint;
	}

	public Settings setRootEndPoint(String rootEndPoint) {
		this.rootEndPoint = rootEndPoint;
		return this;
	}	
}
