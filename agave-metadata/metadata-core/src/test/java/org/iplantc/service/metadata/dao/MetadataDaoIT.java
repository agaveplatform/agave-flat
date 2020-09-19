package org.iplantc.service.metadata.dao;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.exceptions.MetadataStoreException;
import org.iplantc.service.metadata.managers.MetadataPermissionManagerIT;
import org.iplantc.service.metadata.model.MetadataItem;
import org.iplantc.service.metadata.model.MetadataItemCodec;
import org.iplantc.service.metadata.model.MetadataPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.testng.Assert.*;

@Test(groups={"integration"})
public class MetadataDaoIT extends AbstractMetadataDaoIT {
    private final String TEST_USER = "testuser";
    private final String TEST_SHARED_USER = "testshareuser";
    private final String TEST_SHARED_USER2 = "testshareuser2";

    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private MongoClient mockClient;

    @Mock
    private MongoDatabase mockDB;

    @Mock
    private MongoCollection mockCollection;

    @InjectMocks
    private MetadataDao wrapper;

    @AfterMethod
    public void cleanUp(){
        ClassModel<JsonNode> valueModel = ClassModel.builder(JsonNode.class).build();
        ClassModel<MetadataPermission> metadataPermissionModel = ClassModel.builder(MetadataPermission.class).build();
        PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().register(valueModel, metadataPermissionModel).build();

        CodecRegistry registry = CodecRegistries.fromCodecs(new MetadataItemCodec());

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(pojoCodecProvider),
                registry);

        MongoClient mongo4Client = MongoClients.create(MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(
                        new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT))))
                .credential(MongoCredential.createScramSha1Credential(
                        Settings.METADATA_DB_USER, Settings.METADATA_DB_SCHEME, Settings.METADATA_DB_PWD.toCharArray()))
                .codecRegistry(pojoCodecRegistry)
                .build());

        MongoDatabase db = mongo4Client.getDatabase(Settings.METADATA_DB_SCHEME);
        MongoCollection collection = db.getCollection(Settings.METADATA_DB_COLLECTION, MetadataItem.class);

        collection.deleteMany(new Document());
    }

    /**
     * Create a test entity persisted and available for lookup.
     *
     * @return a persisted instance of the entity
     */
    @Override
    public MetadataItem createEntity() {
        MetadataItem entity = null;
        try {
            entity = new MetadataItem();
            entity.setName(MetadataPermissionManagerIT.class.getName());
            entity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
            entity.setOwner(TEST_USER);
            entity.getPermissions().add(new MetadataPermission(TEST_SHARED_USER, PermissionType.READ));

//            wrapper.insert(entity);
            //MetadataDao.getInstance().insert(entity);
        } catch (Exception e) {
            Assert.fail("Unable to create metadata item", e);
        }

        return entity;
    }

    @Override
    public void insertTest() throws MetadataException, PermissionException, MetadataStoreException {
        //Create item to insert
        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);
        MetadataPermission metaPem = new MetadataPermission(TEST_SHARED_USER, PermissionType.ALL);
        testEntity.setPermissions(new ArrayList<>(Arrays.asList(metaPem)));

        MetadataDao inst = wrapper.getInstance();

        //clean collection
        inst.clearCollection();

        List<String> accessibleOwners = new ArrayList<>();
        accessibleOwners.add(TEST_USER);
        inst.setAccessibleOwners(accessibleOwners);
        MetadataItem updatedItem = inst.insert(testEntity);

        List<MetadataItem>  firstResult = inst.find(TEST_USER, new Document("uuid", updatedItem.getUuid()));
        assertEquals(firstResult.get(0).getOwner(), TEST_USER);
        assertEquals(firstResult.get(0).getName(),MetadataDaoIT.class.getName());
        assertEquals(firstResult.get(0).getValue().get("testKey"), testEntity.getValue().get("testKey"));
        assertEquals(firstResult.get(0).getPermissions().size(), 1);
        assertEquals(firstResult.get(0).getPermissionForUsername(TEST_SHARED_USER).getPermission(), PermissionType.ALL);
        assertEquals(firstResult.get(0), updatedItem, "Added Metadata item should be found in the collection.");
    }

    @Override
    public void insertPermissionTest() throws MetadataStoreException, MetadataException, PermissionException {
        MetadataDao inst = wrapper.getInstance();
        inst.clearCollection();

        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);

        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        MetadataItem addedItem = inst.insert(testEntity);

        MetadataPermission pemShareUser = new MetadataPermission(TEST_SHARED_USER, PermissionType.ALL);
        MetadataPermission pemShareUser2 = new MetadataPermission(TEST_SHARED_USER2, PermissionType.READ_WRITE);
        List<MetadataPermission> addList = new ArrayList<>(Arrays.asList(pemShareUser, pemShareUser2));
        addedItem.setPermissions(addList);
        List<MetadataPermission> updatedPermissions = inst.updatePermission(addedItem);

        Assert.assertNotNull(updatedPermissions, "updatePermission should return the successfully updated permissions list.");

        List<MetadataItem> updatedItem = inst.find(TEST_USER, new Document("uuid", addedItem.getUuid()));
        assertEquals(updatedItem.get(0).getPermissions().size(), 2, "There should be 2 permissions added.");
        assertEquals(updatedItem.get(0).getPermissionForUsername(TEST_SHARED_USER).getPermission(), PermissionType.ALL,
                "Permission added for " + TEST_SHARED_USER + " should be ALL.");
        assertEquals(updatedItem.get(0).getPermissionForUsername(TEST_SHARED_USER2).getPermission(), PermissionType.READ_WRITE,
                "Permission added for " + TEST_SHARED_USER2 + " should be READ_WRITE.");
    }

    @Override
    public void removePermissionTest() throws MetadataStoreException, MetadataException, PermissionException {
        MetadataDao inst = wrapper.getInstance();
        inst.clearCollection();

        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);

        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        MetadataItem addedItem = inst.insert(testEntity);
        MetadataPermission metaPem = new MetadataPermission(TEST_SHARED_USER, PermissionType.READ);
        testEntity.setPermissions(new ArrayList<>(Arrays.asList(metaPem)));

        MetadataPermission sharedUserPermission = testEntity.getPermissionForUsername(TEST_SHARED_USER);

        testEntity.removePermission(sharedUserPermission);

