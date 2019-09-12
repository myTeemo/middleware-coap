package com.microthings.middleware.coap.start;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microthings.middleware.coap.components.JettyServerWeb;
import com.microthings.middleware.coap.components.LwM2mService;
import com.microthings.middleware.coap.components.ThingsLinkService;
import com.microthings.middleware.coap.components.TupCoapService;

public class ServiceStartMain {
	static {
		// Define a default logback.configurationFile
		String property = System.getProperty("logback.configurationFile");
		if (property == null) {
			System.setProperty("logback.configurationFile",
					"conf/logback-config.xml");
		}
	}
	private static final Logger logger = LoggerFactory
			.getLogger(ServiceStartMain.class);

	public static void main(String[] args) throws Exception {
		ServiceStartMain ssm = new ServiceStartMain();
		ssm.startAll();
	}

	public void startAll() throws Exception {
		logger.info("==========start the server.==============");
		// 定义lwM2m服务
		LwM2mService lwM2MService = new LwM2mService();
		// 定义TUP服务
		TupCoapService tupCoapService = new TupCoapService(lwM2MService);
		// 定义jetty
		JettyServerWeb jettyServerWeb = new JettyServerWeb(
				lwM2MService.getLwServer());
		ThingsLinkService thingsLinkService = new ThingsLinkService(
				lwM2MService.getLwServer());
	}
}
