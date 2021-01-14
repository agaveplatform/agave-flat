package org.agaveplatform.service.transfers.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.notification.queue.messaging.NotificationMessageBody;
import org.iplantc.service.notification.queue.messaging.NotificationMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.agaveplatform.service.transfers.enumerations.MessageType.TRANSFERTASK_NOTIFICATION;


public class TransferTaskNotificationListener extends AbstractTransferTaskListener {
	private static final Logger logger = LoggerFactory.getLogger(TransferTaskNotificationListener.class);
	protected static final String EVENT_CHANNEL = TRANSFERTASK_NOTIFICATION ;

	protected String eventChannel;

	public TransferTaskNotificationListener() { super(); }
	public TransferTaskNotificationListener(Vertx vertx) {
		super(vertx);
	}
	public TransferTaskNotificationListener(Vertx vertx, String eventChannel) {
		super(vertx, eventChannel);
	}

	public String getDefaultEventChannel() {
		return EVENT_CHANNEL;
	}

	@Override
	public void start() {
		EventBus bus = vertx.eventBus();

		// poc listener to show propagated notifications that woudl be sent to users
		bus.<JsonObject>consumer(getEventChannel(), msg -> {
			msg.reply(TransferTaskNotificationListener.class.getName() + " received.");

			JsonObject body = msg.body();
			String uuid = body.getString("uuid");

			NotificationMessageContext messageBodyContext = new NotificationMessageContext(
					MessageType.TRANSFERTASK_CANCELED_COMPLETED, body.encode(), uuid);

			NotificationMessageBody messageBody = new NotificationMessageBody(
					uuid, body.getString("owner"), body.getString("tenantId"),
					messageBodyContext);

			if (body.getString("event") == null)
				body.put("event", body.getString("status"));
			if (body.getString("type") == null) {
				try {
					AgaveUUID agaveUuid = new AgaveUUID(uuid);
					body.put("type", agaveUuid.getResourceType());
				} catch (UUIDException e) {
					logger.info("Invalid uuid {}. {}", uuid, e.getMessage());
				}
			}

//			logger.info("{} notification event raised for {} {}: {}",
//					body.getString("event"),
//					body.getString("type"),
//					body.getString("uuid"),
//					body.encodePrettily());

			// we publish all notifications to the same channel for consumers to subscribe to. Let them
			// get the event type from the body of the message rather than the channel to reduce the
			// complexity of their client apps and the documentation of our notification semantics.
//			_doPublishEvent(MessageType.TRANSFERTASK_NOTIFICATION, body);
			try {
				notificationEventProcess(new JsonObject(messageBody.toJSON()));
			} catch (JsonProcessingException e) {
				logger.error("Failed to serialize notification for transfer task {} to legacy message format. {}",
						body.getString("uuid"), e.getMessage());
			}
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
			msg.reply(TransferTaskNotificationListener.class.getName() + " received.");

			JsonObject body = msg.body();
			//JsonObject notificationMessage = new NotificationMessageBody();

			logger.info("Transfer task {} completed.", body.getString("uuid"));

			_doPublishEvent(MessageType.NOTIFICATION_CANCELLED, body);
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_FINISHED, msg -> {
			msg.reply(TransferTaskNotificationListener.class.getName() + " received.");

			JsonObject body = msg.body();
			getVertx().eventBus().publish(MessageType.NOTIFICATION, body);

			logger.info("Transfer task {} completed.", body.getString("uuid"));

			getVertx().eventBus().publish(MessageType.NOTIFICATION_COMPLETED, body);
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CREATED, msg -> {
			msg.reply(TransferTaskNotificationListener.class.getName() + " received.");

			JsonObject body = msg.body();

			logger.info("Transfer task {} created.", body.getString("uuid"));
		});


		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
			msg.reply(TransferTaskNotificationListener.class.getName() + " received.");

			JsonObject body = msg.body();

			logger.info("Transfer task {} created.", body.getString("uuid"));
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_ERROR, msg -> {
			msg.reply(TransferTaskNotificationListener.class.getName() + " received.");

			JsonObject body = msg.body();

			logger.info("Transfer task {} created.", body.getString("uuid"));
		});

		bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PARENT_ERROR, msg -> {
			msg.reply(TransferTaskNotificationListener.class.getName() + " received.");

			JsonObject body = msg.body();

			logger.info("Transfer task {} created.", body.getString("uuid"));
		});
	}

	/**
	 * Writes the notification tot he legacy notification queue. Returns if at least one message was written to the queue.
	 * @param body the message body to send
	 * @return true if a message was written
	 */
	protected boolean notificationEventProcess(JsonObject body) {
		logger.info("Sending legacy notification message for transfer task {}", body.getString("uuid"));
		return NotificationManager.process(body.getString("uuid"), body.encode(), body.getString("owner")) > 0;
	}

}
