package org.agaveplatform.service.transfers.listener;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class AbstractTransferTaskListener extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(AbstractTransferTaskListener.class);

    String address;
    String eventChannel;

    public AbstractTransferTaskListener() {
        super();
    }

    public AbstractTransferTaskListener(Vertx vertx) {
        this(vertx, null);
    }

    public AbstractTransferTaskListener(Vertx vertx, String eventChannel) {
        super();
        setVertx(vertx);
        setEventChannel(eventChannel);
    }

    /**
     * Overriding the parent with a null safe check for the verticle context
     * before continuing. This affords us the luxury of spying on this instance
     * with a mock call chain.
     * @return the config if present, an empty JsonObject otherwise
     */
    @Override
    public JsonObject config() {
        if (this.context == null) {
            return new JsonObject();
        } else {
            return this.context.config();
        }
    }

    protected void _doPublishEvent(String event, Object body) {
        logger.debug("Publishing {} event: {}", event, body);
        getVertx().eventBus().publish(event, body);
    }

    /**
     * Sets the vertx instance for this listener
     * @param vertx the current instance of vertx
     */
    protected void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Sets the eventChannel on which to listen
     * @param eventChannel
     */
    public void setEventChannel(String eventChannel) {
        this.eventChannel = eventChannel;
    }

    /**
     * @return the message type to listen to
     */
    public String getEventChannel() {
        return eventChannel == null ? getDefaultEventChannel() : eventChannel;
    }

    public abstract String getDefaultEventChannel();
}
