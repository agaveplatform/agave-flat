package org.agaveplatform.service.transfers.listener;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.agaveplatform.service.transfers.BaseTestCase;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.iplantc.service.systems.exceptions.SystemRoleException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.agaveplatform.service.transfers.enumerations.MessageType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@DisplayName("Transfers assignTransferTask tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferTaskCreatedListenerTest extends BaseTestCase {

	private static final Logger log = LoggerFactory.getLogger(TransferTaskCreatedListenerTest.class);

	TransferTaskCreatedListener getMockListenerInstance(Vertx vertx) {
		TransferTaskCreatedListener ttc = Mockito.mock(TransferTaskCreatedListener.class);
		when(ttc.getEventChannel()).thenReturn(TRANSFERTASK_CREATED);
		when(ttc.getVertx()).thenReturn(vertx);
		when(ttc.getRemoteSystemAO()).thenCallRealMethod();
		when(ttc.taskIsNotInterrupted(any())).thenCallRealMethod();
		doCallRealMethod().when(ttc).assignTransferTask(any(), any());

		return ttc;
	}

	@Test
	@DisplayName("Transfer Task Created Listener - assignment succeeds for valid transfer task")
	public void assignTransferTask(Vertx vertx, VertxTestContext ctx) {

		// get the JsonObject to pass back and forth between verticles
		TransferTask transferTask = _createTestTransferTask();
		JsonObject json = transferTask.toJson();

		// mock out the verticle we're testing so we can observe that its methods were called as expected
		TransferTaskCreatedListener ttc = getMockListenerInstance(vertx);
		try {
			// return true on permission checks for this test
			when(ttc.userHasMinimumRoleOnSystem(eq(transferTask.getTenantId()), eq(transferTask.getOwner()), anyString(), any(RoleType.class))).thenReturn(true);
		} catch (SystemUnknownException | SystemUnavailableException | SystemRoleException e) {
			ctx.failNow(e);
		}

		ttc.assignTransferTask(json, ctx.succeeding(isAssigned -> {
			ctx.verify(() -> {
				assertTrue(isAssigned);
				verify(ttc, times(1))._doPublishEvent(TRANSFERTASK_ASSIGNED, json);
				verify(ttc, never())._doPublishEvent(TRANSFERTASK_ERROR, any(JsonObject.class));
				verify(ttc,times(2)).userHasMinimumRoleOnSystem(any(),any(),any(),any());
				ctx.completeNow();
			});
		}));
	}

	@Test
	@DisplayName("Transfer Task Created Listener - assignment fails with invalid source")
	public void assignTransferTaskFailSrcTest(Vertx vertx, VertxTestContext ctx) {

		// get the JsonObject to pass back and forth between verticles
		TransferTask transferTask = _createTestTransferTask();
		transferTask.setSource("htt://");
		JsonObject json = transferTask.toJson();

		// mock out the verticle we're testing so we can observe that its methods were called as expected
		TransferTaskCreatedListener ttc = getMockListenerInstance(vertx);
		try {
			// return true on permission checks for this test
			when(ttc.userHasMinimumRoleOnSystem(eq(transferTask.getTenantId()), eq(transferTask.getOwner()), anyString(), any(RoleType.class))).thenReturn(true);
		} catch (SystemUnknownException | SystemUnavailableException | SystemRoleException e) {
			ctx.failNow(e);
		}

		ttc.assignTransferTask(json, ctx.failing(cause -> {
			ctx.verify(() -> {
				assertEquals(cause.getClass(), RemoteDataSyntaxException.class, "Result should have been RemoteDataSyntaxException");
				verify(ttc, never())._doPublishEvent(TRANSFERTASK_ASSIGNED, json);
				verify(ttc,never()).userHasMinimumRoleOnSystem(any(),any(),any(),any());

				JsonObject errorBody = new JsonObject()
						.put("cause", cause.getClass().getName())
						.put("message", cause.getMessage())
						.mergeIn(json);
				verify(ttc, times(1))._doPublishEvent(TRANSFERTASK_ERROR, errorBody);
				ctx.completeNow();
			});
		}));
	}

}