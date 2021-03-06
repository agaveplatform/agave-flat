package org.iplantc.service.transfer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public abstract class RemoteDataClientPermissionProviderTest extends BaseTransferTestCase implements IRemoteDataClientPermissionProviderTest {
	private static final Logger	log	= Logger.getLogger(RemoteDataClientPermissionProviderTest.class);

	protected ThreadLocal<RemoteDataClient> threadClient = new ThreadLocal<>();
    
	protected String permissionDir;
	protected String permissionFile;
	
	/**
	 * Initializes the remote system, creating a temp folder to use for
	 * these test cases. Authorization occurs before after each test.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	protected void beforeClass() throws Exception {
		super.beforeClass();

		init();
	}

	/**
	 * Initialize storage system and permission directories
	 * @throws Exception if the client cannot be initialized
	 */
	protected void init() throws Exception {
		permissionDir = LOCAL_DIR_NAME;
		permissionFile = permissionDir + "/" + LOCAL_BINARY_FILE_NAME;
		
		JSONObject json = getSystemJson();
    	json.remove("id");
    	json.put("id", this.getClass().getSimpleName());
		StorageSystem system = StorageSystem.fromJSON(json);
    	system.setOwner(SYSTEM_USER);
    	String homeDir = system.getStorageConfig().getHomeDir();
        homeDir = StringUtils.isEmpty(homeDir) ? "" : homeDir;
        system.getStorageConfig().setHomeDir( homeDir + "/" + getClass().getSimpleName());
        this.system = system;
        this.storageConfig = system.getStorageConfig();
        this.salt = system.getSystemId() + system.getStorageConfig().getHost() +
				system.getStorageConfig().getDefaultAuthConfig().getUsername();

        SystemDao dao = Mockito.mock(SystemDao.class);
        Mockito.when(dao.findBySystemId(Mockito.anyString()))
               .thenReturn(this.system);
	}

	protected StorageSystem getSystem() throws SystemException {
		if (this.system == null) {
			try {
				init();
			} catch (Exception e) {
				throw new SystemException("Failed to initialize test StorageSystem", e);
			}
		}

		return this.system;
	}
	
	/**
     * Gets {@link RemoteDataClient} from current thread
     * @return a valid client configured to use the test directory for this class
     */
    protected RemoteDataClient getClient() 
    {
        RemoteDataClient client;
        try {
			if (threadClient.get() == null) {
				client = getSystem().getRemoteDataClient();
				String threadHomeDir = String.format("%s/thread-%s-%d",
						getSystem().getStorageConfig().getHomeDir(),
                        UUID.randomUUID(),
						Thread.currentThread().getId());
				client.updateSystemRoots(client.getRootDir(), threadHomeDir);
				threadClient.set(client);
			}
		} catch (SystemException e) {
        	Assert.fail("Unable to initialize RemoteDataClient due to missing system definition.", e);
        } catch (RemoteDataException | RemoteCredentialException e) {
            Assert.fail("Failed to get client", e);
        }
        
        return threadClient.get();
    }

	/**
	 * Generates a random directory name for the remote test folder. The
	 * remote folder is not created. Use #createRemoteTestDir() to generate
	 * a test directory on the remote system.
	 *
	 * @see #createRemoteTestDir()
	 * @return string representing remote test directory.
	 */
	protected String getRemoteTestDirPath() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Creates a random test directory on the remote system using the
	 * RemoteDataClient#mkdirs method. The path is obtained form a
	 * call to #getRemoteTestDirPath().
	 *
	 * @see #getRemoteTestDirPath()
	 * @return path to the test directory on the remote system
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	protected String createRemoteTestDir() throws IOException, RemoteDataException {
		String remoteBaseDir = getRemoteTestDirPath();
		getClient().mkdirs(remoteBaseDir);

		return remoteBaseDir;
	}
    
    protected String getLocalDownloadDir() {
        return LOCAL_DOWNLOAD_DIR + Thread.currentThread().getId();
    }
	
    /**
     * Returns a {@link JSONObject} representing the system to test.
     * 
     * @return 
     * @throws JSONException
     * @throws IOException
     */
    protected abstract JSONObject getSystemJson() throws JSONException, IOException;

	/**
	 * Deletes the temp folder used for these test cases.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	protected void afterClass() throws Exception 
    {
    	try 
    	{
    		// reset root dir and delete tmp directory.
    		storageConfig.setRootDir(FilenameUtils.getPath(storageConfig.getRootDir()));
    		getClient().authenticate();
    		getClient().delete("");
    		
    	} catch (Exception e) {
    		log.error("Failed to clean up and delete home directory after tests. Future tests may fail.", e);
    	} 
    	finally {
    		FileUtils.deleteDirectory(getLocalDownloadDir());
    		try { getClient().disconnect(); } catch (Exception ignored) {}
    	}
    }
	
	/**
	 * Does a clean setup of the remote file/folder used for testing.
	 * This means creating the file/folder each time to guarantee
	 * there is no carryover between tests.
	 */
	@BeforeMethod
	protected void beforeMethod(Method m) 
	{
		try 
		{	
			// create permisssion folder
    		getClient().authenticate();
    		
    		if (!getClient().doesExist(m.getName())) {
    		    getClient().mkdirs(m.getName(), system.getOwner());
    		}

    		if (!getClient().doesExist(m.getName() + "/" + LOCAL_DIR_NAME)) {
    		    getClient().put(LOCAL_DIR, m.getName());
    		}

    		if (!getClient().isDirectory(m.getName() + "/" + LOCAL_DIR_NAME)) {
                Assert.fail("Test work directory " + client.resolvePath(m.getName()) + " exists, but is not a directory.");
            }
    	} 
		catch (Exception e) {
    		Assert.fail("Failed to create home directory and stage test data. Tests would fail from here on.", e);
    	} 
	}
	
	/**
	 * Does a full teardown of the remote file/folder used for testing.
	 * This means deleting the file/folder each time to guarantee
	 * there is no carryover between tests even upon failure.
	 * @throws IOException 
	 */
	@AfterMethod
	protected void afterMethod(Method m) throws IOException 
	{
		try {
    		getClient().delete(m.getName());
    	} catch (Exception e) {
    		log.error("Failed to clean up and delete home directory after tests. Future tests may fail.", e);
    	} finally {
//    		getClient().disconnect();
    		FileUtils.deleteDirectory(getLocalDownloadDir());
    	}
	}
	
	@DataProvider(name="basePermissionProvider")
	protected Object[][] basePermissionProvider(Method m)
	{
		return new Object[][] {
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, false, true, "Failed to set delegated permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, true, true, "Failed to set delegated permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, false, true, "Failed to set delegated permission on file"},
			
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, false, false, "Owner should not be able to change their ownership on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, true, false, "Owner should not be able to change their ownership on folder recursively"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, false, false, "Owner should not be able to change their ownership on file"},
		};
	}
	
	@DataProvider(name="getAllPermissionsProvider")
	protected Object[][] getAllPermissionsProvider(Method m)
	{
		return new Object[][] {
				{m.getName() + "/" + LOCAL_DIR_NAME, "No permissions returned for " + m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_DIR_NAME},
				{m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, "No permissions returned for " + m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME},
		};
	}
	
	protected void _isPermissionMirroringRequired() {
	    Assert.assertEquals(getClient().isPermissionMirroringRequired(), system.getStorageConfig().isMirrorPermissions(), 
                "isPermissionMirroringRequired should reflect the StorageConfig settings.");
	}


    protected void _getAllPermissions(String path, String errorMessage)
	{
		try
		{
			List<RemoteFilePermission> pems = getClient().getAllPermissions(path);
			
			Assert.assertNotNull(pems, errorMessage);
			
			Assert.assertFalse(pems.isEmpty(), errorMessage);
			
			boolean found = false;
			for (RemoteFilePermission pem: pems) {
				if (pem.getUsername().equals(SYSTEM_USER)) {
					found = true;
				}
			}
			
			Assert.assertTrue(found, "System owner was not found in permission list.");
		}
		catch (IOException e) {
        	Assert.fail("No file found at " + path, e);
        }
		catch (RemoteDataException e)
		{
			Assert.fail("Failed to retrieve all permissions for path " + path, e);
		}
	}

	protected void _getAllPermissionsWithUserFirst(String path, String errorMessage)
	{
		try
		{
			List<RemoteFilePermission> pems = getClient().getAllPermissionsWithUserFirst(path, SYSTEM_USER);
			
			Assert.assertNotNull(pems, errorMessage);
			
			Assert.assertFalse(pems.isEmpty(), errorMessage);
			
			Assert.assertTrue(pems.get(0).getUsername().equals(SYSTEM_USER), 
					"getAllPermissionsWithUserFirst did not return username first");
		} 
		catch (IOException e) {
        	Assert.fail("No file found at " + path, e);
        }
		catch (RemoteDataException e)
		{
			Assert.fail("Failed to retrieve all permissions for path " + path, e);
		}
	}

	@DataProvider(name="getPermissionForUserProvider")
	protected Object[][] getPermissionForUserProvider(Method m)
	{
		return new Object[][] {
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, true, "No permissions returned for " + m.getName() + "/" + LOCAL_DIR_NAME},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, true, "No permissions returned for " + m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, false, "Permissions returned for user without permission on " + m.getName() + "/" + LOCAL_DIR_NAME},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, false, "No permissions returned for user without permission on " + m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME},
		};
	}

	protected void _getPermissionForUser(String username, String path, boolean shouldHavePermission, String errorMessage)
	{
		try
		{
			PermissionType permissionType = getClient().getPermissionForUser(username, path);
			
			Assert.assertNotEquals(permissionType.equals(PermissionType.NONE), shouldHavePermission, errorMessage);
		}
		catch (IOException e) {
        	Assert.fail("No file found at " + path, e);
        }
		catch (RemoteDataException e)
		{
			Assert.fail("Failed to retrieve permissions for " + username + " on path " + path, e);
		}
	}

	protected void _hasExecutePermission(String username, String path, boolean shouldHavePermission, String errorMessage)
	{
		try 
		{
			Assert.assertEquals(getClient().hasExecutePermission(path, username), shouldHavePermission, errorMessage);
		}
		catch (IOException e) {
        	Assert.fail("No file found at " + path, e);
        }
		catch (RemoteDataException e)
		{
			Assert.fail("hasExecutePermission failed for " + username + " on path " + path, e);
		}
	}

	protected void _hasReadPermission(String username, String path, boolean shouldHavePermission, String errorMessage)
	{
		try 
		{
			Assert.assertEquals(getClient().hasReadPermission(path, username), shouldHavePermission, errorMessage);
		}
		catch (IOException e) {
        	Assert.fail("No file found at " + path, e);
        }
		catch (RemoteDataException e)
		{
			Assert.fail("hasReadPermission failed for " + username + " on path " + path, e);
		}
	}

	protected void _hasWritePermission(String username, String path, boolean shouldHavePermission, String errorMessage)
	{
		try 
		{
			Assert.assertEquals(getClient().hasWritePermission(path, username), shouldHavePermission, errorMessage);
		}
		catch (IOException e) {
        	Assert.fail("No file found at " + path, e);
        }
		catch (RemoteDataException e)
		{
			Assert.fail("hasWritePermission failed for " + username + " on path " + path, e);
		}
	}

	@DataProvider(name="setPermissionForSharedUserProvider")
	protected Object[][] setPermissionForSharedUserProvider(Method m)
	{
		return new Object[][] {
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.ALL, false, true, false, "Failed to set delegated ALL permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.EXECUTE, false, true, false, "Failed to set delegated EXECUTE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ, false, true, false, "Failed to set delegated READ permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE, false, true, false, "Failed to set delegated WRITE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_EXECUTE, false, true, false, "Failed to set delegated READ_EXECUTE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_WRITE, false, true, false, "Failed to set delegated READ_WRITE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE_EXECUTE, false, true, false, "Failed to set delegated WRITE_EXECUTE permission on folder root"},
			
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.ALL, true, true, false, "Failed to set delegated ALL permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.EXECUTE, true, true, false, "Failed to set delegated EXECUTE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ, true, true, false, "Failed to set delegated READ permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE, true, true, false, "Failed to set delegated WRITE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_EXECUTE, true, true, false, "Failed to set delegated READ_EXECUTE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_WRITE, true, true, false, "Failed to set delegated READ_WRITE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE_EXECUTE, true, true, false, "Failed to set delegated WRITE_EXECUTE permission on folder recursively"},
			
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.ALL, false, true, false, "Failed to set delegated ALL permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.EXECUTE, false, true, false, "Failed to set delegated EXECUTE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ, false, true, false, "Failed to set delegated READ permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE, false, true, false, "Failed to set delegated WRITE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_EXECUTE, false, true, false, "Failed to set delegated READ_EXECUTE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_WRITE, false, true, false, "Failed to set delegated READ_WRITE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE_EXECUTE, false, true, false, "Failed to set delegated WRITE_EXECUTE permission on file"},
		};
	}
	
	@DataProvider(name="setPermissionForUserProvider")
	protected Object[][] setPermissionForUserProvider(Method m)
	{
		return new Object[][] {
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.ALL, false, true, false, "Owner should not be able to change their ownership to ALL permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.EXECUTE, false, false, false, "Owner should not be able to change their ownership to EXECUTE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ, false, false, false, "Owner should not be able to change their ownership to READ permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE, false, false, false, "Owner should not be able to change their ownership to WRITE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_EXECUTE, false, false, false, "Owner should not be able to change their ownership to READ_EXECUTE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_WRITE, false, false, false, "Owner should not be able to change their ownership to READ_WRITE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE_EXECUTE, false, false, false, "Owner should not be able to change their ownership to WRITE_EXECUTE permission on file"},
			
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.ALL, false, true, false, "Owner should not be able to change their ownership to ALL permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.EXECUTE, false, false, false, "Owner should not be able to change their ownership to EXECUTE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ, false, false, false, "Owner should not be able to change their ownership to READ permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE, false, false, false, "Owner should not be able to change their ownership to WRITE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_EXECUTE, false, false, false, "Owner should not be able to change their ownership toREAD_EXECUTE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_WRITE, false, false, false, "Owner should not be able to change their ownership to READ_WRITE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE_EXECUTE, false, false, false, "Owner should not be able to change their ownership to WRITE_EXECUTE permission on folder root"},

		};
	}

	protected void _setPermissionForSharedUser(String username, String path, PermissionType type, boolean recursive, boolean shouldSetPermission, boolean shouldThrowException, String errorMessage)
	{
		try {
			getClient().setPermissionForUser(username, path, type, recursive);
			
			if (type.canRead())
				Assert.assertTrue(getClient().hasReadPermission(path, username), "Failed to set read permission for the user.");
			
			if (type.canWrite())
				Assert.assertTrue(getClient().hasWritePermission(path, username), "Failed to set read permission for the user.");
			
			if (type.canExecute())
				Assert.assertTrue(getClient().hasExecutePermission(path, username), "Failed to set read permission for the user.");
			
		} catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		finally {
			getClient().disconnect();
		}
	}

	protected void _setPermissionForUser(String username, String path, PermissionType type, boolean recursive, boolean shouldSetPermission, boolean shouldThrowException, String errorMessage)
	{
		try {
			getClient().setPermissionForUser(username, path, type, recursive);
			
			if (type.canRead())
				Assert.assertTrue(getClient().hasReadPermission(path, username), "Failed to set read permission for the user.");
			
			if (type.canWrite())
				Assert.assertTrue(getClient().hasWritePermission(path, username), "Failed to set read permission for the user.");
			
			if (type.canExecute())
				Assert.assertTrue(getClient().hasExecutePermission(path, username), "Failed to set read permission for the user.");
			
		} catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		finally {
			getClient().disconnect();
		}
	}
	
	@DataProvider(name="setExecutePermissionProvider")
	protected Object[][] setExecutePermissionProvider(Method m)
	{
		return new Object[][] {
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, false, true, "Failed to set delegated ALL permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, true, true, "Failed to set delegated ALL permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, false, true, "Failed to set delegated ALL permission on file"},
			
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, false, true, "Owner should be able to change their ownership to ALL permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, true, true, "Owner should be able to change their ownership to ALL permission on folder recursively"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, false, true, "Owner should be able to change their ownership toALL permission on file"},
			
		};
	}

	protected void _setExecutePermission(String username, String path, boolean recursive, boolean shouldSetPermission, String errorMessage)
	throws RemoteDataException, IOException {
		getClient().setExecutePermission(username, path, recursive);
		if (shouldSetPermission) {
			Assert.assertEquals(getClient().getPermissionForUser(username, path), PermissionType.EXECUTE, errorMessage);
		} else {
			Assert.assertEquals(getClient().getPermissionForUser(username, path), PermissionType.ALL, errorMessage);
		}
	}
	
	@DataProvider(name="setOwnerPermissionProvider")
	protected Object[][] setOwnerPermissionProvider(Method m)
	{
		return new Object[][] {
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, false, true, "Failed to set delegated ALL permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, true, true, "Failed to set delegated ALL permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, false, true, "Failed to set delegated ALL permission on file"},
			
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, false, true, "Owner should not be able to change their ownership to ALL permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, true, true, "Owner should not be able to change their ownership to ALL permission on folder recursively"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, false, true, "Owner should not be able to change their ownership toALL permission on file"},
			
		};
	}

	protected void _setOwnerPermission(String username, String path, boolean recursive, boolean shouldSetPermission, String errorMessage)
	throws Exception
	{
		getClient().setOwnerPermission(username, path, recursive);
		Assert.assertEquals(getClient().getPermissionForUser(username, path), PermissionType.ALL, errorMessage);
	}
	
	@DataProvider(name="setReadPermissionProvider")
	protected Object[][] setReadPermissionProvider(Method m)
    {
	    return basePermissionProvider(m);
    }

	protected void _setReadPermission(String username, String path, boolean recursive, boolean shouldSetPermission, String errorMessage)
	throws Exception
	{
		getClient().setReadPermission(username, path, recursive);
		if (shouldSetPermission) {
			Assert.assertEquals(getClient().getPermissionForUser(username, path), PermissionType.READ, errorMessage);
		} else {
			Assert.assertEquals(getClient().getPermissionForUser(username, path), PermissionType.ALL, errorMessage);
		}
	}
	
	@DataProvider(name="setWritePermissionProvider")
	protected Object[][] setWritePermissionProvider(Method m)
    {
        return basePermissionProvider(m);
    }

	protected void _setWritePermission(String username, String path, boolean recursive, boolean shouldSetPermission, String errorMessage)
	throws Exception
	{
		getClient().setWritePermission(username, path, recursive);
		if (shouldSetPermission) {
			Assert.assertEquals(getClient().getPermissionForUser(username, path), PermissionType.WRITE, errorMessage);
		} else {
			Assert.assertEquals(getClient().getPermissionForUser(username, path), PermissionType.ALL, errorMessage);
		}
	}
	
	@DataProvider(name="removeExecutePermissionProvider")
	protected Object[][] removeExecutePermissionProvider(Method m)
    {
        return setExecutePermissionProvider(m);
    }

	protected void _removeExecutePermission(String username, String path, boolean recursive, boolean shouldRemovePermission, String errorMessage)
	{
	    try
		{
			try {
				getClient().removeExecutePermission(username, path, recursive);
				Assert.assertTrue(getClient().hasExecutePermission(path, username),
					"Failed to set EXECUTE permission for " + username + " on path " + path);
			} catch (Exception ignored) {}

			getClient().removeExecutePermission(username, path, recursive);

			Assert.assertNotEquals(getClient().hasExecutePermission(path, username),
					shouldRemovePermission, errorMessage);

			if (recursive)
			{
				for (RemoteFileInfo fileInfo: getClient().ls(path)) {
					String childPath = path + "/" + fileInfo.getName();
					Assert.assertNotEquals(getClient().hasExecutePermission(childPath, username),
							shouldRemovePermission, errorMessage);
				}
			}
		}
		catch (Exception e)
		{
			Assert.fail("Failed to remove EXECUTE permissions for " + username + " on path " + path, e);
		}
	}

	@DataProvider(name="removeReadPermissionProvider")
	protected Object[][] removeReadPermissionProvider(Method m)
    {
	    return basePermissionProvider(m);
    }

	protected void _removeReadPermission(String username, String path, boolean recursive, boolean shouldRemovePermission, String errorMessage)
	{
		try
		{
			getClient().setReadPermission(username, path, recursive);
			Assert.assertTrue(getClient().hasReadPermission(path, username), 
					"Failed to set READ permission for " + username + " on path " + path);
			
			getClient().removeReadPermission(username, path, recursive);
			
			Assert.assertNotEquals(getClient().hasReadPermission(path, username), 
					shouldRemovePermission, errorMessage);
			
			if (recursive)
			{
				for (RemoteFileInfo fileInfo: getClient().ls(path)) {
					String childPath = path + "/" + fileInfo.getName();
					Assert.assertNotEquals(getClient().hasReadPermission(childPath, username), 
							shouldRemovePermission, errorMessage);
				}
			}
		}
		catch (Exception e)
		{
			Assert.fail("Failed to remove READ permissions for " + username + " on path " + path, e);
		}
	}
    
    @DataProvider(name="removeWritePermissionProvider")
    protected Object[][] removeWritePermissionProvider(Method m)
    {
        return basePermissionProvider(m);
    }

	protected void _removeWritePermission(String username, String path, boolean recursive, boolean shouldRemovePermission, String errorMessage)
	{
		try
		{
			getClient().setWritePermission(username, path, recursive);
			Assert.assertTrue(getClient().hasWritePermission(path, username), 
					"Failed to set WRITE permission for " + username + " on path " + path);
			
			getClient().removeWritePermission(username, path, recursive);
			
			Assert.assertNotEquals(getClient().hasWritePermission(path, username), 
					shouldRemovePermission, errorMessage);
			
			if (recursive && !shouldRemovePermission)
			{
				for (RemoteFileInfo fileInfo: getClient().ls(path)) {
					String childPath = path + "/" + fileInfo.getName();
					Assert.assertNotEquals(getClient().hasWritePermission(childPath, username), 
							shouldRemovePermission, errorMessage);
				}
			}
		}
		catch (Exception e)
		{
			Assert.fail("Failed to remove WRITE permissions for " + username + " on path " + path, e);
		}
	}

	@DataProvider(name="clearPermissionProvider")
	protected Object[][] clearPermissionProvider(Method m)
	{
		return new Object[][] {
			// username, 		 path, 								 initialPermission,  recursive, shouldClearPermission, errorMessage
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.ALL, false, true, "Failed to delete delegated ALL permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.EXECUTE, false, true, "Failed to delete delegated EXECUTE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ, false, true, "Failed to delete delegated READ permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE, false, true, "Failed to delete delegated WRITE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_EXECUTE, false, true, "Failed to delete delegated READ_EXECUTE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_WRITE, false, true, "Failed to delete delegated READ_WRITE permission on folder root"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE_EXECUTE, false, true, "Failed to delete delegated WRITE_EXECUTE permission on folder root"},
			
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.ALL, true, true, "Failed to delete delegated ALL permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.EXECUTE, true, true, "Failed to delete delegated EXECUTE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ, true, true, "Failed to delete delegated READ permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE, true, true, "Failed to delete delegated WRITE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_EXECUTE, true, true, "Failed to delete delegated READ_EXECUTE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_WRITE, true, true, "Failed to delete delegated READ_WRITE permission on folder recursively"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE_EXECUTE, true, true, "Failed to delete delegated WRITE_EXECUTE permission on folder recursively"},
			
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.ALL, false, true, "Failed to delete delegated ALL permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.EXECUTE, false, true, "Failed to delete delegated EXECUTE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ, false, true, "Failed to delete delegated READ permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE, false, true, "Failed to delete delegated WRITE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_EXECUTE, false, true, "Failed to delete delegated READ_EXECUTE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_WRITE, false, true, "Failed to delete delegated READ_WRITE permission on file"},
			{SHARED_SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE_EXECUTE, false, true, "Failed to delete delegated WRITE_EXECUTE permission on file"},
			
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.ALL, false, true, "Owner should not be able to clear their ownership after adding ALL permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.EXECUTE, false, true, "Owner should not be able to clear their ownership after adding EXECUTE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ, false, true, "Owner should not be able to clear their ownership after adding READ permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE, false, true, "Owner should not be able to clear their ownership after adding WRITE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_EXECUTE, false, true, "Owner should not be able to clear their ownership after adding READ_EXECUTE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.READ_WRITE, false, true, "Owner should not be able to clear their ownership after adding READ_WRITE permission on folder root"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME, PermissionType.WRITE_EXECUTE, false, true, "Owner should not be able to clear their ownership after adding WRITE_EXECUTE permission on folder root"},
			
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.ALL, false, false, "Owner should not be able to clear their ownership after adding ALL permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.EXECUTE, false, false, "Owner should not be able to clear their ownership after adding EXECUTE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ, false, false, "Owner should not be able to clear their ownership after adding READ permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE, false, false, "Owner should not be able to clear their ownership after adding WRITE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_EXECUTE, false, false, "Owner should not be able to clear their ownership after adding READ_EXECUTE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.READ_WRITE, false, false, "Owner should not be able to clear their ownership after adding READ_WRITE permission on file"},
			{SYSTEM_USER, m.getName() + "/" + LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME, PermissionType.WRITE_EXECUTE, false, false, "Owner should not be able to clear their ownership after adding WRITE_EXECUTE permission on file"},

		};
	}

	protected void _clearPermissions(String username, String path, PermissionType initialPermission, boolean recursive, boolean shouldClearPermission, String errorMessage)
	throws Exception
	{
		getClient().setPermissionForUser(username, path, initialPermission, recursive);
		if (initialPermission.canRead())
			Assert.assertTrue(getClient().hasReadPermission(path, username), "Failed to set read permission for the user.");
		if (initialPermission.canWrite())
			Assert.assertTrue(getClient().hasWritePermission(path, username), "Failed to set read permission for the user.");
		if (initialPermission.canExecute())
			Assert.assertTrue(getClient().hasExecutePermission(path, username), "Failed to set read permission for the user.");

		getClient().clearPermissions(username, path, recursive);

		Assert.assertEquals(getClient().getPermissionForUser(username, path).equals(PermissionType.NONE), shouldClearPermission, errorMessage);

		if (recursive) {
			if (StringUtils.equalsIgnoreCase(username, SHARED_SYSTEM_USER)) {
				try {
					getClient().ls(path);
				} catch (RemoteDataException e) {
					Assert.assertTrue(e.getMessage().contains("insufficient privileges"),
							"User without permissions should not have permission to view the folder contents after permissions are cleared.");
				}
			} else {
				for (RemoteFileInfo fileInfo : getClient().ls(path)) {
					String childPath = path + "/" + fileInfo.getName();
					Assert.assertEquals(getClient().getPermissionForUser(username, childPath).equals(PermissionType.NONE), shouldClearPermission, errorMessage);
				}
			}
		}
	}
}
