/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.quartz.*;

/**
 * @author dooley
 *
 */
@DisallowConcurrentExecution
public class HelloJob implements InterruptableJob {

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
//		System.out.println("Hello job " + (new Date()).toString());
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		// TODO Auto-generated method stub
	}

}
