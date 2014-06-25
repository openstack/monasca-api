package com.hp.csbu.cc.middleware;

public class SignatureBuilderException extends RuntimeException {

	private static final long serialVersionUID = -2643382825421961020L;
	
	public SignatureBuilderException(String msg) {
		super(msg);
	}
	public SignatureBuilderException(String msg, Exception e) {
		super(msg, e);
	}


}
