package org.iplantc.service.metadata.dao;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.iplantc.service.metadata.model.enumerations.PermissionType.ALL;

import java.net.UnknownHostException;
import java.sql.Date;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mongodb.*;
import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.MongoClients;
import org.apache.log4j.Logger;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.exceptions.MetadataQueryException;
import org.iplantc.service.metadata.exceptions.MetadataStoreException;
import org.iplantc.service.metadata.model.MetadataDocument;
import org.iplantc.service.metadata.model.MetadataItem;
import org.iplantc.service.metadata.model.MetadataPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.json.JSONObject;

import javax.persistence.Basic;

public class MetadataDao {
    
    private static final Logger log = Logger.getLogger(MetadataDao.class);
    
    private MongoDatabase db = null;
    private MongoClient mongoClient = null;
    private MongoClients mongoClients = null;

    private static MetadataDao dao = null;
    
    public static MetadataDao getInstance() {
        if (dao == null) {
            dao = new MetadataDao();
        }
        
        return dao;
    }

    /**
     * Establishes a connection to the mongo server
     *
     * @return valid mongo client connection
     * @throws UnknownHostException when the host cannot be found
     */
    public MongoClient getMongoClient() throws UnknownHostException
    {
        if (mongoClient == null )
        {
            mongoClient = new MongoClient(
                    new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT),
                    getMongoCredential(),
                    MongoClientOptions.builder().build());
        }

