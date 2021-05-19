package org.agaveplatform.service.transfers.listener;

import io.nats.client.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.agaveplatform.service.transfers.TransferTaskConfigProperties;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseService;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.agaveplatform.service.transfers.TransferTaskConfigProperties.CONFIG_TRANSFERTASK_DB_QUEUE;


public class NatsListener extends  AbstractNatsListener {
    private final static Logger log = LoggerFactory.getLogger(NatsListener.class);

    private TransferTaskDatabaseService dbService;
    protected List<String> parentList = new ArrayList<>();
    public final Connection nc = _connect();

    protected static final String EVENT_CHANNEL = MessageType.TRANSFERTASK_ASSIGNED;

    public NatsListener() throws IOException, InterruptedException {
        super();
    }
    public NatsListener(Vertx vertx) throws IOException, InterruptedException {
        super(vertx);
    }
    public NatsListener(Vertx vertx, String eventChannel) throws IOException, InterruptedException {
        super(vertx, eventChannel);
    }

    public String getDefaultEventChannel() {
        return EVENT_CHANNEL;
    }
    public JetStream js = _jsmConnect(
            config().getString(TransferTaskConfigProperties.NATS_URL));
//    ,
//            "TRANSFERTASK_ASSIGNED",
//            MessageType.TRANSFERTASK_ASSIGNED);

    @Override
    public void start() throws IOException, InterruptedException, TimeoutException {

        // init our db connection from the pool
        String dbServiceQueue = config().getString(CONFIG_TRANSFERTASK_DB_QUEUE);
        dbService = TransferTaskDatabaseService.createProxy(vertx, dbServiceQueue);

        // Build our subscription options. Durable is REQUIRED for pull based subscriptions
        PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                .durable("TRANSFERTASK_ASSIGNED_Consumer")
                .stream("TRANSFERTASK_ASSIGNED")
                .build();

        // init our JetStreamSubscription

        try {
            JetStreamSubscription sub = js.subscribe("TRANSFERTASK_ASSIGNED", pullOptions);
            log.info("got subscription: ", sub.getConsumerInfo().toString());

            sub.pull(1);
            Message m = sub.nextMessage(Duration.ofSeconds(1));
            if (m != null) {
                if (m.isJetStream()) {
                    log.info(m.getData().toString());
                    m.ack();
                }
                m.getData();
                log.info("Subject: ",m.getSubject());
                log.info("Data:  ", m.getData());
            }

        } catch (JetStreamApiException e) {
            log.debug("Error with subsription {}", e.getMessage());
        } catch (Exception e){
            log.debug(e.getMessage());
        }
        nc.flush(Duration.ofSeconds(1));

    }

    public void processEvent(JsonObject body, Handler<AsyncResult<Boolean>> handler) {
        log.debug("Got into NatsListener.processEvent ");
        log.debug("body = {} ", body);
        String uuid = body.getString("uuid");
        String source = body.getString("source");
        log.debug( "uuid is {} and source is {}", uuid, source);
        handler.handle(Future.succeededFuture(true));
    }


    public TransferTaskDatabaseService getDbService() {
        return dbService;
    }

    public void setDbService(TransferTaskDatabaseService dbService) {
        this.dbService = dbService;
    }


    public void processInterrupt(JsonObject body, Handler<AsyncResult<Boolean>> handler) {

    }
}
