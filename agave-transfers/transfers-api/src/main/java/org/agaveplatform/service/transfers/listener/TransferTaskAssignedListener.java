package org.agaveplatform.service.transfers.listener;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;


public class TransferTaskAssignedListener extends AbstractTransferTaskListener {
    private final Logger logger = LoggerFactory.getLogger(TransferTaskAssignedListener.class);

    protected HashSet<String> interruptedTasks = new HashSet<String>();

    protected static final String EVENT_CHANNEL = "transfertask.assigned";

    public String getDefaultEventChannel() {
        return EVENT_CHANNEL;
    }

    public TransferTaskAssignedListener(Vertx vertx) {
        super(vertx);
    }

    public TransferTaskAssignedListener(Vertx vertx, String eventChannel) {
        super(vertx, eventChannel);
    }

    @Override
    public void start() {
        EventBus bus = vertx.eventBus();
        bus.<JsonObject>consumer(getEventChannel(), msg -> {
            JsonObject body = msg.body();
            String uuid = body.getString("uuid");
            String source = body.getString("source");
            String dest = body.getString("dest");
            logger.info("Transfer task {} assigned: {} -> {}", uuid, source, dest);
            this.processTransferTask(body);
        });

        bus.<JsonObject>consumer("transfertask.cancel.sync", msg -> {
            JsonObject body = msg.body();
            String uuid = body.getString("uuid");

            logger.info("Transfer task {} cancel detected", uuid);
            this.interruptedTasks.add(uuid);
        });

        bus.<JsonObject>consumer("transfertask.cancel.complete", msg -> {
            JsonObject body = msg.body();
            String uuid = body.getString("uuid");

            logger.info("Transfer task {} cancel completion detected. Updating internal cache.", uuid);
            this.interruptedTasks.remove(uuid);
        });

        // paused tasks
        bus.<JsonObject>consumer("transfertask.paused.sync", msg -> {
            JsonObject body = msg.body();
            String uuid = body.getString("uuid");

            logger.info("Transfer task {} paused detected", uuid);
            this.interruptedTasks.add(uuid);
        });

        bus.<JsonObject>consumer("transfertask.paused.complete", msg -> {
            JsonObject body = msg.body();
            String uuid = body.getString("uuid");

            logger.info("Transfer task {} paused completion detected. Updating internal cache.", uuid);
            this.interruptedTasks.remove(uuid);
        });
    }

