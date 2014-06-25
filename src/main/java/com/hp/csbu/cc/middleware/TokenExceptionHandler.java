package com.hp.csbu.cc.middleware;

import java.io.IOException;
import org.apache.http.client.ClientProtocolException;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.thrift.TException;

public enum TokenExceptionHandler {

	AuthConnectionException {
		@Override
		public void onException(Exception e, ServletResponse resp, String token) {
			AuthConnectionException ae = (AuthConnectionException) e;
			logger.error(ae.getMessage() + " " + ae);
			try {
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED,
						ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED)
								+ " " + token);
			} catch (IOException ie) {
				logger.debug("Error in writing the HTTP response "
						+ ie.getMessage() + " " + ie);
			}
		}
	},
	TException {
		@Override
		public void onException(Exception e, ServletResponse resp, String token) {
			TException t = (TException) e;
			logger.error("Thrift Exception " + t.getMessage() + " " + t);
			try {
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED,
						ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED)
								+ " " + token);
			} catch (IOException ie) {
				logger.debug("Error in writing the HTTP response "
						+ ie.getMessage() + " " + ie);
			}
		}	
	},
  ClientProtocolException {
		@Override
		public void onException(Exception e, ServletResponse resp, String token) {
			ClientProtocolException t = (ClientProtocolException) e;
			logger.error("Http Client Exception " + t.getMessage() + " " + t);
			try {
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED,
						ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED)
								+ " " + token);
			} catch (IOException ie) {
				logger.debug("Error in writing the HTTP response "
						+ ie.getMessage() + " " + ie);
			}
		}
	},
  AuthException {
    @Override
    public void onException(Exception e, ServletResponse resp, String token) {
      AuthException ae = (AuthException) e;
      logger.error(ae.getMessage() + " " + ae);
      String statusText = ae.getMessage();
      if (statusText == null || statusText.isEmpty()) {
        statusText = ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED);
      }
      try {
        ((HttpServletResponse) resp).sendError(
          HttpServletResponse.SC_UNAUTHORIZED,
          statusText + " " + token);
      } catch (IOException ie) {
        logger.debug("Error in writing the HTTP response "
          + ie.getMessage() + " " + ie);
      }
    }
  }, ServiceUnavailableException {
    @Override
    public void onException(Exception e, ServletResponse resp, String token) {
      AuthException ae = (AuthException) e;
      logger.error(ae.getMessage() + " " + ae);
      String statusText = ae.getMessage();
      if (statusText == null || statusText.isEmpty()) {
        statusText = ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED);
      }
      try {
        ((HttpServletResponse) resp).sendError(
          HttpServletResponse.SC_UNAUTHORIZED,
          statusText + " " + token);
      } catch (IOException ie) {
        logger.debug("Error in writing the HTTP response "
          + ie.getMessage() + " " + ie);
      }
    }
  };
	
	final Logger logger = LoggerFactory.getLogger(TokenExceptionHandler.class);
	abstract void onException(Exception e, ServletResponse resp, String token);
}
