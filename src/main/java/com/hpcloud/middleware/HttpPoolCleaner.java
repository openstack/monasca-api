package com.hpcloud.middleware;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;

/**
 * A runner to clean the connection pool! There should only be one!
 * 
 * @author liemmn
 * 
 */
public class HttpPoolCleaner implements Runnable {
	private final ClientConnectionManager connMgr;
	private long timeBetweenEvictionRunsMillis, minEvictableIdleTimeMillis;
	private volatile boolean shutdown;

	public HttpPoolCleaner(ClientConnectionManager connMgr,
			long timeBetweenEvictionRunsMillis, long minEvictableIdleTimeMillis) {
		this.connMgr = connMgr;
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * Start the cleaner.
	 */
	@Override
	public void run() {
		try {
			while (!shutdown) {
				synchronized (this) {
					wait(timeBetweenEvictionRunsMillis);
					// Close expired connections
					connMgr.closeExpiredConnections();
					// Close connections that have been idle longer than x sec
					connMgr.closeIdleConnections(minEvictableIdleTimeMillis,
							TimeUnit.MILLISECONDS);
				}
			}
		} catch (InterruptedException ex) {
			// terminate
		}
	}

	/**
	 * Shutdown the cleaner.
	 */
	public void shutdown() {
		shutdown = true;
		synchronized (this) {
			notifyAll();
		}
	}

}
