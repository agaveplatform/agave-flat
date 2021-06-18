package org.agaveplatform.service.transfers.listener;

import io.nats.client.Connection;
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
import org.agaveplatform.service.transfers.messaging.NatsJetstreamMessageClient;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static org.agaveplatform.service.transfers.enumerations.MessageType.TRANSFER_RETRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@DisplayName("👋 TransferRetryListenerTest test")
//@Disabled
class TransferTaskRetryListenerTest extends BaseTestCase {

    protected TransferTaskRetryListener getMockTransferRetryListenerInstance(Vertx vertx) throws Exception {
        TransferTaskRetryListener listener = mock(TransferTaskRetryListener.class);
        when(listener.getEventChannel()).thenReturn(TRANSFER_RETRY);
        when(listener.getVertx()).thenReturn(vertx);
        when(listener.taskIsNotInterrupted(any())).thenReturn(true);
        when(listener.getRetryRequestManager()).thenCallRealMethod();
        when(listener.uriSchemeIsNotSupported(any())).thenReturn(false);
        doCallRealMethod().when(listener)._doPublishEvent(any(), any(), any());
        doCallRealMethod().when(listener).processRetryTransferTask(any(), any());
        doCallRealMethod().when(listener).doHandleError(any(), any(), any(), any());
        doCallRealMethod().when(listener).doHandleFailure(any(), any(), any(), any());
        Connection mockConnection = mock(Connection.class);
        when(listener.config()).thenReturn(config);
        when(listener.getRemoteDataClient(any(), any(), any())).thenReturn(mock(RemoteDataClient.class));

        NatsJetstreamMessageClient natsClient = mock(NatsJetstreamMessageClient.class);
        doNothing().when(natsClient).push(any(), any());
        when(listener.getMessageClient()).thenReturn(natsClient);


        return listener;
    }
//    NatsJetstreamMessageClient getMockNats() throws MessagingException {
//        NatsJetstreamMessageClient natsClient = Mockito.mock(NatsJetstreamMessageClient.class);
//        doNothing().when(natsClient).push(any(), any(), any());
//        return getMockNats();
//    }

    @AfterAll
    public void finish(Vertx vertx, VertxTestContext ctx) {
        vertx.close(ctx.completing());
    }


    @Test
    @DisplayName("Process TransferTaskPublishesProtocolEvent")
    //@Disabled
    public void processTransferTaskPublishesProtocolEvent(Vertx vertx, VertxTestContext ctx) throws Exception {
        //JsonObject body = new JsonObject();
        TransferTask tt = new TransferTask(TRANSFER_SRC, TRANSFER_DEST, TEST_USERNAME, TENANT_ID, null, null);

        JsonObject body = tt.toJson();


        vertx.eventBus().consumer("transfertask.sftp", msg -> {
            JsonObject bodyRec = (JsonObject) msg.body();
            assertEquals(tt.getUuid(), bodyRec.getString("uuid"));
            ctx.completeNow();
        });
        vertx.eventBus().consumer("transfertask.error", msg -> {
            JsonObject bodyRec = (JsonObject) msg.body();
            ctx.failNow(new Exception(bodyRec.getString("message")));
        });

        TransferTaskRetryListener ta = getMockTransferRetryListenerInstance(vertx);

        //ta.processRetryTransferTask(body);
        ta.processRetryTransferTask(body, resp -> {
            if (resp.succeeded()) {
                System.out.println("Succeeded with the procdessTransferTask in retrying of the event ");
            } else {
                System.out.println("Error with return from retrying the event ");
            }
        });

        String protocolSelected = "http";

        assertEquals(StorageProtocolType.HTTP.name().toLowerCase(), protocolSelected.toLowerCase(), "Protocol used should have been " + StorageProtocolType.SFTP.name().toLowerCase());
        ctx.completeNow();
    }

