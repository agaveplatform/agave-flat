/**
 * 
 */
package org.iplantc.service.systems;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ietf.jgss.GSSCredential;

import java.util.*;

/**
 * @author dooley
 * 
 */
public class Settings {

	private static final Logger log = Logger.getLogger(Settings.class);

	private static Properties					props			= new Properties();

	private static final Map<String, GSSCredential>	userProxies		= Collections
																		.synchronizedMap(new HashMap<String, GSSCredential>());

	/* Trusted user settings */
	public static List<String> 					TRUSTED_USERS = new ArrayList<String>();
	
	public static String						HOSTNAME;
	public static String						AUTH_SOURCE;
	
	public static String						API_VERSION;
	public static String						SERVICE_VERSION;
	
	/* Community user credentials */
	public static String						COMMUNITY_USERNAME;
	public static String						COMMUNITY_PASSWORD;

	/* Authentication service settings */
	public static String 						IPLANT_AUTH_SERVICE;
	public static String						IPLANT_MYPROXY_SERVER;
	public static int							IPLANT_MYPROXY_PORT;
	public static String						IPLANT_LDAP_URL;
	public static String						IPLANT_LDAP_BASE_DN;
	public static String						KEYSTORE_PATH;
	public static String						TRUSTSTORE_PATH;
	public static String						TRUSTED_CA_CERTS_DIRECTORY;
	public static String						MAIL_SERVER;
	public static String 						MAILSMTPSPROTOCOL;
	public static String 						MAILLOGIN;    
    public static String 						MAILPASSWORD;
    
	/* Data service settings */
	public static String						TEMP_DIRECTORY;

	/* Iplant API service endpoints */
	public static String						IPLANT_IO_SERVICE;
	public static String						IPLANT_JOB_SERVICE;
	public static String						IPLANT_PROFILE_SERVICE;
//	public static String						IPLANT_ATMOSPHERE_SERVICE;
	public static String						IPLANT_LOG_SERVICE;
	public static String 						IPLANT_SYSTEM_SERVICE;
	public static String 						IPLANT_METADATA_SERVICE;
	
	/* Job service settings */
	public static boolean						DEBUG;
	public static String						DEBUG_USERNAME;


	public static boolean						SLAVE_MODE;
//	public static int							REFRESH_RATE	= 0;
	
	public static String						IRODS_USERNAME;
	public static String						IRODS_PASSWORD;
	public static String						IRODS_HOST;
	public static int							IRODS_PORT;
	public static String						IRODS_ZONE;
	public static String						IRODS_STAGING_DIRECTORY;
	public static String						IRODS_DEFAULT_RESOURCE;
//	public static int							IRODS_REFRESH_RATE;	// how often to check the irods connection info
	public static String						PUBLIC_USER_USERNAME;
	public static String						WORLD_USER_USERNAME;

    public static Integer 						DEFAULT_PAGE_SIZE;

	public static String 						IPLANT_DOCS;

	public static long							MAX_REMOTE_OPERATION_TIME;
	
