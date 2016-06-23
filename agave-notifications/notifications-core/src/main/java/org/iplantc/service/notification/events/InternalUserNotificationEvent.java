/**
 * 
 */
package org.iplantc.service.notification.events;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class InternalUserNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(InternalUserNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public InternalUserNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = "Username: ${USERNAME}\n" +
				"Email: ${EMAIL}\n" +
				"UUID: ${UUID}\n" +
				"First name: ${FIRST_NAME}\n" +
				"Last name: ${LAST_NAME}\n" +
				"Position: ${POSITION}\n" +
				"Institution: ${INSTITUTION}\n" +
				"Phone: ${PHONE}\n" +
				"Fax: ${FAX}\n" +
				"Research Area: ${RESEARCH_AREA}\n" +
				"Department: ${DEPARTMENT}\n" +
				"City: ${CITY}\n" +
				"State: ${STATE}\n" +
				"Country: ${COUNTRY}\n" + 
				"Gender: ${GENDER}\n" + 
				"Active: ${ACTIVE}";
		
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			body = "The following internal user was created: \n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "updated")) {
			body = "The following internal user was updated: \n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "The internal user ${USERNAME} was deleted.";
		}
		else
		{
			body = "The following internal user experienced a(n) ${EVENT} " +  
					" event. The current internal user profile is now: \n\n" + body;
		}	
		
		return resolveMacros(body, false);
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = "<p><strong>Username: ${USERNAME}<br>" +
                "<strong>Email:</strong> ${EMAIL}<br>" +
                "<strong>UUID:</strong> ${UUID}<br>" +
                "<strong>First name:</strong> ${FIRST_NAME}<br>" +
                "<strong>Last name:</strong> ${LAST_NAME}<br>" +
                "<strong>Position:</strong> ${POSITION}<br>" +
                "<strong>Institution:</strong> ${INSTITUTION}<br>" +
                "<strong>Phone:</strong> ${PHONE}<br>" +
                "<strong>Fax:</strong> ${FAX}<br>" +
                "<strong>Research Area:</strong> ${RESEARCH_AREA}<br>" +
                "<strong>Department:</strong> ${DEPARTMENT}<br>" +
                "<strong>City:</strong> ${CITY}<br>" +
                "<strong>State:</strong> ${STATE}<br>" +
                "<strong>Country:</strong> ${COUNTRY<br>" + 
                "<strong>Gender:</strong> ${GENDER}<br>" + 
                "<strong>Active:</strong> ${ACTIVE}</p>";
        
        if (StringUtils.equalsIgnoreCase(event, "created")) {
            body = "<p>The following internal user was created:</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "updated")) {
            body = "<p>The following internal user was updated:</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
            body = "<p>The internal user ${USERNAME} was deleted.</p>";
        }
        else
        {
            body = "<p>The following internal user experienced a(n) ${EVENT} " +  
                    " event. The current internal user profile is now: </p><br>" + body;
        }   
        
        return resolveMacros(body, false);
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Internal user ${USERNAME} was ${EVENT}", false);
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
			
			Map<String, Object> jobFieldMap = getJobRow("internalusers", associatedUuid.toString());
			
			body = StringUtils.replace(body, "${USERNAME}", (String)jobFieldMap.get("username"));
			body = StringUtils.replace(body, "${EMAIL}", (String)jobFieldMap.get("email"));
			body = StringUtils.replace(body, "${FIRST_NAME}", (String)jobFieldMap.get("first_name"));
			body = StringUtils.replace(body, "${LAST_NAME}", (String)jobFieldMap.get("last_name"));
			body = StringUtils.replace(body, "${POSITION}", (String)jobFieldMap.get("position"));
			body = StringUtils.replace(body, "${INSTITUTION}", (String)jobFieldMap.get("institution"));
			body = StringUtils.replace(body, "${PHONE}", (String)jobFieldMap.get("phone"));
			body = StringUtils.replace(body, "${FAX}", (String)jobFieldMap.get("fax"));
			body = StringUtils.replace(body, "${RESEARCH_AREA}", (String)jobFieldMap.get("research_area"));
			body = StringUtils.replace(body, "${DEPARTMENT}", (String)jobFieldMap.get("department"));
			body = StringUtils.replace(body, "${CITY}", (String)jobFieldMap.get("city"));
			body = StringUtils.replace(body, "${STATE}", (String)jobFieldMap.get("state"));
			body = StringUtils.replace(body, "${COUNTRY}", (String)jobFieldMap.get("country"));
			body = StringUtils.replace(body, "${GENDER}", ((Integer)jobFieldMap.get("gender")).toString());
			body = StringUtils.replace(body, "${ACTIVE}", ((Integer)jobFieldMap.get("currently_active")).toString());
			body = StringUtils.replace(body, "${LAST_UPDATED}", new DateTime(jobFieldMap.get("last_updated")).toString());
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of internal user " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}

}
