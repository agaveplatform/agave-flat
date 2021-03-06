package org.iplantc.service.transfer.s3;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.jclouds.aws.s3.AWSS3Client;
import org.jclouds.aws.s3.blobstore.AWSS3BlobStore;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.*;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.rest.ApiContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;

@Test(groups = {"external","s3.performance","performance","broken"})
public class S3PerformanceTests extends BaseTransferTestCase {
    private static final Logger log = Logger.getLogger(S3PerformanceTests.class);
    private S3Jcloud s3 = null;
    private final String testdirname = "test";
    private String containerName;

    @Override
    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        JSONObject json = getSystemJson();
        json.remove("id");
        json.put("id", this.getClass().getSimpleName());
        system = StorageSystem.fromJSON(json);
        system.setOwner(SYSTEM_USER);
        system.getStorageConfig().setHomeDir("/");
        storageConfig = system.getStorageConfig();
        salt = system.getSystemId() + storageConfig.getHost()
                + storageConfig.getDefaultAuthConfig().getUsername();
        containerName = system.getStorageConfig().getContainerName();
        SystemDao dao = new SystemDao();
        if (dao.findBySystemId(system.getSystemId()) == null) {
            dao.persist(system);
        }
    }

    @AfterClass
    public void afterClass() throws Exception {
        try {
            // auth client and ensure test directory is present
            if (s3 == null) {
                s3 = (S3Jcloud) system.getRemoteDataClient();
            }
            s3.authenticate();

            if (s3.getBlobStore().containerExists(containerName)) {
                s3.getBlobStore().clearContainer(containerName);
            }
        } catch (RemoteDataException e) {
            throw e;
        } catch (Exception e) {
            Assert.fail("Failed to clean up after test.", e);
        }

        try {
            FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR);
            clearSystems();
        } finally {
            try {
                client.disconnect();
            } catch (Exception e) {
            }
        }
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        try {
            FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR);
        } catch (IOException e1) {
        }

        try {
            // auth client and ensure test directory is present
            if (s3 == null) {
                s3 = (S3Jcloud) system.getRemoteDataClient();
            }
            s3.authenticate();
            if (s3.getBlobStore().containerExists(containerName)) {
                s3.getBlobStore().clearContainer(containerName);
            }
        } catch (RemoteDataException e) {
            throw e;
        } catch (Exception e) {
            Assert.fail("Failed to authenticate before test method.", e);
        }
    }

    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "s3.example.com.json");
    }

