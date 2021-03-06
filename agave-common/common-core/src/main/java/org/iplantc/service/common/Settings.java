package org.iplantc.service.common;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.util.IPAddressValidator;
import org.iplantc.service.common.util.OSValidator;
import org.joda.time.DateTimeZone;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;


public class Settings 
{
    private static final Logger log = Logger.getLogger(Settings.class);
    
    protected static final String PROPERTY_FILE = "service.properties";
    
    public static final String INVALID_QUEUE_TASK_TENANT_ID = "!!";

	/* Debug settings */
    public static boolean       DEBUG;
    public static String        DEBUG_USERNAME;
    
    public static String        AUTH_SOURCE;
    public static boolean		VERIFY_JWT_SIGNATURE;
    
    public static String        API_VERSION;
    public static String        SERVICE_VERSION;
    
    public static String        PUBLIC_USER_USERNAME;
    public static String        WORLD_USER_USERNAME;
    
    
    /* Trusted user settings */
    public static List<String>  TRUSTED_USERS = new ArrayList<String>();
    
    /* Community user credentials */
    public static String        COMMUNITY_PROXY_USERNAME;
    public static String        COMMUNITY_PROXY_PASSWORD;
    
    /* Authentication service settings */
    public static String        IPLANT_LDAP_URL;
    public static String        IPLANT_LDAP_BASE_DN;
    public static String        IPLANT_LDAP_PASSWORD;
    public static String        IPLANT_LDAP_USERNAME;
    public static String        KEYSTORE_PATH;
    public static String        TRUSTSTORE_PATH;
    public static String        TACC_MYPROXY_SERVER;
    public static int           TACC_MYPROXY_PORT;
    public static String        IPLANT_AUTH_SERVICE;
    public static String        IPLANT_APP_SERVICE;
    public static String        IPLANT_JOB_SERVICE;
    public static String        IPLANT_FILE_SERVICE;
    public static String        IPLANT_LOG_SERVICE;
    public static String        IPLANT_METADATA_SERVICE;
    public static String        IPLANT_MONITOR_SERVICE;
    public static String        IPLANT_PROFILE_SERVICE;
    public static String        IPLANT_SYSTEM_SERVICE;
    public static String        IPLANT_TRANSFER_SERVICE;
    public static String        IPLANT_NOTIFICATION_SERVICE;
    public static String        IPLANT_POSTIT_SERVICE;
    public static String        IPLANT_TENANTS_SERVICE;
    public static String        IPLANT_ROLES_SERVICE;
    public static String        IPLANT_PERMISSIONS_SERVICE;
    public static String        IPLANT_TAGS_SERVICE;
    public static String        IPLANT_GROUPS_SERVICE;
    public static String		IPLANT_REALTIME_SERVICE;
    public static String		IPLANT_CLIENTS_SERVICE;
    public static String 		IPLANT_REACTOR_SERVICE;
    public static String 		IPLANT_ABACO_SERVICE;
    public static String 		IPLANT_REPOSITORY_SERVICE;
    
    /* iPlant dependent services */
    public static String        QUERY_URL;
    public static String        QUERY_URL_USERNAME;
    public static String        QUERY_URL_PASSWORD;
    
    /* Database Settings */
    public static String        SSH_TUNNEL_USERNAME;
    public static String        SSH_TUNNEL_PASSWORD;
    public static String        SSH_TUNNEL_HOST;
    public static int           SSH_TUNNEL_PORT;
    public static String        DB_USERNAME;
    public static String        DB_PASSWORD;
    public static String        DB_NAME;
    public static boolean       USE_SSH_TUNNEL;
    public static Integer       DEFAULT_PAGE_SIZE;
    public static Integer		MAX_PAGE_SIZE;
    public static String        IPLANT_DOCS;
    
    /* Global notifications */
    public static String                        NOTIFICATION_QUEUE;
    public static String                        NOTIFICATION_TOPIC;
    public static String                        NOTIFICATION_RETRY_QUEUE;
    public static String                        NOTIFICATION_RETRY_TOPIC;

    /* Metadata support */
    public static String 						METADATA_DB_HOST;
    public static String 						METADATA_DB_SCHEME;
    public static String 						METADATA_DB_COLLECTION;
    public static String                        METADATA_DB_SCHEMATA_COLLECTION;
    public static int                           METADATA_DB_PORT;
    public static String                        METADATA_DB_USER;
    public static String                        METADATA_DB_PWD;

