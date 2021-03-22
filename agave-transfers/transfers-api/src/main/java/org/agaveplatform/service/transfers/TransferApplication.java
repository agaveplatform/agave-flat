package org.agaveplatform.service.transfers;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.agaveplatform.service.transfers.bridge.TcpBridge;
import org.agaveplatform.service.transfers.listener.*;
import org.agaveplatform.service.transfers.protocol.TransferAllProtocolVertical;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import org.agaveplatform.service.transfers.enumerations.MessageType;

import static org.agaveplatform.service.transfers.enumerations.MessageType.*;

public class TransferApplication {

    private static final Logger log = LoggerFactory.getLogger(TransferApplication.class);

    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        Vertx vertx = Vertx.vertx();

        int poolSize = 10;
        int instanceSize = 3;

        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setOptional(true)
                .setConfig(new JsonObject().put("path", "config.json"));

        ConfigStoreOptions envPropsStore = new ConfigStoreOptions().setType("env");

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore)
                .addStore(envPropsStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(json -> {
            if (json.succeeded()) {
                JsonObject config = json.result();
                log.debug("Starting the app with config: " + config.encodePrettily());

//                int tcpPort = 7000;
//                // start up the TcpBridge and set the Inbound and Outbound addresses
//                log.info("Starting up the Tcp bridge on port {}", tcpPort);
//                TcpBridge tcpBridge = new TcpBridge();
//                tcpBridge.setUp(vertx, tcpPort);
//                log.info("Done with starting up the bridge");

                DeploymentOptions dbDeploymentOptions = new DeploymentOptions()
                        .setConfig(config)
                        .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS)
                        .setMaxWorkerExecuteTime(500)
                        .setInstances(instanceSize);

                Promise<String> dbVerticleDeployment = Promise.promise();
                log.info("org.agaveplatform.service.transfers.database.TransferTaskDatabaseVerticle");
                vertx.deployVerticle("org.agaveplatform.service.transfers.database.TransferTaskDatabaseVerticle",
                        dbDeploymentOptions, dbVerticleDeployment);

                dbVerticleDeployment.future().compose(id -> {

                    Promise<String> httpVerticleDeployment = Promise.promise();
                    vertx.deployVerticle("org.agaveplatform.service.transfers.resources.TransferAPIVertical", new DeploymentOptions().setConfig(config), res -> {
                        if (res.succeeded()) {
                            log.info("TransferAPIVertical ({}) started on port {}", res.result(), config.getInteger("HTTP_PORT"));

                            DeploymentOptions localOptions = new DeploymentOptions()
//                                    .setWorkerPoolName("streaming-task-worker-pool")
//                                    .setWorkerPoolSize(poolSize)
//                                    .setInstances(instanceSize)
                                    .setConfig(config)
                                    .setWorker(false)
                                    .setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS)
                                    .setMaxWorkerExecuteTime(30);


//                            deployTTC(vertx, localOptions, resp -> {
//                                if (resp.succeeded()) {
//                                    log.info("TransferTaskCreatedListener Deployment id is " + resp.result());
//                                } else {
//                                    log.error("TransferTaskCreatedListener Deployment failed !");
//                                }
//                            });
                            // Deployment TransferTaskCreatedListener verticle
                            vertx.deployVerticle(TransferTaskCreatedListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskCreatedListener",
                                    localOptions, res1 -> {
                                        if (res1.succeeded()) {
                                            log.info("TransferTaskCreatedListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferTaskCreatedListener Deployment failed !");
                                        }
                                    });

                            //Deploy the TransferTaskAssignedListener vertical
                            vertx.deployVerticle(TransferTaskAssignedListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskAssignedListener",
                                    localOptions, res0 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferTaskAssignedListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferTaskAssignedListener Deployment failed !");
                                        }
                                    });

                            // Deploy the TransferRetryListener vertical
                            vertx.deployVerticle(TransferTaskRetryListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferRetryListener",
                                    localOptions, res3 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferRetryListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferRetryListener Deployment failed !");
                                        }
                                    });

                            DeploymentOptions workerOptions = new DeploymentOptions()
                                    .setWorkerPoolName("all-task-worker-pool")
                                    .setWorkerPoolSize(poolSize)
                                    .setInstances(instanceSize)
                                    .setConfig(config)
                                    .setWorker(true)
                                    .setMaxWorkerExecuteTimeUnit(TimeUnit.HOURS)
                                    .setMaxWorkerExecuteTime(12);

                            // Deploy the TransferAllProtocolVertical vertical
                            vertx.deployVerticle(TransferAllProtocolVertical.class.getName(), //"org.agaveplatform.service.transfers.protocol.TransferAllProtocolVertical",
                                    workerOptions, res4 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferAllProtocolVertical Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferAllProtocolVertical Deployment failed !");
                                        }
                                    });

                            // Deploy the TransferCompleteTaskListener vertical
                            vertx.deployVerticle(TransferTaskCompleteTaskListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferCompleteTaskListener",
                                    localOptions, res5 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferCompleteTaskListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferCompleteTaskListener Deployment failed !");
                                        }
                                    });


                            // Deploy the TransferErrorTaskListener vertical
                            vertx.deployVerticle(TransferTaskErrorListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferErrorTaskListener",
                                    localOptions, res6 -> {
                                        if (res.succeeded()) {
                                            log.info("ErrorTaskListener Deployment id is " + res.result());
                                        } else {
                                            log.info("ErrorTaskListener Deployment failed !");
                                        }
                                    });

                            // Deploy the TransferFailureHandler vertical
                            vertx.deployVerticle(TransferTaskErrorFailureHandler.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferFailureHandler",
                                    localOptions, res7 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferFailureHandler Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferFailureHandler Deployment failed !");
                                        }
                                    });

                            // Deploy the TransferTaskCancelListener vertical
                            vertx.deployVerticle(TransferTaskCancelListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskCancelListener",
                                    localOptions, res8 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferTaskCancelListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferTaskCancelListener Deployment failed !");
                                        }
                                    });

                            // Deploy the TransferTaskPausedListener vertical
                            vertx.deployVerticle(TransferTaskPausedListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskPausedListener",
                                    localOptions, res9 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferTaskPausedListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferTaskPausedListener Deployment failed !");
                                        }
                                    });

                            // Deploy the NotificationListener vertical
                            vertx.deployVerticle(TransferTaskNotificationListener.class.getName(), //"org.agaveplatform.service.transfers.listener.NotificationListener",
                                    localOptions, res10 -> {
                                        if (res.succeeded()) {
                                            log.info("NotificationListener Deployment id is " + res.result());
                                        } else {
                                            log.error("NotificationListener Deployment failed !");
                                        }
                                    });

                                    // Deploy the TransferErrorListener vertical
                                    vertx.deployVerticle("org.agaveplatform.service.transfers.listener.TransferErrorListener",
                                            localOptions, res11 -> {
                                                if (res.succeeded()){
                                                    System.out.println("TransferErrorListener Deployment id is " + res.result());
                                                }else{
                                                    System.out.println("TransferErrorListener Deployment failed !");
                                                }
                                            });

                            // Deploy the TransferHealthcheckListener vertical
                            vertx.deployVerticle(TransferTaskHealthcheckListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferHealthcheckListener",
                                    localOptions, res12 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferHealthcheckListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferHealthcheckListener Deployment failed !");
                                        }
                                    });
                            // Deploy the TransferHealthParentcheckListener vertical
                            vertx.deployVerticle(TransferTaskHealthcheckParentListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferHealthcheckListener",
                                    localOptions, res22 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferHealthcheckParentListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferHealthcheckParentListener Deployment failed !");
                                        }
                                    });
                            // Deploy the TransferWatchListener vertical
                            vertx.deployVerticle(TransferTaskWatchListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferWatchListener",
                                    localOptions, res13 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferWatchListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferWatchListener Deployment failed !");
                                        }
                                    });

                            // Deploy the TransferTaskUpdateListener vertical
                            vertx.deployVerticle(TransferTaskUpdateListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskUpdateListener",
                                    localOptions, res14 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferTaskUpdateListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferTaskUpdateListener Deployment failed !");
                                        }
                                    });

                            // Deploy the TransferTaskFinishedListener vertical
                            vertx.deployVerticle(TransferTaskFinishedListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskFinishedListener",
                                    localOptions, res15 -> {
                                        if (res.succeeded()) {
                                            log.info("TransferTaskFinishedListener Deployment id is " + res.result());
                                        } else {
                                            log.error("TransferTaskFinishedListener Deployment failed !");
                                        }
                                    });

                            // Deploy the Nats vertical
