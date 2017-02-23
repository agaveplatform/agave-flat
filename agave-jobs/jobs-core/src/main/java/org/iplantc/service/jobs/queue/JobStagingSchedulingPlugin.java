/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.apache.log4j.Logger;
import org.iplantc.service.common.messaging.MessageQueueListener;
import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.queue.factory.AbstractJobProducerFactory;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

/**
 * Class to initialize worker tasks to stage job data prior to
 * job execution. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class JobStagingSchedulingPlugin extends GenericSchedulingPlugin 
{
	
	private static final Logger log = Logger
			.getLogger(JobStagingSchedulingPlugin.class);
	/**
	 * 
	 */
	public JobStagingSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return StagingWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "Staging";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.jobs.Settings");
//            return Settings.MAX_STAGING_TASKS;
            return 1;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}

	/**
     * Performs cleanup and rollback tasks for the currently allocated jobs
     * in this vm.
     */
	@Override
	public void shutdown()
	{
		// nothing to do here. scheduler is already shutting down.
		try
		{
			log.debug("Shutting down " + getPluginGroup().toLowerCase() + " queue...");
			
			for (JobExecutionContext jobContext : scheduler.getCurrentlyExecutingJobs())
			{
				Job job = jobContext.getJobInstance();
				if (job instanceof MessageQueueListener) {
					((MessageQueueListener)job).stop();
				}
				else if (job instanceof InterruptableJob) {
					((InterruptableJob)job).interrupt();
				}
			}
		}
		catch (NullPointerException e) {
		    // happens when the scheduler wasn't initialized properly. Ususally due to us
		    // disabling it completely.
		}
		catch (SchedulerException e)
		{
			log.error("Failed to shut down queue properly.", e);
		}
		finally {
			for (String uuid: AbstractJobProducerFactory.getStagingjobtaskqueue()) {
				log.debug("Rolling back staging job " + uuid + " prior to shutdown.");
				try {
					org.iplantc.service.jobs.model.Job j = JobDao.getByUuid(uuid);
					if (j != null) {
						log.debug("Located staging job " + uuid + " for rollback during shutdown.");
						JobManager.resetToPreviousState(j, "admin");
						log.debug("Successfully rolled back staging job " + uuid + " during shutdown.");
					}
					else {
						log.debug("Unable to locate staging job " + uuid + " for rollback. "
								+ "This job will be in a zombie state when the container restarts.");
					}
				}
				catch (Throwable t) {
					log.debug("Rollback of staging job " + uuid + " failed during shutdown.", t);
				}
			}
		}
	}

}
