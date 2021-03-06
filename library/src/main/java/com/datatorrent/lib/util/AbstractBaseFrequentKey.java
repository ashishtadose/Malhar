/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.lib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;

/**
 * <p>
 * Occurrences of each key is counted in the input stream,
 * and at the end of window the winning frequent key is emitted on output port "count". <br>
 * This module is an end of window module.
 *
 * @since 0.3.2
 */
public abstract class AbstractBaseFrequentKey<K> extends BaseKeyOperator<K>
{
  /**
   * Counts frequency of a key
   * @param tuple
   */
  public void processTuple(K tuple)
  {
    MutableInt count = keycount.get(tuple);
    if (count == null) {
      count = new MutableInt(0);
      keycount.put(cloneKey(tuple), count);
    }
    count.increment();
  }
  protected HashMap<K, MutableInt> keycount = new HashMap<K, MutableInt>();

  /**
   * override emitTuple to decide the port to emit to
   * @param tuple
   */
  abstract public void emitTuple(HashMap<K,Integer> tuple);
  /**
   * Overide emitList to specify the emit schema
   * @param tlist
   */
  abstract public void emitList(ArrayList<HashMap<K, Integer>> tlist);
  /**
   * Override compareCount to decide most vs least
   * @param val1
   * @param val2
   * @return result of compareCount to be done by subclass
   */
  abstract public boolean compareCount(int val1, int val2);

  /**
   * Emits the result.
   */
  @Override
  public void endWindow()
  {
    // Compute least frequent 
    K key = null;
    int kval = -1;
    HashMap<K, Object> map = new HashMap<K, Object>();
    for (Map.Entry<K, MutableInt> e: keycount.entrySet()) {
      if ((kval == -1)) {
        key = e.getKey();
        kval = e.getValue().intValue();
        map.put(key, null);
      }
      else if (compareCount(e.getValue().intValue(), kval)) {
        key = e.getKey();
        kval = e.getValue().intValue();
        map.clear();
        map.put(key, null);
      }
      else if (e.getValue().intValue() == kval) {
        map.put(e.getKey(), null);
      }
    }
    
    // Emit least frequent key, emit all least frequent keys list 
    // on other ports.
    HashMap<K, Integer> tuple;
    if ((key != null) && (kval > 0)) { 
      tuple = new HashMap<K, Integer>(1);
      tuple.put(key, new Integer(kval));
      emitTuple(tuple);
      ArrayList<HashMap<K, Integer>> elist = new ArrayList<HashMap<K, Integer>>();
      for (Map.Entry<K, Object> e: map.entrySet()) {
        tuple = new HashMap<K, Integer>(1);
        tuple.put(e.getKey(), kval);
        elist.add(tuple);
      }
      emitList(elist);
    }
    keycount.clear();
  }
}
