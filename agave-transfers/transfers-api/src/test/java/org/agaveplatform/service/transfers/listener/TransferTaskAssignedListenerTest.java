package org.agaveplatform.service.transfers.listener;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.agaveplatform.service.transfers.model.TransferTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("👋 TransferTaskAssignedListener test")
@ExtendWith(VertxExtension.class)
class TransferTaskAssignedListenerTest {

	private EventBus eventBus;

	@Test
	void taskAssigned(Vertx vertx, VertxTestContext ctx){
		eventBus = vertx.eventBus();
		vertx.deployVerticle(new TransferTaskAssignedListener(), ctx.succeeding(id -> {
			JsonObject body = new JsonObject();
			body.put("id", "1");  // uuid
			body.put("owner", "dooley");
			body.put("tenantId", "agave.dev");
			body.put("protocol","sftp");

			eventBus.consumer("transfertask.created", msg -> {
				JsonObject bodyRec = (JsonObject) msg.body();
				assertEquals("1", bodyRec.getString("id"));
				assertEquals("dooley", bodyRec.getString("owner"));
				assertEquals("agave.dev", bodyRec.getString("tenantId"));
				assertEquals("sftp", bodyRec.getString("protocol"));

			});

			eventBus.publish("transfertask.assigned.agave.dev.sftp.*.*", body );

		}));
	}

}