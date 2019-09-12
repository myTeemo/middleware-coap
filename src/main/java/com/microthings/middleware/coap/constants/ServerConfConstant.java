package com.microthings.middleware.coap.constants;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ServerConfConstant {

	private static ResourceBundle configBundle = PropertyResourceBundle
			.getBundle("conf/application.properties");
	// jetty端口
	public final static int JETTY_PORT = Integer.valueOf(configBundle
			.getString("jetty.port"));
	// coap端口5683
	public final static int COAP_PORT = Integer.valueOf(configBundle
			.getString("coap.port"));
	public final static String[] MODEL_PATHS = configBundle.getString(
			"lwm2m.models").split(",| ");
	public final static long OBSERVE_TIMEOUT = Long.valueOf(configBundle
			.getString("observe.timeout"));
	public final static long COMMAND_TIMEOUT = Long.valueOf(configBundle.getString("command.timeout"));
	public final static String CODEC_URL = configBundle.getString("codec.url");
	public final static String COAP_TOPIC_PRE = configBundle.getString("command.topic.pre");
	// activemq配置
	public final static String MQ_BROKER_URL = configBundle
			.getString("activemq.receiver.brokerURL");
	public final static String MQ_USER_NAME = configBundle
			.getString("activemq.receiver.username");
	public final static String MQ_USER_PASSWORD = configBundle
			.getString("activemq.receiver.password");
	public final static String MQ_USE_ASYNC_SEND = configBundle
			.getString("activemq.receiver.useAsyncSend");
}
