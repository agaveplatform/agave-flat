package org.agaveplatform.service.transfers.database;

import com.google.common.base.CaseFormat;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.agaveplatform.service.transfers.BaseTestCase;
import org.agaveplatform.service.transfers.enumerations.TransferStatusType;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.agaveplatform.service.transfers.TransferTaskConfigProperties.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@DisplayName("TransferTask Database Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Disabled
public class TransferTaskDatabaseVerticleTest extends BaseTestCase {

    private TransferTaskDatabaseService service;

    @BeforeAll
    @Override
    public void setUpService(Vertx vertx, VertxTestContext ctx) throws IOException {
        Checkpoint authCheckpoint = ctx.checkpoint();
        Checkpoint deployCheckpoint = ctx.checkpoint();
        Checkpoint serviceCheckpoint = ctx.checkpoint();

        // init the jwt auth used in the api calls
        initAuth(vertx, reply -> {
            authCheckpoint.flag();
            if (reply.succeeded()) {
                jwtAuth = reply.result();

                DeploymentOptions options = new DeploymentOptions()
                        .setConfig(config)
                        .setWorker(true)
                        .setMaxWorkerExecuteTime(3600);

                vertx.deployVerticle(new TransferTaskDatabaseVerticle(), options, ctx.succeeding(id -> {
                    deployCheckpoint.flag();
                    service = TransferTaskDatabaseService.createProxy(vertx, config.getString(CONFIG_TRANSFERTASK_DB_QUEUE));

                    ctx.verify(() -> {
                        serviceCheckpoint.flag();
                        assertNotNull(jwtAuth);
                        assertNotNull(config);
                        ctx.completeNow();
                    });
                }));
            } else {
                ctx.failNow(new Exception("Failed to initialize jwtAuth"));
            }
        });
    }

    @Test
    @DisplayName("TransferTaskDatabaseVerticle - transfer task crud operations")
//    @Disabled
    public void crudTest(Vertx vertx, VertxTestContext context) {
        TransferTask testTransferTask = _createTestTransferTask();

        service.create(TENANT_ID, testTransferTask, context.succeeding(createdJsonTransferTask -> {
            assertNotNull(createdJsonTransferTask.getLong("id"), "Object returned from create should have not null id");
            assertNotNull(createdJsonTransferTask.getInstant("created"), "Object returned from create should have not null created");
            assertNotNull(createdJsonTransferTask.getInstant("last_updated"), "Object returned from create should have not null last_updated");
            assertEquals(TENANT_ID, createdJsonTransferTask.getString("tenant_id"),"Object returned from create should have same tenant_id as sent");
            assertEquals(testTransferTask.getUuid(), createdJsonTransferTask.getString("uuid"),"Object returned from create should have same uuid as sent");
            assertEquals(testTransferTask.getSource(), createdJsonTransferTask.getString("source"),"Object returned from create should have same source value as sent");
            assertEquals(testTransferTask.getDest(), createdJsonTransferTask.getString("dest"),"Object returned from create should have same dest value as sent");

            service.getById(TENANT_ID, testTransferTask.getUuid(), context.succeeding(getByIdJsonTransferTask -> {
                assertEquals(TENANT_ID, getByIdJsonTransferTask.getString("tenant_id"),"Object returned from create should have same tenant_id as original");
                assertEquals(testTransferTask.getUuid(), getByIdJsonTransferTask.getString("uuid"),"Object returned from create should have same uuid as original");
                assertEquals(testTransferTask.getSource(), getByIdJsonTransferTask.getString("source"),"Object returned from create should have same source value as original");
                assertEquals(testTransferTask.getDest(), getByIdJsonTransferTask.getString("dest"),"Object returned from create should have same dest value as original");

                service.updateStatus(TENANT_ID, testTransferTask.getUuid(), TransferStatusType.TRANSFERRING.name(), context.succeeding(updateStatusJsonTransferTask -> {
                    assertEquals(TransferStatusType.TRANSFERRING.name(), updateStatusJsonTransferTask.getString("status"),
                            "Object returned from updateStatus should have updated status");
                    assertTrue(getByIdJsonTransferTask.getInstant("last_updated").isBefore(updateStatusJsonTransferTask.getInstant("last_updated")),
                            "Object returned from udpate have last_updated value more recent than original");
                    assertEquals(createdJsonTransferTask.getLong("id"), updateStatusJsonTransferTask.getLong("id"),
                            "Object returned from create should have same id as original");

                    service.getAll(TENANT_ID, 100, 0, context.succeeding(getAllJsonTransferTaskArray -> {
                        assertEquals(1, getAllJsonTransferTaskArray.size(),
                                "getAll should only have a single record after updating status of an existing object." +
                                        "New object was likely added to the db.");

                        JsonObject updatedJsonObject = new JsonObject()
                                .put("attempts", 1) // attempts
                                .put("bytesTransferred", (long)Math.pow(2, 19)) // getBytesTransferred())
//                                .add("created should not update") // getCreated())
//                                .add("dest should not update") // getDest())
                                .put("endTime", Instant.now()) // getEndTime())
//                                .addNull() // getEventId())
//                                .add(Instant.now()) // getLastUpdated())
//                                .add("owner should not update") // getOwner())
//                                .add("source should not update") // getSource())
                                .put("startTime", Instant.now()) // getStartTime())
                                .put("status", TransferStatusType.PAUSED.name()) // getStatus())
//                                .add("tenant_id should not update")
                                .put("totalSize", (long)Math.pow(2, 20)) // getTotalSize())
                                .put("transferRate", 9999.0) // getTransferRate())
//                                .addNull() // getParentTaskId())
//                                .addNull() // getRootTaskId())
//                                .add("this should not change") // getUuid())
                                .put("totalFiles", 10L) // getTotalFiles())
                                .put("totalSkippedFiles", 9L); // getTotalSkippedFiles());

                        TransferTask updatedTransferTask = new TransferTask(updatedJsonObject);

                        service.update(TENANT_ID, testTransferTask.getUuid(), updatedTransferTask, context.succeeding(updateJsonTransferTask -> {

                            service.getAll(TENANT_ID, 100, 0, context.succeeding(getAllJsonTransferTaskArray2 -> {
                                assertEquals(1, getAllJsonTransferTaskArray2.size(),
                                        "getAll should only have a single record after updating an existing object." +
                                                "New object was likely added to the db.");
                                assertEquals(updateJsonTransferTask.getString("uuid"), getAllJsonTransferTaskArray2.getJsonObject(0).getString("uuid"),
                                        "Object returned by getAll should have the same uuid as the updated object.");


                                service.getById(TENANT_ID, testTransferTask.getUuid(), context.succeeding(getByIdJsonTransferTask2 -> {

                                    assertEquals(TENANT_ID, getByIdJsonTransferTask2.getString("tenant_id"),"Object returned from create should have same tenant_id as original");
                                    assertEquals(createdJsonTransferTask.getLong("id"), getByIdJsonTransferTask2.getLong("id"),"Object returned from create should have same id as original");
                                    assertEquals(testTransferTask.getUuid(), getByIdJsonTransferTask2.getString("uuid"),"Object returned from create should have same uuid as original");
                                    assertEquals(testTransferTask.getSource(), getByIdJsonTransferTask2.getString("source"),"Object returned from create should have same source value as original");
                                    assertEquals(testTransferTask.getDest(), getByIdJsonTransferTask2.getString("dest"),"Object returned from create should have same dest value as original");
                                    //Assertions.assertTrue(updateStatusJsonTransferTask.getInstant("last_updated").isBefore(getByIdJsonTransferTask2.getInstant("last_updated")),"Object returned from udpate have last_updated value more recent than original");
                                    // verify the updated values are present in the response
                                    updatedJsonObject
                                            .stream()
                                            .forEach(entry -> {
                                                if (entry.getKey().equalsIgnoreCase("startTime") || entry.getKey().equalsIgnoreCase("endTime")) {
                                                    assertEquals(Instant.parse(((String)entry.getValue())).getEpochSecond(), getByIdJsonTransferTask2.getInstant(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey())).getEpochSecond(),
                                                            entry.getKey() + " should be updated in the response from the service");
                                                } else {
                                                    assertEquals(entry.getValue(), getByIdJsonTransferTask2.getValue(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey())),
                                                            entry.getKey() + " should be updated in the response from the service" );
                                                }
                                            });

//                                    Assertions.assertEquals(TENANT_ID, getByIdJsonTransferTask2.getString("tenant_id"),"Object returned from create should have same tenant_id as original");
//                                    Assertions.assertEquals(createdJsonTransferTask.getLong("id"), getByIdJsonTransferTask2.getLong("id"),"Object returned from create should have same id as original");
//                                    Assertions.assertEquals(testTransferTask.getUuid(), getByIdJsonTransferTask2.getString("uuid"),"Object returned from create should have same uuid as original");
//                                    Assertions.assertEquals(testTransferTask.getSource(), getByIdJsonTransferTask2.getString("source"),"Object returned from create should have same source value as original");
//                                    Assertions.assertEquals(testTransferTask.getDest(), getByIdJsonTransferTask2.getString("dest"),"Object returned from create should have same dest value as original");


                                    service.delete(TENANT_ID, testTransferTask.getUuid(), context.succeeding(v3 -> {

                                        service.getAll(TENANT_ID, 100, 0, context.succeeding( getAllJsonTransferTaskArray3 -> {
                                            assertTrue(getAllJsonTransferTaskArray3.isEmpty(), "No records should be returned from getAll after deleting the test object");
                                            context.completeNow();
                                        }));
                                    }));
                                }));
                            }));
                        }));
                    }));
                }));
            }));
        }));
    }

    @Test
    @DisplayName("TransferTaskDatabaseVerticle - find child task returns correct record")