//    @DataProvider
//    public Object[][] testDirectoryProvider() {
//        return new Object[][]{
//                {testdirname, new String[]{testdirname}, "Failed to create " +
//                        testdirname},
//                {"/" + testdirname, new String[]{"", testdirname},
//                        "Directories with leading slashes should not be created"},
//                {testdirname + "/", new String[]{testdirname, testdirname + "/"},
//                        "Directories with trailing slashes should not be created"},
//                {"/" + testdirname + "/", new String[]{"", testdirname, testdirname +
//                        "/"}, "Directories with bookend slashes should not be created"},
//        };
//    }
//
//    @Test(dataProvider = "testDirectoryProvider")
//    public void testCreateDirectoryUsingRawLibrary(String testdir, String[]
//            expectedItems, String message) throws RemoteDataException {
//        BlobStore blobstore = s3.getBlobStore();
//        blobstore.createDirectory(containerName, testdir);
//        PageSet<? extends StorageMetadata> pageset =
//                blobstore.list(containerName);
//        StorageMetadata[] listing = pageset.toArray(new StorageMetadata[]{});
//        for (String expectedItem : expectedItems) {
//            Assert.assertTrue(blobstore.directoryExists(containerName, testdir),
//                    "Failed to find " + expectedItem + " in bucket " + containerName);
//        }
//
//        for (StorageMetadata meta : listing) {
//            Assert.assertTrue(ArrayUtils.contains(expectedItems, meta.getName()),
//                    "Directory listing returned " + meta.getName() +
//                            " which was not in list of expected results");
//        }
//    }

    @Test//(dependsOnMethods = {"testCreateDirectoryUsingRawLibrary"})
    public void testRecursiveDirectoryCreation() throws RemoteDataException {
        BlobStore blobstore = s3.getBlobStore();
        String[] pathTokens = StringUtils.split(MISSING_DIRECTORY, "/");
        StringBuilder newdirectories = new StringBuilder();

        for (int i = 0; i < ArrayUtils.getLength(pathTokens); i++) {
            newdirectories.append(pathTokens[i]);
            BlobMetadata meta = blobstore.blobMetadata(containerName,
                    newdirectories.toString());
            if (meta == null) {
                blobstore.createDirectory(containerName, newdirectories.toString());
                newdirectories.append("/");
            } else if (meta.getType() != StorageType.FOLDER || meta.getType() !=
                    StorageType.CONTAINER) {
                // trying to create directory that is a file
                throw new RemoteDataException("Failed to create " + newdirectories +
                        ". File already exists.");
            } else {
                // ignore, directory already exists
            }
        }

        String subdirectories = MISSING_DIRECTORY;
        do {
            Assert.assertTrue(blobstore.directoryExists(containerName,
                    subdirectories), "Failed to find " +
                    subdirectories + " in bucket " + containerName + " after creating " +
                    MISSING_DIRECTORY);
            subdirectories = FilenameUtils.getPathNoEndSeparator(subdirectories);
        }
        while (!StringUtils.isEmpty(subdirectories));
    }

    @DataProvider
    public Object[][] testDirectoryDeletionProvider() {
        return new Object[][]{
                {testdirname, testdirname, true,
                        "Failed to delete directory created with no trailing slash"},
                {testdirname + "/", testdirname, false,
                        "Failed to delete directory created with trailing slash"},
                {testdirname + "/", testdirname + "/", false,
                        "Failed to delete directory created with trailing slash"},
                {testdirname + "/" + testdirname, testdirname, false,
                        "Failed to delete directory contents when deleting parent without trailing slash"
                },
                {testdirname + "/" + testdirname, testdirname + "/", false,
                        "Failed to delete directory contents when deleting parent without trailing slash"
                },
                {testdirname + "/" + testdirname + "/", testdirname, false,
                        "Failed to delete directory contents when deleting parent without trailing slash"
                },
                {testdirname + "/" + testdirname + "/", testdirname + "/", false,
                        "Failed to delete directory contents when deleting parent with trailing slash"
                },
        };
    }

    @Test(dataProvider = "testDirectoryDeletionProvider",
            dependsOnMethods = {"testRecursiveDirectoryCreation"})
    public void testDirectoryDeletion(String newdir, String
            directoryToDelete, boolean shouldBeDeleted, String message) throws
            IOException, RemoteDataException {
        BlobStore blobstore = s3.getBlobStore();
        s3.mkdirs(MISSING_DIRECTORY);
        blobstore.deleteDirectory(containerName, directoryToDelete);
        Assert.assertFalse(blobstore.directoryExists(containerName, newdir),
                message);
    }

    @Test(dependsOnMethods = {"testDirectoryDeletion"})
    public void testRecursiveDirectoryDeletion() throws IOException,
            RemoteDataException {
        String parentPath = MISSING_DIRECTORY;
        BlobStore blobstore = s3.getBlobStore();
        s3.mkdirs(MISSING_DIRECTORY);
        blobstore.deleteDirectory(containerName,
                StringUtils.substringBefore(MISSING_DIRECTORY, "/"));

        while (!StringUtils.isEmpty(parentPath)) {
            Assert.assertFalse(blobstore.directoryExists(containerName, parentPath),
                    "Directory " + parentPath +
                            " was not deleted when root folder was deleted.");
            parentPath = FilenameUtils.getPathNoEndSeparator(parentPath);
        }
    }

    @Test(dependsOnMethods = {"testRecursiveDirectoryDeletion"})
    public void testDirectoryContentDeletionFailsWithBlobsPresent() throws
            IOException, RemoteDataException {
        BlobStore blobstore = s3.getBlobStore();
        s3.mkdir(testdirname);

        File uploadFile = new File(LOCAL_BINARY_FILE);

        Blob blob = blobstore.blobBuilder(testdirname + "/deleteme.txt")
                .payload(uploadFile)
                .contentLength(uploadFile.length())
                .contentType(new MimetypesFileTypeMap().getContentType(uploadFile))
                .contentEncoding("UTF-8")
                .contentMD5((HashCode) null)
                .build();

        blobstore.putBlob(containerName, blob, multipart());

        blobstore.deleteDirectory(containerName, testdirname);

        Assert.assertTrue(blobstore.blobExists(containerName, testdirname +
                        "/deleteme.txt"),
                "Blob " + testdirname + "/deleteme.txt" +
                        " was not deleted when parent folder was deleted.");
        Assert.assertFalse(blobstore.directoryExists(containerName, testdirname),
                "Directory " + testdirname +
                        " was not deleted when explicitly deleted.");

    }

    @SuppressWarnings("deprecation")
    @Test(dependsOnMethods = {"testDirectoryContentDeletionFailsWithBlobsPresent"})
    public void testDirectoryContentDeletionSucceedsWhenBlobsDeleted() throws
            IOException, RemoteDataException {
        BlobStore blobstore = s3.getBlobStore();
        s3.mkdirs(MISSING_DIRECTORY);

        File uploadFile = new File(LOCAL_BINARY_FILE);

        String[] pathTokens = StringUtils.split(MISSING_DIRECTORY, "/");
        StringBuilder newdirectories = new StringBuilder();

        for (int i = 0; i < ArrayUtils.getLength(pathTokens); i++) {
            newdirectories.append(pathTokens[i] + "/");
            Blob blob = blobstore.blobBuilder(newdirectories + "deleteme.txt")
                    .payload(uploadFile)
                    .contentLength(uploadFile.length())
                    .contentType(new MimetypesFileTypeMap().getContentType(uploadFile))
                    .contentEncoding("UTF-8")
                    .contentMD5((HashCode) null)
                    .build();

            blobstore.putBlob(containerName, blob, multipart());
        }

        ListContainerOptions options = new ListContainerOptions();
        options.inDirectory(StringUtils.split(MISSING_DIRECTORY, "/")[0]);
        options.recursive();

        for (StorageMetadata meta : blobstore.list(containerName,
                options).toArray(new StorageMetadata[]{})) {
            System.out.println(meta.getType().name() + " - " + meta.getName());
            if (meta.getType() == StorageType.FOLDER || meta.getType() ==
                    StorageType.RELATIVE_PATH)
                blobstore.deleteDirectory(containerName, meta.getName());
            else
                blobstore.removeBlob(containerName, meta.getName());
        }

        blobstore.deleteDirectory(containerName,
                StringUtils.split(MISSING_DIRECTORY, "/")[0]);

        Assert.assertFalse(blobstore.blobExists(containerName, testdirname +
                        "/deleteme.txt"),
                "Blob " + testdirname + "/deleteme.txt" +
                        " was not deleted when parent folder was deleted.");
        Assert.assertFalse(blobstore.directoryExists(containerName, testdirname),
                "Directory " + testdirname +
                        " was not deleted when explicitly deleted.");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFileRename() throws IOException, RemoteDataException {
        AWSS3BlobStore blobstore = (AWSS3BlobStore) s3.getBlobStore();
        File uploadFile = new File(LOCAL_BINARY_FILE);

        // put single file
        Blob blob = blobstore.blobBuilder(uploadFile.getName())
                .payload(uploadFile)
                .contentLength(uploadFile.length())
                .contentType(new MimetypesFileTypeMap().getContentType(uploadFile))
                .contentEncoding("UTF-8")
                .contentMD5((HashCode) null)
                .build();

        blobstore.putBlob(containerName, blob, multipart());

        BlobMetadata meta = blobstore.blobMetadata(containerName,
                uploadFile.getName());

        ApiContext<AWSS3Client> providerContext =
                s3.context.unwrap();

        providerContext.getApi().copyObject(containerName, uploadFile.getName(),
                containerName, uploadFile.getName() + "-bak", null);
        blobstore.removeBlob(containerName, uploadFile.getName());

        Assert.assertTrue(blobstore.blobExists(containerName,
                uploadFile.getName() + "-bak"));
        Assert.assertFalse(blobstore.blobExists(containerName,
                uploadFile.getName()));
    }

    @SuppressWarnings("deprecation")
    public void testDirectoryRename() throws IOException, RemoteDataException {
        String srcDir = StringUtils.split(MISSING_DIRECTORY, "/")[0];
        String destDir = srcDir + "-85";

        BlobStore blobstore = s3.getBlobStore();
        // put directory tree
        s3.mkdirs(MISSING_DIRECTORY);

        File uploadFile = new File(LOCAL_BINARY_FILE);

        String[] pathTokens = StringUtils.split(MISSING_DIRECTORY, "/");
        StringBuilder newdirectories = new StringBuilder();

        for (int i = 0; i < ArrayUtils.getLength(pathTokens); i++) {
            newdirectories.append(pathTokens[i] + "/");
            Blob blob = blobstore.blobBuilder(newdirectories + "deleteme.txt")
                    .payload(uploadFile)
                    .contentLength(uploadFile.length())
                    .contentType(new MimetypesFileTypeMap().getContentType(uploadFile))
                    .contentEncoding("UTF-8")
                    .contentMD5((HashCode) null)
                    .build();

            blobstore.putBlob(containerName, blob, multipart());
        }

        ApiContext<AWSS3Client> providerContext =
                s3.context.unwrap();
        PageSet<? extends StorageMetadata> pageset = null;
        do {
            ListContainerOptions options = new ListContainerOptions();
            options.inDirectory(srcDir);
            options.recursive();
            if (pageset != null && StringUtils.isEmpty(pageset.getNextMarker())) {
                options.afterMarker(pageset.getNextMarker());
            }

            pageset = blobstore.list(containerName, options);
            StorageMetadata meta = null;
            for (Iterator<? extends StorageMetadata> iter = pageset.iterator();
                 iter.hasNext(); meta = iter.next()) {
                if (meta == null) continue;
                System.out.println(meta.getType().name() + " - " + meta.getName());
                String destPath = StringUtils.replaceOnce(meta.getName(), srcDir,
                        destDir);

                if (meta.getType() == StorageType.FOLDER || meta.getType() ==
                        StorageType.RELATIVE_PATH) {
                    // creating remote destination folder
                    System.out.println("MKDIRS " + destPath);
                    s3.mkdirs(destPath);
                    blobstore.deleteDirectory(containerName, meta.getName());
                } else {
                    // copying source file
                    System.out.println("CP " + meta.getName() + " " + destPath);
                    providerContext.getApi().copyObject(containerName, meta.getName(),
                            containerName, destPath);
                    blobstore.removeBlob(containerName, meta.getName());
                }

            }
        }
        while (!StringUtils.isEmpty(pageset.getNextMarker()));

        providerContext.getApi().copyObject(containerName, srcDir, containerName,
                destDir);
        blobstore.deleteDirectory(containerName, srcDir);

        Assert.assertFalse(blobstore.directoryExists(containerName, srcDir),
                "Directory " + srcDir + " was not deleted when explicitly deleted.");

        ListContainerOptions options = new ListContainerOptions();
        options.inDirectory(srcDir);
        options.recursive();
        if (pageset != null && StringUtils.isEmpty(pageset.getNextMarker())) {
            options.afterMarker(pageset.getNextMarker());
        }

        pageset = blobstore.list(containerName, options);

        Assert.assertTrue(pageset.isEmpty(),
                "Directory " + srcDir + " was not recursively deleted after rename.");
    }

    @Test
    // (dependsOnMethods={"testDirectoryRename"})
    public void testFileOverwritesDirectory() throws IOException, RemoteDataException {
        BlobStore blobstore = s3.getBlobStore();
        File uploadFile = new File(LOCAL_BINARY_FILE);

        s3.mkdirs(uploadFile.getName());

        ByteSource payload = Files.asByteSource(uploadFile);
        try {
            Blob blob = blobstore.blobBuilder(uploadFile.getName()).payload(payload)
                    .contentLength(uploadFile.length())
                    .contentType(new MimetypesFileTypeMap().getContentType(uploadFile))
                    .contentEncoding("UTF-8").contentMD5((HashCode) null).build();

            blobstore.putBlob(containerName, blob, multipart());
            Assert.fail("Overwriting directory with file should throw exception.");
        } catch (java.lang.IllegalStateException e) {
            // this should be throws for conflicting object names
        } catch (Throwable t) {
            Assert.fail("Overwriting directory with file should throw IllegalStateException.", t);
        }

        //
        // Assert.assertTrue(blobstore.blobExists(containerName, testdirname +
        // "/deleteme.txt"),
        // "Blob " + testdirname + "/deleteme.txt" +
        // " was not deleted when parent folder was deleted.");
        // Assert.assertFalse(blobstore.directoryExists(containerName,
        // testdirname),
        // "Directory " + testdirname +
        // " was not deleted when explicitly deleted.");
        //
    }

    // @DataProvider
    // public Object[][] testExistenceCheckProvider() {
    // return new Object[][] {
    // // { testdirname, true, "Failed to create " + testdirname },
    // // { "/" + testdirname, false,
    // "Directories with leading slashes should not be created" },
    // // { testdirname + "/", false,
    // "Directories with trailing slashes should not be created" },
    // // { "/" + testdirname + "/", false,
    // "Directories with bookend slashes should not be created" },
    // };
    // }
    // @Test(dataProvider="testExistenceCheckProvider",
    // dependsOnMethods={"testDirectoryDeletion"})
    // public void testExistenceCheck(String testdir, boolean shouldExist,
    // String message)
    // {
    // BlobStore blobstore = s3.getBlobStore();
    // blobstore.createDirectory(containerName, testdir);
    // Assert.assertEquals(blobstore.directoryExists(containerName, testdir),
    // shouldBeCreated, message);
    // }

}
