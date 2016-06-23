/**
 * 
 */
package org.iplantc.service.notification.providers.email;

import java.util.Map;

import org.iplantc.service.notification.exceptions.NotificationException;

/**
 * @author dooley
 *
 */
public interface EmailClient {

    /**
     * Synchronously sends a multipart email in both html and plaintext format.
     * 
     * @param recipientName Full name of recipient (ex. John Smith)
     * @param recipientAddress email address of recipient
     * @param subject of the email
     * @param body of the email in plain text format.
     * @throws NotificationException
     */
    public void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody) throws NotificationException;
    
    
    /**
     * Setter for custom headers set using the conventions of the email
     * service provider.
     * 
     * @param headers
     */
    public void setCustomHeaders(Map<String, String> headers);
    
    /**
     * Getter for the custom headers applied to the email
     * 
     * @return an empty map if no headers have been sent
     */
    public Map<String, String> getCustomHeaders();
}
