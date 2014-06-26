package com.hpcloud.middleware;


import static com.hpcloud.middleware.AuthConstants.AUTH_IDENTITY_STATUS;

import static com.hpcloud.middleware.AuthConstants.AUTH_ROLES;
import static com.hpcloud.middleware.AuthConstants.AUTH_TENANT_NAME;

import static com.hpcloud.middleware.AuthConstants.AUTH_USER_ID;
import static com.hpcloud.middleware.AuthConstants.AUTH_DOMAIN_ID;
import static com.hpcloud.middleware.AuthConstants.AUTH_DOMAIN_NAME;
import static com.hpcloud.middleware.AuthConstants.AUTH_PROJECT_ID;
import static com.hpcloud.middleware.AuthConstants.AUTH_PROJECT_NAME;
import static com.hpcloud.middleware.AuthConstants.AUTH_TENANT_ID;
import static com.hpcloud.middleware.AuthConstants.AUTH_USER_NAME;
import static com.hpcloud.middleware.AuthConstants.IdentityStatus;
import static com.hpcloud.middleware.AuthConstants.AUTH_PROJECT_DOMAIN_ID;
import static com.hpcloud.middleware.AuthConstants.AUTH_PROJECT_DOMAIN_NAME;
import static com.hpcloud.middleware.AuthConstants.AUTH_USER_DOMAIN_ID;
import static com.hpcloud.middleware.AuthConstants.AUTH_USER_DOMAIN_NAME;
import static com.hpcloud.middleware.AuthConstants.AUTH_HP_IDM_ROLES;
import static com.hpcloud.middleware.AuthConstants.AUTH_SERVICE_CATALOG;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import javax.servlet.ServletRequest;

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

    TokenCache<String,String> client = appConfig.getClient();

    if(client !=null)
      appConfig.setClient(null);

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
			StringBuilder roleBuilder = new StringBuilder();
      while (it.hasNext()) {

        //Changed to meet my purposes
        JsonObject role = it.next().getAsJsonObject();
        String currentRole = role.get("name").getAsString();
        roleBuilder.append(currentRole).append(",");
			}
      //My changes to meet my needs
      req.setAttribute(AUTH_ROLES, roleBuilder.toString());
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
		if (data instanceof String) {
			wrapRequestFromHttpResponse(req, ((String) data));
		}
		return req;
	}

	// Insert token into cache
	public static void cacheToken(String token, Object auth) {
    appConfig.getClient().put(token, (String) auth);
  }

	// Get token from cache
	public static Object getCachedToken(String token) throws IOException {
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
}
