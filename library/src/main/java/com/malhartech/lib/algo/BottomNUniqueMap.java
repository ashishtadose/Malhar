/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.lib.algo;

import com.malhartech.annotation.OutputPortFieldAnnotation;
import com.malhartech.api.DefaultOutputPort;
import com.malhartech.lib.util.AbstractBaseNUniqueOperatorMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Input stream of key value pairs are ordered by key, and bottom N of the ordered unique tuples per key are emitted on
 * port "top" at the end of window<p>
 * This is an end of window module<br>
 * <br>
 * <b>Ports</b>:<br>
 * <b>data</b>: expects Map&lt;K,V&gt;<br>
 * <b>bottom</b>: emits HashMap&lt;K, ArrayList&lt;HashMap&lt;V,Integer&gt;&gt;&gt;<br>
 * <br>
 * <b>Properties</b>:<br>
 * <b>N</b>: The number of top values to be emitted per key<br>
 * <br>
 * <b>Specific compile time checks are</b>:<br>
 * N: Has to be >= 1<br>
 * <br>
 * <b>Specific run time checks are</b>: None<br>
 * <br>
 * <b>Benchmarks</b>: Blast as many tuples as possible in inline mode<br>
 * <table border="1" cellspacing=1 cellpadding=1 summary="Benchmark table for BottomNUniqueMap&lt;K,V&gt; operator template">
 * <tr><th>In-Bound</th><th>Out-bound</th><th>Comments</th></tr>
 * <tr><td><b>&gt; 15 Million K,V pairs/s</b></td><td>Bottom N unique values per key per window</td><td>In-bound throughput and number of keys is the main determinant of performance.
 * Tuples are assumed to be immutable. If you use mutable tuples and have lots of keys, the benchmarks may be lower</td></tr>
 * </table><br>
 * <p>
 * <b>Function Table (K=String,V=Integer); n=2</b>:
 * <table border="1" cellspacing=1 cellpadding=1 summary="Function table for BottomNUniqueMap&lt;K,V&gt; operator template">
 * <tr><th rowspan=2>Tuple Type (api)</th><th>In-bound (process)</th><th>Out-bound (emit)</th></tr>
 * <tr><th><i>data</i>(HashMap&lt;K,V&gt;)</th><th><i>bottom</i>(HashMap&lt;K,ArrayList&lt;HashMap&lt;V,Integer&gt;&gt;&gt;)</th></tr>
 * <tr><td>Begin Window (beginWindow())</td><td>N/A</td><td>N/A</td></tr>
 * <tr><td>Data (process())</td><td>{a=2,b=20,c=1000}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{a=-1}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{a=10,b=5}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{a=5,b=-5}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{a=3,h=20,c=2,b=-5}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{d=55,b=12}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{d=22}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{d=14}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{d=46,e=2}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{d=1,a=5}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{d=4,a=23,e=2}</td><td></td></tr>
 * <tr><td>End Window (endWindow())</td><td>N/A</td><td>{a=[{-1=1},{2=1}]}<br>{b=[{-5=2},{5=1}]}<br>{c=[{2=1},{1000=1}]}
 * <br>{d=[{1=1},{4=1}]]<br>{e=[{2=2}]}<br>{h=[{20=1}]}</td></tr>
 * </table>
 * <br>
 * @author Amol Kekre (amol@malhar-inc.com)<br>
 * <br>
 *
 */
public class BottomNUniqueMap<K, V> extends AbstractBaseNUniqueOperatorMap<K, V>
{
  @OutputPortFieldAnnotation(name = "bottom")
  public final transient DefaultOutputPort<HashMap<K, ArrayList<HashMap<V,Integer>>>> bottom = new DefaultOutputPort<HashMap<K, ArrayList<HashMap<V,Integer>>>>(this);

  /**
   * Ascending is set to false as we are looking for Bottom N
   * @return false
   */
  @Override
  public boolean isAscending()
  {
    return false;
  }

  /**
   * Emits tuple to port "bottom"
   * @param tuple
   */
  @Override
  public void emit(HashMap<K, ArrayList<HashMap<V,Integer>>> tuple)
  {
    bottom.emit(tuple);
  }
}