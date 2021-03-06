package org.jboss.rhiot.beacon.publishers.paho;
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.jboss.logging.Logger;
import org.jboss.rhiot.beacon.bluez.BeaconInfo;
import org.jboss.rhiot.beacon.common.Beacon;
import org.jboss.rhiot.beacon.common.MsgPublisher;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Scott Stark (sstark@redhat.com) (C) 2014 Red Hat Inc.
 */
public class MqttPublisher implements MqttCallback, MsgPublisher {
   private static Logger log = Logger.getLogger(MqttPublisher.class);
   private ExecutorService publishService;
   private MqttClient client;
   private String brokerURL;
   private boolean quietMode;
   private MqttConnectOptions conOpt;
   private boolean clean;
   private String password;
   private String userName;
   private String clientID;
   private File dataDir;

   public MqttPublisher(String brokerUrl, String userName, String password) {
      this(brokerUrl, userName, password, null);
   }

   public MqttPublisher(String brokerUrl, String userName, String password, String clientID) {
      this.brokerURL = brokerUrl;
      this.password = password;
      this.userName = userName;
      this.clientID = clientID;
   }

   public boolean isQuietMode() {
      return quietMode;
   }

   public void setQuietMode(boolean quietMode) {
      this.quietMode = quietMode;
   }

   public boolean isClean() {
      return clean;
   }

   public void setClean(boolean clean) {
      this.clean = clean;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getClientID() {
      return clientID;
   }

   public void setClientID(String clientID) {
      this.clientID = clientID;
   }

   public File getDataDir() {
      return dataDir;
   }

   public void setDataDir(File dataDir) {
      this.dataDir = dataDir;
   }

   // MqttCallback methods
   @Override
   public void connectionLost(Throwable cause) {
      log.warn("connectionLost", cause);
   }

   @Override
   public void messageArrived(String topic, MqttMessage message) throws Exception {
   }

   @Override
   public void deliveryComplete(IMqttDeliveryToken token) {
   }

   @Override
   public void publish(String destinationName, Beacon beacon) {
      // Connect to the MQTT server
      log.debugf("Connecting to %s with client ID %s", brokerURL, client.getClientId());
      try {
         client.connect(conOpt);
         log.debug("Connected");

         String time = new Timestamp(System.currentTimeMillis()).toString();
         log.debugf("Publishing at: %s to topic '%s'", time, destinationName);

         // Create and configure a message
         byte[] payload = beacon.toByteMsg();
         MqttMessage message = new MqttMessage(payload);
         message.setQos(0);

         // Send the message to the server, control is not returned until
         client.publish(destinationName, message);

         // Disconnect the client
         client.disconnect();
         log.debug("Disconnected");
      } catch (MqttException|IOException e) {
         log.warnf(e, "Failed to publish message for beacon: %s", beacon);
      }
   }

   @Override
   public void publishStatus(Beacon beacon) {

   }

   @Override
   public void publishProperties(String destinationName, Properties properties) {

   }

   @Override
   public void setDestinationName(String name) {

   }

   @Override
   public String getDestinationName() {
      return null;
   }

   @Override
   public int getReconnectInterval() {
      return 0;
   }

   @Override
   public void setReconnectInterval(int reconnectInterval) {

   }

   @Override
   public boolean isReconnectOnFailure() {
      return false;
   }

   @Override
   public void setReconnectOnFailure(boolean reconnectOnFailure) {

   }

   @Override
   public boolean isConnected() {
      return false;
   }

   @Override
   public void setConnected(boolean connected) {

   }

   @Override
   public void setUseTopics(boolean flag) {

   }

   @Override
   public boolean isUseTopics() {
      return false;
   }

   @Override
   public boolean isUseTransactions() {
      return false;
   }

   @Override
   public void setUseTransactions(boolean useTransactions) {

   }

   @Override
   public void publish(String destinationName, BeaconInfo beaconInfo) {

   }

   @Override
   public void publishStatus(BeaconInfo beaconInfo) {

   }

   @Override
   public void stop() {
      if(publishService != null) {
         publishService.shutdown();
         publishService = null;
      }
      if(client != null) {
         try {
            client.disconnect();
         } catch (MqttException e) {
            log.warn("Failure during client disconnect", e);
         }
         client = null;
      }
   }

   @Override
   public void start(boolean asyncMode) throws IOException, MqttException {
      publishService = Executors.newSingleThreadExecutor();

      //This sample stores in a temporary directory... where messages temporarily
      // stored until the message has been delivered to the server.
      //..a real application ought to store them somewhere
      // where they are not likely to get deleted or tampered with
      String tmpDir = System.getProperty("java.io.tmpdir");
      MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

      // Construct the connection options object that contains connection parameters
      // such as cleanSession and LWT
      conOpt = new MqttConnectOptions();
      conOpt.setCleanSession(clean);
      if (password != null) {
         conOpt.setPassword(this.password.toCharArray());
      }
      if (userName != null) {
         conOpt.setUserName(this.userName);
      }

      // Construct an MQTT blocking mode client
      if (clientID == null) {
         InetAddress addr = InetAddress.getLocalHost();
         clientID = "Parser-" + addr.getHostAddress();
      }
      client = new MqttClient(this.brokerURL, clientID, dataStore);

      // Set this wrapper as the callback handler
      client.setCallback(this);
   }

}
