package com.microthings.middleware.coap.activemq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microthings.middleware.coap.constants.ServerConfConstant;

/**
 * 从消息队列获取命令，并进行下发
 *
 * @author hxk
 */
public class MQCommandReceiver {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ConnectionFactory connectionFactory = null;
    private Connection connection = null;
    private static Session session = null;
    public static MQCommandReceiver commandReceiver = new MQCommandReceiver();
    Map<String, String> map = new ConcurrentHashMap<String, String>();

    private MQCommandReceiver() {
        initConnection();
    }

    /**
     * 初始化连接
     */
    private void initConnection() {
        // 连接工厂
        connectionFactory = new ActiveMQConnectionFactory(
                ServerConfConstant.MQ_USER_NAME,
                ServerConfConstant.MQ_USER_PASSWORD,
                ServerConfConstant.MQ_BROKER_URL);
        try {
            // 获取一个连接
            connection = connectionFactory.createConnection();
            // 开启连接
            connection.start();
            // 建立会话.第一个参数，是否使用事务，如果设置true，操作消息队列后，必须使用 session.commit();
            session = connection.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);
        } catch (Exception e) {
            logger.error("==========init activemq connection error.===========");
            close();
        }
    }

    /**
     * 获取topic下的所有命令（最多10条）
     *
     * @param queue 待获取命令的队列
     * @param topic
     * @return
     */
    public ArrayList<String> getMsgListFromMq(Queue queue, String topic) {
        if (map.get(topic) != null) {
            return null;
        }
        map.put(topic, topic);
        ArrayList<String> messageList = new ArrayList<>();
        MessageConsumer messageConsumer = null;
        try {
            messageConsumer = session.createConsumer(queue);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        int count = 0;
        try {
            while (count < 10) {
                ObjectMessage message = (ObjectMessage) messageConsumer
                        .receive(2000);
                if (message == null) {
                    break;
                }
                count++;
                messageList.add(message.getObject().toString());
            }
        } catch (Exception e) {
            logger.error("=========get Message error============.");
            e.printStackTrace();
        } finally {
            try {
                messageConsumer.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
            map.remove(topic);
        }
        return messageList;
    }

    public static Session getSession() {
        return session;
    }

    public static void setSession(Session session) {
        MQCommandReceiver.session = session;
    }

    public static MQCommandReceiver getCommandReceiver() {
        return commandReceiver;
    }

    /**
     * 关闭activemq连接
     */
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // 创建队列或者话题对象
        Queue queue = MQCommandReceiver.getSession().createQueue("test-48");
        ArrayList<String> messageList = MQCommandReceiver
                .getCommandReceiver().getMsgListFromMq(queue, "test-48");
        Queue queue2 = MQCommandReceiver.getSession().createQueue("test-47");
        ArrayList<String> messageList2 = MQCommandReceiver
                .getCommandReceiver().getMsgListFromMq(queue2, "test-47");
        System.out.println("end");
    }
}
