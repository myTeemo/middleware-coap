package com.microthings.middleware.coap.jetty.servlet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.californium.elements.util.DaemonThreadFactory;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.CommandRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.CommandResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thingslink.coap.constants.ServerConfConstant;

public class CommandToDeviceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final LwM2mServer lwServer;
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private final String LWM2M_TARGET = "19/1/0";
	private final String TUP_TARGET = "17031211/1/0";
	private final String DEVIE_TYPE = "type";
	private final String ENDPOINT_STR = "endpoint";
	private final String COMMAND_STR = "command";
	private final Gson gson;
	ExecutorService exector = Executors.newFixedThreadPool(3,
			new DaemonThreadFactory("processData"));

	public CommandToDeviceServlet(LwM2mServer server) {
		this.lwServer = server;
		GsonBuilder gsonBuilder = new GsonBuilder();
		this.gson = gsonBuilder.create();

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		logger.debug("==========command to device doGet method========");
	}

	/**
	 * req携带endponint,命令base64编码字符串command
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String endpoint = req.getParameter(ENDPOINT_STR);
		String commandStr = req.getParameter(COMMAND_STR).replace(" ", "+");
		logger.info("=======receive command request from:" + endpoint
				+ "commandStr:" + commandStr);
		final byte[] commands = Base64.decodeBase64(commandStr);
		final Registration registration = this.lwServer.getRegistrationService()
				.getByEndpoint(endpoint);
		if (registration != null) {
			exector.execute(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					commandToDevice(registration,commands);
				}
			});
			resp.setContentType("application/json");
			resp.setStatus(HttpServletResponse.SC_OK);
//			processDeviceResponse(req, resp, cResponse);
		} else {
			resp.setContentType("application/json");
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private void commandToDevice(Registration registration,byte[] commands) {
		String target = LWM2M_TARGET;
		String str = registration.getObjectLinks()[6].getUrl();
		if (str.startsWith("/17031211")) {
			target = TUP_TARGET;
		}
		CommandRequest request = new CommandRequest(target, commands);
		CommandResponse cResponse = null;
		logger.info("=============start send command============");
		try {
			cResponse = this.lwServer.send(registration, request,
					ServerConfConstant.COMMAND_TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
//			handleException(e, resp);
		}
	}
	private void handleException(Exception e, HttpServletResponse resp)
			throws IOException {
		if (e instanceof InvalidRequestException || e instanceof CodecException
				|| e instanceof ClientSleepingException) {
			logger.warn("Invalid request", e);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.getWriter().append("Invalid request:").append(e.getMessage())
					.flush();
		} else if (e instanceof RequestRejectedException) {
			logger.warn("Request rejected", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().append("Request rejected:").append(e.getMessage())
					.flush();
		} else if (e instanceof RequestCanceledException) {
			logger.warn("Request cancelled", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().append("Request cancelled:")
					.append(e.getMessage()).flush();
		} else if (e instanceof InvalidResponseException) {
			logger.warn("Invalid response", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().append("Invalid Response:").append(e.getMessage())
					.flush();
		} else if (e instanceof InterruptedException) {
			logger.warn("Thread Interrupted", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().append("Thread Interrupted:")
					.append(e.getMessage()).flush();
		} else {
			logger.warn("Unexpected exception", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().append("Unexpected exception:")
					.append(e.getMessage()).flush();
		}
	}

	private void processDeviceResponse(HttpServletRequest req,
			HttpServletResponse resp, LwM2mResponse cResponse)
			throws IOException {
		if (cResponse == null) {
			logger.warn(String.format("Request %s%s timed out.",
					req.getServletPath(), req.getPathInfo()));
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().append("Request timeout").flush();
		} else {
			String response = this.gson.toJson(cResponse);
			resp.setContentType("application/json");
			resp.getOutputStream().write(response.getBytes());
			resp.setStatus(HttpServletResponse.SC_OK);
		}
	}
}
