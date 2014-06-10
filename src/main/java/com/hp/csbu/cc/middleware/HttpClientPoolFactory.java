package com.hp.csbu.cc.middleware;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

/**
 * A Http request pool factory. Based on Apache Commons Pool.  Singleton.
 * Note that the Apache HttpClient maintains its own connection pool and 
 * does not participate in Apache Commons pool' lifecycle other than creating
 * HTTPRequests.
 * 
 * @author liemmn
 * 
 */
public class HttpClientPoolFactory extends BasePoolableObjectFactory {
	private URI uri;
	private PoolingClientConnectionManager connMgr;
	private HttpPoolCleaner cleaner;	
	private HttpClient client;

	HttpClientPoolFactory(String host, int port, int timeout,
			boolean clientAuth, String keyStore, String keyPass,
			String trustStore, String trustPass, String adminToken,
			int maxActive, long timeBetweenEvictionRunsMillis,
			long minEvictableIdleTimeMillis) {
		// Setup auth URL
		String protocol = (port == 35357) ? "https://" : "http://";
		String urlStr = protocol + host + ":" + port;
		uri = URI.create(urlStr);
		
		// Setup connection pool
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		if (protocol.startsWith("https")) {
			SSLSocketFactory sslf = sslFactory(keyStore, keyPass, trustStore,
					trustPass, clientAuth);
			schemeRegistry.register(new Scheme("https", port, sslf));
		} else {
			schemeRegistry.register(new Scheme("http", port, PlainSocketFactory
					.getSocketFactory()));
		}
		connMgr = new PoolingClientConnectionManager(schemeRegistry, 
				minEvictableIdleTimeMillis, TimeUnit.MILLISECONDS);
		connMgr.setMaxTotal(maxActive);
		connMgr.setDefaultMaxPerRoute(maxActive);
		
		// Http connection timeout
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
		
		// Create a single client
		client = new DefaultHttpClient(connMgr, params);
		
		// Create and start the connection pool cleaner
		cleaner = new HttpPoolCleaner(connMgr, timeBetweenEvictionRunsMillis,
				minEvictableIdleTimeMillis);
		new Thread(cleaner).start();
		
	}

	@Override
	public Object makeObject() throws Exception {
		return new HttpAuthClient(client, uri);
	}
	
	@Override
	public void passivateObject(Object obj) throws Exception {
		((HttpAuthClient) obj).reset();
	}
	
	@Override
	public void destroyObject(Object obj) throws Exception {
		((HttpAuthClient) obj).reset();
		obj = null;
	}

	public void shutDown() {
		// Shutdown all connections
		connMgr.shutdown();
		// Shutdown connection pool cleaner
		cleaner.shutdown();
	}
	
	// get a socket factory
	private static SSLSocketFactory sslFactory(String keyStore, String keyPass,
			String trustStore, String trustPass, boolean clientAuth) {
		try {
			// keystore
			KeyStore ks = null;
			if (clientAuth) {
				ks = KeyStore.getInstance("jks");
				FileInputStream is1 = new FileInputStream(new File(keyStore));
				try {
					ks.load(is1, keyPass.toCharArray());
				} finally {
					is1.close();
				}
			}
			// truststore
			KeyStore ts = KeyStore.getInstance("jks");
			FileInputStream is2 = new FileInputStream(
					new File(trustStore));
			try {
				ts.load(is2, trustPass.toCharArray());
			} finally {
				is2.close();
			}
			SSLSocketFactory sslf = new SSLSocketFactory(ks, keyPass, ts);
			return sslf;
		} catch (Exception e) {
			throw new AuthConnectionException(
					"Failed to create SSLSocketFactory", e);
		}
	}
}
