package com.microthings.middleware.coap.components;

import java.io.File;
import java.util.List;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.InMemoryRegistrationStore;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.impl.FileSecurityStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StaticModelProvider;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microthings.middleware.coap.constants.ServerConfConstant;

public class LwM2mService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    LeshanServerBuilder builder = new LeshanServerBuilder();
    LeshanServer lwServer = null;
    CaliforniumRegistrationStore registrationStore = null;
    EditableSecurityStore securityStore = null;

    public LwM2mService() {
        initAll();
    }

    public void initAll() {
        initModels();
        initCodec(null, null);
        initCoapConf();
        initPersistenceStore();
        this.lwServer = builder.build();
        this.lwServer.start();
    }

    // 初始化服务器支持的模型
    public void initModels() {
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/",
                ServerConfConstant.MODEL_PATHS));
        LwM2mModelProvider modelProvider = new StaticModelProvider(models);
        builder.setObjectModelProvider(modelProvider);
    }

    // 初始化编解码
    public void initCodec(LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {
        if (null == encoder) {
            builder.setEncoder(new DefaultLwM2mNodeEncoder());
        } else {
            builder.setEncoder(encoder);
        }
        if (null == decoder) {
            builder.setDecoder(new DefaultLwM2mNodeDecoder());
        } else {
            builder.setDecoder(decoder);
        }

    }

    //coap协议的参数设置
    public void initCoapConf() {
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        registrationStore = new InMemoryRegistrationStore(2);
        builder.setRegistrationStore(registrationStore);
        builder.setCoapConfig(coapConfig);
    }

    //持久化存储方案
    public void initPersistenceStore() {
        // use file persistence持久化
        securityStore = new FileSecurityStore();
        builder.setSecurityStore(securityStore);
    }

    public LeshanServer getLwServer() {
        return this.lwServer;
    }

    public CaliforniumRegistrationStore getRegistrationStore() {
        return this.registrationStore;
    }

    public SecurityStore getSecurityStore() {
        return this.securityStore;
    }

}
