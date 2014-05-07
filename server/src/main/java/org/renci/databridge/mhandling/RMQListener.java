package org.renci.databridge.mhandling;

import com.rabbitmq.client.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.renci.databridge.util.*;

/**
 * Server class listening for incoming messages on rabbitMQ queue 
 * on receipt: creates a new instance of the MessageHandler to handle the message
 * and forwards the message to it along the org.renci.databridge.updateQueue channel
 *
 * @author Ren Bauer -RENCI (www.renci.org)
 */

public class RMQListener{


  private enum DBs {Neo4j, Titan};

  private static String DB;
  private static String path;
  private static String primaryQueue;
  private static String updateQueue;
  private static String logQueue;
  private static int    logLevel;
  
  /**
   * Main function starts listening on org.renci.databridge.primaryQueue , and loops indefinitely
   * waiting for anh messeges, forwarding them without modification
   */
  public static void main(String[] args) throws Exception{


    try {
       Properties prop = new Properties();
       prop.load(new FileInputStream("db.conf"));
       DB = prop.getProperty("org.renci.databridge.databaseType", "Neo4j");
       path = prop.getProperty("org.renci.databridge.databasePath", "data/");
       primaryQueue = prop.getProperty("org.renci.databridge.primaryQueue", "primary");
       updateQueue = prop.getProperty("org.renci.databridge.updateQueue", "update");
       logQueue = prop.getProperty("org.renci.databridge.logQueue", "log");
       logLevel = Integer.parseInt(prop.getProperty("org.renci.databridge.logLevel", "4"));
    } catch (IOException ex){ }

    System.out.println(" database: " + DB);
    System.out.println(" path: " + path);

    DBs type;
    Object dbService;
    if(DB.equals("Titan")){
        type = DBs.Titan;
        path += "titanHB/";
        dbService = TitanFactory.open(path);
        Runtime.getRuntime().addShutdownHook(new RMQShutdownHook<TitanGraph>((TitanGraph) dbService));
    } else if(DB.equals("Neo4j")){
        type = DBs.Neo4j;
        dbService = new GraphDatabaseFactory().newEmbeddedDatabase(path);
        Runtime.getRuntime().addShutdownHook(new RMQShutdownHook<GraphDatabaseService>((GraphDatabaseService) dbService));
    } else {
       throw new Exception("Invalid database specified in properties");
    }
  
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.confirmSelect();
 
    AMQPLogger logger = new AMQPLogger();
    logger.setTheExchange("");
    logger.setTheChannel(channel);
    logger.setTheQueue(logQueue);
    logger.setTheLevel(logLevel);
    logger.publish(AMQPLogger.LOG_INFO,"DataBridge listener initiated");
    logger.publish(AMQPLogger.LOG_INFO,"database type: " + DB);
    logger.publish(AMQPLogger.LOG_INFO,"database path: " + path);

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    QueueingConsumer consumer = new QueueingConsumer(channel);
    channel.queueDeclare(primaryQueue, true, false, false, null);
    channel.basicConsume(primaryQueue, true, consumer);

    while (true) {
      QueueingConsumer.Delivery delivery = consumer.nextDelivery();
      String message = new String(delivery.getBody());
      System.out.println("received " + message);
      logger.publish(AMQPLogger.LOG_INFO,"Listener: msg received");

      switch(type){
      case Titan:
        new MessageHandler<TitanGraph>((TitanGraph) dbService, message, channel, logQueue, logLevel).start();
      break;
      case Neo4j:
        new MessageHandler<GraphDatabaseService>((GraphDatabaseService) dbService, message, channel, logQueue, logLevel).start();
      break;
      }
    }
  }
}
