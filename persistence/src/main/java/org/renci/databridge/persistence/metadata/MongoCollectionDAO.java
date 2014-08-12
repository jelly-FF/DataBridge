package org.renci.databridge.persistence.metadata;
import  java.util.*;
import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;
import org.bson.types.*;

/** This is the version of the Data Analysis Object for the Collection objects
 *  stored in MongoDB.  It's got the normal CRUD pattern stuff, plus whatever
 *  other methods become needed. 
 */
public class MongoCollectionDAO implements CollectionDAO {

    // This is a little confusing because both the data model and mongo
    // use the term "collection". This is the name of the mongo collection 
    // into which we are storing the DataBridge collection info.
    private static final String MongoName = new String("DB_Collection");
    private static final String MongoExtraName = new String("extra");
    private static final String MongoIdFieldName = new String("_id");



    /** 
     *  An iterator class for the CollectionTransferObject.  An instance is returned to the
     *  the user by the getCollection call.  This implements the Iterator interface.
     */
    private class MongoCollectionDAOIterator implements Iterator<CollectionTransferObject> {
       private DBCursor cursor;

       /** 
        * Returns whether or not there is a next item in this cursor.
        */
       @Override
       public boolean hasNext() {
           // Wrap the mongo cursor opened in the get call.
           return cursor.hasNext();
       }

       /** 
        * Returns the next CollectionTransferObject from the cursor or null.
        */
       @Override
       public CollectionTransferObject next() {
           CollectionTransferObject theCollection = null; 
           try {
               if (cursor.hasNext()) {
                   // Translate the date from the MongoDB representation to the 
                   // representation presented to the users in the Transfer object.
                   theCollection = new CollectionTransferObject();
                   DBObject theEntry = cursor.next();
                   theCollection.setDataStoreId(theEntry.get(MongoIdFieldName).toString());
                   theCollection.setURL((String)theEntry.get("URL"));
                   theCollection.setTitle((String)theEntry.get("title"));
                   theCollection.setDescription((String)theEntry.get("description"));
                   theCollection.setProducer((String)theEntry.get("producer"));
                   theCollection.setSubject((String)theEntry.get("subject"));
                   theCollection.setNameSpace((String)theEntry.get("nameSpace"));
                   theCollection.setVersion((int)theEntry.get("version"));
                   @SuppressWarnings("unchecked")
                   ArrayList<BasicDBObject> extraList = (ArrayList<BasicDBObject>) theEntry.get(MongoExtraName);
                   HashMap<String, String> extra = new HashMap<String, String>();
                   for (BasicDBObject extraObj : extraList) {
                       // Get the key set.  We sort of expect there to be only one but...
                       Set<String> keys = extraObj.keySet();
                       for (String thisKey : keys) {
                           extra.put(thisKey, (String) extraObj.get(thisKey));
                       }
                   }
                   theCollection.setExtra(extra);
               }
           } catch (MongoException e) {
               // should send this back using the message logs eventually
               e.printStackTrace();
           }
           return theCollection;
       }

       /** 
        * Remove the object. Currently and probably always unsupported.
        */
       @Override
       public void remove() throws UnsupportedOperationException{
           throw new UnsupportedOperationException();
       }
    }

