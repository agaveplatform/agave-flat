package org.iplantc.service.metadata.search;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jsonschema.main.AgaveJsonSchemaFactory;
import com.github.fge.jsonschema.main.AgaveJsonValidator;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.mongodb.BasicDBObject;
import io.grpc.Metadata;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.metadata.dao.MetadataSchemaDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.exceptions.MetadataQueryException;
import org.iplantc.service.metadata.exceptions.MetadataSchemaValidationException;
import org.iplantc.service.metadata.exceptions.MetadataStoreException;
import org.iplantc.service.metadata.managers.MetadataSchemaPermissionManager;
import org.iplantc.service.metadata.model.*;
import org.iplantc.service.metadata.model.validation.MetadataSchemaComplianceValidator;
import org.iplantc.service.notification.model.Notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Parse JsonNode and validate
 */
public class JsonHandler {
    private ArrayNode permissions;
    private ArrayNode notifications;
    private MetadataValidation metadataValidation;
    private MetadataItem metadataItem;
    ObjectMapper mapper = new ObjectMapper();


    public ArrayNode getPermissions() {
        return permissions;
    }

    public void setPermissions(ArrayNode permissions) {
        this.permissions = permissions;
    }

    public ArrayNode getNotifications() {
        return notifications;
    }

    public void setNotifications(ArrayNode notifications) {
        this.notifications = notifications;
    }

    public MetadataValidation getMetadataValidation() {
        return metadataValidation;
    }

    public void setMetadataValidation(MetadataValidation paramValidation) {
        this.metadataValidation = paramValidation;
    }

    public MetadataItem getMetadataItem() {
        return metadataItem;
    }

    public void setMetadataItem(MetadataItem metadataItem) {
        this.metadataItem = metadataItem;
    }


    public JsonHandler() {
        this.metadataItem = new MetadataItem();
    }


    /**
     * Parse String in Json format to {@link JsonNode}
     * @param strJson String in Json format
     * @return JsonNode of {@code strJson}
     * @throws MetadataQueryException if {@code strJson} is invalid json format
     */
    public JsonNode parseStringToJson(String strJson) throws MetadataQueryException {
        try {
            JsonFactory factory = this.mapper.getFactory();
            return factory.createParser(strJson).readValueAsTree();
        } catch (IOException e){
            throw new MetadataQueryException("Invalid Json format: " + e.getMessage());
        }
    }


    /**
     * Update corresponding {@link Document} value for {@code key} with parsed Regex value
     * @param doc {@link Document} to update
     * @return {@link Document} with updated Regex value
     */
    public Document parseRegexValueFromDocument(Document doc, String key){
        if (((String) doc.get(key)).contains("*")) {
            Pattern regexPattern = Pattern.compile(doc.getString(key),
                    Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
            doc.put(key, regexPattern);
        }
        return doc;
    }

    /**
     * Parse JsonString {@code userQuery} to {@link Document}
     * Regex will also be parsed
     *
     * @param strJson JsonString to parse
     * @return {@link Document} of the JsonString {@code userQuery}
     * @throws MetadataQueryException if invalid Json format
     */
    public Document parseStringToDocument(String strJson) throws MetadataQueryException {
        Document doc = new Document();
        if (StringUtils.isNotEmpty(strJson)){
            try {
                doc = Document.parse(strJson);
                for (String key : doc.keySet()){
                    if (doc.get(key) instanceof String){
                        doc = parseRegexValueFromDocument(doc, key);
                    }
                }
            } catch (Exception e) {
                throw new MetadataQueryException("Unable to parse query ", e);
            }
        }
        return doc;
    }

    /**
     * Parse {@link JsonNode} to {@link MetadataItem}
     *
     * @param jsonMetadata {@link JsonNode} parse from the query string
     * @throws MetadataQueryException if query values are missing or invalid
     */
    public void parseJsonMetadata(JsonNode jsonMetadata) throws MetadataQueryException {
        try {
            this.permissions = this.mapper.createArrayNode();
            this.notifications = this.mapper.createArrayNode();

            this.metadataItem.setName(parseNameToString(jsonMetadata));
            this.metadataItem.setValue(parseValueToJsonNode(jsonMetadata));
            this.metadataItem.setAssociations(parseAssociationIdsToArrayNode(jsonMetadata));
            this.metadataItem.setSchemaId(parseSchemaIdToString(jsonMetadata));
            this.permissions = parsePermissionToArrayNode(jsonMetadata);

//            if (this.permissions == null)
//                this.metadataItem.setPermissions(null);

            this.notifications = parseNotificationToArrayNode(jsonMetadata);

        } catch (MetadataQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new MetadataQueryException(
                    "Unable to parse form. " + e.getMessage());
        }
    }

//    public Document parseDocument(JsonNode jsonMetadata){
////        Document doc = (Document) jsonMetadata;
//
//    }
    public Document parseJsonNodeToDocument(JsonNode jsonNode){
        Document doc = new Document();
        for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
            String key = it.next();

            if (jsonNode.get(key).isObject()) {
                JsonNode nestedNode = jsonNode.get(key);
                doc.append(key, parseJsonNodeToDocument(nestedNode));
            } else {
                doc.append(key, String.valueOf(jsonNode.get(key).textValue()));
            }
        }
        return doc;
    }

