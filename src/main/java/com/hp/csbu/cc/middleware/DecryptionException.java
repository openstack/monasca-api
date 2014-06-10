package com.hp.csbu.cc.middleware;

/**
 * An exception caused for memcache decryption issues.
 * 
 * @author liemmn
 *
 */
public class DecryptionException extends RuntimeException {
	private static final long serialVersionUID = 5463487714560524511L;

	public DecryptionException(String msg) {
		super(msg);
	}
	
	public DecryptionException(String msg, Exception e) {
		super(msg, e);
	}
}
