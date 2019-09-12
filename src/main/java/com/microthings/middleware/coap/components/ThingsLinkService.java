package com.microthings.middleware.coap.components;

import org.eclipse.leshan.server.californium.impl.LeshanServer;

import com.thingslink.lwm2m.opera.LwM2mEvent;

public class ThingsLinkService {
	private LwM2mEvent lwM2mEvent = null;
	private LeshanServer lwServer;
	public ThingsLinkService(LeshanServer server) {
		this.lwServer = server;
		initAll();
	}
	public void initAll() {
		initRegEvent();
	}
	public void initRegEvent() {
		lwM2mEvent = new LwM2mEvent(lwServer);
	}
}
