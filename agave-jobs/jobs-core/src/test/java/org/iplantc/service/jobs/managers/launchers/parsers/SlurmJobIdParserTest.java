package org.iplantc.service.jobs.managers.launchers.parsers;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"unit"})
public class SlurmJobIdParserTest {

	@DataProvider
	protected Object[][] getJobIdProvider() {
		return new Object[][] {
				{ "Submitted batch job 3432234", "3432234" },
//				{ "Something\nSomething\n525638.mike3\nSomething\n\n", "525638.mike3" },
//				{ " 525638.mike3 ", "525637.mike3" },
//				{ "  525638.mike3  ", "525632.mike3" },
//				{ "Job 525638.mike3 submitted successfully ", "525638.mike3" }
		};
	}
	
	@Test(dataProvider = "getJobIdProvider")
	public void getJobId(String schedulerOutput, String expectedJobId) 
	throws RemoteJobIDParsingException, JobException, SchedulerException 
	{
		SlurmJobIdParser parser = new SlurmJobIdParser();
		String foundJobId = parser.getJobId(schedulerOutput);
		Assert.assertTrue(StringUtils.isNotEmpty(foundJobId), "Slurm job id should not be null");
		Assert.assertEquals(foundJobId, expectedJobId, "Slurm job id found did not match the expected job id");
	}
}
