package org.renci.databridge.engines.network;

import org.renci.databridge.util.*;
import org.renci.databridge.persistence.metadata.*;
import org.renci.databridge.persistence.network.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * A thread that listens to AMQP for messages based on specified headers and 
 * dispatches them to a handler as they arrive.
 *
 * @author lander
 */
public class NetworkEngineMessageListener extends Thread {

  protected static final long LISTENER_TIMEOUT_MS = 1000;

  protected AMQPComms amqpComms;
  protected AMQPMessageType amqpMessageType;
  protected AMQPMessageHandler amqpMessageHandler;
  protected Logger logger;
  protected Properties theProps = null;

  /**
   * @param props Properties object used for AMQPComms object initialization.
   * @param amqpMessageType
   * @param amqpMessageHandler
   * @param logger can be null.
   */
  public NetworkEngineMessageListener (Properties props,
                                       AMQPMessageType amqpMessageType, 
                                       AMQPMessageHandler amqpMessageHandler, 
                                       Logger logger) throws IOException {

    this.amqpMessageType = amqpMessageType;
    this.amqpMessageHandler = amqpMessageHandler;
    this.logger = logger;

    // creating AMQPComms here because passing it in would enable reusing
    // the same AMQPComms instance, which is not safe across multiple clients
    this.amqpComms = new AMQPComms (props);
    this.theProps = props;

  }

  protected volatile boolean terminate;
 
  /**
   * Call this to tell the listener to stop. The listener will stop in at most LISTENER_TIMEOUT_MS 
   * + any current handler processing time.
   */
  public void terminate () {
    this.terminate = true;
  }

  @Override
  public void run () {

    // Set up to talk to the metadata and network databases.
    // Only mongo and neo4j are supported at the moment.
    String dbType;
    String dbName;
    String dbHost;
    int    dbPort;
    String dbUser;
    String dbPwd;
    String networkDBLocation;
    String networkDBType;

    MetadataDAOFactory metadataFactory = null;
    NetworkDAOFactory  networkFactory = null;

    try {
        dbType = theProps.getProperty("org.renci.databridge.relevancedb.dbType", "mongo");
        dbName = theProps.getProperty("org.renci.databridge.relevancedb.dbName", "test");
        dbHost = theProps.getProperty("org.renci.databridge.relevancedb.dbHost", "localhost");
        dbPort = Integer.parseInt(theProps.getProperty("org.renci.databridge.relevancedb.dbPort", "27017"));
        dbUser = theProps.getProperty("org.renci.databridge.relevancedb.dbUser", "localhost");
        dbPwd = theProps.getProperty("org.renci.databridge.relevancedb.dbPassword", "localhost");
        networkDBLocation = theProps.getProperty("org.renci.databridge.networkdb.location", "testData");
        networkDBType = theProps.getProperty("org.renci.databridge.networkdb.dbType", "neo4j");
    } catch (Exception ex) { 
        this.logger.log (Level.SEVERE, "Could not retrieve needed properties");
        return;
    }

    if (dbType.compareToIgnoreCase("mongo") != 0) {
        this.logger.log (Level.SEVERE, "Unsupported database type: " + dbType);
        return;
    }

    if (dbType.compareToIgnoreCase("mongo") == 0) {
        metadataFactory = MetadataDAOFactory.getMetadataDAOFactory(MetadataDAOFactory.MONGODB, 
                                                              dbName, dbHost, dbPort, dbUser, dbPwd);
        if (null == metadataFactory) {
           this.logger.log (Level.SEVERE, "Couldn't produce the MetadataDAOFactory");
           return;
        }
    }

    if (networkDBType.compareToIgnoreCase("neo4j") != 0) {
        this.logger.log (Level.SEVERE, "Unsupported network database type: " + networkDBType);
        return;
    }

    if (networkDBType.compareToIgnoreCase("neo4j") == 0) {
        networkFactory = 
            NetworkDAOFactory.getNetworkDAOFactory(NetworkDAOFactory.NEO4JDB, networkDBLocation);
        if (null == networkFactory) {
           this.logger.log (Level.SEVERE, "Couldn't produce the NetworkDAOFactory");
           return;
        }
    }


    // Grab the desired headers from the type.
    String bindHeaders = this.amqpMessageType.getBindHeaders ();
    this.amqpComms.bindTheQueue (bindHeaders);

    if (this.logger != null) {
      this.logger.log (Level.FINE, "Bound '" + bindHeaders + "' to AMQPComms");
    }

    while (!terminate) { 

      try {

        AMQPMessage am = this.amqpComms.receiveMessage (LISTENER_TIMEOUT_MS);
        if (am != null) {
           if (null == metadataFactory) {
              this.logger.log (Level.SEVERE, "metadataFactory is null");
              return;
           } 
          // Need to pass both factories, so we will store them in array. The message 
          // handler also needs the property file so it can send action messages, so we
          // store it in an array of Objects along with the needed factories.
          Object theFactories[] = new Object[3];
          theFactories[0] = (Object) metadataFactory;
          theFactories[1] = (Object) networkFactory;
          theFactories[2] = (Object) theProps;
          this.amqpMessageHandler.handle (am, (Object) theFactories);
        }

      } catch (Exception e) {

        // dispatch exception to handler st it doesn't stop dispatch thread
        this.amqpMessageHandler.handleException (e);

        // @todo deal with exceptions here.

      }

    } 

  }

}
