package com.hp.csbu.cc.middleware;

import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * An HTTP factory.
 * 
 * @author liemmn
 * 
 */
public class HttpClientFactory extends AuthClientFactory {
	private HttpClientPoolFactory clientPool;

	HttpClientFactory(String host, int port, int timeout, boolean clientAuth,
			String keyStore, String keyPass, String trustStore,
			String trustPass, String adminToken, int maxActive,
			long timeBetweenEvictionRunsMillis, long minEvictableIdleTimeMillis) {
		clientPool = new HttpClientPoolFactory(host, port, timeout, clientAuth,
				keyStore, keyPass, trustStore, trustPass, adminToken,
				maxActive, timeBetweenEvictionRunsMillis,
				minEvictableIdleTimeMillis);
		pool = new GenericObjectPool(clientPool);
	}

	@Override
	public void shutdown() {
		clientPool.shutDown();
		super.shutdown();
	}
}