        return mongoClient;
    }

    public com.mongodb.client.MongoClient getMongoClients() throws UnknownHostException {
        if (mongoClients == null) {
            CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));

            return mongoClients.create(MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(
                            new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT))))
                    .credential(getMongoCredential())
                    .codecRegistry(pojoCodecRegistry)
                    .build());
        }
        return null;
    }

    /**
     * Creates a new MongoDB credential for the database collections
     * @return valid mongo credential for this instance
     */
    private MongoCredential getMongoCredential() {
        return MongoCredential.createScramSha1Credential(
                Settings.METADATA_DB_USER, Settings.METADATA_DB_SCHEME, Settings.METADATA_DB_PWD.toCharArray());
    }


    /**
     * Gets the default metadata collection from the default mongodb metatadata db.
     * @return collection from the db
     * @throws UnknownHostException if the connection cannot be found/created, or db connection is bad
     */
    public MongoCollection getDefaultCollection() throws UnknownHostException {
        return getCollection(Settings.METADATA_DB_SCHEME, Settings.METADATA_DB_COLLECTION);
    }

    /**
     * Gets the named collection from the named db.
     * @param dbName database name
     * @param collectionName collection name
     * @return collection from the db
     * @throws UnknownHostException if the connection cannot be found/created, or db connection is bad
     */
    public MongoCollection getCollection(String dbName, String collectionName) throws UnknownHostException {

        db = getMongoClient().getDatabase(dbName);
        db = getMongoClients().getDatabase(dbName); //update to 4.0

        //MongoCollection<MetadataDocument> newDb = db.getCollection("MetadataDocument", MetadataDocument.class);


        // Gets a collection, if it does not exist creates it
        return db.getCollection(collectionName);
    }

    /**
     * Stores the provided {@link MetadataItem} in the mongo collection
     * @param metadataItem the {@link MetadataItem} to be inserted
     * @return the inserted {@link MetadataItem}
     * @throws MetadataStoreException when the insertion failed
     */
    public MetadataItem insert(MetadataItem metadataItem) throws MetadataStoreException {
        ObjectMapper mapper = new ObjectMapper();
        MongoCollection collection;
        try {
            collection = getDefaultCollection();
            collection.insertOne(Document.parse(mapper.writeValueAsString(metadataItem)));
            return metadataItem;
        } catch (UnknownHostException| JsonProcessingException e) {
            throw new MetadataStoreException("Failed to insert metadata item", e);
        }
    }

    /**
     * Return the metadataItem from the collection
     * @param metadataItem to be returned
     * @return metadataItem
     * @throws MetadataStoreException
     */
    public MetadataItem find(MetadataItem metadataItem, String user, BasicDBObject query) throws MetadataStoreException {
        MongoCollection collection;

        try{
            collection = getDefaultCollection();

            //generate permissions list
//            BasicDBList pemList = new BasicDBList();
//            for (MetadataPermission pem : metadataItem.getPermissions()){
//                pemList.add(new BasicDBObject("username:", pem.getUsername())
//                        .append("group", null)
//                        .append("permissions:", Arrays.asList(pem.getPermission().toString())));
//            }

            BasicDBList or = new BasicDBList();
            or.add(new BasicDBObject("owner",user));
            or.add(new BasicDBObject("permissions",
                    new BasicDBObject("$nin", Arrays.asList(PermissionType.NONE.toString()))));


            //generate query
            BasicDBObject doc = new BasicDBObject("uuid", metadataItem.getUuid())
                    .append("tenantId", metadataItem.getTenantId())
                    .append("schemaId", metadataItem.getSchemaId())
                    .append("$or", or);

            BasicDBList aggList = new BasicDBList();
            aggList.add(new BasicDBObject("$match", doc));

            if (query != null) {
                aggList.add(new BasicDBObject("$match", query));
            }

            MongoCursor<Document> cursor = null;
            cursor =  collection.aggregate(aggList).iterator();

            //cursor = collection.find().iterator();ß

            if (cursor != null) {
                while (cursor.hasNext()) {

                    //create Metadata Item for result
                    Document result = cursor.next();

                    return parseResult(result);
                    //return (MetadataItem) cursor.next();
                }
            }

        } catch (UnknownHostException e) {
            throw new MetadataStoreException("Failed to find metadata item", e);
        }
        return null;
    }

    /**
     * Removes the permission for the specified user
     * @param metadataItem to be updated
     * @param user to be removed
     * @throws MetadataStoreException
     */
    public MetadataItem deleteUserPermission(MetadataItem metadataItem, String user) throws MetadataStoreException{
        MongoCollection collection;
        try{
            collection = getDefaultCollection();

            BasicDBObject query = new BasicDBObject("uuid", metadataItem.getUuid());
            query.append("permission.user", user);

            BasicDBObject pullQuery = new BasicDBObject("permission", new BasicDBObject("username", user))
                    .append("uuid", metadataItem.getUuid());

            collection.updateOne(query, new BasicDBObject("$pull", pullQuery));

            MetadataPermission toDelete = metadataItem.getPermissions_User(user);
            metadataItem.updatePermissions_delete(toDelete);

            return metadataItem;

        } catch (Exception e){
            throw new MetadataStoreException("Failed to delete user's permission", e);
        }
    }

    /**
     * Removes all permissions for the metadataItem
     * @param metadataItem to be updated
     * @throws MetadataStoreException
     */
    public void deleteAllPermissions(MetadataItem metadataItem) throws MetadataStoreException{
        MongoCollection collection;
        try{
            collection = getDefaultCollection();

            metadataItem.setPermissions(new ArrayList<MetadataPermission>());

            BasicDBList pemList = setPermissionsForDB(metadataItem.getPermissions());

            BasicDBObject query = new BasicDBObject("uuid", metadataItem.getUuid());
            collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject("permissions", pemList)));

        } catch (Exception e) {
            throw new MetadataStoreException("Failed to delete all permissions", e);
        }
    }

    /**
     * Update the permision for the specified user to the specified permission
     * @param metadataItem to be updated
     * @param user to be updated
     * @param pem PermissionType to be updated
     * @throws MetadataStoreException
     */
    public void updatePermission(MetadataItem metadataItem, String user, PermissionType pem) throws MetadataStoreException{
        MongoCollection collection;
        try{
            collection = getDefaultCollection();

            MetadataPermission newPem = new MetadataPermission(metadataItem.getUuid(), user, pem);
            metadataItem.updatePermissions(newPem);

            BasicDBObject query = new BasicDBObject("uuid", metadataItem.getUuid());
            BasicDBList pemList = setPermissionsForDB(metadataItem.getPermissions());
            collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject("permissions", pemList)));

        } catch (Exception e) {
            throw new MetadataStoreException("Failed to update permission", e);
        }
    }

    public void updateMetadata (MetadataItem metadataItem, String user) throws MetadataStoreException {
        MongoCollection collection;

        try{
            collection = getDefaultCollection();
            BasicDBList aggList = new BasicDBList();
            BasicDBObject permType = new BasicDBObject("$nin", Arrays.asList(PermissionType.NONE, PermissionType.READ));

            BasicDBObject pem = new BasicDBObject("username", user)
                    .append("permissions", permType);

            BasicDBList or = new BasicDBList();
            or.add(new BasicDBObject("permission", new BasicDBObject("$elemMatch", pem)));
            or.add(new BasicDBObject("$or", user));

            BasicDBObject query = new BasicDBObject("uuid", metadataItem.getUuid())
                    .append("tenantId", metadataItem.getTenantId())
                    .append("schemaId", metadataItem.getSchemaId())
                    .append("internalUsername", metadataItem.getInternalUsername())
                    .append("$or", or);

            aggList.add(query);
            collection.updateOne((Bson) aggList, new BasicDBObject("$set", new BasicDBObject("value", metadataItem.getValue())));

            //  new BasicDBObject("$set", new BasicDBObject("permissions", metadataItem.getPermissions())));


        } catch (Exception e) {
            throw new MetadataStoreException("Failed to update metadata");
        }
    }

    public BasicDBList setPermissionsForDB(List<MetadataPermission> pemList){
        BasicDBList returnList = new BasicDBList();
        for (MetadataPermission pem : pemList) {
            returnList.add(new BasicDBObject("username:", pem.getUsername())
                    .append("group", null)
                    .append("permissions:", Arrays.asList(pem.getPermission().toString())));
        }
        return returnList;
    }

    public MetadataItem parseResult(Document result){
        MetadataItem returnItem = new MetadataItem();

        returnItem.setUuid(result.get("uuid").toString());
        returnItem.setTenantId(result.get("tenantId").toString());

        returnItem.setSchemaId((result.get("schemaId")==null) ? null : result.get("schemaId").toString());
        returnItem.setOwner(result.get("owner").toString());
        returnItem.setName(result.get("name").toString());

        String jsonResult = result.toJson();

//        returnItem.setValue(result.get("value"));   //change to json?
        returnItem.setInternalUsername((result.get("schemaId")==null) ? null : result.get("internalUsername").toString());
//        Date created = new DateFormat.parse(result.get("created").toString());
//        returnItem.setCreated(new Date(result.get("created").toString()));
//        returnItem.setLastUpdated(result.get("lastUpdated").toString());
//        returnItem.setPermissions(result.get("permissions").toString());
        return returnItem;
    }

