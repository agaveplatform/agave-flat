package org.agaveplatform.service.transfers.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nats.client.Connection;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.iplantc.service.common.messaging.Message;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.notification.queue.messaging.NotificationMessageBody;
import org.iplantc.service.notification.queue.messaging.NotificationMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.agaveplatform.service.transfers.enumerations.MessageType.*;

public class TransferTaskNotificationListener extends AbstractNatsListener {
	private static final Logger logger = LoggerFactory.getLogger(TransferTaskNotificationListener.class);
	protected static final String EVENT_CHANNEL = MessageType.TRANSFERTASK_NOTIFICATION ;

	protected String eventChannel;
    public Connection nc;

	public TransferTaskNotificationListener() throws IOException, InterruptedException {
	    super();
	}

	public TransferTaskNotificationListener(Vertx vertx) throws IOException, InterruptedException {
		super(vertx);
    }

	public TransferTaskNotificationListener(Vertx vertx, String eventChannel) throws IOException, InterruptedException {
		super(vertx, eventChannel);
    }

	public String getDefaultEventChannel() {
		return EVENT_CHANNEL;
	}

	@Override
	public void start() {

        List<String> notificationEvents = List.of(
                TRANSFERTASK_CREATED,
                TRANSFERTASK_UPDATED,
                TRANSFERTASK_FINISHED,
                TRANSFERTASK_FAILED,
                TRANSFERTASK_PAUSED_COMPLETED,
                TRANSFERTASK_CANCELED_COMPLETED);


        try {
            //group subscription so each message only processed by this vertical type once
            subscribeToSubjectGroup(EVENT_CHANNEL, this::handleMessage);
        } catch (Exception e) {
            logger.error("TRANSFER_ALL - Exception {}", e.getMessage());
        }

//		// poc listener to show propagated notifications that woudl be sent to users
//		//bus.<JsonObject>consumer(getEventChannel(), msg -> {
//        //Connection nc = _connect();
//        Dispatcher d = getConnection().createDispatcher((msg) -> {});
//        //bus.<JsonObject>consumer(getEventChannel(), msg -> {
//        Subscription s = d.subscribe(MessageType.TRANSFERTASK_NOTIFICATION, msg -> {
//            //msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
//            String response = new String(msg.getData(), StandardCharsets.UTF_8);
//            JsonObject body = new JsonObject(response) ;
//            String uuid = body.getString("uuid");
//            String source = body.getString("source");
//            String dest = body.getString("dest");
//			//msg.reply(TransferTaskNotificationListener.class.getName() + " received.");
//
//
//			NotificationMessageContext messageBodyContext = new NotificationMessageContext(
//					MessageType.TRANSFERTASK_CANCELED_COMPLETED, body.encode(), uuid);
//
//			NotificationMessageBody messageBody = new NotificationMessageBody(
//					uuid, body.getString("owner"), body.getString("tenant_id"),
//					messageBodyContext);
//
//			if (body.getString("event") == null)
//				body.put("event", body.getString("status"));
//
//            logger.info("{} notification event raised for {} {}: {}",
//                    body.getString("event"), // event that is sending this body
//                    body.getString("type"),  // message type
//                    body.getString("uuid"),
//                    body.encodePrettily());
//
//			// we publish all notifications to the same channel for consumers to subscribe to. Let them
//			// get the event type from the body of the message rather than the channel to reduce the
//			// complexity of their client apps and the documentation of our notification semantics.
////			_doPublishEvent(MessageType.TRANSFERTASK_NOTIFICATION, body);
//			try {
//				notificationEventProcess(new JsonObject(messageBody.toJSON()));
//			} catch (JsonProcessingException e) {
//				logger.error("Failed to serialize notification for transfer task {} to legacy message format. {}",
//						body.getString("uuid"), e.getMessage());
//			}
//		});
//        d.subscribe(MessageType.TRANSFERTASK_NOTIFICATION);
//        getConnection().flush(Duration.ofMillis(500));
//
//		//bus.<JsonObject>consumer(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
//        s = d.subscribe(MessageType.TRANSFERTASK_CANCELED_COMPLETED, msg -> {
//            //msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
//            String response = new String(msg.getData(), StandardCharsets.UTF_8);
//            JsonObject body = new JsonObject(response) ;
//            String uuid = body.getString("uuid");
//            String source = body.getString("source");
//            String dest = body.getString("dest");
//            //msg.reply(TransferTaskNotificationListener.class.getName() + " received.");
//
//
//            JsonObject notificationMessageBody = processForNotificationMessageBody(MessageType.TRANSFERTASK_CANCELED_COMPLETED, body);
//            notificationEventProcess(notificationMessageBody);
//            logger.info("Transfer task canceled for uuid {} is completed.", body.getString("uuid"));
//            try {
//			    _doPublishEvent(MessageType.NOTIFICATION_CANCELED, body);
//            } catch (Exception e) {
//                logger.debug(e.getMessage());
//            }
//		});
//        d.subscribe(MessageType.TRANSFERTASK_CANCELED_COMPLETED);
//        getConnection().flush(Duration.ofMillis(500));
//
//
//		//bus.<JsonObject>consumer(MessageType.TRANSFERTASK_FINISHED, msg -> {
//        s = d.subscribe(MessageType.TRANSFERTASK_FINISHED, msg -> {
//            //msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
//            String response = new String(msg.getData(), StandardCharsets.UTF_8);
//            JsonObject body = new JsonObject(response) ;
//            String uuid = body.getString("uuid");
//            String source = body.getString("source");
//            String dest = body.getString("dest");
//			//msg.reply(TransferTaskNotificationListener.class.getName() + " received.");
//
//            JsonObject notificationMessageBody = processForNotificationMessageBody(MessageType.TRANSFERTASK_FINISHED, body);
//            notificationEventProcess(notificationMessageBody);
//            logger.info("Transfer task finished for uuid {} is completed.", body.getString("uuid"));
//            try {
//                _doPublishEvent(MessageType.NOTIFICATION_COMPLETED, body);
//            } catch (Exception e) {
//                logger.debug(e.getMessage());
//            }
//		});
//        d.subscribe(MessageType.TRANSFERTASK_FINISHED);
//        getConnection().flush(Duration.ofMillis(500));
//
//
//		//bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
//        s = d.subscribe(MessageType.TRANSFERTASK_PAUSED_COMPLETED, msg -> {
//            //msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
//            String response = new String(msg.getData(), StandardCharsets.UTF_8);
//            JsonObject body = new JsonObject(response) ;
//            String uuid = body.getString("uuid");
//            String source = body.getString("source");
//            String dest = body.getString("dest");
//			//msg.reply(TransferTaskNotificationListener.class.getName() + " received.");
//
//            JsonObject notificationMessageBody = processForNotificationMessageBody(MessageType.TRANSFERTASK_PAUSED_COMPLETED, body);
//            notificationEventProcess(notificationMessageBody);
//            logger.info("Transfer task {} created.", body.getString("uuid"));
//        });
//        d.subscribe(MessageType.TRANSFERTASK_PAUSED_COMPLETED);
//        getConnection().flush(Duration.ofMillis(500));
//
//
//        //bus.<JsonObject>consumer(MessageType.TRANSFERTASK_PARENT_ERROR, msg -> {
//        s = d.subscribe(MessageType.TRANSFERTASK_PARENT_ERROR, msg -> {
//            //msg.reply(TransferTaskAssignedListener.class.getName() + " received.");
//            String response = new String(msg.getData(), StandardCharsets.UTF_8);
//            JsonObject body = new JsonObject(response) ;
//            String uuid = body.getString("uuid");
//            String source = body.getString("source");
//            String dest = body.getString("dest");
//            //msg.reply(TransferTaskNotificationListener.class.getName() + " received.");
//
//            JsonObject notificationMessageBody = processForNotificationMessageBody(MessageType.TRANSFERTASK_PARENT_ERROR, body);
//            notificationEventProcess(notificationMessageBody);
//            logger.info("Transfer task {} created.", body.getString("uuid"));
//        });
//        d.subscribe(MessageType.TRANSFERTASK_PARENT_ERROR);
//        getConnection().flush(Duration.ofMillis(500));


    }

