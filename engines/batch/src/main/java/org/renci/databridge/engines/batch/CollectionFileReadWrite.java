
package org.renci.databridge.engines.batch;

import java.io.*;
import java.util.*;
import com.google.gson.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.*;
import org.renci.databridge.persistence.metadata.*;

public class CollectionFileReadWrite {

  public static void writeToFile(String outputFile, CollectionTransferObject cto1) {

    BufferedWriter nodeWriter = null;

    try {
        // Create the node file and it's writer
        File oFile = new File(outputFile);
        if (!oFile.exists()) {
           oFile.createNewFile();
        }
        nodeWriter = new BufferedWriter(new FileWriter(oFile.getAbsoluteFile()));  

        // Create the Gson object and read the file
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

        nodeWriter.write(gson.toJson(cto1));

     }  catch (Exception e) {
         e.printStackTrace();
     } finally {
         try {
            nodeWriter.close();
         } catch (Exception e) {
            e.printStackTrace();
         }
     }
  }

  public static CollectionTransferObject readFromFile(String inputFile) {
  
    CollectionTransferObject theObject = null;
    BufferedReader nodeReader = null;

    try {
        // Create the node file and it's reader
        File iFile = new File(inputFile);
        if (iFile.exists()) {
           nodeReader = new BufferedReader(new FileReader(iFile.getAbsoluteFile()));  

           // Create the Gson object and read the file
           Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

           theObject = gson.fromJson(nodeReader, CollectionTransferObject.class);
        }

     }  catch (Exception e) {
         e.printStackTrace();
     } finally {
         try {
            nodeReader.close();
         } catch (Exception e) {
            e.printStackTrace();
         }
         return theObject;
     }
  }

}
