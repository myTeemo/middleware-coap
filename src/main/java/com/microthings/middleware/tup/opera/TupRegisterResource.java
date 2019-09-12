package com.microthings.middleware.tup.opera;

import static org.eclipse.leshan.core.californium.EndpointContextUtil.extractIdentity;
import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * tup协议设备的注册（/t/d）
 * @author Administrator
 *
 */
public class TupRegisterResource extends CoapResource {

	private static final String QUERY_PARAM_ENDPOINT = "ep=";
	public static final String RESOURCE_NAME = "r";
	public static final String DESTINATION_NAME = "t/d";
	private final RegistrationHandler registrationHandler;
	private static final Logger LOG = LoggerFactory
			.getLogger(TupRegisterResource.class);

	public TupRegisterResource(RegistrationHandler registrationHandler) {
		super(RESOURCE_NAME);
		this.registrationHandler = registrationHandler;
		getAttributes().addResourceType("TUP device register");
	}

	@Override
	public void handleRequest(Exchange exchange) {
		try {
			super.handleRequest(exchange);
		} catch (InvalidRequestException e) {
			LOG.debug(
					"InvalidRequestException while handling request({}) on the /t/r resource",
					exchange.getRequest(), e);
			Response response = new Response(ResponseCode.BAD_REQUEST);
			response.setPayload(e.getMessage());
			exchange.sendResponse(response);
		} catch (RuntimeException e) {
			LOG.error(
					"Exception while handling request({}) on the /t/r resource",
					exchange.getRequest(), e);
			exchange.sendResponse(new Response(
					ResponseCode.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void handlePOST(CoapExchange exchange) {
		Request request = exchange.advanced().getRequest();
		LOG.debug("POST received : {}", request);
		// 请求类型为con
		if (!Type.CON.equals(request.getType())) {
			exchange.respond(ResponseCode.BAD_REQUEST);
			return;
		}
		List<String> uri = exchange.getRequestOptions().getUriPath();
		if (uri == null || uri.size() == 0 || !RESOURCE_NAME.equals(uri.get(1))) {
			exchange.respond(ResponseCode.BAD_REQUEST);
			return;
		}
		// 进行tup协议设备注册处理
		handleRegister(exchange, request);
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		LOG.debug("DELETE received : {}", exchange.advanced().getRequest());
		List<String> uri = exchange.getRequestOptions().getUriPath();
		// 进行处理

	}

	/**
	 * 进行注册
	 * 
	 * @param exchange
	 * @param request
	 */
	private void handleRegister(CoapExchange exchange, Request request) {
		// Get identity
		Identity sender = extractIdentity(request.getSourceContext());
		String endpoint = null;
		Long lifetime = 86400L;
		String smsNumber = null;
		String lwVersion = "1.0";
		BindingMode binding = BindingMode.valueOf("U");
		Link[] objectLinks = Link.parse(new byte[] { 60, 47, 62, 59, 114, 116,
				61, 34, 111, 109, 97, 46, 108, 119, 109, 50, 109, 34, 44, 32,
				60, 47, 49, 47, 48, 62, 44, 32, 60, 47, 51, 47, 48, 62, 44, 32,
				60, 47, 52, 47, 48, 62, 44, 32, 60, 47, 53, 47, 48, 62, 44, 32,
				60, 47, 50, 48, 47, 48, 62, 44, 32, 60, 47, 49, 55, 48, 51, 49,
				50, 49, 49, 47, 48, 62 });
		Map<String, String> additionalParams = new HashMap<>();
		// Get parameters
		for (String param : request.getOptions().getUriQuery()) {
			if (param.startsWith(QUERY_PARAM_ENDPOINT)) {
				endpoint = param.substring(3);
			} else {
				String[] tokens = param.split("\\=");
				if (tokens != null && tokens.length == 2) {
					additionalParams.put(tokens[0], tokens[1]);
				}
			}
		}
		LOG.debug("============Device's endpoint:" + endpoint);
		// Create request
		RegisterRequest registerRequest = new RegisterRequest(endpoint,
				lifetime, lwVersion, binding, smsNumber, objectLinks,
				additionalParams);

		// Handle request
		// -------------------------------
		InetSocketAddress serverEndpoint = exchange.advanced().getEndpoint()
				.getAddress();
		final SendableResponse<RegisterResponse> sendableResponse = registrationHandler
				.register(sender, registerRequest, serverEndpoint);
		RegisterResponse response = sendableResponse.getResponse();

		// 对于已经created的设备修改为发送changed
		if (response.getCode() == org.eclipse.leshan.ResponseCode.CREATED) {
			exchange.respond(ResponseCode.CREATED);
		} else {
			exchange.respond(toCoapResponseCode(response.getCode()),
					response.getErrorMessage());
		}
		sendableResponse.sent();
	}

	@Override
	public Resource getChild(String name) {
		return this;
	}

}