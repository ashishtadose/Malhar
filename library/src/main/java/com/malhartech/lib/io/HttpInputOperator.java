/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.lib.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.malhartech.annotation.OutputPortFieldAnnotation;
import com.malhartech.annotation.ShipContainingJars;
import com.malhartech.api.BaseOperator;
import com.malhartech.api.DefaultOutputPort;
import com.malhartech.api.OperatorConfiguration;
import com.malhartech.api.SyncInputOperator;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * Reads via GET from given URL as input stream (entities on stream delimited by leading length)<p>
 * <br>
 * Data of type {@link java.util.Map} is converted to JSON. All other types are sent in their {@link Object#toString()} representation.<br>
 * <br>
 *
 */
@ShipContainingJars(classes={com.sun.jersey.api.client.ClientHandler.class})
public class HttpInputOperator extends BaseOperator implements SyncInputOperator, Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(HttpInputOperator.class);

  /**
   * Timeout interval for reading from server. 0 or negative indicates no timeout.
   */
  public int readTimeoutMillis = 0;

  /**
   * The URL of the web service resource for the POST request.
   */
  public URI resourceUrl;

  private transient Client wsClient;
  private transient WebResource resource;

  @OutputPortFieldAnnotation(name="outputPort")
  final public transient DefaultOutputPort<Map<String, Object>> outputPort = new DefaultOutputPort<Map<String, Object>>(this);

  @Override
  public void setup(OperatorConfiguration config)
  {
    wsClient = Client.create();
    wsClient.setFollowRedirects(true);
    wsClient.setReadTimeout(readTimeoutMillis);
    resource = wsClient.resource(resourceUrl);
    LOG.info("URL: {}", resourceUrl);
  }

  @Override
  public void teardown() {
    if (wsClient != null) {
      wsClient.destroy();
    }
    super.teardown();
  }

  @Override
  public Runnable getDataPoller() {
    return this;
  }

  @Override
  public void run() {
    while (true) {
      try {
        ClientResponse response = resource.header("x-stream", "rockon").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        LOG.debug("Opening stream: " + resource);

        // media type check, if any, should be configuration based
        //if (!MediaType.APPLICATION_JSON_TYPE.equals(response.getType())) {
        //  LOG.error("Unexpected response type " + response.getType());
        //  response.close();
        //} else {
          InputStream is = response.getEntity(java.io.InputStream.class);
          while (true) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] bytes = new byte[255];
            int bytesRead;
            while ((bytesRead = is.read(bytes)) != -1) {
              //LOG.debug("read {} bytes", bytesRead);
              bos.write(bytes, 0, bytesRead);
              if (is.available() == 0 && bos.size() > 0) {
                // give chance to process what we have before blocking on read
                break;
              }
            }
            if (processBytes(bos.toByteArray())) {
              LOG.debug("End of chunked input stream.");
              response.close();
              break;
            }

            if (bytesRead == -1) {
              LOG.error("Unexpected end of chunked input stream");
              response.close();
              break;
            }

            bos.reset();
          }
        //}
      } catch (Exception e) {
          LOG.error("Error reading from " + resource.getURI(), e);
      }

      try {
        Thread.sleep(500);
      }
      catch (InterruptedException e) {
        LOG.info("Exiting IO loop {}.", e.toString());
        break;
      }
    }
  }

  private boolean processBytes(byte[] bytes) throws IOException, JSONException {
    StringBuilder chunkStr = new StringBuilder();
    // hack: when line is a number we skip to next object instead of using it to read length chunk bytes
    List<String> lines = IOUtils.readLines(new ByteArrayInputStream(bytes));
    boolean endStream = false;
    int currentChunkLength = 0;
    for (String line : lines) {
      try {
        int nextLength = Integer.parseInt(line);
        if (nextLength == 0) {
          endStream = true;
          break;
        }
        // switch to next chunk
        processChunk(chunkStr, currentChunkLength);
        currentChunkLength = nextLength;

        //LOG.debug("chunk length: " + line);
      } catch (NumberFormatException e) {
        // add to chunk
        chunkStr.append(line);
        chunkStr.append("\n");
      }
    }
    // process current chunk, if any
    processChunk(chunkStr, currentChunkLength);
    return endStream;
  }

  /**
   * We are expecting a JSON object and converting one level of keys to a map, no further mapping is performed
   * @param chunk
   * @param expectedLength
   * @throws JSONException
   */
  private void processChunk(StringBuilder chunk, int expectedLength) throws JSONException {
    if (expectedLength > 0 && chunk.length() > 0) {
      //LOG.debug("completed chunk: " + chunkStr);
      JSONObject json = new JSONObject(chunk.toString());
      chunk = new StringBuilder();
      Map<String, Object> tuple = new HashMap<String, Object>();
      Iterator<?> it = json.keys();
      while (it.hasNext()) {
        String key = (String)it.next();
        Object val = json.get(key);
        if (val != null) {
          tuple.put(key, val);
        }
      }
      if (!tuple.isEmpty()) {
        LOG.debug("Got: " + tuple);
        outputPort.emit(tuple);
        chunk.setLength(0);
      }
    }
  }

}