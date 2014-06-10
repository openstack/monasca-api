package com.hp.csbu.cc.middleware;

import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_DOMAIN_ID;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_DOMAIN_NAME;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_HP_IDM_ROLES;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_IDENTITY_STATUS;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_PROJECT_DOMAIN_ID;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_PROJECT_DOMAIN_NAME;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_PROJECT_ID;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_PROJECT_NAME;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_ROLE;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_ROLES;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_SERVICE_CATALOG;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_TENANT;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_TENANT_NAME;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_USER;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_USER_DOMAIN_ID;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_USER_DOMAIN_NAME;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_USER_ID;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_TENANT_ID;
import static com.hp.csbu.cc.middleware.AuthConstants.AUTH_USER_NAME;
import static com.hp.csbu.cc.middleware.AuthConstants.IdentityStatus;

/*import com.hp.csbu.cc.security.cs.thrift.service.AuthResponseV2;
import com.hp.csbu.cc.security.cs.thrift.service.AuthResponseV3;
import com.hp.csbu.cc.security.cs.thrift.service.EndpointV3;
import com.hp.csbu.cc.security.cs.thrift.service.Role;
import com.hp.csbu.cc.security.cs.thrift.service.ServiceForCatalogV3;
import com.hp.csbu.cc.security.cs.thrift.service.V3Role;
*/

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletRequest;

