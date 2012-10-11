/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.lib.testbench;

import com.malhartech.annotation.ModuleAnnotation;
import com.malhartech.annotation.PortAnnotation;
import com.malhartech.dag.AbstractModule;
import com.malhartech.dag.FailedOperationException;
import com.malhartech.dag.ModuleConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes a in stream <b>data</b> and just drops the tuple. Increments a int count and writes the net number (rate) to console<p>
 * Mainly to be used to benchmark other modules
 * <br>
 * <br>
 * Benchmarks: This node has been benchmarked at over ?? million tuples/second in local/inline mode<br>
 *
 * <b>Tuple Schema</b>
 * Not relevant
 * <b>Port Interface</b><br>
 * <b>count</b>: Output port for emitting the results<br>
 * <b>data</b>: Input port for receiving the incoming tuple<br>
 * <br>
 * <b>Properties</b>:
 * rolling_window_count: Number of windows to average over
 * <br>
 * Compile time checks are:<br>
 * none
 * <br>
 *
 * @author amol
 */
@ModuleAnnotation(
        ports = {
  @PortAnnotation(name = DevNullCounter.IPORT_DATA, type = PortAnnotation.PortType.INPUT)
})
public class DevNullCounter extends AbstractModule
{
  public static final String IPORT_DATA = "data";
  private static Logger LOG = LoggerFactory.getLogger(DevNullCounter.class);

  private long windowStartTime = 0;
  private final int rolling_window_count_default = 1;
  private int rolling_window_count = rolling_window_count_default;
  long[] tuple_numbers = null;
  long[] time_numbers = null;
  int tuple_index = 0;
  int count_denominator = 1;
  long count_windowid = 0;
  long tuple_count = 1; // so that the first begin window starts the count down

  /**
   *
   * The Maximum number of Windows to pump out.
   */
  public static final String ROLLING_WINDOW_COUNT = "rolling_window_count";


  /**
   *
   * Code to be moved to a proper base method name
   *
   * @param config
   * @return boolean
   */
  public boolean myValidation(ModuleConfiguration config)
  {
    String rstr = config.get(ROLLING_WINDOW_COUNT);
    boolean ret = true;

    if ((rstr != null) && !rstr.isEmpty()) {
      try {
        Integer.parseInt(rstr);
      }
      catch (NumberFormatException e) {
        ret = false;
        throw new IllegalArgumentException(String.format("%s has to be an integer (%s)", ROLLING_WINDOW_COUNT, rstr));
      }
    }
    return ret;
  }

  /**
   * Sets up all the config parameters. Assumes checking is done and has passed
   *
   * @param config
   */
  @Override
  public void setup(ModuleConfiguration config) throws FailedOperationException
  {
    if (!myValidation(config)) {
      throw new FailedOperationException("Did not pass validation");
    }

    rolling_window_count = config.getInt(ROLLING_WINDOW_COUNT, rolling_window_count_default);
    windowStartTime = 0;
    if (rolling_window_count != 1) { // Initialized the tuple_numbers
      tuple_numbers = new long[rolling_window_count];
      time_numbers = new long[rolling_window_count];
      for (int i = tuple_numbers.length; i > 0; i--) {
        tuple_numbers[i - 1] = 0;
        time_numbers[i - 1] = 0;
      }
      tuple_index = 0;
    }
  }

  @Override
  public void beginWindow()
  {
    if (tuple_count != 0) { // Do not restart time if no tuples were sent
      windowStartTime = System.currentTimeMillis();
      tuple_count = 0;
    }
  }


  /**
   * convenient method for not sending more than configured number of windows.
   */
  @Override
  public void endWindow()
  {
    if (tuple_count == 0) {
      return;
    }
    long elapsedTime = System.currentTimeMillis() - windowStartTime;
    if (elapsedTime == 0) {
      elapsedTime = 1; // prevent from / zero
    }

    long average = 0;
    long tuples_per_sec = (tuple_count * 1000) / elapsedTime; // * 1000 as elapsedTime is in millis
    if (rolling_window_count == 1) {
      average = tuples_per_sec;
    }
    else { // use tuple_numbers
      long slots;
      if (count_denominator == rolling_window_count) {
        tuple_numbers[tuple_index] = tuple_count;
        time_numbers[tuple_index] = elapsedTime;
        slots = rolling_window_count;
        tuple_index++;
        if (tuple_index == rolling_window_count) {
          tuple_index = 0;
        }
      }
      else {
        tuple_numbers[count_denominator - 1] =tuple_count;
        time_numbers[count_denominator - 1] =elapsedTime;
        slots = count_denominator;
        count_denominator++;
      }
      long time_slot = 0;
      long numtuples = 0;
      for (int i = 0; i < slots; i++) {
        numtuples += tuple_numbers[i];
        time_slot += time_numbers[i];
      }
      average = (numtuples * 1000) / time_slot;
    }
    LOG.debug(String.format("Windowid (%d), Time (%d ms): The rate for %d tuples is %d. This window had %d tuples_per_sec ",
              count_windowid++, elapsedTime, tuple_count, average, tuples_per_sec));
  }



  /**
   * Process each tuple. Expects upstream node to compute number of tuples in that window and send it as an int
   *
   * @param payload
   */
  @Override
  public void process(Object payload)
  {
    tuple_count++;
  }

  /**
   *
   * Checks for user specific configuration values<p>
   *
   * @param config
   * @return boolean
   */
  @Override
  public boolean checkConfiguration(ModuleConfiguration config)
  {
    boolean ret = true;
    // TBD
    return ret && super.checkConfiguration(config);
  }
}