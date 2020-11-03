package org.agaveplatform.service.transfers.protocol;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
import org.iplantc.service.transfer.RemoteTransferListener;
import org.agaveplatform.service.transfers.protocol.URLCopy;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import static org.agaveplatform.service.transfers.TransferTaskConfigProperties.CONFIG_TRANSFERTASK_DB_QUEUE;
import static org.agaveplatform.service.transfers.enumerations.MessageType.TRANSFERTASK_CANCELED_ACK;

public class TransferAllProtocolVertical extends AbstractTransferTaskListener {
	private final Logger log = LoggerFactory.getLogger(TransferAllProtocolVertical.class);
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
		log.info("Got into TransferAllProtocolVertical");

		// init our db connection from the pool
		String dbServiceQueue = config().getString(CONFIG_TRANSFERTASK_DB_QUEUE);
		dbService = TransferTaskDatabaseService.createProxy(vertx, dbServiceQueue);

		bus.<JsonObject>consumer(getEventChannel(), msg -> {
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
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			log.info("Transfer task {} cancel detected", uuid);
			if (uuid != null) {
				addCancelledTask(uuid);
			}
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			log.info("Transfer task {} cancel completion detected. Updating internal cache.", uuid);
			if (uuid != null) {
				removeCancelledTask(uuid);
			}
		});