//                            vertx.deployVerticle(NatsListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskFinishedListener",
//                                    localOptions, res16 -> {
//                                        if (res.succeeded()) {
//                                            log.info("Nats Deployment id is " + res.result());
//                                        } else {
//                                            log.error("Nats Deployment failed !");
//                                        }
//                                    });
                        } else {
                            log.error("TransferAPIVertical deployment failed !\n" + res.cause());
                            res.cause().printStackTrace();
                        }
                    });

                    return httpVerticleDeployment.future();

                }).setHandler(ar -> {
                    if (ar.succeeded()) {
                        log.info("TransferApiVertical ({}) started on port {}", ar.result(), config.getInteger("HTTP_PORT"));
                    } else {
                        log.error("TransferApiVertical deployment failed !\n" + ar.cause());
                        ar.cause().printStackTrace();
                    }
                });
            } else {
                log.error("Error retrieving configuration.");
            }
        });
    }

//    protected static void deployTTC(Vertx vertx, DeploymentOptions localOptions, Handler<AsyncResult<Boolean>> handler ){
//    // Deployment TransferTaskCreatedListener verticle
//    log.info("got to deployTTC");
//        vertx.deployVerticle(TransferTaskCreatedListener.class.getName(), //"org.agaveplatform.service.transfers.listener.TransferTaskCreatedListener",
//        localOptions,res1 -> {
//            if (res1.succeeded()) {
//                log.info("TransferTaskCreatedListener Deployment id is " + res1.result());
//                handler.handle(Future.succeededFuture(true));
//            } else {
//                log.error("TransferTaskCreatedListener Deployment failed !");
//                handler.handle(Future.succeededFuture(false));
//            }
//        });
//    }
}