//        List<MetadataPermission> updatedPermissions = inst.updatePermission(testEntity);

        List<MetadataItem> resultList = inst.find(TEST_USER, new Document("uuid", testEntity.getUuid()));

        Assert.assertNull(resultList.get(0).getPermissionForUsername(TEST_SHARED_USER), "Removed permission should return null.");
    }

    @Test
    public void deleteMetadataTest() throws PermissionException, MetadataStoreException {
        MetadataItem toAdd = createEntity();
        MetadataDao dao = new MetadataDao().getInstance();
        MetadataItem addedItem =  dao.insert(toAdd); ;

        MetadataItem removedItem = dao.deleteMetadata(addedItem);
        Assert.assertNotNull(removedItem, "Removed item should be returned after successful delete.");
        Assert.assertEquals(removedItem.getUuid(), addedItem.getUuid(), "Deleted item uuid should match.");

        List<MetadataItem> resultList  = dao.find(TEST_USER, new Document("uuid", addedItem.getUuid()));
        assertEquals(resultList.size(), 0, "Searching for removed metadata item by its uuid should return an empty list.");
    }

    @Test
    public void deleteMissingMetadataTest() throws PermissionException {
        MetadataItem toAdd = createEntity();
        MetadataDao dao = new MetadataDao().getInstance();

        MetadataItem removedItem = dao.deleteMetadata(toAdd);
        Assert.assertNull(removedItem, "Null should be returned if no matching MetadataItem was found");
    }

    @Override
    public void updateTest() throws MetadataException, MetadataStoreException, PermissionException {
        //add entity without any permissions
        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);
        List<MetadataPermission> listPem = new ArrayList<>();
        testEntity.setPermissions(listPem);

        MetadataDao inst = wrapper.getInstance();

        Document docQuery = new Document("uuid", testEntity.getUuid())
                .append("tenantId", testEntity.getTenantId());

        Bson docFilter = inst.createQueryFromMetadataItem(testEntity);

        //clean collection
        inst.clearCollection();
        assertEquals(inst.getCollectionSize(), 0);

        //insert metadataItem
        List<String> accessibleOwners = new ArrayList<>();
        inst.setAccessibleOwners(accessibleOwners);
        inst.getMongoClients();
        inst.insert(testEntity);

        //check it was added
        if (inst.hasRead(TEST_SHARED_USER, testEntity.getUuid())) {
            List<MetadataItem>  firstResult = inst.find(TEST_SHARED_USER, docFilter);
            Assert.assertNull(firstResult, "Item should not be found because no permissions were set for the user yet.");
        }
        assertEquals(inst.getCollectionSize(), 1);

        //add permission for test share user with read
        MetadataPermission sharedUserPermission = new MetadataPermission(TEST_SHARED_USER, PermissionType.READ);
        List<MetadataPermission> metadataPermissionList = testEntity.getPermissions();
        metadataPermissionList.add(sharedUserPermission);
        testEntity.setPermissions(metadataPermissionList);
        List<MetadataPermission> updatePem = inst.updatePermission(testEntity);

        //check permission updated
        assertEquals(inst.getCollectionSize(), 1, "Updating permission should not change collection size.");
        List<MetadataItem>  updatePemResult = inst.find(TEST_SHARED_USER, docFilter);

