package org.iplantc.service.jobs.managers.monitors;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitoringException;
import org.iplantc.service.jobs.managers.JobEventProcessor;
import org.iplantc.service.jobs.managers.JobStatusResponse;
import org.iplantc.service.jobs.managers.monitors.parsers.JobMonitorResponseParserFactory;
import org.iplantc.service.jobs.managers.monitors.parsers.JobStatusResponseParser;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobEventType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.joda.time.DateTime;

import java.nio.channels.ClosedByInterruptException;

/**
 * Monitors a remote {@link Job} by querying the {@link ExecutionSystem} for the local job id.
 *
 * @author dooley
 */
public class DefaultJobMonitor extends AbstractJobMonitor {
	private static final Logger log = Logger.getLogger(DefaultJobMonitor.class);

	/**
	 * Default constructor for all child classes.
	 * @param job the job to monitor
	 * @param executionSystem the execution system on which the job is running
	 */
	public DefaultJobMonitor(Job job, ExecutionSystem executionSystem)
	{
		super(job, executionSystem);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.monitors.AbstractJobMonitor#monitor()
	 */
	@Override
	public Job monitor() throws RemoteJobMonitoringException, SystemUnavailableException, ClosedByInterruptException {
		// skip it all if the job is null
		if (getJob() == null) return null;

		RemoteSubmissionClient remoteSubmissionClient = null;

		// get the execution system from the getter to ensure we check availability before proceeding.
		ExecutionSystem executionSystem = getExecutionSystem();

		// if the execution system login config is local, then we cannot submit
		// jobs to this system remotely. In this case, a worker will be running
		// dedicated to that system and will submitting jobs locally. All workers
		// other that this will should pass on accepting this job.
		if (executionSystem.getLoginConfig().getProtocol().equals(LoginProtocolType.LOCAL) &&
				!Settings.LOCAL_SYSTEM_ID.equals(getJob().getSystem())) {
			return getJob();
		} else { // otherwise, check the remote server
			try {
				// increment the number of checks
				getJob().setLastUpdated(new DateTime().toDate());
				getJob().setStatusChecks(getJob().getStatusChecks() + 1);
				updateJobStatus(getJob().getStatus(), getJob().getErrorMessage());

				checkStopped();

				String queryCommand = String.format("%s%s",
						getStartupScriptCommand(),
						getBatchQueryCommand());

				try {
					JobStatusResponse<?> response = getJobStatusResponse(getJobStatusResponseParser(), queryCommand);

					// if the job status has changed
					if (getJob().getStatus() != response.getStatus()) {
						//							TODO: We should validate the observed value against the job state machine to ensure we do
//								not introduce an infinite loop where a job keeps getting resubmitted and rerun over and
//								over again due to unforseen parsing issues or a bug on the remote side.
						if (response.getStatus() != null && getJob().getStatus().getNextValidStates().contains(response.getStatus())) {
							log.error("Job " + getJob().getUuid() + " was found in a remote status of " +
									response.getRemoteSchedulerJobStatus().toString() +
									". This maps to an Agave job status of " + response.getStatus() +
									", which is an illegal transition from the job's current state of " +
									getJob().getStatus().name() + ". This observed transition will be accepted, but may be " +
									"removed in a future update.");
						}

						// if the response status maps to a job status, use that
						if (response.getRemoteSchedulerJobStatus().isFailureStatus()) {
							JobStatusType nextStatus = getJob().isArchiveOutput() ? JobStatusType.CLEANING_UP : JobStatusType.FAILED;
							log.debug("Job " + getJob().getUuid() + " was found in a failure state on " +
									getJob().getSystem() + " as local job id " + getJob().getLocalJobId() +
									". Updating status to " + nextStatus.name() + ".");

							updateJobStatus(nextStatus,
									"Job status change to failed detected by job monitor.");

						} else if (response.getRemoteSchedulerJobStatus().isDoneStatus()) {
							log.debug("Job " + getJob().getUuid() + " was found in a completed state on " +
									getJob().getSystem() + " as local job id " + getJob().getLocalJobId() +
									". Updating status to " + JobStatusType.CLEANING_UP.name() + ".");

							updateJobStatus(JobStatusType.CLEANING_UP,
									"Job status change to completed detected by job monitor.");
						} else if (response.getRemoteSchedulerJobStatus().getRunningStatuses().contains(response.getRemoteSchedulerJobStatus())) {
							log.debug("Job " + getJob().getUuid() + " was found in a running state on " +
									getJob().getSystem() + " as local job id " + getJob().getLocalJobId() +
									". Updating status to " + JobStatusType.RUNNING.name() + ".");

							updateJobStatus(JobStatusType.RUNNING,
									"Job status change to running detected by job monitor.");
						} else if (response.getRemoteSchedulerJobStatus().isQueuedStatus()) {
							log.debug("Job " + getJob().getUuid() + " was found in a queued state on " +
									getJob().getSystem() + " as local job id " + getJob().getLocalJobId() +
									". Updating status to " + JobStatusType.QUEUED.name() + ".");

							updateJobStatus(JobStatusType.QUEUED,
									"Job status change to queued detected by job monitor.");
						} else if (response.getRemoteSchedulerJobStatus().isPausedStatus()) {
							log.debug("Job " + getJob().getUuid() + " was found in a paused state on " +
									getJob().getSystem() + " as local job id " + getJob().getLocalJobId() +
									". Updating status to " + JobStatusType.PAUSED.name() + ".");

							updateJobStatus(JobStatusType.PAUSED,
									"Job status change to paused detected by job monitor.");
						} else if (response.getRemoteSchedulerJobStatus().isUnrecoverableStatus()) {
							JobStatusType nextStatus = getJob().isArchiveOutput() ? JobStatusType.CLEANING_UP : JobStatusType.FAILED;
							log.debug("Job " + getJob().getUuid() + " was found in an unrecoverable state on " +
									getJob().getSystem() + " as local job id " + getJob().getLocalJobId() +
									". Updating status to " + nextStatus.name() + ".");

							updateJobStatus(nextStatus,
									"Job status change to an unrecoverable state detected by job monitor.");
						} else {
							updateJobStatus(getJob().getStatus(),
									"Job " + getJob().getUuid() + " was found in an unknown state on " +
											getJob().getSystem() + " as local job id " + getJob().getLocalJobId() +
											". The job will remain active until a terminal state is detected.");
						}
					} else {
						updateJobStatus(getJob().getStatus(), getJob().getErrorMessage());
					}
				}
				// if the response was empty, raise an event so anyone who cares to investigate can do so
				catch (RemoteJobMonitorEmptyResponseException e) {
					JobEvent event = new JobEvent(JobEventType.EMPTY_STATUS_RESPONSE.name(),
							JobEventType.EMPTY_STATUS_RESPONSE.getDescription(), getJob().getOwner());
					event.setJob(getJob());
					JobEventProcessor eventProcessor = new JobEventProcessor(event);
					eventProcessor.process();

					// TODO: shall we check the agave job log and/or accounting logs for the job status at this point?

				} catch (RemoteJobMonitorResponseParsingException e) {
					log.error(e.getMessage());
					updateJobStatus(getJob().getStatus(), getJob().getErrorMessage());
				}

				return getJob();
			} catch (JobException e) {
				// this is a db issue that cannot be worked around. If we can't update the db, then the monitor
				// will have to be retried by a process that can. Until then, we just exit out from here.
				log.error(e.getMessage());
				return getJob();
			} catch (ClosedByInterruptException | SystemUnavailableException | StaleObjectStateException | UnresolvableObjectException e) {
				throw e;
			} catch (Throwable e) {
				throw new RemoteJobMonitoringException("Failed to query status of job " + getJob().getUuid(), e);
			}
		}
	}

	/**
	 * Performs the remote call to make the job status check.
	 * @param responseParser the parser to be used to analyze the response from running the queryCommand
	 * @param queryCommand the command to run on the {@link ExecutionSystem}
	 * @return the response from the job check command.
	 * @throws RemoteJobMonitoringException if unable to query the remote job status
	 * @throws RemoteJobMonitorEmptyResponseException if no repsonse comes back from the server when it should never be emtpy
	 * @throws RemoteJobMonitorResponseParsingException if the response from the server cannot be parsed
	 */
	public JobStatusResponse<?> getJobStatusResponse(JobStatusResponseParser responseParser, String queryCommand)
			throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobMonitoringException {

		String schedulerResponseText = queryRemoteJobStatus(queryCommand);

		return responseParser.parse(getJob().getLocalJobId(), schedulerResponseText);
	}

	/**
	 * Wrapper around the factory call to {@link JobMonitorResponseParserFactory#getInstance(Job)} for easier testing
	 * @return an instance of {@link JobStatusResponseParser} for the current {@link #getJob()}
	 * @throws SystemUnknownException when the system does not exist anymore.
	 * @throws SystemUnavailableException when the system is not available due to downtime or being set unavailable.
	 */
	protected JobStatusResponseParser getJobStatusResponseParser() throws SystemUnavailableException, SystemUnknownException {
		return new JobMonitorResponseParserFactory().getInstance(getJob());
	}

	/**
	 * Performs the remote call to make the job status check.
	 * @param queryCommand the command to run on the {@link ExecutionSystem}
	 * @return the response from the job check command.
	 * @throws RemoteJobMonitoringException if unable to query the remote job status
	 */
	protected String queryRemoteJobStatus(String queryCommand) throws RemoteJobMonitoringException {

		try (RemoteSubmissionClient remoteSubmissionClient = getRemoteSubmissionClient()) {

			log.debug("Forking command \"" + queryCommand + "\" on " + remoteSubmissionClient.getHost() + ":" +
					remoteSubmissionClient.getPort() + " for job " + getJob().getUuid());

			String schedulerResponseText = remoteSubmissionClient.runCommand(queryCommand);

			log.debug("Response for job " + getJob().getUuid() + " monitoring command was: " + schedulerResponseText);

			return schedulerResponseText;
		} catch (Exception e) {
			throw new RemoteJobMonitoringException("Failed to retrieve status information for job " + getJob().getUuid() +
					" from remote system.", e);
		}
	}
}
