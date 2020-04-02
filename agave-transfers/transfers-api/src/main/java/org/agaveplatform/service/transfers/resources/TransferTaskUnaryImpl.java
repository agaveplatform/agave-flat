package org.agaveplatform.service.transfers.resources;

import com.google.common.io.Files;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.enumerations.TransferStatusType;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.agaveplatform.service.transfers.protocol.TransferSftpVertical;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.URLCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;

public class TransferTaskUnaryImpl extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(TransferSftpVertical.class);
	private String eventChannel = "transfertask.process.unary";

	public TransferTaskUnaryImpl(Vertx vertx) {
		this(vertx, null);
	}

	public TransferTaskUnaryImpl(Vertx vertx, String eventChannel) {
		super();
		setVertx(vertx);
		setEventChannel(eventChannel);
	}

	@Override
	public void start() {
		EventBus bus = vertx.eventBus();

		bus.<JsonObject>consumer(getEventChannel(), msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");
			String source = body.getString("source");
			String dest = body.getString("dest");
			logger.info("Transfer task {} created: {} -> {}", uuid, source, dest);
			this.processFileTask(body);
		});

		// paused tasks
		bus.<JsonObject>consumer("transfertask.paused.sync", msg -> {
			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			logger.info("Transfer task {} paused detected", uuid);

			vertx.eventBus().publish("transfertask.paused.complete", body);
		});

	}
	protected void processFileTask(JsonObject body){
		String uuid = body.getString("id");
		String source = body.getString("source");
		String dest =  body.getString("dest");
		String username = body.getString("owner");
		String tenantId = body.getString("tenantId");
		String protocol	= "";
		TransferTask bodyTask = new TransferTask(body);

		try {
//			URI srcUri = new URI(source);
//			URI destUri = new URI(dest);
////			if (srcUri.getScheme().equalsIgnoreCase("agave")) {
////				RemoteSystem srcSystem = new SystemDao().findBySystemId(srcUri.getHost());
////				protocol = srcSystem.getStorageConfig().getProtocol().toString();
////			} else {
////				protocol = srcUri.getScheme();
////			}
//
//			RemoteDataClient srcRDC = new RemoteDataClientFactory().getInstance(username, null, srcUri);
//			RemoteDataClient destRDC = new RemoteDataClientFactory().getInstance(username, null, destUri);
//
//			URLCopy urlCopy = new URLCopy(srcRDC, destRDC);
//			TransferTask tt = urlCopy.copy(srcUri.getPath(), destUri.getPath(), bodyTask);

			bodyTask.setStatus(TransferStatusType.COMPLETED);
			bodyTask.setStartTime(Instant.now());
			bodyTask.setEndTime(Instant.now());
			bodyTask.setAttempts(1);
			bodyTask.setBytesTransferred(99);
			bodyTask.setTotalSize(99);
			bodyTask.setTotalFiles(1);
			bodyTask.setTotalSkippedFiles(0);
			bodyTask.setTransferRate(Long.MAX_VALUE);

			vertx.eventBus().publish("transfertask.completed", bodyTask.toJson());

		} catch (Exception e) {
			logger.error(e.getMessage());
			JsonObject json = new JsonObject()
					.put("cause", e.getClass().getName())
					.put("message", e.getMessage())
					.mergeIn(body);

			vertx.eventBus().publish("transfertask.error", json);
		}
	}
	/**
	 * Sets the vertx instance for this listener
	 *
	 * @param vertx the current instance of vertx
	 */
	private void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	/**
	 * @return the message type to listen to
	 */
	public String getEventChannel() {
		return eventChannel;
	}

	/**
	 * Sets the message type for which to listen
	 *
	 * @param eventChannel
	 */
	public void setEventChannel(String eventChannel) {
		this.eventChannel = eventChannel;
	}


}
