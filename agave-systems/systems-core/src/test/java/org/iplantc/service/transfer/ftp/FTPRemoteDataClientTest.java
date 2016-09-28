/**
 * 
 */
package org.iplantc.service.transfer.ftp;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"ftp","filesystem","broken"})
public class FTPRemoteDataClientTest extends AbstractRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "ftp.example.com.json");
	}
	
	@Override
	protected String getForbiddenDirectoryPath(boolean shouldExist) {
		if (shouldExist) {
			return "/home/testotheruser";
		} else {
			return "/root/helloworld";
		}
	}
}
