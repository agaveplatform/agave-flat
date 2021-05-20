package org.agaveplatform.service.transfers.messaging;

import io.nats.client.*;
import io.nats.client.api.*;
import io.vertx.junit5.VertxExtension;
import org.agaveplatform.service.transfers.enumerations.MessageType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@DisplayName("NATS JetStream message client tests")
public class NatsJetstreamMessageClientIT {
    private static final Logger log = LoggerFactory.getLogger(NatsJetstreamMessageClientIT.class);

    public static String TEST_STREAM = "AGAVE_TRANSFERS_INTEGRATION_TESTS";
    public static String TEST_STREAM_SUBJECT = "test.transfers.>";
    public static String TEST_MESSAGE_SUBJECT_PREFIX = "test.transfers.agave_dev.";
    public static final String TEST_CLIENT_NAME = NatsJetstreamMessageClientIT.class.getSimpleName();
    public static final String NATS_URL = "nats://nats:4222";

    protected JetStreamSubscription nativeClientSubscription;
    protected Connection nc;
    protected JetStreamManagement jsm;

    @BeforeAll
    public void beforeAll() throws IOException, InterruptedException, JetStreamApiException {
        Options.Builder builder = new Options.Builder()
                .server(NATS_URL)
                .connectionTimeout(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(10))
                .reconnectWait(Duration.ofSeconds(1))
                .maxReconnects(-1)
                .connectionListener(new NatsConnectionListener())
                .errorListener(new NatsErrorListener());

        nc = Nats.connect(builder.build());
        jsm = nc.jetStreamManagement();
        try {
            StreamInfo si = jsm.getStreamInfo(TEST_STREAM);
            PurgeResponse pr = jsm.purgeStream(TEST_STREAM);
            if (!pr.isSuccess()) {
                fail("Failed to purge test queue prior to test run");
            }
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                StreamConfiguration streamConfiguration = StreamConfiguration.builder()
                        .name(TEST_STREAM)
                        .addSubjects(TEST_STREAM_SUBJECT)
                        .storageType(StorageType.Memory)
                        .build();

                jsm.addStream(streamConfiguration);
            }
        }

        PullSubscribeOptions pullSubscribeOptions = PullSubscribeOptions.builder()
                .stream(TEST_STREAM)
                .durable(getTestClientName())
                .configuration(ConsumerConfiguration.builder()
                        .ackPolicy(AckPolicy.Explicit)
                        .build())
                .build();
        nativeClientSubscription = nc.jetStream().subscribe(TEST_STREAM_SUBJECT, pullSubscribeOptions);
    }

    @AfterEach
    protected void afterEach() throws IOException, JetStreamApiException {
        if (jsm != null) {
            jsm.purgeStream(TEST_STREAM);
        }
    }

    @AfterAll
    protected void afterAll() throws IOException, JetStreamApiException {
        if (jsm != null) {
            jsm.purgeStream(TEST_STREAM);
            jsm.deleteConsumer(TEST_STREAM, getTestClientName());
        }
    }

    /**
     * Helper method to generate a durable clientname based on the test class.
     *
     * @return unique name based on the test client
     */
    private String getTestClientName() {
        return NatsJetstreamMessageClientIT.class.getSimpleName();
    }

    /**
     * Helper function to fetch and ack a single message from NATS using the native test client.
     *
     * @return the next message of any subject in the stream, or null if none exist
     */
    private Message getSingleNatsMessage() {
        List<io.nats.client.Message> messages = nativeClientSubscription.fetch(1, Duration.ofSeconds(1));

        if (messages.isEmpty()) {
            return null;
        } else {
            io.nats.client.Message msg = messages.get(0);
            msg.ack();
            return msg;
        }
    }

    @Test
    @DisplayName("Push message onto queue...")
    public void push() {
        NatsJetstreamMessageClient messageClient;
        String testBody = UUID.randomUUID().toString();
        io.nats.client.Message msg = null;

        try {
            messageClient = new NatsJetstreamMessageClient(NATS_URL,"DEV", "push-test-consumer");

            // test pushing of message for each message type
            for (String messageType : MessageType.values()) {
                String subject = TEST_MESSAGE_SUBJECT_PREFIX + messageType;
                messageClient.push(TEST_STREAM, subject, testBody);

                nc.flush(Duration.ofSeconds(1));
                msg = getSingleNatsMessage();

                assertNotNull(msg,
                        "No message found on test stream with the given subject after pushing message.");

                assertEquals(subject, msg.getSubject(),
                        "Subject of read message should match subject of message sent. " +
                                "Something else is likely writing test data");

                assertEquals(testBody, new String(msg.getData()),
                        "Received message should match sent message");

                try {
                    if (!jsm.deleteMessage(TEST_STREAM, Long.parseLong(msg.getSID()))) {
                        log.debug("Failed to delete test message after test");
                    }
                } catch (Exception e1) {
                    log.debug("Failed to delete test message after test", e1);
                }
            }
        } catch (Exception e) {
            fail("Failed to push a message to the message queue", e);
        }
    }

    @Test
    @DisplayName("Fetch message from queue...")
    public void fetch() {
        NatsJetstreamMessageClient agaveMessageClient;

        try {
            agaveMessageClient = new NatsJetstreamMessageClient(NATS_URL, "DEV", "fetch-test-consumer");

            // test pushing of message for each message type
            for (String messageType : MessageType.values()) {
                String testBody = UUID.randomUUID().toString();
                String subject = TEST_MESSAGE_SUBJECT_PREFIX + messageType;

                // push a message with a unique body to ensure we can get it back.
                agaveMessageClient.push(TEST_STREAM, subject, testBody);

                List<org.iplantc.service.common.messaging.Message> messages =
                        agaveMessageClient.fetch(TEST_STREAM, subject, 1, 2);

                assertFalse(messages.isEmpty(), "Test message pushed to stream should be fetched ");

                assertEquals(1, messages.size(),
                        "Exactly one message should be returned from test stream with when one is requested");

                assertEquals(testBody, messages.get(0).getMessage(),
                        "Body of fetched message should match body of message sent. " +
                                "Something else is likely writing test data");

                messages.forEach(msg -> {
                    try {
                        if (!jsm.deleteMessage(TEST_STREAM, (long)msg.getId())) {
                            log.debug("Failed to delete test message after test");
                        }
                    } catch (Exception e1) {
                        log.debug("Failed to delete test message after test", e1);
                    }
                });
//                break;
            }
        } catch (Exception e) {
            fail("Failed to push a message to the message queue", e);
        }
    }
