package org.agaveplatform.service.transfers.listener;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.agaveplatform.service.transfers.BaseTestCase;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.agaveplatform.service.transfers.enumerations.MessageType.TRANSFERTASK_ERROR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@DisplayName("TransferErrorListener test")
//@Disabled
class TransferErrorListenerTest extends BaseTestCase {
	private static final Logger log = LoggerFactory.getLogger(TransferErrorListener.class);

	@AfterAll
	public void finish(Vertx vertx, VertxTestContext ctx) {
		vertx.close(ctx.completing());
	}

	protected TransferErrorListener getMockTransferErrorListenerInstance(Vertx vertx) {
		TransferErrorListener ttc = mock(TransferErrorListener.class );
		when(ttc.config()).thenReturn(config);
		when(ttc.getEventChannel()).thenReturn(TRANSFERTASK_ERROR);
		when(ttc.getVertx()).thenReturn(vertx);
		when(ttc.config()).thenReturn(config);
		return ttc;
	}

	@Test
	@DisplayName("TransferErrorListener.processError RemoteDataException and Status= QUEUED test")
	protected void processErrorRDE_test(Vertx vertx, VertxTestContext ctx) {
		TransferTask tt = new TransferTask(TRANSFER_SRC, TRANSFER_DEST, TEST_USERNAME, TENANT_ID, null, null);
		tt.setUuid(new AgaveUUID(UUIDType.TRANSFER).toString());

		log.info("Starting process of notificationEventProcess.");
		JsonObject body = new JsonObject(tt.toJSON());
		body.put("id",4);
		body.put("cause", RemoteDataException.class.getName());
		body.put("message", "Error Message");

		log.info("Cause: = {}", body.getString("cause"));
		TransferErrorListener txfrErrorListener = getMockTransferErrorListenerInstance(vertx);
		doCallRealMethod().when(txfrErrorListener).processError(any(JsonObject.class), any());
		when(txfrErrorListener.taskIsNotInterrupted(tt)).thenCallRealMethod();

		txfrErrorListener.processError(body, resp -> ctx.verify(() -> {
			assertFalse(resp.result(), "processError should return false when the TransferErrorListener.prcessError is called.  It is already in the QUEUE");
			ctx.completeNow();
		}));
	}

	@Test
	@DisplayName("TransferErrorListener.processError IOException and Status= QUEUED test")
	//@Disabled
	protected void processErrorIOE_test(Vertx vertx, VertxTestContext ctx) {
		TransferTask tt = new TransferTask(TRANSFER_SRC, TRANSFER_DEST, TEST_USERNAME, TENANT_ID, null, null);
		tt.setUuid(new AgaveUUID(UUIDType.TRANSFER).toString());

		log.info("Starting process of notificationEventProcess.");
		JsonObject body = new JsonObject(tt.toJSON());
		body.put("id", 3);
		body.put("cause", IOException.class.getName());
		body.put("message", "Error Message");

		log.info("Cause: = {}", body.getString("cause"));
		TransferErrorListener txfrErrorListener = getMockTransferErrorListenerInstance(vertx);
		doCallRealMethod().when(txfrErrorListener).processError(any(JsonObject.class), any());
		when(txfrErrorListener.taskIsNotInterrupted(tt)).thenCallRealMethod();

		txfrErrorListener.processError(body, resp -> ctx.verify(() -> {
			assertFalse(resp.result(), "processError should return true when the TransferErrorListener.prcessError is called for an IOException. It is already in the QUEUE");
			ctx.completeNow();
		}));
	}

	@Test
	@DisplayName("TransferErrorListener.processError IOException and Status= COMPLETED test")
	//@Disabled
	protected void processErrorIOE_COMPLETE_test(Vertx vertx, VertxTestContext ctx) {

		TransferTask tt = new TransferTask(TRANSFER_SRC, TRANSFER_DEST, TEST_USERNAME, TENANT_ID, null, null);
		JsonObject body = tt.toJson();

		log.info("Starting process of notificationEventProcess.");
		body.put("id", 1);
		body.put("cause", IOException.class.getName());
		body.put("message", "Error Message");
		//body.put("status", "COMPLETED");

		TransferErrorListener txfrErrorListener = getMockTransferErrorListenerInstance(vertx);
		doCallRealMethod().when(txfrErrorListener).processError(any(JsonObject.class), any());
		when(txfrErrorListener.taskIsNotInterrupted(tt)).thenCallRealMethod();

		txfrErrorListener.processError(body, resp -> ctx.verify(() -> {
			assertFalse(resp.result(), "processError should return FALSE when the TransferErrorListener.prcessError is called for an IOException and Status = COMPLETED");
			ctx.completeNow();
		}));
	}


	@Test
	@DisplayName("TransferErrorListener.processError InterruptedException and Status= FAILED test")
	//@Disabled
	protected void processErrorInterruptedException_FAILED_test(Vertx vertx, VertxTestContext ctx) {
		TransferTask tt = new TransferTask(TRANSFER_SRC, TRANSFER_DEST, TEST_USERNAME, TENANT_ID, null, null);
		tt.setUuid(new AgaveUUID(UUIDType.TRANSFER).toString());

		log.info("Starting process of notificationEventProcess.");
		JsonObject body = new JsonObject(tt.toJSON());
		body.put("id", 2);
		body.put("cause", "java.lang.InterruptedException");
		body.put("message", "Error Message");
		body.put("status", "FAILED");

		log.info("Cause: = {}", body.getString("cause"));
		TransferErrorListener txfrErrorListener = getMockTransferErrorListenerInstance(vertx);
		doCallRealMethod().when(txfrErrorListener).processError(any(JsonObject.class), any());
		when(txfrErrorListener.taskIsNotInterrupted(tt)).thenCallRealMethod();

		txfrErrorListener.processError(body, resp -> ctx.verify(() -> {
			assertFalse(resp.result(), "processError should return FALSE when the TransferErrorListener.prcessError is called for an IOException and Status = COMPLETED");
			ctx.completeNow();
		}));
	}
}