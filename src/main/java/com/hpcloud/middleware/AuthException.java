package com.hpcloud.middleware;

/**
 * An exception to indicate any authentication error.
 * 
 * @author liemmn
 *
 */
public class AuthException extends RuntimeException {

	public AuthException(String msg) {
		super(msg);
	}
	
	public AuthException(String msg, Exception e) {
		super(msg, e);
	}
}
