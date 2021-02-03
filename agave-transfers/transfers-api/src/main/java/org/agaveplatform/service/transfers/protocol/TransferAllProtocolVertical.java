package org.agaveplatform.service.transfers.protocol;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseService;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.agaveplatform.service.transfers.listener.AbstractTransferTaskListener;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.common.exceptions.AgaveNamespaceException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferTaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import static org.agaveplatform.service.transfers.TransferTaskConfigProperties.CONFIG_TRANSFERTASK_DB_QUEUE;
import static org.agaveplatform.service.transfers.enumerations.MessageType.TRANSFERTASK_CANCELED_ACK;

public class TransferAllProtocolVertical extends AbstractTransferTaskListener {
	private static final Logger log = LoggerFactory.getLogger(TransferAllProtocolVertical.class);
	protected static final String EVENT_CHANNEL = MessageType.TRANSFER_ALL;
	private TransferTaskDatabaseService dbService;

	public TransferAllProtocolVertical() {
		super();
	}
	public TransferAllProtocolVertical(Vertx vertx) {
		super(vertx);
	}
	public TransferAllProtocolVertical(Vertx vertx, String eventChannel) {
		super(vertx, eventChannel);
	}

	public String getDefaultEventChannel() {
		return EVENT_CHANNEL;
	}

	@Override
	public void start() {
		EventBus bus = vertx.eventBus();
		log.debug("Got into TransferAllProtocolVertical");

		// init our db connection from the pool
		String dbServiceQueue = config().getString(CONFIG_TRANSFERTASK_DB_QUEUE);
		dbService = TransferTaskDatabaseService.createProxy(vertx, dbServiceQueue);

		bus.<JsonObject>consumer(getEventChannel(), msg -> {
            msg.reply(TransferAllProtocolVertical.class.getName() + " received.");

            JsonObject body = msg.body();
			String uuid = body.getString("uuid");
			String source = body.getString("source");
			String dest = body.getString("dest");

			log.info("Transfer task (ALL) {} transferring: {} -> {}", uuid, source, dest);
			processEvent(body, resp -> {
				if (resp.succeeded()) {
					log.debug("Completed processing (ALL) {} event for transfer task (TA) {}", getEventChannel(), uuid);
				} else {
					log.error("Unable to process (ALL) {} event for transfer task (TA) message: {}", getEventChannel(), body.encode(), resp.cause());
				}
			});
		});

		// cancel tasks
		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_SYNC, msg -> {
            msg.reply(TransferAllProtocolVertical.class.getName() + " received.");

            JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			log.info("Transfer task {} cancel detected", uuid);
			if (uuid != null) {
				addCancelledTask(uuid);
			}
		});

        bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
            msg.reply(TransferAllProtocolVertical.class.getName() + " received.");

            JsonObject body = msg.body();
            String uuid = body.getString("uuid");