    /** 
     * insert the specified Collection object into mongo.
     *
     * @param theCollection CollectionTransfer object containing info to be inserted.
     */
    public boolean insertCollection(CollectionTransferObject theCollection) {
        boolean returnCode = true;
        try {
          BasicDBObject thisDoc = new BasicDBObject();
          // We don't want Mongo to create an ID for us, as we will need it later
          // to files and variables.  So we are going to create the id, use it with the 
          // insert and store it in the transfer object.
          ObjectId theId = new ObjectId();
          theCollection.setDataStoreId(theId.toString());
          
          thisDoc.put(MongoIdFieldName, theId);
          thisDoc.put("URL", theCollection.getURL());
          thisDoc.put("title", theCollection.getTitle());
          thisDoc.put("description", theCollection.getDescription());
          thisDoc.put("producer", theCollection.getProducer());
          thisDoc.put("subject", theCollection.getSubject());
          thisDoc.put("nameSpace", theCollection.getNameSpace());
          thisDoc.put("version", theCollection.getVersion());
          HashMap <String, String> extra = theCollection.getExtra();

          if (null != extra) {
             // Convert the hash map of extra metadata to a list so that
             // it can be inserted as an embedded array in the Mongo table.
             List<BasicDBObject> extraList = new ArrayList<BasicDBObject>();
             for (String key: extra.keySet()) {
                 extraList.add(new BasicDBObject(key, extra.get(key)));
             }
             thisDoc.put(MongoExtraName, extraList);
          }
          DB theDB = MongoDAOFactory.getTheDB();
          DBCollection theTable = theDB.getCollection(MongoName);
          theTable.insert(thisDoc);
        } catch (MongoException e) {
            // should send this back using the message logs eventually
            e.printStackTrace(); 
            returnCode = false;
        }
        
        return returnCode;
    }


    /** 
     * retrieve an iterator for all records that match the given search key.
     *
     * @param searchMap A HashMap with search keys.
     */
    public Iterator<CollectionTransferObject> getCollections(HashMap<String, String> searchMap) {
        MongoCollectionDAOIterator theIterator = null;
        try {
            BasicDBObject thisDoc = new BasicDBObject();
            for (String key : searchMap.keySet()) {
                thisDoc.put(key, searchMap.get(key));
            }
            DB theDB = MongoDAOFactory.getTheDB();
            DBCollection theTable = theDB.getCollection(MongoName);
            DBCursor cursor = theTable.find(thisDoc);
            theIterator = new MongoCollectionDAOIterator();
            theIterator.cursor = cursor;
        } catch (MongoException e) {
            // should send this back using the message logs eventually
            e.printStackTrace(); 
        }

        return theIterator;
    }

    public boolean updateCollection( CollectionTransferObject theCollection, 
                                    Object collectionID) {
        return true;

    }

    /** 
     * delete the specified Collection object from mongo. Note that this API deletes whatever matches
     * the search keys in the search map.
     *
     * @param searchMap A HashMap with search keys.
     */
    public int deleteCollection(CollectionTransferObject theCollection) {
        WriteResult theResult = null;
        try {
            BasicDBObject thisDoc = new BasicDBObject();
            ObjectId theId = new ObjectId(theCollection.getDataStoreId());
            thisDoc.put(MongoIdFieldName, theId);
            DB theDB = MongoDAOFactory.getTheDB();
            DBCollection theTable = theDB.getCollection(MongoName);
            theResult  = theTable.remove(thisDoc);
        } catch (MongoException e) {
            // should send this back using the message logs eventually
            e.printStackTrace();
        }
        return theResult.getN();

    }

    /** 
     * delete the specified Collection object from mongo. Note that this API deletes whatever matches
     * the search keys in the search map.
     *
     * @param searchMap A HashMap with search keys.
     */
    public int deleteCollection(HashMap<String, String> searchMap) {
        WriteResult theResult = null;
        try {
            BasicDBObject thisDoc = new BasicDBObject();
            for (String key : searchMap.keySet()) {
                // Special case for the Id field though we would expect the user to use
                // deleteCollectionById instead.  Still want it to work here...
                if (key.compareTo(MongoIdFieldName) == 0) {
                    ObjectId theId = new ObjectId(searchMap.get(key));
                    thisDoc.put(key, theId);
                } else {
                    thisDoc.put(key, searchMap.get(key));
                }
                System.out.println("Inserting key: " + key + " with value " + searchMap.get(key));
            }
            DB theDB = MongoDAOFactory.getTheDB();
            DBCollection theTable = theDB.getCollection(MongoName);
            theResult  = theTable.remove(thisDoc);
        } catch (MongoException e) {
            // should send this back using the message logs eventually
            e.printStackTrace();
        }
        return theResult.getN();

    }
}
