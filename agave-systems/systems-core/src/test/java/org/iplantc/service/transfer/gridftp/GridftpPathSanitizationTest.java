/**
 * 
 */
package org.iplantc.service.transfer.gridftp;

import org.iplantc.service.transfer.AbstractPathSanitizationTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author dooley
 *
 */
@Test(enabled = false, singleThreaded=true, groups= {"gridftp","sanitization","broken"})
public class GridftpPathSanitizationTest extends AbstractPathSanitizationTest {

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.AbstractPathSanitizationTest#getSystemJson()
     */
    @Override
    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "gridftp.example.com.json");
    }

}
