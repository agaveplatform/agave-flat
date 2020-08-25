/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.jobs.exceptions.JobMacroResolutionException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;

/**
 * Concreate class for fully custom LoadLeveler batch submit scripts. This behaves 
 * similarly to the {@link LoadLevelerSubmitScript}, but does not attempt to 
 * set any info, rather deferring to the user to customize their scheduler
 * directives as they see fit.
 * @author dooley
 * 
 */
public class CustomLoadLevelerSubmitScript extends LoadLevelerSubmitScript {

	public static final String DIRECTIVE_PREFIX = "#@ ";

	/**
	 * Default constructor used by all {@link SubmitScript}. Note that node count will be forced to 1
	 * whenever the {@link Software#getParallelism()} is {@link ParallelismType#SERIAL} or null.
	 *
	 * @param job the job for which the submit script is being created
	 * @param software the app being run by the job
	 * @param executionSystem the system on which the app will be run
	 */
	public CustomLoadLevelerSubmitScript(Job job, Software software, ExecutionSystem executionSystem)
	{
		super(job, software, executionSystem);
	}

	/**
	 * Serializes the object to a batch submit script using a predefined 
	 * job name error, and output directive and whatever was provided in the
	 * {@link BatchQueue#getCustomDirectives()} for the queue assigned to
	 * the associated job.
	 * 
	 * @return serialized scheduler directives for appending to the job *.ipcexe script
	 */
	@Override
	public String getScriptText() throws JobMacroResolutionException {
		if (StringUtils.isEmpty(queue.getCustomDirectives())) {
			return super.getScriptText();
		}
		else {
			String result = "#! /bin/bash -l \n" 
				+ DIRECTIVE_PREFIX + "- " + name + "\n"
				+ DIRECTIVE_PREFIX + "output = " + standardOutputFile + "\n" 
				+ DIRECTIVE_PREFIX + "error = " + standardErrorFile + "\n";
			
			result += resolveMacros(queue.getCustomDirectives()) + "\n\n";
			
			return result;
		}
	}

}
