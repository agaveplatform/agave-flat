/**
 * 
 */
package org.iplantc.service.transfer.sftp;

import org.iplantc.service.transfer.AbstractPathSanitizationTest;
import org.iplantc.service.transfer.IPathSanitizationTest;
import org.iplantc.service.transfer.TransferTestRetryAnalyzer;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * @author dooley
 *
 */
@Test(groups= {"sftprelay","sftprelay.path.sanitization"})
public class SftpRelayPathSanitizationIT extends AbstractPathSanitizationTest implements IPathSanitizationTest {

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.AbstractPathSanitizationTest#getSystemJson()
     */
    @Override
    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp.example.com.json");
    }

    @Override
    @Test(groups={"mkdir"}, dataProvider="mkDirSanitizesSingleSpecialCharacterProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
    public void mkDirSanitizesSingleSpecialCharacterRelativePath(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
    {
        String relativePath = UUID.randomUUID() + "/";

        _mkDirsSanitizationTest(relativePath + filename, shouldSucceed, message);
    }

    @Override
    @Test(groups={"mkdir"}, dataProvider="mkDirSanitizesSingleSpecialCharacterProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
    public void mkDirSanitizesSingleSpecialCharacterAbsolutePath(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
    {
        String absolutePath = system.getStorageConfig().getHomeDir() +
                "/thread-" + Thread.currentThread().getId() +
                "/" + UUID.randomUUID() + "/";

        _mkDirsSanitizationTest(absolutePath + filename, shouldSucceed, message);
    }

    @Override
    @Test(groups={"mkdir"}, dataProvider="mkDirSanitizesRepeatedSpecialCharacterProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
    public void mkDirSanitizesRepeatedSpecialCharacter(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
    {
        _mkDirSanitizationTest(filename, shouldSucceed, message);
    }

    @Override
    @Test(groups={"mkdir"}, dataProvider="mkDirSanitizesWhitespaceProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
    public void mkDirSanitizesWhitespace(String filename, String message)  throws IOException, RemoteDataException
    {
        _mkDirSanitizationTest(filename, false, message);
    }

    @Override
    @Test(groups={"mkdirs"}, dataProvider="mkDirSanitizesSingleSpecialCharacterProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
    public void mkDirsSanitizesSingleSpecialCharacterRelativePath(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
    {
        _mkDirsSanitizationTest(filename, shouldSucceed, message);
    }

    @Override
    @Test(groups={"mkdirs"}, dataProvider="mkDirSanitizesSingleSpecialCharacterProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
    public void mkDirsSanitizesSingleSpecialCharacterAbsolutePath(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
    {
        _mkDirsAbsolutePathSanitizationTest(filename, shouldSucceed, message);
    }

    @Override
    @Test(groups={"mkdirs"}, dataProvider="mkDirSanitizesRepeatedSpecialCharacterProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
    public void mkDirsSanitizesRepeatedSpecialCharacter(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
    {
        _mkDirsSanitizationTest(filename, shouldSucceed, message);
    }
}