    @Test
    @DisplayName("Process processTransferTaskPublishesChildTasksForDirectory")
    //@Disabled
    public void processTransferTaskPublishesChildTasksForDirectory(Vertx vertx, VertxTestContext ctx) throws Exception {

        TransferTask tt = _createTestTransferTask();

        TransferTaskRetryListener ta = getMockTransferRetryListenerInstance(vertx);
//        NatsJetstreamMessageClient nats = getMockNats();
        // mock out the db service so we can can isolate method logic rather than db
        TransferTaskDatabaseService dbService = getMockTranserTaskDatabaseService(tt.toJson());

        // mock a successful outcome from the call to processRetry
        AsyncResult<Boolean> processRetryHandler = getMockAsyncResult(true);
        // mock the handler passed into updateStatus
        doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
            @SuppressWarnings("unchecked")
            Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(1, Handler.class);
            handler.handle(processRetryHandler);
            return null;
        }).when(ta).processRetry(any(TransferTask.class), any());

        RemoteDataClient srcClient = mock(RemoteDataClient.class);
        RemoteDataClient destClient = mock(RemoteDataClient.class);

        // allow the first one to succeed since it's not an agave URI
        when(ta.getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getSource())))).thenReturn(srcClient);
            // force the second one to fail since it is an agave URI and can result in a bad syste lookup.
        when(ta.getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getDest()))))
            .thenThrow(new SystemUnknownException("THis should be thrown during the test and propagated back as the handler.cause() method."));

        ta.processRetryTransferTask(tt.toJson(), resp -> ctx.verify(() -> {
            assertFalse(resp.succeeded(), "processRetry should fail when system is unknown");
            verify(ta, atLeastOnce())._doPublishEvent(eq(MessageType.TRANSFERTASK_ERROR), any(JsonObject.class), any());

            ctx.completeNow();
        }));


        TransferTask tta = _createTestTransferTask();

        JsonObject body = tta.toJson();

        ta.processRetryTransferTask(body, resp -> {
            if (resp.succeeded()) {
                System.out.println("Succeeded with the procdessTransferTask in retrying of the event ");
            } else {
                System.out.println("Error with return from retrying the event ");
            }
        });
        String protocolSelected = "http";

//        assertEquals(StorageProtocolType.HTTP.name().toLowerCase(), protocolSelected.toLowerCase(), "Protocol used should have been " + StorageProtocolType.SFTP.name().toLowerCase());
        ctx.completeNow();
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

        doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
            @SuppressWarnings("unchecked")
            Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(3, Handler.class);
            handler.handle(getByAnyHandler);
            return null;
        }).when(dbService).update(any(), any(), any(), any());

        return dbService;
    }

    @Test
    @DisplayName("Process processTransferTaskPublishesErrorOnSystemUnavailble")
    public void processTransferTaskPublishesErrorOnSystemUnavailble(Vertx vertx, VertxTestContext ctx) throws Exception {
        TransferTask tt = _createTestTransferTask();
        tt.setParentTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
        tt.setRootTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());

        JsonObject body = tt.toJson();

        // mock out the db service so we can can isolate method logic rather than db
        TransferTaskDatabaseService dbService = getMockTranserTaskDatabaseService(tt.toJson());

        TransferTaskRetryListener ta = getMockTransferRetryListenerInstance(vertx);

        // mock a successful outcome with updated json transfer task result from getById call to db
        AsyncResult<JsonObject> getByAnyHandler = getMockAsyncResult(tt.toJson());
        doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
            @SuppressWarnings("unchecked")
            Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(3, Handler.class);
            handler.handle(getByAnyHandler);
            return null;
        }).when(dbService).update(any(), any(), any(), any());

        // mock the handler passed into getById
        doAnswer((Answer<AsyncResult<JsonObject>>) arguments -> {
            @SuppressWarnings("unchecked")
            Handler<AsyncResult<JsonObject>> handler = arguments.getArgumentAt(2, Handler.class);
            handler.handle(getByAnyHandler);
            return null;
        }).when(dbService).getByUuid(any(), any(), any());

        //ta.processRetryTransferTask(body);
        ta.processRetryTransferTask(body, resp -> {
            if (resp.succeeded()) {
                System.out.println("Succeeded with the procdessTransferTask in retrying of the event ");
            } else {
                System.out.println("Error with return from retrying the event ");
            }
        });

        String protocolSelected = "http";
        assertEquals(StorageProtocolType.HTTP.name().toLowerCase(), protocolSelected.toLowerCase(), "Protocol used should have been " + StorageProtocolType.SFTP.name().toLowerCase());
        ctx.completeNow();
    }

    @Test
    @DisplayName("TransferTaskRetryListenerTest - error event thrown on unknown dest system")
    //@Disabled
    public void processTransferTaskPublishesErrorOnSystemUnknown(Vertx vertx, VertxTestContext ctx) throws Exception {
//        NatsJetstreamMessageClient nats = getMockNats();
        TransferTask tt = _createTestTransferTask();
        tt.setParentTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
        tt.setRootTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());

        //RemoteDataClient srcClient = mock(RemoteDataClient.class);

        TransferTaskRetryListener ta = getMockTransferRetryListenerInstance(vertx);

        doCallRealMethod().when(ta).processRetry(any(), any());
        try {
            // allow the first one to succeed since it's not an agave URI
            when(ta.getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getSource()))))
                    .thenReturn(mock(RemoteDataClient.class));
            // force the second one to fail since it is an agave URI and can result in a bad syste lookup.
            when(ta.getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getDest()))))
                    .thenThrow(new SystemUnknownException("THis should be thrown during the test and propagated back as the handler.cause() method."));
        } catch (Exception e) {
            ctx.failNow(e);
        }

        ta.processRetry(tt, processRetryResult -> ctx.verify(() -> {
            assertFalse(processRetryResult.succeeded(), "processRetry should fail when system is unknown");
            verify(ta, atLeastOnce()).getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getSource())));
            verify(ta, atLeastOnce()).getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getDest())));
            verify(ta, atLeastOnce())._doPublishEvent( eq(MessageType.TRANSFERTASK_ERROR), any(JsonObject.class), any() );
            ctx.completeNow();
        }));
    }

    @Test
    @DisplayName("TransferRetryListenerTest - error event thrown on unknown source system")
    //@Disabled
    public void processTransferTaskPublishesErrorOnSrcSystemUnknown(Vertx vertx, VertxTestContext ctx) throws Exception {
//        NatsJetstreamMessageClient nats = getMockNats();
        TransferTask tt = _createTestTransferTask();
        tt.setParentTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
        tt.setRootTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
        RemoteDataClient destClient = mock(RemoteDataClient.class);

        TransferTaskRetryListener ta = getMockTransferRetryListenerInstance(vertx);

        doCallRealMethod().when(ta).processRetry(any(), any());

        try {
            // force the source one to fail since it is an agave URI and can result in a bad syste lookup.
            when(ta.getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getSource()))))
                    .thenThrow(new SystemUnknownException("This should be thrown during the test and propagated back as the handler.cause() method."));
            // allow the dest one to succeed since it's not an agave URI
            when(ta.getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getDest()))))
                    .thenReturn(destClient);
        } catch (Exception e) {
            ctx.failNow(e);
        }

        ta.processRetry(tt, processRetryResult -> ctx.verify(() -> {
            //assertTrue(processRetryResult.succeeded(), "processRetry should fail when system is unknown");
            //assertEquals(processRetryResult.cause().getClass(), SystemUnknownException.class, "processRetry should propagate SystemUnknownException back to handler when thrown.");
            verify(ta, times(1)).getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getSource())));
            verify(ta, never()).getRemoteDataClient(eq(tt.getTenantId()), eq(tt.getOwner()), eq(URI.create(tt.getDest())));
            //verify(ta, times(1))._doPublishEvent(  eq(TRANSFERTASK_ERROR), any(JsonObject.class));
