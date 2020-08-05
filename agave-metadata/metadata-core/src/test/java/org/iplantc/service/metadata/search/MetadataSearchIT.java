package org.iplantc.service.metadata.search;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import com.mongodb.TaggableReadPreference;
import io.grpc.Metadata;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.exceptions.MetadataAssociationException;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.exceptions.MetadataQueryException;
import org.iplantc.service.metadata.exceptions.MetadataStoreException;
import org.iplantc.service.metadata.model.MetadataAssociationList;
import org.iplantc.service.metadata.model.MetadataItem;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.model.serialization.MetadataItemSerializer;
import org.json.JSONException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Test(groups = {"integration"})
public class MetadataSearchIT {

    String uuid;
    String username = "testuser";
    String sharedUser = "testSharedUser";
    List<String> queryList = new ArrayList<>();
    AgaveUUID jobUuid = new AgaveUUID(UUIDType.JOB);
    AgaveUUID schemaUuid = new AgaveUUID(UUIDType.SCHEMA);

    public List<String> setQueryList(List<String> stringList) {
        if (stringList.isEmpty()) {
            this.queryList.add("  {" +
                    "    \"name\": \"mustard plant\"," +
                    "    \"value\": {" +
                    "      \"type\": \"a plant\"," +
                    "        \"profile\": {" +
                    "        \"status\": \"active\"" +
                    "           }," +
                    "        \"description\": \"The seed of the mustard plant is used as a spice...\"" +
                    "       }," +
                    "       \"associationIds\": [\"" + jobUuid.toString() + "\"]" +
                    "   }");

            this.queryList.add("  {" +
                    "    \"name\": \"cactus (cactaeceae)\"," +
                    "    \"value\": {" +
                    "      \"type\": \"a plant\"," +
                    "      \"order\": \"Caryophyllales\", " +
                    "        \"profile\": {" +
                    "        \"status\": \"inactive\"" +
                    "           }," +
                    "        \"description\": \"It could take a century for a cactus to produce its first arm. /n" +
                    "                           A type of succulent and monocots. .\"" +
                    "       }" +
                    "   }");

            this.queryList.add(
                    "  {" +
                            "    \"name\": \"Agavoideae\"," +
                            "    \"value\": {" +
                            "      \"type\": \"a flowering plant\"," +
                            "      \"order\": \" Asparagales\", " +
                            "        \"profile\": {" +
                            "        \"status\": \"paused\"" +
                            "           }," +
                            "        \"description\": \"Includes desert and dry-zone types such as the agaves and yuucas.\"" +
                            "       }" +
                            "   }");
            this.queryList.add("  {" +
                    "    \"name\": \"wisteria\"," +
                    "    \"value\": {" +
                    "      \"type\": \"a flowering plant\"," +
                    "      \"order\": \" Fabales\", " +
                    "        \"profile\": {" +
                    "        \"status\": \"active\"" +
                    "           }," +
                    "        \"description\": \"native to China, Korea, Japan, and the Eastern United States.\"" +
                    "       }," +
                    "       \"associationIds\": [\"" + jobUuid.toString() + "\", " +
                    "       \"" + schemaUuid.toString() + "\"]" +
                    "   }");
        } else {
            this.queryList.addAll(stringList);
        }
        return this.queryList;
    }

    public MetadataItem createSingleMetadataItem(String query) throws MetadataException, MetadataStoreException, IOException, MetadataQueryException, PermissionException, UUIDException {
        if (StringUtils.isEmpty(query)) {
            query = "{\"value\": {\"title\": \"Example Metadata\", \"properties\": {\"species\": \"arabidopsis\", " +
                    "\"description\": \"A model organism...\"}}, \"name\": \"test metadata\"}\"";
        }
        MetadataSearch search = new MetadataSearch(username);
        MetadataSearch spySearch = Mockito.spy(search);
        spySearch.setAccessibleOwnersExplicit();

        Mockito.doReturn(createResponseString(jobUuid)).when(spySearch).getValidationResponse(jobUuid.toString());
        Mockito.doReturn(createResponseString(schemaUuid)).when(spySearch).getValidationResponse(schemaUuid.toString());

        JsonFactory factory = new ObjectMapper().getFactory();
        JsonNode jsonMetadataNode = factory.createParser(query).readValueAsTree();
        spySearch.parseJsonMetadata(jsonMetadataNode);
        spySearch.setOwner(username);
        uuid = new AgaveUUID(UUIDType.METADATA).toString();
        spySearch.setUuid(uuid);
        spySearch.updateMetadataItem();
        return spySearch.getMetadataItem();
    }

