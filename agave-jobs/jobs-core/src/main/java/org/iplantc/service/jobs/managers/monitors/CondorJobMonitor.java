/**
 *
 */
package org.iplantc.service.jobs.managers.monitors;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitoringException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author dooley
 *
 */
public class CondorJobMonitor extends DefaultJobMonitor {
    private static final Logger log = Logger.getLogger(CondorJobMonitor.class);

    /**
     * Default constructor for all child classes.
     * @param job the job to monitor
     * @param executionSystem the execution system on which the job is running
     */
    public CondorJobMonitor(Job job, ExecutionSystem executionSystem) {
        super(job, executionSystem);
    }

    /**
     * Fetches the condor job log file from the remote host. For CondorHT systems, this is a more reliable approach
     * than trying to query status values, which may be less descriptive and updated less frequently.
     *
     * @param queryCommand the command to run on the {@link ExecutionSystem}. This is ignored in favor of fetching the job log file
     * @return contents of the condor job's log file
     * @throws RemoteJobMonitoringException if unable to fetch the condor log file content for any reason.
     */
    protected String queryRemoteJobStatus(String queryCommand) throws RemoteJobMonitoringException {
        String remoteLogFilePath = job.getWorkPath() + "/runtime.log";
        RemoteDataClient remoteDataClient = null;
        try {
            remoteDataClient = getAuthenticatedRemoteDataClient();
        } catch (Throwable e) {
            throw new RemoteJobMonitoringException("Unable to check status of job " + job.getUuid() +
                    ". Failed to establish connection to exectuion system "
                    + job.getSystem(), e);
        }

        log.debug("Fetching condor runtime log file for job " + this.job.getUuid()
                + " at agave://" + job.getSystem() + "/" + remoteLogFilePath);

        Path localRuntimeLogFilePath = null;
        try {
            // Create temp file to hold the fetched runtime log
            localRuntimeLogFilePath = Files.createTempFile(job.getUuid(), null);
            // download the content to the file
            remoteDataClient.get(remoteLogFilePath, localRuntimeLogFilePath.toAbsolutePath().toString());

            return new String(Files.readAllBytes(localRuntimeLogFilePath));
        } catch (RemoteDataException | IOException e) {
            throw new RemoteJobMonitoringException("Unable to check status of job " + job.getUuid() +
                    ". Failed to retrieve remote runtime log file from job work directory", e);
        } finally {
            try {
                if (localRuntimeLogFilePath != null) Files.delete(localRuntimeLogFilePath);
            } catch (Exception ignored) {}
            // clean up the remote data client, closing the connection
            try { if (remoteDataClient != null) remoteDataClient.disconnect(); } catch (Exception ignored) {}
        }
    }

//	/* (non-Javadoc)
//	 * @see org.iplantc.service.jobs.managers.monitors.AbstractJobMonitor#monitor()
//	 */
//	@Override
//	public Job monitor() throws RemoteJobMonitoringException, SystemUnavailableException, ClosedByInterruptException
//	{
//		try
//        {
//        	if (job != null)
//        	{
//        		this.executionSystem = JobManager.getJobExecutionSystem(this.job);
//
//				// if the execution system login config is local, then we cannot submit
//				// jobs to this system remotely. In this case, a worker will be running
//				// dedicated to that system and will submitting jobs locally. All workers
//				// other that this will should pass on accepting this job.
//				if (this.executionSystem.getLoginConfig().getProtocol().equals(LoginProtocolType.LOCAL) &&
//						!Settings.LOCAL_SYSTEM_ID.equals(this.job.getSystem()))
//				{
//					return this.job;
//				}
//				else
//				{
//					// increment the number of checks and lastupdated timestamp
//				    this.job.setLastUpdated(new DateTime().toDate());
//				    this.job.setStatusChecks(this.job.getStatusChecks() + 1);
//		        	this.job = JobManager.updateStatus(this.job, this.job.getStatus(), this.job.getErrorMessage());
//
//		        	try {
//                        JobStatusResponseParser parser = JobMonitorResponseParserFactory.getInstance(this.job);
//                        JobStatusResponse<CondorJobStatus> response = parser.parse(job.getLocalJobId(), fetchCondorRuntimeLogFile());
//
//                        if (response.getRemoteSchedulerJobStatus().isDoneStatus()) {
//
//                        }
//		        	}
//		        	catch (IOException e) {
//
//		        	    // log file not there, so check status in condor_q or condor_monitor
//	                    checkRemoteCondorStatus();
//		        	}
//		        	catch (JobTerminationException e) {
//		        	    // failed to kill the process. status and history have already
//		        	    // been updated.
//		        	}
//				}
//        	}
//
//        	return this.job;
//        }
//        catch (ClosedByInterruptException e) {
//            throw e;
//        }
//        catch (StaleObjectStateException | UnresolvableObjectException e) {
//            throw e;
//        }
//        catch (RemoteJobMonitoringException | SystemUnavailableException e) {
//			throw e;
//		}
//        catch (Throwable e)
//		{
//			throw new RemoteJobMonitoringException("Failed to query status of job " + job.getUuid(), e);
//		}
//        finally {
//        	FileUtils.deleteQuietly(retrievedCondorLogFile);
//        	try { this.remoteDataClient.disconnect(); } catch (Exception e) {}
//        }
//	}
//
//	/**
//     * Fetches the remote condor log directory from the job work directory.
//     *
//     * @return {@link File} object reference to the fetched file cached locally to disk
//	 * @throws IOException if the remote file could not be fetched
//     * @throws RemoteJobMonitoringException if unable to fetch the log file from the remote system
//     * @throws ClosedByInterruptException if interrupted by an external process
//     */
//    protected String fetchCondorRuntimeLogFile() throws RemoteJobMonitoringException, IOException
//    {
//        String remoteLogFilePath = job.getWorkPath() + "/runtime.log";
//
//        // Create temp file to hold the fetched runtime log
//        Path localRuntimeLogFilePath = Files.createTempFile(job.getUuid(), null);
//
//        checkStopped();
//
//        try {
//            this.remoteDataClient = getAuthenticatedRemoteDataClient();
//        }
//        catch (Throwable e) {
//            throw new RemoteJobMonitoringException("Unable to check status of job " + job.getUuid() +
//                    ". Failed to establish connection to exectuion system "
//                    + job.getSystem());
//        }
//
//        log.debug("Fetching condor runtime log file for job " + this.job.getUuid()
//                + " at agave://" + job.getSystem() + "/" + remoteLogFilePath);
//
//        checkStopped();
//
//        try {
//            if (this.remoteDataClient.doesExist(remoteLogFilePath))
//            {
//                checkStopped();
//
//                RemoteTransferListener listener = null;
//
//                this.remoteDataClient.get(remoteLogFilePath, localRuntimeLogFilePath.toAbsolutePath().toString(), listener);
//
//                return new String(Files.readAllBytes(localRuntimeLogFilePath));
//            }
//            else
//            {
//                throw new FileNotFoundException("No such file or directory");
//            }
//        }
//        catch (FileNotFoundException e) {
//            throw e;
//        } catch (RemoteDataException e) {
//            throw new RemoteJobMonitoringException("Unable to check status of job " + job.getUuid() +
//                    ". Failed to retrieve remote runtime log file from job work directory", e);
//        }
//    }
//
//    /**
//     * Kills the job due to errors detected in its runtime log.
//     *
//     * @return true if killed, false otherwise
//     * @throws ClosedByInterruptException if this thread is interrupted by a worker shutdown
//     */
//    protected boolean killJob() throws ClosedByInterruptException, JobException, JobTerminationException
//    {
//        try
//        {
//            checkStopped();
//
//            JobKiller killer = JobKillerFactory.getInstance(job);
//            int retries = 0;
//            while (retries < Settings.MAX_SUBMISSION_RETRIES)
//            {
//                checkStopped();
//
//                try
//                {
//                    log.debug("Attempt " + (retries+1) + " to kill job " + job.getUuid() +
//                            " and clean up assets");
//
//                    killer.attack();
//
//                    this.job = killer.getJob();
//
//                    log.debug("Successfully killed remaining processes of job " + job.getUuid());
//
//                    this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED,
//                            "Successfully killed remote job process.");
//
//                    return true;
//                }
//                catch (RemoteExecutionException e) {
//
//                    this.job = killer.getJob();
//
//                    String message = "Failed to kill job " + job.getUuid()
//                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem()
//                            + " Response from " + job.getSystem() + ": " + e.getMessage();
//
//                    log.debug(message);
//
//                    try {
//                        HibernateUtil.getSession()
//                                    .createSQLQuery("update jobs j set j.status = :status, j.last_updated = :date where j.id = :id")
//                                    .setLong("id", job.getId())
//                                    .setString("status", JobStatusType.FAILED.name())
//                                    .setDate("date", new DateTime().toDate())
//                                    .executeUpdate();
//                        JobEvent evt = new JobEvent(JobStatusType.FAILED, message, job.getOwner());
//                        evt.setJob(this.job);
//                        JobEventDao.persist(evt);
//
//                        NotificationManager.process(job.getUuid(), evt.getStatus(), evt.getCreatedBy());
//                        this.job = JobDao.getById(this.job.getId());
////                        Job job2 =
////                        this.job = JobManager.updateStatus(job2, JobStatusType.FAILED, "Failed to kill remote process."
////                            + " Response from " + job2.getSystem() + ": " + e.getMessage());
//                        HibernateUtil.flush();
//                    } catch (Exception e2) {
//                        log.error("Failed to update job " + job.getUuid() + " status to FAILED after failed " +
//                                "to kill the remote job process");
//                    }
////                    JobDao.persist(job);
//
//                    throw new JobTerminationException("Failed to kill job " + job.getUuid()
//                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem(), e);
//                }
//                catch (JobTerminationException e) {
//
//                    retries++;
//
////                    this.job = killer.getJob();
//
//                    String message = "Failed to kill job " + job.getUuid() +
//                            " on attempt " + retries + ". Response from " + job.getSystem() + ": " + e.getMessage();
//
//                    log.debug(message);
//
//                    this.job = JobManager.updateStatus(job, job.getStatus(), message);
//
//                    if (retries == Settings.MAX_SUBMISSION_RETRIES) {
//
//                        message = "Failed to kill job " + job.getUuid() +
//                                " after " + retries + "  attempts. Terminating job.";
//
//                        log.debug(message);
//
//                        this.job = JobManager.updateStatus(job, JobStatusType.FAILED, message);
//
//                        throw e;
//                    }
//                }
//            }
//
//            return false;
//        }
//        catch (ClosedByInterruptException | JobException | StaleObjectStateException | UnresolvableObjectException e) {
//            throw e;
//        }
//        catch (JobTerminationException e) {
//            throw e;
//        }
//        catch (Throwable e)
//        {
//            throw new JobTerminationException("Job " + job.getUuid() + " failed during execution and further " +
//                    "attempts to explicitly clean up the process failed.", e);
//        }
//    }
//
//    /**
//     * Updates the job based on the results of data found in the job's
//     * Condor runtime log file.
//     *
//     * @param {@link File} reference to local cached condor runtime log
//     * @throws IOException
//     * @throws JobException
//     * @throws JobTerminationException
//     */
//    protected void parseLogFile(JobStatusResponse<CondorJobStatus> response) throws IOException, JobException, JobTerminationException
//    {
////    	CondorJobStatusResponseParser condorJobLogParser = new CondorJobStatusResponseParser(retrievedCondorLogFile);
//
//        // if the runtime log says it failed, we need to kill the job. we leave the assets
//        // in place...
//        // TODO: How to feed back the assets afterwards. may or may not reliably be there
//        if (response.getRemoteSchedulerJobStatus().isFailureStatus())
//        {
//            log.debug("Job " + job.getUuid() + " was found in a FAILED state on " + job.getSystem() +
//                    " as local job id " + job.getLocalJobId()
//                    + " based on its runtime log file.");
//
//            killJob();
//        }
//        // here we know the job is done, so we can update the status.
//        // once we do that, the ArchiveWatch will kick in and handle things.
//        else if (response.getRemoteSchedulerJobStatus().isDoneStatus())
//        {
//            log.debug("Job " + job.getUuid() + " was found in a CLEANING_UP state on " + job.getSystem() +
//                    " based on its runtime log file. Updating status to CLEANING_UP.");
//
//            this.job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP,
//                    "Job completion detected by Condor monitor.");
//
//            if (!this.job.isArchiveOutput()) {
//                log.debug("Job " + this.job.getUuid() + " will skip archiving at user request.");
//                this.job = JobManager.updateStatus(this.job, JobStatusType.FINISHED, "Job completed. Skipping archiving at user request.");
//                log.debug("Job " + this.job.getUuid() + " finished.");
//            }
//        }
//        // if the runtime log says it failed, we need to kill the job. we leave the assets
//        // in place...
//        // TODO: How to feed back the assets afterwards. may or may not reliably be there
//        else if (response.getRemoteSchedulerJobStatus().isFailureStatus())
//        {
//            log.debug("Job " + job.getUuid() + " was found in a FAILED state on " + job.getSystem() +
//                    " as local job id " + job.getLocalJobId()
//                    + " based on its runtime log file.");
//
//            killJob();
//        }
//        else if (response.getStatus() != job.getStatus()) {
//
//
//             this.job = JobManager.updateStatus(job, job.getStatus());
//        }
//        // nothing to see. timestamp was already updated. carry on.
//        else
//        {
//
//            log.debug("Runtime file found for job " + job.getUuid() + ". Job remains "
//                    + job.getStatus() + " based on its runtime log file.");
//            // this.job = JobManager.updateStatus(job, job.getStatus());
//        }
//    }
//
//    /**
//     * Queries condor scheduler and history respectively looking for the job status.
//     * This is called if the runtime log file is not found.
//     *
//     * @throws ClosedByInterruptException
//     * @throws RemoteJobMonitoringException
//     * @throws JobException
//     */
//    public void checkRemoteCondorStatus() throws ClosedByInterruptException, RemoteJobMonitoringException
//    {
//        RemoteSubmissionClient remoteSubmissionClient = null;
//
//        // make sure we have not abored
//        checkStopped();
//
//        // get a remote execution client
//        try {
//            remoteSubmissionClient = this.executionSystem.getRemoteSubmissionClient(job.getInternalUsername());
//        }
//        catch (Throwable e) {
//            throw new RemoteJobMonitoringException("Failed to run job status query on " + job.getSystem());
//        }
//
//        String result = null;
//
//
//        try
//        {
//            String queryCommand = this.executionSystem.getScheduler().getBatchQueryCommand() + " " + job.getLocalJobId();
//
//            // run the check against condor_q
//            result = remoteSubmissionClient.runCommand(queryCommand);
//
//            // nothing returned from condor_q, so try condor_history since it will give us info on any job submitted
//            if (StringUtils.isEmpty(result)) {
//                result = remoteSubmissionClient.runCommand("condor_history -format '%d' JobStatus " + job.getLocalJobId());
//            }
//
//            // parse the status response to update our job status
//            if (StringUtils.isEmpty(result)) {
//                this.job = JobManager.updateStatus(job, JobStatusType.FAILED, "No record of the job on the remote Condor system. Assuming job deleted forcefully on the Condor system.");
//            }
//            else if (StringUtils.equals("0", result)) {
//                this.job = JobManager.updateStatus(job, JobStatusType.STOPPED, "Job failed to checkpoint. When it starts running, it will start running from the beginning");
//            }
//            else if (StringUtils.equals("1", result)) {
//                this.job = JobManager.updateStatus(job, JobStatusType.STOPPED, "Job is current in queue");
//            }
//            else if (StringUtils.equals("2", result)) {
//                this.job = JobManager.updateStatus(job, JobStatusType.STOPPED, "Job removed forcefully on the Condor system");
//            }
//            else if (StringUtils.equals("3", result)) {
//                this.job = JobManager.updateStatus(job, JobStatusType.STOPPED, "Job removed forcefully on the Condor system");
//            }
//            else if (StringUtils.equals("4", result)) {
//                job.setEndTime(new DateTime().toDate());
//                this.job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, "Job completed, but no callback received");
//                log.debug("Job " + job.getUuid() + " was found in a state of CLEANING UP.");
//                if (!this.job.isArchiveOutput()) {
//                    log.debug("Job " + this.job.getUuid() + " will skip archiving at user request.");
//                    this.job = JobManager.updateStatus(this.job, JobStatusType.FINISHED, "Job completed. Skipping archiving at user request.");
//                    log.debug("Job " + this.job.getUuid() + " finished.");
//                }
//            }
//            else if (StringUtils.equals("5", result)) {
//                this.job = JobManager.updateStatus(job, JobStatusType.PAUSED, "Job paused");
//            }
//            else if (StringUtils.equals("6", result)) {
//                this.job = JobManager.updateStatus(job, JobStatusType.FAILED, "Job failed during submission by the Condor server");
//            }
//        }
//        catch (StaleObjectStateException | UnresolvableObjectException e) {
//            throw e;
//        }
//        catch (Throwable e) {
//            throw new RemoteJobMonitoringException("Failed to run job status query on " + job.getSystem());
//        }
//    }

}