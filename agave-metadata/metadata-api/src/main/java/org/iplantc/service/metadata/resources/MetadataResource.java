/**
 *
 */
package org.iplantc.service.metadata.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataDao;
import org.iplantc.service.metadata.model.MetadataItem;
import org.iplantc.service.metadata.model.serialization.MetadataItemSerializer;
import org.iplantc.service.metadata.search.JsonHandler;
import org.iplantc.service.metadata.search.MetadataSearch;
import org.iplantc.service.notification.managers.NotificationManager;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.*;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.METADATA02;

/**
 * Class to handle CRUD operations on metadata entities.
 *
 * @author dooley
 *
 */
@SuppressWarnings("deprecation")
public class MetadataResource extends AgaveResource {
    private static final Logger log = Logger.getLogger(MetadataResource.class);

    private String username;
    private String internalUsername;
    private String uuid;
    private String userQuery;

    private MongoClient mongoClient;
    private DB db;

    /**
     * @param context
     * @param request
     * @param response
     */
    public MetadataResource(Context context, Request request, Response response) {
        super(context, request, response);

        this.username = getAuthenticatedUsername();

        uuid = (String) request.getAttributes().get("uuid");

        Form form = request.getOriginalRef().getQueryAsForm();
        if (form != null) {
            userQuery = (String) form.getFirstValue("q");

            if (!StringUtils.isEmpty(userQuery)) {
                try {
                    userQuery = URLDecoder.decode(userQuery, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.error("Invalid URL encoding in URL. Apparently.", e);
                    response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                    response.setEntity(new IplantErrorRepresentation("Invalid URL-encoded Character(s)."));
                }
            }
        }

        internalUsername = (String) context.getAttributes().get("internalUsername");

        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        // Set up MongoDB connection
//        try {
//            mongoClient = ((MetadataApplication) getApplication()).getMongoClient();
//            db = mongoClient.getDB(Settings.METADATA_DB_SCHEME);
//            // Gets a collection, if it does not exist creates it
//            collection = db.getCollection(Settings.METADATA_DB_COLLECTION);
//            schemaCollection = db.getCollection(Settings.METADATA_DB_SCHEMATA_COLLECTION);
//        } catch (Exception e) {
//            log.error("Unable to connect to metadata store", e);
////        	try { mongoClient.close(); } catch (Exception e1) {}
//            response.setStatus(Status.SERVER_ERROR_INTERNAL);
//            response.setEntity(new IplantErrorRepresentation("Unable to connect to metadata store."));
//        }
    }

    /**
     * This method represents the HTTP GET action. The input files for the authenticated user are
     * retrieved from the database and sent to the user as a {@link org.json.JSONArray JSONArray}
     * of {@link org.json.JSONObject JSONObject}.
     *
     */
    @Override
    public Representation represent(Variant variant) throws ResourceException {
        DBCursor cursor = null;
        try {
            // Include user defined query clauses given within the URL as q=<clauses>
            AgaveLogServiceClient.log(METADATA02.name(), MetaGetById.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());

            String strResult = "";

            MetadataDao dao = MetadataDao.getInstance();

            // TODO: Should be folded into the fetch call? Much faster, but also removes ability to determine pems
            //   vs a standard not found.
            if (dao.hasRead(getAuthenticatedUsername(), uuid)){
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                return new IplantErrorRepresentation("User does not have permission to read this metadata entry.");
            }

            Document uuidLookupDoc = new Document().append("uuid", uuid).append("tenantId", TenancyHelper.getCurrentTenantId());
            MetadataSearch search = new MetadataSearch(username);
            search.setAccessibleOwnersImplicit();
            search.setUuid(uuid);

            if (hasJsonPathFilters()){
                Document userResult = search.filterFindOne(uuidLookupDoc, jsonPathFilters);
                if (userResult == null)
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                            "No metadata item found for user with id " + uuid);

                strResult = userResult.toJson();
            } else {
                MetadataItem metadataItem = MetadataDao.getInstance().findSingleMetadataItem(uuidLookupDoc);
                if (metadataItem == null)
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                            "No metadata item found for user with id " + uuid);
                MetadataItemSerializer metadataItemSerializer = new MetadataItemSerializer(metadataItem);
                strResult = metadataItemSerializer.formatMetadataItemResult().toString();
            }

            return new IplantSuccessRepresentation(strResult);

        }
        catch (ResourceException e) {
            log.error("Failed to fetch metadata " + uuid + " for user " + username, e);
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
        }
        catch (Throwable e) {
            log.error("Failed to list metadata " + uuid, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "An unexpected error occurred while fetching the metadata item. "
                            + "If this continues, please contact your tenant administrator.", e);
        } finally {
            try { cursor.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * HTTP POST for Creating and Updating Metadata
     * @param entity the entity to process and update the existing metadata item with
     */
    @Override
    public void acceptRepresentation(Representation entity) {
        AgaveLogServiceClient.log(METADATA02.name(), MetaEdit.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());

        DBCursor cursor = null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode items = mapper.createArrayNode();
            MetadataItem updatedMetadataItem = null;
            Document metadataDocument;
            MetadataDao dao = MetadataDao.getInstance();

            try {

                JsonNode jsonMetadata = super.getPostedEntityAsObjectNode(false);
                JsonHandler jsonHandler = new JsonHandler();
                metadataDocument = jsonHandler.parseJsonMetadataToDocument(jsonMetadata);

            } catch (ResourceException e) {
                throw e;
            } catch (Exception e) {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Unable to parse form. " + e.getMessage());
            }

            try {
                // TODO: Should be folded into the fetch call? Much faster, but also removes ability to determine pems
                //   vs a standard not found.
                if (dao.hasWrite(getAuthenticatedUsername(), uuid)){
                    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                    throw new PermissionException("User does not have permission to update metadata");
                }

                MetadataSearch search = new MetadataSearch(getAuthenticatedUsername());
                search.setAccessibleOwnersImplicit();
                search.setUuid((String)getRequest().getAttributes().get("uuid"));
                Document updatedMetadataDoc = search.updateMetadataItem(metadataDocument);

                if (updatedMetadataDoc == null) {
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                            "No metadata item found for user with id " + uuid);
                } else {
                    NotificationManager.process(uuid, "UPDATED", username);
                    getResponse().setStatus(Status.SUCCESS_OK);
                }

                getResponse().setEntity(new IplantSuccessRepresentation(updatedMetadataDoc.toJson()));


            } catch (PermissionException e) {
                throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
                        "User does not have permission to update metadata");
            }

            return;
        } catch (ResourceException e) {
            log.error("Failed to update metadata item " + uuid + ". " + e.getMessage());

            getResponse().setStatus(e.getStatus());
            getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update metadata item " + uuid);

            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            getResponse().setEntity(new IplantErrorRepresentation("Unable to store the metadata object. " +
                    "If this problem persists, please contact the system administrators."));
        } finally {
            try { cursor.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * DELETE
     **/
    @Override
    public void removeRepresentations() {
        AgaveLogServiceClient.log(METADATA02.name(), MetaDelete.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());

        DBCursor cursor = null;
        MetadataItem deletedMetadataItem = null;
        MetadataDao dao = MetadataDao.getInstance();
        try {

            if (StringUtils.isEmpty(uuid)) {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "No object identifier provided.");
            }

            MetadataSearch search = new MetadataSearch(this.username);
            search.setAccessibleOwnersImplicit();

            try {

                // TODO: Should be folded into the fetch call? Much faster, but also removes ability to determine pems
                //   vs a standard not found.
                if (dao.hasWrite(getAuthenticatedUsername(), uuid)){
                    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                    throw new PermissionException("User does not have permission to delete this metadata entry.");
                }

                search.setUuid(uuid);
                deletedMetadataItem = search.deleteCurrentMetadataItem();

                if (deletedMetadataItem == null) {
                    throw new Exception();
                }
                for (String aid : deletedMetadataItem.getAssociations().getAssociatedIds().keySet()) {
                    NotificationManager.process((String) aid, "METADATA_DELETED", username);
                }

                NotificationManager.process(uuid, "DELETED", username);

                getResponse().setStatus(Status.SUCCESS_OK);
                getResponse().setEntity(new IplantSuccessRepresentation());

            } catch (PermissionException e) {
                getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
                getResponse().setEntity(new IplantErrorRepresentation(
                        "User does not have permission to update metadata"));
                return;
            }

        } catch (ResourceException e) {
            log.error("Failed to delete metadata item " + uuid + ". " + e.getMessage());

            getResponse().setStatus(e.getStatus());
            getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete metadata " + uuid, e);

            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            getResponse().setEntity(new IplantErrorRepresentation("Unable to delete the associated metadata. " +
                    "If this problem persists, please contact the system administrators."));
        } finally {
            try { cursor.close(); } catch (Exception ignore) {}
        }
    }

    private DBObject formatMetadataObject(DBObject metadataObject) throws UUIDException {
        metadataObject.removeField("_id");
        metadataObject.removeField("tenantId");
        BasicDBObject hal = new BasicDBObject();
        hal.put("self", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/" + metadataObject.get("uuid")));
        hal.put("permissions", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/" + metadataObject.get("uuid") + "/pems"));
        hal.put("owner", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + metadataObject.get("owner")));

        if (metadataObject.containsField("associationIds")) {
            // TODO: break this into a list of object under the associationIds attribute so
            // we dont' overwrite the objects in the event there are multiple of the same type.
            BasicDBList halAssociationIds = new BasicDBList();

            for (Object associatedId : (BasicDBList) metadataObject.get("associationIds")) {
                AgaveUUID agaveUUID = new AgaveUUID((String) associatedId);

                try {
                    String resourceUrl = agaveUUID.getObjectReference();
                    BasicDBObject assocResource = new BasicDBObject();
                    assocResource.put("rel", (String) associatedId);
                    assocResource.put("href", TenancyHelper.resolveURLToCurrentTenant(resourceUrl));
                    assocResource.put("title", agaveUUID.getResourceType().name().toLowerCase());
                    halAssociationIds.add(assocResource);
                } catch (UUIDException e) {
                    BasicDBObject assocResource = new BasicDBObject();
                    assocResource.put("rel", (String) associatedId);
                    assocResource.put("href", null);
                    if (agaveUUID != null) {
                        assocResource.put("title", agaveUUID.getResourceType().name().toLowerCase());
                    }
                    halAssociationIds.add(assocResource);
                }
            }

            hal.put("associationIds", halAssociationIds);
        }

        if (metadataObject.get("schemaId") != null && !StringUtils.isEmpty(metadataObject.get("schemaId").toString())) {
            AgaveUUID agaveUUID = new AgaveUUID((String) metadataObject.get("schemaId"));
            hal.append(agaveUUID.getResourceType().name().toLowerCase(), new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(agaveUUID.getObjectReference())));
        }
        metadataObject.put("_links", hal);

        return metadataObject;
    }

    /* (non-Javadoc)
     * @see org.restlet.resource.Resource#allowPost()
     */
    @Override
    public boolean allowPost() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.restlet.resource.Resource#allowDelete()
     */
    @Override
    public boolean allowDelete() {
        return true;
    }

}