    public List<MetadataItem> createMultipleMetadataItems(List<String> queryList) throws MetadataException, MetadataQueryException, MetadataStoreException, PermissionException, IOException, UUIDException {
        List<MetadataItem> resultList = new ArrayList<>();
        for (String query : queryList) {
            resultList.add(createSingleMetadataItem(query));
        }
        return resultList;
    }

    public String createResponseString(AgaveUUID uuid) throws UUIDException {
        return "  {" +
                "    \"uuid\": \"" + uuid.toString() + "\"," +
                "    \"type\": \"" + uuid.getResourceType().toString() + "\"," +
                "    \"_links\": {" +
                "      \"self\": {" +
                "        \"href\": \"" + TenancyHelper.resolveURLToCurrentTenant(uuid.getObjectReference()) + "\"" +
                "      }" +
                "    }" +
                "  }";
    }

    @Test
    public void createTest() throws IOException, MetadataException, UUIDException, MetadataQueryException, MetadataStoreException, PermissionException {
        JsonFactory factory = new ObjectMapper().getFactory();

        MetadataSearch search = new MetadataSearch(this.username);
        MetadataSearch spySearch = Mockito.spy(search);
        spySearch.clearCollection();
        spySearch.setAccessibleOwnersImplicit();

        AgaveUUID associatedUuid = new AgaveUUID(UUIDType.JOB);
        Mockito.doReturn(createResponseString(associatedUuid)).when(spySearch).getValidationResponse(associatedUuid.toString());

        //create metadata item to insert
        String strJson =
                "  {" +
                        "    \"uuid\": \"" + uuid + "\"," +
                        "    \"schemaId\": null," +
                        "    \"associationIds\": [" +
                        "      \"" + associatedUuid.toString() + "\"" +
                        "    ]," +
                        "    \"name\": \"some metadata\"," +
                        "    \"value\": {" +
                        "      \"title\": \"Example Metadata\"," +
                        "      \"properties\": {" +
                        "        \"species\": \"arabidopsis\"," +
                        "        \"description\": \"A model plant organism...\"" +
                        "      }" +
                        "    }," +
                        "    \"owner\": \"" + username + "\"" +
                        "   }";

        JsonNode jsonMetadataNode = factory.createParser(strJson).readValueAsTree();
        spySearch.parseJsonMetadata(jsonMetadataNode);
        spySearch.setOwner(username);

        //insert metadata item
        MetadataItem insertedMetadataItem = spySearch.updateMetadataItem();

        String userQuery = "{\"name\": \"some metadata\"}";
        List<MetadataItem> findResult = spySearch.find(userQuery);

        Assert.assertTrue(findResult.get(0).equals(insertedMetadataItem));
    }

    @Test
    public void updateExistingItemAsOwner() throws MetadataException, MetadataQueryException, MetadataStoreException, IOException, PermissionException, UUIDException {
        MetadataSearch search = new MetadataSearch(this.username);
        search.clearCollection();
        search.setAccessibleOwnersImplicit();

        MetadataItem metadataItem = createSingleMetadataItem("");

        String strUpdate =
                "  {" +
                        "    \"name\": \"New Metadata\"," +
                        "    \"value\": {" +
                        "      \"title\": \"Changed Metadata Title\"," +
                        "      \"properties\": {" +
                        "        \"species\": \"wisteria\"," +
                        "        \"description\": \"A model flower organism...\"" +
                        "       }" +
                        "       }" +
                        "   }";
        String queryAfterUpdate = "{\"name\": \"New Metadata\", \"value.properties.species\": \"wisteria\"}";
        JsonFactory factory = new ObjectMapper().getFactory();
        JsonNode jsonMetadataNode = factory.createParser(strUpdate).readValueAsTree();
        search.parseJsonMetadata(jsonMetadataNode);
        search.setUuid(metadataItem.getUuid());
        search.setOwner(metadataItem.getOwner());
        MetadataItem insertedMetadataItem = search.updateMetadataItem();

        List<MetadataItem> result = search.find(queryAfterUpdate);
        Assert.assertEquals(result.get(0), insertedMetadataItem);
    }

