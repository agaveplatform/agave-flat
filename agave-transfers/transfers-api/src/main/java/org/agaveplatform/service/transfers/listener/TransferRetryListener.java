package org.agaveplatform.service.transfers.listener;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseService;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.agaveplatform.service.transfers.enumerations.TransferStatusType;
import org.agaveplatform.service.transfers.exception.InterruptableTransferTaskException;
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
import org.agaveplatform.service.transfers.listener.TransferTaskAssignedListener;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

import static org.agaveplatform.service.transfers.TransferTaskConfigProperties.CONFIG_TRANSFERTASK_DB_QUEUE;
import static org.agaveplatform.service.transfers.enumerations.MessageType.TRANSFER_COMPLETED;
import static org.agaveplatform.service.transfers.enumerations.TransferStatusType.COMPLETED;

public class TransferRetryListener extends AbstractTransferTaskListener{
	private final Logger logger = LoggerFactory.getLogger(TransferRetryListener.class);

	protected HashSet<String> interruptedTasks = new HashSet<String>();
	private TransferTaskDatabaseService dbService;
	protected static final String EVENT_CHANNEL = MessageType.TRANSFER_RETRY;
	public String getDefaultEventChannel() {
		return EVENT_CHANNEL;
	}
	public TransferRetryListener(Vertx vertx) {
		super(vertx);
	}
	public TransferRetryListener(Vertx vertx, String eventChannel) {
		super(vertx, eventChannel);
	}

	@Override
	public void start() {
		// init our db connection from the pool
		String dbServiceQueue = config().getString(CONFIG_TRANSFERTASK_DB_QUEUE);
		dbService = TransferTaskDatabaseService.createProxy(vertx, dbServiceQueue);

		EventBus bus = vertx.eventBus();
		bus.<JsonObject>consumer(getEventChannel(), msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");
			String source = body.getString("source");
			String dest = body.getString("dest");
			logger.info("Transfer task {} assigned: {} -> {}", uuid, source, dest);
			this.retryProcessTransferTask(body);
		});

		// cancel tasks
		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_SYNC, msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			logger.info("Transfer task {} cancel detected", uuid);
			super.processInterrupt("add", body);
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			logger.info("Transfer task {} cancel completion detected. Updating internal cache.", uuid);
			super.processInterrupt("remove", body);
		});

		// paused tasks
		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_SYNC, msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			logger.info("Transfer task {} paused detected", uuid);
			super.processInterrupt("add", body);
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			logger.info("Transfer task {} paused completion detected. Updating internal cache.", uuid);
			super.processInterrupt("remove", body);
		});
	}

