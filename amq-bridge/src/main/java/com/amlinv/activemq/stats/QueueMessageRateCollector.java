package com.amlinv.activemq.stats;

import com.amlinv.stats.MovingAverage;

/**
 * Collector of message rates for a single Queue.
 *
 * Created by art on 5/28/15.
 */
public class QueueMessageRateCollector {
    /**
     * One minute moving-average measured every second.  This one will be volatile.
     */
    public MovingAverage oneMinuteDequeueAverage = new MovingAverage(1000, 60);
    public MovingAverage oneMinuteEnqueueAverage = new MovingAverage(1000, 60);

    /**
     * One hour moving-average measured every 15 seconds.  Using 15 seconds instead of 60 seconds to keep down the
     * volatility.
     */
    public MovingAverage oneHourDequeueAverage = new MovingAverage(15000, 240);
    public MovingAverage oneHourEnqueueAverage = new MovingAverage(15000, 240);

    /**
     * One day moving-average measured every 10 minutes.  Again, 10 minutes instead of 1 hour to keep down the
     * volatility.
     */
    public MovingAverage oneDayDequeueAverage = new MovingAverage(600000, 144);
    public MovingAverage oneDayEnqueueAverage = new MovingAverage(600000, 144);

    public void onSample (long dequeueCountDelta, long enqueueCountDelta) {
        this.onTimestampSample(System.nanoTime() / 1000000L, dequeueCountDelta, enqueueCountDelta);
    }

    public void onTimestampSample (long timestamp, long dequeueCountDelta, long enqueueCountDelta) {
        synchronized ( this ) {
            this.oneMinuteDequeueAverage.add(timestamp, dequeueCountDelta);
            this.oneHourDequeueAverage.add(timestamp, dequeueCountDelta);
            this.oneDayDequeueAverage.add(timestamp, dequeueCountDelta);

            this.oneMinuteEnqueueAverage.add(timestamp, enqueueCountDelta);
            this.oneHourEnqueueAverage.add(timestamp, enqueueCountDelta);
            this.oneDayEnqueueAverage.add(timestamp, enqueueCountDelta);
        }
    }

    /**
     * Retrieve the one-minute average dequeue rate, in msg/sec.
     *
     * @return the rate in messages/second.
     */
    public double getOneMinuteAverageDequeueRate () {
        double value;
        synchronized ( this ) {
            value = this.oneMinuteDequeueAverage.getFullPeriodAverage();
        }

        return value;
    }

    /**
     * Retrieve the one-hour average dequeue rate, in msg/sec.
     *
     * @return the rate in messages/second.
     */
    public double getOneHourAverageDequeueRate () {
        double value;
        synchronized ( this ) {
            value = this.oneHourDequeueAverage.getFullPeriodAverage();
        }

        return value / 15;
    }

    /**
     * Retrieve the one-day average dequeue rate, in msg/sec.
     *
     * @return the rate in messages/second.
     */
    public double getOneDayAverageDequeueRate () {
        double value;
        synchronized ( this ) {
            value = this.oneDayDequeueAverage.getFullPeriodAverage();
        }

        return value / 600;
    }

    /**
     * Retrieve the one-minute average enqueue rate, in msg/sec.
     *
     * @return the rate in messages/second.
     */
    public double getOneMinuteAverageEnqueueRate () {
        double value;
        synchronized ( this ) {
            value = this.oneMinuteEnqueueAverage.getFullPeriodAverage();
        }

        return value;
    }

    /**
     * Retrieve the one-hour average enqueue rate, in msg/sec.
     *
     * @return the rate in messages/second.
     */
    public double getOneHourAverageEnqueueRate () {
        double value;
        synchronized ( this ) {
            value = this.oneHourEnqueueAverage.getFullPeriodAverage();
        }

        return value / 15;
    }

    /**
     * Retrieve the one-day average enqueue rate, in msg/sec.
     *
     * @return the rate in messages/second.
     */
    public double getOneDayAverageEnqueueRate () {
        double value;
        synchronized ( this ) {
            value = this.oneDayEnqueueAverage.getFullPeriodAverage();
        }

        return value / 600;
    }
}