    public Document parseJsonMetadataToDocument(JsonNode jsonMetadata) throws MetadataQueryException, PermissionException, MetadataStoreException, UUIDException, MetadataException {
        Document doc = new Document();
        ObjectMapper mapper = new ObjectMapper();

        String name = parseNameToString(jsonMetadata);
        if (name != null)
            doc.append("name", name);

        JsonNode value = parseValueToJsonNode(jsonMetadata);
        if (value != null) {
            Document valueDoc = parseJsonNodeToDocument(value);
            doc.append("value", valueDoc);
        }

        MetadataAssociationList associationList = parseAssociationIdsToArrayNode(jsonMetadata);
        if (associationList.size() > 0)
            doc.append("associationIds", associationList.getAssociatedIds().keySet().toArray());

        String schemaId = parseSchemaIdToString(jsonMetadata);
        if (schemaId != null)
            doc.append("schemaId", schemaId);

        ArrayNode permissions = parsePermissionToArrayNode(jsonMetadata);
        if (permissions != null) {
            doc.append("permissions", permissions);
        }

        ArrayNode notifications = parseNotificationToArrayNode(jsonMetadata);
        if (notifications != null)
            doc.append("notifications", notifications);

        doc.append("uuid", this.metadataItem.getUuid());
        return doc;
    }

    /**
     * Get {@link JsonNode} name field
     *
     * @param nameNode {@link JsonNode} to parse field from
     * @return {@link String} value for the name field
     * @throws MetadataQueryException if name field is missing or invalid format
     */
    public String parseNameToString(JsonNode nameNode) throws MetadataQueryException {
        if (nameNode.has("name") && nameNode.get("name").isTextual()
                && !nameNode.get("name").isNull()) {
            return nameNode.get("name").asText();
        } else {
            throw new MetadataQueryException(
                    "No name attribute specified. Please associate a value with the metadata name.");
        }
    }

    /**
     * Get {@link JsonNode} value field
     *
     * @param valueNode {@link JsonNode} to parse field from
     * @return {@link JsonNode} of {@code value}
     * @throws MetadataQueryException if value field is invalid json format or missing
     */
    public JsonNode parseValueToJsonNode(JsonNode valueNode) throws MetadataQueryException {
        if (valueNode.has("value") && !valueNode.get("value").isNull()) {
            return valueNode.get("value");
        } else
            throw new MetadataQueryException(
                    "No value attribute specified. Please associate a value with the metadata value.");
    }