    @Test
    public void updateExistingItemAsSharedUserWithRead() throws IOException, MetadataStoreException, MetadataException, MetadataQueryException, PermissionException, UUIDException {
        MetadataSearch searchAsOwner = new MetadataSearch(username);
        searchAsOwner.clearCollection();
        searchAsOwner.setAccessibleOwnersExplicit();
        MetadataItem metadataItem = createSingleMetadataItem("");
        searchAsOwner.updatePermissions(sharedUser, "", PermissionType.READ);

        MetadataSearch search = new MetadataSearch(sharedUser);
        search.setAccessibleOwnersExplicit();

        String strUpdate =
                "  {" +
                        "    \"name\": \"New Metadata\"," +
                        "    \"value\": {" +
                        "      \"title\": \"Changed Metadata Title\"," +
                        "      \"properties\": {" +
                        "        \"species\": \"wisteria\"," +
                        "        \"description\": \"A model flower organism...\"" +
                        "       }" +
                        "       }" +
                        "   }";

        JsonFactory factory = new ObjectMapper().getFactory();
        JsonNode jsonMetadataNode = factory.createParser(strUpdate).readValueAsTree();
        search.parseJsonMetadata(jsonMetadataNode);
        search.setUuid(metadataItem.getUuid());

        try {
            MetadataItem updatedMetadataItem = search.updateMetadataItem();
        } catch (PermissionException p) {
            Assert.assertEquals(p.getMessage(), "User does not have sufficient access to edit public metadata item.");
        }
    }

    @Test
    public void updateExistingItemAsSharedUserWithReadWrite() throws IOException, MetadataStoreException, MetadataException, MetadataQueryException, PermissionException, UUIDException {
        MetadataSearch searchAsOwner = new MetadataSearch(username);
        searchAsOwner.clearCollection();
        searchAsOwner.setAccessibleOwnersImplicit();

        MetadataItem metadataItem = createSingleMetadataItem("");
        searchAsOwner.setMetadataItem(metadataItem);
        searchAsOwner.updatePermissions(sharedUser, "", PermissionType.READ_WRITE);

        MetadataSearch search = new MetadataSearch( sharedUser);
        search.setAccessibleOwnersImplicit();

        search.setMetadataItem(metadataItem);
        String strUpdate =
                "  {" +
                        "    \"name\": \"New Metadata\"," +
                        "    \"value\": {" +
                        "      \"title\": \"Changed Metadata Title\"," +
                        "      \"properties\": {" +
                        "        \"species\": \"wisteria\"," +
                        "        \"description\": \"A model flower organism...\"" +
                        "       }" +
                        "       }" +
                        "   }";

        JsonFactory factory = new ObjectMapper().getFactory();
        JsonNode jsonMetadataNode = factory.createParser(strUpdate).readValueAsTree();
        search.parseJsonMetadata(jsonMetadataNode);
        search.setUuid(metadataItem.getUuid());
        search.setOwner(username);

        List<MetadataItem> allResult = search.findAll();
        MetadataItem updatedMetadataItem = search.updateMetadataItem();

        String queryAfterUpdate = "{\"name\": \"New Metadata\", \"value.title\": \"Changed Metadata Title\"}";
        queryAfterUpdate = "{\"name\": \"New Metadata\"}";
        List<MetadataItem> result = search.find(queryAfterUpdate);
        allResult = search.findAll();

        Assert.assertEquals(result.get(0), updatedMetadataItem);
    }

