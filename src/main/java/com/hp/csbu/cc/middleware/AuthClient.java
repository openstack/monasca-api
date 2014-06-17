package com.hp.csbu.cc.middleware;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.thrift.TException;
import org.apache.thrift.TException;


import com.hp.csbu.cc.security.cs.thrift.service.AuthResponse;
import com.hp.csbu.cc.security.cs.thrift.service.ResourceException;
import com.hp.csbu.cc.security.cs.thrift.service.SigAuthRequest;


/**
 * A client that can communicate to an authentication server for authentication.
 * 
 * @author liemmn
 * 
 */
public interface AuthClient {
	public Object validateTokenForServiceEndpointV2(String token,
			String serviceIds, String endpointIds, boolean includeCatalog)
					throws TException, ClientProtocolException; //ResourceException
	public Object validateTokenForServiceEndpointV3(String token,
			Map<String, String> inputParams) throws TException, ClientProtocolException; //ResourceException
	
	public AuthResponse validateSignature(SigAuthRequest request) throws ResourceException, TException;

}
