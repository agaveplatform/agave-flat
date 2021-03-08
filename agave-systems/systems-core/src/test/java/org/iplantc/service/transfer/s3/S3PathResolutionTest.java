package org.iplantc.service.transfer.s3;

import org.iplantc.service.transfer.AbstractPathResolutionTests;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.annotations.Test;

@Test(groups={"unit"})
public class S3PathResolutionTest extends AbstractPathResolutionTests
{
	@Override
	protected Class<? extends RemoteDataClient> getRemoteDataClientClass() {
		return S3Jcloud.class;
	}

	@Test(dataProvider="resolvePathProvider")
	@Override
	public void resolvePath(RemoteDataClient client, String beforePath,
							String resolvedPath, boolean shouldThrowException, String message) {
		super.abstractResolvePath(client, beforePath, resolvedPath, shouldThrowException, message);
	}
}