    public void updateExistingItemAsSharedUserWithNoPermission() throws IOException, MetadataStoreException, MetadataException, MetadataQueryException, PermissionException, UUIDException {
        MetadataSearch searchAsOwner = new MetadataSearch(username);
        searchAsOwner.clearCollection();
        searchAsOwner.setAccessibleOwnersImplicit();

        MetadataItem metadataItem = createSingleMetadataItem("");

        MetadataSearch search = new MetadataSearch(sharedUser);
        search.setAccessibleOwnersImplicit();

        String strUpdate =
                "  {" +
                        "    \"name\": \"New Metadata\"," +
                        "    \"value\": {" +
                        "      \"title\": \"Changed Metadata Title\"," +
                        "      \"properties\": {" +
                        "        \"species\": \"wisteria\"," +
                        "        \"description\": \"A model flower organism...\"" +
                        "       }" +
                        "       }" +
                        "   }";

        JsonFactory factory = new ObjectMapper().getFactory();
        JsonNode jsonMetadataNode = factory.createParser(strUpdate).readValueAsTree();
        search.parseJsonMetadata(jsonMetadataNode);
        search.setUuid(metadataItem.getUuid());

        try {
            MetadataItem updatedMetadataItem = search.updateMetadataItem();
        } catch (PermissionException p) {
            Assert.assertEquals(p.getMessage(), "User does not have sufficient access to edit public metadata item.");
        }
    }

    @Mock
    AuthorizationHelper mockAuthorizationHelper;

    @InjectMocks
    MetadataSearch mockSearch;

    @Test
    public void findAllMetadataForUserImplicitSearchTest() throws MetadataException, MetadataQueryException, MetadataStoreException, PermissionException, IOException, UUIDException {
        MetadataSearch implicitSearch = new MetadataSearch(this.username);

        MetadataSearch spyImplicitSearch = Mockito.spy(implicitSearch);

        spyImplicitSearch.clearCollection();
        spyImplicitSearch.setAccessibleOwnersImplicit();

        if (this.queryList.isEmpty())
            setQueryList(new ArrayList<String>());

        createMultipleMetadataItems(this.queryList);

        String userQuery = "";
        List<MetadataItem> searchResult;

        searchResult = spyImplicitSearch.find(userQuery);
        Assert.assertEquals(searchResult.size(), 4, "Implicit Search should find 4 metadata items ");
    }

    @Test
    public void findAllMetadataForUserExplicitSearchTest() throws MetadataException, MetadataQueryException, MetadataStoreException, PermissionException, IOException, UUIDException {
        MetadataSearch explicitSearch = new MetadataSearch(this.sharedUser);

        MetadataSearch spyExplicitSearch = Mockito.spy(explicitSearch);
        spyExplicitSearch.clearCollection();
        spyExplicitSearch.setAccessibleOwnersExplicit();

        if (this.queryList.isEmpty())
            setQueryList(new ArrayList<String>());

        createMultipleMetadataItems(this.queryList);

        String userQuery = "";
        List<MetadataItem> searchResult;

        searchResult = spyExplicitSearch.find(userQuery);
        Assert.assertEquals(searchResult.size(), 0, "Search should find 0 metadata items because user has not been given explicit permissions for the items.");

    }


    @Test
    public void regexSearchTest() throws IOException, MetadataException, MetadataQueryException, MetadataStoreException, PermissionException, UUIDException {
        MetadataSearch search = new MetadataSearch( username);
        search.setAccessibleOwnersImplicit();

        search.clearCollection();

        if (this.queryList.isEmpty())
            setQueryList(new ArrayList<String>());
        createMultipleMetadataItems(this.queryList);

        String queryByValueRegex = "{ \"value.description\": { \"$regex\": \".*monocots.*\", \"$options\": \"m\"}}";
        List<MetadataItem> resultList;
        resultList = search.find(queryByValueRegex);

        Assert.assertEquals(resultList.size(), 1, "There should be 1 metadata item found: cactus");
    }

