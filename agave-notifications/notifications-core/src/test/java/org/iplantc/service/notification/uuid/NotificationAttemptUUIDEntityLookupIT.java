package org.iplantc.service.notification.uuid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AbstractUUIDTest;
import org.iplantc.service.common.uuid.UUIDEntityLookup;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.FailedNotificationAttemptQueue;
import org.iplantc.service.notification.dao.NotificationAttemptDao;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

@Test(groups={"integration"})
public class NotificationAttemptUUIDEntityLookupIT implements AbstractUUIDTest<NotificationAttempt> {
    UUIDEntityLookup uuidEntityLookup = new UUIDEntityLookup();
    AbstractNotificationTest it;

    @BeforeClass
    protected void beforeClass() throws Exception {
        it = new AbstractNotificationTest();
    }

    @AfterClass
    public void afterClass() throws NotificationException {
        it.clearNotifications();
    }

    /**
     * Get the type of the entity. This is the type of UUID to test.
     *
     * @return
     */
    @Override
    public UUIDType getEntityType() {
        return UUIDType.NOTIFICATION_DELIVERY;
    }

    /**
     * Create a test entity persisted and available for lookup.
     *
     * @return
     */
    @Override
    public NotificationAttempt createEntity() {
        NotificationAttempt notificationAttempt = null;
        try {
            Notification notification = it.createEmailNotification();
            new NotificationDao().persist(notification);
            notificationAttempt = new NotificationAttempt(notification.getUuid(), notification.getCallbackUrl(),
                    notification.getOwner(), notification.getAssociatedUuid(),
                    notification.getEvent(), "This is a test", Timestamp.from(Instant.now()));
            notificationAttempt.setAssociatedUuid(notification.getAssociatedUuid());
            FailedNotificationAttemptQueue.getInstance().push(notificationAttempt);
        } catch (Exception e) {
            Assert.fail("Unable to create notification delivery attempt", e);
        }

        return notificationAttempt;
    }

    /**
     * Serialize the entity into a JSON string. This should call out
     * to a {@link Notification#toJSON()} method in the entity or delegate to
     * Jackson Annotations.
     *
     * @param testEntity entity to serialize
     * @return
     */
    public String serializeEntityToJSON(NotificationAttempt testEntity) {
        String json = null;
        try {
            json = testEntity.toJSON();
        } catch (NotificationException e) {
            Assert.fail("Failed to serialized notification delivery attempt", e);
        }

        return json;
    }

    /**
     * Get the uuid of the entity. This should call out to the {@link NotificationAttempt#getUuid()}
     * method in the entity.
     *
     * @param testEntity
     * @return
     */
    @Override
    public String getEntityUuid(NotificationAttempt testEntity) {
        return testEntity.getUuid();
    }

    @Test
    public void testGetResourceUrl() {
        NotificationAttempt testEntity = null;
        try {
            testEntity = createEntity();
            String resolvedUrl = UUIDEntityLookup
                    .getResourceUrl(getEntityType(), getEntityUuid(testEntity));
            String resolvedTenantUrl = TenancyHelper.resolveURLToCurrentTenant(resolvedUrl);
            String hypermediaUrl = getUrlFromEntityJson(serializeEntityToJSON(testEntity));
            Assert.assertEquals(
                    resolvedTenantUrl,
                    hypermediaUrl,
                    "Resolved "
                            + getEntityType().name().toLowerCase()
                            + " urls should match those created by the entity class itself.");
        } catch (UUIDException | IOException e) {
            Assert.fail("Failed ot resolve UUID: " + e.getMessage(), e);
        }
        finally {
            try {
                if (testEntity != null) {
                    FailedNotificationAttemptQueue.getInstance().getMongoClient()
                            .getDatabase(Settings.FAILED_NOTIFICATION_DB_SCHEME)
                            .getCollection(testEntity.getNotificationId())
                            .drop();
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Extracts the HAL from a json representation of an object and returns the
     * _links.self.href value.
     *
     * @param entityJson the serialized json object
     * @return self link of the object json representation
     * @throws JsonProcessingException if the HAL structure is missing from the json string
     * @throws IOException if the JSON representation is unreadable
     */
    protected String getUrlFromEntityJson(String entityJson)
            throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readTree(entityJson).get("_links").get("self")
                .get("href").asText();
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme