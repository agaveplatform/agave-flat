package org.iplantc.service.systems.model;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;

@Test(groups={"unit"})
public class BatchQueueTest extends SystemsModelTestCommon{

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();
    }
    
    @BeforeMethod
	public void setUpMethod() throws Exception {
		jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE)
				.getJSONArray("queues").getJSONObject(0);
	}
    
    @DataProvider(name = "batchQueueName")
    public Object[][] batchQueueName() {
    	return new Object[][] {
    			{ "name", null, "name cannot be null", true },
    			{ "name", "", "name cannot be empty", true },
    			{ "name", new Object(), "name cannot be object", true },
    			{ "name", Collections.singletonList("Harry"), "name cannot be array", true },
    			{ "name", "test name", "name cannot contain spaces", true },
    			{ "name", "test~name", "name cannot contain ~ characters", true },
    			{ "name", "test`name", "name cannot contain ` characters", true },
    			{ "name", "test!name", "name cannot contain ! characters", true },
    			{ "name", "test@name", "name cannot contain @ characters", true },
    			{ "name", "test#name", "name cannot contain # characters", true },
    			{ "name", "test$name", "name cannot contain $ characters", true },
    			{ "name", "test%name", "name cannot contain % characters", true },
    			{ "name", "test^name", "name cannot contain ^ characters", true },
    			{ "name", "test&name", "name cannot contain & characters", true },
    			{ "name", "test*name", "name cannot contain * characters", true },
    			{ "name", "test(name", "name cannot contain ( characters", true },
    			{ "name", "test)name", "name cannot contain ) characters", true },
    			{ "name", "test_name", "name cannot contain _ characters", true },
    			{ "name", "test+name", "name cannot contain + characters", true },
    			{ "name", "test=name", "name cannot contain = characters", true },
    			{ "name", "test{name", "name cannot contain { characters", true },
    			{ "name", "test}name", "name cannot contain } characters", true },
    			{ "name", "test|name", "name cannot contain | characters", true },
    			{ "name", "test\\name", "name cannot contain \\ characters", true },
    			{ "name", "test\nname", "name cannot contain carrage return characters", true },
    			{ "name", "test\tname", "name cannot contain tab characters", true },
    			{ "name", "test:name", "name cannot contain : characters", true },
    			{ "name", "test;name", "name cannot contain ; characters", true },
    			{ "name", "test'name", "name cannot contain ' characters", true },
    			{ "name", "test\"name", "name cannot contain \" characters", true },
    			{ "name", "test,name", "name cannot contain , characters", true },
    			{ "name", "test?name", "name cannot contain ? characters", true },
    			{ "name", "test/name", "name cannot contain / characters", true },
    			{ "name", "test<name", "name cannot contain < characters", true },
    			{ "name", "test>name", "name cannot contain > characters", true },
    			{ "name", "test-name", "name should allow dashes", false },
    			{ "name", "test.name", "name should allow periods", false },
    			{ "name", "testname", "name should allow all chars", false },
    			{ "name", "1234567890", "name should allow all numbers", false },
    			{ "name", "test0name", "name should allow alpha", false },
    			
    			{ "mappedName", null, "mappedName can be null", false },
    			{ "mappedName", "", "mappedName can be empty", false },
    			{ "mappedName", new Object(), "mappedName cannot be object", true },
    			{ "mappedName", Collections.singletonList("Harry"), "mappedName cannot be array", true },
    			{ "mappedName", "test mappedName", "mappedName cannot contain spaces", true },
    			{ "mappedName", "test~mappedName", "mappedName cannot contain ~ characters", true },
    			{ "mappedName", "test`mappedName", "mappedName cannot contain ` characters", true },
    			{ "mappedName", "test!mappedName", "mappedName cannot contain ! characters", true },
    			{ "mappedName", "test@mappedName", "mappedName cannot contain @ characters", true },
    			{ "mappedName", "test#mappedName", "mappedName cannot contain # characters", true },
    			{ "mappedName", "test$mappedName", "mappedName cannot contain $ characters", true },
    			{ "mappedName", "test%mappedName", "mappedName cannot contain % characters", true },
    			{ "mappedName", "test^mappedName", "mappedName cannot contain ^ characters", true },
    			{ "mappedName", "test&mappedName", "mappedName cannot contain & characters", true },
    			{ "mappedName", "test*mappedName", "mappedName cannot contain * characters", true },
    			{ "mappedName", "test(mappedName", "mappedName cannot contain ( characters", true },
    			{ "mappedName", "test)mappedName", "mappedName cannot contain ) characters", true },
    			{ "mappedName", "test_mappedName", "mappedName cannot contain _ characters", true },
    			{ "mappedName", "test+mappedName", "mappedName cannot contain + characters", true },
    			{ "mappedName", "test=mappedName", "mappedName cannot contain = characters", true },
    			{ "mappedName", "test{mappedName", "mappedName cannot contain { characters", true },
    			{ "mappedName", "test}mappedName", "mappedName cannot contain } characters", true },
    			{ "mappedName", "test|mappedName", "mappedName cannot contain | characters", true },
    			{ "mappedName", "test\\mappedName", "mappedName cannot contain \\ characters", true },
    			{ "mappedName", "test\nmappedName", "mappedName cannot contain carrage return characters", true },
    			{ "mappedName", "test\tmappedName", "mappedName cannot contain tab characters", true },
    			{ "mappedName", "test:mappedName", "mappedName cannot contain : characters", true },
    			{ "mappedName", "test;mappedName", "mappedName cannot contain ; characters", true },
    			{ "mappedName", "test'mappedName", "mappedName cannot contain ' characters", true },
    			{ "mappedName", "test\"mappedName", "mappedName cannot contain \" characters", true },
    			{ "mappedName", "test,mappedName", "mappedName cannot contain , characters", true },
    			{ "mappedName", "test?mappedName", "mappedName cannot contain ? characters", true },
    			{ "mappedName", "test/mappedName", "mappedName cannot contain / characters", true },
    			{ "mappedName", "test<mappedName", "mappedName cannot contain < characters", true },
    			{ "mappedName", "test>mappedName", "mappedName cannot contain > characters", true },
    			{ "mappedName", "test-mappedName", "mappedName should allow dashes", false },
    			{ "mappedName", "test.mappedName", "mappedName should allow periods", false },
    			{ "mappedName", "testmappedName", "mappedName should allow all chars", false },
    			{ "mappedName", "1234567890", "mappedName should allow all numbers", false },
    			{ "mappedName", "test0mappedName", "mappedName should allow alpha", false },
    			
    			{ "description", null, "description can be null", false },
    			{ "description", "", "description can be empty", false },
    			{ "description", new Object(), "description cannot be object", true },
    			{ "description", Collections.singletonList("Harry"), "description cannot be array", true },
    			{ "description", "aasdfas12e23445", "description can be alpha", false },
    			{ "description", "@$#*%!&^@$(^)(&_*)_Q#R[];'\\/\"><", "description can contain non alpha characters", false },

    			{ "maxJobs", "", "maxJobs cannot be empty string", true },
    			{ "maxJobs", "Harry", "maxJobs cannot be string", true },
    			{ "maxJobs", new Object(), "maxJobs cannot be object", true },
    			{ "maxJobs", Collections.singletonList("Harry"), "maxJobs cannot be array", true },
    			{ "maxJobs", 22.0f, "maxJobs is rounded when a floating point number", false },
    			{ "maxJobs", null, "maxJobs can be null", false },
    			{ "maxJobs", -1, "maxJobs can be -1", false },
    			{ "maxJobs", 0, "maxJobs cannot be zero", true },
    			{ "maxJobs", -2, "maxJobs can be negative other than -1", true },
    			{ "maxJobs", 22, "maxJobs can be an integer", false },
    			
    			{ "maxUserJobs", "", "maxUserJobs cannot be empty string", true },
    			{ "maxUserJobs", "Harry", "maxUserJobs cannot be string", true },
    			{ "maxUserJobs", new Object(), "maxUserJobs cannot be object", true },
    			{ "maxUserJobs", Collections.singletonList("Harry"), "maxUserJobs cannot be array", true },
    			{ "maxUserJobs", 22.0f, "maxUserJobs is rounded when a floating point number", false },
    			{ "maxUserJobs", null, "maxUserJobs can be null", false },
    			{ "maxUserJobs", -1, "maxUserJobs can be -1", false },
    			{ "maxUserJobs", 0, "maxUserJobs cannot be zero", true },
    			{ "maxUserJobs", -2, "maxUserJobs can be negative other than -1", true },
    			{ "maxUserJobs", 22, "maxUserJobs can be an integer", false },
    			
    			{ "maxMemoryPerNode", null, "maxMemoryPerNode can be null", false },
    			{ "maxMemoryPerNode", "", "maxMemoryPerNode cannot be empty string", true },
    			{ "maxMemoryPerNode", new Object(), "maxMemoryPerNode cannot be object", true },
    			{ "maxMemoryPerNode", Collections.singletonList("Harry"), "maxMemoryPerNode cannot be array", true },
    			{ "maxMemoryPerNode", "2KB", "maxMemoryPerNode cannot specify KB", true },
    			{ "maxMemoryPerNode", "2MB", "maxMemoryPerNode cannot specify MB", true },
    			{ "maxMemoryPerNode", "2GB", "maxMemoryPerNode can specify GB", false },
    			{ "maxMemoryPerNode", "2gb", "maxMemoryPerNode can specify gb", false },
    			{ "maxMemoryPerNode", "-2GB", "maxMemoryPerNode cannot be negative", true },
    			{ "maxMemoryPerNode", "2TB", "maxMemoryPerNode can specify TB", false },
    			{ "maxMemoryPerNode", "2tb", "maxMemoryPerNode can specify tb", false },
    			{ "maxMemoryPerNode", "2PB", "maxMemoryPerNode can specify PB", false },
    			{ "maxMemoryPerNode", "2pb", "maxMemoryPerNode can specify pb", false },
    			{ "maxMemoryPerNode", "2 GB", "maxMemoryPerNode ignores spaces", false },
    			{ "maxMemoryPerNode", 1024L, "maxMemoryPerNode can be an integer", false },
    			{ "maxMemoryPerNode", 22.1f, "maxMemoryPerNode can be a decimal", false },
    			
    			{ "maxNodes", "", "maxNodes cannot be empty string", true },
    			{ "maxNodes", "Harry", "maxNodes cannot be string", true },
    			{ "maxNodes", new Object(), "maxNodes cannot be object", true },
    			{ "maxNodes", Collections.singletonList("Harry"), "maxNodes cannot be array", true },
    			{ "maxNodes", 22.0f, "maxNodes is rounded when a floating point number", false },
    			{ "maxNodes", null, "maxNodes can be null", false },
    			{ "maxNodes", 0, "maxNodes cannot be zero", true },
    			{ "maxNodes", -1, "maxNodes can be negative", false },
    			{ "maxNodes", -2, "maxNodes can be negative other than -1", true },
    			{ "maxNodes", 22, "maxNodes can be an integer", false },
    			
    			{ "maxProcessorsPerNode", "", "maxProcessorsPerNode cannot be empty string", true },
    			{ "maxProcessorsPerNode", "Harry", "maxProcessorsPerNode cannot be string", true },
    			{ "maxProcessorsPerNode", new Object(), "maxProcessorsPerNode cannot be object", true },
    			{ "maxProcessorsPerNode", Collections.singletonList("Harry"), "maxProcessorsPerNode cannot be array", true },
    			{ "maxProcessorsPerNode", 22.0f, "maxProcessorsPerNode is rounded when a floating point number", false },
    			{ "maxProcessorsPerNode", null, "maxProcessorsPerNode can be null", false },
    			{ "maxProcessorsPerNode", 0, "maxProcessorsPerNode cannot be zero", true },
    			{ "maxProcessorsPerNode", -1, "maxProcessorsPerNode can be -1", false },
    			{ "maxProcessorsPerNode", -2, "maxProcessorsPerNode can be negative other than -1", true },
    			{ "maxProcessorsPerNode", 22, "maxProcessorsPerNode can be an integer", false },
    			
    			{ "maxRequestedTime", "", "maxRequestedTime cannot be empty string", true },
    			{ "maxRequestedTime", "00:00:00", "maxRequestedTime seconds can be a 6 character zero string", false },
    			{ "maxRequestedTime", "00:00:01", "maxRequestedTime seconds can be 01", false },
    			{ "maxRequestedTime", "00:00:10", "maxRequestedTime seconds can be 10", false },
    			{ "maxRequestedTime", "00:00:09", "maxRequestedTime seconds can be 09", false },
    			{ "maxRequestedTime", "00:00:59", "maxRequestedTime seconds can be 59", false },
    			{ "maxRequestedTime", "00:00:60", "maxRequestedTime seconds must be less than 60", true },
    			{ "maxRequestedTime", "00:00:70", "maxRequestedTime seconds must be less than 60", true },
    			{ "maxRequestedTime", "00:00:80", "maxRequestedTime seconds must be less than 60", true },
    			{ "maxRequestedTime", "00:00:90", "maxRequestedTime seconds must be less than 60", true },
    			{ "maxRequestedTime", "00:00:000", "maxRequestedTime seconds must be two digits", true },
    			
    			{ "maxRequestedTime", "00:01:00", "maxRequestedTime minutes can be 01", false },
    			{ "maxRequestedTime", "00:10:00", "maxRequestedTime minutes can be 10", false },
    			{ "maxRequestedTime", "00:09:00", "maxRequestedTime minutes can be 09", false },
    			{ "maxRequestedTime", "00:59:00", "maxRequestedTime minutes can be 59", false },
    			{ "maxRequestedTime", "00:60:00", "maxRequestedTime minutes must be less than 60", true },
    			{ "maxRequestedTime", "00:70:00", "maxRequestedTime minutes must be less than 60", true },
    			{ "maxRequestedTime", "00:80:00", "maxRequestedTime minutes must be less than 60", true },
    			{ "maxRequestedTime", "00:90:00", "maxRequestedTime minutes must be less than 60", true },
    			{ "maxRequestedTime", "00:000:00", "maxRequestedTime minutes must be two digits", true },
    			
    			{ "maxRequestedTime", "01:00:00", "maxRequestedTime hours can be 01", false },
    			{ "maxRequestedTime", "10:00:00", "maxRequestedTime hours can be 10", false },
    			{ "maxRequestedTime", "09:00:00", "maxRequestedTime hours can be 09", false },
    			{ "maxRequestedTime", "59:00:00", "maxRequestedTime hours can be 59", false },
    			{ "maxRequestedTime", "60:00:00", "maxRequestedTime hours can be 60", false },
    			{ "maxRequestedTime", "70:00:00", "maxRequestedTime hours can be 70", false },
    			{ "maxRequestedTime", "80:00:00", "maxRequestedTime hours can be 80", false },
    			{ "maxRequestedTime", "90:00:00", "maxRequestedTime hours can be 90", false },
    			{ "maxRequestedTime", "99:00:00", "maxRequestedTime hours can be 99", false },
    			
    			{ "maxRequestedTime", "001:00:00", "maxRequestedTime hours can be 001", false },
    			{ "maxRequestedTime", "010:00:00", "maxRequestedTime hours can be 010", false },
    			{ "maxRequestedTime", "009:00:00", "maxRequestedTime hours can be 009", false },
    			{ "maxRequestedTime", "059:00:00", "maxRequestedTime hours can be 059", false },
    			{ "maxRequestedTime", "060:00:00", "maxRequestedTime hours can be 060", false },
    			{ "maxRequestedTime", "070:00:00", "maxRequestedTime hours can be 070", false },
    			{ "maxRequestedTime", "080:00:00", "maxRequestedTime hours can be 080", false },
    			{ "maxRequestedTime", "090:00:00", "maxRequestedTime hours can be 090", false },
    			{ "maxRequestedTime", "100:00:00", "maxRequestedTime hours can be 100", false },
    			{ "maxRequestedTime", "600:00:00", "maxRequestedTime hours can be 600", false },
    			{ "maxRequestedTime", "700:00:00", "maxRequestedTime hours can be 700", false },
    			{ "maxRequestedTime", "800:00:00", "maxRequestedTime hours can be 800", false },
    			{ "maxRequestedTime", "900:00:00", "maxRequestedTime hours can be 900", false },
    			{ "maxRequestedTime", "999:00:00", "maxRequestedTime hours can be 999", false },
    			
    			{ "maxRequestedTime", "0010:00:00", "maxRequestedTime hours can be 001", false },
    			{ "maxRequestedTime", "0100:00:00", "maxRequestedTime hours can be 010", false },
    			{ "maxRequestedTime", "0090:00:00", "maxRequestedTime hours can be 009", false },
    			{ "maxRequestedTime", "0590:00:00", "maxRequestedTime hours can be 059", false },
    			{ "maxRequestedTime", "0600:00:00", "maxRequestedTime hours can be 060", false },
    			{ "maxRequestedTime", "0700:00:00", "maxRequestedTime hours can be 070", false },
    			{ "maxRequestedTime", "0800:00:00", "maxRequestedTime hours can be 080", false },
    			{ "maxRequestedTime", "0900:00:00", "maxRequestedTime hours can be 090", false },
    			{ "maxRequestedTime", "1000:00:00", "maxRequestedTime hours can be 100", false },
    			{ "maxRequestedTime", "6000:00:00", "maxRequestedTime hours can be 600", false },
    			{ "maxRequestedTime", "7000:00:00", "maxRequestedTime hours can be 700", false },
    			{ "maxRequestedTime", "8000:00:00", "maxRequestedTime hours can be 800", false },
    			{ "maxRequestedTime", "9000:00:00", "maxRequestedTime hours can be 900", false },
    			{ "maxRequestedTime", "9999:00:00", "maxRequestedTime hours can be 999", false },
    			{ "maxRequestedTime", "10000:00:00", "maxRequestedTime hours cannot be greater than 9999", true },
    			
    			{ "maxRequestedTime", "0000000", "maxRequestedTime cannot be an integer string", true },
    			{ "maxRequestedTime", "00:00:00:00", "maxRequestedTime cannot be 4 sets of values", true },
    			{ "maxRequestedTime", "00:00", "maxRequestedTime cannot be 2 sets of values", true },
    			{ "maxRequestedTime", "00", "maxRequestedTime cannot be 1 set of values", true },
    			{ "maxRequestedTime", new Object(), "maxRequestedTime cannot be object", true },
    			{ "maxRequestedTime", Collections.singletonList("00:00:00"), "maxRequestedTime cannot be array", true },
    			{ "maxRequestedTime", 22.0f, "maxRequestedTime cannot be a floating point number", true },
    			{ "maxRequestedTime", null, "maxRequestedTime can be null", false },
    			{ "maxRequestedTime", -1, "maxRequestedTime cannot be negative", true },
    			{ "maxRequestedTime", 22, "maxRequestedTime cannot be an integer", true },
    			
    			{ "default", "", "default cannot be empty string", true },
    			{ "default", "Harry", "default cannot be string", true },
    			{ "default", new Object(), "default cannot be object", true },
    			{ "default", Collections.singletonList("Harry"), "default cannot be array", true },
    			{ "default", null, "default can be null", false },
    	};
    }

    @Test (groups={"model","system"}, dataProvider="batchQueueName")
    public void batchQueueNameValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        super.commonBatchQueueFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "batchQueueMaxMemoryParser")
    public Object[][] batchQueueMaxMemoryParser() {
    	return new Object[][] {
    			{ "2.0GB", 2d, "Decimal GB are converted to long values", false },
    			{ "2.5GB", 2.5d, "Decimal GB are rounded down to long values", false },
    			{ "2.7GB", 2.7d, "Decimal GB are rounded down to long values", false },
    			{ "2GB", 2d, "GB are converted to long values", false },
    			{ "2gb", 2d, "gb are converted to long values", false },
    			{ "2TB", 2048d, "TB are converted to long values", false },
    			{ "2tb", 2048d, "tb are converted to long values", false },
    			{ "2PB", 2097152d, "PB are converted to long values", false },
    			{ "2pb", 2097152d, "pb are converted to long values", false },
    			{ "2EB", 2147483648d, "EB are converted to long values", false },
    			{ "2eb", 2147483648d, "eb are converted to long values", false },
    			{ "2", 2d, "integers are treated as gb", false },
    			{ "2.5", 2.5d, "decimals greater than 1 are treated as gb", false },
    			{ "2048", 2048d, "integers are treated as gb", false },
    			{ "0.5", .5d, "decimals less than 1 are treated as partial gb", false },
    	};
    }
    
    @Test (groups={"model","system", "broken"}, dataProvider="batchQueueMaxMemoryParser")
    public void batchQueueMaxMemoryParserTest(String value, double expectedValue, String message, boolean exceptionThrown) 
    throws Exception 
    {
    	boolean exceptionFlag = false;
		String exceptionMsg = message;
		BatchQueue bq = new BatchQueue();
    	
    	try 
		{
    		bq.setMaxMemoryPerNode(value);
			Assert.assertEquals(bq.getMaxMemoryPerNode(), expectedValue, message);
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = "Invalid JSON submitted, attribute maxMemory " + message + " \n\"maxMemory\" = \"" + value + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				Assert.fail(exceptionMsg, se);
		}

//		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		Assert.assertEquals(exceptionThrown, exceptionFlag, exceptionMsg);
    }
    
    @DataProvider(name = "batchQueueDefault")
    public Object[][] batchQueueDefault() {
    	return new Object[][] { 
		    { "default", null, false, "default is false if null", false },
			{ "default", Boolean.FALSE, false, "default can be false", false },
			{ "default", Boolean.TRUE, true, "default can be true", false },
		};
	}
    
    @Test (groups={"model","system"}, dataProvider="batchQueueDefault")
    public void batchQueueDefaultTest(String name, Boolean changeValue, boolean expectedValue, String message, boolean exceptionThrown) 
    throws Exception 
    {
    	if (changeValue == null) {
    		jsonTree.remove("default");
    	} else {
    		jsonTree.put("default", changeValue.booleanValue());
    	}
		
		try 
		{
			BatchQueue queue = BatchQueue.fromJSON(jsonTree);
			Assert.assertEquals(queue.isSystemDefault(), expectedValue, message);
		}
		catch(Exception se){
			se.printStackTrace();
		}
    }
    
    @Test (groups={"model","system"})
    public void batchQueueEqualityTest() 
    throws Exception 
    {
    	try 
		{
			BatchQueue queue = BatchQueue.fromJSON(jsonTree);
			BatchQueue testQueue = BatchQueue.fromJSON(jsonTree);

			Assert.assertEquals(queue, testQueue, "Identical values with different uuid should be equal");
			
			testQueue.setName(queue.getName() + "-copy");
			Assert.assertNotEquals(queue, testQueue, "Different names with identical mappedNames should not be equal");
			
			testQueue.setName(queue.getName());
			testQueue.setMappedName(queue.getName() + "-copy");
			Assert.assertNotEquals(queue, testQueue, "Identical names with different mappedNames should not be equal");
			
			testQueue.setMappedName(queue.getMappedName());
			testQueue.setDescription(queue.getDescription() + "-copy");
			Assert.assertEquals(queue, testQueue, "Identical names with different mappedNames should be equal");
		}
		catch(Exception se){
			se.printStackTrace();
		}
    }
    
    @Test (groups={"model","system"})
    public void cloneTest() 
    throws Exception 
    {
    	ExecutionSystem e = new ExecutionSystem();
    	
    	BatchQueue original = BatchQueue.fromJSON(jsonTree);
    	original.setId(1L);
    	original.setExecutionSystem(e);
    	
    	BatchQueue clone = original.clone();

		Assert.assertNotEquals(clone, original, "Cloned queue should not pass the equality test");

		Assert.assertEquals(clone.getName(), original.getName(), "Queue name was not copied over on clone. ");
		Assert.assertEquals(clone.getMappedName(), original.getMappedName(), "Queue mappedName was not copied over on clone. ");
		Assert.assertEquals(clone.getDescription(), original.getDescription(), "Queue description was not copied over on clone. ");
		Assert.assertEquals(clone.getMaxJobs(), original.getMaxJobs(), "Queue maxJobs was not copied over on clone. ");
		Assert.assertEquals(clone.getMaxUserJobs(), original.getMaxUserJobs(), "Queue maxUserJobs was not copied over on clone. ");
		Assert.assertEquals(clone.getMaxNodes(), original.getMaxNodes(), "Queue maxNodes was not copied over on clone. ");
		Assert.assertEquals(clone.getMaxProcessorsPerNode(), original.getMaxProcessorsPerNode(), "Queue maxProcessorsPerNode was not copied over on clone. ");
		Assert.assertEquals(clone.getMaxMemoryPerNode(), original.getMaxMemoryPerNode(), "Queue maxMemoryPerNode was not copied over on clone. ");
		Assert.assertEquals(clone.getCustomDirectives(), original.getCustomDirectives(), "Queue customDirectives was not copied over on clone. ");
    	Assert.assertEquals(original.isSystemDefault(), clone.isSystemDefault(), "Queue systemDefault was not copied over on clone. ");
    	Assert.assertNull(clone.getExecutionSystem(), "Queue executionSystem should not be copied on a cone operation.");
    	Assert.assertNotEquals(original.getId(), clone.getId(), "Queue id should not be copied id a cone operation.");
    	Assert.assertNotEquals(original.getUuid(), clone.getUuid(), "Queue uuid should not be copied id a cone operation.");
    }

}
