package org.agaveplatform.service.transfers.listener;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Options;
import io.nats.client.Subscription;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseService;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.agaveplatform.service.transfers.exception.MaxTransferTaskAttemptsExceededException;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.exceptions.AgaveNamespaceException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.agaveplatform.service.transfers.TransferTaskConfigProperties.CONFIG_TRANSFERTASK_DB_QUEUE;
import static org.agaveplatform.service.transfers.enumerations.MessageType.*;
import static org.agaveplatform.service.transfers.enumerations.TransferStatusType.*;

public class TransferTaskRetryListener extends AbstractNatsListener {
	private static final Logger log = LoggerFactory.getLogger(TransferTaskRetryListener.class);
	protected static final String EVENT_CHANNEL = MessageType.TRANSFER_RETRY;

	private TransferTaskDatabaseService dbService;
	public Connection nc;

	public TransferTaskRetryListener() throws IOException, InterruptedException {
		super();
		setConnection();
	}

	public TransferTaskRetryListener(Vertx vertx) throws IOException, InterruptedException {
		super(vertx);
		setConnection();
	}

	public TransferTaskRetryListener(Vertx vertx, String eventChannel) throws IOException, InterruptedException {
		super(vertx, eventChannel);
		setConnection();
	}

	public String getDefaultEventChannel() {
		return EVENT_CHANNEL;
	}

	public Connection getConnection(){return nc;}

	public void setConnection() throws IOException, InterruptedException {
		try {
			nc = _connect(CONNECTION_URL);
		} catch (IOException e) {
			//use default URL
			nc = _connect(Options.DEFAULT_URL);
		}
	}

	@Override
	public void start() throws TimeoutException, InterruptedException, IOException {
		// init our db connection from the pool
		String dbServiceQueue = config().getString(CONFIG_TRANSFERTASK_DB_QUEUE);
		dbService = TransferTaskDatabaseService.createProxy(vertx, dbServiceQueue);


		//EventBus bus = vertx.eventBus();
		//bus.<JsonObject>consumer(getEventChannel(), msg -> {
		//Connection nc = _connect();
		Dispatcher d = getConnection().createDispatcher((msg) -> {});
		//bus.<JsonObject>consumer(getEventChannel(), msg -> {
		Subscription s = d.subscribe(getDefaultEventChannel(), msg -> {
			//msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
			String response = new String(msg.getData(), StandardCharsets.UTF_8);
			JsonObject body = new JsonObject(response) ;
			String uuid = body.getString("uuid");
			String source = body.getString("source");
			String dest = body.getString("dest");
//			msg.reply(TransferTaskRetryListener.class.getName() + " received.");

			log.info("Transfer task {} retry: {} -> {}", uuid, source, dest);

			try {
				processRetryTransferTask(body, resp -> {
					if (resp.succeeded()) {
						log.debug("Completed processing {} event for transfer task {}", getEventChannel(), body.getString("uuid"));
						// TODO: retry won't be reflected in the message body, so what are we listening to here? At
						//   this point
						body.put("event", this.getClass().getName());
						body.put("type", getEventChannel());
						try {
							_doPublishNatsJSEvent( TRANSFERTASK_NOTIFICATION, body);
						} catch (Exception e) {
							log.debug(e.getMessage());
						}
					} else {
						log.debug("Unable to process {} event for transfer task (TTRL) message: {}", getEventChannel(), body.encode(), resp.cause());
						try {
							_doPublishNatsJSEvent( MessageType.TRANSFERTASK_ERROR, body);
						} catch (Exception e) {
							log.debug(e.getMessage());
						}
					}
				});
			} catch (Exception ex) {
				log.debug("Error with the TRANSFER_RETRY message.  The error is {}", ex.getMessage());
				try {
					_doPublishNatsJSEvent(MessageType.TRANSFERTASK_ERROR, body);
				} catch (Exception e) {
					log.debug(e.getMessage());
				}
			}
		});
		d.subscribe(getDefaultEventChannel());
		getConnection().flush(Duration.ofMillis(500));


		// cancel tasks
		//bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_SYNC, msg -> {
		s = d.subscribe(MessageType.TRANSFERTASK_CANCELED_SYNC, msg -> {
			//msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
			String response = new String(msg.getData(), StandardCharsets.UTF_8);
			JsonObject body = new JsonObject(response) ;
			String uuid = body.getString("uuid");
			String source = body.getString("source");
			String dest = body.getString("dest");
			//msg.reply(TransferTaskRetryListener.class.getName() + " received.");

			log.info("Transfer task {} cancel detected", uuid);
			if (uuid != null) {
				addCancelledTask(uuid);
			}
		});
		d.subscribe(MessageType.TRANSFERTASK_CANCELED_SYNC);
		getConnection().flush(Duration.ofMillis(500));

		//bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
		s = d.subscribe(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
			//msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
			String response = new String(msg.getData(), StandardCharsets.UTF_8);
			JsonObject body = new JsonObject(response) ;
			String uuid = body.getString("uuid");
			//msg.reply(TransferTaskRetryListener.class.getName() + " received.");

			log.info("Transfer task {} cancel completion detected. Updating internal cache.", uuid);
			if (uuid != null) {
				removeCancelledTask(uuid);
			}
		});
		d.subscribe(MessageType.TRANSFERTASK_CANCELED_COMPLETED);
		getConnection().flush(Duration.ofMillis(500));