//        Assert.assertNotNull(updatePemResult, "User permission updated should not be null");
        List<MetadataItem> testResult = inst.find(TEST_USER, docFilter);
        List<MetadataItem>  newResult = inst.find(TEST_USER, new Document());
        List<MetadataItem> resultList = inst.findAll();

        Assert.assertNotNull(updatePemResult, "Item should be found after adding");
        assertEquals(updatePemResult.get(0).getPermissionForUsername(TEST_SHARED_USER).getPermission(), PermissionType.READ,
                "Permission for user should be READ after updating.");

        //change metadata value
        testEntity.setValue(mapper.createObjectNode().put("newKey", "newValue"));

        // whi is this optional?
        MetadataItem updateResultItem = null;
        if (inst.hasWrite(TEST_SHARED_USER, testEntity.getUuid())) {
            updateResultItem = inst.updateMetadata(testEntity, TEST_SHARED_USER);

            assertEquals(updateResultItem.getUuid(), testEntity.getUuid(),"UUID should be present in response from update");
        }

        //metadata should not be updated
        Assert.assertNull(updateResultItem, "User does not have correct permissions, metataItem should not be updated.");

        //update permission to read_write
        sharedUserPermission = testEntity.getPermissionForUsername(TEST_SHARED_USER);
        sharedUserPermission.setPermission(PermissionType.READ_WRITE);
        testEntity.updatePermissions(sharedUserPermission);

        List<MetadataPermission> metadataPermission = inst.updatePermission(testEntity);

        Assert.assertNotNull(metadataPermission, "Permission should be updated");
        Assert.assertTrue(metadataPermission.size() > 0, "Permission should be updated");

        if (inst.hasWrite(TEST_SHARED_USER, testEntity.getUuid())){
            updateResultItem = inst.updateMetadata(testEntity, TEST_SHARED_USER);
        }

        Assert.assertNotNull(updateResultItem, "User has correct permissions, metadataItem should be updated.");

        //metadata value should be updated
        List<MetadataItem> updateResult = inst.find(TEST_SHARED_USER, docQuery);
        assertEquals(updateResult.get(0).getValue(), testEntity.getValue());
    }

    @Override
    public void updatePermissionTest() throws MetadataStoreException, MetadataException, PermissionException {
        MetadataDao inst = wrapper.getInstance();
        inst.clearCollection();

        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);
        MetadataPermission pemShareUser = new MetadataPermission(TEST_SHARED_USER, PermissionType.ALL);
        List<MetadataPermission> listPem = new ArrayList<>(Arrays.asList(pemShareUser));
        testEntity.setPermissions(listPem);

        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        MetadataItem addedItem = inst.insert(testEntity);

        pemShareUser.setPermission(PermissionType.READ);
        MetadataPermission pemShareUser2 = new MetadataPermission(TEST_SHARED_USER2, PermissionType.READ_WRITE);
        List<MetadataPermission> updatedList = new ArrayList<>(Arrays.asList(pemShareUser, pemShareUser2));
        addedItem.setPermissions(updatedList);
        List<MetadataPermission> updatedPermissions = inst.updatePermission(addedItem);

        Assert.assertNotNull(updatedPermissions, "updatePermission should return the successfully updated permissions list.");

        List<MetadataItem> updatedItem = inst.find(TEST_USER, new Document("uuid", addedItem.getUuid()));
        assertEquals(updatedItem.get(0).getPermissions().size(), 2, "There should be 2 permissions added.");
        assertEquals(updatedItem.get(0).getPermissionForUsername(TEST_SHARED_USER).getPermission(), PermissionType.READ,
                "Permission for " + TEST_SHARED_USER + " should be updated to READ.");
        assertEquals(updatedItem.get(0).getPermissionForUsername(TEST_SHARED_USER2).getPermission(), PermissionType.READ_WRITE,
                "Permission for " + TEST_SHARED_USER2 + " should be added as READ_WRITE.");
    }

    @Override
    public void findTest() throws MetadataException, PermissionException, MetadataStoreException {
        MetadataDao inst = wrapper.getInstance();
        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);

        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        inst.insert(testEntity);

        List<MetadataItem> foundItem = inst.find(TEST_USER, new Document("value.testKey", "testValue"));
        assertEquals(foundItem.get(0), testEntity, "MetadataItem found should match the created entity.");
    }

    @Override
    public void findWithOffsetAndLimitTest() throws MetadataException, PermissionException, MetadataStoreException {
        MetadataDao inst = wrapper.getInstance();

        int offset = 2;
        int limit = 3;

        for (int numItems = 0; numItems < 5; numItems++){
            MetadataItem testEntity = new MetadataItem();
            testEntity.setName(MetadataDaoIT.class.getName() + numItems);
            testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
            testEntity.setOwner(TEST_USER);

            inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
            inst.insert(testEntity);

        }

        List<MetadataItem> foundItems = inst.find(TEST_USER, new Document("value.testKey", "testValue"), offset, limit, new BasicDBObject());

        assertEquals(foundItems.size(), 3);
        for (int numFound = 0; numFound < foundItems.size(); numFound ++){
            assertEquals(foundItems.get(numFound).getName(), MetadataDaoIT.class.getName() + (numFound + offset));
        }
    }

    @Override
    public void findSingleMetadataItemTest() throws MetadataException, PermissionException, MetadataStoreException {
        MetadataDao inst = wrapper.getInstance();
        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);


        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        inst.insert(testEntity);

        MetadataItem foundItem = inst.findSingleMetadataItem(new Document("uuid", testEntity.getUuid()));
        assertEquals(foundItem, testEntity, "MetadataItem found should match the created entity.");
    }

    @Override
    public void findSingleMetadataItemNonexistentTest(){
        String invalidUuid = new AgaveUUID(UUIDType.METADATA).toString();

        MetadataDao inst = wrapper.getInstance();
        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        MetadataItem foundItem = inst.findSingleMetadataItem(new Document("uuid", invalidUuid));
        Assert.assertNull(foundItem, "No item should be found for an item that doesn't exist");
    }

    @Override
    public void findPermissionTest() throws MetadataException, PermissionException, MetadataStoreException {
        MetadataDao inst = wrapper.getInstance();
        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(mapper.createObjectNode().put("testKey", "testValue"));
        testEntity.setOwner(TEST_USER);
        MetadataPermission pemShareUser = new MetadataPermission(TEST_SHARED_USER, PermissionType.READ);
        List<MetadataPermission> listPem = new ArrayList<>(Arrays.asList(pemShareUser));
        testEntity.setPermissions(listPem);

        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        inst.insert(testEntity);
        MetadataItem foundItem = inst.findSingleMetadataItem(new Document("uuid", testEntity.getUuid()));
        assertEquals(foundItem.getPermissionForUsername(TEST_SHARED_USER).getPermission(), PermissionType.READ);
    }

    @Override
    public void findMetadataItemWithFiltersTest() throws MetadataException, PermissionException, MetadataStoreException {
        ObjectNode value = mapper.createObjectNode().put("testKey", "testValue");

        MetadataDao inst = wrapper.getInstance();
        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(value);
        testEntity.setOwner(TEST_USER);
        MetadataPermission pemShareUser = new MetadataPermission(TEST_SHARED_USER, PermissionType.READ);
        List<MetadataPermission> listPem = new ArrayList<>(Arrays.asList(pemShareUser));
        testEntity.setPermissions(listPem);
        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        inst.insert(testEntity);

        Document docFilter = new Document("uuid", 1)
                .append("name",1)
                .append("value", 1);

        List<Document> foundItems = inst.filterFind(new Document("uuid", testEntity.getUuid()), docFilter);

        assertEquals(foundItems.get(0).get("uuid"), testEntity.getUuid(), "Document found should include the filtered field 'uuid'.");
        assertEquals(foundItems.get(0).get("name"), MetadataDaoIT.class.getName(), "Document found should include the filtered field 'name'.");
        assertEquals(foundItems.get(0).getEmbedded(List.of("value", "testKey"), String.class), "testValue", "Document found should include the filtered field 'value'.");
        Assert.assertNull(foundItems.get(0).get("permissions"), "Items not included in the filter should return null.");
    }

    @Override
    public void findMetadataItemWithInvalidFiltersTest() throws MetadataException, PermissionException, MetadataStoreException {
        ObjectNode value = mapper.createObjectNode().put("testKey", "testValue");

        MetadataDao inst = wrapper.getInstance();
        MetadataItem testEntity = new MetadataItem();
        testEntity.setName(MetadataDaoIT.class.getName());
        testEntity.setValue(value);
        testEntity.setOwner(TEST_USER);
        MetadataPermission pemShareUser = new MetadataPermission(TEST_SHARED_USER, PermissionType.READ);
        List<MetadataPermission> listPem = new ArrayList<>(Arrays.asList(pemShareUser));
        testEntity.setPermissions(listPem);
        inst.setAccessibleOwners(new ArrayList<>(Arrays.asList(TEST_USER)));
        inst.insert(testEntity);

        Document docFilter = new Document("uuid", 1)
                .append("name",1)
                .append("value", 1)
                .append("invalidField", 1);

        List<Document> foundItems = inst.filterFind(new Document("uuid", testEntity.getUuid()), docFilter);

        Assert.assertNull(foundItems.get(0).get("invalidField"), "Invalid/missing fields should not be included in the result.");
    }

    @Test
    public void checkHasReadQueryTest() throws MetadataStoreException {
        MetadataItem toAdd = createEntity();
        MetadataDao dao = new MetadataDao().getInstance();
        MetadataItem addedItem = dao.insert(toAdd);
        assertTrue(dao.hasRead(TEST_SHARED_USER, addedItem.getUuid()), "User with READ permission should be able to read Metadata Item.");
        assertTrue(dao.hasRead(TEST_USER, addedItem.getUuid()), "Owner should be able to read Metadata Item.");
        assertFalse(dao.hasRead(TEST_SHARED_USER2, addedItem.getUuid()), "User without permission set should not be able to read Metadata Item.");
    }

    @Test
    public void checkHasWriteQueryTest() throws MetadataStoreException, MetadataException {
        MetadataItem toAdd = createEntity();
        toAdd.getPermissions().add(new MetadataPermission(TEST_SHARED_USER2, PermissionType.READ_WRITE));

        MetadataDao dao = new MetadataDao().getInstance();
        MetadataItem addedItem = dao.insert(toAdd);
        assertTrue(dao.hasWrite(TEST_SHARED_USER2, addedItem.getUuid()), "User with READ_WRITE permission should be able to  write to  Metadata Item.");
        assertTrue(dao.hasWrite(TEST_USER, addedItem.getUuid()), "Owner should be able to write to Metadata Item.");
        assertFalse(dao.hasWrite(TEST_SHARED_USER, addedItem.getUuid()), "User with READ permission should not be able to  write to  Metadata Item.");
        assertFalse(dao.hasWrite("INVALID_USER", addedItem.getUuid()), "User without permission set should not be able to write to Metadata Item.");
    }

}
