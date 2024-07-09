package com.example.exceptions;

public class ToDoServiceException extends Exception {
	public static enum Type {
		AUTHENTICATION_ERROR("authentication_error"),
		AUTHORIZATION_ERROR("authorization_error"),
		INTERNAL_SERVER_ERROR("internal_server_error"),
		IO_ERROR("io_error"),
		INTERRUPTED_ERROR("interrupted_error");
		
		private final String message;

		private Type(String message) {
            this.message = message;
        }
		
		public String getMessage() {
			return message;
		}
	}
	
	private final Type type;
	
	public Type getType() {
        return type;
    }
	
	public ToDoServiceException(Type type) {
		super(type.getMessage());
		this.type = type;
	}

	public ToDoServiceException(Type type, Throwable cause) {
		super(type.getMessage(), cause);
		this.type = type;
	}
}
