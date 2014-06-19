package com.hp.csbu.cc.middleware;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Config implements AuthConstants {

	// Thee faithful logger
  private static final Logger logger = LoggerFactory
    .getLogger(Config.class);

	private static final Config instance = new Config();
	
	private static final String PASSWORD = "password";
	private static final String ACCESS_KEY = "accesskey";

	// Application wide init param -- ServletContext
	private ServletContext context = null;

	// Memcache client--There shall only be one
	///private MemcacheCrypt client = null;
  //
  private TokenCache<String, String> client = null;

	// Auth client factory
	private AuthClientFactory factory = null;

	// The service IDs that this filter serves
	private String serviceIds;

	// The optional endpoint IDs that this filter serves
	private String endpointIds;

	// Memcache timeout value
	private long memCacheTimeOut;

  //the time to cache token
  private long timeToCacheToken;

	// flag to set if auth decision can be delegated to next filter
	private boolean delayAuthDecision;

	// retries and pauseTime configuration for retry logic
	private int retries;
	private int pauseTime;

	// configuration to authenticate against CS api
	private String authVersion;

	// flag to include catalog in the response 
	private boolean includeCatalog;

	// configuration for admin authentication method to be used for 2-way SSL
	private String adminAuthMethod;

	// configuration for admin default project
	private String adminProjectId;	

	// flag to indicate if the filter is already intialized with required parameters
	private volatile boolean initialized = false;

  //context is not getting properly filed so will use FilterConfig
  private FilterConfig filterConfig;
	private Config() {
	}

	public static Config getInstance() {
		return instance;
	}

	public synchronized void initialize(FilterConfig config, ServletRequest req, Map<String,String> map) throws ServletException {
		this.context = config.getServletContext();
    this.filterConfig = config;

		try {
			// Initialize serviceIds...

      serviceIds = filterConfig.getInitParameter(SERVICE_IDS);
			// Initialize endpointIds...
          endpointIds = filterConfig.getInitParameter(ENDPOINT_IDS);

      // Initialize auth server connection parameters...

      String host = filterConfig.getInitParameter(SERVER_VIP);

      int port = Integer.parseInt(filterConfig.getInitParameter(SERVER_PORT));
			
			// HP Keystone Server only supports authentication against
			// V3.0 api
			authVersion = getValue(AUTH_VERSION, "v3.0");

			if ((serviceIds == null || serviceIds.isEmpty())
					&& (endpointIds == null || endpointIds.isEmpty())
					&& authVersion.equalsIgnoreCase("v2.0")) {
				throw new Throwable("Need to specify " + SERVICE_IDS);
			}

			// Initialize memcache...
			String cacheHosts = context.getInitParameter(MEMCACHE_HOSTS);
			boolean isEncrypted = Boolean.valueOf(context
					.getInitParameter(MEMCACHE_ENCRYPT));
			memCacheTimeOut = getValue(MEMCACHE_TIMEOUT, 2000L);
		/*	if (cacheHosts != null && !cacheHosts.isEmpty()) {
				this.client = new MemcacheCrypt(cacheHosts, isEncrypted);
			}*/



			// Initialize Certificates

      String keyStore = filterConfig.getInitParameter(KEYSTORE);
      String keyPass =  filterConfig.getInitParameter(KEYSTORE_PASS);
      String trustStore = filterConfig.getInitParameter(TRUSTSTORE);
      String trustPass = filterConfig.getInitParameter(TRUSTSTORE_PASS);

      String adminToken = getValue(ADMIN_TOKEN, "");
			int timeout = getValue(CONN_TIMEOUT, 0);
			boolean clientAuth = getValue(CONN_SSL_CLIENT_AUTH, true);
			int maxActive = getValue(CONN_POOL_MAX_ACTIVE, 3);
			int maxIdle = getValue(CONN_POOL_MAX_IDLE, 3);
			long evictPeriod = getValue(CONN_POOL_EVICT_PERIOD, 60000L);
			long minIdleTime = getValue(CONN_POOL_MIN_IDLE_TIME, 90000L);
			retries = getValue(CONN_TIMEOUT_RETRIES, 3);
			pauseTime = getValue(PAUSE_BETWEEN_RETRIES, 100);
			delayAuthDecision = getValue(DELAY_AUTH_DECISION, false);
			includeCatalog = getValue(INCLUDE_SERVICE_CATALOG, true);
			adminAuthMethod = getValue(ADMIN_AUTH_METHOD, "");
			adminProjectId = getValue(ADMIN_PROJECT_ID, "");
			this.factory = AuthClientFactory.build(host, port, timeout,
					clientAuth, keyStore, keyPass, trustStore, trustPass,
					maxActive, maxIdle, evictPeriod, minIdleTime, adminToken);
			verifyRequiredParamsForAuthMethod();
      this.client = new TokenCache<>(20,map);
			logger.info("Auth host (2-way SSL: " + clientAuth + "): " + host);
			logger.info("Read Servlet Initialization Parameters ");
			initialized = true;
		} catch (Throwable t) {
			logger.error("Failed to read Servlet Initialization Parameters ",
					t.getMessage());
			throw new ServletException(
					"Failed to read Servlet Initialization Parameters :: "
							+ t.getMessage(), t);
		}
	}
	
	public boolean isInitialized() {
		return initialized;
	}

	protected String getAdminProject() {
		return adminProjectId;
	}

	protected String getAdminAccessKey() {
		if (context.getAttribute(ADMIN_ACCESS_KEY) != null) {
			return (String) context.getAttribute(ADMIN_ACCESS_KEY);
		} else {
			return getValue(ADMIN_ACCESS_KEY, "");
		}
	}

	protected String getAdminSecretKey() {
		if (context.getAttribute(ADMIN_SECRET_KEY) != null) {
			return (String) context.getAttribute(ADMIN_SECRET_KEY);
		} else {
			return getValue(ADMIN_SECRET_KEY, "");
		}
	}

	protected String getAdminAuthMethod() {
		return adminAuthMethod;
	}

	protected String getAdminUser() {
		if (context.getAttribute(ADMIN_USER) != null) {
			return (String) context.getAttribute(ADMIN_USER);
		} else {
			return getValue(ADMIN_USER, "");
		}
	}

	protected String getAdminPassword() {
		if (context.getAttribute(ADMIN_PASSWORD) != null) {
			return (String) context.getAttribute(ADMIN_PASSWORD);
		} else {
			return getValue(ADMIN_PASSWORD, "");
		}
	}

	protected boolean isIncludeCatalog() {
		return includeCatalog;
	}

	protected long getMemCacheTimeOut() {
		return memCacheTimeOut;
	}

	protected String getAuthVersion() {
		return authVersion;
	}

	protected void setMemCacheTimeOut(long memCacheTimeOut) {
		this.memCacheTimeOut = memCacheTimeOut;
	}

	// Is caching enabled?
	protected boolean isCaching() {
		return this.client != null;
  }

	protected ServletContext getConfig() {
		return context;
	}

  protected TokenCache<String,String> getClient() {
    return client;
  }

	protected AuthClientFactory getFactory() {
		return factory;
	}

	protected String getServiceIds() {
		return serviceIds;
	}

	protected String getEndpointIds() {
		return endpointIds;
	}

	protected boolean isDelayAuthDecision() {
		return delayAuthDecision;
	}

	protected int getRetries() {
		return retries;
	}

	protected int getPauseTime() {
		return pauseTime;
	}

  public long getTimeToCacheToken() { return timeToCacheToken; }

  public void setTimeToCacheToken(long timeToCachedToken) {
    this.timeToCacheToken = timeToCachedToken;
  }
	private <T> T getValue(String paramName, T defaultValue) {
		Class type = defaultValue.getClass();

    String initparamValue = filterConfig.getInitParameter(paramName);
		if (initparamValue != null && !initparamValue.isEmpty()) {
			if (type.equals(Integer.class)) {
				int paramValue = Integer.parseInt(initparamValue);
				return (T) type.cast(paramValue);
			} else if (type.equals(Long.class)) {
				long paramValue = Long.parseLong(initparamValue);
				return (T) type.cast(paramValue);
			} else if (type.equals(Boolean.class)) {
				boolean paramValue = Boolean.parseBoolean(initparamValue);
				return (T) type.cast(paramValue);
			} else if (type.equals(String.class)) {
				return (T) type.cast(initparamValue);
			}
		}
		return defaultValue;
	}

	private void verifyRequiredParamsForAuthMethod() {
		if (adminAuthMethod.equalsIgnoreCase(PASSWORD)) {
			if (getAdminUser().isEmpty() || getAdminPassword().isEmpty()) {
				String msg = String
						.format("admin user and password must be specified if admin auth method is %s",
								adminAuthMethod);
				throw new AuthException(msg);
			}
		} else if (adminAuthMethod.equalsIgnoreCase(ACCESS_KEY)) {
			if (getAdminAccessKey().isEmpty() || getAdminSecretKey().isEmpty()) {
				String msg = String
						.format("admin access and secret key must be specified if admin auth method is %s",
								adminAuthMethod);
				throw new AuthException(msg);
			}
		}
	}
}
