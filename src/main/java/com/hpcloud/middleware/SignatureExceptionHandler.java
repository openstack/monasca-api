package com.hpcloud.middleware;

import java.io.IOException;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

//import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.hp.csbu.cc.security.cs.thrift.service.ResourceException;

public enum SignatureExceptionHandler {
	
	AuthConnectionException {
		@Override
		public void onException(Exception e, ServletResponse resp) {
			AuthConnectionException ae = (AuthConnectionException) e;
			logger.error(ae.getMessage() + " " + ae);
			try {
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED,
						ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED));
			} catch (IOException ie) {
				logger.debug("Error in writing the HTTP response "
						+ ie.getMessage() + " " + ie);
			}
		}
	},
	TException {
		@Override
		public void onException(Exception e, ServletResponse resp) {
		//	TException t = (TException) e;
			//logger.error("Thrift Exception " + t.getMessage() + " " + t);
			try {
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED,
						ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED));
			} catch (IOException ie) {
				logger.debug("Error in writing the HTTP response "
						+ ie.getMessage() + " " + ie);
			}
		}	
	},
	SignatureBuilderException {
		@Override
		public void onException(Exception e, ServletResponse resp) {
			SignatureBuilderException sbe = (SignatureBuilderException) e;
			logger.error(sbe.getMessage() + " " + sbe);
			try {
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED,
						ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED));
			} catch (IOException ie) {
				logger.debug("Error in writing the HTTP response "
						+ ie.getMessage() + " " + ie);
			}
		}
	},
	AuthException {
		@Override
		public void onException(Exception e, ServletResponse resp) {
			AuthException ae = (AuthException) e;
			logger.error(ae.getMessage() + " " + ae);
			try {
				((HttpServletResponse) resp).sendError(
						HttpServletResponse.SC_UNAUTHORIZED,
						ExceptionHandlerUtil.getStatusText(HttpServletResponse.SC_UNAUTHORIZED));
			} catch (IOException ie) {
				logger.debug("Error in writing the HTTP response "
						+ ie.getMessage() + " " + ie);
			}
		}
	};

	final Logger logger = LoggerFactory.getLogger(SignatureExceptionHandler.class);
	abstract void onException(Exception e, ServletResponse resp);

}
