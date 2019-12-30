package org.iplantc.service.remote.ssh;

import java.io.IOException;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.remote.AbstractRemoteSubmissionClientTest;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups={"ssh-tunnel.run", "broken"})
public class MaverickSSHTunnelSubmissionClientIT extends AbstractRemoteSubmissionClientTest {

	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(JSONTestDataUtil.TEST_PROXY_EXECUTION_SYSTEM_FILE);
	}

	@Test
	public void canAuthentication() {
		try {
			Assert.assertTrue(getClient().canAuthentication(), "Authentication should succeed on testbed system.");
		} 
		catch (Throwable t) {
			Assert.fail("No exceptions should be thrown on authentication.", t);
		}
	}

	@Test
	public void runCommand() {
		try {
			String response = getClient().runCommand("echo $HOSTNAME");
			Assert.assertEquals(StringUtils.trim(response), 
					system.getLoginConfig().getHost(), 
					"Response from whoami on remote system should be the login config default auth config username");
		}
		catch (Exception e) {
			Assert.fail("Running whoami should not throw exception.", e);
		}
	}

}
