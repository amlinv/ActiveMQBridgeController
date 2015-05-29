package com.amlinv.stats;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MovingAverageTest {
    public static final long SLOT_SIZE = 10000;
    public static final long NUM_SLOT = 60;

    private MovingAverage movingAverage;

    @Before
    public void setupTest() throws Exception {
        //
        // 10-second slots with 60 seconds of data
        //
        movingAverage = new MovingAverage(SLOT_SIZE, 60);
    }

    @Test
    public void testAdd() throws Exception {
        assertEquals(0.0, movingAverage.getAverage(), 0.0);
        assertEquals(0.0, movingAverage.getFullPeriodAverage(), 0.0);

        movingAverage.add(SLOT_SIZE * 10, 10);
        assertEquals(10.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(10.0 / 60.0, movingAverage.getFullPeriodAverage(), 0.0000001);

        movingAverage.add((SLOT_SIZE * 11) - 1, 10);
        assertEquals(20.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(20.0 / 60.0, movingAverage.getFullPeriodAverage(), 0.0000001);

        movingAverage.add(SLOT_SIZE * 11, 10);
        assertEquals(15.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(30.0 / 60.0, movingAverage.getFullPeriodAverage(), 0.0000001);
    }

    @Test
    public void testFullSlots() throws Exception {
        long curSlot = 0;
        while (curSlot < NUM_SLOT) {
            movingAverage.add(curSlot * SLOT_SIZE, 10);
            assertEquals(10.0, movingAverage.getAverage(), 0.0000001);
            assertEquals(((10.0 * (curSlot + 1)) / NUM_SLOT), movingAverage.getFullPeriodAverage(), 0.0000001);

            curSlot++;
        }

        //
        // Add 10 more values and verify the average.
        //
        curSlot = 0;
        while (curSlot < 10) {
            movingAverage.add(curSlot * SLOT_SIZE + (SLOT_SIZE * NUM_SLOT), 10);
            assertEquals(10.0, movingAverage.getAverage(), 0.0000001);
            assertEquals(10.0, movingAverage.getFullPeriodAverage(), 0.0000001);

            curSlot++;
        }
    }

    @Test
    public void testContinuallyIncreasingValues() throws Exception {
        //
        // Verify the average up until the history is full.
        //
        long accum = 0;
        long curSlot = 0;
        while ( curSlot < NUM_SLOT ) {
            long value = curSlot + 1;

            movingAverage.add(curSlot * SLOT_SIZE, value);
            accum += value;

            assertEquals(((double) accum) / ( curSlot + 1 ), movingAverage.getAverage(), 0.0000001);
            assertEquals(((double) accum) / NUM_SLOT, movingAverage.getFullPeriodAverage(), 0.0000001);

            curSlot++;
        }

        //
        // Now verify the history as values decay off.
        //
        curSlot = 0;
        while ( curSlot < 10 ) {
            long oldValue = curSlot + 1;
            long value = curSlot + NUM_SLOT + 1;

            movingAverage.add((curSlot + NUM_SLOT) * SLOT_SIZE, value);
            accum += value - ( oldValue );

            assertEquals(((double) accum) / NUM_SLOT, movingAverage.getAverage(), 0.0000001);
            assertEquals(((double) accum) / NUM_SLOT, movingAverage.getFullPeriodAverage(), 0.0000001);

            curSlot++;
        }

        //
        // Now verify decay of more than one value.
        //
        long value = curSlot + NUM_SLOT + 1;
        movingAverage.add((curSlot + NUM_SLOT + 9) * SLOT_SIZE, value);

        long decayIter = 0;
        long totalDecay = 0;
        while ( decayIter < 10 ) {
            totalDecay += curSlot + decayIter + 1;
            decayIter++;
        }

        accum += value - totalDecay;

        assertEquals(((double) accum) / NUM_SLOT, movingAverage.getAverage(), 0.0000001);
        assertEquals(((double) accum) / NUM_SLOT, movingAverage.getFullPeriodAverage(), 0.0000001);
    }

    @Test
    public void testConcreteValues () throws Exception {
        MovingAverage movingAverage2 = new MovingAverage(1000, 3);

        movingAverage2.add(0, 1);
        assertEquals(1.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(0.3333333, movingAverage2.getFullPeriodAverage(), 0.000001);

        movingAverage2.add(1000, 2);
        assertEquals(1.5, movingAverage2.getAverage(), 0.0000001);
        assertEquals(1.0, movingAverage2.getFullPeriodAverage(), 0.000001);

        movingAverage2.add(2000, 3);
        assertEquals(2.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(2.0, movingAverage2.getFullPeriodAverage(), 0.000001);

        movingAverage2.add(3000, 4);
        assertEquals(3.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(3.0, movingAverage2.getFullPeriodAverage(), 0.000001);

        movingAverage2.add(5000, 5);
        assertEquals(3.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(3.0, movingAverage2.getFullPeriodAverage(), 0.0000001);

        movingAverage2.add(8000, 6);
        assertEquals(2.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(2.0, movingAverage2.getFullPeriodAverage(), 0.0000001);

        movingAverage2.add(12000, 9);
        assertEquals(3.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(3.0, movingAverage2.getFullPeriodAverage(), 0.0000001);

        movingAverage2.add(10000, 10);
        assertEquals(19.0 / 3.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(19.0 / 3.0, movingAverage2.getFullPeriodAverage(), 0.0000001);

        movingAverage2.add(11000, 11);
        assertEquals(10.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(10.0, movingAverage2.getFullPeriodAverage(), 0.0000001);


        // OLD slot; will get ignored
        movingAverage2.add(9000, 12);
        assertEquals(10.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(10.0, movingAverage2.getFullPeriodAverage(), 0.000001);

        // Match an existing middle slot
        movingAverage2.add(11000, 13);
        assertEquals(43.0 / 3.0, movingAverage2.getAverage(), 0.0000001);
        assertEquals(43.0 / 3.0, movingAverage2.getFullPeriodAverage(), 0.0000001);
    }

    @Test
    public void testPut () throws Exception {
        movingAverage.put(10000, 100);
        assertEquals(100.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(100.0 / 60, movingAverage.getFullPeriodAverage(), 0.0000001);

        movingAverage.put(10000, 200);
        assertEquals(200.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(200.0 / 60, movingAverage.getFullPeriodAverage(), 0.0000001);

        movingAverage.put(20000, 300);
        assertEquals(250.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(500.0 / 60, movingAverage.getFullPeriodAverage(), 0.0000001);

        movingAverage.put(30000, 400);
        assertEquals(300.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(900.0 / 60, movingAverage.getFullPeriodAverage(), 0.0000001);

        movingAverage.put(20000, 600);
        assertEquals(400.0, movingAverage.getAverage(), 0.0000001);
        assertEquals(1200.0 / 60, movingAverage.getFullPeriodAverage(), 0.0000001);
    }
}
