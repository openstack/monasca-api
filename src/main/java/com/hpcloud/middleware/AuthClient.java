package com.hpcloud.middleware;


import java.util.Map;
import org.apache.http.client.ClientProtocolException;
import org.apache.thrift.TException;

/**
 * A client that can communicate to an authentication server for authentication.
 * 
 * @author liemmn
 * 
 */
public interface AuthClient {
	public Object validateTokenForServiceEndpointV2(String token,
			String serviceIds, String endpointIds, boolean includeCatalog)
					throws TException, ClientProtocolException;
	public Object validateTokenForServiceEndpointV3(String token,
			Map<String, String> inputParams) throws TException, ClientProtocolException;
	

}