//    public MetadataItem persist(MetadataItem item) throws MetadataStoreException {
//        ObjectMapper mapper = new ObjectMapper();
//        MongoCollection collection;
//        try {
//            collection = getDefaultCollection();
//            collection.insertOne(BasicDBObject.parse(mapper.writeValueAsString(item)));
////            BsonDocument query = new BsonDocument()
////                    .append("uuid", new BsonString(item.getUuid()))
////                    .append("tenant_id", new BsonString(TenancyHelper.getCurrentTenantId()));
////            Object obj = collection.find(query).limit(1).first();
//            return item;
//        } catch (UnknownHostException|JsonProcessingException e) {
//            throw new MetadataStoreException("Failed to insert metadata item", e);
//        }
//    }

//    public JacksonDBCollection<MetadataItem, String> getDefaultCollection() throws UnknownHostException {
//        return getCollection(Settings.METADATA_DB_SCHEME, Settings.METADATA_DB_COLLECTION);
//    }
//    
//    public JacksonDBCollection<MetadataItem, String> getCollection(String dbName, String collectionName) throws UnknownHostException {
//        
//        db = getClient().getDB(dbName);
//        // Gets a collection, if it does not exist creates it
//        return JacksonDBCollection.wrap(db.getCollection(collectionName), MetadataItem.class,
//                String.class);
//    }
//    
//    @SuppressWarnings("deprecation")
//    private MongoClient getClient() throws UnknownHostException {
//        if (mongoClient == null) {
//            
//            mongoClient = new MongoClient(new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT), 
//                    Arrays.asList(MongoCredential.createMongoCRCredential(
//                            Settings.METADATA_DB_USER, "api", Settings.METADATA_DB_PWD.toCharArray())));
//        } 
//        else if (!mongoClient.getConnector().isOpen()) 
//        {
//            try { mongoClient.close(); } catch (Exception e) { log.error("Failed to close mongo client.", e); }
//            mongoClient = null;
//            mongoClient = new MongoClient(new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT), 
//                    Arrays.asList(MongoCredential.createMongoCRCredential(
//                            Settings.METADATA_DB_USER, "api", Settings.METADATA_DB_PWD.toCharArray())));
//        }
//            
//        return mongoClient;
//    }
//    
//    /**
//     * Generates a {@link Query} from the given {@code uuid} and {@link TenancyHelper#getCurrentTenantId()}
//     * @param uuid
//     * @return
//     */
//    private Query _getDBQueryForUuidAndTenant(String uuid) {
//        return _getDBQueryForUuidAndTenant(uuid, TenancyHelper.getCurrentTenantId());
//    }
//    
//    /**
//     * Generates a {@link Query} from the given {@code uuid} and {code tenantId}
//     * @param uuid
//     * @param tenantId
//     * @return
//     */
//    private Query _getDBQueryForUuidAndTenant(String uuid, String tenantId) {
//        return DBQuery.and(
//                DBQuery.is("uuid", uuid), 
//                DBQuery.is("tenantId", tenantId));
//    }
//
//    /**
//     * Returns a {@link MetadataItem} with the matching {@code uuid} and {code tenantId}
//     * @param uuid
//     * @param tenantId
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataItem findByUuidAndTenant(String uuid, String tenantId) 
//    throws MetadataQueryException 
//    {   
//        try {
//            return (MetadataItem)getDefaultCollection().findOne(_getDBQueryForUuidAndTenant(uuid, tenantId));
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to find metadata by UUID", e);
//        }        
//    }
//    
//    
//    /**
//     * Implements search for metadata items.
//     * 
//     * @param username
//     * @param tenantId
//     * @param userSearchTermMap
//     * @param offset
//     * @param limit
//     * @return
//     * @throws MetadataQueryException
//     */
//    public DBCursor<MetadataItem> findMatching(String username, String tenantId, Map<SearchTerm, Object> userSearchTermMap, int offset, int limit)
//    throws MetadataQueryException 
//    {   
//        try {
//            
//            DBObject userSearchCriteria = parseUserSearchCriteria(userSearchTermMap);
//            
//            DBObject tenantCriteria = QueryBuilder.start("tenantId").is(tenantId).get();
//            
//            DBCursor<MetadataItem> cursor = null; 
//                    
//            // skip permission queries if user is admin
//            if (AuthorizationHelper.isTenantAdmin(username)) {
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                                    userSearchCriteria,
//                                    tenantCriteria).get())
//                                .skip(offset)
//                                .limit(limit);
//            } 
//            // non admins must check permissions for ownership or read grants
//            else {
//                DBObject authCriteria = createAuthCriteria(username, READ);
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                                    authCriteria,
//                                    userSearchCriteria,
//                                    tenantCriteria).get())
//                                .skip(offset)
//                                .limit(limit);
//            }                
//        
//            return cursor;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Failed to fetch metadata from db", e);
//        }        
//    }
//    
//    public DBCursor<MetadataItem> legacyFindMatching(String username, String tenantId, Map<SearchTerm, Object> userSearchTermMap, int offset, int limit)
//    throws MetadataQueryException 
//    {
//        try {
//            DBObject userSearchCriteria = parseUserSearchCriteria(userSearchTermMap);
//            
//            DBObject tenantCriteria = QueryBuilder.start("tenantId").is(tenantId).get();
//            
//            DBCursor<MetadataItem> cursor = null; 
//                    
//            // skip permission queries if user is admin
//            if (AuthorizationHelper.isTenantAdmin(username)) {
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                                    userSearchCriteria,
//                                    tenantCriteria).get())
//                                .skip(offset)
//                                .limit(limit);
//            } 
//            // non admins must check permissions for ownership or read grants
//            else {
//                
//                DBObject ownerCriteria = QueryBuilder.start("owner").in(
//                        Arrays.asList(Settings.PUBLIC_USER_USERNAME,
//                                      Settings.WORLD_USER_USERNAME,
//                                      username)).get();
//                
//                List<String> relationalSharedMetadataUuid = 
//                        MetadataPermissionDao.getUuidOfAllSharedMetataItemReadableByUser(username);
//                
//                DBObject sharedMetadataCriteria = QueryBuilder.start("uuid").in(
//                        relationalSharedMetadataUuid).get();
//                
//                DBObject authCriteria = QueryBuilder.start()
//                            .or(
//                                ownerCriteria, 
//                                sharedMetadataCriteria
//                                )
//                        .get();
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                        authCriteria, 
//                        userSearchCriteria,
//                        tenantCriteria).get())
//                    .skip(offset)
//                    .limit(limit);
//            }
//            
//            return cursor;
//        } 
//        catch (Exception e) {
//            throw new MetadataQueryException("Failed to fetch metadata from db", e);
//        } 
//    }
//    
//    /**
//     * Delete one or more metadata value fields atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @throws MetadataQueryException
//     */
//    public void delete(String uuid, String tenantId, List<String> uuids) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (uuids == null || uuids.isEmpty()) {
//                return;
//             }
//             else 
//             {
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().remove(
//                         DBQuery.and(DBQuery.in("uuid", uuids), DBQuery.is("tenantId", TenancyHelper.getCurrentTenantId())));
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to delete one or more items", 
//                             result.getWriteResult().getLastError().getException());
//                 }
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    
//    /**
//     * Unsets one or more metadata value fields atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> unset(String uuid, String tenantId, List<String> fields) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (fields == null || fields.isEmpty()) {
//                return new ArrayList<MetadataItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: fields) {
//                     if (builder == null) {
//                         builder = DBUpdate.unset(key);
//                     } else {
//                         builder = builder.unset(key);
//                     }
//                 }
//                 
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to unset one or more item fields", 
//                             result.getWriteResult().getLastError().getException());
//                 }
//                 return result.getSavedObjects();
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//
//    /**
//     * Insert a new metadata item
//     * @param item
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataItem insert(MetadataItem item) throws MetadataQueryException {
//        try {
//            WriteResult<MetadataItem, String> result = getDefaultCollection().insert(item);
//            return result.getSavedObject();
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Insert a new metadata item if another {@link MetadataItem} with the same {@code name}, 
//     * {@code tenantId}, {@code owner}, {@code value}, {@code associatedIds}, and {@code internalUser} 
//     * does not already exist.
//     *  
//     * @param item
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataItem insertIfNotPresent(MetadataItem item) 
//    throws MetadataQueryException 
//    {
//        try {
//            MetadataItem existingItem = getDefaultCollection().findOne(DBQuery.and(
//                                                                    DBQuery.is("name", item.getUuid()), 
//                                                                    DBQuery.is("tenantId", item.getTenantId()),
//                                                                    DBQuery.is("owner",  item.getOwner()),
//                                                                    DBQuery.is("value",  item.getValue()),
//                                                                    DBQuery.all("associatedIds", item.getAssociations().getRawUuid()),
//                                                                    DBQuery.is("internalUser",  item.getInternalUsername())));
//            
//            if (existingItem == null) {
//                return getDefaultCollection().insert(item).getSavedObject();
//            } else {
//                return existingItem;
//            }
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Update one or more metadata names or values in part or whole atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> update(String uuid, String tenantId, Map<String, JsonNode> updates) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (updates == null || updates.isEmpty()) {
//                return new ArrayList<MetadataItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: updates.keySet()) {
//                     if (builder == null) {
//                         builder = DBUpdate.set(key, updates.get(key));
//                     } else {
//                         builder = builder.set(key, updates.get(key));
//                     }
//                 }
//                 
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to update one or more items", 
//                             result.getWriteResult().getLastError().getException());
//                 }
//                 return result.getSavedObjects();
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Add the given value to the array value if it doesn't already exist in the specified field atomically
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> add(String uuid, String tenantId, Map<String, JsonNode> updates) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (updates == null || updates.isEmpty()) {
//                return new ArrayList<MetadataItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: updates.keySet()) {
//                     if (builder == null) {
//                         builder = DBUpdate.addToSet(key, updates.get(key));
//                     } else {
//                         builder = builder.addToSet(key, updates.get(key));
//                     }
//                 }
//                 
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to add to one or more items", 
//                             result.getWriteResult().getLastError().getException());
//                 }
//                 return result.getSavedObjects();
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Add one or ore values to the array value at each of the specified fields atomically
//     * @param uuid
//     * @param tenantId
//     * @param additions
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> append(String uuid, String tenantId, Map<String, JsonNode> additions) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (additions == null || additions.isEmpty()) {
//                return new ArrayList<MetadataItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: additions.keySet()) {
//                     if (builder == null) {
//                         if (additions.get(key) instanceof List) {
//                             builder = DBUpdate.pushAll(key, additions.get(key));
//                         } else {
//                             builder = DBUpdate.push(key, additions.get(key));
//                         }
//                     } else {
//                         if (additions.get(key) instanceof List) {
//                             builder = builder.pushAll(key, additions.get(key));
//                         } else {
//                             builder = builder.push(key, additions.get(key));
//                         }
//                     }
//                 }
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to append to one or more items", 
//                             result.getWriteResult().getLastError().getException());
//                 }
//                 return result.getSavedObjects();
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Perform an atomic increment action of a user-defined amount on the given metadata values(s).
//     * @param uuid
//     * @param tenantId
//     * @param increments
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> increment(String uuid, String tenantId, Map<String, Integer> increments) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (increments == null || increments.isEmpty()) {
//               return new ArrayList<MetadataItem>();
//            }
//            else 
//            {
//                Builder builder = null;
//                for (String key: increments.keySet()) {
//                    if (builder == null) {
//                        builder = DBUpdate.inc(key, increments.get(key).intValue());
//                    } else {
//                        builder = builder.inc(key, increments.get(key).intValue());
//                    }
//                }
//                WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                // do we need to check for errors?
//                if (!result.getWriteResult().getLastError().isEmpty()) {
//                    throw new MetadataQueryException("Failed to increment one or more items", 
//                            result.getWriteResult().getLastError().getException());
//                }
//                return result.getSavedObjects();
//            }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }       
//    }
    
    /**
     * Creates a {@link DBObject} representing the appropriate permission check 
     * for {@code username} to establish they have @{link permission) for a 
     * {@link MetadataItem}.
     * 
     * @param username
     * @param permission
     * @return
     */
    protected DBObject createAuthCriteria(String username, PermissionType permission) {
        BasicDBList ownerList = new BasicDBList();
        ownerList.addAll(Arrays.asList(Settings.PUBLIC_USER_USERNAME, Settings.WORLD_USER_USERNAME, username));
        
        BasicDBList aclCriteria = new BasicDBList();
        aclCriteria.add(QueryBuilder.start("username").in(ownerList).get());
        if (permission == ALL) {
            aclCriteria.add(QueryBuilder.start("read").is(true).and("write").is(true).get());
        } 
        else if (permission.canRead() && permission.canWrite()) {
            aclCriteria.add(QueryBuilder.start("read").is(true).and("write").is(true).get());
        } 
        else if (permission.canRead()) {
            aclCriteria.add(QueryBuilder.start("read").is(true).get());
        } 
        else if (permission.canWrite()) {
            aclCriteria.add(QueryBuilder.start("write").is(true).get());
        }
        
        BasicDBList authConditions = new BasicDBList();
        authConditions.add(QueryBuilder.start("owner").in(ownerList).get());
        authConditions.add(QueryBuilder.start("acl").all(aclCriteria).get());
        
        DBObject authCriteria = QueryBuilder.start().all(
                authConditions).get();
        
        return authCriteria;
    }
    
    /**
     * Turns the search criteria supplied by the user in the URL query into a 
     * {@link DBObject} we can pass to the MongoDB driver.
     * 
     * @param searchCriteria
     * @return
     * @throws MetadataQueryException 
     */
    @SuppressWarnings("unchecked")
    protected DBObject parseUserSearchCriteria(Map<SearchTerm, Object> searchCriteria) throws MetadataQueryException {
        DBObject userCriteria = null;
        QueryBuilder queryBuilder = null;
        
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return new BasicDBObject();
        } 
        else {
            for (SearchTerm searchTerm: searchCriteria.keySet()) {
                
                // this is a freeform search query. Support regex then move on. if this exists, it is the only
                // search criteria
                if (searchCriteria.get(searchTerm) instanceof DBObject) {
                    
                    userCriteria = (DBObject)searchCriteria.get(searchTerm);
                    
                    // support regex in the freeform queries
                    for (String key: userCriteria.keySet()) {
                        
                        // TODO: throw exception on unsafe mongo keywords in freeform search
                        
                        // we're just going one layer deep on the regex support. anything else won't work anyway due to 
                        // the lack of freeform query support in the java driver
                        if (userCriteria.get(key) instanceof String) {
                            if (((String) userCriteria.get(key)).contains("*")) {
                                try {
                                    Pattern regexPattern = Pattern.compile((String)userCriteria.get(key), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
                                    userCriteria.put(key, regexPattern);
                                } catch (Exception e) {
                                    throw new MetadataQueryException("Invalid regular expression for " + key + " query", e);
                                }
                            }
                        }
                    }
                }
                // they are using the json.sql notation to search their metadata value
                else { // if (searchTerm.getSearchField().equalsIgnoreCase("value")) {
                    if (queryBuilder == null) {
                        queryBuilder = QueryBuilder.start(searchTerm.getMappedField());
                    } else {
                        queryBuilder.and(searchTerm.getMappedField());
                    }
                    
                    if (searchTerm.getOperator() == SearchTerm.Operator.EQ) {
                        queryBuilder.is(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.NEQ) {
                        queryBuilder.notEquals(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.IN) {
                        queryBuilder.in(Arrays.asList(searchCriteria.get(searchTerm)));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.NIN) {
                        queryBuilder.notIn(Arrays.asList(searchCriteria.get(searchTerm)));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.GT) {
                        queryBuilder.greaterThan(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.GTE) {
                        queryBuilder.greaterThanEquals(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.LT) {
                        queryBuilder.lessThan(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.LTE) {
                        queryBuilder.lessThanEquals(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.LIKE) {
                        try {
                            Pattern regexPattern = Pattern.compile((String)searchCriteria.get(searchTerm), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
                            queryBuilder.regex(regexPattern);
                        } catch (Exception e) {
                            throw new MetadataQueryException("Invalid regular expression for " + searchTerm.getMappedField() + " query", e);
                        }
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.NLIKE) {
                        try {
                            Pattern regexPattern = Pattern.compile((String)searchCriteria.get(searchTerm), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
                            queryBuilder.not().regex(regexPattern);
                        } catch (Exception e) {
                            throw new MetadataQueryException("Invalid regular expression for " + searchTerm.getMappedField() + " query", e);
                        }
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.ON) {
                        queryBuilder.is(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.BEFORE) {
                        
                        queryBuilder.lessThan((Date)searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.AFTER) {
                        queryBuilder.greaterThan((Date)searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN) {
                        List<Date> dateRange = (List<Date>)searchCriteria.get(searchTerm);
                        queryBuilder.greaterThan(dateRange.get(0))
                                    .and(searchTerm.getMappedField())
                                        .lessThan(dateRange.get(1));
                    }
                }
            }
        
            // generate the query if we used the query builder
            if (queryBuilder != null) {
                userCriteria = queryBuilder.get();
            }
            // if there wasn't a freeform search query, we need to init
            else if (userCriteria == null) {
                userCriteria = new BasicDBObject();
            } 
            
            return userCriteria;
        }
    }
}
