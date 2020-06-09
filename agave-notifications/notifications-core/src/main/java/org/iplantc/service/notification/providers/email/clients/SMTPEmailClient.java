/**
 * 
 */
package org.iplantc.service.notification.providers.email.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.providers.email.EmailClient;

/**
 * @author dooley
 *
 */
public class SMTPEmailClient implements EmailClient {

    protected Map<String, String> customHeaders = new HashMap<String, String>();
    
    /* (non-Javadoc)
     * @see org.iplantc.service.notification.email.EmailClient#send(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody)
    throws NotificationException 
    {
        if (StringUtils.isEmpty(recipientAddress)) {
            throw new NotificationException("Email recipient address cannot be null.");
        }
        
        if (StringUtils.isEmpty(body)) {
            throw new NotificationException("Email body cannot be null.");
        }
        
        if (StringUtils.isEmpty(htmlBody)) {
            htmlBody = "<p><pre>" + body + "</pre></p>";
        }
        
        if (StringUtils.isEmpty(subject)) {
            throw new NotificationException("Email subject cannot be null.");
        }
        
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", Settings.SMTP_HOST_NAME);
        props.put("mail.smtp.port", Settings.SMTP_HOST_PORT);
        props.put("mail.smtp.auth", Boolean.toString(Settings.SMTP_AUTH_REQUIRED));

        Authenticator auth = new SMTPAuthenticator();
        Session mailSession = Session.getDefaultInstance(props, auth);
        
        // uncomment for debugging infos to stdout
        mailSession.setDebug(true);
        
        try 
        {
            Transport transport = mailSession.getTransport();
        
            MimeMessage message = new MimeMessage(mailSession);
    
            Multipart multipart = new MimeMultipart("alternative");
    
            BodyPart part1 = new MimeBodyPart();
            part1.setText(body);
    
            BodyPart part2 = new MimeBodyPart();
            part2.setContent(htmlBody, "text/html");
    
            multipart.addBodyPart(part1);
            multipart.addBodyPart(part2);
    
            message.setContent(multipart);
            message.setSubject(subject);
            
            message.setFrom(new InternetAddress(
                    Settings.SMTP_FROM_ADDRESS, Settings.SMTP_FROM_NAME));
            
            // add custom headers if present
            if (!getCustomHeaders().isEmpty()) {
                for (Entry<String,String> entry: getCustomHeaders().entrySet()) {
                    message.addHeader(entry.getKey(), entry.getValue());
                }
            }
            
            message.addRecipient(Message.RecipientType.TO,
                new InternetAddress(recipientAddress, recipientName));
            
            transport.connect();
            transport.sendMessage(message,
                message.getRecipients(Message.RecipientType.TO));
            transport.close();
        } 
        catch (NoSuchProviderException e) {
            throw new NotificationException("Failed to send email message due to unknown email provider.", e);
        } 
        catch (Throwable e) {
            throw new NotificationException("Failed to send email message due to internal error.", e);
        }
    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
           String username = Settings.SMTP_AUTH_USER;
           String password = Settings.SMTP_AUTH_PWD;
           return new PasswordAuthentication(username, password);
        }
    }

    /**
     * @return the customHeaders
     */
    public synchronized Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    /**
     * @param customHeaders the customHeaders to set
     */
    public synchronized void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

}
