package com.microthings.middleware.coap.components;

import java.net.InetSocketAddress;

import lombok.Getter;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingslink.coap.constants.ServerConfConstant;
import com.thingslink.coap.jetty.servlet.ClientServlet;
import com.thingslink.coap.jetty.servlet.CommandToDeviceServlet;
import com.thingslink.coap.jetty.servlet.EventServlet;
import com.thingslink.coap.jetty.servlet.ObjectSpecServlet;

/**
 * 前端展示
 * @author hxk
 *
 */
public class JettyServerWeb {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	//处理节点的server
	LeshanServer lwServer = null;
	@Getter
	Server jettyServer = null;
	WebAppContext root = null;
	public JettyServerWeb(LeshanServer lwServer) {
		this.lwServer = lwServer;
		initAll();
	}
	public void initAll() {
		initJetty();
		initEventServlet();
		initClientServlet();
		initObjServlet();
		initCommandServlet();
		try {
			this.jettyServer.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void initJetty() {
		InetSocketAddress jettyAddr = new InetSocketAddress(ServerConfConstant.JETTY_PORT);
		jettyServer = new Server(jettyAddr);
		root = new WebAppContext();
		root.setContextPath("/");
		root.setResourceBase(this.getClass().getClassLoader()
				.getResource("webapp").toExternalForm());
		root.setParentLoaderPriority(true);
		jettyServer.setHandler(root);
	}
	public void initEventServlet() {
		EventServlet eventServlet = new EventServlet(lwServer);
		ServletHolder eventServletHolder = new ServletHolder(eventServlet);
		root.addServlet(eventServletHolder, "/event/*");
	}
	public void initClientServlet() {
		ClientServlet clientServlet = new ClientServlet(lwServer);
		ServletHolder clientServletHolder = new ServletHolder(clientServlet);
		root.addServlet(clientServletHolder, "/api/clients/*");
	}
	public void initObjServlet() {
		ObjectSpecServlet objectSpecServlet = new ObjectSpecServlet(lwServer.getModelProvider());
		ServletHolder objectSpecServletHolder = new ServletHolder(objectSpecServlet);
		root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");
	}
	/**
	 * 给设备发送命令
	 */
	public void initCommandServlet() {
		CommandToDeviceServlet commandToDeviceServlet = new CommandToDeviceServlet(lwServer);
		ServletHolder commandServletHolder = new ServletHolder(commandToDeviceServlet);
		root.addServlet(commandServletHolder, "/api/command/*");
	}
 }
