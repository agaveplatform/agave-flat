package org.iplantc.service.io.queue;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.QueueTask;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractJobWatch<T extends QueueTask> implements WorkerWatch<T> {

    protected static final Logger log = Logger.getLogger(AbstractJobWatch.class);

    protected AtomicBoolean stopped = new AtomicBoolean(false);
    protected AtomicBoolean taskComplete = new AtomicBoolean(false);
    protected T queueTask = null;
    protected LogicalFile file = null;
    protected Long queueTaskId;

    private boolean allowFailure = false;

    public AbstractJobWatch() {
        super();
    }

    public AbstractJobWatch(boolean allowFailure) {
        this();
        this.allowFailure = allowFailure;
    }

    /* (non-Javadoc)
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
                log.debug("Queue draining has been enabled. Skipping " + this.getClass().getSimpleName() + " task.");
                return;
            }

            if (context.getMergedJobDataMap().containsKey("queueTaskId")) {
                setQueueTaskId(context.getMergedJobDataMap().getLong("queueTaskId"));
            }

            if (getQueueTask() != null
                    && (getQueueTask().getStatusAsString().equalsIgnoreCase("STAGING_QUEUED")
                    || getQueueTask().getStatusAsString().equalsIgnoreCase("TRANSFORMING_QUEUED"))) {
                // this is a new thread and thus has no tenant info loaded. we set it up
                // here so things like app and system lookups will stay local to the 
                // tenant
                TenancyHelper.setCurrentTenantId(getQueueTask().getLogicalFile().getTenantId());
                TenancyHelper.setCurrentEndUser(getQueueTask().getLogicalFile().getOwner());

                if (log.isDebugEnabled())
                    log.debug("Worker found " + getClass().getSimpleName() + " " + getQueueTask().getId()
                            + " for user " + getQueueTask().getLogicalFile().getOwner() + " to process");
                doExecute();

            } else {
                // Log why we got here.
                if (log.isTraceEnabled()) {
                    String msg;
                    if (getQueueTask() == null)
                        msg = getClass().getSimpleName() + " skipping execution of null task!";
                    else {
                        msg = getClass().getSimpleName() + " skipping execution of task " + getQueueTask().getId()
                                + " because status equals " + getQueueTask().getStatusAsString() + ".";
                        if (getQueueTask().getLogicalFile() != null)
                            msg += " The associated file is " +
                                    getQueueTask().getLogicalFile().getAgaveRelativePathFromAbsolutePath() +
                                    " for user " + getQueueTask().getLogicalFile().getOwner() + ".";
                    }
                    log.trace(msg);
                }
            }
        } catch (JobExecutionException e) {
            String msg = "Unable to execute quartz job: " + e.getMessage();
            log.error(msg, e);
            if (allowFailure) throw e;
        } catch (Throwable e) {
            String msg = "Unexpected error during job worker execution: " + e.getMessage();
            log.error(msg, e);
            if (allowFailure)
                throw new JobExecutionException(msg, e);
        } finally {
            if (this.queueTaskId != null) {
                log.debug("Releasing " + getClass().getSimpleName() + " " + this.queueTaskId
                        + " after task completion");
                releaseJob();
            }
        }
    }

    @Override
    public abstract void interrupt() throws UnableToInterruptJobException;

    /**
     * Performs the job status rollback to a previous state allowing it
     * to be picked up by other workers.
     */
    protected abstract void rollbackStatus();

    @Override
    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * Release the job from the correct threadsafe pool for future processing.
     */
    protected abstract void releaseJob();

    @Override
    public void setStopped(boolean killed) throws UnableToInterruptJobException {
        this.stopped.set(killed);
        int timeout = 0;

        while (!isTaskComplete()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            timeout++;
            if (timeout >= 30) {
                String msg = "Unable to interrupt " + getClass().getName() + " task "
                        + getQueueTask().getId() + " after 30 seconds.";
                throw new UnableToInterruptJobException(msg);
            }
        }
    }

    @Override
    public boolean isTaskComplete() {
        return taskComplete.get();
    }

    @Override
    public void setTaskComplete(boolean complete) {
        this.taskComplete.set(complete);
    }

    /**
     * @param queueTask the queue task to set
     */
    @Override
    public synchronized void setQueueTask(T queueTask) {
        this.queueTask = queueTask;
    }

    /**
     * @return the queue task for this job
     */
    @Override
    public synchronized T getQueueTask() {
        return this.queueTask;
    }

    /**
     * @return the queueTaskId
     */
    public synchronized Long getQueueTaskId() {
        return this.queueTaskId;
    }

    /**
     * @param queueTaskId the id of the queue task for this job
     */
    public synchronized void setQueueTaskId(Long queueTaskId) {
        this.queueTaskId = queueTaskId;
    }

}