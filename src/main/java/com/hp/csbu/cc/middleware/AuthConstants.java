package com.hp.csbu.cc.middleware;

public interface AuthConstants {
	/** 'Confirmed' or 'Invalid' */
	public static enum IdentityStatus {
		Confirmed, Invalid
	}

	// =============================== TOKEN ===================================
	/** Credential (token) header */
	public static final String TOKEN = "X-AUTH-TOKEN";
	/** Auth status parameter */
	public static final String AUTH_IDENTITY_STATUS = "X-IDENTITY-STATUS";
	/** Auth user Id parameter */
	public static final String AUTH_USER_ID = "X-USER-ID";
	/** Auth user name parameter */
	public static final String AUTH_USER_NAME = "X-USER-NAME";

	/** Auth user roles parameter, comma-separated roles */
	public static final String AUTH_ROLES = "X-ROLES";
	/** json encoded keystone service catalog */
	public static final String AUTH_SERVICE_CATALOG = "X-SERVICE-CATALOG";
	/** Service Ids initialization parameter */
	public static final String SERVICE_IDS = "ServiceIds";
	/** Endpoint Ids initialization parameter */
	public static final String ENDPOINT_IDS = "EndpointIds";
	/** Keystone admin token for use in vanilla Keystone */
	public static final String ADMIN_TOKEN = "AdminToken";

	// ============================ CONNECTION =================================
	/** Auth server initialization parameter */
	public static final String SERVER_VIP = "ServerVIP";
	/** Auth server port: 9543 for Thrift, 35357 for HTTP. */
	public static final String SERVER_PORT = "ServerPort";
	/** connection timeout initialization parameter */
	public static final String CONN_TIMEOUT = "ConnTimeout";
	/** 2-way SSL initialization parameter: True or False */
	public static final String CONN_SSL_CLIENT_AUTH = "ConnSSLClientAuth";
	/** SSL keystore initialization parameter */
	public static final String KEYSTORE = "Keystore";
	/** SSL keystore password initialization parameter */
	public static final String KEYSTORE_PASS = "KeystorePass";
	/** SSL truststore initialization parameter */
	public static final String TRUSTSTORE = "Truststore";
	/** SSL truststore password initialization parameter */
	public static final String TRUSTSTORE_PASS = "TruststorePass";

	// ============================== POOLING ==================================
	/**
	 * Maximum number of objects that can be allocated by the pool (checked out
	 * to clients, or idle awaiting checkout) at a given time. When
	 * non-positive, there is no limit to the number of objects that can be
	 * managed by the pool at one time. When maxActive is reached, the pool is
	 * said to be exhausted. The default setting for this parameter is 8.
	 */
	public static final String CONN_POOL_MAX_ACTIVE = "ConnPoolMaxActive";
	/**
	 * Maximum number of objects that can sit idle in the pool at any time. When
	 * negative, there is no limit to the number of objects that may be idle at
	 * one time. The default setting for this parameter is 8.
	 */
	public static final String CONN_POOL_MAX_IDLE = "ConnPoolMaxIdle";
	/**
	 * How long the eviction thread should sleep before "runs" of examining idle
	 * objects. When non-positive, no eviction thread will be launched. The
	 * default setting for this parameter is -1 (i.e., idle object eviction is
	 * disabled by default).
	 */
	public static final String CONN_POOL_EVICT_PERIOD = "ConnPoolEvictPeriod";
	/**
	 * Minimum amount of time that an object may sit idle in the pool before it
	 * is eligible for eviction due to idle time. When non-positive, no object
	 * will be dropped from the pool due to idle time alone. This setting has no
	 * effect unless ConnPoolEvictPeriod > 0. The default setting for this
	 * parameter is 30 minutes.
	 */
	public static final String CONN_POOL_MIN_IDLE_TIME = "ConnPoolMinIdleTime";

	// ============================== CACHING ==================================
	/** Memcache hosts */
	public static final String MEMCACHE_HOSTS = "MemcacheHosts";
	/** Memcache connection timeout */
	public static final String MEMCACHE_TIMEOUT = "MemcacheTimeout";
	/** Memcache encryption */
	public static final String MEMCACHE_ENCRYPT = "MemcacheEncrypt";

