package com.hp.csbu.cc.middleware;

import org.apache.commons.pool.impl.GenericObjectPool;

//import com.hp.csbu.cc.security.cs.thrift.service.CsThriftService.Client;

/**
 * A factory for building {@link AuthClient}s.
 * 
 * @author liemmn
 *
 */
public abstract class AuthClientFactory {
	private static AuthClientFactory instance = null;
	protected static GenericObjectPool pool;
	
	/**
	 * Build a AuthClientFactory. Singleton.
	 * 
	 * @param host
	 *            Auth host
	 * @param port
	 *            Auth port
	 * @param timeout
	 *            Auth connection timeout
	 * @param clientAuth
	 *            2-way SSL (if false, 1-way SSL is used)
	 * @param keyStore
	 *            Keystore
	 * @param keyPass
	 *            Keystore password
	 * @param trustStore
	 *            Truststore
	 * @param trustPass
	 *            Truststore password
	 * @param maxActive
	 *            Maximum number of objects that can be allocated by the pool
	 *            (checked out to clients, or idle awaiting checkout) at a given
	 *            time. When non-positive, there is no limit to the number of
	 *            objects that can be managed by the pool at one time. When
	 *            maxActive is reached, the pool is said to be exhausted. The
	 *            default setting for this parameter is 8.
	 * @param maxIdle
	 *            Maximum number of objects that can sit idle in the pool at any
	 *            time. When negative, there is no limit to the number of
	 *            objects that may be idle at one time. The default setting for
	 *            this parameter is 8.
	 * @param timeBetweenEvictionRunsMillis
	 *            How long the eviction thread should sleep before "runs" of
	 *            examining idle objects. When non-positive, no eviction thread
	 *            will be launched. The default setting for this parameter is -1
	 *            (i.e., idle object eviction is disabled by default).
	 * @param minEvictableIdleTimeMillis
	 *            Minimum amount of time that an object may sit idle in the pool
	 *            before it is eligible for eviction due to idle time. When
	 *            non-positive, no object will be dropped from the pool due to
	 *            idle time alone. This setting has no effect unless
	 *            timeBetweenEvictionRunsMillis > 0. The default setting for
	 *            this parameter is 30 minutes.
	 *  @param adminToken
	 *  		  Admin token for use with vanilla Keystone.
	 * 
	 * @return AuthClientFactory singleton.
	 * @throws Exception
	 */
	public static synchronized AuthClientFactory build(String host, int port,
			int timeout, boolean clientAuth, String keyStore, String keyPass,
			String trustStore, String trustPass, int maxActive, int maxIdle,
			long timeBetweenEvictionRunsMillis,
			long minEvictableIdleTimeMillis, String adminToken)
			throws Exception {
		if (instance == null) {
			/*if (port == 9543) {
				instance = new ThriftClientFactory(host, port, timeout,
						clientAuth, keyStore, keyPass, trustStore, trustPass);
			} else {*/
				instance = new HttpClientFactory(host, port, timeout,
						clientAuth, keyStore, keyPass, trustStore, trustPass,
						adminToken, maxActive, timeBetweenEvictionRunsMillis, 
						minEvictableIdleTimeMillis);
		//	}

			// Pool tweaking
			pool.setMaxActive(maxActive);
			pool.setMaxIdle(maxIdle);
			pool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
			pool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);			
		}
		return instance;
	}
	
	/**
	 * Get a client. Don't forget to {@link #recycleClient(Client)} after you
	 * are done using it, successfully or not.
	 * 
	 * @return Client
	 * @throws Exception
	 */	
	public AuthClient getClient() {
		try {
			return (AuthClient) pool.borrowObject();
		} catch (Exception e) {
			throw new AuthConnectionException("Failed to get a client "+ e.getMessage(), e);
		}	
	}
	
	/**
	 * Recycle the client for next usage.
	 * 
	 * @param client
	 *            Client to recycle
	 * @throws Exception
	 */
	public void recycle(AuthClient client) {
		try {
			pool.returnObject(client);
		} catch (Exception e) {
			throw new AuthConnectionException("Failed to recycle client", e);
		}
	}
	
	/**
	 * Call this if the client is unusable (i.e., exception).
	 * 
	 * @param client
	 *            Client to discard.
	 */
	public void discard(AuthClient client) {
		try {
			pool.invalidateObject(client);
		} catch (Exception e) {
			throw new AuthConnectionException("Failed to destroy client", e);
		}
	}	
	
	/**
	 * Shut down this factory.
	 */
	public void shutdown() {
		try {
			pool.close();
		} catch (Exception e) {
			throw new AuthConnectionException("Failed to close client pool", e);
		}
	}
}