	static
	{
		props = org.iplantc.service.common.Settings.loadRuntimeProperties();
		
		HOSTNAME = org.iplantc.service.common.Settings.getLocalHostname();

		API_VERSION = props.getProperty("iplant.api.version");
		
		SERVICE_VERSION = props.getProperty("iplant.service.version");
		
		COMMUNITY_USERNAME = (String) props.get("iplant.community.username");

		COMMUNITY_PASSWORD = (String) props.get("iplant.community.password");

		String trustedUsers = (String)props.get("iplant.trusted.users");
		if (!StringUtils.isEmpty(trustedUsers)) {
			for (String user: trustedUsers.split(",")) {
				TRUSTED_USERS.add(user);
			}
		}
		
		SERVICE_VERSION = props.getProperty("iplant.service.version");
		
		IPLANT_AUTH_SERVICE = (String)props.get("iplant.auth.service");

		try {
		    IPLANT_MYPROXY_SERVER = (String) props.get("iplant.myproxy.server");
		} catch (Exception e) {
			log.error("Failure loading setting iplant.myproxy.server", e);
			IPLANT_MYPROXY_SERVER = "myproxy.xsede.org";
		}

		try {
			IPLANT_MYPROXY_PORT = Integer.valueOf((String) props.get("iplant.myproxy.port"));
		} catch (Exception e) {
			log.error("Failure loading setting iplant.myproxy.port", e);
			IPLANT_MYPROXY_PORT = 7512;
		}

		IPLANT_LDAP_URL = (String) props.get("iplant.ldap.url");

		IPLANT_LDAP_BASE_DN = (String) props.get("iplant.ldap.base.dn");

		TEMP_DIRECTORY = (String) props.get("iplant.server.temp.dir");
		
		IPLANT_SYSTEM_SERVICE = (String) props.get("iplant.system.service");
		if (!IPLANT_SYSTEM_SERVICE.endsWith("/")) IPLANT_SYSTEM_SERVICE += "/";
		
		IPLANT_IO_SERVICE = (String) props.get("iplant.io.service");
		if (!IPLANT_IO_SERVICE.endsWith("/")) IPLANT_IO_SERVICE += "/";
		
		IPLANT_JOB_SERVICE = (String) props.get("iplant.job.service");
		if (!IPLANT_JOB_SERVICE.endsWith("/")) IPLANT_JOB_SERVICE += "/";

		IPLANT_PROFILE_SERVICE = (String) props.get("iplant.profile.service");
		if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";
//
//		IPLANT_ATMOSPHERE_SERVICE = (String) props.get("iplant.atmosphere.service");
//		if (!IPLANT_ATMOSPHERE_SERVICE.endsWith("/")) IPLANT_ATMOSPHERE_SERVICE += "/";
		
		IPLANT_LOG_SERVICE = (String) props.get("iplant.log.service");
		if (!IPLANT_LOG_SERVICE.endsWith("/")) IPLANT_LOG_SERVICE += "/";
		
		IPLANT_METADATA_SERVICE = (String) props.get("iplant.metadata.service");
		if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_DOCS = (String) props.get("iplant.service.documentation");
		if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		DEBUG = Boolean.valueOf((String) props.get("iplant.debug.mode"));

		DEBUG_USERNAME = (String) props.get("iplant.debug.username");

		
		KEYSTORE_PATH = (String) props.get("system.keystore.path");

		TRUSTSTORE_PATH = (String) props.get("system.truststore.path");
		
		TRUSTED_CA_CERTS_DIRECTORY = (String) props.get("system.ca.certs.path");
		
		MAIL_SERVER = props.getProperty("mail.smtps.host");
		
		MAILSMTPSPROTOCOL = props.getProperty("mail.smtps.auth");
		
		MAILLOGIN = props.getProperty("mail.smtps.user");
		
		MAILPASSWORD = props.getProperty("mail.smtps.passwd");

//		REFRESH_RATE = Integer.valueOf((String) props.get("iplant.refresh.interval"));
		
		SLAVE_MODE = Boolean.valueOf((String) props.get("iplant.slave.mode"));
		
		IRODS_USERNAME = (String) props.get("iplant.irods.username");

		IRODS_PASSWORD = (String) props.get("iplant.irods.password");

		IRODS_HOST = (String) props.get("iplant.irods.host");

		try {
            IRODS_PORT = Integer.valueOf((String) props.get("iplant.irods.port"));
        } catch (Exception e){
			log.error("Failure loading setting iplant.myproxy.server", e);
			IRODS_PORT = 1247;
        }

		IRODS_ZONE = (String) props.get("iplant.irods.zone");

		IRODS_STAGING_DIRECTORY = (String) props
				.get("iplant.irods.staging.directory");

		IRODS_DEFAULT_RESOURCE = (String) props
				.get("iplant.irods.default.resource");

//		IRODS_REFRESH_RATE = Integer.valueOf((String) props.get("iplant.irods.refresh.interval"));

		PUBLIC_USER_USERNAME = (String) props.get("iplant.public.user");

		WORLD_USER_USERNAME = (String) props.get("iplant.world.user");

		try {
			DEFAULT_PAGE_SIZE = Integer.parseInt(props.getProperty("iplant.default.page.size", "25"));
		} catch (Exception e){
			log.error("Failure loading setting iplant.default.page.size", e);
			DEFAULT_PAGE_SIZE = 25;
		}

		try {
			MAX_REMOTE_OPERATION_TIME = Integer.parseInt(props.getProperty("iplant.max.remote.connection.time", "90"));
		} catch (Exception e){
			log.error("Failure loading setting iplant.max.remote.connection.time", e);
			MAX_REMOTE_OPERATION_TIME = 90;
		}
	}

}