//	public void addInteruptedTask(String uuid){
//		this.interruptedTasks.add(uuid);
//	}
//	public void removeInteruptedTask(String uuid){
//		this.interruptedTasks.remove(uuid);
//	}

	protected void retryProcessTransferTask(JsonObject body) {
		String uuid = body.getString("uuid");
		String source = body.getString("source");
		String dest =  body.getString("dest");
		String username = body.getString("owner");
		String tenantId = body.getString("tenantId");
		String protocol = null;
		Integer attempts = body.getInteger("attempts");

		TransferTask bodyTask = new TransferTask(body);

		Promise<Boolean> promise = Promise.promise();

		// check to see if the uuid is Canceled or Completed
		this.getDbService().getById(tenantId, uuid, reply -> {
			if (reply.succeeded()) {
				TransferTask transferTaskDb = new TransferTask(new JsonObject(String.valueOf(reply)));
				if (transferTaskDb.getStatus() != TransferStatusType.CANCELLED ||
						transferTaskDb.getStatus() != TransferStatusType.COMPLETED ||
						transferTaskDb.getStatus() != TransferStatusType.FAILED ||
						transferTaskDb.getStatus() != TransferStatusType.TRANSFERRING) {
					// we're good to to go forward.

					// the status is not in the states above.  Now check to see if the # of attempts exceeds the max
					int configMaxTries = config().getInteger("transfertask.max.tries");
					if (configMaxTries <= transferTaskDb.getAttempts()) {
						// # of retries is less.

						// increment the attempts
						transferTaskDb.setAttempts( attempts +1 );
						getDbService().update(tenantId, uuid, transferTaskDb, updateBody -> {
							if (updateBody.succeeded()) {
								logger.info("[{}] Transfer task {} updated.", tenantId, uuid);
								promise.handle(Future.succeededFuture(Boolean.TRUE));
							} else {
								logger.error("[{}] Task {} update failed: {}",
										tenantId, uuid, reply.cause());
								JsonObject json = new JsonObject()
										.put("cause", updateBody.cause().getClass().getName())
										.put("message", updateBody.cause().getMessage())
										.mergeIn(body);
								_doPublishEvent(MessageType.TRANSFERTASK_ERROR, json);
								promise.handle(Future.failedFuture(updateBody.cause()));
							}
						});

						try {
							if (! super.isTaskInterrupted(bodyTask)) {
								processRetry(transferTaskDb.toJson());
							}else {
								logger.info("Transfer was Canceled or Paused for uuid {}", uuid);
							}
						} catch (InterruptableTransferTaskException e) {
							e.printStackTrace();
						}

						//promise.handle(Future.failedFuture(reply.cause()));
					} else {
						// this handles the failure of the getDbService
						//promise.handle(Future.succeededFuture(Boolean.FALSE));
					}
				}
				promise.handle(Future.succeededFuture(Boolean.TRUE));
			}

		});
		//return protocol;
	}


	public void processRetry(JsonObject body) {

		String uuid = body.getString("uuid");
		String source = body.getString("source");
		String dest =  body.getString("dest");
		String username = body.getString("owner");
		String tenantId = body.getString("tenantId");
		String protocol = null;
		Integer attempts = body.getInteger("attempts");
		TransferTask bodyTask = new TransferTask(body);

		try {

			URI srcUri;
			URI destUri;
			try {
				srcUri = URI.create(source);
				destUri = URI.create(dest);
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
							RemoteDataClient srcClient = srcSystem.getRemoteDataClient();

							// pull the dest system out of the url. system id is the hostname in an agave uri
							RemoteSystem destSystem = new SystemDao().findBySystemId(destUri.getHost());
							RemoteDataClient destClient = destSystem.getRemoteDataClient();

							// stat the remote path to check its type
							RemoteFileInfo fileInfo = srcClient.getFileInfo(srcUri.getPath());

							// if the path is a file, then we can move it directly, so we raise an event telling the protocol
							// listener to move the file item
							if (fileInfo.isFile()) {
								// write to the protocol event channel. the uri is all they should need for this....
								// might need tenant id. not sure yet.
								_doPublishEvent("transfer." + srcSystem.getStorageConfig().getProtocol().name().toLowerCase(),
										body);//"agave://" + srcSystem.getSystemId() + "/" + srcUri.getPath());
							} else {
								// path is a directory, so walk the first level of the directory
								List<RemoteFileInfo> remoteFileInfoList = srcClient.ls(srcUri.getPath());

								// if the directory is emnpty, mark as complete and exit
								if (remoteFileInfoList.isEmpty()) {
									_doPublishEvent(TRANSFER_COMPLETED, body);
								}
								// if there are contents, walk the first level, creating directories on the remote side
								// as we go to ensure that out of order processing by worker tasks can still succeed.
								else {
									// create the remote directory to ensure it's present when the transfers begin. This
									// also allows us to check for things like permissions ahead of time and save the
									// traversal in the event it's not allowed.
									destClient.mkdirs(destUri.getPath());

									for (RemoteFileInfo childFileItem : remoteFileInfoList) {
										// allow for interrupts to come out of band
										try {
											if (!isTaskInterrupted(bodyTask)) {
												break;
											}
										} catch (InterruptableTransferTaskException e) {
											e.printStackTrace();
											break;
										}

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
											_doPublishEvent(MessageType.TRANSFERTASK_CREATED, transferTask.toJson());

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

//                                        // create the remote directory to ensure it's present when the transfers begin. This
//                                        // also allows us to check for things like permissions ahead of time and save the
//                                        // traversal in the event it's not allowed.
//                                        boolean isDestCreated = destClient.mkdirs(destUri.getPath() + "/" + childFileItem.getName());

											TransferTask transferTask = new TransferTask(childSource, childDest, tenantId);
											transferTask.setTenantId(tenantId);
											transferTask.setOwner(username);
											transferTask.setParentTaskId(uuid);
											if (StringUtils.isNotEmpty(body.getString("rootTask"))) {
												transferTask.setRootTaskId(body.getString("rootTaskId"));
											}
											_doPublishEvent(MessageType.TRANSFERTASK_CREATED, transferTask.toJson());
										}
									}
								}
							}
						}
						// it's not an agave uri, so we forward on the raw uri as we know that we can
						// handle it from the wrapping if statement check
						else {
							_doPublishEvent("transfer." + srcUri.getScheme(), body);
						}
					} else {
						// tell everyone else that you killed this task
						// also set the status to CANCELLED
						Promise<Boolean> promise = Promise.promise();
						bodyTask.setStatus(TransferStatusType.CANCELLED);

						getDbService().update(tenantId, uuid, bodyTask, updateBody -> {
							if (updateBody.succeeded()) {
								logger.info("[{}] Transfer task {} updated.", tenantId, uuid);
								promise.handle(Future.succeededFuture(Boolean.TRUE));
							} else {
								logger.error("[{}] Task {} retry failed",
										tenantId, uuid);
								JsonObject json = new JsonObject()
										.put("cause", updateBody.cause().getClass().getName())
										.put("message", updateBody.cause().getMessage())
										.mergeIn(body);
								_doPublishEvent(MessageType.TRANSFERTASK_ERROR, json);
								promise.handle(Future.failedFuture(updateBody.cause()));
							}
						});

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

			_doPublishEvent(MessageType.TRANSFERTASK_ERROR, json);
		} catch (InterruptableTransferTaskException e) {
			e.printStackTrace();
		} finally {
			// any interrupt involving this task will be processeda t this point, so acknowledge
			// the task has been processed
			try {
				if (isTaskInterrupted(bodyTask)) {
					_doPublishEvent(MessageType.TRANSFERTASK_CANCELED_ACK, body);
				}
			} catch (InterruptableTransferTaskException e) {
				e.printStackTrace();
			}
		}
	}



//	/**
//	 * Checks whether the transfer task or any of its children exist in the list of
//	 * interrupted tasks.
//	 *
//	 * @param transferTask the current task being checked from the running task
//	 * @return true if the transfertask's uuid, parentTaskId, or rootTaskId are in the {@link #isTaskInterrupted(TransferTask)} list
//	 */


	public TransferTaskDatabaseService getDbService() {
		return dbService;
	}

	public void setDbService(TransferTaskDatabaseService dbService) {
		this.dbService = dbService;
	}


}