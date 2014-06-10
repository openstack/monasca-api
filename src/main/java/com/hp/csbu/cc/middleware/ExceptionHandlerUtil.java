package com.hp.csbu.cc.middleware;

public class ExceptionHandlerUtil {
	
	public final static String SERVICE_UNAVAILABLE = "Service Unavailable";
	public final static String UNAUTHORIZED_TOKEN = "Unauthorized Token";
	public final static String INTERNAL_SERVER_ERROR = "Internal Server Error";
	
	private ExceptionHandlerUtil() {	
	}
	
	public static String getStatusText(int errorCode) {
		if (errorCode == 401) {
			return UNAUTHORIZED_TOKEN;
		}
		if (errorCode == 503) {
			return SERVICE_UNAVAILABLE;
		}
		if (errorCode == 500) {
			return INTERNAL_SERVER_ERROR;
		}
		return "Unknown Error";

	}
	
	public static TokenExceptionHandler lookUpTokenException(Exception ex) {
		try {
			return TokenExceptionHandler.valueOf(ex.getClass().getSimpleName());
		} catch (IllegalArgumentException iae) {
			return TokenExceptionHandler.valueOf("ResourceException");
		}
	}

	/*public static SignatureExceptionHandler lookUpSignatureException(Exception ex) {
		try {
			return SignatureExceptionHandler.valueOf(ex.getClass().getSimpleName());
		} catch (IllegalArgumentException iae) {
			return SignatureExceptionHandler.valueOf("ResourceException");
		}
	} */

}
