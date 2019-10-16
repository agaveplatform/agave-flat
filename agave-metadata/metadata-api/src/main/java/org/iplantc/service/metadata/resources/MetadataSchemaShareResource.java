package org.iplantc.service.metadata.resources;

import com.mongodb.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.metadata.MetadataApplication;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataSchemaPermissionDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.managers.MetadataSchemaPermissionManager;
import org.iplantc.service.metadata.model.MetadataSchemaPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.util.ServiceUtils;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import java.util.List;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.*;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.METADATA02;


/**
 * The MetadataShareResource object enables HTTP GET and POST actions on permissions.
 * 
 * @author dooley
 * 
 */
@SuppressWarnings("deprecation")
public class MetadataSchemaShareResource extends AgaveResource 
{
	private static final Logger	log	= Logger.getLogger(MetadataSchemaShareResource.class);

	private String username; // authenticated user
	private String schemaId;  // object id
    private String owner;
	private String sharedUsername; // user receiving permissions
	private DB db;
    private DBCollection collection;

   /**
	 * @param context the request context
	 * @param request the request object
	 * @param response the response object
	 */
	public MetadataSchemaShareResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		this.schemaId = (String) request.getAttributes().get("schemaId");

		this.sharedUsername = (String) request.getAttributes().get("user");

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));

		try 
        {
			MongoClient mongoClient = ((MetadataApplication) getApplication()).getMongoClient();
        	db = mongoClient.getDB(Settings.METADATA_DB_SCHEME);
            // Gets a collection, if it does not exist creates it
            collection = db.getCollection(Settings.METADATA_DB_SCHEMATA_COLLECTION);
            
            if (!StringUtils.isEmpty(schemaId)) 
            {
    	        DBObject returnVal = collection.findOne(new BasicDBObject("uuid", schemaId));
    	
    	        if (returnVal == null) {
    	            throw new MetadataException("No metadata schema item found for user with id " + schemaId);
    	        }
    	        
    	        owner = (String)returnVal.get("owner");
            }
            else
            {
            	throw new MetadataException("No metadata schema id provided");
            }

        } catch (MetadataException e) {
        	response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            response.setEntity(new IplantErrorRepresentation(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Unable to connect to metadata store", e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            response.setEntity(new IplantErrorRepresentation("Unable to connect to metadata store."));
        }
//        finally {
////        	try { mongoClient.close(); } catch (Throwable e) {}
//        }
	}

	/**
	 * This method represents the HTTP GET action. Gets Perms on specified iod.
	 */
	@Override
	public Representation represent(Variant variant)
	{
		AgaveLogServiceClient.log(METADATA02.name(), SchemaPemsList.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(schemaId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("No metadata schema id provided.");
		}

		try
		{
			MetadataSchemaPermissionManager pm = new MetadataSchemaPermissionManager(schemaId, owner);
			if (pm.canRead(username))
			{
				List<MetadataSchemaPermission> permissions = MetadataSchemaPermissionDao
						.getBySchemaId(schemaId);
				
				if (StringUtils.isEmpty(sharedUsername)) 
				{
					StringBuilder jPems = new StringBuilder(new MetadataSchemaPermission(schemaId, owner, PermissionType.ALL).toJSON());
					for (int i=offset; i< Math.min((limit+offset), permissions.size()); i++)
	    			{
						jPems.append(",").append(permissions.get(i).toJSON());
					}
					return new IplantSuccessRepresentation("[" + jPems + "]");
				} 
				else 
				{
					if (ServiceUtils.isAdmin(sharedUsername) || StringUtils.equals(owner, sharedUsername))
					{
						MetadataSchemaPermission pem = new MetadataSchemaPermission(schemaId, sharedUsername, PermissionType.ALL);
						return new IplantSuccessRepresentation(pem.toJSON());
					}
					else 
					{
						MetadataSchemaPermission pem = 
								MetadataSchemaPermissionDao.getByUsernameAndSchemaId(sharedUsername, schemaId);
						
						if (pem == null) {
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
									"No permissions found for user " + sharedUsername);
						}
						else {
							return new IplantSuccessRepresentation(pem.toJSON());
						}
					}
				}
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to view this resource");
			}

		}
		catch (ResourceException e)
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Exception e)
		{
			// Bad request
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}
	}

	/**
	 * Post action for adding (and overwriting) permissions on a metadata iod
	 * 
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
		try
		{
			if (StringUtils.isEmpty(schemaId))
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"No schema id provided.");
			}
			
			String name;
            String sPermission;

            JSONObject postPermissionContent = super.getPostedEntityAsJsonObject(true);
            
            if (StringUtils.isEmpty(sharedUsername))
            {
            	AgaveLogServiceClient.log(METADATA02.name(), SchemaPemsUpdate.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
            	
                if (postPermissionContent.has("username")) 
                {
                    name = postPermissionContent.getString("username");
            	} 
                else
                {	
                	// a username must be provided either in the form or the body
                	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                			"No username specified. Please specify a valid user to whom the permission will apply."); 
                }
            }
            else
            {
            	AgaveLogServiceClient.log(METADATA02.name(), SchemaPemsCreate.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
            	
            	// name in url and json, if provided, should match
            	if (postPermissionContent.has("username") && 
            			!StringUtils.equalsIgnoreCase(postPermissionContent.getString("username"), sharedUsername)) 
                {
            		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                			"The username value in the POST body, " + postPermissionContent.getString("username") + 
                			", does not match the username in the URL, " + sharedUsername);            		
            	} 
                else
                {
                	name = sharedUsername;
                }
            }
            
            if (postPermissionContent.has("permission")) 
            {
                sPermission = postPermissionContent.getString("permission");
                if (StringUtils.equalsIgnoreCase(sPermission, "none") ||
                		StringUtils.equalsIgnoreCase(sPermission, "null")) {
                	sPermission = null;
                }
            } 
            else 
            {
            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Missing permission field. Please specify a valid permission of READ, WRITE, or READ_WRITE.");
            }
            
			if (!ServiceUtils.isValid(name)) { 
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST, "No user found matching " + name); 
			} 
			else 
			{
				// validate the user they are giving permissions to exists
				AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(
						Settings.IPLANT_PROFILE_SERVICE, 
						Settings.IRODS_USERNAME, 
						Settings.IRODS_PASSWORD);
				
				if (authClient.getUser(name) == null) {
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No user found matching " + name);
				}
			}

            MetadataSchemaPermissionManager pm = new MetadataSchemaPermissionManager(schemaId, owner);

			if (pm.canWrite(username))
			{
				// if the permission is null or empty, the permission
				// will be removed
				try 
				{
					pm.setPermission(name, sPermission);
					
					if (StringUtils.isEmpty(sPermission)) {
						getResponse().setStatus(Status.SUCCESS_OK);
					} else {
						getResponse().setStatus(Status.SUCCESS_CREATED);
					}
					MetadataSchemaPermission pem = MetadataSchemaPermissionDao.getByUsernameAndSchemaId(name, schemaId);
					if (pem == null) {
						pem = new MetadataSchemaPermission(schemaId, name, PermissionType.NONE);
					}
					getResponse().setEntity(new IplantSuccessRepresentation(pem.toJSON()));
				} 
				catch (PermissionException e) {
					throw new ResourceException(
							Status.CLIENT_ERROR_FORBIDDEN,
							e.getMessage(), e);
				}
				catch (IllegalArgumentException iae) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST,
							"Invalid permission value. Valid values are: " + PermissionType.supportedValuesAsString());
				}
			}
			else
			{
				throw new ResourceException(
						Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to modify this resource.");
			}

		}
		catch (ResourceException e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (Throwable e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Failed to update metadata schema permissions: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#removeRepresentations()
	 */
	@Override
	public void removeRepresentations() throws ResourceException
	{
		AgaveLogServiceClient.log(METADATA02.name(), SchemaPemsDelete.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try
		{
			if (StringUtils.isEmpty(schemaId))
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Schema id cannot be empty");
			}
			
			MetadataSchemaPermissionManager pm = new MetadataSchemaPermissionManager(schemaId, owner);

			if (pm.canWrite(username))
			{
				if (StringUtils.isEmpty(sharedUsername)) {
					// clear all permissions
					pm.clearPermissions();
				} else { // clear pems for user
					pm.setPermission(sharedUsername, null);
				}
				
				getResponse().setEntity(new IplantSuccessRepresentation());
			}
			else
			{
				throw new ResourceException(
						Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to modify this resource.");
			}
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"Failed to remove metadata schema permissions: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut()
	{
		return false;
	}

	/**
	 * Allow the resource to be modified
	 * 
	 * @return true
	 */
	public boolean setModifiable()
	{
		return true;
	}

	/**
	 * Allow the resource to be read
	 * 
	 * @return true
	 */
	public boolean setReadable()
	{
		return true;
	}
}
