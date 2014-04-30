package com.amlinv.activemq.bridge.engine;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
 * Created by art on 4/28/14.
 */
public class AmqBridgeStatistics {
    public static final String  MESSAGES_RECEIVED_METER_NAME = "messagesReceivedMeter";
    public static final String  MESSAGES_SENT_COUNTER_NAME   = "messagesSentCounter";
    public static final String  MESSAGES_ERROR_METER_NAME    = "messagesErrorMeter";

    private static MetricRegistry   defaultMetricRegistry = new MetricRegistry();

    private String          bridgeId;
    private MetricRegistry  metricsRegistry;
    private Meter           messagesReceivedMeter;
    private Counter         messagesSentCounter;    // Doesn't need a meter; will have the same rates as received
    private Meter           messageErrorMeter;

    public static void  setDefaultMetricRegistry (MetricRegistry newDefault) {
        defaultMetricRegistry = newDefault;
    }

    public AmqBridgeStatistics (String newBridgeId, MetricRegistry registry) {
        this.bridgeId              = newBridgeId;
        this.metricsRegistry       = registry;
        this.messagesReceivedMeter = this.metricsRegistry.meter(this.bridgeStatName(bridgeId, MESSAGES_RECEIVED_METER_NAME));
        this.messagesSentCounter   = this.metricsRegistry.counter(this.bridgeStatName(bridgeId, MESSAGES_SENT_COUNTER_NAME));
        this.messageErrorMeter     = this.metricsRegistry.meter(this.bridgeStatName(bridgeId, MESSAGES_ERROR_METER_NAME));
    }

    public AmqBridgeStatistics (String newBridgeId) {
        this(newBridgeId, defaultMetricRegistry);
    }

    public String   getBridgeId () {
        return  this.bridgeId;
    }

    public long getNumMessagesReceived () {
        return  this.messagesReceivedMeter.getCount();
    }

    public double getMessageRate() {
        return  this.messagesReceivedMeter.getMeanRate();
    }

    public double getMessageOneMinuteRate () {
        return  this.messagesReceivedMeter.getOneMinuteRate();
    }

    /**
     * Retrieve the message rates recorded: mean, 1 minute, 5 minutes, and 15 minute.
     * @return
     */
    public double[] getRecordedMessageRates () {
        double[] result = new double[4];

        synchronized ( this.messagesReceivedMeter ) {
            result[0] = this.messagesReceivedMeter.getMeanRate();
            result[1] = this.messagesReceivedMeter.getOneMinuteRate();
            result[2] = this.messagesReceivedMeter.getFifteenMinuteRate();
            result[3] = this.messagesReceivedMeter.getFifteenMinuteRate();
        }

        return  result;
    }

    public long getNumErrorMessagesReceived () {
        return  this.messageErrorMeter.getCount();
    }

    public double getErrorMessageRate() {
        return  this.messageErrorMeter.getMeanRate();
    }

    public double getErrorMessageOneMinuteRate () {
        return  this.messageErrorMeter.getOneMinuteRate();
    }

    /**
     * Retrieve the message rates recorded: mean, 1 minute, 5 minutes, and 15 minute.
     * @return
     */
    public double[] getErrorMessageRates () {
        double[] result = new double[4];

        synchronized ( this.messageErrorMeter ) {
            result[0] = this.messageErrorMeter.getMeanRate();
            result[1] = this.messageErrorMeter.getOneMinuteRate();
            result[2] = this.messageErrorMeter.getFifteenMinuteRate();
            result[3] = this.messageErrorMeter.getFifteenMinuteRate();
        }

        return  result;
    }

    public void incrementMessagesReceived () {
        synchronized ( this.messagesReceivedMeter ) {
            this.messagesReceivedMeter.mark();
        }
    }

    public void incrementMessagesSent () {
        synchronized ( this.messagesSentCounter ) {
            this.messagesSentCounter.inc();
        }
    }

    public void incrementMessageErrors () {
        synchronized ( this.messageErrorMeter ) {
            this.messageErrorMeter.mark();
        }
    }

    protected String    bridgeStatName (String bridgeId, String statName) {
        return  MetricRegistry.name(AmqBridgeStatistics.class, bridgeId, statName);
    }
}