    /* Notification retry */
    public static String 						FAILED_NOTIFICATION_DB_HOST;
    public static String 						FAILED_NOTIFICATION_DB_SCHEME;
    public static int                           FAILED_NOTIFICATION_DB_PORT;
    public static String                        FAILED_NOTIFICATION_DB_USER;
    public static String                        FAILED_NOTIFICATION_DB_PWD;
    public static int                           FAILED_NOTIFICATION_COLLECTION_LIMIT;
    public static int                           FAILED_NOTIFICATION_COLLECTION_SIZE;

    /* Messaging support */
    public static String                        MESSAGING_SERVICE_PROVIDER;
    public static String                        MESSAGING_SERVICE_HOST;
    public static int                           MESSAGING_SERVICE_PORT;
    public static String                        MESSAGING_SERVICE_USERNAME;
    public static String                        MESSAGING_SERVICE_PASSWORD;
    
    /* API specific queues */
    public static String                        FILES_ENCODING_QUEUE;
    public static String                        FILES_STAGING_QUEUE;
    public static String                        FILES_STAGING_TOPIC;
    public static String                        FILES_ENCODING_TOPIC;
    
    public static String                        TRANSFERS_ENCODING_QUEUE;
    public static String                        TRANSFERS_ENCODING_TOPIC;
    public static String                        TRANSFERS_DECODING_QUEUE;
    public static String                        TRANSFERS_DECODING_TOPIC;
    public static String                        TRANSFERS_STAGING_TOPIC;
    public static String                        TRANSFERS_STAGING_QUEUE;
    
    public static String                        JOBS_STAGING_QUEUE;
    public static String                        JOBS_STAGING_TOPIC;
    public static String                        JOBS_SUBMISSION_QUEUE;
    public static String                        JOBS_SUBMISSION_TOPIC;
    public static String                        JOBS_MONITORING_QUEUE;
    public static String                        JOBS_MONITORING_TOPIC;
    public static String                        JOBS_ARCHIVING_QUEUE;
    public static String                        JOBS_ARCHIVING_TOPIC;

    public static String                        MONITORS_CHECK_QUEUE;
    public static String                        MONITORS_CHECK_TOPIC;
    
    public static String                        APPS_PUBLISHING_QUEUE;
    public static String                        APPS_PUBLISHING_TOPIC;

    public static String                        USAGETRIGGERS_CHECK_QUEUE;
    public static String                        USAGETRIGGERS_CHECK_TOPIC;
    
    public static String                        PLATFORM_STORAGE_SYSTEM_ID;
    
    private static String                       DEDICATED_TENANT_ID;

    private static String[]                     DEDICATED_USER_IDS;

    private static String[]                     DEDICATED_USER_GROUPS;

    private static String[]                     DEDICATED_SYSTEM_IDS;

    private static String                       DRAIN_QUEUES;
        
    public static String						TEMP_DIRECTORY;
    