    @Test
    public void nameSearchTest() throws MetadataQueryException, MetadataException, MetadataStoreException, UUIDException, PermissionException, IOException {
        MetadataSearch search = new MetadataSearch(username);
        search.clearCollection();
        search.setAccessibleOwnersImplicit();


        if (this.queryList.isEmpty())
            setQueryList(new ArrayList<String>());
        createMultipleMetadataItems(this.queryList);

        String queryByName = "{\"name\":\"mustard plant\"}";
        List<MetadataItem> resultList;
        resultList = search.find(queryByName);

        Assert.assertEquals(resultList.size(), 1, "There should be 1 metadata item found: mustard plant");
    }

    @Test
    public void nestedValueSearchTest() throws MetadataQueryException, MetadataException, MetadataStoreException, UUIDException, PermissionException, IOException {
        MetadataSearch search = new MetadataSearch(username);
        search.clearCollection();
        search.setAccessibleOwnersImplicit();


        if (this.queryList.isEmpty())
            setQueryList(new ArrayList<String>());
        createMultipleMetadataItems(this.queryList);

        String queryByValue = "{\"value.type\":\"a plant\"}";
        List<MetadataItem> resultList;
        resultList = search.find(queryByValue);

        Assert.assertEquals(resultList.size(), 2, "There should be 2 metadata items found: mustard plant and cactus");
    }

    @Test
    public void conditionalSearchTest() throws MetadataQueryException, MetadataException, MetadataStoreException, UUIDException, PermissionException, IOException {
        MetadataSearch search = new MetadataSearch( username);
        search.clearCollection();
        search.setAccessibleOwnersImplicit();


        if (this.queryList.isEmpty())
            setQueryList(new ArrayList<String>());
        createMultipleMetadataItems(this.queryList);

        String queryByValueConditional = "{" +
                "   \"$or\":[" +
                "      {" +
                "         \"value.description\":{" +
                "            \"$regex\": " +
                "               \".*century.*\"" +
                "            \"$options\":\"i\"" +
                "         }" +
                "      }," +
                "      {" +
                "         \"value.type\":{" +
                "            \"$regex\":\".*plant.*\"" +
                "         }," +
                "         \"value.order\":{" +
                "            \"$regex\":\"Asparagales\"" +
                "         }" +
                "      }" +
                "   ]" +
                "}";

        List<MetadataItem> resultList;
        resultList = search.find(queryByValueConditional);

        Assert.assertEquals(resultList.size(), 2, "There should be 2 metadata items found: cactus and Agavoideae");
    }


    @Test
    public void findPermissionForUuidTest() throws MetadataException, MetadataQueryException, MetadataStoreException, PermissionException, IOException, UUIDException {
        MetadataSearch search = new MetadataSearch( this.username);
        search.clearCollection();
        search.setAccessibleOwnersExplicit();


        String metadataQueryAgavoideae =
                "  {" +
                        "    \"name\": \"Agavoideae\"," +
                        "    \"value\": {" +
                        "      \"type\": \"a flowering plant\"," +
                        "      \"order\": \" Asparagales\", " +
                        "      \"properties\": {" +
                        "        \"profile\": {" +
                        "        \"status\": \"paused\"" +
                        "           }," +
                        "        \"description\": \"Includes desert and dry-zone types such as the agaves and yuucas.\"" +
                        "       }" +
                        "       }" +
                        "   }";

        MetadataItem metadataItem = createSingleMetadataItem(metadataQueryAgavoideae);
        search.setUuid(metadataItem.getUuid());

        List<MetadataItem> resultAll = search.findAll();
        Assert.assertEquals(resultAll.size(), 1);

        List<MetadataItem> result = search.findPermission_User(username, metadataItem.getUuid());
        Assert.assertEquals(result.size(), 1, "There is only 1 metadataitem with the specified permission");

        result = search.findPermission_User(sharedUser, metadataItem.getUuid());
        Assert.assertEquals(result.size(), 0, "The user doesn't have any permissions specified for the uuid");

        //adding permissions
        search.updatePermissions(sharedUser, "", PermissionType.READ);
        result = search.findPermission_User(sharedUser, metadataItem.getUuid());
        Assert.assertEquals(result.size(), 1, "The user has specified permission for the uuid");

    }


