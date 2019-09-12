package com.microthings.middleware.coap.components;

import java.util.Collection;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.registration.RandomStringRegistrationIdProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingslink.coap.constants.ServerConfConstant;
import com.thingslink.tup.opera.TupRegisterResource;

public class TupCoapService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	LwM2mService lwService = null;
	RegistrationListener registrationListener = null;
	public TupCoapService(LwM2mService lwService) {
		this.lwService = lwService;
		initAll();
	}
	public void initAll() {
		initRegisterLister(this.lwService.getLwServer());
		initServer();
	}
	public void initServer() {
		RegistrationServiceImpl registrationService = new RegistrationServiceImpl(this.lwService.getRegistrationStore());
		registrationService.addListener(registrationListener);
		Authorizer authorizer = new DefaultAuthorizer(this.lwService.getSecurityStore());
		RandomStringRegistrationIdProvider registrationIdProvider = new RandomStringRegistrationIdProvider();
		RegistrationHandler registrationHandler = new RegistrationHandler(
				registrationService, authorizer, registrationIdProvider);
		CoapResource coapResource_r = new TupRegisterResource(
				registrationHandler);
		CoapResource coapResource_t = new CoapResource("t");
		coapResource_t.setVisible(false);
		coapResource_t.add(coapResource_r);
		lwService.getLwServer().coap().getServer().add(coapResource_t);
	}
	public void initRegisterLister(final LeshanServer lwServer) {
		registrationListener = new RegistrationListener() {
			@Override
			public void registered(Registration reg,
					Registration previousReg,
					Collection<Observation> previousObsersations) {
				String target = "17031211/0/0";
				// create & process request
				ObserveRequest request = new ObserveRequest(ContentFormat.OPAQUE,target);
				ObserveResponse cResponse = null;
				try {
					// 定义一个匿名内部类，并且希望它使用一个在其外部定义的对象，那么编译器会要求其参数引用是final的
					cResponse = lwServer.send(reg, request, ServerConfConstant.OBSERVE_TIMEOUT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				  if(null==cResponse) {
		            	logger.error(reg.getEndpoint()+"============tup observe fail==============");
		            } else{
		            	logger.info(reg.getEndpoint()+"============tup observe sucess==============");
		            }
			}

			@Override
			public void updated(RegistrationUpdate update,
					Registration updatedRegistration,
					Registration previousRegistration) {
			}

			@Override
			public void unregistered(Registration registration,
					Collection<Observation> observations, boolean expired,
					Registration newReg) {
			}

		};
	}
}
