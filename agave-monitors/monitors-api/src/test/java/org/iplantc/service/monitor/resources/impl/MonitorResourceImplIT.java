package org.iplantc.service.monitor.resources.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.monitor.AbstractMonitorIT;
import org.iplantc.service.monitor.MonitorApplication;
import org.iplantc.service.monitor.ServletJaxRsApplication;
import org.iplantc.service.monitor.TestDataHelper;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.Monitor;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Form;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import org.testng.Assert;
import org.testng.annotations.*;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.restlet.data.Status.SUCCESS_OK;

@Test(groups={"integration"})
public class MonitorResourceImplIT extends AbstractMonitorIT
{
	private static Logger log = Logger.getLogger(MonitorResourceImplIT.class);
	private Component comp = new Component();
	private ServletJaxRsApplication application = null;
	private MonitorDao dao = new MonitorDao();
	private Client client = new Client(new Context(), Protocol.HTTP);
	private String testJWT;
	
	private void initRestletServer() throws Exception
	{
		// create Component (as ever for Restlet)
        Server server = comp.getServers().add(Protocol.HTTP, 8182);

		server.getContext().getParameters().add("maxTotalConnections", "512");

        // create JAX-RS runtime environment
		application = new ServletJaxRsApplication(comp.getContext());

		// Attach the application to the component and start it
        comp.getDefaultHost().attach(application);
        comp.start();
	}
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		initRestletServer();
		super.beforeClass();
	}
	
	@AfterMethod
	public void afterMethod() throws Exception
	{
		clearMonitors();
		clearNotifications();
		clearQueues();
	}
	
	@AfterClass
	public void afterClass() throws Exception
	{
		super.afterClass();

		// shut down all quartz schedulers so the server can complete shutdown
		for (Scheduler scheduler: application.getSchedulerFactory().getAllSchedulers()) {
			try {
				log.error("Stopping quartz scheduler " + scheduler.getSchedulerName() + "...");
				scheduler.shutdown();
				log.error("Quartz scheduler " + scheduler.getSchedulerName() + " stopped");
			} catch (Throwable ignored) {
				try {
					log.error("Failed to shutdown quartz scheduler " + scheduler.getSchedulerName());
				} catch (SchedulerException e) {
					log.error("Failed to shutdown unnamed quartz scheduler");
				}
			}
		}
		comp.stop();
	}

	/**
	 * Parses standard response stanza for the actual body and code
	 * 
	 * @param representation
	 * @param shouldSucceed
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	private JsonNode verifyResponse(Representation representation, boolean shouldSucceed) 
	throws JSONException, IOException 
	{
		String responseBody = representation.getText();
		
		Assert.assertNotNull(responseBody, "Null body returned");
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(responseBody);
		if (shouldSucceed) {
			Assert.assertEquals(json.get("status").asText().toLowerCase(), "success", "Error when success should have occurred");
		} else {
			Assert.assertEquals(json.get("status").asText().toLowerCase(), "error", "Success when error should have occurred");
		}
		
		return json.get("result");
	}
	
	/**
	 * Creates a client to call the api given by the url. This will reuse the connection
	 * multiple times rather than starting up a new client every time.
	 *
	 * @param url the url to point the client towards
	 * @return
	 */
	private ClientResource getService(String url) throws TenantException {
		String jwt = JWTClient.createJwtForTenantUser(TEST_USER, "agave.dev", false);
		ClientResource resource = new ClientResource(url);
		resource.setReferrerRef("http://test.example.com");
		resource.getRequest().getHeaders().add("X-JWT-ASSERTION-AGAVE_DEV", jwt);

		return resource;
	}

	@DataProvider(name="addMonitorProvider")
	public Object[][] addMonitorProvider() throws Exception
	{
		ObjectNode jsonExecutionMonitorNoSystem = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoFrequency = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoUpdateSystemStatus = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoInternalUsername = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		jsonExecutionMonitorNoSystem.remove("target");
		jsonExecutionMonitorNoFrequency.remove("frequency");
		jsonExecutionMonitorNoUpdateSystemStatus.remove("updateSystemStatus");
		jsonExecutionMonitorNoInternalUsername.remove("internalUsername");
		
		return new Object[][] {
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR), "Valid monitor json should parse", false },
			{ jsonExecutionMonitorNoSystem, "Missing system should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", ""), "Empty target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createObjectNode()), "Object for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createArrayNode()), "Array for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5), "Integer for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5.5F), "Decimal for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicStorageSystem.getSystemId()), "Public storage system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicExecutionSystem.getSystemId()), "Public execution system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateExecutionSystem.getSystemId()), "Private execution system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateStorageSystem.getSystemId()), "Private storage system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", sharedExecutionSystem.getSystemId()), "Shared execution system should not throw an exception", false },
			

			{ jsonExecutionMonitorNoFrequency, "Missing frequency should succeed and default to 720", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", ""), "Empty frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createObjectNode()), "Object for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createArrayNode()), "Array for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5.5F), "Decimal for frequency should succeed and round decimal", false },
			
			{ jsonExecutionMonitorNoUpdateSystemStatus, "Missing updateSystemStatus should succeed and default to false", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", ""), "Empty updateSystemStatus should succeed and evaluate to false", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createObjectNode()), "Object for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createArrayNode()), "Array for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5), "Integer for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5.5F), "Decimal for updateSystemStatus should throw exception", true },
			
			{ jsonExecutionMonitorNoInternalUsername, "Missing internalUsername should succeed and default to null", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", ""), "Empty internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createObjectNode()), "Object for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createArrayNode()), "Array for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5), "Integer for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5.5F), "Decimal for internalUsername should throw exception", true },
			
		};
	}
	
	@Test(dataProvider="addMonitorProvider")
	public void addSingleMonitor(JsonNode body, String errorMessage, boolean shouldThrowException)
	{
		JsonNode json = null;
		Representation response = null;
		try
		{
			ClientResource resource = getService("http://localhost:8182/");
			response = resource.post(new JsonRepresentation(body.toString()));
			Assert.assertFalse(shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			
			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No monitor uuid given");
		}
		catch (ResourceException e) {
 			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

	@DataProvider(name="addMonitorFromFormProvider")
	public Object[][] addMonitorFromFormProvider() throws Exception
	{
		ObjectNode jsonExecutionMonitorNoSystem = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoFrequency = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoUpdateSystemStatus = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoInternalUsername = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		jsonExecutionMonitorNoSystem.remove("target");
		jsonExecutionMonitorNoFrequency.remove("frequency");
		jsonExecutionMonitorNoUpdateSystemStatus.remove("updateSystemStatus");
		jsonExecutionMonitorNoInternalUsername.remove("internalUsername");

		return new Object[][] {
				{ dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR), "Valid monitor json should parse", false },
				{ jsonExecutionMonitorNoSystem, "Missing system should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", ""), "Empty target should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5), "Integer for target should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5.5F), "Decimal for target should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicStorageSystem.getSystemId()), "Public storage system should not throw an exception", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicExecutionSystem.getSystemId()), "Public execution system should not throw an exception", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateExecutionSystem.getSystemId()), "Private execution system should not throw an exception", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateStorageSystem.getSystemId()), "Private storage system should not throw an exception", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", sharedExecutionSystem.getSystemId()), "Shared execution system should not throw an exception", false },


				{ jsonExecutionMonitorNoFrequency, "Missing frequency should succeed and default to 720", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", ""), "Empty frequency should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5.5F), "Decimal for frequency should succeed and round decimal", false },

				{ jsonExecutionMonitorNoUpdateSystemStatus, "Missing updateSystemStatus should succeed and default to false", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", ""), "Empty updateSystemStatus should succeed and evaluate to false", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5), "Integer for updateSystemStatus should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5.5F), "Decimal for updateSystemStatus should throw exception", true },

				{ jsonExecutionMonitorNoInternalUsername, "Missing internalUsername should succeed and default to null", false },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", ""), "Empty internalUsername should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5), "Integer for internalUsername should throw exception", true },
				{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5.5F), "Decimal for internalUsername should throw exception", true },

		};
	}
	
	@Test(dataProvider="addMonitorFromFormProvider")
	public void addMonitorFromForm(JsonNode body, String errorMessage, boolean shouldThrowException)
	{

		JsonNode json = null;
		try 
		{
			ClientResource resource = getService("http://localhost:8182/");

			Form form = new Form();
			if (body != null) 
			{
				if (body.has("target")) 
				{
					form.add("target", body.get("target").asText());
				}
				
				if (body.has("frequency")) 
				{
					form.add("frequency", body.get("frequency").asText());
				}
				
				if (body.has("updateSystemStatus")) 
				{
					form.add("updateSystemStatus", body.get("updateSystemStatus").asText());
				}
				
				if (body.has("internalUsername")) 
				{
					form.add("internalUsername", body.get("internalUsername").asText());
				}
			}
			
			Representation response = resource.post(form.getWebRepresentation());
			Assert.assertFalse(shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			
			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No monitor uuid given");
		}
		catch (ResourceException e) {
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		} catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

	@DataProvider(name="deleteMonitorProvider")
	public Object[][] deleteMonitorProvider() throws Exception
	{
		Monitor validMonitor = createStorageMonitor();
		dao.persist(validMonitor);
		
		Monitor otherUserMonitor = createExecutionMonitor();
		otherUserMonitor.setSystem(sharedExecutionSystem);
		otherUserMonitor.setOwner(TestDataHelper.SYSTEM_SHARE_USER);
		dao.persist(otherUserMonitor);
		
		return new Object[][] {
			{ validMonitor.getUuid(), "Deleting valid storage monitor should succeed", false },
			{ "", "Empty uuid should fail", true },
			{ "abcd", "Invalid uuid should fail", true },
			{ otherUserMonitor.getUuid(), "Deleting unowned monitor should fail", true },
		};
	}
	
	@Test(dataProvider="deleteMonitorProvider")
	public void deleteMonitor(String uuid, String errorMessage, boolean shouldThrowException)
	{
		JsonNode json = null;
		try 
		{
			ClientResource resource = getService("http://localhost:8182/" + uuid);

			Representation response = resource.delete();
			Assert.assertFalse(shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			
			Assert.assertNull(json, "Message results attribute was not null");
		}
		catch (ResourceException e) {
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

//	@Test(dataProvider="deleteMonitorProvider")
//	public void fireMonitor(String uuid, String errorMessage, boolean shouldThrowException)
//	{
//		ClientResource resource = getService("http://localhost:8182//" + uuid);
//
//		JsonNode json = null;
//		try
//		{
//			Representation response = resource.delete();
//
//			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
//			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON,
//					"Expected json media type returned, instead received " + response.getMediaType());
//
//			json = verifyResponse(response, shouldThrowException);
//			Assert.assertFalse(shouldThrowException, "Response failed when it should not have");
//
//		}
//		catch (Exception e) {
//			Assert.fail("Unexpected exception thrown", e);
//		}
//	}
//
	@Test(expectedExceptions = ResourceException.class)
	public void getMonitorFailsOnInvalidId() throws ResourceException
	{
		ClientResource resource = null;
		try {
			resource = getService("http://localhost:8182/abcd");
		} catch (TenantException e) {
			Assert.fail("Tenant lookup should not fail", e);
		}
		Representation response = resource.get();
	}

	@Test(expectedExceptions = ResourceException.class)
	public void getMonitorFailsOnForbiddenMonitor()
	{
		JsonNode json = null;
		try
		{
			Monitor otherUserMonitor = createExecutionMonitor();
			otherUserMonitor.setSystem(sharedExecutionSystem);
			otherUserMonitor.setOwner(TestDataHelper.SYSTEM_SHARE_USER);
			dao.persist(otherUserMonitor);

			ClientResource resource = getService("http://localhost:8182/" + otherUserMonitor.getUuid());
			Representation response = resource.get();
		} catch (TenantException | MonitorException | JSONException | IOException e) {
			Assert.fail("Test monitor creation should not fail.", e);
		}
	}

	@Test
	public void getMonitorReturnsActiveMonitor()
	{
		JsonNode json = null;
		try
		{
			Monitor monitor = createStorageMonitor();
			monitor.setSystem(privateStorageSystem);
			dao.persist(monitor);

			ClientResource resource = getService("http://localhost:8182/" + monitor.getUuid());
			Representation response = resource.get();

			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON,
					"Expected json media type returned, instead received " + response.getMediaType());

			json = verifyResponse(response, true);

			Assert.assertEquals(json.get("id").asText().toLowerCase(), monitor.getUuid(),
					"Incorrect monitor returned. UUID of returned monitor should match the requested UUID");
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

	@Test
	public void getMonitorReturnsInActiveMonitor()
	{
		JsonNode json = null;
		try
		{
			Monitor monitor = createStorageMonitor();
			monitor.setSystem(privateStorageSystem);
			monitor.setActive(false);
			dao.persist(monitor);

			ClientResource resource = getService("http://localhost:8182/" + monitor.getUuid());
			Representation response = resource.get();

			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON,
					"Expected json media type returned, instead received " + response.getMediaType());

			json = verifyResponse(response, true);

			Assert.assertEquals(json.get("id").asText().toLowerCase(), monitor.getUuid(),
					"Incorrect monitor returned. UUID of returned monitor should match the requested UUID");
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

	@Test
	public void getMonitors()
	{
		JsonNode json = null;
		try
		{
			ClientResource resource = getService("http://localhost:8182/");

			Monitor m1 = createStorageMonitor();
			dao.persist(m1);
			
			Monitor m2 = createExecutionMonitor();
			dao.persist(m2);
			
			Representation response = resource.get();
			
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			Assert.assertTrue(json instanceof ArrayNode, "Service returned object rather than array");
			Assert.assertEquals(json.size(), 2, "Invalid number of monitors returned.");
			
		}
		catch (Exception e) {
			Assert.fail("Listing monitors should not throw exception.", e);
		}
	}

	@Test
	public void searchMonitors()
	{
		JsonNode json = null;
		try
		{
			ClientResource resource = getService("http://localhost:8182/?target.eq=sftp.example.com");

			Monitor m1 = createStorageMonitor();
			dao.persist(m1);

			Monitor m2 = createExecutionMonitor();
			dao.persist(m2);

			Representation response = resource.get();

			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON,
					"Expected json media type returned, instead received " + response.getMediaType());

			json = verifyResponse(response, true);
			Assert.assertTrue(json instanceof ArrayNode, "Service returned object rather than array");
			Assert.assertEquals(json.size(), 1, "Invalid number of monitors returned.");
			Assert.assertEquals(((ArrayNode)json).get(0).get("id").asText(), m1.getUuid(),
					"Search by target should return all monitors for that target for the given user.");
		}
		catch (Exception e) {
			Assert.fail("Searching monitors should not throw exception.", e);
		}
	}
}
