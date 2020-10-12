/**
 * 
 */
package org.iplantc.service.transfer.sftp;

import org.iplantc.service.transfer.IRemoteDataClientIT;
import org.iplantc.service.transfer.RemoteDataClientTestUtils;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.s3.TransferTestRetryAnalyzer;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * @author dooley
 *
 */
@Test(singleThreaded = false, groups={"sftp","sftp.operations"})
public class SftpSshKeysRemoteDataClientIT extends RemoteDataClientTestUtils implements IRemoteDataClientIT {

	protected String containerName;

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp-sshkeys.example.com.json");
	}

	@Override
	protected String getForbiddenDirectoryPath(boolean shouldExist) {
		if (shouldExist) {
			return "/root";
		} else {
			return "/root/" + UUID.randomUUID().toString();
		}
	}

	@Override
	@Test(groups={"proxy"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isPermissionMirroringRequired() {
		_isPermissionMirroringRequired();
	}

	@Override
	@Test(groups={"proxy"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isThirdPartyTransferSupported()
	{
		_isThirdPartyTransferSupported();
	}

	@Override
	@Test(groups={"stat"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getFileInfoReturnsRemoteFileInfoForFile() {
		_getFileInfoReturnsRemoteFileInfoForFile();
	}

	@Override
	@Test(groups={"stat"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getFileInfoReturnsRemoteFileInfoForDirectory() {
		_getFileInfoReturnsRemoteFileInfoForDirectory();
	}

	@Override
	@Test(groups={"stat"}, expectedExceptions = FileNotFoundException.class, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getFileInfoReturnsErrorOnMissingPath() throws FileNotFoundException {
		_getFileInfoReturnsErrorOnMissingPath();
	}

	@Override
	@Test(groups={"exists"}, dataProvider="doesExistProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void doesExist(String remotedir, boolean shouldExist, String message) {
		_doesExist(remotedir, shouldExist, message);
	}

	@Override
	@Test(groups={"size"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void length()
	{
		_length();
	}

	@Override
	@Test(groups={"mkdir"}, dataProvider="mkdirProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void mkdir(String remotedir, boolean shouldReturnFalse, boolean shouldThrowException, String message) {
		_mkdir(remotedir, shouldReturnFalse, shouldThrowException, message);
	}

	@Override
	@Test(groups={"mkdir"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void mkdirWithoutRemotePermissionThrowsRemoteDataException() {
		_mkdirWithoutRemotePermissionThrowsRemoteDataException();
	}

	@Override
	@Test(groups={"mkdir"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void mkdirThrowsRemoteDataExceptionIfDirectoryAlreadyExists() {
		_mkdirThrowsRemoteDataExceptionIfDirectoryAlreadyExists();
	}

	@Override
	@Test(groups={"mkdir"}, dataProvider="mkdirsProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void mkdirs(String remotedir, boolean shouldThrowException, String message) {
		_mkdirs(remotedir, shouldThrowException, message);
	}

	@Override
	@Test(groups={"mkdir"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void mkdirsWithoutRemotePermissionThrowsRemoteDataException() {
		_mkdirsWithoutRemotePermissionThrowsRemoteDataException();
	}

	@Override
	@Test(groups={"put"}, dataProvider = "putFileProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFile(String remotePath, String expectedRemoteFilename, boolean shouldThrowException, String message) {
		_putFile(remotePath, expectedRemoteFilename, shouldThrowException, message);
	}

	@Override
	@Test(groups={"put"}, dataProvider="putFileOutsideHomeProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFileOutsideHome(String remoteFilename, String expectedRemoteFilename, boolean shouldThrowException, String message) {
		_putFileOutsideHome(remoteFilename, expectedRemoteFilename, shouldThrowException, message);
	}

	@Override
	@Test(groups={"put"}, dataProvider="putFolderProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFolderCreatesRemoteFolder(String remotePath, String expectedRemoteFilename, boolean shouldThrowException, String message) {
		_putFolderCreatesRemoteFolder(remotePath, expectedRemoteFilename, shouldThrowException, message);
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFolderCreatesRemoteSubFolderWhenNamedRemotePathExists() {
		_putFolderCreatesRemoteSubFolderWhenNamedRemotePathExists();
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFolderMergesContentsWhenRemoteFolderExists()
	{
		_putFolderMergesContentsWhenRemoteFolderExists();
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFileOverwritesExistingFile()
	{
		_putFileOverwritesExistingFile();
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFileWithoutRemotePermissionThrowsRemoteDataException() {
		_putFileWithoutRemotePermissionThrowsRemoteDataException();
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFolderWithoutRemotePermissionThrowsRemoteDataException() {
		_putFolderWithoutRemotePermissionThrowsRemoteDataException();
	}

	@Override
	@Test(groups={"put"}, dataProvider="putFolderProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFolderOutsideHome(String remoteFilename, String expectedRemoteFilename, boolean shouldThrowException, String message) {
		_putFolderOutsideHome(remoteFilename, expectedRemoteFilename, shouldThrowException, message);
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFileFailsToMissingDestinationPath()
	{
		_putFileFailsToMissingDestinationPath();
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFolderFailsToMissingDestinationPath()
	{
		_putFolderFailsToMissingDestinationPath();
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFailsForMissingLocalPath()
	{
		_putFailsForMissingLocalPath();
	}

	@Override
	@Test(groups={"put"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void putFolderFailsToRemoteFilePath()
	{
		_putFolderFailsToRemoteFilePath();
	}

	@Override
	@Test(groups={"delete"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void delete()
	{
		_delete();
	}

	@Override
	@Test(groups={"delete"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void deleteFailsOnMissingDirectory()
	{
		_deleteFailsOnMissingDirectory();
	}

	@Override
	@Test(groups={"delete"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void deleteThrowsExceptionWhenNoPermission()
	{
		_deleteThrowsExceptionWhenNoPermission();
	}

	@Override
	@Test(groups={"is"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isDirectoryTrueForDirectory()
	{
		_isDirectoryTrueForDirectory();
	}

	@Override
	@Test(groups={"is"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isDirectoryFalseForFile()
	{
		_isDirectoryFalseForFile();
	}

	@Override
	@Test(groups={"is"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isDirectorThrowsExceptionForMissingPath()
	{
		_isDirectorThrowsExceptionForMissingPath();
	}

	@Override
	@Test(groups={"is"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isFileFalseForDirectory()
	{
		_isFileFalseForDirectory();
	}

	@Override
	@Test(groups={"is"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isFileTrueForFile()
	{
		_isFileTrueForFile();
	}

	@Override
	@Test(groups={"is"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void isFileThrowsExceptionForMissingPath()
	{
		_isFileThrowsExceptionForMissingPath();
	}

	@Override
	@Test(groups={"list"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void ls()
	{
		_ls();
	}

	@Override
	@Test(groups={"list"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void lsFailsOnMissingDirectory()
	{
		_lsFailsOnMissingDirectory();
	}

	@Override
	@Test(groups={"list"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void lsThrowsExceptionWhenNoPermission()
	{
		_lsThrowsExceptionWhenNoPermission();
	}

	@Override
	@Test(groups={"get"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getThrowsExceptionOnMissingRemotePath()
	{
		_getThrowsExceptionOnMissingRemotePath();
	}

	@Override
	@Test(groups={"get"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getThrowsExceptionWhenDownloadingFolderToLocalFilePath() {
		_getThrowsExceptionWhenDownloadingFolderToLocalFilePath();
	}

	@Override
	@Test(groups={"get"}, expectedExceptions = FileNotFoundException.class, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getDirectoryThrowsExceptionWhenDownloadingFolderToNonExistentLocalPath() throws FileNotFoundException {
		_getDirectoryThrowsExceptionWhenDownloadingFolderToNonExistentLocalPath();
	}

	@Override
	@Test(groups={"get"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath() {
		_getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath();
	}

	@Override
	@Test(groups={"get"}, dataProvider="getDirectoryRetrievesToCorrectLocationProvider", retryAnalyzer = TransferTestRetryAnalyzer.class)
	public void getDirectoryRetrievesToCorrectLocation(String localdir, boolean createTestDownloadFolder, String expectedDownloadPath, String message) {
		_getDirectoryRetrievesToCorrectLocation(localdir, createTestDownloadFolder, expectedDownloadPath, message);
	}

	@Override
	@Test(groups={"get"}, dataProvider="getFileRetrievesToCorrectLocationProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getFileRetrievesToCorrectLocation(Path localPath, Path expectedDownloadPath, String message) {
		_getFileRetrievesToCorrectLocation(localPath, expectedDownloadPath, message);
	}

	@Override
	@Test(groups={"get"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getFileOverwritesExistingFile()
	{
		_getFileOverwritesExistingFile();
	}

    @Override
	@Test(groups={"putstream"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getOutputStream()
	{
		_getOutputStream();
	}

	@Override
	@Test(groups={"putstream"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getOutputStreamFailsWhenRemotePathIsNullOrEmpty()
	{
		_getOutputStreamFailsWhenRemotePathIsNullOrEmpty();
	}

	@Override
	@Test(groups={"putstream"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getOutputStreamFailsWhenRemotePathIsDirectory()
	{
		_getOutputStreamFailsWhenRemotePathIsDirectory();
	}

	@Override
	@Test(groups={"putstream"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getOutputStreamFailsOnMissingPath()
	{
		_getOutputStreamFailsOnMissingPath();
	}

	@Override
	@Test(groups={"putstream"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getOutputStreamThrowsExceptionWhenNoPermission()
	{
		_getOutputStreamThrowsExceptionWhenNoPermission();
	}

	@Override
	@Test(groups={"getstream"}, dataProvider="getInputStreamProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getInputStream(String localFile, String message)
	{
		_getInputStream(localFile, message);
	}

	@Override
	@Test(groups={"getstream"}, dataProvider="getInputStreamOnRemoteDirectoryThrowsExceptionProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getInputStreamOnRemoteDirectoryThrowsException(String remotedir, String message) {
		_getInputStreamOnRemoteDirectoryThrowsException(remotedir, message);
	}

	@Override
	@Test(groups={"getstream"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void getInputStreamThrowsExceptionWhenNoPermission()
	{
		_getInputStreamThrowsExceptionWhenNoPermission();
	}

	@Override
	@Test(groups={"checksum"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void checksumDirectoryFails()
	{
		_checksumDirectoryFails();
	}

	@Override
	@Test(groups={"checksum"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void checksumMissingPathThrowsException()
	{
		_checksumMissingPathThrowsException();
	}

	@Override
	@Test(groups={"checksum"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void checksum()
	{
		_checksum();
	}

	@Override
	@Test(groups={"rename"}, dataProvider="doRenameProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void doRename(String oldpath, String newpath, boolean shouldThrowException, String message) {
		_doRename(oldpath, newpath, shouldThrowException, message);
	}

	@Override
	@Test(groups={"rename"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void doRenameThrowsRemotePermissionExceptionToRestrictedSource() {
		_doRenameThrowsRemotePermissionExceptionToRestrictedSource();
	}

	@Override
	@Test(groups={"rename"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void doRenameThrowsRemotePermissionExceptionToRestrictedDest() {
		_doRenameThrowsRemotePermissionExceptionToRestrictedDest();
	}

	@Override
	@Test(groups={"rename"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void doRenameThrowsFileNotFoundExceptionOnMissingDestPath() {
		_doRenameThrowsFileNotFoundExceptionOnMissingDestPath();
	}

	@Override
	@Test(groups={"rename"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void doRenameThrowsFileNotFoundExceptionOnMissingSourcePath() {
		_doRenameThrowsFileNotFoundExceptionOnMissingSourcePath();
	}

	@Override
	@Test(enabled=false)
	public void getUrlForPath()
	{
		_getUrlForPath();
	}

	@Override
	@Test(groups={"copy"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void copyFile()
	{
		_copyFile();
	}

	@Override
	@Test(groups={"copy"}, dataProvider="copyIgnoreSlashesProvider", retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void copyDir(String src, String dest, boolean createDest, String expectedPath, boolean shouldThrowException, String message) {
		_copyDir(src, dest, createDest, expectedPath, shouldThrowException, message);
	}

	@Override
	@Test(groups={"copy"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void copyThrowsRemoteDataExceptionToRestrictedDest()
	{
		_copyThrowsRemoteDataExceptionToRestrictedDest();
	}

	@Override
	@Test(groups={"copy"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void copyThrowsFileNotFoundExceptionOnMissingDestPath() {
		_copyThrowsFileNotFoundExceptionOnMissingDestPath();

	}

	@Override
	@Test(groups={"copy"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void copyThrowsFileNotFoundExceptionOnMissingSourcePath() {
		_copyThrowsFileNotFoundExceptionOnMissingSourcePath();
	}

	@Override
	@Test(groups={"copy"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void copyThrowsRemoteDataExceptionToRestrictedSource()
	{
		_copyThrowsRemoteDataExceptionToRestrictedSource();
	}

	@Override
	@Test(groups={"sync"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void syncToRemoteFile()
	{
		_syncToRemoteFile();
	}

	@Override
	@Test(groups={"sync"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void syncToRemoteFolderAddsMissingFiles()
	{
		_syncToRemoteFolderAddsMissingFiles();
	}

	@Override
	@Test(groups={"sync"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void syncToRemoteFolderLeavesExistingFilesUntouched()
	{
		_syncToRemoteFolderLeavesExistingFilesUntouched();
	}

	@Override
	@Test(groups={"sync"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void syncToRemoteFolderOverwritesFilesWithDeltas()
	{
		_syncToRemoteFolderOverwritesFilesWithDeltas();
	}

	@Override
	@Test(groups={"sync"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void syncToRemoteFolderReplacesOverlappingFilesAndFolders() {
		_syncToRemoteFolderReplacesOverlappingFilesAndFolders();
	}
	
	@Override
	@Test(groups={"sync"}, retryAnalyzer=TransferTestRetryAnalyzer.class)
	public void syncToRemoteSyncesSubfoldersRatherThanEmbeddingThem() throws IOException, RemoteDataException {
		_syncToRemoteSyncesSubfoldersRatherThanEmbeddingThem();
	}
}
