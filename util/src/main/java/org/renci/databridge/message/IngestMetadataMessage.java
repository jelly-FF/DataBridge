package org.renci.databridge.message;
import org.renci.databridge.util.AMQPMessageType;

/**
 * @author mrshoffn
 */
public class IngestMetadataMessage implements AMQPMessageType {

  public static final String bindHeaders = "type:databridge;subtype:ingestmetadata;x-match:all";

  public static final String NAME = "name";
  public static final String CLASS = "className";
  public static final String NAME_SPACE = "nameSpace";
  public static final String FIRE_EVENT = "fireEvent";
  public static final String INPUT_URI = "inputURI";
  public static final String INPUT_DIR = "inputDir";

  // Message types for the ingest engine
  public static final String INSERT_METADATA_JAVA_URI_METADATADB = "Insert.Metadata.Java.URI.MetadataDB";
  public static final String INSERT_METADATA_JAVA_FILES_METADATADB = "Insert.Metadata.Java.Files.MetadataDB";
  @Override
  public String getBindHeaders () {
     return bindHeaders;
  }

}