    protected void handleMessage(Message message) {
        try {
            JsonObject notificationMessageBody = new JsonObject(message.getMessage());
            String uuid = notificationMessageBody.getString("uuid");
            String source = notificationMessageBody.getString("source");
            String dest = notificationMessageBody.getString("dest");
            sentToLegacyMessageQueue(notificationMessageBody);

        } catch (DecodeException e) {
            logger.error("Unable to parse message {} body {}. {}", message.getId(), message.getMessage(), e.getMessage());
        } catch (Throwable t) {
            logger.error("Unknown exception processing message message {} body {}. {}", message.getId(), message.getMessage(), t.getMessage());
        }
    }
    /**
     * Process the {@link JsonObject} we recieve from the {@link EventBus} to a {@link NotificationMessageBody} for \
     * compatibility with our legacy message queue.
     *
     * @param messageType {@link MessageType} for the Transfer Task notification event
     * @param body        {@link JsonObject} of the Transfer Task
     * @return {@link NotificationMessageBody}
     */
    protected JsonObject processForNotificationMessageBody(String messageType, JsonObject body) {
        try {
            String uuid = body.getString("uuid");
            String tenantId = body.getString("tenant_id");

            logger.debug("tenantId = {}", tenantId);
            if (uuid == null) {
                logger.error("Transfer task uuid cannot be null.");
            } else {
                NotificationMessageContext messageBodyContext = new NotificationMessageContext(
                        messageType, body.encode(), uuid);

                NotificationMessageBody messageBody = new NotificationMessageBody(
                        uuid, body.getString("owner"), body.getString("tenant_id"),
                        messageBodyContext);

                if (body.getString("event") == null)
                    body.put("event", body.getString("status"));
                if (body.getString("type") == null) {
                    body.put("type", messageType);
                }

                logger.info("{} notification event raised for {} {}: {}",
                        body.getString("event"),
                        body.getString("type"),
                        body.getString("uuid"),
                        body.encodePrettily());

                return new JsonObject(messageBody.toJSON());
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize notification for transfer task {} to legacy message format. {}",
                    body.getString("uuid"), e.getMessage());
        }
        return null;
    }

    /**
     * Writes the notification tot he legacy notification queue. Returns if at least one message was written to the queue.
     *
     * @param body the message body to send
     * @return true if a message was written
     */
    protected boolean sentToLegacyMessageQueue(JsonObject body) {
        logger.info("Sending legacy notification message for transfer task {}", body.getString("uuid"));
        logger.debug("tenantId = {}", body.getString("tenant_id"));
        org.iplantc.service.common.Settings.NOTIFICATION_QUEUE = org.iplantc.service.common.Settings.FILES_STAGING_QUEUE;
        org.iplantc.service.common.Settings.NOTIFICATION_TOPIC = org.iplantc.service.common.Settings.FILES_STAGING_TOPIC;

        return NotificationManager.process(body.getString("uuid"), body.encode(), body.getString("owner")) > 0;
    }

}