		// paused tasks
		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_SYNC, msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			log.info("Transfer task {} paused detected", uuid);
			if (uuid != null) {
				addPausedTask(uuid);
			}
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
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
	 * and update legacy {@link org.iplantc.service.transfer.model.TransferTask} records as it goes.
	 * @param body the transfer all event body
	 * @param handler the callback receiving the result of the event processing
	 */
	public void processEvent(JsonObject body, Handler<AsyncResult<Boolean>> handler) {
		log.info("Got into TransferAllProtocolVertical.processEvent");

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
			// don' tneed this shim
			org.iplantc.service.transfer.model.TransferTask legacyTransferTask;
			boolean makeRealCopy = true;
			if (makeRealCopy) {

				// pull the system out of the url. system id is the hostname in an agave uri
				log.debug("Creating source remote data client to {} for transfer task {}", srcUri.getHost(), tt.getUuid());
				if (makeRealCopy) srcClient = getRemoteDataClient(tt.getTenantId(), tt.getOwner(), srcUri);

				log.debug("Creating dest remote data client to {} for transfer task {}", destUri.getHost(), tt.getUuid());
				// pull the dest system out of the url. system id is the hostname in an agave uri
				if (makeRealCopy) destClient = getRemoteDataClient(tt.getTenantId(), tt.getOwner(), destUri);

				if (taskIsNotInterrupted(tt)) {
//					legacyTransferTask =
//							new org.iplantc.service.transfer.model.TransferTask(tt.getSource(), tt.getDest(), tt.getOwner(), null, null);
//
//					legacyTransferTask.setUuid(tt.getUuid());
//					legacyTransferTask.setTenantId(tt.getTenantId());
//					legacyTransferTask.setStatus(TransferStatusType.valueOf(tt.getStatus().name()));
//					legacyTransferTask.setAttempts(tt.getAttempts());
//					legacyTransferTask.setBytesTransferred(tt.getBytesTransferred());
//					legacyTransferTask.setTotalSize(tt.getTotalSize());
//					legacyTransferTask.setCreated(Date.from(tt.getCreated()));
//					legacyTransferTask.setLastUpdated(Date.from(tt.getLastUpdated()));
//					legacyTransferTask.setStartTime(Date.from(tt.getStartTime()));
//					legacyTransferTask.setEndTime(Date.from(tt.getEndTime()));
//					if (tt.getParentTaskId() != null) {
//						org.iplantc.service.transfer.model.TransferTask legacyParentTask = new org.iplantc.service.transfer.model.TransferTask();
//						legacyParentTask.setUuid(tt.getParentTaskId());
//						legacyTransferTask.setParentTask(legacyParentTask);
//					}
//					if (tt.getRootTaskId() != null) {
//						org.iplantc.service.transfer.model.TransferTask legacyRootTask = new org.iplantc.service.transfer.model.TransferTask();
//						legacyRootTask.setUuid(tt.getRootTaskId());
//						legacyTransferTask.setRootTask(legacyRootTask);
//					}

						log.info("Initiating transfer of {} to {} for transfer task {}", source, dest, tt.getUuid());
						//result = processCopyRequest(source, srcClient, dest, destClient, legacyTransferTask);
					TransferTask resultingTransferTask = new TransferTask();
					resultingTransferTask = processCopyRequest(srcClient, destClient, tt);

					handler.handle(Future.succeededFuture(result));
					log.info("Completed copy of {} to {} for transfer task {} with status {}", source, dest, tt.getUuid(), resultingTransferTask);
				} else {
					log.info("Transfer task {} was interrupted", tt.getUuid());
					getDbService().updateStatus(tt.getTenantId(), tt.getUuid(),TransferStatusType.CANCELLED.name(), updateReply -> {
						if (updateReply.succeeded()) {
							_doPublishEvent(TRANSFERTASK_CANCELED_ACK, tt.toJson());
							handler.handle(Future.succeededFuture(false));
						} else {
							handler.handle(Future.failedFuture(updateReply.cause()));
						}
					});

				}
			} else {
				log.debug("Initiating fake transfer of {} to {} for transfer task {}", source, dest, tt.getUuid());
				log.debug("Completed fake transfer of {} to {} for transfer task {} with status {}", source, dest, tt.getUuid(), result);

				_doPublishEvent(MessageType.TRANSFER_COMPLETED, body);
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
//		log.info("Got into TransferAllProtocolVertical.processCopyRequest ");

//		getDbService().updateStatus(legacyTransferTask.getTenantId(), legacyTransferTask.getUuid(), org.agaveplatform.service.transfers.enumerations.TransferStatusType.TRANSFERRING.toString(), updateReply -> {
//			if (updateReply.succeeded()) {
//				Future.succeededFuture(Boolean.TRUE);
//			} else {
//				// update failed
//				Future.succeededFuture(Boolean.FALSE);
//			}
//		});


		URI srcUri = URI.create(transferTask.getSource());
		URI destUri = URI.create(transferTask.getDest());

//		log.info("Get up to the urlCopy");
		URLCopy urlCopy = getUrlCopy(srcClient, destClient);
		// TODO: pass in a {@link RemoteTransferListener} after porting this class over so the listener can check for
		//   interrupts in this method upon updates from the transfer thread and interrupt it. Alternatively, we can
		//   just run the transfer in an observable and interrupt it via a timer task started by vertx.

		// note:  run the following as Blocking code.
		getVertx().executeBlocking(future -> {
			TransferTask tt = transferTask;
			try {
				tt = urlCopy.copy(transferTask, null);
				future.complete();
			} catch (TransferException e){
				log.error(e.toString());
				future.fail(e.getCause());
			} catch (RemoteDataException e){
				log.error(e.toString());
				future.fail(e.getCause());
			}catch (RemoteDataSyntaxException e){
				log.error(e.toString());
				future.fail(e.getCause());
			} catch (IOException e){
				log.error(e.toString());
				future.fail(e.getCause());
			}
		}, res -> {
			log.info("Blocking code worked properly.");
		});

		return transferTask;
	}

	protected URLCopy getUrlCopy(RemoteDataClient srcClient, RemoteDataClient destClient){
		return new URLCopy(srcClient,destClient);
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


//	protected TransferTask copy(String src, String dest, TransferTask transferTask) {
//		if (sourceClient.equals(destClient)) {
//			RemoteTransferListener listener = null;
//
//			// we can potentially make a server-side copy here. attempt that first
//			// before making an unnecessary round-trip
//			sourceClient.copy(srcPath + "/", destPath, listener);
//
//			// everything was copied over server side, so delete whatever was in the
//			// list of exclusions
//			for (String excludedOutputFile : exclusions) {
//				try {
//					destClient.delete(destPath + "/" + excludedOutputFile);
//				} catch (Exception ignored) {
//				}
//			}
//	}

}
