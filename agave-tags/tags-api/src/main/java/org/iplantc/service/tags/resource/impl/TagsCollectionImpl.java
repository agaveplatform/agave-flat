/**
 * 
 */
package org.iplantc.service.tags.resource.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.tags.dao.TagDao;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagValidationException;
import org.iplantc.service.tags.managers.TagManager;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.resource.TagsCollection;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author dooley
 *
 */
@Path("")
public class TagsCollectionImpl extends AbstractTagCollection implements TagsCollection {

	private static final Logger log = Logger.getLogger(TagsCollectionImpl.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	public TagsCollectionImpl() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagsCollection#getTags()
	 */
	@Override
	public Response getTags() throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagsList);
        
		TagDao dao = new TagDao();
        try
        {
        	Map<SearchTerm, Object>  searchCriteria = getQueryParameters();
			
        	List<Tag> tags = dao.findMatching(getAuthenticatedUsername(), searchCriteria, getOffset(), getLimit());
				
        	return Response.ok(new AgaveSuccessRepresentation(mapper.writeValueAsString(tags))).build();
            
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to retrieve tags", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while fetching the tag. "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
		finally {
			try { HibernateUtil.closeSession(); } catch (Throwable ignored) {}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagsCollection#addTagFromForm(java.lang.String, java.util.List)
	 */
	@Override
	public Response addTag(Representation input) {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagPermissionUpdate);
        
        try
        {
          	JsonNode contentJson = getPostedContentAsJsonNode(input);  	
          	TagManager manager = new TagManager();
          	Tag tag = manager.addTagForUser(contentJson, getAuthenticatedUsername());
          	return Response.ok().entity(new AgaveSuccessRepresentation(tag.toJSON().toString())).status(201).build();
        }
        catch (TagException e) {
        	log.error(e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "Failed to add tag. If this problem persists, please contact your administrator.");
        }
        catch (TagValidationException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());

        }
        catch (Exception e) {
        	log.error("Failed to add tag", e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
        			"An unexpected error occurred while adding the tag. "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
		finally {
			try { HibernateUtil.closeSession(); } catch (Throwable ignored) {}
		}
	}
}
