package org.iplantc.service.transfer;

import com.sshtools.sftp.FileTransferProgress;
import org.globus.ftp.MarkerListener;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferTask;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

public interface RemoteTransferListener extends MarkerListener, TransferStatusCallbackListener, FileTransferProgress {

    /**
     * Returns the {@link TransferTask} associated with this transfer.
     * @return
     */
    TransferTask getTransferTask();

    /**
     * @param transferTask the transferTask to set
     * @throws InterruptedException
     */
    void setTransferTask(TransferTask transferTask);

    /**
     * For recursive copies, this returns the transfer task of the parent task.
     * @return the parent {@link TransferTask} of a recursive copy
     */
    TransferStatus getOverallStatusCallback();

    /**
     * Called when a file item is being skipped due to previous copy or blacklisting
     * @param totalSize size of the skipped fileitem
     * @param remoteFile remote path of the skipped file item
     */
    void skipped(long totalSize, String remoteFile);

    /**
     * Marks a transfer as cancelled. This is called by the respective cancel methods of the
     * underlying protocol callback listeners.
     */
    void cancel();

    /**
     * Marks a transfer as failed. This is called by the respective failure callback methods of the
     * underlying protocol callback listeners.
     */
    void failed();

    /**
     * Creates a new child transfer task at the given source and destination paths with
     * this listener's {@link #getTransferTask()} as the parent.
     *
     * @param childSourcePath the source of the child {@link TransferTask}
     * @param childDestPath the dest of the child {@link TransferTask}
     * @return the persisted {@link TransferTask}
     * @throws TransferException if the child transfer task cannot be saved
     */
    public TransferTask createAndPersistChildTransferTask(String childSourcePath, String childDestPath) throws TransferException;


    /**
     * Creates a new concrete {@link RemoteTransferListener} using the context of current object and the
     * paths of the child.
     * * @param childSourcePath the source of the child {@link TransferTask}
     * @param childDestPath the dest of the child {@link TransferTask}
     * @return the persisted {@link RemoteTransferListener}
     * @throws TransferException if the child remote transfer task listener cannot be saved
     */
    public RemoteTransferListener createChildRemoteTransferListener(String childSourcePath, String childDestPath) throws TransferException;

    /**
     * Creates a new concrete {@link RemoteTransferListener} using the context of current object and the
     * child {@link TransferTask}.
     *
     * @param childTransferTask the the child {@link TransferTask}
     * @return the persisted {@link RemoteTransferListener}
     */
    public RemoteTransferListener createChildRemoteTransferListener(TransferTask childTransferTask);

    /**
     * Returns true if the transfer has been cancelled.
     * @return true if cancelled
     */
    boolean isCancelled();
}
