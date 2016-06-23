/**
 * 
 */
package org.iplantc.service.notification.events;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class RealtimeChannelNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(RealtimeChannelNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public RealtimeChannelNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = null;
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			body = "Tag ${REALTIMECHANNEL_NAME} was created and associated with the following resources:\n\n" +
					"${REALTIMECHANNEL_ASSOCIATEDIDS}\n";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "Tag ${REALTIMECHANNEL_NAME} was deleted.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "updated")) {
			body = "Tag ${REALTIMECHANNEL_NAME} was updated\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_grant")) {
			body = "User ${PERMISSION_USERNAME} was granted ${PERMISSION_PERMISSION} on tag ${REALTIMECHANNEL_NAME}\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_revoke")) {
			body = "The permissions of user ${PERMISSION_USERNAME} were revoked on tag ${REALTIMECHANNEL_NAME}\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "published")) {
			body = "Tag ${REALTIMECHANNEL_NAME} was published. This tag is now visible to all tenant users.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "unpublished")) {
			body = "Tag ${REALTIMECHANNEL_NAME} was unpublished. This tag is no longer visible to all tenant users.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "resource_added")) {
            body = "A new resource was added to the ${REALTIMECHANNEL_NAME} tag. The resulting tag has the "
            		+ "following resources associated with it:\n\n" + 
            		"${REALTIMECHANNEL_ASSOCIATEDIDS}\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "resource_removed")) {
			body = "A new resource was removed from the ${REALTIMECHANNEL_NAME} tag. The resulting tag has the "
					+ "following resources associated with it:\n\n" + 
            		"${REALTIMECHANNEL_ASSOCIATEDIDS}\n\n";
		} else {
			body = "Tag ${REALTIMECHANNEL_NAME} received a ${EVENT} event. The resulting tag description is: \n\n "
                    + "${APP_JSON}\n\n";
		}
		
		return resolveMacros(body, false);	
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = null;
        if (StringUtils.equalsIgnoreCase(event, "created")) {
			body = "<p>Tag ${REALTIMECHANNEL_NAME} was created and associated with the following resources:</p>" +
                    "<br>" +
					"<p>${REALTIMECHANNEL_ASSOCIATEDIDS}</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "<p>Tag ${REALTIMECHANNEL_NAME} was deleted.</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "updated")) {
			body = "<p>Tag ${REALTIMECHANNEL_NAME} was updated. The resulting tag is:</p>" +
                    "<br>" +
                    "<p>${APP_JSON}</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_grant")) {
			body = "<p>User ${PERMISSION_USERNAME} was granted ${PERMISSION_PERMISSION} on tag ${REALTIMECHANNEL_NAME}</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_revoke")) {
			body = "<p>The permissions of user ${PERMISSION_USERNAME} were revoked on tag ${REALTIMECHANNEL_NAME}</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "published")) {
			body = "<p>Tag ${REALTIMECHANNEL_NAME} was published. This tag is now visible to all tenant users.</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "unpublished")) {
			body = "<p>Tag ${REALTIMECHANNEL_NAME} was unpublished. This tag is no longer visible to all tenant users.</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "resource_added")) {
            body = "<p>A new resource was added to the ${REALTIMECHANNEL_NAME} tag. The resulting tag has the " +
            		"following resources associated with it:</p>" +
                    "<br>" +
            		"<p>${REALTIMECHANNEL_ASSOCIATEDIDS}</p>";
		} else if (StringUtils.equalsIgnoreCase(event, "resource_removed")) {
			body = "<p>A new resource was removed from the ${REALTIMECHANNEL_NAME} tag. The resulting tag has the " +
					"following resources associated with it:</p>" +
                    "<br>" +
            		"<p>${REALTIMECHANNEL_ASSOCIATEDIDS}</p>";
		} else {
			body = "<p>Tag ${REALTIMECHANNEL_NAME} received a(n) ${EVENT} event. The resulting tag description is:</p>" +
                    "<br>" +
                    "<p>${APP_JSON}<p>";
		}
        
        return resolveMacros(body, false);  
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		String subject = null;
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			subject = "Tag ${REALTIMECHANNEL_NAME} was created";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			subject = "Tag ${REALTIMECHANNEL_NAME} was deleted";
		} else if (StringUtils.equalsIgnoreCase(event, "updated")) {
			subject = "Tag ${REALTIMECHANNEL_NAME} updated a user role";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_grant")) {
			subject = "Tag ${REALTIMECHANNEL_NAME} granted a user role to ${PERMISSION_USERNAME}";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_revoke")) {
			subject = "Tag ${REALTIMECHANNEL_NAME} revoked a user role to ${PERMISSION_USERNAME}";
		} else if (StringUtils.equalsIgnoreCase(event, "published")) {
			subject = "Tag ${REALTIMECHANNEL_NAME} was published";
		} else if (StringUtils.equalsIgnoreCase(event, "unpublished")) {
			subject = "Tag ${REALTIMECHANNEL_NAME} was published";
		} else if (StringUtils.equalsIgnoreCase(event, "resource_added")) {
			subject = "A new resource was tagged with ${REALTIMECHANNEL_NAME}";
		} else if (StringUtils.equalsIgnoreCase(event, "resource_removed")) {
			subject = "A resource was untagged from ${REALTIMECHANNEL_NAME}";
		// catchall
        } else {
			subject = "Tag ${REALTIMECHANNEL_NAME} recieved a(n) ${EVENT} event";
		}
		
		return resolveMacros(subject, false);	
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			body = StringUtils.replace(body, "${UUID}", this.associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", this.event);
			body = StringUtils.replace(body, "${OWNER}", this.owner);
			
			if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
			    ObjectMapper mapper = new ObjectMapper();
			    try {
			        JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
		        	if (json.has("realtimechannel") ) {
		        		JsonNode jsonEntity = json.get("realtimechannel");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_ID}", jsonEntity.has("id") ? jsonEntity.get("id").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_NAME}", jsonEntity.has("name") ? jsonEntity.get("name").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_OWNER}", jsonEntity.has("owner") ? jsonEntity.get("owner").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_URL}", jsonEntity.has("_links") ? jsonEntity.get("_links").get("href").get("self").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_PROTOCOL}", jsonEntity.has("protocol") ? jsonEntity.get("protocol").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_CREATED}", jsonEntity.has("created") ? jsonEntity.get("created").asText(): "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_EVENTS}", jsonEntity.has("events") ? jsonEntity.get("events").toString() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_JSON}", jsonEntity.toString());
                    } else if (json.has("permission") ) {
                    	JsonNode jsonPermission = json.get("permission");
                        body = StringUtils.replace(body, "${PERMISSION_ID}", jsonPermission.has("id") ? jsonPermission.get("id").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_PERMISSION}", jsonPermission.has("permission") ? jsonPermission.get("permission").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_USERNAME}", jsonPermission.has("username") ? jsonPermission.get("username").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_LASTUPDATED}", jsonPermission.has("lastUpdated") ? jsonPermission.get("lastUpdated").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_JSON}", jsonPermission.toString());
                    } else if (json.has("id")) {
                    	body = StringUtils.replace(body, "${REALTIMECHANNEL_ID}", json.has("id") ? json.get("id").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_NAME}", json.has("name") ? json.get("name").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_OWNER}", json.has("owner") ? json.get("owner").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_URL}", json.has("_links") ? json.get("_links").get("href").get("self").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_PROTOCOL}", json.has("protocol") ? json.get("protocol").asText() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_CREATED}", json.has("created") ? json.get("created").asText(): "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_EVENTS}", json.has("events") ? json.get("events").toString() : "");
                        body = StringUtils.replace(body, "${REALTIMECHANNEL_JSON}", json.toString());
			        }
    			    body = StringUtils.replace(body, "${RAW_JSON}", mapper.writer().withDefaultPrettyPrinter().writeValueAsString(getCustomNotificationMessageContextData()));
			    } catch (Exception e) {
			        body = StringUtils.replace(body, "${RAW_JSON}", getCustomNotificationMessageContextData());
			    }
			}
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of realtime channel with uuid " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}	
}
