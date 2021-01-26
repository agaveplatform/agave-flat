/**
 * 
 */
package org.iplantc.service.jobs.queue.actions;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.exceptions.SoftwareUnavailableException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.launchers.JobLauncher;
import org.iplantc.service.jobs.managers.launchers.JobLauncherFactory;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;

import java.net.ConnectException;
import java.nio.channels.ClosedByInterruptException;

/**
 * @author dooley
 *
 */
public class SubmissionAction extends AbstractWorkerAction {
    
    private static Logger log = Logger.getLogger(SubmissionAction.class);
    
    private JobLauncher jobLauncher = null;

    protected JobManager jobManager = null;

    public SubmissionAction(Job job) {
        super(job);
    }

    /**
     * Basic getter for job manager instance. Useful for testing
     * @return JobManager instance
     */
    protected JobManager getJobManager() {
        if (jobManager == null) {
            jobManager = new JobManager();
        }

        return jobManager;
    }

    @Override
    public synchronized void setStopped(boolean stopped) {
        super.setStopped(stopped);
        
        if (getJobLauncher() != null) {
            getJobLauncher().setStopped(true);
        }
    }
    
    /**
     * This method attempts to archive a job's output by retirieving the
     * .agave.archive shadow file from the remote job directory and staging
     * everything not in there to the user-supplied Job.archivePath on the 
     * Job.archiveSystem
     *
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     * @throws JobException
     * @throws JobDependencyException 
     */
    public void run() throws SystemUnavailableException, SystemUnknownException, JobException,
            ClosedByInterruptException, JobDependencyException
    {
        boolean submitted = false;
        
        int attempts = this.job.getRetries();
        
        try 
        {
            while (!submitted && attempts <= Settings.MAX_SUBMISSION_RETRIES)
            {
                checkStopped();
                
                this.job.setRetries(attempts);
                
                attempts++;
                
                log.debug("Attempt " + attempts + " to submit job " + job.getUuid() +
                          " (max retries = " + Settings.MAX_SUBMISSION_RETRIES + ").");
                
                this.job = getJobManager().updateStatus(this.job, JobStatusType.SUBMITTING, "Attempt " + attempts + " to submit job");
                
                try 
                {
                    setJobLauncher(new JobLauncherFactory().getInstance(job));
                    
                    getJobLauncher().launch();
                    
                    submitted = true;
                    
                    log.info("Successfully submitted job " + getJob().getUuid() + " to " + getJob().getSystem());
                }
                catch (ClosedByInterruptException e) {
                    log.debug("Interrupted: " + getJob().getUuid() + e.getMessage());
                    throw e;
                } 
                catch (ConnectException e) 
                {
                    try
                    {
                        log.debug("Failed to submit job " + getJob().getUuid() + " on " + getJob().getSystem() +
                            ". Unable to connect to system.", e);
                        this.job = getJobManager().updateStatus(getJob(), getJob().getStatus(), e.getMessage() +
                            " The service was unable to connect to the target execution system " +
                            "for this application, " + getJob().getSystem() + ". This job will " +
                            "remain in queue until the system becomes available. Original error message: " + e.getMessage());
                    }
                    catch (Exception e1) {
                        log.error("Failed to update job " + getJob().getUuid() + " status to " + job.getStatus(), e1);
                    }
                }
                catch (SystemUnavailableException e) 
                {
                    try
                    {
                        log.debug("One or more dependent systems for job " + getJob().getUuid() + " is currently unavailable. " + e.getMessage());
                        this.job = getJobManager().updateStatus(getJob(), JobStatusType.STAGED,
                            "Remote execution of  job " + getJob().getUuid() + " is currently paused waiting for " + getJob().getSystem() + 
                            " to become available. If the system becomes available again within 7 days, this job " +
                            "will resume staging. After 7 days it will be killed. Original error message: " + e.getMessage());
                    }
                    catch (Exception e1) {
                        log.error("Failed to update job " + getJob().getUuid() + " status to STAGED", e1);
                    }   
                    break;
                }
                catch (SoftwareUnavailableException e) 
                {
                    try
                    {
                        log.debug("Software for job " + getJob().getUuid() + " is currently unavailable. " + e.getMessage());
                        this.job = getJobManager().updateStatus(getJob(), JobStatusType.STAGED,
                            "Remote execution of  job " + getJob().getUuid() + " is currently paused waiting for " + job.getSoftwareName() + 
                            " to become available. If the app becomes available again within 7 days, this job " +
                            "will resume staging. After 7 days it will be killed. Original error message: " + e.getMessage());
                    }
                    catch (Exception e1) {
                        log.error("Failed to update job " + getJob().getUuid() + " status to STAGED", e1);
                    }   
                    break;
                }
                catch (SchedulerException e) 
                {
                    try
                    {
                        log.error("Failed to submit job " + getJob().getUuid() + " on " + getJob().getSystem() + 
                                " due to scheduler exception.", e);
                        this.job = getJobManager().updateStatus(getJob(), getJob().getStatus(), "Attempt "
                            + attempts + " failed to submit job due to scheduler exception. " + e.getMessage());
                    }
                    catch (Exception e1) {
                        log.error("Failed to update job " + getJob().getUuid() + " status to " + getJob().getStatus(), e1);
                    }
                } 
                catch (Exception e) 
                {   
                    if (e.getCause() instanceof UnresolvableObjectException || 
                            e.getCause() instanceof ObjectNotFoundException) {
                        log.error("Race condition was just avoided for job " + job.getUuid(), e);
                        break;
                    }
                    else if (attempts >= Settings.MAX_SUBMISSION_RETRIES ) 
                    {
                        try
                        {
                            log.error("Failed to submit job " + job.getUuid() + 
                                " after " + attempts + " attempts.", e);
                            this.job = getJobManager().updateStatus(job, job.getStatus(), "Attempt "
                                    + attempts + " failed to submit job. " + e.getCause().getMessage());
                        }
                        catch (Exception e1) {
                            log.error("Failed to update job " + job.getUuid() + " status to " + job.getStatus(), e1);
                        }
                    
                        try 
                        {
                            if (job.isArchived()) {
                                this.job = getJobManager().deleteStagedData(job);
                            }
                        } 
                        catch (Exception e1)
                        {
                            try
                            {
                                log.error("Failed to remove remote work directory for job " + job.getUuid(), e1);
                                this.job = getJobManager().updateStatus(job, job.getStatus(),
                                    "Failed to remove remote work directory.");
                            }
                            catch (Exception e2) {
                                log.error("Failed to update job " + job.getUuid() + " status to " + job.getStatus(), e2);
                            }
                        }
                        
                        try
                        {
                            log.error("Unable to submit job " + job.getUuid() + 
                                    " after " + attempts + " attempts. Job cancelled.");
                            this.job = getJobManager().updateStatus(job, JobStatusType.FAILED,
                                    "Unable to submit job after " + attempts + " attempts. Job cancelled.");
                        }
                        catch (Exception e1) {
                            log.error("Failed to update job " + job.getUuid() + " status to FAILED", e1);
                        }
                        
                        break;
                    } 
                    else 
                    {
                        try 
                        {
                            String msg = "Attempt " + attempts + " failed to submit job. " + e.getCause().getMessage();
                            log.info(msg, e);
                            this.job = getJobManager().updateStatus(job, job.getStatus(), msg);
                        }
                        catch (Exception e1) {
                            log.error("Failed to update job " + job.getUuid() + " status to " + job.getStatus(), e1);
                        }
                    }
                }
            }
        }
        finally 
        {
            // clean up the job directory now that we're either done or failed
            if (getJobLauncher() != null) {
                FileUtils.deleteQuietly(getJobLauncher().getTempAppDir());
            }
        }
    }

    public synchronized JobLauncher getJobLauncher() {
        return jobLauncher;
    }

    public synchronized void setJobLauncher(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }
    
    /**
     * Returns the current job refernce. Since the launcher will update the
     * job status as it goes and invalidate the current referenced entity,
     * this method will use that value if present, otherwise it will use its
     * own.  (This, of course, is ridiculous.)
     * 
     * @return current valid job reference
     */
    @Override
    public synchronized Job getJob() {
        if (getJobLauncher() == null) {
            return this.job;
        } else {
            return getJobLauncher().getJob();
        }
    }
}