	/** Number of connection timeout retries **/
	public static final String CONN_TIMEOUT_RETRIES = "ConnRetryTimes";
	/** Number of connection timeout retries **/
	public static final String PAUSE_BETWEEN_RETRIES = "ConnRetryInterval";
	/** Authentication decision is forwarded to next filter **/
	public static final String DELAY_AUTH_DECISION = "DelayAuthDecision";

	public static final String SIGNATURE_METHOD = "HmacSHA1";

  public static final String TIME_TO_CACHE_TOKEN ="TimeToCacheToken";
	/** Version of CS to authenticate the credentials **/
	public static final String AUTH_VERSION = "AuthVersion";

	/** Include Service Catalog as part of Authentication Response **/
	public static final String INCLUDE_SERVICE_CATALOG = "IncludeServiceCatalog";

	/**
	 * Identity service managed unique identifier, string. Only present if this
	 * is a project-scoped v3 token, or a tenant-scoped v2 token.
	 **/
	public static final String AUTH_PROJECT_ID = "X-PROJECT-ID";

	/**
	 * Project name, unique within owning domain, string. Only present if this
	 * is a project-scoped v3 token, or a tenant-scoped v2 token.
	 **/
	public static final String AUTH_PROJECT_NAME = "X-PROJECT-NAME";

	/**
	 * Identity service managed unique identifier of owning domain of project,
	 * string. Only present if this is a project-scoped v3 token. If this
	 * variable is set, this indicates that the PROJECT_NAME can only be assumed
	 * to be unique within this domain.
	 **/
	public static final String AUTH_PROJECT_DOMAIN_ID = "X-PROJECT-DOMAIN-ID";

	/**
	 * Name of owning domain of project, string. Only present if this is a
	 * project-scoped v3 token. If this variable is set, this indicates that the
	 * PROJECT_NAME can only be assumed to be unique within this domain.
	 **/
	public static final String AUTH_PROJECT_DOMAIN_NAME = "X-PROJECT-DOMAIN-NAME";

	/**
	 * Identity service managed unique identifier of owning domain of user,
	 * string. If this variable is set, this indicates that the USER_NAME can
	 * only be assumed to be unique within this domain.
	 **/
	public static final String AUTH_USER_DOMAIN_ID = "X-USER-DOMAIN-ID";

	/**
	 * Name of owning domain of user, string. If this variable is set, this
	 * indicates that the USER_NAME can only be assumed to be unique within this
	 * domain.
	 **/
	public static final String AUTH_USER_DOMAIN_NAME = "X-USER-DOMAIN-NAME";

	/**
	 * Identity service managed unique identifier, string. Only present if this
	 * is a domain-scoped v3 token.
	 **/
	public static final String AUTH_DOMAIN_ID = "X-DOMAIN-ID";

	/**
	 * Unique domain name, string. Only present if this is a domain-scoped v3
	 * token.
	 **/
	public static final String AUTH_DOMAIN_NAME = "X-DOMAIN-NAME";

	public static final String AUTH_HP_IDM_ROLES = "X-HP-IDM-Non-Tenant-Roles";
	
	public static final String REMOTE_HOST = "RemoteHost";
	public static final String REMOTE_ADDR = "RemoteAddress";

	// Depracated Headers.
	/** Auth user roles parameter, comma-separated roles */
	public static final String AUTH_ROLE = "X-ROLE";
	/** Auth tenant Id parameter */
	public static final String AUTH_TENANT_ID = "X-TENANT-ID";
	/** Auth tenant name parameter */
	public static final String AUTH_TENANT_NAME = "X-TENANT-NAME";
	/** Auth tenant name parameter */
	public static final String AUTH_TENANT = "X-TENANT";
	/**
	 * *Deprecated* in favor of HTTP_X_USER_ID and HTTP_X_USER_NAME User name,
	 * unique within owning domain, string
	 **/
	public static final String AUTH_USER = "X-USER";

	public static final String AUTH_SUBJECT_TOKEN = "X-Subject-Token";
	public static final String ADMIN_USER = "AdminUser";
	public static final String ADMIN_PASSWORD = "AdminPassword";
	public static final String ADMIN_AUTH_METHOD = "AdminAuthMethod";
	public static final String ADMIN_ACCESS_KEY = "AdminAccessKey";
	public static final String ADMIN_SECRET_KEY = "AdminSecretKey";
	public static final String ADMIN_PROJECT_ID = "AdminProjectId";

}