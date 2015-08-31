package com.amlinv.activemq.persistence.impl;

import com.amlinv.activemq.persistence.ApplicationPersistenceAdapter;
import com.amlinv.activemq.topo.registry.BrokerRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.DestinationInfo;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/**
 * Created by art on 5/16/15.
 */
public class JsonFileApplicationPersistenceAdapter implements ApplicationPersistenceAdapter {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JsonFileApplicationPersistenceAdapter.class);

    private final File storePath;
    private Logger log = DEFAULT_LOGGER;

    private BrokerRegistry brokerRegistry;
    private DestinationRegistry queueRegistry;
    private DestinationRegistry topicRegistry;

    public JsonFileApplicationPersistenceAdapter(File storePath) {
        this.storePath = storePath;
    }

    public JsonFileApplicationPersistenceAdapter(String storePath) {
        this(new File(storePath));
    }

    @Override
    public void setQueueRegistry(DestinationRegistry queueRegistry) {
        this.queueRegistry = queueRegistry;
    }

    @Override
    public void setTopicRegistry(DestinationRegistry topicRegistry) {
        this.topicRegistry = topicRegistry;
    }

    @Override
    public void setBrokerRegistry(BrokerRegistry brokerRegistry) {
        this.brokerRegistry = brokerRegistry;
    }

    @Override
    public void load() throws IOException {
        log.info("Loading persistence data from file: file={}", this.storePath);

        Gson gson = new GsonBuilder()
                .create();

        try (InputStream inputStream = new FileInputStream(this.storePath)) {
            try (Reader rdr = new InputStreamReader(inputStream)) {
                MyDataModel updated = gson.fromJson(rdr, MyDataModel.class);

                if ( updated != null ) {
                    // Update the existing data with the new so that anyone else sharing those structures will also share
                    //  the updates.
                    this.update(updated);
                } else {
                    log.info("Persistence file empty: file={}", this.storePath);
                }
            }
        }
    }

    @Override
    public void save() throws IOException {
        log.info("Saving persistence data to file: file={}", this.storePath);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        try (OutputStream outputStream = new FileOutputStream(this.storePath)) {
            try (Writer writer = new OutputStreamWriter(outputStream)) {
                MyDataModel outBound = new MyDataModel();

                if ( this.brokerRegistry != null ) {
                    outBound.brokerRegistry = this.brokerRegistry.asMap();
                }

                if ( queueRegistry != null ) {
                    outBound.queueRegistry = this.queueRegistry.asMap();
                }

                if ( topicRegistry != null ) {
                    outBound.topicRegistry = this.topicRegistry.asMap();
                }

                gson.toJson(outBound, writer);
            }
        }
    }

    public void loadOnInit() {
        try {
            this.load();
        } catch (Exception ioExc) {
            log.error("Failed to load persistence file: file={}", this.storePath, ioExc);
        }
    }

    public void saveOnDestory() {
        try {
            this.save();
        } catch (Exception ioExc) {
            log.error("Failed to save persistence file: file={}", this.storePath, ioExc);
        }
    }

    protected void update (MyDataModel source) {
        if (source.queueRegistry != null) {
            for (String queueName : source.queueRegistry.keySet()) {
                this.queueRegistry.put(queueName, new DestinationState(source.queueRegistry.get(queueName)));
            }
        }

        if (source.topicRegistry != null) {
            for (String topicName : source.topicRegistry.keySet()) {
                this.topicRegistry.put(topicName, new DestinationState(source.topicRegistry.get(topicName)));
            }
        }

        if ( source.brokerRegistry != null ) {
            for (Map.Entry<String, BrokerInfo> entry : source.brokerRegistry.entrySet()) {
                this.brokerRegistry.put(entry.getKey(), entry.getValue());
            }
        }
    }

    protected static class MyDataModel {
        public Map<String, ? extends DestinationInfo> queueRegistry;
        public Map<String, ? extends DestinationInfo> topicRegistry;
        public Map<String, BrokerInfo> brokerRegistry;
    }
}
