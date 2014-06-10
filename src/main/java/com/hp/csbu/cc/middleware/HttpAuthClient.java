package com.hp.csbu.cc.middleware;

import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_SUBJECT_TOKEN;
import static com.hp.csbu.cc.middleware.AuthConstants.TOKEN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//import com.hp.csbu.cc.security.cs.thrift.service.AuthResponse;
//import com.hp.csbu.cc.security.cs.thrift.service.SigAuthRequest;

public class HttpAuthClient implements AuthClient {
	private static final String ACCESSKEY = "accesskey";
	private static final String PASSWORD = "password";
	private static final String SERVICE_IDS_PARAM = "serviceIds";
	private static final String ENDPOINT_IDS_PARAM = "endpointIds";
	private static final int DELTA_TIME_IN_SEC = 30;
	private static SimpleDateFormat expiryFormat;
	static {
		expiryFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmmmmm'Z'");
		expiryFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	private final Config appConfig = Config.getInstance();

	private HttpClient client;
	private String adminToken;
	private String adminTokenExpiry;
	private URI uri;

	public HttpAuthClient(HttpClient client, URI uri) {
		this.client = client;
		this.uri = uri;
	}

	@Override
	public Object validateTokenForServiceEndpointV2(String token,
			String serviceIds, String endpointIds, boolean includeCatalog)
			throws ClientProtocolException {
		String newUri = uri.toString() + "/v2.0/tokens/" + token;
		return verifyUUIDToken(token, newUri, null, serviceIds, endpointIds);
	}

	@Override
	public Object validateTokenForServiceEndpointV3(String token,
			Map<String, String> inputParams) throws ClientProtocolException {
		String newUri = uri.toString() + "/v3/auth/tokens/";
		Header[] header = new Header[1];
		header[0] = new BasicHeader(AUTH_SUBJECT_TOKEN, token);
		String serviceIds = null;
		String endpointIds = null;
		if (inputParams.containsKey(SERVICE_IDS_PARAM))
			serviceIds = inputParams.get(SERVICE_IDS_PARAM);
		if (inputParams.containsKey(ENDPOINT_IDS_PARAM))
			endpointIds = inputParams.get(ENDPOINT_IDS_PARAM);
		return verifyUUIDToken(token, newUri, header, serviceIds, endpointIds);
	}

	private Object verifyUUIDToken(String token, String newUri,
			Header[] header, String serviceIds, String endpointIds)
			throws ClientProtocolException {
		HttpResponse response = sendGet(newUri, header, serviceIds, endpointIds);
		int code = response.getStatusLine().getStatusCode();
		if (code == 404) {
			throw new AuthException("Authorization failed for token: " + token);
		}
		if (code != 200) {
			adminToken = null;
			throw new AuthException("Failed to validate via HTTP " + code
					+ " " +response.getStatusLine().getReasonPhrase());
		}
		return parseResponse(response);
	}

	private HttpResponse sendPost(String uri, StringEntity body)
			throws ClientProtocolException {
		HttpResponse response = null;
		HttpPost post = new HttpPost(uri);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		try {
			post.setEntity(body);
			response = client.execute(post);
			int code = response.getStatusLine().getStatusCode();
			if (!(code == 201 || code == 200 || code == 203)) {
				adminToken = null;
				throw new AuthException(
						"Failed to authenticate admin credentials " + code
								+ response.getStatusLine().getReasonPhrase());
			}
		} catch (IOException e) {
			post.abort();
			throw new ClientProtocolException(
					"IO Exception during POST request ", e);
		}
		return response;
	}

	private HttpResponse sendGet(String newUri, Header[] headers,
			String serviceIds, String endpointIds)
			throws ClientProtocolException {
		HttpResponse response = null;
		HttpGet get = null;
		boolean hasServiceIds = false;
		if (serviceIds != null && !serviceIds.isEmpty()) {
			newUri += "?HP-IDM-serviceId=" + serviceIds;
			hasServiceIds = true;
		}
		if (endpointIds != null && !endpointIds.isEmpty()) {
			newUri += hasServiceIds ? "&HP-IDM-endpointTemplateId="
					+ endpointIds : "?HP-IDM-endpointTemplateId=" + endpointIds;
		}

		get = new HttpGet(newUri);
		get.setHeader("Accept", "application/json");
		get.setHeader("Content-Type", "application/json");
		if (headers != null) {
			for (Header header : headers) {
				get.setHeader(header);
			}
		}
		if (!appConfig.getAdminAuthMethod().isEmpty()) {
			get.setHeader(new BasicHeader(TOKEN, getAdminToken()));
		}
		try {
			response = client.execute(get);
		} catch (IOException e) {
			get.abort();
			throw new ClientProtocolException(
					"IO Exception during GET request ", e);
		}
		return response;
	}

	private String parseResponse(HttpResponse response) {
		StringBuffer json = new StringBuffer();
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream instream;
			try {
				instream = entity.getContent();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(instream));
				String line = reader.readLine();
				while (line != null) {
					json.append(line);
					line = reader.readLine();
				}
			} catch (Exception e) {
				throw new AuthException("Failed to parse Http Response ", e);
			}
		}
		return json.toString();
	}

	private String getAdminToken() throws ClientProtocolException {
		HttpResponse response;
		String json;
		JsonParser jp = new JsonParser();

		if (adminTokenExpiry != null) {
			if (isExpired(adminTokenExpiry)) {
				adminToken = null;
			}
		}
		if (adminToken == null) {
			if (appConfig.getAuthVersion().equalsIgnoreCase("v2.0")) {
				StringEntity params = getUnscopedV2AdminTokenRequest();
				String authUri = uri + "/v2.0/tokens";
				response = sendPost(authUri, params);
				json = parseResponse(response);
				JsonObject access = jp.parse(json).getAsJsonObject()
						.get("access").getAsJsonObject();
				JsonObject token = access.get("token").getAsJsonObject();
				adminToken = token.get("id").getAsString();
				adminTokenExpiry = token.get("expires").getAsString();
			} else {
				StringEntity params = getUnscopedV3AdminTokenRequest();
				String authUri = uri + "/v3/auth/tokens";
				response = sendPost(authUri, params);
				adminToken = response.getFirstHeader(AUTH_SUBJECT_TOKEN)
						.getValue();
				json = parseResponse(response);
				JsonObject token = jp.parse(json).getAsJsonObject()
						.get("token").getAsJsonObject();
				adminTokenExpiry = token.get("expires_at").getAsString();

			}
		}
		return adminToken;
	}

	private StringEntity getUnscopedV2AdminTokenRequest() {
		StringBuffer bfr = new StringBuffer();
		if (appConfig.getAdminAuthMethod().equalsIgnoreCase(PASSWORD)) {
			bfr.append("{\"auth\": {\"passwordCredentials\": {\"username\": \"");
			bfr.append(appConfig.getAdminUser());
			bfr.append("\",\"password\": \"");
			bfr.append(appConfig.getAdminPassword());
			if (appConfig.getAdminProject() != null && !appConfig.getAdminProject().isEmpty()) {
				bfr.append("\"}, \"tenantId\": \"");
				bfr.append(appConfig.getAdminProject());
				bfr.append("\"}}");
			} else {
				bfr.append("\"}}}");
			}
			try {
				return new StringEntity(bfr.toString());
			} catch (UnsupportedEncodingException e) {
				throw new AuthException("Invalid V2 authentication request "
						+ e);
			}
		} else {
			String msg = String.format("Admin auth method %s not supported",appConfig.getAdminAuthMethod());
			throw new AuthException(msg);
		}
	}

	private StringEntity getUnscopedV3AdminTokenRequest() {
		StringBuffer bfr = new StringBuffer();
		if (appConfig.getAdminAuthMethod().equalsIgnoreCase(PASSWORD)) {
			bfr.append("{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"name\": \"");
			bfr.append(appConfig.getAdminUser());
			bfr.append("\",\"password\": \"");
			bfr.append(appConfig.getAdminPassword());
			if (appConfig.getAdminProject() != null && !appConfig.getAdminProject().isEmpty()) {
				bfr.append("\"},\"scope\": { \"project\": { \"id\": \"");
				bfr.append(appConfig.getAdminProject());
				bfr.append("\"}}}}}}");
			} else {
				bfr.append("\"}}}}}");
			}
		} else if (appConfig.getAdminAuthMethod().equalsIgnoreCase(ACCESSKEY)) {
			bfr.append("{\"auth\": {\"identity\": {\"methods\": [\"accessKey\"], \"accessKey\": { \"accessKey\": \"");
			bfr.append(appConfig.getAdminAccessKey());
			bfr.append("\", \"secretKey\": \"");
			bfr.append(appConfig.getAdminSecretKey());
			if (appConfig.getAdminProject() != null && !appConfig.getAdminProject().isEmpty()) {
				bfr.append("\"},\"scope\": { \"project\": { \"id\": \"");
				bfr.append(appConfig.getAdminProject());
				bfr.append("\"}}}}}");
			} else {
				bfr.append("\"}}}}");
			}
		} else {
			String msg = String.format("Admin auth method %s not supported",appConfig.getAdminAuthMethod());
			throw new AuthException(msg);
		}
		try {
			return new StringEntity(bfr.toString());
		} catch (UnsupportedEncodingException e) {
			throw new AuthException("Invalid V3 authentication request " + e);
		}
	}

	private boolean isExpired(String expires) {
		Date tokenExpiryDate = null;
		try {
			tokenExpiryDate = expiryFormat.parse(expires);
		} catch (ParseException e) {
			return true;
		}
		Date current = new Date();
		return tokenExpiryDate.getTime() < (current.getTime() + DELTA_TIME_IN_SEC * 1000);
	}

	public void reset() {
	}

/*	@Override
	public AuthResponse validateSignature(SigAuthRequest request) {
		// TODO Auto-generated method stub
		return null;
	}*/
}