    protected String processTransferTask(JsonObject body) {
        String uuid = body.getString("uuid");
        String source = body.getString("source");
//		String dest =  body.getString("dest");
        String username = body.getString("owner");
        String tenantId = body.getString("tenant_id");
        String protocol = null;
        TransferTask bodyTask = new TransferTask(body);

        try {

            URI srcUri;
            try {
                srcUri = URI.create(source);
            } catch (Exception e) {
                String msg = String.format("Unable to parse source uri %s for transfertask %s: %s",
                        source, uuid, e.getMessage());
                throw new RemoteDataSyntaxException(msg, e);
            }

            // check for task interruption
            if (!isTaskInterrupted(bodyTask)) {
                // basic sanity check on uri again
                if (RemoteDataClientFactory.isSchemeSupported(srcUri)) {
                    // if it's an "agave://" uri, look up the connection info, get a rdc, and process the remote
                    // file item
                    if (srcUri.getScheme().equalsIgnoreCase("agave")) {
                        // pull the system out of the url. system id is the hostname in an agave uri
                        RemoteSystem srcSystem = new SystemDao().findBySystemId(srcUri.getHost());
                        // get a remtoe data client for the sytem
                        RemoteDataClient client = srcSystem.getRemoteDataClient();
                        // stat the remote path to check its type
                        RemoteFileInfo fileInfo = client.getFileInfo(srcUri.getPath());

                        // if the path is a file, then we can move it directly, so we raise an event telling the protocol
                        // listener to move the file item
                        if (fileInfo.isFile()) {
                            // write to the protocol event channel. the uri is all they should need for this....
                            // might need tenant id. not sure yet.
                            _doPublishEvent("transfer." + srcSystem.getStorageConfig().getProtocol().name().toLowerCase(),
                                        body);//"agave://" + srcSystem.getSystemId() + "/" + srcUri.getPath());
                        } else {
                            // path is a directory, so walk the first level of the directory
                            client.ls(srcUri.getPath())
                                    .stream()
                                    .takeWhile(rfi -> {
                                        // check for an interrupt that comes while processing a potentially large dir listing
                                        return !isTaskInterrupted(bodyTask);
                                    })
                                    .forEach(childFileItem -> {
                                        // if it's a file, we can process this as we would if the original path were a file
                                        if (childFileItem.isFile()) {
                                            // build the child paths
                                            String childSource = body.getString("source") + "/" + childFileItem.getName();
                                            String childDest = body.getString("dest") + "/" + childFileItem.getName();

                                            TransferTask transferTask = new TransferTask(childSource, childDest, tenantId);
                                            transferTask.setTenantId(tenantId);
                                            transferTask.setOwner(username);
                                            transferTask.setParentTaskId(uuid);
                                            if (StringUtils.isNotEmpty(body.getString("rootTask"))) {
                                                transferTask.setRootTaskId(body.getString("rootTaskId"));
                                            }
                                            _doPublishEvent("transfertask.created", transferTask.toJson());

//                                            _doPublishEvent("transfertask." + srcSystem.getType(),
//                                                    "agave://" + srcSystem.getSystemId() + "/" + srcUri.getPath() + "/" + childFileItem.getName());
                                        }
                                        // if a directory, then create a new transfer task to repeat this process,
                                        // keep the association between this transfer task, the original, and the children
                                        // in place for traversal in queries later on.
                                        else {
                                            // build the child paths
                                            String childSource = body.getString("source") + "/" + childFileItem.getName();
                                            String childDest = body.getString("dest") + "/" + childFileItem.getName();

                                            TransferTask transferTask = new TransferTask(childSource, childDest, tenantId);
                                            transferTask.setTenantId(tenantId);
                                            transferTask.setOwner(username);
                                            transferTask.setParentTaskId(uuid);
                                            if (StringUtils.isNotEmpty(body.getString("rootTask"))) {
                                                transferTask.setRootTaskId(body.getString("rootTaskId"));
                                            }
                                            _doPublishEvent("transfertask.created", transferTask.toJson());
                                        }
                                    });
                        }
                    }
                    // it's not an agave uri, so we forward on the raw uri as we know that we can
                    // handle it from the wrapping if statement check
                    else {
                        _doPublishEvent("transfer." + srcUri.getScheme(), body);
                    }
                } else {
                    // tell everyone else that you killed this task
                    throw new InterruptedException(String.format("Transfer task %s interrupted due to cancel event", uuid));
                }
            } else {
                String msg = String.format("Unknown source schema %s for the transfertask %s",
                        srcUri.getScheme(), uuid);
                throw new RemoteDataSyntaxException(msg);
            }
        }
        catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            JsonObject json = new JsonObject()
                    .put("cause", e.getClass().getName())
                    .put("message", e.getMessage())
                    .mergeIn(body);

            _doPublishEvent("transfertask.error", json);
        }
        finally {
            // any interrupt involving this task will be processeda t this point, so acknowledge
            // the task has been processed
            if (isTaskInterrupted(bodyTask)) {
                _doPublishEvent("transfertask.cancel.ack", body);
            }
        }

        return protocol;
    }

    /**
     * Checks whether the transfer task or any of its children exist in the list of
     * interrupted tasks.
     *
     * @param transferTask the current task being checked from the running task
     * @return true if the transfertask's uuid, parentTaskId, or rootTaskId are in the {@link #isTaskInterrupted(TransferTask)} list
     */
    public boolean isTaskInterrupted( TransferTask transferTask ){
        return this.interruptedTasks.contains(transferTask.getUuid()) ||
                this.interruptedTasks.contains(transferTask.getParentTaskId()) ||
                this.interruptedTasks.contains(transferTask.getRootTaskId());
    }

}