    static {
        // trust everyone. we need this due to the unknown nature of the callback urls
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());

        } catch (Exception e) { // should never happen 
            log.error("Unexpected error configuring HTTPS URL connection. Continuing initialization.");
        }

        Properties props = loadRuntimeProperties();

        DateTimeZone.setDefault(DateTimeZone.forID("America/Chicago"));
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        try {
            DEBUG = Boolean.valueOf(props.getProperty("iplant.debug.mode", "false"));
        } catch (Exception e) {
            log.error("Failure loading setting iplant.debug.mode.", e);
            DEBUG = false;
        }

        DEBUG_USERNAME = props.getProperty("iplant.debug.username");

        AUTH_SOURCE = props.getProperty("iplant.auth.source");

        try {
            VERIFY_JWT_SIGNATURE = Boolean.valueOf((String) props.getProperty("iplant.verify.jwt.signature", "no"));
        } catch (Exception e) {
            log.error("Failure loading setting iplant.verify.jwt.signature", e);
            VERIFY_JWT_SIGNATURE = false;
        }

        API_VERSION = props.getProperty("iplant.api.version");

        // ensure the default system temp directory is set to the TEMP_DIRECTORY value so that any containers
        // sharing a mounted directory will both have access to any temp directories created.
        TEMP_DIRECTORY = props.getProperty("iplant.server.temp.dir", "/scratch");
        System.setProperty("java.io.tmpdir", TEMP_DIRECTORY);
        try {
            // ensure the temp dir exists and is writeble. If not, none of our file operations are going to work
            // in a containerized setting.
            File tmpDir = Paths.get(TEMP_DIRECTORY).toFile();
            // create dir if not present
            if (!tmpDir.exists()) {
                // if unable to create it, throw an exception we can log and catch.
                if (!tmpDir.mkdirs()) {
                    throw new IOException("Failed to create missing temp directory");
                }
                else {
                    log.debug("Successfully created temp directory: " + TEMP_DIRECTORY);
                }
            } else {
                log.debug("Temp directory found: " + TEMP_DIRECTORY);
            }

            if (!tmpDir.canWrite()) {
                throw new PermissionException("Temp directory is not writeable");
            } else if (!tmpDir.canRead()) {
                throw new PermissionException("Temp directory is not readable");
            }
        } catch (IOException|PermissionException e) {
            log.error("Error found while validating temp directory, " + TEMP_DIRECTORY + ": " +
                    e.getMessage() + ". Some remote data movement operations may fail.");
        }

        SERVICE_VERSION = props.getProperty("iplant.service.version");

        IPLANT_LDAP_URL = props.getProperty("iplant.ldap.url");

        IPLANT_LDAP_BASE_DN = props.getProperty("iplant.ldap.base.dn");

        IPLANT_LDAP_PASSWORD = props.getProperty("iplant.ldap.password");

        IPLANT_LDAP_USERNAME = props.getProperty("iplant.ldap.username");

        PUBLIC_USER_USERNAME = props.getProperty("iplant.public.user", "public");

        WORLD_USER_USERNAME = props.getProperty("iplant.world.user", "world");

        COMMUNITY_PROXY_USERNAME = props.getProperty("iplant.community.username");

        COMMUNITY_PROXY_PASSWORD = props.getProperty("iplant.community.password");

        String trustedUsers = props.getProperty("iplant.trusted.users");
        if (trustedUsers != null && !trustedUsers.equals("")) {
            for (String user : trustedUsers.split(",")) {
                TRUSTED_USERS.add(user);
            }
        }

        KEYSTORE_PATH = props.getProperty("system.keystore.path");

        TRUSTSTORE_PATH = props.getProperty("system.truststore.path");

        TACC_MYPROXY_SERVER = props.getProperty("iplant.myproxy.server");

        try {
            TACC_MYPROXY_PORT = Integer.valueOf((String) props.getProperty("iplant.myproxy.port", "7512"));
        } catch (Exception e) {
            log.error("Failure loading setting iplant.myproxy.port - continuing using default value.", e);
            TACC_MYPROXY_PORT = 7512;
        }

        IPLANT_APP_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.app.service", "https://sandbox.agaveplatform.org/apps/v2");
        IPLANT_AUTH_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.auth.service", "https://sandbox.agaveplatform.org/auth/v2");
        IPLANT_CLIENTS_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.clients.service", "http://sandbox.agaveplatform.org/clients/v2");
        IPLANT_SYSTEM_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.system.service", "https://sandbox.agaveplatform.org/systems/v2");
        IPLANT_DOCS = Settings.getSantizedServiceUrl(props, "iplant.service.documentation", "https://sandbox.agaveplatform.org/docs/v2");
        IPLANT_FILE_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.io.service", "https://sandbox.agaveplatform.org/files/v2");
        IPLANT_GROUPS_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.groups.service", "https://sandbox.agaveplatform.org/groups/v2");
        IPLANT_JOB_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.job.service", "https://sandbox.agaveplatform.org/jobs/v2");
        IPLANT_LOG_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.log.service", "https://sandbox.agaveplatform.org/logging/v2");
        IPLANT_METADATA_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.metadata.service", "https://sandbox.agaveplatform.org/meta/v2");
        IPLANT_MONITOR_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.monitor.service", "https://sandbox.agaveplatform.org/monitors/v2");
        IPLANT_NOTIFICATION_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.notification.service", "https://sandbox.agaveplatform.org/notifications/v2");
        IPLANT_PERMISSIONS_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.permissions.service", "https://sandbox.agaveplatform.org/permissions/v2");
        IPLANT_POSTIT_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.postit.service", "https://sandbox.agaveplatform.org/postits/v2");
        IPLANT_PROFILE_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.profile.service", "https://sandbox.agaveplatform.org/profiles/v2");
        IPLANT_REALTIME_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.realtime.service", "http://public.realtime.sandbox.agaveplatform.org/channels/v2");
        IPLANT_ROLES_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.roles.service", "https://sandbox.agaveplatform.org/roles/v2");
        IPLANT_TAGS_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.tags.service", "https://sandbox.agaveplatform.org/tags/v2");
        IPLANT_TENANTS_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.tenants.service", "https://agaveplatform.org/tenants/");
        IPLANT_TRANSFER_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.transfer.service", "https://sandbox.agaveplatform.org/transfers/v2");
        IPLANT_REACTOR_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.reactors.service", "https://sandbox.agaveplatform.org/reactors/v2");
        IPLANT_REPOSITORY_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.repositories.service", "https://sandbox.agaveplatform.org/repositories/v2");
        IPLANT_ABACO_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.abaco.service", "https://sandbox.agaveplatform.org/abaco/v2");

        NOTIFICATION_QUEUE = props.getProperty("iplant.notification.service.queue", "prod.notifications.queue");
        NOTIFICATION_TOPIC = props.getProperty("iplant.notification.service.topic", "prod.notifications.queue");
        NOTIFICATION_RETRY_QUEUE = props.getProperty("iplant.notification.service.retry.queue", "retry." + NOTIFICATION_QUEUE);
        NOTIFICATION_RETRY_TOPIC = props.getProperty("iplant.notification.service.retry.topic", "retry." + NOTIFICATION_TOPIC);

        METADATA_DB_COLLECTION = (String) props.getProperty("iplant.metadata.db.collection", "metadata");
        METADATA_DB_SCHEMATA_COLLECTION = (String) props.getProperty("iplant.metadata.schemata.db.collection", "schemata");
        METADATA_DB_SCHEME = (String) props.getProperty("iplant.metadata.db.scheme", "api");
        METADATA_DB_HOST = (String) props.getProperty("iplant.metadata.db.host", "mongodb");
        METADATA_DB_PORT = Integer.parseInt((String) props.getProperty("iplant.metadata.db.port", "27017"));
        METADATA_DB_USER = (String) props.getProperty("iplant.metadata.db.user", "agaveuser");
        METADATA_DB_PWD = (String) props.getProperty("iplant.metadata.db.pwd", "password");

        FAILED_NOTIFICATION_DB_SCHEME = (String) props.getProperty("iplant.notification.failed.db.scheme", "api");
        FAILED_NOTIFICATION_DB_HOST = (String) props.getProperty("iplant.notification.failed.db.host", "mongodb");
        FAILED_NOTIFICATION_DB_PORT = org.apache.commons.lang.math.NumberUtils.toInt((String) props.getProperty("iplant.notification.failed.db.port", "27017"), 27017);
        FAILED_NOTIFICATION_DB_USER = (String) props.getProperty("iplant.notification.failed.db.user", "agaveuser");
        FAILED_NOTIFICATION_DB_PWD = (String) props.getProperty("iplant.notification.failed.db.pwd", "password");
        FAILED_NOTIFICATION_COLLECTION_SIZE = org.apache.commons.lang.math.NumberUtils.toInt((String) props.getProperty("iplant.notification.failed.db.max.queue.size"), 1048576);
        FAILED_NOTIFICATION_COLLECTION_LIMIT = org.apache.commons.lang.math.NumberUtils.toInt((String) props.getProperty("iplant.notification.failed.db.max.queue.limit"), 1000);

        MESSAGING_SERVICE_PROVIDER = props.getProperty("iplant.messaging.provider");
        MESSAGING_SERVICE_USERNAME = props.getProperty("iplant.messaging.username");
        MESSAGING_SERVICE_PASSWORD = props.getProperty("iplant.messaging.password");
        MESSAGING_SERVICE_HOST = props.getProperty("iplant.messaging.host");
        try {
            MESSAGING_SERVICE_PORT = Integer.parseInt((String) props.get("iplant.messaging.port"));
        } catch (Exception e) {
            log.error("Failure loading setting iplant.messaging.port - continuing.", e);
        }
        
        FILES_ENCODING_QUEUE = props.getProperty("iplant.files.service.encoding.queue", "encoding.prod.files.queue");
        FILES_ENCODING_TOPIC = props.getProperty("iplant.files.service.encoding.topic", "encoding.prod.files.topic");
        FILES_STAGING_QUEUE = props.getProperty("iplant.files.service.staging.queue", "staging.prod.files.queue");
        FILES_STAGING_TOPIC = props.getProperty("iplant.files.service.staging.topic", "staging.prod.files.topic");
        
        TRANSFERS_DECODING_QUEUE = props.getProperty("iplant.transfers.service.decoding.queue", "decoding.prod.transfers.queue");
        TRANSFERS_DECODING_TOPIC = props.getProperty("iplant.transfers.service.decoding.topic", "decoding.prod.transfers.topic");
        TRANSFERS_ENCODING_QUEUE = props.getProperty("iplant.transfers.service.encoding.queue", "encoding.prod.transfers.queue");
        TRANSFERS_ENCODING_TOPIC = props.getProperty("iplant.transfers.service.encoding.topic", "encoding.prod.transfers.topic");
        TRANSFERS_STAGING_QUEUE = props.getProperty("iplant.transfers.service.staging.queue", "staging.prod.transfers.queue");
        TRANSFERS_STAGING_TOPIC = props.getProperty("iplant.transfers.service.staging.topic", "staging.prod.transfers.topic");
        
        JOBS_STAGING_QUEUE = props.getProperty("iplant.jobs.service.staging.queue", "staging.prod.jobs.queue");
        JOBS_STAGING_TOPIC = props.getProperty("iplant.jobs.service.staging.topic", "staging.prod.jobs.topic");
        JOBS_SUBMISSION_QUEUE = props.getProperty("iplant.jobs.service.submission.queue", "submission.prod.jobs.queue");
        JOBS_SUBMISSION_TOPIC = props.getProperty("iplant.jobs.service.submission.topic", "submission.prod.jobs.topic");
        JOBS_MONITORING_QUEUE = props.getProperty("iplant.jobs.service.monitoring.queue", "monitoring.prod.jobs.queue");
        JOBS_MONITORING_TOPIC = props.getProperty("iplant.jobs.service.monitoring.topic", "monitoring.prod.jobs.topic");
        JOBS_ARCHIVING_QUEUE = props.getProperty("iplant.jobs.service.archiving.queue", "archiving.prod.jobs.queue");
        JOBS_ARCHIVING_TOPIC = props.getProperty("iplant.jobs.service.archiving.topic", "archiving.prod.jobs.topic");
        
        MONITORS_CHECK_QUEUE = props.getProperty("iplant.monitors.service.checks.queue", "checks.prod.monitors.queue");
        MONITORS_CHECK_TOPIC = props.getProperty("iplant.monitors.service.checks.topic", "checks.prod.monitors.topic");
        
        APPS_PUBLISHING_QUEUE = props.getProperty("iplant.apps.service.publishing.queue", "publish.prod.apps.queue");
        APPS_PUBLISHING_TOPIC = props.getProperty("iplant.apps.service.publishing.topic", "publish.prod.apps.topic");
        
        USAGETRIGGERS_CHECK_QUEUE = props.getProperty("iplant.usagetriggers.service.queue", "check.prod.usagetriggers.queue");
        USAGETRIGGERS_CHECK_TOPIC = props.getProperty("iplant.usagetriggers.service.topic", "check.prod.usagetriggers.topic");

        
        try {DEFAULT_PAGE_SIZE = NumberUtils.toInt((String) props.getProperty("iplant.default.page.size", "100"), 100);}
        	catch (Exception e) {
        		log.error("Failure loading setting iplant.default.page.size", e);
        		DEFAULT_PAGE_SIZE = 100;
        	}
        
        try {MAX_PAGE_SIZE = NumberUtils.toInt((String) props.getProperty("iplant.max.page.size", "100"), 100);}
    	catch (Exception e) {
    		log.error("Failure loading setting iplant.max.page.size", e);
    		DEFAULT_PAGE_SIZE = 100;
    	}
        
        PLATFORM_STORAGE_SYSTEM_ID = props.getProperty("iplant.platform.storage.system", "agave-s3-prod");
        
        DRAIN_QUEUES = props.getProperty("iplant.drain.all.queues");
        
        DEDICATED_TENANT_ID = props.getProperty("iplant.dedicated.tenant.id");
        
        DEDICATED_USER_IDS = StringUtils.split(props.getProperty("iplant.dedicated.user.id"), ",");
        
        DEDICATED_USER_GROUPS = StringUtils.split(props.getProperty("iplant.dedicated.user.group.id"), ",");
        
        DEDICATED_SYSTEM_IDS = StringUtils.split(props.getProperty("iplant.dedicated.system.id", ""), ",");
    }
    
    /**
     * Reads in the service.properties file from disk and merges it with
     * any environment variables injected at runtime. 
     * 
     * @return {@link Properties} object containing the runtime properties
     * to be used by this app.
     */
    public static Properties loadRuntimeProperties()
    {
        Properties props = new Properties();
        InputStream stream = null;
        try {
        	stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
            props.load(stream);
        }
        catch (Exception e) {
        	String msg = "Unable to load " + PROPERTY_FILE + ".";
            log.error(msg, e);
        }
        finally {
        	if (stream != null) try {stream.close();} catch (Exception e){}
        }
        
        Map<String, String> environment = System.getenv();
        for (String varName : environment.keySet())
        {
            if (StringUtils.isBlank(varName)) continue;
            
            String propName = varName.toLowerCase().replaceAll("_", ".");
            
            props.remove(propName);
            props.setProperty(propName, environment.get(varName));
        }
        
        return props;
    }
    
    /**
     * Reads in a service url from the properties file and ensures it ends in a 
     * single trailing slash.
     * @param props the properties to fetch the service url from.
     * @param propertyKey the key to lookup
     * @param defaultValue value to use if no value is present in the properties object
     * @return
     */
    public static String getSantizedServiceUrl(Properties props, String propertyKey, String defaultValue) {
    	return StringUtils.trimToEmpty(
    			props.getProperty(propertyKey, defaultValue)).replaceAll("/$", "") + "/";
	}

	/**
     * Reads in the tenant id every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * @return the Tenant.tenantCode
     */
    public static synchronized String getDedicatedTenantIdFromServiceProperties()
    {
        if (StringUtils.isEmpty(DEDICATED_TENANT_ID)) 
        {
            Properties props = new Properties();
            InputStream stream = null;
            try
            {
            	stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
                props.load(stream);
                
                DEDICATED_TENANT_ID = props.getProperty("iplant.dedicated.tenant.id");
            }
            catch (Exception e)
            {
            	log.error("Failure loading setting iplant.dedicated.tenant.id", e);
                DEDICATED_TENANT_ID = null;
            }
            finally {
            	if (stream != null) try {stream.close();} catch (Exception e){}
            }
        }
        
        return StringUtils.stripToNull(DEDICATED_TENANT_ID);
    }
    
    /**
     * Reads in the dedicated username every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * @return an array of usernames
     */
    public static String[] getDedicatedUsernamesFromServiceProperties()
    {
        if (ArrayUtils.isEmpty(DEDICATED_USER_IDS)) 
        {
            Properties props = new Properties();
            InputStream stream = null;
            try
            {
            	stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
                props.load(stream);
                
                List<String> userIds = new ArrayList<String>();
                for (String userId: StringUtils.split((String)props.getProperty("iplant.dedicated.user.id", ""), ",")) {
                    userId = StringUtils.trimToNull(userId);
                    if (userId != null) {
                        userIds.add(userId);
                    }
                }
                
                String[] stringArray = new String[userIds.size()];
                DEDICATED_USER_IDS = userIds.toArray(stringArray);
                
            }
            catch (Exception e)
            {
            	log.error("Failure loading setting iplant.dedicated.user.id", e);
                DEDICATED_USER_IDS = new String[] {};
            }
            finally {
            	if (stream != null) try {stream.close();} catch (Exception e){}
            }
        }
        
        return (String[])ArrayUtils.clone(DEDICATED_USER_IDS);
    }
    
    /**
     * Reads in the dedicated username every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * @return an array of user group names
     */
    public static String[] getDedicatedUserGroupsFromServiceProperties()
    {
        if (ArrayUtils.isEmpty(DEDICATED_USER_GROUPS)) 
        {
            Properties props = new Properties();
            InputStream stream = null;
            try
            {
            	stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
                props.load(stream);
                
                List<String> groupIds = new ArrayList<String>();
                for (String groupId: StringUtils.split((String)props.getProperty("iplant.dedicated.user.group", ""), ",")) {
                    groupId = StringUtils.trimToNull(groupId);
                    if (groupId != null) {
                        groupIds.add(groupId);
                    }
                }
                
                String[] stringArray = new String[groupIds.size()];
                DEDICATED_USER_GROUPS = groupIds.toArray(stringArray);
            }
            catch (Exception e)
            {
            	log.error("Failure loading setting iplant.dedicated.user.group", e);
                DEDICATED_USER_GROUPS = new String[] {};
            }
            finally {
            	if (stream != null) try {stream.close();} catch (Exception e){}
            }
        }
        
        return (String[])ArrayUtils.clone(DEDICATED_USER_GROUPS);
    }
    
    /**
     * Reads in the system ids every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * Multiple systems may be specified as a comma-delimited list.
     * If further isolation at the BatchQueue level may be obtained by 
     * providing the queue name in {@code systemId#queueName} format.
     *  
     * @return an array of system ids
     */
    public static String[] getDedicatedSystemIdsFromServiceProperties()
    {
        if (ArrayUtils.isEmpty(DEDICATED_SYSTEM_IDS)) 
        {
            Properties props = new Properties();
            InputStream stream = null;
            try
            {
            	stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
                props.load(stream);
                
                List<String> systemIds = new ArrayList<String>();
                for (String systemId: StringUtils.split((String)props.getProperty("iplant.dedicated.system.id", ""), ",")) {
                    systemId = StringUtils.trimToNull(systemId);
                    if (systemId != null) {
                        systemIds.add(systemId);
                    }
                }
                
                String[] stringArray = new String[systemIds.size()];
                DEDICATED_SYSTEM_IDS = systemIds.toArray(stringArray);
            }
            catch (Exception e)
            {
            	log.error("Failure loading setting iplant.dedicated.system.id", e);
                DEDICATED_SYSTEM_IDS = new String[] {};
            }
            finally {
            	if (stream != null) try {stream.close();} catch (Exception e){}
            }
        }
    
        return (String[])ArrayUtils.clone(DEDICATED_SYSTEM_IDS);
    }
    
    /**
     * Reads in the iplant.dedicated.tenant.id property every time from 
     * the service properties file. This can be updated in real time to 
     * prevent workers from accepting any more work at run time.
     * 
     * @return true if iplant.dedicated.tenant.id is true, false otherwise
     */
    public static boolean isDrainingQueuesEnabled()
    {
        if (StringUtils.isEmpty(DRAIN_QUEUES)) 
        {
            Properties props = new Properties();
            InputStream stream = null;
            try
            {
            	stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
                props.load(stream);
                
                DRAIN_QUEUES = props.getProperty("iplant.drain.all.queues");
            }
            catch (Exception e)
            {
            	log.error("Failure loading setting iplant.drain.all.queues", e);
                DRAIN_QUEUES = null;
            }
            finally {
            	if (stream != null) try {stream.close();} catch (Exception e){}
            }
        }
        
        return Boolean.parseBoolean(DRAIN_QUEUES);
    }
    
    /**
     * Returns the local hostname by resolving the HOSTNAME environment variable.
     * If that variable is not available, the {@link InetAddress} class is used
     * to resolve it from the system.
     *  
     * @return hostname of current running process
     */
    public static String getLocalHostname()
    {
        String hostname = System.getenv("HOSTNAME");
        
        if (StringUtils.isBlank(hostname)) 
        {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.error("Unable to resolve local hostname", e);
                hostname = "localhost";
            }
        }
        
        return hostname;
    }

    /**
     * Returns the ip address of the container/host. 127.0.0.1 if the ip cannot
     * be determined.
     * 
     * @return host ip or 127.0.0.1
     */
    public static String getIpLocalAddress() 
    {
        IPAddressValidator ipAddressValidator = null;
        try {
            ipAddressValidator = new IPAddressValidator();
            
            // try to resolve via service discovery first
            String hostname = System.getenv("TUTUM_CONTAINER_FQDN");
            if (StringUtils.isNotEmpty(hostname)) 
            {
                InetAddress ip = null;
                try {
                    ip = InetAddress.getByName(hostname);
                    if (!ip.isLoopbackAddress() && !ip.isAnyLocalAddress()) {
                        return ip.getHostAddress();
                    }
                } catch (Exception e) {}
            } 
            
            if (OSValidator.isMac() || OSValidator.isUnix()) 
            {
                // check for the hostname variable and try to resolve that
                hostname = System.getenv("HOSTNAME");
                if (StringUtils.isNotEmpty(hostname)) {
                    InetAddress ip = null;
                    try {
                        ip = InetAddress.getByName(hostname);
                        if (!ip.isLoopbackAddress() && !ip.isAnyLocalAddress()) {
                            return ip.getHostAddress();
                        }
                    } catch (Exception e) {}
                }
                
                // ip could not be found from the environment, check ifconfig
                List<String> ipAddresses = Settings.getIpAddressesFromNetInterface();
                if (!ipAddresses.isEmpty()) {
                    if (ipAddresses.contains("192.168.59.3")) {
                        return "192.168.59.3";
                    }
                    return ipAddresses.get(0);
                } 
                
                // ifconfig failed. try netstat
                ipAddresses = Settings.fork("netstat -rn | grep 'default' | awk '{print $2}' | head -n 1");
                if (!ipAddresses.isEmpty()) {
                    for(String ip: ipAddresses) {
                        if (ipAddressValidator.validate(StringUtils.trimToEmpty(ip))) {
                            return ip;
                        }
                    }
                }
            }
            
            return "127.0.0.1";
        }
        catch (Exception e) {
            log.error("Failed to retrieve local ip address. Some processes may not operate correctly.", e);
            return "127.0.0.1";
        }
    }
    
    /**
     * Helper to fork commands and return response as a newline
     * delimited list.
     * 
     * @param command
     * @return response broken up by newline characters.
     */
    public static List<String> fork(String command) 
    {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        String[] cmd = { "sh", "-c", command };
        
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
             
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
         
            String line = "";
            while ((line = reader.readLine())!= null) {
                lines.add(line);
            }
        }
        catch (IOException | InterruptedException e) {
            log.error("Failed to fork command " + command);
        }
        finally {
            if (reader != null) try {reader.close();} catch (Exception e) {}
        }
        
        return lines;
    }
    
    /**
     * Parses local network interface and returns a list of hostnames
     * for 
     * @return list of ip addresses for the host machine
     */
    public static List<String> getIpAddressesFromNetInterface() 
    {
        List<String> ipAddresses = new ArrayList<String>();
        IPAddressValidator ipAddressValidator = new IPAddressValidator();
        try 
        {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) 
            {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && !ni.isPointToPoint() && !ni.isVirtual()) 
                {
                    Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) 
                    {
                        InetAddress add = inetAddresses.nextElement();
                        String ip = add.getHostAddress();
                        if (ipAddressValidator.validate(ip) 
                                && !StringUtils.startsWithAny(ip, new String[]{"127", ":", "0"})) 
                        {
                            ipAddresses.add(ip);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            log.error("Unable to resolve local ip address from network interface. Passive connections may fail.");
        }
        
        return ipAddresses;
    }
    
    /**
     * Returns the container id from the local environment. If this cannot
     * be found (eg. running standard docker) then null is returned.
     * @return the Docker container id or null if it cannot be found.
     */
    public static String getContainerId() {
        
        String tutumNodeApiUri = System.getenv("TUTUM_CONTAINER_API_URI");
        // look for container id in tutum environment
        if (StringUtils.isNotEmpty(tutumNodeApiUri)) {
            // container id is last token in uri
            // ex. /api/v1/container/f17015ce-d907-48d7-87e8-0521aa2c67e6/
            tutumNodeApiUri = StringUtils.removeEnd(tutumNodeApiUri, "/");
            return StringUtils.substringAfterLast(tutumNodeApiUri, "/");
        }
        // look for container id in docker environment...probably not there
        else 
        {
            return null;
        }
    }
    
    public static List<String> getEditableRuntimeConfigurationFields() {
        return Arrays.asList(
        		"DRAIN_QUEUES",
                "DEDICATED_TENANT_ID",
                "DEDICATED_SYSTEM_IDS",
                "DEDICATED_USER_IDS",
                "DEDICATED_USER_GROUPS");
    }
    
    /** Retrieve and parse the queue task tenant ids if they were assigned.
     * If unassigned, return an empty array. 
     * 
     * @return a non-null string array of tenant ids
     */
    public static String[] getQueueTaskTenantIds(Properties props)
    {
        // See if the queue task tenant ids property was assigned.
        String inputIds = props.getProperty("iplant.queuetask.tenant.ids");
        if (StringUtils.isBlank(inputIds)) {
            if (log.isInfoEnabled()) {
                String msg = "No tenant id specified for queue tasks, using all tenants.";
                log.info(msg);
            }
            return new String[] {};
        }
        
        // Parse the comma separated list of tenant ids.  During the 
        // parsing we validate that the list only contains assertions
        // or negations, but not both.
        boolean hasAssertion = false;
        boolean hasNegation  = false;
        List<String> tenantIds = new ArrayList<String>();
        for (String tenantId: StringUtils.split(inputIds, ",")) {
            tenantId = StringUtils.trimToNull(tenantId);
            if (tenantId != null) {
                // Record assertion/negation type.
                if (tenantId.startsWith("!")) hasNegation = true;
                 else hasAssertion = true;
                tenantIds.add(tenantId);
            }
        }
        
        // Validate that all members are either assertions or negations.
        // If this condition does not hold, return an array with the
        // distinguished invalid tenant id to disable task queue queries.
        if (hasAssertion && hasNegation) {
            String msg = "The iplant.queuetask.tenant.ids property cannot contain " +
                         "both assertions and negations.  Please check the configuration " +
                         "for this service.  The received tenant id values will be ignored: " + 
                         inputIds;
            log.error(msg);
            return new String[] {INVALID_QUEUE_TASK_TENANT_ID};
        }
        
        // Tracing.
        if (log.isInfoEnabled()) {
            String msg = "Using the following tenant id configuration for queue tasks: " + inputIds;
            log.info(msg);
        }
        
        // Return the tenant ids in an array.
        String[] stringArray = new String[tenantIds.size()];
        return tenantIds.toArray(stringArray);
    }
}
