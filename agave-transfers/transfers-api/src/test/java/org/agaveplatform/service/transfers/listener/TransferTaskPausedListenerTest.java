package org.agaveplatform.service.transfers.listener;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.agaveplatform.service.transfers.BaseTestCase;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseService;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.agaveplatform.service.transfers.enumerations.TransferStatusType;
import org.agaveplatform.service.transfers.messaging.NatsJetstreamMessageClient;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.agaveplatform.service.transfers.enumerations.MessageType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@DisplayName("Transfers processPausedRequest tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Disabled
class TransferTaskPausedListenerTest extends BaseTestCase {

	TransferTaskPausedListener getMockListenerInstance(Vertx vertx) throws IOException, InterruptedException, TimeoutException {
		TransferTaskPausedListener listener = Mockito.mock(TransferTaskPausedListener.class);
		when(listener.getEventChannel()).thenReturn(MessageType.TRANSFERTASK_PAUSED);
		when(listener.getVertx()).thenReturn(vertx);
		when(listener.getRetryRequestManager()).thenCallRealMethod();
		doNothing().when(listener)._doPublishEvent(any(), any());
		//doNothing().when(listener)._doPublishEvent(any(), any());
		doCallRealMethod().when(listener).processPauseRequest(any(), any());
		doCallRealMethod().when(listener).processParentEvent(any(),any(),any());
		doCallRealMethod().when(listener).doHandleError(any(),any(),any(),any());
		doCallRealMethod().when(listener).doHandleFailure(any(),any(),any(),any());
		return listener;
	}

	protected TransferTaskAssignedListener getMockTransferAssignedListenerInstance(Vertx vertx) throws IOException, InterruptedException, TimeoutException {
		TransferTaskAssignedListener listener = mock(TransferTaskAssignedListener.class);
		when(listener.getEventChannel()).thenReturn(TRANSFERTASK_ASSIGNED);
		when(listener.getVertx()).thenReturn(vertx);
		when(listener.taskIsNotInterrupted(any())).thenReturn(true);
		when(listener.uriSchemeIsNotSupported(any())).thenReturn(false);
		doCallRealMethod().when(listener).doHandleError(any(),any(),any(),any());
		doCallRealMethod().when(listener).doHandleFailure(any(),any(),any(),any());
		when(listener.getRetryRequestManager()).thenCallRealMethod();
		doNothing().when(listener)._doPublishEvent(any(), any());
		//doNothing().when(listener)._doPublishEvent(any(), any());
		doCallRealMethod().when(listener).processTransferTask(any(JsonObject.class), any());
		try {
			doCallRealMethod().when(listener).start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return listener;
	}
	NatsJetstreamMessageClient getMockNats() throws MessagingException {
		NatsJetstreamMessageClient natsClient = Mockito.mock(NatsJetstreamMessageClient.class);
		doNothing().when(natsClient).push(any(), any(), any());
		return getMockNats();
	}

	/**
	 * Generates a mock of the {@link TransferTaskDatabaseService} with the {@link TransferTaskDatabaseService#getByUuid(String, String, Handler)}
	 * method mocked out to return the given {@code transferTask};
	 *
	 * @param transferTaskToReturn {@link JsonObject} to return from the {@link TransferTaskDatabaseService#getByUuid(String, String, Handler)} handler
	 * @return a mock of the db service with the getById mocked out to return the {@code transferTaskToReturn} as an async result.
	 */
	private TransferTaskDatabaseService getMockTranserTaskDatabaseService(JsonObject transferTaskToReturn) {
		// mock out the db service so we can can isolate method logic rather than db
		TransferTaskDatabaseService dbService = mock(TransferTaskDatabaseService.class);

		// mock a successful outcome with updated json transfer task result from getById call to db
		AsyncResult<JsonObject> getByAnyHandler = getMockAsyncResult(transferTaskToReturn);

		// mock the handler passed into getById
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(getByAnyHandler);
			return null;
		}).when(dbService).getByUuid(any(), any(), any());

		return dbService;
	}

	/**
	 * Creates a new mock remote data client that will return a valid mock {@link RemoteFileInfo} with
	 * {@link RemoteFileInfo#isDirectory()}, {@link RemoteFileInfo#isFile()}, {@link RemoteFileInfo#getName()} mocked
	 * out. The return value is based on the value of {@code isFile} passed in.
	 *
	 * @param remotePath   the path of the remote file item
	 * @param isDir        true if the path should represent a directory, false otherwise
	 * @param withChildren true if the listing should return more than just the root task. multiple remote file info will be added to directory if true
	 * @return a remote data client that will mock out valie remote responses..
	 */
	private RemoteDataClient getMockRemoteDataClient(String remotePath, boolean isDir, boolean withChildren) {
		RemoteDataClient remoteDataClient = mock(RemoteDataClient.class);
		try {
			// accept the mkdir by default. we don't need to test that here
			when(remoteDataClient.mkdirs(any())).thenReturn(true);
			// mock out the stat on the remote path. this should return a {@link RemoteFileInfo} instance for the path alone
			RemoteFileInfo remotePathFileInfo = generateRemoteFileInfo(remotePath, isDir);
			when(remoteDataClient.getFileInfo(remotePath)).thenReturn(remotePathFileInfo);

			// generate content for directory listing responses
			List<RemoteFileInfo> listing = null;
			if (isDir && withChildren) {
				// if the path should represent a directory, generate the items in the listing response
				listing = List.of(
						generateRemoteFileInfo(remotePath + "/.", true),
						generateRemoteFileInfo(remotePath + "/" + UUID.randomUUID(), true),
						generateRemoteFileInfo(remotePath + "/" + UUID.randomUUID(), false),
						generateRemoteFileInfo(remotePath + "/" + UUID.randomUUID(), false)
				);
			} else {
				// if the path should represent a file, a listing will only return the file item itself
				listing = List.of(remotePathFileInfo);
			}

			// mock out directory listing response
			when(remoteDataClient.ls(any())).thenReturn(listing);

		} catch (RemoteDataException | IOException ignored) {
		}

		return remoteDataClient;
	}


	/**
	 * Generates a new {@link RemoteFileInfo} for the given {@code remotePath}. Directories will have
	 * size 4096. Files will have a random size between 0 and {@link Integer#MAX_VALUE}. The current
	 * date will be set as the last motified time. Name will be the given path.
	 *
	 * @param remotePath the path of the remote file item
	 * @param isDir     false if the instance should have type {@link RemoteFileInfo#FILE_TYPE}, true if {@link RemoteFileInfo#DIRECTORY_TYPE}
	 * @return a valid, populated instance, sans permissions.
	 */
	private RemoteFileInfo generateRemoteFileInfo(String remotePath, boolean isDir) {
		RemoteFileInfo remoteFileInfo = new RemoteFileInfo();
		remoteFileInfo.setName(remotePath);
		remoteFileInfo.setLastModified(new Date());
		remoteFileInfo.setFileType(isDir ? RemoteFileInfo.DIRECTORY_TYPE : RemoteFileInfo.FILE_TYPE);
		remoteFileInfo.setOwner(TEST_USER);
		remoteFileInfo.setSize(isDir ? new Random().nextInt(Integer.MAX_VALUE) : 4096);

		return remoteFileInfo;
	}

	@AfterAll
	public void finish(Vertx vertx, VertxTestContext ctx) {
		vertx.close(ctx.completing());
	}

	@Test
	@DisplayName("Transfer Task Paused Listener - task uuid != root/parent uuid")
	@Disabled
	public void processPauseRequestTest(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();

		TransferTask transferTask = _createTestTransferTask();
		transferTask.setStatus(TransferStatusType.TRANSFERRING);
		transferTask.setStartTime(Instant.now());
		transferTask.setEndTime(Instant.now());
		transferTask.setRootTaskId(parentTask.getUuid());
		transferTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();
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
		}).when(dbService).getByUuid(eq(transferTask.getTenantId()), eq(transferTask.getUuid()), anyObject() );

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
				//verify(listener, never())._doPublishEvent( eq(TRANSFERTASK_PAUSED), eq(transferTask.toJson()));

				// make sure the parent was not processed when none existed for the transfer task
				verify(listener, never()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
				//verify(listener)._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				//verify(listener, never())._doPublishEvent( eq(TRANSFERTASK_PARENT_ERROR), any());
				verify(nats, never()).push(any(),eq(TRANSFERTASK_ERROR),any());
				assertTrue(result.result(),
						"TransferTask response should be true indicating the task completed successfully.");

				assertTrue(result.succeeded(), "TransferTask update should have succeeded");

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("Transfer Task Paused Listener with parent that is active, no parent/root uuid")
	@Disabled
	public void processPauseRequestWithParent(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
//		parentTask.setRootTaskId(parentTask.getUuid());
//		parentTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();

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
		}).when(dbService).getByUuid(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );

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
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());
				verify(nats, never()).push(any(),eq(TRANSFERTASK_ERROR),any());
				Assertions.assertTrue(result.result(),
									"TransferTask response should be true indicating the task completed successfully.");

				assertTrue(result.succeeded(), "TransferTask update should have succeeded");

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("Transfer Task Paused Listener with no parent that is active - processPauseRequest")
	@Disabled
	public void processPauseRequestWithNoParentPaused(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
//		parentTask.setRootTaskId(parentTask.getUuid());
//		parentTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();

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
		}).when(dbService).getByUuid(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );

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
				//verify(listener)._doPublishEvent(eq(TRANSFERTASK_PAUSED_SYNC), any());
				verify(nats, times(1)).push(any(),eq(TRANSFERTASK_PAUSED_SYNC),any());

				// make sure the parent was processed at least one time
				// TODO: why is this at least once? Do we know how many times it should be called?
				verify(listener, atLeastOnce()).processParentEvent(any(), any(), any());

				// make sure no error event is ever thrown
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(nats, never()).push(any(),eq(TRANSFERTASK_ERROR),any());
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				Assertions.assertTrue(result.result(),
						"TransferTask response should be true indicating the task completed successfully.");

				assertTrue(result.succeeded(), "TransferTask update should have succeeded");

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("Transfer Task Paused Listener with parent = child that is active - processPauseRequest")
	@Disabled
	public void processPauseRequestWithParentSamePaused(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
		parentTask.setRootTaskId(parentTask.getUuid());
		parentTask.setParentTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();

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
		}).when(dbService).getByUuid(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );

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
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PAUSED), eq(parentTask.toJson()));
				verify(nats, never()).push(any(),any());

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
	@Disabled
	public void processPauseRequestProcessParentPaused(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.TRANSFERRING);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));
		parentTask.setParentTaskId(parentTask.getUuid());
		parentTask.setRootTaskId(parentTask.getUuid());

		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();

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
		}).when(dbService).getByUuid(eq(parentTask.getTenantId()), eq(parentTask.getUuid()), anyObject() );



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
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(nats, never()).push(any(),any(),any());
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("TransferTask Paused Listener with child and parent task that is active - processParentEvent")
	@Disabled
	public void processPauseRequestProcessNotParentPaused(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

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
		NatsJetstreamMessageClient nats = getMockNats();

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
		}).when(dbService).getByUuid(eq(childTask.getTenantId()), eq(childTask.getUuid()), anyObject() );

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
	@Disabled
	public void processParentEventParentPaused(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		TransferTask childTask = _createTestTransferTask();

		childTask.setStatus(TransferStatusType.TRANSFERRING);
		childTask.setStartTime(Instant.now());
		childTask.setEndTime(Instant.now());
		childTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));


		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();

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
		}).when(dbService).getByUuid(eq(childTask.getTenantId()), eq(childTask.getUuid()), anyObject() );

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
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(nats, never()).push(any(),any(),any());

				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				//verify(listener, atLeastOnce())._doPublishEvent(eq(TRANSFERTASK_PAUSED), any());

				ctx.completeNow();
			});
		});
	}

	@Test
	@DisplayName("TransferTask Paused Listener with parent and check TaskAssigned - processParentEvent")
	@Disabled
	public void processParentEvent2ParentPaused(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		TransferTask childTask = _createTestTransferTask();

		childTask.setStatus(TransferStatusType.TRANSFERRING);
		childTask.setStartTime(Instant.now());
		childTask.setEndTime(Instant.now());
		childTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));


		TransferTaskPausedListener listener = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();

		// mock out the TransferTaskAssinged class
		TransferTaskAssignedListener ta = getMockTransferAssignedListenerInstance(vertx);

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
		}).when(dbService).getByUuid(eq(childTask.getTenantId()), eq(childTask.getUuid()), anyObject() );

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
				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
				verify(nats, never()).push(any(),any(),any());

				//verify(listener, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());

				//verify(listener, atLeastOnce())._doPublishEvent(eq(TRANSFERTASK_PAUSED), any());

				//verify(ta);
				ctx.completeNow();
			});
		});
	}


	@Test
	@DisplayName("Transfer Paused Listener smoke test with Assigned Vertical and checking Paused Vertical")
	@Disabled
	public void processTransferTaskAbortsChildProcessingOnInterrupt(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException, TimeoutException, MessagingException {
		// mock out the test class
		TransferTaskAssignedListener ta = getMockTransferAssignedListenerInstance(vertx);
		// mock of the Paused Listener
		TransferTaskPausedListener tp = getMockListenerInstance(vertx);
		NatsJetstreamMessageClient nats = getMockNats();

		// generate a fake transfer task
		TransferTask rootTransferTask = _createTestTransferTask();
		rootTransferTask.setId(1L);
		JsonObject rootTransferTaskJson = rootTransferTask.toJson();
		// generate the expected updated JsonObject
		JsonObject updatedTransferTaskJson = rootTransferTaskJson.copy().put("status", TransferStatusType.ASSIGNED.name());

		// now mock out our db interactions.
		// Here we are mockign the createOrUpdateChildTransferTask
		TransferTaskDatabaseService dbService = getMockTranserTaskDatabaseService(rootTransferTaskJson);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
			// returning the given transfer task, adding an id if it doesn't have one.
			JsonObject childTask = arguments.getArgumentAt(1, TransferTask.class).toJson().put("id", Instant.now().toEpochMilli());
			handler.handle(Future.succeededFuture(childTask));
//			handler.handle(updatedTransferTaskAsyncResult);
			return null;
		}).when(dbService).createOrUpdateChildTransferTask(any(), any(), any(Handler.class));

		// now we are mocking the updateStatus
		AsyncResult<JsonObject> updatedStatusAsyncResult = getMockAsyncResult(updatedTransferTaskJson);
		doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(3, Handler.class);
			handler.handle(updatedStatusAsyncResult);
			return null;
		}).when(dbService).updateStatus(any(), any(), any(), any(Handler.class));


		// assign the mock db service to the test listener
		when(ta.getDbService()).thenReturn(dbService);


		// get mock remote data clients to mock the remote src system interactions
		URI srcUri = URI.create(rootTransferTask.getSource());
		RemoteDataClient srcRemoteDataClient = getMockRemoteDataClient(srcUri.getPath(), true, true);

		// get mock remote data clients to mock the remote dest system interactions
		URI destUri = URI.create(rootTransferTask.getDest());
		RemoteDataClient destRemoteDataClient = getMockRemoteDataClient(destUri.getPath(), true, true);

		try {
			when(ta.getRemoteDataClient(eq(rootTransferTask.getTenantId()), eq(rootTransferTask.getOwner()), eq(srcUri))).thenReturn(srcRemoteDataClient);
			when(ta.getRemoteDataClient(eq(rootTransferTask.getTenantId()), eq(rootTransferTask.getOwner()), eq(destUri))).thenReturn(destRemoteDataClient);
		} catch (Exception e) {
			// bubble the failure up to the VertxTestContext so exceptions are reported with messages
			try {
				fail("Failed to initialize the remote data clients during test setup.", e);
			} catch (Throwable t) {
				ctx.failNow(t);
			}
		}

		// mock a successful outcome with a puased result from processParentRequest indicating the child has no active parents
		AsyncResult<Boolean> processPauseRequest = getMockAsyncResult(Boolean.FALSE);
		// mock the handler passed into processPauseRequest
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(1, Handler.class);
			handler.handle(processPauseRequest);
			return null;
		}).when(tp).processPauseRequest(any(), any());

		// mock a successful outcome with a puased result from processParentRequest indicating the child has no active parents
		AsyncResult<Boolean> processParentRequestHandler = getMockAsyncResult(Boolean.FALSE);

		// mock the handler passed into processParentEvent
		doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
			@SuppressWarnings("unchecked")
			Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(2, Handler.class);
			handler.handle(processParentRequestHandler);
			return null;
		}).when(tp).processParentEvent(any(), any(), any());


		// we already mock this once in the mock listener setup. This will override the mock returning true the first
		// invocation of the method and false thereafter.
		when(ta.taskIsNotInterrupted(any())).thenReturn(true,false);

		try {
			ta.start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		vertx.eventBus().send(TRANSFERTASK_PAUSED_SYNC, rootTransferTaskJson);
		ctx.verify(() ->{
			doCallRealMethod().when(ta).addCancelledTask(anyString());
			ctx.completeNow();
		});
//
//		ta.processTransferTask(rootTransferTaskJson, result -> {
//			ctx.verify(() -> {
//				assertTrue(result.succeeded(), "Task assignment should return true on successful processing.");
//				assertFalse(result.result(), "Callback result should be true after successful assignment.");
//
//				verify(ta).addCancelledTask(rootTransferTask.getUuid() );

//				// remote file info should be obtained once.
//				verify(srcRemoteDataClient, times(1)).getFileInfo(eq(srcUri.getPath()));
//
//				// mkdir should never be called on the src client.
//				verify(srcRemoteDataClient, never()).mkdirs(eq(srcUri.getPath()));
//
//				// mkdir should only be called on non-empty directory items.
//				verify(destRemoteDataClient, times(1)).mkdirs(any());
//
//				// listing should be called with srch path
//				verify(srcRemoteDataClient, times(1)).ls(eq(srcUri.getPath()));
//
//				// should be called once after the children are processed
//				verify(dbService, never()).updateStatus(any(), any(), any(), any());
//
//				// get the test list of remote child file items
//				List<RemoteFileInfo> remoteFileInfoList = srcRemoteDataClient.ls(srcUri.getPath());
//
//				// according to our above mock, this interruption check should be called twice. once upon entry
//				// where it returns true. Another time when processing the first child when it shold return false and
//				// trigger an abortion of the stream processing.
//				verify(ta, times(2)).taskIsNotInterrupted(eq(rootTransferTask));
//
//				// the method should never be reached. the interrupt should abort all further processing of the children
//				verify(dbService, never()).createOrUpdateChildTransferTask(any(), any(), any());
//
//				// directory children should raise TRANSFERTASK_CREATED events
//				verify(ta, times(1))._doPublishEvent(eq(TRANSFERTASK_CANCELED_ACK), eq(rootTransferTaskJson));
//				// file item children should never be reached
//				verify(ta, never())._doPublishEvent(eq(TRANSFER_ALL), any(JsonObject.class));
//				verify(ta, never())._doPublishEvent(eq(TRANSFERTASK_CREATED), any(JsonObject.class));
//
//				// no error event should have been raised
//				verify(ta, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
//
//				ctx.completeNow();
//			});

//			tp.processPauseRequest(rootTransferTask.toJson(), tpResult -> {
//				ctx.verify(() -> {
//					assertTrue(tpResult.succeeded(), "Task assignment should return true on successful processing.");
//					assertFalse(tpResult.result(), "Callback result should be true after successful assignment.");
//
//					// verify that the completed event was created. this should always be throws
//					// if the updateStatus result succeeds.
//					verify(tp)._doPublishEvent(eq(TRANSFERTASK_PAUSED_SYNC), any());
//
//					// make sure the parent was processed at least one time
//					verify(tp).processParentEvent(any(), any(), any());
//
//					verify(tp, never())._doPublishEvent(eq(TRANSFERTASK_ERROR), any());
//					verify(tp, never())._doPublishEvent(eq(TRANSFERTASK_PARENT_ERROR), any());
//
//					ctx.completeNow();
//				});
//			});
//		});
	}



}