//
//	@Test(dependsOnMethods={"pop"})
//	public void popMultiple()
//	{
//		BeanstalkClient messageClient = new BeanstalkClient();
//
//		try
//		{
//			String messageText = "abcd1234-abcd1234-abcd1234-abcd1234";
//			// push a message onto the exchange
//			nativeClient.useTube(TEST_EXCHANGE_TOPIC_QUEUE);
//			List<Long> notifs = new ArrayList<Long>();
//			for(int i=0;i<5;i++)
//			{
//				notifs.add(nativeClient.put(65536, 0, 120, messageText.getBytes()));
//			}
//
//			for(int i=0;i<5;i++)
//			{
//				Message poppedMessage = messageClient.pop(TEST_STREAM, TEST_EXCHANGE_TOPIC_QUEUE);
//
//				Assert.assertNotNull(poppedMessage, "No message popped from the queue.");
//				Assert.assertNotNull(poppedMessage.getId(), "No message id returned.");
//				Assert.assertEquals(notifs.get(i), (Long)poppedMessage.getId(), "Retrieved wrong message from queue");
//				Assert.assertEquals(poppedMessage.getMessage(), messageText);
//
//				messageClient.delete(TEST_STREAM, TEST_EXCHANGE_TOPIC_QUEUE, (Long)poppedMessage.getId());
//			}
//		}
//		catch (Throwable e)
//		{
//			Assert.fail("Failed to pop multiple messages", e);
//		}
//		finally {
//			messageClient.stop();
//		}
//	}
//
//
//	@Test(dependsOnMethods={"push"})
//	public void reject()
//	{
//		BeanstalkClient messageClient = new BeanstalkClient();
//		String message = "abcd1234-abcd1234-abcd1234-abcd1234";
//		try
//		{
//			// push a message onto the exchange
//			messageClient.push(TEST_NATS_JS_STREAM, TEST_EXCHANGE_TOPIC_QUEUE, message);
//
//			Message msg = messageClient.pop(TEST_STREAM, TEST_EXCHANGE_TOPIC_QUEUE);
//
//			Assert.assertEquals(msg.getMessage(), message, "Pushed and popped messages do not match");
//
//			messageClient.reject(TEST_STREAM, TEST_EXCHANGE_TOPIC_QUEUE, (Long)msg.getId(), message);
//
//			nativeClient.watch(TEST_EXCHANGE_TOPIC_QUEUE);
//			nativeClient.delete((Long)msg.getId());
//
//			Assert.assertNull(nativeClient.peek((Long)msg.getId()), "Job was not deleted");
//		}
//		catch (Exception e)
//		{
//			Assert.fail("Failed to release message back to queue", e);
//		}
//		finally {
//			messageClient.stop();
//		}
//	}
}
