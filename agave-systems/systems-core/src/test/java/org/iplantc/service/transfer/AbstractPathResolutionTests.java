package org.iplantc.service.transfer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageConfig;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractPathResolutionTests// extends BaseTransferTestCase
{
//	protected abstract JSONObject getSystemJson() throws JSONException, IOException;
//
//	@BeforeClass
//    public void beforeClass() throws Exception {
//    	super.beforeClass();
//
//    	JSONObject json = getSystemJson();
//    	system = (StorageSystem)StorageSystem.fromJSON(json);
//    	system.setOwner(SYSTEM_USER);
//
//		String originalHomeDir = system.getStorageConfig().getHomeDir();
//		originalHomeDir = StringUtils.isEmpty(originalHomeDir) ? "" : originalHomeDir;
//		String threadHomeDir = String.format("%s/%s/thread-%s-%d",
//				originalHomeDir,
//				getClass().getSimpleName(),
//				UUID.randomUUID().toString(),
//				Thread.currentThread().getId());
//
//		system.getStorageConfig().setHomeDir(threadHomeDir);
//		storageConfig = system.getStorageConfig();
//        String oldSalt = system.getSystemId() + storageConfig.getHost() +
//        		storageConfig.getDefaultAuthConfig().getUsername();
//
//        salt = this.getClass().getSimpleName() +
//				system.getStorageConfig().getHost() +
//    			system.getStorageConfig().getDefaultAuthConfig().getUsername();
//
//        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPassword())) {
//        	system.getStorageConfig().getDefaultAuthConfig().setPassword(system.getStorageConfig().getDefaultAuthConfig().getClearTextPassword(oldSalt));
//        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPassword(salt);
//        }
//
//        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPublicKey())) {
//        	system.getStorageConfig().getDefaultAuthConfig().setPublicKey(system.getStorageConfig().getDefaultAuthConfig().getClearTextPublicKey(oldSalt));
//        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPublicKey(salt);
//        }
//
//        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPrivateKey())) {
//        	system.getStorageConfig().getDefaultAuthConfig().setPrivateKey(system.getStorageConfig().getDefaultAuthConfig().getClearTextPrivateKey(oldSalt));
//        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPrivateKey(salt);
//        }
//
//        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getCredential())) {
//        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentCredential(salt);
//        }
//
//		system.setSystemId("serializedCredentialTest");
//
//		SystemDao dao = Mockito.mock(SystemDao.class);
//        Mockito.when(dao.findBySystemId(Mockito.anyString()))
//            .thenReturn(system);
//    }
//
//	@AfterClass
//    public void afterClass() throws Exception
//    {
//    	clearSystems();
//    }

	/**
	 * Returns the {@link RemoteDataClient} class to mock out for testing.
	 * @return the class to tests
	 */
	protected abstract Class<? extends RemoteDataClient> getRemoteDataClientClass();

	/**
	 * Creates a mock {@link RemoteDataClient} instance of type {@link #getRemoteDataClientClass()}}
	 * with rootDir and homeDir set to the proper values and {@link RemoteDataClient#resolvePath(String)}
	 * left to call the real method.
	 * @param rootDir the {@link StorageConfig#getRootDir()}
	 * @param homeDir the {@link StorageConfig#getHomeDir()}
	 * @return a mocked {@link RemoteDataClient} to test
	 * @throws Exception
	 */
	protected RemoteDataClient createRemoteDataClient(String rootDir, String homeDir)  throws Exception {
		Path rootPath = Paths.get(rootDir.isEmpty() ? "/" : rootDir);
		Path homePath = rootPath.resolve(homeDir.startsWith("/") ? homeDir.substring(1) : homeDir);

		String srootPath = rootPath.toString();
		srootPath += srootPath.endsWith("/") ? "" : "/";

		String shomePath = homePath.toString();
		shomePath += shomePath.endsWith("/") ? "" : "/";

		RemoteDataClient remoteDataClient = mock(getRemoteDataClientClass());
		when(remoteDataClient.getRootDir()).thenReturn(srootPath);
		when(remoteDataClient.getHomeDir()).thenReturn(shomePath);
		when(remoteDataClient.resolvePath(anyString())).thenCallRealMethod();

		return remoteDataClient;
	}
    
	@DataProvider(name = "resolvePathProvider", parallel=true)
    public Object[][] resolvePathProvider() throws Exception 
    {
		RemoteDataClient noRootNoHome = createRemoteDataClient("",  "");
    	RemoteDataClient absoluteRootNoHome = createRemoteDataClient("/root",  "");
    	RemoteDataClient relateveRootNoHome = createRemoteDataClient("root",  "");
    	RemoteDataClient noRootAbsoluteHome = createRemoteDataClient("",  "/home");
    	RemoteDataClient noRootRelativeHome = createRemoteDataClient("",  "home");
    	RemoteDataClient absoluteRootRelativeHome = createRemoteDataClient("/root",  "home");
    	RemoteDataClient relativeRootAbsoluteHome = createRemoteDataClient("root",  "/home");
    	RemoteDataClient absoluteRootAbsoluteHome = createRemoteDataClient("/root",  "/home");

    	return new Object[][] {
    			{ noRootNoHome, "testuser", "/testuser", false, "no root no home defaults to /, so /testuser" },
        		{ noRootNoHome, "/testuser", "/testuser", false, "no root no home all paths return unchanged" },
        		{ noRootNoHome, "../", "../", true, "noRootNoHome relative paths outside of rootDir should throw exception" },
        		{ noRootNoHome, "../root", "../root", true, "noRootNoHome relative path outside of rootDir should throw exception" },
        		
        		{ absoluteRootNoHome, "testuser", "/root/testuser", false, "absoluteRootNoHome all paths return unchanged" },
        		{ absoluteRootNoHome, "/testuser", "/root/testuser", false, "absoluteRootNoHome all paths return unchanged" },
        		{ absoluteRootNoHome, "..", "", true, "absoluteRootNoHome relative path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "../", "", true, "absoluteRootNoHome relative path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "/..", "", true, "absoluteRootNoHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "/../", "", true, "absoluteRootNoHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "/../root", "/root", false, "absoluteRootNoHome absolute path inside of rootDir return valid path" },
        		{ absoluteRootNoHome, "../root", "/root", false, "absoluteRootNoHome relative path inside of rootDir return valid path" },
        		{ absoluteRootNoHome, "/../root/../", "", true,"absoluteRootNoHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "../root/../", "", true,"absoluteRootNoHome relative path outside of rootDir should throw exception" },
        		
        		{ relateveRootNoHome, "testuser", "root/testuser", false, "relative root no home all paths return unchanged" },
        		{ relateveRootNoHome, "/testuser", "root/testuser", false, "relative root no home all paths return unchanged" },
        		{ relateveRootNoHome, "..", "", true, "relateveRootNoHome relative path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "../", "", true, "relateveRootNoHome relative path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "/..", "", true, "relateveRootNoHome absolute path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "/../", "", true, "relateveRootNoHome absolute path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "/../root", "root", false, "relateveRootNoHome absolute path inside of rootDir return valid path" },
        		{ relateveRootNoHome, "../root", "root", false, "relateveRootNoHome relative path inside of rootDir return valid path" },
        		{ relateveRootNoHome, "/../root/../", "", true,"relateveRootNoHome absolute path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "../root/../", "", true,"relateveRootNoHome relative path outside of rootDir should throw exception" },
        		
        		{ noRootAbsoluteHome, "testuser", "/home/testuser", false, "no root absolute home all paths return unchanged" },
        		{ noRootAbsoluteHome, "/testuser", "/testuser", false, "no root absolute home all paths return unchanged" },
        		{ noRootAbsoluteHome, "..", "/", false, "noRootAbsoluteHome relative path outside of rootDir should resolve" },
        		{ noRootAbsoluteHome, "../", "/", false, "noRootAbsoluteHome relative path outside of rootDir should resolve" },
        		{ noRootAbsoluteHome, "/..", "/", true, "noRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ noRootAbsoluteHome, "/../", "/", true, "noRootAbsoluteHome absolute path outside of rootDir should throw exception" },

        		{ noRootAbsoluteHome, "/../root", "/root", true, "noRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ noRootAbsoluteHome, "../root", "/root", false, "noRootAbsoluteHome relative path inside of rootDir should resolve" },
        		{ noRootAbsoluteHome, "/../root/../", "/", true,"noRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ noRootAbsoluteHome, "../root/../", "/", false,"noRootAbsoluteHome relative path outside of rootDir should resolve" },
        		
        		{ noRootRelativeHome, "testuser", "/home/testuser", false, "no root relative home all paths return unchanged" },
        		{ noRootRelativeHome, "/testuser", "/testuser", false, "no root relative home all paths return unchanged" },
                // relative path should resolve to "" with a relative Home of "home/"
        		{ noRootRelativeHome, "..", "/", false, "noRootRelativeHome relative path outside of rootDir should return unchanged" },
                    // relative path should resolve to "" with a relative Home of "home/"
        		{ noRootRelativeHome, "../", "/", false, "noRootRelativeHome relative path outside of rootDir should return unchanged" },
        		{ noRootRelativeHome, "/..", "/", true, "noRootRelativeHome absolute path outside of rootDir should throw exception" },
        		{ noRootRelativeHome, "/../", "/", true, "noRootRelativeHome absolute path outside of rootDir should throw excpetion" },

        		{ noRootRelativeHome, "/../root", "/root", true, "noRootRelativeHome absolute path outside of rootDir should throw exception" },
                // "home/../root" should resolve to "root"
        		{ noRootRelativeHome, "../root", "/root", false, "noRootRelativeHome relative path inside of rootDir should resolve" },
        		{ noRootRelativeHome, "/../root/../", "/", true,"noRootRelativeHome absolute path outside of rootDir should throw exception" },
                // // "home/../root/.." should resolve to ""
        		{ noRootRelativeHome, "../root/../", "/", false,"noRootRelativeHome relative path outside of rootDir should resolve" },
        		
        		{ absoluteRootRelativeHome, "testuser", "/root/home/testuser", false, "absolute root relative home all paths return unchanged" },
        		{ absoluteRootRelativeHome, "/testuser", "/root/testuser", false, "absolute root relative home all paths return unchanged" },
        		{ absoluteRootRelativeHome, "..", "/root/", false, "absoluteRootRelativeHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootRelativeHome, "../", "/root/", false, "absoluteRootRelativeHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootRelativeHome, "/..", "/", true, "absoluteRootRelativeHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootRelativeHome, "/../", "/", true, "absoluteRootRelativeHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootRelativeHome, "/../root", "/root", false, "absoluteRootRelativeHome absolute path inside of rootDir should resolve" },
        		{ absoluteRootRelativeHome, "../root", "/root/root", false, "absoluteRootRelativeHome relative path inside of rootDir should resolve" },
        		{ absoluteRootRelativeHome, "/../root/../", "/", true,"absoluteRootRelativeHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootRelativeHome, "../root/../", "/root/", false,"absoluteRootRelativeHome relative path inside of rootDir should resolve" },
        		
        		{ relativeRootAbsoluteHome, "testuser", "root/home/testuser", false, "relative root absolute home all paths return unchanged" },
        		{ relativeRootAbsoluteHome, "/testuser", "root/testuser", false, "relative root absolute home all paths return unchanged" },
        		{ relativeRootAbsoluteHome, "..", "root/", false, "relativeRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ relativeRootAbsoluteHome, "../", "root/", false, "relativeRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ relativeRootAbsoluteHome, "/..", "", true, "relativeRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ relativeRootAbsoluteHome, "/../", "", true, "relativeRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ relativeRootAbsoluteHome, "/../root", "root", false, "relativeRootAbsoluteHome absolute path inside of rootDir should resolve" },
        		{ relativeRootAbsoluteHome, "../root", "root/root", false, "relativeRootAbsoluteHome relative path inside of rootDir should resolve" },
        		{ relativeRootAbsoluteHome, "/../root/../", "", true,"relativeRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ relativeRootAbsoluteHome, "../root/../", "root/", false,"relativeRootAbsoluteHome relative path inside of rootDir should resolve" },
        		
        		{ absoluteRootAbsoluteHome, "testuser", "/root/home/testuser", false, "absolute root absolute home all paths return unchanged" },
        		{ absoluteRootAbsoluteHome, "/testuser", "/root/testuser", false, "absolute root absolute home all paths return unchanged" },
        		{ absoluteRootAbsoluteHome, "..", "/root/", false, "absoluteRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootAbsoluteHome, "../", "/root/", false, "absoluteRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootAbsoluteHome, "/..", "/", true, "absoluteRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootAbsoluteHome, "/../", "/", true, "absoluteRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootAbsoluteHome, "/../root", "/root", false, "absoluteRootAbsoluteHome absolute path inside of rootDir should resolve" },
        		{ absoluteRootAbsoluteHome, "../root", "/root/root", false, "absoluteRootAbsoluteHome relative path inside of rootDir should resolve" },
        		{ absoluteRootAbsoluteHome, "/../root/../", "/", true,"absoluteRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootAbsoluteHome, "../root/../", "/root/", false,"absoluteRootAbsoluteHome relative path inside of rootDir should resolve" },
    	};
    }
	
	/**
	 * Tests whether the given path resolves correctly against the virutal root and home for a given protocol's remote data client.
	 * 
	 * @param remoteDataClient mock remote data client to test the method
	 * @param testAgaveSystemPath the {@link RemoteSystem} path provided by users for remote data actions
	 * @param expectedResolvedRemotePath the expected path the {@code testAgaveSystemPath} resolves to on the remote system
	 * @param shouldThrowException true if the path resolution should throw an exception
	 * @param message message for failed test assertion exceptions
	 */
	protected void abstractResolvePath(RemoteDataClient remoteDataClient, String testAgaveSystemPath, String expectedResolvedRemotePath, boolean shouldThrowException, String message)
	{
    	try
    	{
    		String afterPath = remoteDataClient.resolvePath(testAgaveSystemPath);
    		Assert.assertEquals(afterPath, expectedResolvedRemotePath,
    				"Resolved path " + afterPath + " did not match the expected resolved path of " +
							expectedResolvedRemotePath);
    	} 
    	catch (Exception e) {
    		if (!shouldThrowException) {
        		Assert.fail(message, e);
			}
        }
	}
	
	public abstract void resolvePath(RemoteDataClient client, String beforePath, String resolvedPath, boolean shouldThrowException, String message);
	

}
