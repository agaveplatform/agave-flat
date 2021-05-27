package org.iplantc.service.jobs.model;

import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;

import java.util.Date;

/**
 * Created with IntelliJ IDEA. User: wcs Date: 3/21/13 Time: 4:12 PM To change
 * this template use File | Settings | File Templates.
 */
public class JobTestHelper {

	public RemoteSystem getStorageSystem() throws SystemArgumentException
	{
		StorageSystem storageSystem = new StorageSystem();
		storageSystem.setType(RemoteSystemType.STORAGE);
        storageSystem.setOwner("testuser");
        storageSystem.setAvailable(true);
        storageSystem.setCreated(new Date());
        storageSystem.setDescription("test description");
        storageSystem.setGlobalDefault(false);
        storageSystem.setLastUpdated(new Date());
        storageSystem.setName("irods");
        storageSystem.setPubliclyAvailable(true);
        storageSystem.setRevision(2);
        storageSystem.setSystemId("example.data.com");
        return storageSystem;
    }
}
