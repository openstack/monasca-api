package com.hp.csbu.cc.middleware;

import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A token-based authentication filter. This filter uses Thrift protocol to
 * communicate with the CS server. The token to validate is set via the header
 * {@link #TOKEN}.
 * <p>
 * A token is required to validate. However, if no token is presented, the
 * filter will set the {@link #AUTH_IDENTITY_STATUS} request parameter to
 * <code>Invalid</code> and let any other filter downstream to decide what to
 * do. For instance, if a downstream filter knows how to deal with signature
 * rather than tokens, then it will go ahead and validate with signatures.
 * <p>
 * Upon successful validation, all the Auth request parameters will be
 * populated, including information such as tenant, user and user roles, and
 * passed down to the next filter downstream.
 * <p>
 * Upon unsuccessful validation, this filter will terminate the request by
 * returning a 401 (unauthorized).
 * 
 * @author liemmn
 * 
 */
public class TokenAuth implements Filter, AuthConstants {

	private static final String TOKEN_NOTFOUND = "Bad Request: Token not found in the request";
	private static final String SERVICE_IDS_PARAM = "serviceIds";
	private static final String ENDPOINT_IDS_PARAM = "endpointIds";
	private static final String SERVICE_CATALOG_PARAM = "includeCatalog";
	private static final String API_VERSION_PARAM = "apiVersion";

  private final Config appConfig = Config.getInstance();
	
	private FilterConfig filterConfig;

	// Thee faithful logger
	private static final Logger logger = LoggerFactory
			.getLogger(TokenAuth.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;		
	}


	/**
	 * {@inheritDoc}
	 */
	public void destroy() {
		FilterUtils.destroyFilter();
	}

	/**
	 * {@inheritDoc}
	 */
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		Object auth = null;
		int numberOfTries = 0;		
		if (!appConfig.isInitialized()) {
			appConfig.initialize(filterConfig,req,getInputParams());
		}		
		int retries = appConfig.getRetries();
		long pauseTime = appConfig.getPauseTime();
    AuthClientFactory factory = appConfig.getFactory();

		// Extract credential
    String token = ((HttpServletRequest) req).getHeader(TOKEN);

		if (token == null) {
			if (!appConfig.isDelayAuthDecision()) {
				logger.error(HttpServletResponse.SC_UNAUTHORIZED
						+ " No token found.");
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED, TOKEN_NOTFOUND);
				return;
			} else {
				logger.info("No token found...Skipping");
			}
		} else {
      // Retrieve from cache
      //AuthClient client = null;
      //try {
      auth = FilterUtils.getCachedToken(token);
      //if (auth == null) {

      // Validate credential

      //  do {

      //auth = FilterUtils.getCachedToken(token);
      //client = factory.getClient();
      //factory.recycle(client);
            /*if (appConfig.getAuthVersion().equalsIgnoreCase("v2.0")) {
                    auth = client.validateTokenForServiceEndpointV2((token, appConfig.getServiceIds(),
              appConfig.getEndpointIds(), appConfig.isIncludeCatalog());

						} else {
							//auth = client.validateTokenForServiceEndpointV3(token, getInputParams());
              auth =  new TokenCache<String,String>(appConfig.getTimeToCacheToken(),getInputParams());
						} */
      // Cache token
      //FilterUtils.cacheToken(token, auth);
      // Return to connection pool for re-use
           /*if(auth==null)
             throw new TTransportException();
            factory.recycle(client);
            */
      //			logger.debug("Successful Authentication");
      //			break;
					/*} catch (TTransportException t) {
						if (client != null)
							factory.discard(client);
						if (numberOfTries < retries) {
							FilterUtils.pause(pauseTime);
							logger.debug("Retrying connection after "
									+ pauseTime + " seconds.");
							numberOfTries++;
							continue;

						} else {
							TokenExceptionHandler handler = TokenExceptionHandler
									.valueOf("TException");
							handler.onException(t, resp, token);
						}
						return;
					} */ /*}catch (ClientProtocolException c) {
						if (client != null){

							factory.discard(client);
						/*if (numberOfTries < retries) {
							FilterUtils.pause(pauseTime);
							logger.debug("Retrying connection after "
									+ pauseTime + " seconds.");
							numberOfTries++;
							continue;
              */
              //return;
						/*} else {
							TokenExceptionHandler handler = TokenExceptionHandler
									.valueOf("ClientProtocolException");
							handler.onException(c, resp, token);
						} */
						//return;
					//}

     /* }catch (Exception ex) {
						if (client != null)
							factory.recycle(client);
						TokenExceptionHandler handler = ExceptionHandlerUtil
								.lookUpTokenException(ex);
						handler.onException(ex, resp, token);
						return;
					}*/
      //} while (numberOfTries <= retries);
			/*} else {
				// Got a cached token!
				logger.debug("Got cached token: " + token);
			}
		}*/
    }
		req = FilterUtils.wrapRequest(req, auth);
		logger.debug("TokenAuth: Forwarding down stream to next filter/servlet");
		// Forward downstream...
		chain.doFilter(req, resp);
	}
	
	private Map<String, String> getInputParams() {
		Map<String, String> inputParams = new HashMap<String, String>();
		if (appConfig.getServiceIds() != null) {
			inputParams.put(SERVICE_IDS_PARAM, appConfig.getServiceIds());
		}
		if (appConfig.getEndpointIds() != null) {
			inputParams.put(ENDPOINT_IDS_PARAM, appConfig.getEndpointIds());
		}
		inputParams.put(SERVICE_CATALOG_PARAM, String.valueOf(appConfig.isIncludeCatalog()));
		inputParams.put(API_VERSION_PARAM, appConfig.getAuthVersion());
		return inputParams;
	}


}