    /**
     * Get {@JsonNode} associationIds field
     *
     * @param associationNode {@link JsonNode} to parse field from
     * @return {@link ArrayNode} of {@link AgaveUUID} listed in the associationIds field
     */
    public MetadataAssociationList parseAssociationIdsToArrayNode(JsonNode associationNode) throws UUIDException, MetadataException, MetadataQueryException {
        ArrayNode associationItems = this.mapper.createArrayNode();

        MetadataAssociationList associationList = new MetadataAssociationList();

        if (associationNode.has("associationIds")) {
            if (associationNode.get("associationIds").isArray()) {
                associationItems = (ArrayNode) associationNode.get("associationIds");
            } else {
                if (associationNode.get("associationIds").isTextual())
                    associationItems.add(associationNode.get("associationIds").asText());
            }
            //validate ids
            if (metadataValidation == null)
                metadataValidation = new MetadataValidation();
            associationList = metadataValidation.checkAssociationIds_uuidApi(associationItems);
        }
        return associationList;
    }

    /**
     * Get the {@JsonNode} notifications field
     *
     * @param notificationNode{@link JsonNode} to parse field from
     * @return {@link ArrayNode} of notifications, null if notification field is not specified
     * @throws MetadataQueryException if notifications field is invalid format
     */
    public ArrayNode parseNotificationToArrayNode(JsonNode notificationNode) throws MetadataQueryException {
        if (notificationNode.hasNonNull("notifications")) {
            if (notificationNode.get("notifications").isArray()) {
                 return (ArrayNode) notificationNode.get("notifications");
            } else {
                throw new MetadataQueryException(
                        "Invalid notifications value. notifications should be an "
                                + "JSON array of notification objects.");
            }
        }
        return null;
    }

    /**
     * Get {@JsonNode} permissions field
     *
     * @param permissionNode{@link JsonNode} to parse field from
     * @return {@link ArrayNode} of permissions, null if permission field is not specified
     * @throws MetadataQueryException if permissions field is invalid format
     */
    public ArrayNode parsePermissionToArrayNode(JsonNode permissionNode) throws MetadataQueryException {
        if (permissionNode.hasNonNull("permissions")) {
            if (permissionNode.get("permissions").isArray()) {
                return (ArrayNode) permissionNode.get("permissions");
            } else {
                throw new MetadataQueryException(
                        "Invalid permissions value. permissions should be an "
                                + "JSON array of permission objects.");
            }
        }
        return null;
    }

    /**
     * Get {@link JsonNode} schemaId field
     *
     * @param schemaNode {@link JsonNode} to get schemaId from
     * @return {@link String} of schemaId
     * @throws MetadataStoreException if unable to connect to the mongo collection
     * @throws PermissionException    if user doesn't have permissions to view the metadata schema
     */
    public String parseSchemaIdToString(JsonNode schemaNode) throws MetadataStoreException, PermissionException {
        if (schemaNode.has("schemaId") && schemaNode.get("schemaId").isTextual()) {

            if (metadataValidation == null)
                metadataValidation = new MetadataValidation();

            Document schemaDoc = MetadataSchemaDao.getInstance().findOne(new Document("uuid", schemaNode.get("schemaId").asText())
                    .append("tenantId", TenancyHelper.getCurrentTenantId()));

//            Document schemaDoc = metadataValidation.checkSchemaIdExists(schemaNode.get("schemaId").asText());

            //where to check for permission?
            if (schemaDoc != null)
                return schemaNode.get("schemaId").asText();
        }
        return null;
    }

    /**
     * Validate given JsonNode against the schemaId
     */
    public String validateValueAgainstSchema(String value, String schema) throws MetadataQueryException, MetadataSchemaValidationException {
        try {
            JsonFactory factory = this.mapper.getFactory();
            JsonNode jsonSchemaNode = factory.createParser(schema).readValueAsTree();
            JsonNode jsonMetadataNode = factory.createParser(value).readValueAsTree();
            AgaveJsonValidator validator = AgaveJsonSchemaFactory.byDefault().getValidator();

            ProcessingReport report = validator.validate(jsonSchemaNode, jsonMetadataNode);
            if (!report.isSuccess()) {
                StringBuilder sb = new StringBuilder();
                for (ProcessingMessage processingMessage : report) {
                    sb.append(processingMessage.toString());
                    sb.append("\n");
                }
                throw new MetadataSchemaValidationException("Metadata does not conform to schema.");
            }
            return value;
        } catch (Exception e) {
            throw new MetadataSchemaValidationException("Metadata does not conform to schema.");
        }
    }
}
