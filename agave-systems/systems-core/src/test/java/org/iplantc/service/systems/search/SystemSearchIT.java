package org.iplantc.service.systems.search;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.PersistedSystemsModelTestCommon;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.*;

import java.text.SimpleDateFormat;
import java.util.*;

@Test(groups = {"integration", "broken"})
public class SystemSearchIT extends PersistedSystemsModelTestCommon {

    private final SystemDao systemDao = new SystemDao();

    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        super.beforeClass();
        clearSystems();
    }

//	@AfterClass
//	@Override
//	public void afterClass() throws Exception
//	{
//		super.afterClass();
//	}

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @AfterMethod
    public void tearDown() throws Exception {
        clearSystems();
    }

    private ExecutionSystem createExecutionSystem() throws Exception {
        ExecutionSystem system = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_PROXY_EXECUTION_SYSTEM_FILE));
        system.setOwner(SYSTEM_OWNER);
        return system;
    }

    @DataProvider
    public Object[][] systemsProvider() throws Exception {
        ExecutionSystem system = createExecutionSystem();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return new Object[][]{
                {"globaldefault", system.isGlobalDefault()},
                {"default", system.getUsersUsingAsDefault().contains(system.getOwner())},
                {"available", system.isAvailable()},
                {"description", system.getDescription()},
                {"id", system.getSystemId()},
                {"name", system.getName()},
                {"owner", system.getOwner()},
                {"public", system.isPubliclyAvailable()},
                {"site", system.getSite()},
                {"status", system.getStatus()},
                {"storage.zone", system.getStorageConfig().getZone()},
                {"storage.resource", system.getStorageConfig().getResource()},
                {"storage.container", system.getStorageConfig().getContainerName()},
                {"storage.host", system.getStorageConfig().getHost()},
                {"storage.port", system.getStorageConfig().getPort()},
                {"storage.homedir", system.getStorageConfig().getHomeDir()},
                {"storage.rootdir", system.getStorageConfig().getRootDir()},
                {"storage.protocol", system.getStorageConfig().getProtocol()},
                {"storage.proxy.name", system.getStorageConfig().getProxyServer().getName()},
                {"storage.proxy.host", system.getStorageConfig().getProxyServer().getHost()},
                {"storage.proxy.port", system.getStorageConfig().getProxyServer().getPort()},
                {"status", system.getStatus()},
                {"type", system.getType()},
                {"login.host", system.getLoginConfig().getHost()},
                {"login.port", system.getLoginConfig().getPort()},
                {"login.protocol", system.getLoginConfig().getProtocol()},
                {"login.proxy.name", system.getLoginConfig().getProxyServer().getName()},
                {"login.proxy.host", system.getLoginConfig().getProxyServer().getHost()},
                {"login.proxy.port", system.getLoginConfig().getProxyServer().getPort()},
                {"workdir", system.getWorkDir()},
                {"scratchdir", system.getScratchDir()},
                {"maxsystemjobs", system.getMaxSystemJobs()},
                {"maxsystemjobsperuser", system.getMaxSystemJobsPerUser()},
                {"startupscript", system.getStartupScript()},
                {"executiontype", system.getExecutionType()},
                {"environment", system.getEnvironment()},
                {"scheduler", system.getScheduler()},
                {"executiontype", system.getExecutionType()},
                {"queues.customdirectives", system.getDefaultQueue().getCustomDirectives()},
                {"queues.maxjobs", system.getDefaultQueue().getMaxJobs()},
                {"queues.maxmemorypernode", system.getDefaultQueue().getMaxMemoryPerNode()},
                {"queues.maxnodes", system.getDefaultQueue().getMaxNodes()},
                {"queues.maxprocessorspernode", system.getDefaultQueue().getMaxProcessorsPerNode()},
                {"queues.maxuserjobs", system.getDefaultQueue().getMaxUserJobs()},
                {"queues.name", system.getDefaultQueue().getName()},
        };
    }

    @Test(dataProvider = "systemsProvider")
    public void findMatching(String attribute, Object value) throws Exception {
        ExecutionSystem system = createExecutionSystem();
        if (StringUtils.equalsIgnoreCase(attribute, "privateonly")) {
            system.setPubliclyAvailable(false);
        } else if (StringUtils.equalsIgnoreCase(attribute, "publiconly")) {
            system.setPubliclyAvailable(true);
        }
        systemDao.persist(system);
        Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");

        Map<String, String> map = new HashMap<String, String>();
        if (StringUtils.equals(attribute, "uuid")) {
            map.put(attribute, system.getUuid());
        } else {
            map.put(attribute, String.valueOf(value));
        }

        List<SystemSearchResult> systems = systemDao.findMatching(system.getOwner(), new SystemSearchFilter().filterCriteria(map));
        Assert.assertNotNull(systems, "findMatching failed to find any system.");
        Assert.assertEquals(systems.size(), 1, "findMatching returned the wrong number of system for search by " + attribute);
        Assert.assertEquals(systems.get(0).getSystemId(), system.getSystemId(), "findMatching did not return the saved system.");
    }

    @DataProvider
    public Object[][] findMatchingTimeProvider() throws Exception {
        return new Object[][]{
                {"created"},
                {"lastupdated"},
        };
    }

    @Test(dataProvider = "findMatchingTimeProvider")
    public void findMatchingTime(String field) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        ExecutionSystem system = createExecutionSystem();
        systemDao.persist(system);
        Assert.assertTrue(system.getId() > 0, "Failed to generate a system ID.");

        String searchField = field + ".ON";
        Map<String, String> map = new HashMap<String, String>();
        map.put(searchField, formatter.format(system.getLastUpdated()));

        List<SystemSearchResult> systems = systemDao.findMatching(system.getOwner(), new SystemSearchFilter().filterCriteria(map));
        Assert.assertNotNull(systems,
                "findMatching failed to find any systems matching " + searchField);
        Assert.assertEquals(systems.size(), 1,
                "findMatching returned the wrong number of system records for search by " + searchField);
        Assert.assertEquals(systems.get(0).getSystemId(), system.getSystemId(),
                "findMatching did not return the saved system when searching by " + searchField);
    }

    @Test(dataProvider = "systemsProvider")
    public void findMatchingCaseInsensitive(String attribute, Object value) throws Exception {
        ExecutionSystem system = createExecutionSystem();
        if (StringUtils.equalsIgnoreCase(attribute, "privateonly")) {
            system.setPubliclyAvailable(false);
        } else if (StringUtils.equalsIgnoreCase(attribute, "publiconly")) {
            system.setPubliclyAvailable(true);
        }
        systemDao.persist(system);
        Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");

        Map<String, String> map = new HashMap<String, String>();
        if (StringUtils.equals(attribute, "uuid")) {
            map.put(attribute, system.getUuid());
        } else {
            map.put(attribute, String.valueOf(value));
        }

        List<SystemSearchResult> systems = systemDao.findMatching(system.getOwner(), new SystemSearchFilter().filterCriteria(map));
        Assert.assertNotNull(systems, "findMatching failed to find any systems.");
        Assert.assertEquals(systems.size(), 1, "findMatching returned the wrong number of system records for search by " + attribute);
        Assert.assertEquals(systems.get(0).getSystemId(), system.getSystemId(), "findMatching did not return the saved system.");
    }

    @DataProvider
    protected Object[][] dateSearchExpressionTestProvider() {
        List<Object[]> testCases = new ArrayList<Object[]>();
        String[] timeFormats = new String[]{"Ka", "KKa", "K a", "KK a", "K:mm a", "KK:mm a", "Kmm a", "KKmm a", "H:mm", "HH:mm", "HH:mm:ss"};
        String[] sqlDateFormats = new String[]{"YYYY-MM-dd", "YYYY-MM"};
        String[] relativeDateFormats = new String[]{"yesterday", "today", "-1 day", "-1 month", "-1 year", "+1 day", "+1 month"};
        String[] calendarDateFormats = new String[]{"MMMM d", "MMM d", "YYYY-MM-dd", "MMMM d, Y",
                "MMM d, Y", "MMMM d, Y", "MMM d, Y",
                "M/d/Y", "M/dd/Y", "MM/dd/Y", "M/d/YYYY", "M/dd/YYYY", "MM/dd/YYYY"};

        DateTime dateTime = new DateTime().minusMonths(1);
        for (String field : new String[]{"created.before"}) {

            // milliseconds since epoch
            testCases.add(new Object[]{field, "" + dateTime.getMillis(), true});

            // ISO 8601
            testCases.add(new Object[]{field, dateTime.toString(), true});

            // SQL date and time format
            for (String date : sqlDateFormats) {
                for (String time : timeFormats) {
                    testCases.add(new Object[]{field, dateTime.toString(date + " " + time), date.contains("dd") && !time.contains("Kmm")});
                }
                testCases.add(new Object[]{field, dateTime.toString(date), true});
            }

            // Relative date formats
            for (String date : relativeDateFormats) {
                for (String time : timeFormats) {
                    testCases.add(new Object[]{field, date + " " + dateTime.toString(time), !(!(date.contains("month") || date.contains("year") || date.contains(" day")) && time.contains("Kmm"))});
                }
                testCases.add(new Object[]{field, date, true});
            }


            for (String date : calendarDateFormats) {
                for (String time : timeFormats) {
                    testCases.add(new Object[]{field, dateTime.toString(date + " " + time), !(date.contains("d") && time.contains("Kmm"))});
                }
                testCases.add(new Object[]{field, dateTime.toString(date), true});
            }

        }

        return testCases.toArray(new Object[][]{});
    }

    @Test(dataProvider = "dateSearchExpressionTestProvider")
    public void dateSearchExpressionTest(String attribute, String dateFormattedString, boolean shouldSucceed) throws Exception {
        ExecutionSystem system = createExecutionSystem();
        system.setCreated(new DateTime().minusYears(5).toDate());
        systemDao.persist(system);
        Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");

        Map<String, String> map = new HashMap<String, String>();
        map.put(attribute, dateFormattedString);

        try {
            List<SystemSearchResult> systems = systemDao.findMatching(system.getOwner(), new SystemSearchFilter().filterCriteria(map));
            Assert.assertNotEquals(systems == null, shouldSucceed, "Searching by date string of the format "
                    + dateFormattedString + " should " + (shouldSucceed ? "" : "not ") + "succeed");
            if (shouldSucceed) {
                Assert.assertEquals(systems.size(), 1, "findMatching returned the wrong number of system records for search by "
                        + attribute + "=" + dateFormattedString);
                Assert.assertEquals(systems.get(0).getSystemId(), system.getSystemId(), "findMatching did not return the saved system.");
            }
        } catch (Exception e) {
            if (shouldSucceed) {
                Assert.fail("Searching by date string of the format "
                        + dateFormattedString + " should " + (shouldSucceed ? "" : "not ") + "succeed", e);
            }
        }
    }

}
