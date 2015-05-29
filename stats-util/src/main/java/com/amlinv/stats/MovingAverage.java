package com.amlinv.stats;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Missing samples are treated as 0 values.  Until at least one sample is recorded, however, the result is treated as
 * no data.
 * <p>
 *     Note that adding elements out-of-order is supported, but is tricky and will lead to poor results unless the
 *     values are tightly packed.  For example, adding values that pre-date the covered time period has no effect, and
 *     adding values far into the future compared to the rest of the values will lead to premature devaluation of the
 *     entire average until the intermediate values are filled-in.
 *
 *     In addition, updates anywhere besides the end of the list run in linear time - i.e. O(n).
 * </p>
 *
 * <b>CONCURRENCY WARNING:</b> this class is not safe under concurrency; synchronize all accesses as-needed for
 * concurrent use.
 *
 * Created by art on 5/27/15.
 */
public class MovingAverage {
    private final LinkedList<SampleData> samples;

    private final long sampleTimePeriod;  // In milliseconds

    /**
     * Total number of slots covered by the average; sampleTimePeriod * maxSlots gives the total time covered by the
     * average.  Virtual slots are included here, meaning slots with no recorded data, so it's not possible to increase
     * the total time covered by leaving slots empty.
     */
    private final long maxSlots;          // Number of slots

    private BigDecimal accumulator = BigDecimal.ZERO;

    private boolean full;

    public MovingAverage(long sampleTimePeriod, long maxSlots) {
        this.sampleTimePeriod = sampleTimePeriod;
        this.maxSlots = maxSlots;

        this.samples = new LinkedList<>();
    }

    /**
     * Put the given value at the timeslot for the given timestamp, replacing the existing value, if any.
     *
     * @param timestamp
     * @param value
     */
    public void put (long timestamp, long value) {
        this.update(timestamp, value, false);
    }

    /**
     * Add the given value with the specified timestamp to the average.  If data already exists for the same time
     * slot, the value is added and the average updated.
     *
     * @param timestamp
     * @param value
     */
    public void add (long timestamp, long value) {
        this.update(timestamp, value, true);
    }

    /**
     * Update the average history and accumulator with the given value at the given timestamp, adding or replacing the
     * value as specified.
     *
     * @param timestamp timestamp assigned to the value.
     * @param value value to store in the history for the moving average.
     * @param addInd true => add the value to any existing value; false => replace the current value with the new one.
     */
    protected void update (long timestamp, long value, boolean addInd) {
        long newSlot = timestamp / sampleTimePeriod;

        if ( samples.isEmpty() ) {
            //
            // First value; simply store it.
            //
            samples.add(new SampleData(newSlot, value));

            accumulator = accumulator.add(new BigDecimal(value));
        } else {
            //
            // Determine where this value fits into the history (after the end, before the start, or in the middle).
            //
            long lastSlot = samples.getLast().slot;
            if ( newSlot == lastSlot ) {
                //
                // Determine the right amount to add to the existing sample.
                //
                long updateAmount;
                if ( addInd ) {
                    updateAmount = value;
                } else {
                    updateAmount = value - samples.getLast().value;
                }

                //
                // Update by the amount needed.
                //
                samples.getLast().value += updateAmount;
                accumulator = accumulator.add(new BigDecimal(updateAmount));
            } else if ( newSlot > lastSlot ) {
                    //
                    // Add at the end and decay out old values at the start.
                    //

                samples.addLast(new SampleData(newSlot, value));

                BigDecimal decayAmount = this.decayOldValues(newSlot);

                accumulator = accumulator.subtract(decayAmount);
                accumulator = accumulator.add(BigDecimal.valueOf(value));
            } else {
                long firstSlot = samples.getFirst().slot;

                if ( newSlot >= firstSlot ) {
                    //
                    // Linear search; start at the end and work back as the most obvious use case is one adding values
                    //  at or near the end.
                    //

                    int curPos;
                    curPos = samples.size() - 1;

                    Iterator<SampleData> iter = samples.descendingIterator();
                    SampleData curSample = iter.next();

                    while ( curSample.slot > newSlot ) {
                        curSample = iter.next();
                        curPos--;
                    }

                    long updateAmount;
                    if ( newSlot == curSample.slot ) {
                        //
                        // Found a matching slot; add to it.
                        //

                        if ( addInd ) {
                            updateAmount = value;
                        } else {
                            updateAmount = value - curSample.value;
                        }

                        curSample.value += updateAmount;
                    } else {
                        //
                        // Put the new sample after the last one checked.
                        //
                        updateAmount = value;
                        samples.add(curPos + 1, new SampleData(newSlot, value));
                    }

                    //
                    // Add the amount of the update to the accumulator.
                    //
                    accumulator = accumulator.add(BigDecimal.valueOf(updateAmount));
                } else {
                    if ( newSlot > ( lastSlot - this.maxSlots ) ) {
                        //
                        // New value at the start.
                        //
                        accumulator = accumulator.add(BigDecimal.valueOf(value));

                        samples.addFirst(new SampleData(newSlot, value));
                    } else {
                        // Ignore the value - it's before the time period allowed by the moving average
                    }
                }
            }
        }
    }

    /**
     * Return the moving average of the values collected.  Early in the moving average, the values will appear strongly
     * weighted since so few samples are included in the average.
     *
     * @return average of the collected values.
     */
    public double getAverage () {
        if ( samples.size() == 0 ) {
            return  0.0;
        }

        return ( accumulator.doubleValue() / calcNumSlot() );
    }

    /**
     * Return the moving average of the values collected while assuming 0 values for any slots not yet collected.
     * Early in the moving average, the values will appear weakly weighted since so few actual values are included in
     * the average and a large number of 0 values are assumed.
     *
     * @return average of the collected values and assumed 0 values to fill out the moving average period.
     */
    public double getFullPeriodAverage () {
        return accumulator.doubleValue() / maxSlots;
    }

    /**
     * Decay old slots given the new ending slot number.
     *
     * @param endSlot the new ending slot number.
     * @return sum of the values removed from history.
     */
    protected BigDecimal decayOldValues (long endSlot) {
        BigDecimal result = BigDecimal.ZERO;

        long newFirstSlot = ( endSlot - this.maxSlots ) + 1;

        while ( samples.getFirst().slot < newFirstSlot ) {
            full = true;
            result = result.add(BigDecimal.valueOf(samples.pop().value));
        }

        return result;
    }

    /**
     * Calculate the number of effective slots used by the history, including "sparse" slots - i.e. slots for which no
     * data is actually stored.
     *
     * @return number of slots to which the accumulated data applies.
     */
    protected long calcNumSlot () {
        if ( full ) {
            return this.maxSlots;
        }

        return ( samples.getLast().slot - samples.getFirst().slot ) + 1;
    }

    protected class SampleData {
        public long slot;
        public long value;

        public SampleData(long slot, long value) {
            this.slot = slot;
            this.value = value;
        }
    }
}
