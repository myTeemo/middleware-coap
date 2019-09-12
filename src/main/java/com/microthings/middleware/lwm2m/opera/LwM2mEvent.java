package com.microthings.middleware.lwm2m.opera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.JMSException;
import javax.jms.Queue;

import com.microthings.middleware.coap.activemq.MQCommandReceiver;
import com.microthings.middleware.coap.constants.ServerConfConstant;
import com.microthings.middleware.coap.utils.HttpRequest;
import com.microthings.middleware.lwm2m.custom.CommandRequest;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.elements.util.Base64;
import org.eclipse.californium.elements.util.DaemonThreadFactory;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.ObserveUtil;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 关于lwm2m设备一系列操作
 *
 * @author hxk
 */
public class LwM2mEvent {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    LeshanServer server = null;
    ExecutorService executor = Executors.newFixedThreadPool(5,
            new DaemonThreadFactory("lwm2mEvent"));
    // 设备注册之后发送observe
    private final RegistrationListener registrationListener = new RegistrationListener() {

        @Override
        public void updated(RegistrationUpdate update, Registration updatedReg,
                            Registration previousReg) {
            // TODO Auto-generated method stub

        }

        @Override
        public void unregistered(Registration reg,
                                 Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            // TODO Auto-generated method stub

        }

        @Override
        public void registered(final Registration reg,
                               Registration previousReg,
                               Collection<Observation> previousObsersations) {
            // TODO Auto-generated method stub
            logger.info(reg.getEndpoint()
                    + "==========向设备发送observe请求================");
            /*
             * 2018-11-27 Jae新增如下代码 当客户端注册成功后立即订阅/19/0/0资源
             */
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String target = "19/0/0";
                    ObserveRequest request = new ObserveRequest(
                            ContentFormat.OPAQUE, target);
                    ObserveResponse cResponse = null;
                    try {
                        cResponse = server.send(reg, request,
                                ServerConfConstant.OBSERVE_TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (null == cResponse) {
                        logger.error(reg.getEndpoint()
                                + "============lwm2m observe fail==============");
                    } else {
                        logger.info(reg.getEndpoint()
                                + "============lwm2m observe sucess==============");
                    }

                }
            });

        }
    };
    // observe事件通知
    private final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void onResponse(Observation observation,
                               final Registration registration, final ObserveResponse response) {
            // TODO Auto-generated method stub
            logger.info("================deal notify event.=================");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Response coapResponse = (Response) response.getCoapResponse();
                    // 更新节点连接信息
                    updateRegistInfo(registration, coapResponse);
                    // 上传数据
                    updataToCodec(registration.getEndpoint(), coapResponse.getPayload(), "");
                    // 重消息队列中获取该imei下的命令
                    sendCommand(registration.getEndpoint());
                }
            });
        }

        @Override
        public void onError(Observation observation, Registration registration,
                            Exception error) {
            // TODO Auto-generated method stub

        }

        @Override
        public void newObservation(Observation observation,
                                   Registration registration) {
            // TODO Auto-generated method stub

        }

        @Override
        public void cancelled(Observation observation) {
            // TODO Auto-generated method stub

        }
    };
    // notify通知事件（被订阅的节点上传数据）
    private final NotificationListener notificationListener = new NotificationListener() {

        @Override
        public void onNotification(Request request, Response response) {
            // TODO Auto-generated method stub
            logger.info(request.getUserContext().get(ObserveUtil.CTX_ENDPOINT)
                    + "=========notify通知事件");
        }

    };

    public LwM2mEvent(LeshanServer server) {
        this.server = server;
        server.getRegistrationService().addListener(registrationListener);
        server.getObservationService().addListener(observationListener);
        /******* 因为observe事件下发成功，但服务器接收到响应经常失败，此处添加一个response拦截器 *******/
        // ResponseDataInterceptor responseDataInterceptor = new
        // ResponseDataInterceptor(
        // this.lwServer);
		/*for (Endpoint endpoint : server.coap().getServer().getEndpoints()) {
			endpoint.addInterceptor(responseDataInterceptor);
			endpoint.addNotificationListener(notificationListener);
		}*/
    }

    /**
     * 更新设备注册信息（ip:port，刷新注册信息）
     *
     * @param registration
     * @param response
     */
    public void updateRegistInfo(Registration registration, Response response) {
        // Update client with the new address/port
        Identity obsIdentity = EndpointContextUtil.extractIdentity(response
                .getSourceContext());
        logger.debug("==========验证设备的连接是否改变===========");
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
     * 把数据发送到编解码服务器
     *
     * @param sceneTitle
     * @param byteDatas
     * @param devKey
     */
    public void updataToCodec(String sceneTitle, byte[] byteDatas, String devKey) {
        if (byteDatas.length <= 0) {
            return;
        }
        // 上传url
        String url = ServerConfConstant.CODEC_URL + sceneTitle;
        try {
            logger.info("============send to codec============");
            HttpRequest.postRequest(url, byteDatas, devKey,
                    "application/octet-stream");
        } catch (Exception e) {
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
        Registration registration = server.getRegistrationService().getByEndpoint(endpoint);
        Queue queue = null;
        // 判断设备是tup还是lwm2m
        String str = registration.getObjectLinks()[6].getUrl();
        String target = "19/1/0";
        if (str.startsWith("/17031211")) {
            target = "17031211/1/0";
        }
        try {
            queue = MQCommandReceiver.getSession().createQueue(topic);
        } catch (JMSException e) {
            e.printStackTrace();
            return;
        }
        ArrayList<String> messageList = MQCommandReceiver.getCommandReceiver()
                .getMsgListFromMq(queue, topic);
        if (null == messageList || messageList.size() == 0) {
            return;
        }
        for (String message : messageList) {
            byte[] commands = null;
            try {
                commands = Base64.decode(message);
            } catch (IOException e1) {
                e1.printStackTrace();
                continue;
            }
            CommandRequest request = new CommandRequest(target, commands);
            // WriteRequest发出的请求是put类型（需要post类型），不能用
            // WriteRequest request = new WriteRequest(ContentFormat.OPAQUE,
            // 17031211, 1, 0, commands);
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

}
