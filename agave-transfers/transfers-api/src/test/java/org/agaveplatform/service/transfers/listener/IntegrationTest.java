package org.agaveplatform.service.transfers.listener;


import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.agaveplatform.service.transfers.BaseTestCase;
import org.agaveplatform.service.transfers.TransferApplication;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseService;
import org.agaveplatform.service.transfers.database.TransferTaskDatabaseVerticle;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.agaveplatform.service.transfers.enumerations.TransferStatusType;
import org.agaveplatform.service.transfers.exception.TransferException;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.agaveplatform.service.transfers.protocol.TransferAllProtocolVertical;
import org.agaveplatform.service.transfers.resources.TransferAPIVertical;
import org.agaveplatform.service.transfers.util.CryptoHelper;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.agaveplatform.service.transfers.enumerations.MessageType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@DisplayName("Transfers Task Integration tests")
@Disabled
public class IntegrationTest extends BaseTestCase {
	Logger log = LoggerFactory.getLogger(IntegrationTest.class);
	Vertx vertx = null;
	private JWTAuth jwtAuth;
	private static RequestSpecification requestSpecification;
	private TransferTaskDatabaseService dbService;
	List<String> messages = new ArrayList<String>();

	TransferErrorListener getMockErrListenerInstance(Vertx vertx) {
		TransferErrorListener listener = spy(new TransferErrorListener(vertx));
		when(listener.getEventChannel()).thenCallRealMethod();
		when(listener.getVertx()).thenReturn(vertx);
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(anyString(), any(JsonObject.class));
		return listener;
	}

	TransferFailureHandler getMockFailListenerInstance(Vertx vertx) {
		TransferFailureHandler listener = spy(new TransferFailureHandler(vertx));
		when(listener.getEventChannel()).thenCallRealMethod();
		when(listener.getVertx()).thenReturn(vertx);
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(anyString(), any(JsonObject.class));
		return listener;
	}

