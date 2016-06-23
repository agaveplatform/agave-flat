/**
 * 
 */
package org.iplantc.service.notification.events;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class MetadataSchemaNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(MetadataSchemaNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public MetadataSchemaNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = "The following metadata schemata received a(n) " + event +  
					" event at "+ new DateTime().toString() + " from " + owner + ".\n\n";
		
		return resolveMacros(body, false);
	}
	
	@Override
    public String getHtmlEmailBody()
    {
        return "<p>" + getEmailBody() + "</p>";
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Metadata schemata ${UUID} received a(n) ${EVENT} event", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			body = StringUtils.replace(body,"${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body,"${EVENT}", event);
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create metadata schemata body", e);
			return "The status of metadata schemata " + associatedUuid.toString() +
					" has received a(n) " + event;
		}
	}

}
