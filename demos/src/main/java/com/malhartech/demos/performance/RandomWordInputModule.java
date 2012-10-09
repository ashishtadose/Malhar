/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.malhartech.demos.performance;

import com.malhartech.annotation.ModuleAnnotation;
import com.malhartech.annotation.PortAnnotation;
import com.malhartech.annotation.PortAnnotation.PortType;
import com.malhartech.dag.AbstractInputModule;
import com.malhartech.dag.AbstractModule;
import com.malhartech.dag.Component;
import com.malhartech.dag.Tuple;

/**
 *
 * @author Chetan Narsude <chetan@malhar-inc.com>
 */
@ModuleAnnotation(ports = {
  @PortAnnotation(name = Component.OUTPUT, type = PortType.OUTPUT)
})
public class RandomWordInputModule extends AbstractInputModule
{
  byte[] output = new byte[8];
  long lastWindowId = 0;
  int count = 1;
  int totalIterations = 0;

  @Override
  public final void process(Object payload)
  {
    if (((Tuple)payload).getWindowId() == lastWindowId) {
      emit(output);
      count++;
    }
    else {
      for (int i = count--; i-- > 0;) {
        emit(output);
      }
      lastWindowId = ((Tuple)payload).getWindowId();
      if (totalIterations++ > 10)
      deactivate();
    }
  }
}