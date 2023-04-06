package com.example;

import java.lang.reflect.Type;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * DAO for ToDo App
 */
public class DAO {
	private String url;
	private HttpClient client;

	private Gson gson = new Gson();
	private Type todosType = new TypeToken<ArrayList<ToDo>>() {
	}.getType();

	public DAO(String url, String user, String pass) {
		this.url = url;
		
		// Basic認証を利用
		this.client = HttpClient.newBuilder()
				  .authenticator(new Authenticator() {
				      @Override
				      protected PasswordAuthentication getPasswordAuthentication() {
				          return new PasswordAuthentication(user, pass.toCharArray());
				      }
				  })
				  .build(); 
	}

	public ToDo get(int id) {
		String api = url + "/todos/" + id;
		ToDo todo = null;
		try {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(api))
					.build();
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 404) {
				System.out.println("404 Not Found: URLが間違っている、あるいは指定されたidのToDoがありません。" + api);
				return todo;
			}
			if (res.statusCode() == 200) {
				String body = res.body();
				todo = gson.fromJson(body, ToDo.class);
				return todo;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return todo;
	}

	public ArrayList<ToDo> getAll() {
		String api = url + "/todos";
		var todos = new ArrayList<ToDo>();
		try {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(api))
					.build();
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 404) {
				System.out.println("404 Not Found: URLが間違っています：" + api);
				return todos;
			}
			if (res.statusCode() == 200) {
				String body = res.body();
				todos = gson.fromJson(body, todosType);
				return todos;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return todos;
	}

	public ToDo create(String title, String date, boolean completed) {
		String api = url + "/todos";
		ToDo todo = null;
		try {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(api))
					.POST(HttpRequest.BodyPublishers
							.ofString("title=%s&date=%s&submit=%s".formatted(title, date, completed)))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.build();
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 404) {
				System.out.println("404 Not Found: URLが間違っている、あるいは指定されたidのToDoがありません。" + api);
				return todo;
			}
			if (res.statusCode() == 200) {
				String body = res.body();
				todo = gson.fromJson(body, ToDo.class);
				return todo;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return todo;
	}

	public void updateTitle(int id, String title) {
		String api = url + "/todos/" + id;
		try {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(api))
					.PUT(HttpRequest.BodyPublishers.ofString("title=" + title))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.build();
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 404) {
				System.out.println("404 Not Found: URLが間違っている、あるいは指定されたidのToDoがありません。" + api);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateDate(int id, String date) {
		String api = url + "/todos/" + id;
		try {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(api))
					.PUT(HttpRequest.BodyPublishers.ofString("date=" + date))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.build();
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 404) {
				System.out.println("404 Not Found: 指定されたidのToDoはありません。");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateCompleted(int id, boolean completed) {
		String api = url + "/todos/" + id;
		try {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(api))
					.PUT(HttpRequest.BodyPublishers.ofString("completed=" + completed))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.build();
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 404) {
				System.out.println("404 Not Found: 指定されたidのToDoはありません。");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void delete(int id) {
		String api = url + "/todos/" + id;
		try {
			var request = HttpRequest.newBuilder()
					.uri(URI.create(api))
					.DELETE()
					.build();
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 404) {
				System.out.println("404 Not Found: 指定されたidのToDoはありません。");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
