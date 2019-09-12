package com.microthings.middleware.coap.operal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.JMSException;
import javax.jms.Queue;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.elements.util.DaemonThreadFactory;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.CommandRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.server.californium.ObserveUtil;
import org.eclipse.leshan.server.californium.impl.InMemoryRegistrationStore;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingslink.coap.activemq.MQCommandReceiver;
import com.thingslink.coap.constants.ServerConfConstant;
import com.thingslink.coap.utils.HttpRequest;

public class ResponseDataInterceptor implements MessageInterceptor {

	private final LeshanServer server;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	ExecutorService exector = Executors.newFixedThreadPool(5,
			new DaemonThreadFactory("processData"));

	public ResponseDataInterceptor(LeshanServer server) {
		this.server = server;
	}

	@Override
	public void sendRequest(Request request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendResponse(Response response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendEmptyMessage(EmptyMessage message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveRequest(Request request) {
		// TODO Auto-generated method stub
	}

	@Override
	public void receiveResponse(final Response response) {
		// TODO Auto-generated method stub
		LeshanServer leshanServer = (LeshanServer) (this.server);
		Observation obser = ((InMemoryRegistrationStore) leshanServer
				.getRegistrationStore()).get(response.getToken());
		logger.info("================receivce datas.==================");
		if (obser != null) {
			// use the endpoint get sceneId and devKey
			final String endpoint = obser.getRequest().getUserContext()
					.get(ObserveUtil.CTX_ENDPOINT);
			if (null == endpoint || endpoint.length() == 0) {
				return;
			}
			logger.info("=============device's endpoint name:" + endpoint);
			exector.execute(new Runnable() {
				@Override
				public void run() {
					// 更新节点连接信息
					updateRegistInfo(endpoint, response);
					// 上传数据
					updataToCodec(endpoint, response.getPayload(), "");
					// 重消息队列中获取该imei下的命令
					sendCommand(endpoint);
				}
			});
		} else {
			//没有该节点信息，发送RST包
			if(response.getType()!=CoAP.Type.NON) {
				return ;
			}
			EmptyMessage rst = EmptyMessage.newRST(response);
			Endpoint endpoint = this.server.coap().getServer().getEndpoints().get(0);
			CoapEndpoint coapEndpoint = (CoapEndpoint) endpoint;
			coapEndpoint.getCoapStack().sendEmptyMessage(null, rst);
			logger.info("============endpoint's observation is NULL.===============");
		}
	}

	@Override
	public void receiveEmptyMessage(EmptyMessage message) {
		// TODO Auto-generated method stub
	}

	/**
	 * 对设备信息进行更新(ip:port和注册信息)(不用修改源码)
	 * 
	 * @param endpointName
	 * @param coapResponse
	 */
	public void updateRegistInfo(String endpointName, Response coapResponse) {
		Registration registration = server.getRegistrationService()
				.getByEndpoint(endpointName);
		Identity obsIdentity = EndpointContextUtil.extractIdentity(coapResponse
				.getSourceContext());
		logger.debug("================验证设备的连接是否改变.=================");
		if (!registration.getIdentity().equals(obsIdentity)) {
			logger.error(
					"Observation with invalid identity. Expected {} but was {}",
					registration.getIdentity(), obsIdentity);
			// Create a fake registration update
			RegistrationUpdate regUpdate = new RegistrationUpdate(
					registration.getId(), obsIdentity, null, null, null, null,
					null);
			// Get the store (maybe you can access to it in a proper way (e.g.
			// if you implement your own store)
			RegistrationStore store = ((RegistrationServiceImpl) server
					.getRegistrationService()).getStore();
			// Update registration
			store.updateRegistration(regUpdate);
		}
	}

	/**
	 * verify the ip:port update
	 */
	@Deprecated
	public void updateRegistInfo2(String endpointName, Response response) {
		logger.info("===========check the connect info==============");
		Registration registration = server.getRegistrationService()
				.getByEndpoint(endpointName);
		// 注册时的连接信息
		Identity identity = registration.getIdentity();
		InetSocketAddress isa = identity.getPeerAddress();
		if (isa.getAddress() != response.getSourceContext().getPeerAddress()
				.getAddress()
				|| isa.getPort() != response.getSourceContext()
						.getPeerAddress().getPort()) {
			logger.info("================update the connect info=================");
			// 当前连接的ip和端口port
			InetSocketAddress tem = new InetSocketAddress(response
					.getSourceContext().getPeerAddress().getAddress(), response
					.getSourceContext().getPeerAddress().getPort());
			registration.getIdentity().setInetSocketAddress(tem);
		}
	}

	/**
	 * 把数据发送到编解码服务器
	 * 
	 * @param sceneTitle
	 * @param byteDatas
	 * @param devKey
	 */
	public void updataToCodec(String sceneTitle, byte[] byteDatas, String devKey) {
		if (byteDatas == null || byteDatas.length <= 0) {
			return;
		}
		// 上传url
		String url = ServerConfConstant.CODEC_URL + sceneTitle;
		try {
			logger.info("============send to codec============");
			HttpRequest.postRequest(url, byteDatas, devKey,
					"application/octet-stream");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 从消息队列中获取该设备的消息
	 * 
	 * @param endpoint
	 */
	public void sendCommand(String endpoint) {
		String topic = ServerConfConstant.COAP_TOPIC_PRE + endpoint;
		Queue queue = null;
		Registration registration = server.getRegistrationService()
				.getByEndpoint(endpoint);
		//判断设备是tup还是lwm2m
		String str = registration.getObjectLinks()[6].getUrl();
		String target = "19/1/0";
		if(str.startsWith("/17031211")) {
			target = "17031211/1/0";
		}
		try {
			queue = MQCommandReceiver.getSession().createQueue(topic);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		ArrayList<String> messageList = MQCommandReceiver.getCommandReceiver()
				.getMsgListFromMq(queue, topic);
		if (null == messageList || messageList.size() == 0) {
			return;
		}
		registration.getRootPath();
		for (String message : messageList) {
			byte[] commands = null;
			commands = Base64.decodeBase64(message);
			CommandRequest request = new CommandRequest(target,
					commands);
//			WriteRequest request = new WriteRequest(ContentFormat.OPAQUE, 17031211, 1, 0, commands);
			logger.info("=============start send cached command============");
			try {
				server.send(registration, request, 4000);
			} catch (CodecException | InvalidResponseException
					| RequestCanceledException | RequestRejectedException
					| ClientSleepingException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		// String url =
		// "http://222.25.188.1:50197/rosetta/dataproc/bindatas/869405030279260";
		String url = "http://127.0.0.1:8080/rosetta/dataproc/bindatas/869405030279260";
		System.out.println(url);
		byte[] byteDeviceReq = new byte[] { 0x40, 0x02, (byte) 0xaa, 0x31,
				(byte) 0x81, 0x68, 0x2b, 0x37, 0x11, 0x01, 0x00, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, 0x34, 0x36, 0x30, 0x30,
				0x34, 0x30, 0x39, 0x37, 0x37, 0x35, 0x30, 0x34, 0x30, 0x31,
				0x30, 0x76, 0x79, 0x35, 0x17, 0x00, 0x4b, 0x12, 0x00, 0x38,
				0x36, 0x39, 0x34, 0x30, 0x35, 0x30, 0x33, 0x30, 0x32, 0x37,
				0x39, 0x32, 0x36, 0x30, (byte) 0x93 };
		HttpRequest.postRequest(url, byteDeviceReq, "",
				"application/octet-stream");
	}
}
