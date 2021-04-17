/**
 * 
 */
package org.iplantc.service.metadata.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataDao;
import org.iplantc.service.metadata.exceptions.MetadataAssociationException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;

/**
 * @author dooley
 *
 */
public class MetadataAssociationList {
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    private LinkedHashMap<String, AssociatedReference> associatedIds = new LinkedHashMap<String, AssociatedReference>();
    
    public MetadataAssociationList() {}
    
//    @JsonCreator
//    public MetadataAssociationList(List<String> associatedIds) throws MetadataAssociationException, PermissionException {
//        this.addAll(associatedIds);
//    }

    /**
     * Adds a single {@code uuid} to this {@link MetadataAssociationList}.
     * @param uuid a valid serialized {@link AgaveUUID} strings.
     * @throws MetadataAssociationException if the {@code uuid} is invalid
     * @throws PermissionException if the user does not have permission to reference the {@code uuid}
     */
    public void add(String uuid) throws MetadataAssociationException, PermissionException {
            
        if (StringUtils.isEmpty(uuid) || !associatedIds.containsKey(uuid)) {
            AssociatedReference ref = checkForValidAssociationUuid(uuid);
            this.associatedIds.put(uuid, ref);
        }
    }

    /**
     * Adds a single {@code ref} to this {@link MetadataAssociationList}.
     * @param ref a valid {@link AssociatedReference}
     */
    public void add(AssociatedReference ref){
        if (ref != null && !associatedIds.containsKey(ref.getUuid().toString())) {
            this.associatedIds.put(ref.getUuid().toString(), ref);
        }
    }

    /**
     * Adds all the {@code uuids} to this {@link MetadataAssociationList}.
     * @param uuids list of valid serialized {@link AgaveUUID} strings.
     * @throws MetadataAssociationException if one of the {@code uuids} is invalid
     * @throws PermissionException if the user does not have permission to reference one of the {@code uuids}
     */
    public void addAll(Collection<String> uuids)  throws MetadataAssociationException, PermissionException {
        for (String uuid: uuids) {
            this.add(uuid);
        }
    }
    
    /**
     * Removes all the {@code uuids} from this {@link MetadataAssociationList}.
     */
    public void clear() {
        this.associatedIds.clear();
    }
    
    /**
     * @return true if there are no associated uuid, falst otherwise
     */
    public boolean isEmpty() {
        return this.associatedIds.isEmpty();
    }
    
    /**
     * Removes all the {@code uuids} from this {@link MetadataAssociationList}.
     * @param uuid the uuid to remove
     */
    public void remove(String uuid) {
        this.associatedIds.remove(uuid);
    }
    
    /**
     * Validates that a given uuid is valid and that the current user has the
     * proper permissions to access it.
     * 
     * @param uuid the uuid to check
     * @return a reference object to the uuid
     * @throws MetadataAssociationException if unable to resolve the associatedId
     * @throws PermissionException if user does not have permission to view the associatedId
     */
    @SuppressWarnings("unused")
    private AssociatedReference checkForValidAssociationUuid(String uuid) throws MetadataAssociationException, PermissionException {
        
        if (StringUtils.isEmpty(uuid)) {
            throw new MetadataAssociationException("Associated UUID cannot be empty");
        } 
        else 
        {
            AgaveUUID associationUuid;
            try {
                associationUuid = new AgaveUUID(uuid);
            
                if (UUIDType.METADATA == associationUuid.getResourceType() || UUIDType.SCHEMA == associationUuid.getResourceType()) {
                        String url = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE + "data/" + uuid);
                        return new AssociatedReference(associationUuid, url);
                } else {
                    // check that the object reference resolves
                    String url = associationUuid.getObjectReference();

//                    if (false) {
//                        throw new PermissionException("User does not permission to reference this resource.");
//                    }

                    return new AssociatedReference(associationUuid, url);
                }
            }
            catch (UUIDException e1) {
                throw new MetadataAssociationException("No associated resource found with uuid " + uuid);
//            catch (PermissionException e) {
//                throw e;
//            }
            }
            catch (Exception e) {
                throw new MetadataAssociationException("Unable to resolve associated resource uuid " + uuid, e);
            }
        }
    }

    /**
     * Returns map with url as keys and resolved referces as values
     * @return the json object with associatedId group info
     */
    public ObjectNode getReferenceGroupMap() {
        ObjectNode json = mapper.createObjectNode();
        for (String uuid: associatedIds.keySet()) {
            AssociatedReference ref = associatedIds.get(uuid);
            if (!json.has(ref.getUuid().getResourceType().name().toLowerCase())) {
                json.putArray(ref.getUuid().getResourceType().name().toLowerCase())
                        .addObject()
                            .put("title", ref.getUuid().toString())
                            .put("href", ref.getUrl());
            } else {
                ((ArrayNode)json.get(ref.getUuid().getResourceType().name().toLowerCase()))
                        .addObject()
                            .put("title", ref.getUuid().toString())
                            .put("href", ref.getUrl());
            }
        }
        
        return json;
    }
     
    /**
     * Returns list of uuid as a serialized {@link ArrayNode}
     * @return serialized list of uuid
     */
    @JsonValue
    public String toString() {
        ArrayNode associations = mapper.createArrayNode();
        for(String uuid: associatedIds.keySet()) {
            associations.add(uuid);
        }
        
        return associations.toString();
    }

    /**
     * Returns a list of the UUID string values.
     * @return the raw set of associatedId uuids
     */
    @JsonProperty("associatedIds")
    public Set<String> getRawUuid() {
        return associatedIds.keySet();
    }

    /**
     * Returns count of UUIDs 
     * @return  the number of associatedIds
     */
    public int size() {
        return associatedIds.size();
    }

    public LinkedHashMap<String, AssociatedReference> getAssociatedIds() {
        return associatedIds;
    }

    public void setAssociatedIds(LinkedHashMap<String, AssociatedReference> associatedIds) {
        this.associatedIds = associatedIds;
    }
}