//import net.rubyeye.xmemcached.exception.MemcachedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FilterUtils {

	private FilterUtils() {
	}

	private static final Config appConfig = Config.getInstance();

	private static final Gson gson = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.FINAL)
			.create();

	// Thee faithful logger
	private static final Logger logger = LoggerFactory
			.getLogger(FilterUtils.class);

	public static void destroyFilter() {
		/*MemcacheCrypt client = appConfig.getClient();
		// Shutdown memcache
		if (client != null) {
			try {
				client.shutdown();
			} catch (IOException e) {
				logger.warn("Failed to shutdown memcache", e);
			}
		}   */

		AuthClientFactory factory = appConfig.getFactory();
		// Shutdown factory
		if (factory != null) {
			factory.shutdown();
		}
	}

	public static ServletRequest wrapRequestFromHttpResponse(
			ServletRequest req, String data) {
		if (appConfig.getAuthVersion().equalsIgnoreCase("v2.0")) {
			wrapRequestFromHttpV2Response(req, data);

		} else {
			wrapRequestFromHttpV3Response(req, data);
		}
		return req;
	}

	private static void wrapRequestFromHttpV3Response(ServletRequest req,
			String data) {
		StringBuilder tenants = new StringBuilder();
		StringBuilder nonTenants = new StringBuilder();
		JsonParser jp = new JsonParser();
		JsonObject token = jp.parse(data).getAsJsonObject().get("token")
				.getAsJsonObject();
		// Domain Scoped Token
		if (token.get("domain") != null) {
			JsonObject domain = token.get("domain").getAsJsonObject();
			req.setAttribute(AUTH_DOMAIN_ID, domain.get("id").getAsString());
			if (domain.get("name") != null) {
				req.setAttribute(AUTH_DOMAIN_NAME, domain.get("name")
						.getAsString());
			}
		}
		// Project Scoped Token
		if (token.get("project") != null) {
			JsonObject project = token.get("project").getAsJsonObject();
			req.setAttribute(AUTH_PROJECT_ID, project.get("id").getAsString());
			req.setAttribute(AUTH_PROJECT_NAME, project.get("name")
					.getAsString());

			JsonObject projectDomain = project.get("domain").getAsJsonObject();
			// special case where the value of id is null and the
			// projectDomain.get("id") != null
			if (!projectDomain.get("id").equals(new JsonNull())) {
				req.setAttribute(AUTH_PROJECT_DOMAIN_ID, projectDomain
						.get("id").getAsString());
			}
			if (projectDomain.get("name") != null) {
				req.setAttribute(AUTH_PROJECT_DOMAIN_NAME,
						projectDomain.get("name"));
			}
		}
		// User info
		if (token.get("user") != null) {
			JsonObject user = token.get("user").getAsJsonObject();
			req.setAttribute(AUTH_USER_ID, user.get("id").getAsString());
			req.setAttribute(AUTH_USER_NAME, user.get("name").getAsString());

			JsonObject userDomain = user.get("domain").getAsJsonObject();
			if (userDomain.get("id") != null) {
				req.setAttribute(AUTH_USER_DOMAIN_ID, userDomain.get("id")
						.getAsString());
			}
			if (userDomain.get("name") != null) {
				req.setAttribute(AUTH_USER_DOMAIN_NAME, userDomain.get("name")
						.getAsString());
			}

		}
		// Roles
		JsonArray roles = token.getAsJsonArray("roles");
		if (roles != null) {
			Iterator<JsonElement> it = roles.iterator();
			while (it.hasNext()) {
				JsonObject role = it.next().getAsJsonObject();
				if (role.get("HP-IDM") != null) {
					JsonObject hpIdm = role.get("HP-IDM").getAsJsonObject();
					if (hpIdm.get("projectId") != null) {
						tenants.append(",");
						tenants.append(role.get("name").getAsString());
					} else {
						nonTenants.append(",");
						nonTenants.append(role.get("name").getAsString());
					}
				}
			}
		}
		String tenantRoles = (tenants.length() > 0) ? tenants.substring(1)
				: tenants.toString();
		String nonTenantRoles = (nonTenants.length() > 0) ? nonTenants
				.substring(1) : nonTenants.toString();
		if (!tenantRoles.equals("")) {
			req.setAttribute(AUTH_ROLES, tenantRoles);
		}
		if (!nonTenantRoles.equals("")) {
			req.setAttribute(AUTH_HP_IDM_ROLES, nonTenantRoles);
		}
		// Catalog
		if (token.get("catalog") != null && appConfig.isIncludeCatalog()) {
			JsonArray catalog = token.get("catalog").getAsJsonArray();
			req.setAttribute(AUTH_SERVICE_CATALOG, catalog.toString());
		}
	}

	private static void wrapRequestFromHttpV2Response(ServletRequest req,
			String data) {
		StringBuilder tenants = new StringBuilder();
		StringBuilder nonTenants = new StringBuilder();
		JsonParser jp = new JsonParser();
		JsonObject access = jp.parse(data).getAsJsonObject().get("access")
				.getAsJsonObject();
		JsonObject token = access.get("token").getAsJsonObject();

		// Tenant info
		if (token.get("tenant") != null) {
			JsonObject tenant = token.get("tenant").getAsJsonObject();

			String id = tenant.get("id").getAsString();
			String name = tenant.get("name").getAsString();
			if (id != null)
				req.setAttribute(AUTH_TENANT_ID, id);
			if (name != null)
				req.setAttribute(AUTH_TENANT_NAME, name);
		}
		// User info
		if (access.get("user") != null) {
			JsonObject user = access.get("user").getAsJsonObject();

			String userId = user.get("id").getAsString();
			String username = user.get("name").getAsString();
			if (userId != null)
				req.setAttribute(AUTH_USER_ID, userId);
			if (username != null)
				req.setAttribute(AUTH_USER_NAME, username);
			// Roles
			JsonArray roles = user.getAsJsonArray("roles");
			if (roles != null) {
				Iterator<JsonElement> it = roles.iterator();
				while (it.hasNext()) {
					JsonObject role = it.next().getAsJsonObject();
					if (role.get("tenantId") != null) {
						tenants.append(",");
						tenants.append(role.get("name").getAsString());
					} else {
						nonTenants.append(",");
						nonTenants.append(role.get("name").getAsString());
					}
				}
			}
			String tenantRoles = (tenants.length() > 0) ? tenants.substring(1)
					: tenants.toString();
			if (!tenantRoles.equals("")) {
				req.setAttribute(AUTH_ROLES, tenantRoles);
			}
			String nonTenantRoles = (nonTenants.length() > 0) ? nonTenants
					.substring(1) : nonTenants.toString();
			if (!nonTenantRoles.equals("")) {
				req.setAttribute(AUTH_HP_IDM_ROLES, nonTenantRoles);
			}
		}
		// Service catalog
		if (access.get("serviceCatalog") != null
				&& appConfig.isIncludeCatalog()) {
			JsonArray serviceCatalog = access.get("serviceCatalog")
					.getAsJsonArray();
			req.setAttribute(AUTH_SERVICE_CATALOG, serviceCatalog.toString());
		}
	}

	public static ServletRequest wrapRequest(ServletRequest req, Object data) {
		if (data == null) {
			req.setAttribute(AUTH_IDENTITY_STATUS,
					IdentityStatus.Invalid.toString());
			logger.debug("Failed Authentication. Setting identity status header to Invalid");
		}
		req.setAttribute(AUTH_IDENTITY_STATUS,
				IdentityStatus.Confirmed.toString());
		//if (data instanceof String) {
			wrapRequestFromHttpResponse(req, ((String) data));
		//} else {
		//	wrapRequestFromThriftResponse(req, data);
		//}
		return req;
	}

	/*private static void wrapRequestFromThriftResponse(ServletRequest req,
			Object data) {
		StringBuilder tenants = new StringBuilder();
		StringBuilder nonTenants = new StringBuilder();
		if (data instanceof AuthResponseV2) {
			AuthResponseV2 auth = (AuthResponseV2) data;
			req.setAttribute(AUTH_TENANT_ID, auth.userInfo.tenantId);
			req.setAttribute(AUTH_TENANT_NAME, auth.userInfo.tenantName);
			req.setAttribute(AUTH_USER_ID, auth.userInfo.userId);
			req.setAttribute(AUTH_USER_NAME, auth.userInfo.username);
			getRoles(auth.userInfo.roles, tenants, nonTenants);
			String tenantRoles = (tenants.length() > 0) ? tenants.substring(1)
					: tenants.toString();
			if (!tenantRoles.equals("")) {
				req.setAttribute(AUTH_ROLES, tenantRoles);
			}
			String nonTenantRoles = (nonTenants.length() > 0) ? nonTenants
					.substring(1) : nonTenants.toString();
			if (!nonTenantRoles.equals("")) {
				req.setAttribute(AUTH_HP_IDM_ROLES, nonTenantRoles);
			}
			if (auth.getServiceCatalog() != null) {
				req.setAttribute(AUTH_SERVICE_CATALOG,
						gson.toJson(auth.getServiceCatalog()));
			}

		} else if (data instanceof AuthResponseV3) {
			AuthResponseV3 auth = (AuthResponseV3) data;
			if (auth.getToken().getDomain() != null) {
				req.setAttribute(AUTH_DOMAIN_ID, auth.getToken().getDomain()
						.getId());
				if (auth.getToken().getDomain().getName() != null) {
					req.setAttribute(AUTH_DOMAIN_NAME, auth.getToken()
							.getDomain().getName());
				}
			} else if (auth.getToken().getProject() != null) {
				req.setAttribute(AUTH_PROJECT_ID, auth.getToken().getProject()
						.getId());
				req.setAttribute(AUTH_PROJECT_NAME, auth.getToken()
						.getProject().getName());
				req.setAttribute(AUTH_PROJECT_DOMAIN_ID, auth.getToken()
						.getProject().getDomain().getId());
				if (auth.getToken().getProject().getDomain().getName() != null) {
					req.setAttribute(AUTH_PROJECT_DOMAIN_NAME, auth.getToken()
							.getProject().getDomain().getName());
				}
			}
			req.setAttribute(AUTH_USER_ID, auth.getToken().getUser()
					.getUserId());
			req.setAttribute(AUTH_USER_NAME, auth.getToken().getUser()
					.getUsername());
			req.setAttribute(AUTH_USER_DOMAIN_ID, auth.getToken().getUser()
					.getDomain().getId());
			if (auth.getToken().getUser().getDomain().getName() != null) {
				req.setAttribute(AUTH_USER_DOMAIN_NAME, auth.getToken()
						.getUser().getDomain().getName());
			}
			getRoles(auth.getToken().getRoles(), tenants, nonTenants);
			String tenantRoles = (tenants.length() > 0) ? tenants.substring(1)
					: tenants.toString();
			String nonTenantRoles = (nonTenants.length() > 0) ? nonTenants
					.substring(1) : nonTenants.toString();
			if (!tenantRoles.equals("")) {
				req.setAttribute(AUTH_ROLES, tenantRoles);
			}
			if (!nonTenantRoles.equals("")) {
				req.setAttribute(AUTH_HP_IDM_ROLES, nonTenantRoles);
			}
			if (auth.getToken().getCatalog() != null) {
				req.setAttribute(AUTH_SERVICE_CATALOG, gson
						.toJson(buildServiceCatalogV3(auth.getToken()
								.getCatalog())));
			}

			setDeprecatedHeaders(req, auth, tenantRoles);
		}
	}  */

	/*private static List<CatalogV3> buildServiceCatalogV3(
			List<ServiceForCatalogV3> catalogs) {
		List<CatalogV3> v3Catalogs = new ArrayList<CatalogV3>();
		for (ServiceForCatalogV3 catalog : catalogs) {
			CatalogV3 catalogv3 = new CatalogV3();
			catalogv3.setId(catalog.getId());
			catalogv3.setType(catalog.getType());
			List<EndpointV3> endPoints = catalog.getEndpoints();
			List<Properties> endPointsv3 = new ArrayList<Properties>();
			for (EndpointV3 endPoint : endPoints) {
				Properties endPointv3 = new Properties();
				if (endPoint.getInterfaceName() != null) {
					endPointv3.put("interface", endPoint.getInterfaceName());
				}
				if (endPoint.getEndpointId() != null) {
					endPointv3.put("id", endPoint.getEndpointId());
				}
				if (endPoint.getServiceId() != null) {
					endPointv3.put("service_id", endPoint.getServiceId());
				}
				if (endPoint.getRegion() != null) {
					endPointv3.put("region", endPoint.getRegion());
				}
				if (endPoint.getUrl() != null) {
					endPointv3.put("url", endPoint.getUrl());
				}
				endPointsv3.add(endPointv3);
			}
			catalogv3.setEndPoints(endPointsv3);
			v3Catalogs.add(catalogv3);
		}
		return v3Catalogs;
	}  */

	// Method will be removed after keystone removes the deprecated headers.
	/*private static void setDeprecatedHeaders(ServletRequest req,
			AuthResponseV3 auth, String tenantRoles) {
		// Deprecated
		req.setAttribute(AUTH_USER, auth.getToken().getUser().getUsername());
		if (auth.getToken().getProject() != null) {
			req.setAttribute(AUTH_TENANT_ID, auth.getToken().getProject()
					.getId());
			req.setAttribute(AUTH_TENANT_NAME, auth.getToken().getProject()
					.getName());
			req.setAttribute(AUTH_TENANT, auth.getToken().getProject()
					.getName());
		}
		if (!tenantRoles.equals("")) {
			req.setAttribute(AUTH_ROLE, tenantRoles);
		}
	} */

	// Insert token into cache
	public static void cacheToken(String token, Object auth) {
		if (isCaching()) {
      appConfig.getClient().put(token, auth);
			/*try {
				appConfig.getClient().putToken(token, auth);
			} catch (TimeoutException e) {
				logger.error("Error timeout setting memcache: " + token);
			} catch (InterruptedException e) {
				logger.error("Error memcache interrupted");
			} catch (MemcachedException e) {
				logger.error("Error memcache", e);
			} */
		}
	}

	// Get token from cache
	public static Object getCachedToken(String token) {
		if (isCaching()) {
			long timeout = appConfig.getMemCacheTimeOut();

      /*try {
        return appConfig.getClient().getToken(token, timeout);
			} catch (TimeoutException e) {
				logger.error("Error timeout getting from memcache: " + token);
			} catch (InterruptedException e) {
				logger.error("Error memcache interrupted");
			} catch (MemcachedException e) {
				logger.error("Error memcache", e);
			} */
		}
		return appConfig.getClient().getToken(token);
	}

	public static void pause(long pauseTime) {
		try {
			Thread.currentThread().sleep(pauseTime);
		} catch (InterruptedException e) {
			logger.debug("Thread is interrupted while sleeping before "
					+ pauseTime + " seconds. ");
		}
	}

	// Is caching enabled?
	private static boolean isCaching() {
		return appConfig.getClient() != null;
	}

	/* private static void getRoles(Object obj, StringBuilder tenants,
			StringBuilder nonTenants) {
		if (appConfig.getAuthVersion().equalsIgnoreCase("v2.0")) {
			List<Role> roles = (List<Role>) obj;
			for (Role role : roles) {
				if (role.getTenantId() != null) {
					tenants.append(",");
					tenants.append(role.getName());
				} else {
					nonTenants.append(",");
					nonTenants.append(role.getName());
				}
			}
		} else {
			List<V3Role> roles = (List<V3Role>) obj;
			for (V3Role role : roles) {
				if (role.getProjectId() != null) {
					tenants.append(",");
					tenants.append(role.getName());
				} else {
					nonTenants.append(",");
					nonTenants.append(role.getName());
				}
			}
		}
	}  */
}