//            verify(nats, times(1)).push(any(), any(), any(JsonObject.class).toString());
            ctx.completeNow();
        }));
    }

    @Test
    @DisplayName("TransferRetryListener - isTaskInterruptedTest")
   // @Disabled
    void isTaskInterrupted(Vertx vertx, VertxTestContext ctx) throws IOException, InterruptedException {
        TransferTask tt = _createTestTransferTask();
        tt.setParentTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
        tt.setRootTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());

        TransferTaskRetryListener ta = new TransferTaskRetryListener(vertx);

        // mock out the db service so we can can isolate method logic rather than db
        TransferTaskDatabaseService dbService = getMockTranserTaskDatabaseService(tt.toJson());


        //doNothing().when(ta).getRetryRequestManager().request(any(), any(), any());

        ctx.verify(() -> {
            ta.addCancelledTask(tt.getUuid());
            assertFalse(ta.taskIsNotInterrupted(tt), "UUID of tt present in cancelledTasks list should indicate task is interrupted");
            ta.removeCancelledTask(tt.getUuid());

            ta.addPausedTask(tt.getUuid());
            assertFalse(ta.taskIsNotInterrupted(tt), "UUID of tt present in pausedTasks list should indicate task is interrupted");
            ta.removePausedTask(tt.getUuid());

            ta.addCancelledTask(tt.getParentTaskId());
            assertFalse(ta.taskIsNotInterrupted(tt), "UUID of tt parent present in cancelledTasks list should indicate task is interrupted");
            ta.removeCancelledTask(tt.getParentTaskId());

            ta.addPausedTask(tt.getParentTaskId());
            assertFalse(ta.taskIsNotInterrupted(tt), "UUID of tt parent present in pausedTasks list should indicate task is interrupted");
            ta.removePausedTask(tt.getParentTaskId());

            ta.addCancelledTask(tt.getRootTaskId());
            assertFalse(ta.taskIsNotInterrupted(tt), "UUID of tt root present in cancelledTasks list should indicate task is interrupted");
            ta.removeCancelledTask(tt.getRootTaskId());

            ta.addPausedTask(tt.getRootTaskId());
            assertFalse(ta.taskIsNotInterrupted(tt), "UUID of tt root present in pausedTasks list should indicate task is interrupted");
            ta.removePausedTask(tt.getRootTaskId());

            ctx.completeNow();
        });
    }

    @Test
    @DisplayName("TransferRetryListener - Task retried for active or errored transfers")
    public void retryActiveTransfersTest(Vertx vertx, VertxTestContext ctx) throws MessagingException {
        ArrayList<TransferTask> tasks = new ArrayList<>();
//        NatsJetstreamMessageClient nats = getMockNats();

        for (TransferStatusType status : TransferStatusType.values()) {
            TransferTask tt = _createTestTransferTask();
            tt.setParentTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
            tt.setRootTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
            tt.setStatus(status);
            tasks.add(tt);
        }

        ctx.verify(()->{
            for (TransferTask tt : tasks) {
                TransferTaskRetryListener ta = getMockTransferRetryListenerInstance(vertx);

                // mock out the db service so we can can isolate method logic rather than db
                TransferTaskDatabaseService dbService = getMockTranserTaskDatabaseService(tt.toJson());

                when(ta.getDbService()).thenReturn(dbService);
                AsyncResult<Boolean> handlerResult = getMockAsyncResult(true);
                doAnswer((Answer<AsyncResult<Boolean>>) arguments -> {
                    @SuppressWarnings("unchecked")
                    Handler<AsyncResult<Boolean>> handler = arguments.getArgumentAt(1, Handler.class);
                    handler.handle(handlerResult);
                    return null;
                }).when(ta).processRetry(any(), any());

                JsonObject jsonTransferTask = tt.toJson();

                ta.processRetryTransferTask(jsonTransferTask, isRetried -> {
                    if (isRetried.succeeded()) {
                        if (tt.getStatus().isActive() || tt.getStatus().equals(TransferStatusType.ERROR)) {
                                //Active transfer tasks or recoverable errors should be retried
                                verify(ta, times(1)).processRetry(eq(tt), any());
                            } else {
                                //Inactive transfer tasks should not be retried"
                                verify(ta, never()).processRetry(any(), any());
                            }
                    } else {
                        ctx.failNow(isRetried.cause());
                    }
                });
            }
            ctx.completeNow();
        });
    }

    @Test
    @DisplayName("TransferRetryListener - Task failed whem max attempts reached")
    public void failTaskWhenMaxAttemptsReachedTest(Vertx vertx, VertxTestContext ctx) throws Exception {
//        NatsJetstreamMessageClient nats = getMockNats();

        TransferTask tt = _createTestTransferTask();
        tt.setParentTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
        tt.setRootTaskId(new AgaveUUID(UUIDType.TRANSFER).toString());
        tt.setStatus(TransferStatusType.ASSIGNED);
        tt.setAttempts(config.getInteger("TRANSFERTASK_MAX_ATTEMPTS") + 1);

        TransferTaskRetryListener ta = getMockTransferRetryListenerInstance(vertx);

        // mock out the db service so we can can isolate method logic rather than db
        TransferTaskDatabaseService dbService = getMockTranserTaskDatabaseService(tt.toJson());

        when(ta.getDbService()).thenReturn(dbService);
        //doNothing().when(ta)._doPublishEvent(any(), any());

        JsonObject jsonTransferTask = tt.toJson();

        ta.processRetryTransferTask(jsonTransferTask, isRetried -> {
            if (isRetried.succeeded()) {
                ctx.verify(() -> {
                    //verify(ta, times(1))._doPublishEvent(eq(TRANSFER_FAILED), any());
//                    verify(nats, times(1)).push(any(), any(), any(JsonObject.class).toString());
                    assertFalse(isRetried.result(), "TRANSFER_FAILED event should be sent when max attempts is reached.");
                    ctx.completeNow();
                });
            } else {
                ctx.failNow(isRetried.cause());
            }
        });
    }

}