//    @Disabled
    public void findChildTransferTaskTest(Vertx vertx, VertxTestContext context) {

        initTestTransferTaskTree(testTreeReply -> {
            if (testTreeReply.succeeded()) {
                TransferTask rootTask = testTreeReply.result().get(0);
                TransferTask parentTask = testTreeReply.result().get(1);
                TransferTask child1Task = testTreeReply.result().get(2);
                TransferTask child2Task = testTreeReply.result().get(3);

                List<TransferTask> testTasks = List.of(
                        child1Task,
                        child2Task);

                Future<JsonObject> child1Future = futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource(), child1Task.getDest());
                Future<JsonObject> child2Future = futureFindTransferTask(child2Task.getTenantId(), child2Task.getRootTaskId(), child2Task.getSource(), child2Task.getDest());

                CompositeFuture.all(child1Future, child2Future).setHandler(rh -> {
                    if (rh.succeeded()) {
                        CompositeFuture composite = rh.result();
                        context.verify(() -> {
                            // iterate over the completed futures adding the result from the db call into our saved tasks array
                            for (int i = 0; i < composite.size(); i++) {

                                assertNotNull(composite.resultAt(i), "Child task " + i + " should not be null");
                                assertEquals(testTasks.get(i).getUuid(), ((JsonObject)composite.resultAt(i)).getString("uuid"),
                                        "Child returned from the db should match the test child transfer task.");
                            }
                            context.completeNow();
                        });
                    } else {
                        // fail now. this didn't work
                        context.failNow(rh.cause());
                    }
                });

            } else {
                context.failNow(testTreeReply.cause());
            }
        });
    }

    @Test
    @DisplayName("TransferTaskDatabaseVerticle - find child task returns null when no match")
    public void findChildTransferTaskTestReturnsNullOnNoMatch(Vertx vertx, VertxTestContext context) {

        initTestTransferTaskTree(testTreeReply -> {
            if (testTreeReply.succeeded()) {
                TransferTask child1Task = testTreeReply.result().get(2);
                List<Future> testFutures = new ArrayList<>();

                String[] prefixes = new String[]{" ", "  "};//, "prefix-",};
                String[] suffixes = new String[]{"-suffix"};

                for (String prefix: prefixes) {
                    for (String suffix: suffixes) {
                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), prefix + child1Task.getSource(), child1Task.getDest()));
                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource() + suffix, child1Task.getDest()));
                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), prefix + child1Task.getSource() + suffix, child1Task.getDest()));

                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource(), prefix + child1Task.getDest()));
                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource(), child1Task.getDest() + suffix));
                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource(), prefix + child1Task.getDest() + suffix));

                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), prefix + child1Task.getSource(), prefix + child1Task.getDest()));
                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource() + suffix, child1Task.getDest() + suffix));
                        testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), prefix + child1Task.getSource() + suffix, prefix + child1Task.getDest() + suffix));
                    }
                }

                testFutures.add(futureFindTransferTask(null, child1Task.getRootTaskId(), child1Task.getSource(), child1Task.getDest()));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), null, child1Task.getSource(), child1Task.getDest()));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), null, child1Task.getDest()));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource(), null));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), null, null));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), null, null, null));
                testFutures.add(futureFindTransferTask(null, null, null, null));

                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource().substring(0, child1Task.getSource().length()-4), child1Task.getDest()));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), UUID.randomUUID().toString(), child1Task.getDest()));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), child1Task.getSource(), UUID.randomUUID().toString()));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), child1Task.getRootTaskId(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
                testFutures.add(futureFindTransferTask(child1Task.getTenantId(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
                testFutures.add(futureFindTransferTask(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));

                CompositeFuture.all(testFutures).setHandler(rh -> {
                    if (rh.succeeded()) {
                        CompositeFuture composite = rh.result();
                        context.verify(() -> {
                            // iterate over the completed futures adding the result from the db call into our saved tasks array
                            for (int i = 0; i < composite.size(); i++) {
                                assertNull(composite.resultAt(i), "Child task " + i + " should be null");
                            }
                            context.completeNow();
                        });
                    } else {
                        // fail now. this didn't work
                        context.failNow(rh.cause());
                    }
                });

            } else {
                context.failNow(testTreeReply.cause());
            }
        });
    }

    @Test
    @DisplayName("TransferTaskDatabaseVerticle - createOrUpdateChildTransferTask creates a new child task when none exists")
    public void createOrUpdateChildTransferTaskCreatesNewTask(Vertx vertx, VertxTestContext context) {
        service.create(TENANT_ID, _createTestTransferTask(), createReply -> {
            if (createReply.succeeded()) {
                final TransferTask rootTask = new TransferTask(createReply.result());
                // random paths won't be present on any records in the db, even if we somehow didn't wipe all the data
                // between tests
                final TransferTask childTask = _createChildTestTransferTask(rootTask);

                service.createOrUpdateChildTransferTask(rootTask.getTenantId(), childTask, reply -> {
                    context.verify(() -> {
                      assertTrue(reply.succeeded(), "create or update should have succeeded for valid file");
                      assertNotNull(reply.result(), "create or update should return JsonObject representing new/udpated record.");
                      assertNotNull(reply.result().getValue("id"), "returned record should have a valid id.");
                      assertEquals(childTask.getUuid(), reply.result().getString("uuid"), "create or update should return JsonObject with same UUID as the provided task when not present in the db");
                      assertEquals(rootTask.getUuid(), reply.result().getString("root_task"), "create or update should return JsonObject with same root task id as the provided task");
                      context.completeNow();
                    });
                });
            } else {
                context.failNow(createReply.cause());
            }
        });
    }

    @Test
    @DisplayName("TransferTaskDatabaseVerticle - createOrUpdateChildTransferTask updates existing child task when none exists")
    public void createOrUpdateChildTransferTaskUpdatesExistingTask(Vertx vertx, VertxTestContext context) {
        initTestTransferTaskTree(testTreeReply -> {
            if (testTreeReply.succeeded()) {
                TransferTask rootTask = testTreeReply.result().get(0);
                TransferTask child1Task = testTreeReply.result().get(2);

                // random paths won't be present on any records in the db, even if we somehow didn't wipe all the data
                // between tests
                final TransferTask childTask = _createChildTestTransferTask(rootTask);
                childTask.setStatus(TransferStatusType.TRANSFERRING);
                childTask.setSource(child1Task.getSource());
                childTask.setDest(child1Task.getDest());

                service.createOrUpdateChildTransferTask(rootTask.getTenantId(), childTask, reply -> {
                    context.verify(() -> {
                        assertTrue(reply.succeeded(), "create or update should have succeeded for valid file");
                        assertNotNull(reply.result(), "create or update should return JsonObject representing new/udpated record.");
                        assertNotNull(reply.result().getValue("id"), "returned record should have a valid id.");
                        assertEquals(childTask.getStatus().name(), reply.result().getString("status"), "create or update should return JsonObject with same status as the provided task when the task already exists");
                        assertEquals(child1Task.getUuid(), reply.result().getString("uuid"), "create or update should return JsonObject with same UUID as the provided task when the task already exists");
                        assertEquals(rootTask.getUuid(), reply.result().getString("root_task"), "create or update should return JsonObject with same root task when the task already exists");
                        context.completeNow();
                    });
                });
            } else {
                context.failNow(testTreeReply.cause());
            }
        });
    }


    /**
     * Generates a tree of {@link TransferTask} with root, parent, and 2 child tasks. The resolved value will be a list
     * of the persisted transfer tasks to use in tests.
     * @param handler the callback to pass the list of persisted {@link TransferTask}
     */
    public void initTestTransferTaskTree(Handler<AsyncResult<List<TransferTask>>> handler) {
        final TransferTask rootTransferTask = _createTestTransferTask();
        rootTransferTask.setSource("agave://source/" + UUID.randomUUID().toString());
        rootTransferTask.setDest("agave://dest/" + UUID.randomUUID().toString());
        rootTransferTask.setStatus(TransferStatusType.QUEUED);

        // create the task
        final JsonObject[] testTT = new JsonObject[4];
        service.create(rootTransferTask.getTenantId(), rootTransferTask, rootReply -> {
            if (rootReply.failed()) {
                handler.handle(Future.failedFuture(rootReply.cause()));
            } else {
                // saved root transfer task
                final TransferTask rootTask = new TransferTask(rootReply.result());

                service.create(rootTransferTask.getTenantId(), _createChildTestTransferTask(rootTask), parentReply -> {
                    if (parentReply.failed()) {
                        handler.handle(Future.failedFuture(parentReply.cause()));
                    } else {
                        // parent transfer task, child to rootTransferTask
                        final TransferTask parentTask = new TransferTask(parentReply.result());

                        service.create(rootTransferTask.getTenantId(), _createChildTestTransferTask(parentTask), child1Reply -> {
                            if (child1Reply.failed()) {
                                handler.handle(Future.failedFuture(child1Reply.cause()));
                            } else {
                                // leaf transfer task, child to parentTask, task under test
                                final TransferTask child1Task = new TransferTask(child1Reply.result());

                                service.create(rootTransferTask.getTenantId(), _createChildTestTransferTask(parentTask), child2Reply -> {
                                    if (child2Reply.failed()) {
                                        handler.handle(Future.failedFuture(child2Reply.cause()));
                                    } else {
                                        // leaf transfer task, child to parentTask, task under test
                                        final TransferTask child2Task = new TransferTask(child2Reply.result());
                                        handler.handle(Future.succeededFuture(List.of(rootTask, parentTask, child1Task, child2Task)));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Generates a TransferTask configured with the {@code parentTransferTask} as the parent. The parent's root task id is
     * set as the child's root unless it is null, in which case, the parent is set as the child's root. Random
     * source and dest subpaths are generated for the client based on the source and dest of the parent.
     * @param parentTransferTask the parent {@code TransferTask} of the child
     * @return a new {@link TransferTask} instance initialized wiht the given parent.
     */
    private TransferTask _createChildTestTransferTask(TransferTask parentTransferTask) {
        TransferTask child = _createTestTransferTask();

        if (parentTransferTask != null) {
            child.setTenantId(parentTransferTask.getTenantId());
            child.setParentTaskId(parentTransferTask.getUuid());
            child.setRootTaskId(parentTransferTask.getRootTaskId() == null ? parentTransferTask.getUuid() : parentTransferTask.getRootTaskId());
            child.setSource(parentTransferTask.getSource() + "/" + UUID.randomUUID().toString());
            child.setDest(parentTransferTask.getDest() + "/" + UUID.randomUUID().toString());
            child.setOwner(parentTransferTask.getOwner());
            child.setStatus(parentTransferTask.getStatus());
        }

        return child;

    }

    /**
     * Generates a TransferTask configured with the {@code parentTransferTask} as the parent. The parent's root task id is
     * set as the child's root unless it is null, in which case, the parent is set as the child's root. Random
     * source and dest subpaths are generated for the client based on the source and dest of the parent.
     * @param parentTransferTask the parent {@code TransferTask} serialized as {@link JsonObject} of the child
     * @return a new {@link TransferTask} instance initialized wiht the given parent.
     */
    private TransferTask _createChildTestTransferTask(JsonObject parentTransferTask) {
        return _createChildTestTransferTask(new TransferTask(parentTransferTask));
    }

    /**
     * Async creates {@code count} transfer tasks by calling {@link #addTransferTask(TransferTask)} using a {@link CompositeFuture}.
     * The saved tasks are returned as a {@link JsonArray} to the callback once complete
     * @param count number of tasks to create
     * @param handler the callback with the saved tasks
     */
    protected void addTransferTasks(int count, TransferTask parentTask, Handler<AsyncResult<JsonArray>> handler) {
        JsonArray savedTasks = new JsonArray();

        // generate a future for each task to save
        List<Future> futureTasks = new ArrayList<>();
        for (int i=0; i<count; i++) {
            futureTasks.add(addTransferTask(parentTask));
        }
        // collect them all into a composite future so we wait until they are all complete.
        CompositeFuture.all(futureTasks).setHandler(rh -> {
            if (rh.succeeded()) {
                CompositeFuture composite = rh.result();
                // iterate over the completed futures adding the result from the db call into our saved tasks array
                for (int index = 0; index < composite.size(); index++) {
                    if (composite.succeeded(index) && composite.resultAt(index) != null) {
                        savedTasks.add(composite.<JsonObject>resultAt(index));
                    }
                }
                // resolve the callback handler with the saved tasks
                handler.handle(Future.succeededFuture(savedTasks));
            } else {
                // fail now. this didn't work
                handler.handle(Future.failedFuture(rh.cause()));
            }
        });
    }

    /**
     * Creates a promise that resolves when a new {@link TransferTask} is inserted into the db.
     * @return a future that resolves the saved task.
     */
    protected Future<JsonObject> addTransferTask(TransferTask parentTransferTask) {

        Promise<JsonObject> promise = Promise.promise();

        TransferTask tt = _createChildTestTransferTask(parentTransferTask);

        service.create(tt.getTenantId(), tt, resp -> {
            if (resp.failed()) {
                promise.fail(resp.cause());
            } else {
                promise.complete(resp.result());
            }
        });
        return promise.future();
    }

    /**
     * Creates a promise that resolves when a new {@link TransferTask} is fetched from calling
     * {@link TransferTaskDatabaseService#findChildTransferTask)}
     * @param tenantId the tenant
     * @param rootTaskId the uuid of the root task of the child to search for
     * @param src the source path of the child to search for
     * @param dest the dest path of the child to search for
     * @return a future that resolves the search results.
     */
    protected Future<JsonObject> futureFindTransferTask(String tenantId, String rootTaskId, String src, String dest) {

        Promise<JsonObject> promise = Promise.promise();

        service.findChildTransferTask(tenantId, rootTaskId, src, dest, fetch -> {
            if (fetch.failed()) {
                promise.fail(fetch.cause());
            } else {
                promise.complete(fetch.result());
            }
        });
        return promise.future();
    }

}