		// paused tasks
		//bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_SYNC, msg -> {
		s = d.subscribe(MessageType.TRANSFERTASK_PAUSED_SYNC, msg -> {
			//msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
			String response = new String(msg.getData(), StandardCharsets.UTF_8);
			JsonObject body = new JsonObject(response) ;
			String uuid = body.getString("uuid");
//			msg.reply(TransferTaskRetryListener.class.getName() + " received.");

			log.info("Transfer task {} paused detected", uuid);
			if (uuid != null) {
				addPausedTask(uuid);
			}
		});
		d.subscribe(MessageType.TRANSFERTASK_PAUSED_SYNC);
		getConnection().flush(Duration.ofMillis(500));


		//bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
		s = d.subscribe(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
			//msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
			String response = new String(msg.getData(), StandardCharsets.UTF_8);
			JsonObject body = new JsonObject(response) ;
			String uuid = body.getString("uuid");
//			msg.reply(TransferTaskRetryListener.class.getName() + " received.");
			log.info("Transfer task {} paused completion detected. Updating internal cache.", uuid);
			if (uuid != null) {
				addPausedTask(uuid);
			}
		});
		d.subscribe(MessageType.TRANSFERTASK_PAUSED_COMPLETED);
		getConnection().flush(Duration.ofMillis(500));


	}

	/**
	 * Handles the event body processing. This will lookup the task record, and if in an active state, attempt to retry
	 * task assignment after incrementing the attempt count.
	 *
	 * @param body the retry message body.
	 */
	protected void processRetryTransferTask(JsonObject body, Handler<AsyncResult<Boolean>> handler) {
		try {
			String uuid = body.getString("uuid");
			String tenantId = body.getString("tenant_id");
			Integer attempts = body.getInteger("attempts");
			log.debug(body.encode());
			// check to see if the uuid is Canceled or Completed
			getDbService().getByUuid(tenantId, uuid, reply -> {
				if (reply.succeeded()) {

					TransferTask transferTaskToRetry = new TransferTask(reply.result());
					if (transferTaskToRetry.getStatus().isActive() || transferTaskToRetry.getStatus().equals(ERROR)) {
						// we're good to to go forward.

						// the status is not in the states above.  Now check to see if the # of attempts exceeds the max
						int configMaxTries = config().getInteger("TRANSFERTASK_MAX_ATTEMPTS");
						if (configMaxTries >= transferTaskToRetry.getAttempts()) {
							// # of retries is less.

							// increment the attempts
							transferTaskToRetry.setAttempts(attempts + 1);
							transferTaskToRetry.setStatus(RETRYING);
							getDbService().update(tenantId, uuid, transferTaskToRetry, updateBody -> {
								if (updateBody.succeeded()) {
									log.debug("Beginning attempt {} for transfer task {}", tenantId, uuid);
									processRetry(new TransferTask(updateBody.result()), handler);
								} else {
									log.error("[{}] Task {} update failed: {}",
											tenantId, uuid, reply.cause());
									JsonObject json = new JsonObject()
											.put("cause", updateBody.cause().getClass().getName())
											.put("message", updateBody.cause().getMessage())
											.mergeIn(body);
									try {
										_doPublishNatsJSEvent( MessageType.TRANSFERTASK_ERROR, json);
										handler.handle(Future.succeededFuture(false));
									} catch (Exception e) {
										log.debug(e.getMessage());
									}
								}
							});
						} else {
							String msg = "Maximum attempts have been exceeded for transfer task " + uuid + ". " +
									"No further retries will be attempted.";
							JsonObject json = new JsonObject()
									.put("cause", MaxTransferTaskAttemptsExceededException.class.getName())
									.put("message", msg)
									.mergeIn(body);
							try {
								_doPublishNatsJSEvent(TRANSFER_FAILED, json);
								handler.handle(Future.succeededFuture(false));
							} catch (Exception e) {
								log.debug(e.getMessage());
							}
						}
					} else {
						log.debug("Skipping retry of transfer task {}. Task has a status of {} and is no longer in an active state.",
								uuid, transferTaskToRetry.getStatus().name());
						handler.handle(Future.succeededFuture(true));
					}
				} else {
					String msg = "Unable to verify the current status of transfer task " + uuid + ". " + reply.cause();
					JsonObject json = new JsonObject()
							.put("cause", reply.cause().getClass().getName())
							.put("message", msg)
							.mergeIn(body);
					try {
						_doPublishNatsJSEvent(TRANSFERTASK_ERROR, json);
						handler.handle(Future.succeededFuture(false));
					} catch (Exception e) {
						log.debug(e.getMessage());
					}
				}

			});
//			handler.handle(Future.succeededFuture(true));
		} catch (Throwable t) {

			// fail the processing if there is any kind of issue
			JsonObject json = new JsonObject()
					.put("cause", t.getClass().getName())
					.put("message", t.getMessage())
					.mergeIn(body);
			try {
				_doPublishNatsJSEvent(TRANSFERTASK_ERROR, json);
				handler.handle(Future.failedFuture(t));
			} catch (Exception e) {
				log.debug(e.getMessage());
			}
		}
	}

	/**
	 * Handles the execution of the retry behavior. This is essentially the same code as
	 * {@link TransferTaskAssignedListener#processTransferTask(JsonObject, Handler)},
	 * {@link TransferTaskAssignedListener#processTransferTask(JsonObject, Handler)} method
	 *
	 * @param retryTransferTask the updated transfer task to retry
	 * @param handler the callback to the calling method with the boolean async result of the processing task
	 */
	public void processRetry(TransferTask retryTransferTask, Handler<AsyncResult<Boolean>> handler) {
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;

		try {
			URI srcUri;
			URI destUri;
			try {
				srcUri = URI.create(retryTransferTask.getSource());
				destUri = URI.create(retryTransferTask.getDest());
			} catch (Throwable e) {
				String msg = String.format("Unable to parse source uri %s for transfertask %s: %s",
						retryTransferTask.getSource(), retryTransferTask.getUuid(), e.getMessage());
				throw new RemoteDataSyntaxException(msg, e);
			}

			// check to be sure if the root task and parent task are present
			if (retryTransferTask.getRootTaskId() != null && retryTransferTask.getParentTaskId() != null) {


				// check for task interruption
				if (taskIsNotInterrupted(retryTransferTask)) {

					// basic sanity check on uri again
					if (uriSchemeIsNotSupported(srcUri)) {
						String msg = String.format("Failing transfer task %s due to invalid scheme in source URI, %s",
								retryTransferTask.getUuid(), retryTransferTask.getSource());
						throw new RemoteDataSyntaxException(msg);
					} else if (uriSchemeIsNotSupported(destUri)) {
						String msg = String.format("Failing transfer task %s due to invalid scheme in destination URI, %s",
								retryTransferTask.getUuid(), retryTransferTask.getDest());
						throw new RemoteDataSyntaxException(msg);
					} else {
						// if it's an "agave://" uri, look up the connection info, get a rdc, and process the remote
						// file item
//					if (srcUri.getScheme().equalsIgnoreCase("agave")) {
						// get a remote data client for the source and dest system
						srcClient = getRemoteDataClient(retryTransferTask.getTenantId(), retryTransferTask.getOwner(), srcUri);
//					}
						// should we check writability here?
						destClient = getRemoteDataClient(retryTransferTask.getTenantId(), retryTransferTask.getOwner(), destUri);

						// stat the remote path to check its type
						RemoteFileInfo fileInfo = srcClient.getFileInfo(srcUri.getPath());

						// if the path is a file, then we can move it directly, so we raise an event telling the protocol
						// listener to move the file item
						if (fileInfo.isFile()) {
							// write to the protocol event channel. the uri is all they should need for this....
							// might need tenant id. not sure yet.
							_doPublishNatsJSEvent( TRANSFER_ALL, retryTransferTask.toJson());
						} else {
							// path is a directory, so walk the first level of the directory
							List<RemoteFileInfo> remoteFileInfoList = srcClient.ls(srcUri.getPath());

							// if the directory is empty, the listing will only contain the target path as the "." folder.
							// mark as complete and wrap it up.
							if (remoteFileInfoList.size() <= 1) {
								_doPublishNatsJSEvent(TRANSFER_COMPLETED, retryTransferTask.toJson());
							}
							// if there are contents, walk the first level, creating directories on the remote side
							// as we go to ensure that out of order processing by worker tasks can still succeed.
							else {
								// create the remote directory to ensure it's present when the transfers begin. This
								// also allows us to check for things like permissions ahead of time and save the
								// traversal in the event it's not allowed. While this may have been created in the
								// original attempt, it may also have been deleted, so we ensure it's present
								// on every retry.
								destClient.mkdirs(destUri.getPath());

								for (RemoteFileInfo childFileItem : remoteFileInfoList) {
									// if the assigned or ancestor transfer task were cancelled while this was running,
									// skip the rest.
									if (taskIsNotInterrupted(retryTransferTask)) {
										// if it's a file, we can process this as we would if the original path were a file
										if (childFileItem.isFile()) {
											// build the child paths
											String childSource = retryTransferTask.getSource() + "/" + childFileItem.getName();
											String childDest = retryTransferTask.getDest() + "/" + childFileItem.getName();

											TransferTask transferTask = new TransferTask(childSource, childDest, retryTransferTask.getTenantId());
											transferTask.setTenantId(retryTransferTask.getTenantId());
											transferTask.setOwner(retryTransferTask.getOwner());
											transferTask.setParentTaskId(retryTransferTask.getUuid());
											if (StringUtils.isNotEmpty(retryTransferTask.getRootTaskId())) {
												transferTask.setRootTaskId(retryTransferTask.getRootTaskId());
											}
											_doPublishNatsJSEvent( MessageType.TRANSFERTASK_CREATED, transferTask.toJson());
										}
										// if a directory, then create a new transfer task to repeat this process,
										// keep the association between this transfer task, the original, and the children
										// in place for traversal in queries later on.
										else {
											// build the child paths
											String childSource = retryTransferTask.getSource() + "/" + childFileItem.getName();
											String childDest = retryTransferTask.getDest() + "/" + childFileItem.getName();

											TransferTask transferTask = new TransferTask(childSource, childDest, retryTransferTask.getTenantId());
											transferTask.setTenantId(retryTransferTask.getTenantId());
											transferTask.setOwner(retryTransferTask.getOwner());
											transferTask.setParentTaskId(retryTransferTask.getUuid());
											if (StringUtils.isNotEmpty(retryTransferTask.getRootTaskId())) {
												transferTask.setRootTaskId(retryTransferTask.getRootTaskId());
											}
											_doPublishNatsJSEvent(MessageType.TRANSFERTASK_CREATED, transferTask.toJson());
										}
									} else {
										// interrupt happened wild processing children. skip the rest.
										log.info("Skipping processing of child file items for transfer tasks {} due to interrupt event.", retryTransferTask.getUuid());
										getDbService().updateStatus(retryTransferTask.getTenantId(), retryTransferTask.getUuid(), org.iplantc.service.transfer.model.enumerations.TransferStatusType.CANCELLED.name(), updateReply -> {
											if (updateReply.succeeded()) {
												handler.handle(Future.succeededFuture(false));
												try {
													_doPublishNatsJSEvent( MessageType.TRANSFERTASK_CANCELED_ACK, retryTransferTask.toJson());
												} catch (Exception e) {
													log.debug(e.getMessage());
												}
											} else {
												try {
													_doPublishNatsJSEvent( MessageType.TRANSFERTASK_CANCELED_ACK, retryTransferTask.toJson());
													handler.handle(Future.failedFuture(updateReply.cause()));
												} catch (Exception e) {
													log.debug(e.getMessage());
												}
											}
										});

										break;
									}
								}
							}
						}
					}

					handler.handle(Future.succeededFuture(true));
				} else {
					// task was interrupted, so don't attempt a retry
					log.info("Skipping processing of child file items for transfer tasks {} due to interrupt event.", retryTransferTask.getUuid());
					_doPublishNatsJSEvent(MessageType.TRANSFERTASK_CANCELED_ACK, retryTransferTask.toJson());
					handler.handle(Future.succeededFuture(false));
				}
			} else {
				log.info("rootTaskId or parentTaskId are null {}  {}", retryTransferTask.getParentTaskId(), retryTransferTask.getRootTaskId());
			}
		} catch (RemoteDataSyntaxException ex) {
			String message = String.format("Failing transfer task %s due to invalid source syntax. %s", retryTransferTask.getTenantId(), ex.getMessage());
			try {
				doHandleFailure(ex, message, retryTransferTask.toJson(), handler);
			} catch (IOException | InterruptedException e) {
				log.debug(e.getMessage());
			}
		} catch (Exception ex) {
			try {
				doHandleError(ex, ex.getMessage(), retryTransferTask.toJson(), handler);
			} catch (IOException | InterruptedException e) {
				log.debug(e.getMessage());
			}
		} finally {
			// cleanup the remote data client connections
			try { if (srcClient != null) srcClient.disconnect(); } catch (Exception ignored) {}
			try { if (destClient != null) destClient.disconnect(); } catch (Exception ignored) {}
		}

		handler.handle(Future.succeededFuture(true));
	}

	public TransferTaskDatabaseService getDbService() {
		return dbService;
	}

	public void setDbService(TransferTaskDatabaseService dbService) {
		this.dbService = dbService;
	}

	/**
	 * Obtains a new {@link RemoteDataClient} for the given {@code uri}. The schema and hostname are used to identify
	 * agave {@link RemoteSystem} URI vs externally accesible URI. Tenancy is honored.
	 * @param tenantId the tenant whithin which any system lookups should be made
	 * @param username the user for whom the system looks should be made
	 * @param target the uri from which to parse the system info
	 * @return a new instance of a {@link RemoteDataClient} for the given {@code target}
	 * @throws SystemUnknownException if the sytem is unknown
	 * @throws AgaveNamespaceException if the URI does match any known agave uri pattern
	 * @throws RemoteCredentialException if the credentials for the system represented by the URI cannot be found/refreshed/obtained
	 * @throws PermissionException when the user does not have permission to access the {@code target}
	 * @throws FileNotFoundException when the remote {@code target} does not exist
	 * @throws RemoteDataException when a connection cannot be made to the {@link RemoteSystem}
	 * @throws NotImplementedException when the schema is not supported
	 */
	protected RemoteDataClient getRemoteDataClient(String tenantId, String username, URI target) throws NotImplementedException, SystemUnknownException, AgaveNamespaceException, RemoteCredentialException, PermissionException, FileNotFoundException, RemoteDataException {
		TenancyHelper.setCurrentTenantId(tenantId);
		return new RemoteDataClientFactory().getInstance(username, null, target);
	}
}