    @Test
    public void validateUuid() throws MetadataException, MetadataQueryException, MetadataStoreException, PermissionException, IOException, UUIDException {
        MetadataSearch search = new MetadataSearch(this.username);
        MetadataSearch spySearch = Mockito.spy(search);
        spySearch.setAccessibleOwnersImplicit();


        Mockito.doReturn(createResponseString(jobUuid)).when(spySearch).getValidationResponse(jobUuid.toString());
        Mockito.doReturn(createResponseString(schemaUuid)).when(spySearch).getValidationResponse(schemaUuid.toString());

        String invalidUuid = new AgaveUUID(UUIDType.FILE).toString();
        Mockito.doReturn(null).when(spySearch).getValidationResponse(invalidUuid);

        spySearch.clearCollection();
        setQueryList(new ArrayList<>());

        for (String query : this.queryList) {
            uuid = new AgaveUUID(UUIDType.METADATA).toString();
            JsonFactory factory = new ObjectMapper().getFactory();
            JsonNode jsonMetadataNode = factory.createParser(query).readValueAsTree();
            spySearch.parseJsonMetadata(jsonMetadataNode);
            spySearch.setOwner(username);
            spySearch.setUuid(uuid);
            spySearch.updateMetadataItem();
        }

        List<MetadataItem> metadataItemList = spySearch.find("{\"name\": \"mustard plant\"}");
        Assert.assertTrue(metadataItemList.get(0).getAssociations().getAssociatedIds().containsKey(jobUuid.toString()));

        metadataItemList = spySearch.find("{\"name\": \"wisteria\"}");
        Assert.assertTrue(metadataItemList.get(0).getAssociations().getAssociatedIds().containsKey(schemaUuid.toString()));
    }

    @Test
    public void StringToMetadataItemTest() throws IOException, UUIDException, PermissionException, MetadataAssociationException, MetadataException, MetadataStoreException, MetadataQueryException {
        String metadataQueryAloe =
                "  {" +
                        "    \"name\": \"Aloe\"," +
                        "    \"value\": {" +
                        "      \"type\": \"a plant\"" +
                        "}" +
                        "   }";


        MetadataSearch search = new MetadataSearch( this.username);
        search.setAccessibleOwnersExplicit();

        MetadataItem aloeMetadataItem = createSingleMetadataItem(metadataQueryAloe);
        String metadataQueryAgavoideae =
                "  {" +
                        "    \"name\": \"Agavoideae\"," +
                        "    \"value\": {" +
                        "      \"type\": \"a flowering plant\"," +
                        "      \"order\": \" Asparagales\", " +
                        "      \"properties\": {" +
                        "        \"profile\": {" +
                        "        \"status\": \"paused\"" +
                        "           }," +
                        "        \"description\": \"Includes desert and dry-zone types such as the agaves and yuucas.\"" +
                        "       }" +
                        "       }," +
                        "       \"associationIds\": [" +
                        "        \"" + aloeMetadataItem.getUuid() + "\"]" +
                        "   }";

        ObjectMapper mapper = new ObjectMapper();
        MetadataItem bean = mapper.readValue(metadataQueryAgavoideae, MetadataItem.class);

        MetadataAssociationList associationList = bean.getAssociations();
        associationList.add(aloeMetadataItem.getUuid());
        bean.setAssociations(associationList);

        try {
            MetadataItemSerializer metadataItemSerializer = new MetadataItemSerializer(bean);
//            System.out.println(metadataItemSerializer.formatMetadataItemResult().toString());
        } catch (Exception e) {
            Assert.fail("Serializing MetadataItem to Json String should not throw exception");
        }
    }

}