	InteruptEventListener getMockInteruptListenerInstance(Vertx vertx) {
		InteruptEventListener listener = spy(new InteruptEventListener(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	NotificationListener getMockNotificationListenerInstance(Vertx vertx) {
//		NotificationListener listener = spy(new NotificationListener(vertx));
		NotificationListener listener = Mockito.spy(new NotificationListener(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	TransferCompleteTaskListener getMockTCTListenerInstance(Vertx vertx) {
		TransferCompleteTaskListener listener = spy(new TransferCompleteTaskListener(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	TransferTaskAssignedListener getMockTTAListenerInstance(Vertx vertx) {
		TransferTaskAssignedListener listener = spy(new TransferTaskAssignedListener(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	TransferTaskCancelListener getMockTTCancelListenerInstance(Vertx vertx) {
		TransferTaskCancelListener listener = spy(new TransferTaskCancelListener(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	TransferTaskCreatedListener getMockTTCListenerInstance(Vertx vertx) {
		TransferTaskCreatedListener listener = spy(new TransferTaskCreatedListener(vertx));
		doReturn(config).when(listener).config();
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	TransferTaskPausedListener getMockTTPausedListenerInstance(Vertx vertx) {
		TransferTaskPausedListener listener = spy(new TransferTaskPausedListener(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	TransferAllProtocolVertical getMockAllProtocolVerticalInstance(Vertx vertx) {
		TransferAllProtocolVertical listener = spy(new TransferAllProtocolVertical(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}
	TransferAPIVertical getMockTransferAPIVerticalInstance(Vertx vertx) {
		TransferAPIVertical listener = spy(new TransferAPIVertical(vertx));
		when(listener.config()).thenReturn(config);
		doCallRealMethod().when(listener)._doPublishEvent(any(), any());
		return listener;
	}

	/**
	 * Generates a JWT token to authenticate to the service. Token is signed using the
	 * test private_key.pem and public_key.pem files in the resources directory.
	 *
	 * @param username Name of the test user
	 * @return signed jwt token
	 */
	private String makeJwtToken(String username) {
		// Add wso2 claims set
		JsonObject claims = new JsonObject()
				.put("http://wso2.org/claims/subscriber", username)
				.put("http://wso2.org/claims/applicationid", "-9999")
				.put("http://wso2.org/claims/applicationname", "agaveops")
				.put("http://wso2.org/claims/applicationtier", "Unlimited")
				.put("http://wso2.org/claims/apicontext", "/internal")
				.put("http://wso2.org/claims/version", Settings.SERVICE_VERSION)
				.put("http://wso2.org/claims/tier", "Unlimited")
				.put("http://wso2.org/claims/keytype", "PRODUCTION")
				.put("http://wso2.org/claims/usertype", "APPLICATION_USER")
				.put("http://wso2.org/claims/enduser", username)
				.put("http://wso2.org/claims/enduserTenantId", "-9999")
				.put("http://wso2.org/claims/emailaddress", "testuser@example.com")
				.put("http://wso2.org/claims/fullname", "Test User")
				.put("http://wso2.org/claims/givenname", "Test")
				.put("http://wso2.org/claims/lastname", "User")
				.put("http://wso2.org/claims/primaryChallengeQuestion", "N/A")
				.put("http://wso2.org/claims/role", "Internal/everyone,Internal/subscriber")
				.put("http://wso2.org/claims/title", "N/A");

		JWTOptions jwtOptions = new JWTOptions()
				.setAlgorithm("RS256")
				.setExpiresInMinutes(10_080) // 7 days
				.setIssuer("transfers-api-integration-tests")
				.setSubject(username);
		return jwtAuth.generateToken(claims, jwtOptions);
	}

	/**
	 * Initializes the jwt auth options and the
	 * @throws IOException when the key cannot be read
	 */
	private void initAuth(Vertx vertx) throws IOException {
		JWTAuthOptions jwtAuthOptions = new JWTAuthOptions()
				.addPubSecKey(new PubSecKeyOptions()
						.setAlgorithm("RS256")
						.setPublicKey(CryptoHelper.publicKey())
						.setSecretKey(CryptoHelper.privateKey()));

		jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
	}


	@BeforeAll
	public void beforeAll(Vertx vertx, VertxTestContext ctx) throws IOException {
		super.setUpService();
		DeploymentOptions options = new DeploymentOptions().setConfig(config);
		vertx.deployVerticle(TransferTaskDatabaseVerticle.class.getName(), options, dbId -> {
			log.debug("Completed deploying transfer task db verticles");
			vertx.deployVerticle(TransferAPIVertical.class.getName(), options, apiId -> {
				log.debug("Completed deploying transfer api verticles");
				vertx.deployVerticle(TransferTaskCreatedListener.class.getName(), options, createdId -> {
					log.debug("Completed deploying transfer task createdverticles");
					vertx.deployVerticle(TransferTaskAssignedListener.class.getName(), options, assignedId -> {
						log.debug("Completed deploying transfer task assigned verticles");
						vertx.deployVerticle(TransferAllProtocolVertical.class.getName(), options, httpId -> {
							log.debug("Completed deploying transfer all verticles");
							vertx.deployVerticle(TransferCompleteTaskListener.class.getName(), options, completedId -> {
								log.debug("Completed deploying transfer complete verticles");
								ctx.completeNow();
							});
						});
					});
				});
			});
		});
	}

	@BeforeEach
	protected void beforeEach(Vertx vertx, VertxTestContext ctx) {

		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.QUEUED);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));

		RequestSpecification requestSpecification = new RequestSpecBuilder()
				.setBaseUri("http://localhost:" + port + "/")
				.build();

		Checkpoint requestCheckpoint = ctx.checkpoint();
		ctx.verify(() -> {
			String response = given()
					.spec(requestSpecification)
					.contentType(ContentType.JSON)
					.body(parentTask.toJSON())
					.when()
					.post("api/transfers")
					.then()
					.assertThat()
					.statusCode(201)
					.extract()
					.asString();

			JsonObject createdTransferTask = new JsonObject(response);
			assertThat(createdTransferTask).isNotNull();
			requestCheckpoint.flag();
			ctx.completeNow();
		});
	}


	@Test
	@DisplayName("")
	@Disabled
	protected void test_integrationTest(Vertx vertx,  VertxTestContext ctx) {

		// Set up our transfertask for testing
		TransferTask parentTask = _createTestTransferTask();
		parentTask.setStatus(TransferStatusType.QUEUED);
		parentTask.setStartTime(Instant.now());
		parentTask.setEndTime(Instant.now());
		parentTask.setSource(TRANSFER_SRC.substring(0, TRANSFER_SRC.lastIndexOf("/") - 1));

		TransferTask transferTask = _createTestTransferTask();
		transferTask.setStatus(TransferStatusType.QUEUED);
		transferTask.setStartTime(Instant.now());
		transferTask.setEndTime(Instant.now());
		transferTask.setRootTaskId(parentTask.getUuid());
		transferTask.setParentTaskId(parentTask.getUuid());


		// mock out the verticle we're testing so we can observe that its methods were called as expected
		TransferTaskCreatedListener transferTaskCreatedListener = getMockTTCListenerInstance(vertx);
		TransferTaskAssignedListener transferTaskAssignedListener = getMockTTAListenerInstance(vertx);
		TransferTaskCancelListener transferTaskCancelListener = getMockTTCancelListenerInstance(vertx);
		TransferAllProtocolVertical transferAllProtocolVertical = getMockAllProtocolVerticalInstance(vertx);
		TransferCompleteTaskListener transferCompleteTaskListener = getMockTCTListenerInstance(vertx);
		TransferErrorListener errorTaskListener = getMockErrListenerInstance(vertx);
		InteruptEventListener interuptEventListener = getMockInteruptListenerInstance(vertx);
		NotificationListener notificationListener = getMockNotificationListenerInstance(vertx);
		TransferTaskPausedListener transferTaskPausedListener = getMockTTPausedListenerInstance(vertx);
		TransferAPIVertical transferAPIVertical = getMockTransferAPIVerticalInstance(vertx);

		Checkpoint dbDeploymentCheckpoint = ctx.checkpoint();
		Checkpoint apiDeploymentCheckpoint = ctx.checkpoint();
		Checkpoint createdDeploymentCheckpoint = ctx.checkpoint();
		Checkpoint assignedDeploymentCheckpoint = ctx.checkpoint();
		Checkpoint httpDeploymentCheckpoint = ctx.checkpoint();
		Checkpoint requestCheckpoint = ctx.checkpoint();
		Checkpoint completedDeploymentCheckpoint = ctx.checkpoint();
		Checkpoint apiListenerCheckpoint = ctx.checkpoint();
		Checkpoint createdListenerCheckpoint = ctx.checkpoint();
		Checkpoint assignedListenerCheckpoint = ctx.checkpoint();
		Checkpoint transferAllListenerCheckpoint = ctx.checkpoint();
		Checkpoint transferCompletedListenerCheckpoint = ctx.checkpoint();
		Checkpoint transferTaskCompletedListenerCheckpoint = ctx.checkpoint();

		DeploymentOptions options = new DeploymentOptions().setConfig(config);
		vertx.deployVerticle(TransferTaskDatabaseVerticle.class.getName(), options, ctx.succeeding(dbId -> {
			dbDeploymentCheckpoint.flag();

			vertx.deployVerticle(transferAPIVertical, options, ctx.succeeding(apiId -> {
				apiDeploymentCheckpoint.flag();

				vertx.deployVerticle(transferTaskCreatedListener, options, ctx.succeeding(createdId -> {
					createdDeploymentCheckpoint.flag();

					vertx.deployVerticle(transferTaskAssignedListener, options, ctx.succeeding(assignedId -> {
						assignedDeploymentCheckpoint.flag();

						vertx.deployVerticle(transferAllProtocolVertical, options, ctx.succeeding(httpId -> {
							httpDeploymentCheckpoint.flag();

							vertx.deployVerticle(transferCompleteTaskListener, options, ctx.succeeding(completedId -> {
								completedDeploymentCheckpoint.flag();

								RequestSpecification requestSpecification = new RequestSpecBuilder()
										.setBaseUri("http://localhost:" + port + "/")
										.build();

								ctx.verify(() -> {
									String response = given()
											.spec(requestSpecification)
											.contentType(ContentType.JSON)
											.body(parentTask.toJSON())
											.when()
											.post("api/transfers")
											.then()
											.assertThat()
											.statusCode(201)
											.extract()
											.asString();

									JsonObject createdTransferTask = new JsonObject(response);
									assertThat(createdTransferTask).isNotNull();
									requestCheckpoint.flag();
									ctx.verify(() -> {
										vertx.eventBus().addOutboundInterceptor(dc -> {
											String address = dc.message().address();
											if (address.equals(TRANSFERTASK_CREATED)) {
												createdListenerCheckpoint.flag();
											}
											dc.next();
										});
									});
								});
							}));
						}));
					}));
				}));
			}));
		}));
	}

		//log.debug("Publishing {} event: {}", "TRANSFERTASK_CREATED", transferTask.toJson());
		//getVertx().eventBus().publish(MessageType.TRANSFERTASK_CREATED, transferTask.toJson());


		//verify(ttc)._doPublishEvent(TRANSFERTASK_ASSIGNED, transferTask.toJson());



}
