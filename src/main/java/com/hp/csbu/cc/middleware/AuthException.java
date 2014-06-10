package com.hp.csbu.cc.middleware;

/**
 * An exception to indicate any authentication error.
 * 
 * @author liemmn
 *
 */
public class AuthException extends RuntimeException {
	private static final long serialVersionUID = 2287073516214658461L;

	public AuthException(String msg) {
		super(msg);
	}
	
	public AuthException(String msg, Exception e) {
		super(msg, e);
	}
}
