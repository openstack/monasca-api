package com.hp.csbu.cc.middleware;

/**
 * Memcache encryption exception.
 * 
 * @author liemmn
 *
 */
public class EncryptionException extends RuntimeException {
	private static final long serialVersionUID = 8249423387842730866L;

	public EncryptionException(String msg) {
		super(msg);
	}
	
	public EncryptionException(String msg, Exception e) {
		super(msg, e);
	}
}
