package org.agaveplatform.service.transfers.listener;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.agaveplatform.service.transfers.BaseTestCase;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseService;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.agaveplatform.service.transfers.enumerations.TransferStatusType;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.time.Instant;

import static org.agaveplatform.service.transfers.enumerations.MessageType.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@DisplayName("Transfers processPausedRequest tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Disabled
class TransferTaskPausedListenerTest extends BaseTestCase {


	TransferTaskPausedListener getMockListenerInstance(Vertx vertx) {
		TransferTaskPausedListener listener = Mockito.mock(TransferTaskPausedListener.class);
		when(listener.getEventChannel()).thenReturn(MessageType.TRANSFERTASK_PAUSED);
		when(listener.getVertx()).thenReturn(vertx);
		when(listener.getRetryRequestManager()).thenCallRealMethod();
		doNothing().when(listener)._doPublishEvent(any(), any());
		doCallRealMethod().when(listener).processPauseRequest(any(), any());
		doCallRealMethod().when(listener).doHandleError(any(),any(),any(),any());
		doCallRealMethod().when(listener).doHandleFailure(any(),any(),any(),any());
		return listener;
	}

	@AfterAll
	public void finish(Vertx vertx, VertxTestContext ctx) {
		vertx.close(ctx.completing());
	}

	@Test
	@DisplayName("Transfer Task Paused Listener - task uuid != root/parent uuid")
	//@Disabled
	public void processPauseRequestTest(Vertx vertx, VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();

		TransferTask transferTask = _createTestTransferTask();
		transferTask.setStatus(TransferStatusType.TRANSFERRING);
		transferTask.setStartTime(Instant.now());
		transferTask.setEndTime(Instant.now());
		transferTask.setRootTaskId(parentTask.getUuid());
		transferTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		//doAnswer((Answer )).when(listener.getTransferTask(anyString(), anyString(), any()));

		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock the dbService getter in our mocked vertical so we don't need to use powermock
		when(listener.getDbService()).thenReturn(dbService);

		JsonObject expectedgetByIdAck = transferTask.toJson();
		AsyncResult<JsonObject> updateGetById = getMockAsyncResult(expectedgetByIdAck);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateGetById);
			return null;
		}).when(dbService).getById(eq(transferTask.getTenantId()), eq(transferTask.getUuid()), anyObject() );

		// mock a successful outcome with updated json transfer task result from updateStatus
		JsonObject expectedUdpatedJsonObject = transferTask.toJson()
				.put("status", TransferStatusType.PAUSED.name())
				.put("endTime", Instant.now());

		AsyncResult<JsonObject> updateStatusHandler = getMockAsyncResult(expectedUdpatedJsonObject);

		// mock the handler passed into updateStatus
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(3, Handler.class);
			handler.handle(updateStatusHandler);
			return null;
		}).when(dbService).updateStatus(any(), any(), any(), any());


		// mock a successful outcome with a puased result from processPauseRequest indicating the child has no active parents
		AsyncResult<Boolean> processPauseRequest = getMockAsyncResult(Boolean.FALSE);

		// mock the handler passed into processParentEvent
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(processPauseRequest);
			return null;
		}).when(listener).processParentEvent(any(), any(), any());

		// now we run the actual test using our test transfer task data
		listener.processPauseRequest(transferTask.toJson(), result -> {
			ctx.verify(() -> {
				assertTrue(result.succeeded(), "Call to processPauseRequest should succeed for active task");
				assertTrue(result.result(), "Response from processPauseRequest should be true when task is active root task");

				// verify the db service was called to update the task status
//				verify(dbService).updateStatus(eq(transferTask.getTenantId()),
//						eq(transferTask.getUuid()), eq(TransferStatusType.PAUSED.name()), any());

				// verify that the completed event was created. this should always be throws
				// if the updateStatus result succeeds.
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PAUSED), eq(transferTask.toJson()));

				// make sure the parent was not processed when none existed for the transfer task
				verify(listener, never()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
				verify(listener)._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				assertTrue(result.result(),
						"TransferTask response should be true indicating the task completed successfully.");

				assertTrue(result.succeeded(), "TransferTask update should have succeeded");

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("Transfer Task Paused Listener with parent that is active, no parent/root uuid")
	//@Disabled
	public void processPauseRequestWithParent(Vertx vertx, VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
//		parentTask.setRootTaskId(parentTask.getUuid());
//		parentTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		//doAnswer((Answer )).when(listener.getTransferTask(anyString(), anyString(), any()));
//		when(listener.getTransferTask(eq(parentTask.getParentTaskId()), eq(parentTask.getUuid()), any() ) ).thenReturn(parentTask);

		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock the dbService getter in our mocked vertical so we don't need to use powermock
		when(listener.getDbService()).thenReturn(dbService);

		JsonObject expectedgetByIdAck = parentTask.toJson();
		AsyncResult<JsonObject> updateGetById = getMockAsyncResult(expectedgetByIdAck);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateGetById);
			return null;
		}).when(dbService).getById(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );

		// mock a successful outcome with updated json transfer task result from updateStatus
		JsonObject expectedUdpatedJsonObject = parentTask.toJson()
				.put("status", TransferStatusType.PAUSED.name())
				.put("endTime", Instant.now());

		AsyncResult<JsonObject> updateStatusHandler = getMockAsyncResult(expectedUdpatedJsonObject);

		// mock the handler passed into updateStatus
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(3, Handler.class);
			handler.handle(updateStatusHandler);
			return null;
		}).when(dbService).updateStatus(any(), any(), any(), any());


		// mock a successful outcome with a puased result from processPauseRequest indicating the child has no active parents
		AsyncResult<Boolean> processPauseRequest = getMockAsyncResult(Boolean.FALSE);

		// mock the handler passed into processParentEvent
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(processPauseRequest);
			return null;
		}).when(listener).processParentEvent(any(), any(), any());

		// now we run the actual test using our test transfer task data
		listener.processPauseRequest(parentTask.toJson(), result -> {
			ctx.verify(() -> {
				assertTrue(result.succeeded(), "Call to processPauseRequest should succeed for active task");
				assertTrue(result.result(), "Response from processPauseRequest should be true when task is an active root task");

				// verify the db service was called to update the task status
				verify(dbService).updateStatus(eq(parentTask.getTenantId()),
						eq(parentTask.getUuid()), eq(TransferStatusType.PAUSE_WAITING.name()), any());

				// verify that the completed event was created. this should always be throws
				// if the updateStatus result succeeds.
				//verify(listener)._doPublishEvent(eq(TRANSFERTASK_PAUSED_SYNC), eq(parentTask.toJson()));

				// make sure the parent was processed at least one time
				// TODO: why is this at least once? Do we know how many times it should be called?
				verify(listener, atLeastOnce()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				Assertions.assertTrue(result.result(),
									"TransferTask response should be true indicating the task completed successfully.");

				assertTrue(result.succeeded(), "TransferTask update should have succeeded");

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("Transfer Task Paused Listener with no parent that is active - processPauseRequest")
	//@Disabled
	public void processPauseRequestWithNoParentPaused(Vertx vertx, VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
//		parentTask.setRootTaskId(parentTask.getUuid());
//		parentTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		//doAnswer((Answer )).when(listener.getTransferTask(anyString(), anyString(), any()));
//		when(listener.getTransferTask(eq(parentTask.getParentTaskId()), eq(parentTask.getUuid()), any() ) ).thenReturn(parentTask);

		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock the dbService getter in our mocked vertical so we don't need to use powermock
		when(listener.getDbService()).thenReturn(dbService);

		JsonObject expectedgetByIdAck = parentTask.toJson();
		AsyncResult<JsonObject> updateGetById = getMockAsyncResult(expectedgetByIdAck);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateGetById);
			return null;
		}).when(dbService).getById(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );

		// mock a successful outcome with updated json transfer task result from updateStatus
		JsonObject expectedUdpatedJsonObject = parentTask.toJson()
				.put("status", TransferStatusType.PAUSED.name())
				.put("endTime", Instant.now());

		AsyncResult<JsonObject> updateStatusHandler = getMockAsyncResult(expectedUdpatedJsonObject);

		// mock the handler passed into updateStatus
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(3, Handler.class);
			handler.handle(updateStatusHandler);
			return null;
		}).when(dbService).updateStatus(any(), any(), any(), any());


		// mock a successful outcome with a puased result from processPauseRequest indicating the child has no active parents
		AsyncResult<Boolean> processPauseRequest = getMockAsyncResult(Boolean.FALSE);

		// mock the handler passed into processParentEvent
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(processPauseRequest);
			return null;
		}).when(listener).processParentEvent(any(), any(), any());

		// now we run the actual test using our test transfer task data
		listener.processPauseRequest(parentTask.toJson(), result -> {
			ctx.verify(() -> {
				assertTrue(result.succeeded(), "Call to processPauseRequest should succeed for active task");
				assertTrue(result.result(), "Response from processPauseRequest should be true when task is not active and there is no root task");

				// verify the db service was called to update the task status
				verify(dbService).updateStatus(eq(parentTask.getTenantId()),
						eq(parentTask.getUuid()), eq(TransferStatusType.PAUSE_WAITING.name()), any());

				// verify that the completed event was created. this should always be throws
				// if the updateStatus result succeeds.
				verify(listener)._doPublishEvent(eq(TRANSFERTASK_PAUSED_SYNC), any());

				// make sure the parent was processed at least one time
				// TODO: why is this at least once? Do we know how many times it should be called?
				verify(listener, atLeastOnce()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				Assertions.assertTrue(result.result(),
						"TransferTask response should be true indicating the task completed successfully.");

				assertTrue(result.succeeded(), "TransferTask update should have succeeded");

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("Transfer Task Paused Listener with parent = child that is active - processPauseRequest")
	//@Disabled
	public void processPauseRequestWithParentSamePaused(Vertx vertx, VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
		parentTask.setRootTaskId(parentTask.getUuid());
		parentTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		//doAnswer((Answer )).when(listener.getTransferTask(anyString(), anyString(), any()));
//		when(listener.getTransferTask(eq(parentTask.getParentTaskId()), eq(parentTask.getUuid()), any() ) ).thenReturn(parentTask);

		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock the dbService getter in our mocked vertical so we don't need to use powermock
		when(listener.getDbService()).thenReturn(dbService);

		JsonObject expectedgetByIdAck = parentTask.toJson();
		AsyncResult<JsonObject> updateGetById = getMockAsyncResult(expectedgetByIdAck);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateGetById);
			return null;
		}).when(dbService).getById(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );

		// mock a successful outcome with updated json transfer task result from updateStatus
		JsonObject expectedUdpatedJsonObject = parentTask.toJson()
				//.put("status", TransferStatusType.PAUSED.name())
				.put("endTime", Instant.now());

		AsyncResult<JsonObject> updateStatusHandler = getMockAsyncResult(expectedUdpatedJsonObject);

		// mock the handler passed into updateStatus
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(3, Handler.class);
			handler.handle(updateStatusHandler);
			return null;
		}).when(dbService).updateStatus(any(), any(), any(), any());


		// mock a successful outcome with a puased result from processPauseRequest indicating the child has no active parents
		AsyncResult<Boolean> processPauseRequest = getMockAsyncResult(Boolean.FALSE);

		// mock the handler passed into processParentEvent
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(processPauseRequest);
			return null;
		}).when(listener).processParentEvent(any(), any(), any());

		// now we run the actual test using our test transfer task data
		listener.processPauseRequest(parentTask.toJson(), result -> {
			ctx.verify(() -> {
				assertTrue(result.succeeded(), "Call to processPauseRequest should succeed for active task");
				assertTrue(result.result(), "Response from processPauseRequest should be true when task is not active and there is no root task");

				// verify the db service was called to update the task status
//				verify(dbService).updateStatus(eq(parentTask.getTenantId()),
//						eq(parentTask.getUuid()), eq(TransferStatusType.PAUSED.name()), any());

				// verify that the completed event was created. this should always be throws
				// if the updateStatus result succeeds.
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PAUSED), eq(parentTask.toJson()));


				// make sure the parent was processed at least one time
				// TODO: why is this at least once? Do we know how many times it should be called?
				verify(listener, never()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
//				verify(listener, times(1))._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
//				verify(listener, times(1))._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				Assertions.assertTrue(result.result(),
						"TransferTask response should be true indicating the task completed successfully.");

				assertTrue(result.succeeded(), "TransferTask update should have succeeded");

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("TransferTask Paused Listener with parent=child that is active - processParentEvent")
	//@Disabled
	public void processPauseRequestProcessParentPaused(Vertx vertx, VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
		parentTask.setParentTaskId(parentTask.getUuid());
		parentTask.setRootTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		//doAnswer((Answer )).when(listener.getTransferTask(anyString(), anyString(), any()));
//		when(listener.getTransferTask(eq(parentTask.getParentTaskId()), eq(parentTask.getUuid()), any() ) ).thenReturn(parentTask);

		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock the dbService getter in our mocked vertical so we don't need to use powermock
		when(listener.getDbService()).thenReturn(dbService);

		JsonObject expectedgetByIdAck = parentTask.toJson();
		AsyncResult<JsonObject> updateGetById = getMockAsyncResult(expectedgetByIdAck);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateGetById);
			return null;
		}).when(dbService).getById(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );



		// mock a successful outcome with updated json transfer task result from updateStatus
		JsonObject expectedUdpatedJsonObject = parentTask.toJson()
				//.put("status", TransferStatusType.PAUSED.name())
				.put("endTime", Instant.now());

		AsyncResult<Boolean> updateStatusHandler = getMockAsyncResult(Boolean.TRUE);

		// mock the handler passed into updateStatus
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateStatusHandler);
			return null;
		}).when(dbService).allChildrenCancelledOrCompleted(any(), any(), any());



		doCallRealMethod().when(listener).processParentEvent(any(), any(), any());

		// now we run the actual test using our test transfer task data
		listener.processParentEvent(parentTask.getTenantId(), parentTask.getUuid(), result -> {
			ctx.verify(() -> {
				assertTrue(result.succeeded(), "Call to processPauseRequest should succeed for active task");
				assertFalse(result.result(), "Response from processPauseRequest should be true when task is not active and there is no root task");

				// make sure the parent was processed at least one time
				verify(listener, atLeastOnce()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("TransferTask Paused Listener with child and parent task that is active - processParentEvent")
	//@Disabled
	public void processPauseRequestProcessNotParentPaused(Vertx vertx, VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		TransferTask childTask = _createTestTransferTask();

		childTask.setStatus(TransferStatusType.TRANSFERRING);
		childTask.setStartTime(Instant.now());
		childTask.setEndTime(Instant.now());
		childTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
		childTask.setParentTaskId(parentTask.getParentTaskId());
		childTask.setRootTaskId(parentTask.getRootTaskId());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);

		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock the dbService getter in our mocked vertical so we don't need to use powermock
		when(listener.getDbService()).thenReturn(dbService);

		JsonObject expectedgetByIdAck = childTask.toJson();
		AsyncResult<JsonObject> updateGetById = getMockAsyncResult(expectedgetByIdAck);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateGetById);
			return null;
		}).when(dbService).getById(eq(childTask.getTenantId()), eq(childTask.getUuid()), anyObject() );

		// mock a successful outcome with updated json transfer task result from updateStatus
		JsonObject expectedUdpatedJsonObject = childTask.toJson()
				//.put("status", TransferStatusType.PAUSED.name())
				.put("endTime", Instant.now());

		AsyncResult<Boolean> updateStatusHandler = getMockAsyncResult(Boolean.TRUE);

		// mock the handler passed into updateStatus
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateStatusHandler);
			return null;
		}).when(dbService).allChildrenCancelledOrCompleted(any(), any(), any());

		doCallRealMethod().when(listener).processParentEvent(any(), any(), any());

		// now we run the actual test using our test transfer task data
		listener.processParentEvent(childTask.getTenantId(), childTask.getUuid(), result -> {
			ctx.verify(() -> {
				assertTrue(result.succeeded(), "Call to processParentEvent should succeed for active task");
				assertTrue(result.result(), "Response from processPauseRequest should be false when task is a child aka leaf task");

				// make sure the parent was processed at least one time
				verify(listener, atLeastOnce()).processParentEvent(any(), any(), any());


				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("TransferTask Paused Listener with parent task that is active - processParentEvent")
	//@Disabled
	public void processParentEventParentPaused(Vertx vertx, VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		TransferTask childTask = _createTestTransferTask();

		childTask.setStatus(TransferStatusType.TRANSFERRING);
		childTask.setStartTime(Instant.now());
		childTask.setEndTime(Instant.now());
		childTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));


		TransferTaskPausedListener listener = getMockListenerInstance(vertx);

		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock the dbService getter in our mocked vertical so we don't need to use powermock
		when(listener.getDbService()).thenReturn(dbService);

		JsonObject expectedgetByIdAck = childTask.toJson();
		AsyncResult<JsonObject> updateGetById = getMockAsyncResult(expectedgetByIdAck);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateGetById);
			return null;
		}).when(dbService).getById(eq(childTask.getTenantId()), eq(childTask.getUuid()), anyObject() );

		// mock a successful outcome with updated json transfer task result from updateStatus
		JsonObject expectedUdpatedJsonObject = childTask.toJson()
				//.put("status", TransferStatusType.PAUSED.name())
				.put("endTime", Instant.now());

		AsyncResult<Boolean> updateStatusHandler = getMockAsyncResult(Boolean.TRUE);

		// mock the handler passed into updateStatus
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(updateStatusHandler);
			return null;
		}).when(dbService).allChildrenCancelledOrCompleted(any(), any(), any());

		doCallRealMethod().when(listener).processParentEvent(any(), any(), any());

		// now we run the actual test using our test transfer task data
		listener.processParentEvent(childTask.getTenantId(), childTask.getUuid(), result -> {
			ctx.verify(() -> {
				assertTrue(result.succeeded(), "Call to processParentEvent should succeed for active task");
				assertTrue(result.result(), "Response from processPauseRequest should be false when task is a child aka leaf task");

				// make sure the parent was processed at least one time
				verify(listener, atLeastOnce()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				verify(listener, atLeastOnce())._doPublishEvent(eq(TRANSFERTASK_PAUSED), any());

				ctx.completeNow();
			});
		});
	}



}