			log.info("Transfer task {} cancel completion detected. Updating internal cache.", uuid);
			if (uuid != null) {
				removeCancelledTask(uuid);
			}
		});

        // paused tasks
        bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_SYNC, msg -> {
            msg.reply(TransferAllProtocolVertical.class.getName() + " received.");

            JsonObject body = msg.body();
            String uuid = body.getString("uuid");

			log.info("Transfer task {} paused detected", uuid);
			if (uuid != null) {
				addPausedTask(uuid);
			}
		});

        bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
            msg.reply(TransferAllProtocolVertical.class.getName() + " received.");

            JsonObject body = msg.body();
            String uuid = body.getString("uuid");

			log.info("Transfer task {} paused completion detected. Updating internal cache.", uuid);
			if (uuid != null) {
				addPausedTask(uuid);
			}
		});
	}

	/**
	 * Handles processing of the actual transfer operation using the {@link URLCopy} class to manage the transfer.
	 * A promise is returned wiht the result of the operation. Note that this use of {@link URLCopy} will not create
	 * and update legacy {@link TransferTaskImpl} records as it goes.
	 * @param body the transfer all event body
	 * @param handler the callback receiving the result of the event processing
	 */
	public void processEvent(JsonObject body, Handler<AsyncResult<Boolean>> handler) {
		log.debug("Got into TransferAllProtocolVertical.processEvent");

		TransferTask tt = new TransferTask(body);
		String source = tt.getSource();
		String dest = tt.getDest();
		Boolean result = true;
		URI srcUri;
		URI destUri;
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;

		try {
			srcUri = URI.create(source);
			destUri = URI.create(dest);

			// stat the remote path to check its type
			//RemoteFileInfo fileInfo = srcClient.getFileInfo(srcUri.getPath());

			// migrate from the current transfertask format passed in as serialized json
			// to the legacy transfer task format managed by hibernate. Different db,
			// different packages, this won't work for real, but it will allow us to
			// smoke test this method with real object. We'll port the url copy class
			// over in the coming week to handle current transfertask objects so we
			// don't need this shim
			org.iplantc.service.transfer.model.TransferTask legacyTransferTask;
			boolean makeRealCopy = true;
			if (makeRealCopy) {

				// pull the system out of the url. system id is the hostname in an agave uri
				log.debug("Creating source remote data client to {} for transfer task {}", srcUri.getHost(), tt.getUuid());
				if (makeRealCopy) srcClient = getRemoteDataClient(tt.getTenantId(), tt.getOwner(), srcUri);

				log.debug("Creating dest remote data client to {} for transfer task {}", destUri.getHost(), tt.getUuid());
				// pull the dest system out of the url. system id is the hostname in an agave uri
				if (makeRealCopy) destClient = getRemoteDataClient(tt.getTenantId(), tt.getOwner(), destUri);

                WorkerExecutor executor = getVertx().createSharedWorkerExecutor("check-cancel-child-all-task-worker-pool");
                RemoteDataClient finalSrcClient = srcClient;
                RemoteDataClient finalDestClient = destClient;

                executor.executeBlocking(promise -> {
                        getDbService().getById(tt.getTenantId(), tt.getRootTaskId(), checkCancelled -> {
                            if (checkCancelled.succeeded()){
                                TransferTask targetTransferTask = new TransferTask(checkCancelled.result());
                                if (targetTransferTask.getStatus().isActive()){
                                    TransferTask resultingTransferTask = new TransferTask();
                                    try {
                                        log.info("Initiating worker transfer of {} to {} for transfer task {}", source, dest, tt.getUuid());

                                        resultingTransferTask = processCopyRequest(finalSrcClient, finalDestClient, tt);
                                        handler.handle(Future.succeededFuture(result));
                                        promise.complete();
                                    } catch (Exception e) {
										log.error("Failed to copy Transfer Task {}", tt.toJSON() );
										_doPublishEvent(MessageType.TRANSFERTASK_ERROR, tt.toJson());
										handler.handle(Future.failedFuture(e.getMessage()));
										promise.fail(e.getMessage());
									}
                                } else {
                                    log.info("Worker Transfer task {} was interrupted", tt.getUuid());
                                    _doPublishEvent(TRANSFERTASK_CANCELED_ACK, tt.toJson());
                                    handler.handle(Future.succeededFuture(false));
                                    promise.complete();
                                }
                            } else {
								log.error("Failed to get status of parent Transfer Task {}, {}", tt.getParentTaskId(), tt.toJSON());
								_doPublishEvent(MessageType.TRANSFERTASK_ERROR, tt.toJson());
                                handler.handle(Future.failedFuture(checkCancelled.cause()));
                                promise.fail("Failed to retrieve status....");
                            }
                        });
                    }, res -> {
					});

			} else {
				log.debug("Initiating fake transfer of {} to {} for transfer task {}", source, dest, tt.getUuid());
				log.debug("Completed fake transfer of {} to {} for transfer task {} with status {}", source, dest, tt.getUuid(), result);

//				_doPublishEvent(MessageType.TRANSFER_COMPLETED, body);
				handler.handle(Future.succeededFuture(true));
			}
		} catch (RemoteDataException e){
			log.error("RemoteDataException occured for TransferAllVerticle {}: {}", body.getString("uuid"), e.getMessage());
			JsonObject json = new JsonObject()
					.put("cause", e.getClass().getName())
					.put("message", e.getMessage())
					.mergeIn(body);
			_doPublishEvent(MessageType.TRANSFERTASK_ERROR, json);
			handler.handle(Future.failedFuture(e));
		} catch (RemoteCredentialException e){
			log.error("RemoteCredentialException occured for TransferAllVerticle {}: {}", body.getString("uuid"), e.getMessage());
			JsonObject json = new JsonObject()
					.put("cause", e.getClass().getName())
					.put("message", e.getMessage())
					.mergeIn(body);
			_doPublishEvent(MessageType.TRANSFERTASK_ERROR, json);
			handler.handle(Future.failedFuture(e));
		} catch (IOException e){
			log.error("IOException occured for TransferAllVerticle {}: {}", body.getString("uuid"), e.getMessage());
			JsonObject json = new JsonObject()
					.put("cause", e.getClass().getName())
					.put("message", e.getMessage())
					.mergeIn(body);
			_doPublishEvent(MessageType.TRANSFERTASK_ERROR, json);
			handler.handle(Future.failedFuture(e));
		} catch (Exception e){
			log.error("Unexpected Exception occured for TransferAllVerticle {}: {}", body.getString("uuid"), e.getMessage());
			JsonObject json = new JsonObject()
					.put("cause", e.getClass().getName())
					.put("message", e.getMessage())
					.mergeIn(body);

			_doPublishEvent(MessageType.TRANSFERTASK_ERROR, json);
			handler.handle(Future.failedFuture(e));
		}
	}

	protected TransferTask processCopyRequest(RemoteDataClient srcClient, RemoteDataClient destClient, TransferTask transferTask)
			throws TransferException, RemoteDataSyntaxException, RemoteDataException, IOException {
		log.debug("Got into TransferAllProtocolVertical.processCopyRequest ");

		log.trace("Got up to the urlCopy");

		// TODO: pass in a {@link RemoteTransferListener} after porting this class over so the listener can check for
		//   interrupts in this method upon updates from the transfer thread and interrupt it. Alternatively, we can
		//   just run the transfer in an observable and interrupt it via a timer task started by vertx.
		URLCopy urlCopy = getUrlCopy(srcClient, destClient);

		log.debug("Calling urlCopy.copy");
		TransferTask updatedTransferTask = null;

		WorkerExecutor executor = getVertx().createSharedWorkerExecutor("child-all-task-worker-pool");
		executor.executeBlocking(promise -> {
			TransferTask finishedTask = null;
			try {
				finishedTask = urlCopy.copy(transferTask);
				_doPublishEvent(MessageType.TRANSFER_COMPLETED, finishedTask.toJson());
				promise.complete();
				log.info("Completed copy of {} to {} for transfer task {} with status {}", finishedTask.getSource(),
						finishedTask.getDest(), finishedTask.getUuid(), finishedTask);
			} catch (Exception e) {
				log.error("Failed to copy Transfer Task {}, {}", transferTask.getUuid(), transferTask.toJSON() );
				_doPublishEvent(MessageType.TRANSFERTASK_ERROR, transferTask.toJson());
				promise.fail(e.getMessage());
			}
		}, res -> {
		});

		return updatedTransferTask;
	}

	protected URLCopy getUrlCopy(RemoteDataClient srcClient, RemoteDataClient destClient){
		return new URLCopy(srcClient, destClient, getVertx(), getRetryRequestManager());
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

	public TransferTaskDatabaseService getDbService() {
		return dbService;
	}

	public void setDbService(TransferTaskDatabaseService dbService) {
		this.dbService = dbService;
	}

}
