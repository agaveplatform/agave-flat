/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.jobs.exceptions.JobMacroResolutionException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;

/**
 * Concreate class for SGE batch submit scripts.
 * 
 * @author dooley
 * 
 */
public class TorqueSubmitScript extends AbstractSubmitScript {

	public static final String DIRECTIVE_PREFIX = "#PBS ";

	/**
	 * Default constructor used by all {@link SubmitScript}. Note that node count will be forced to 1
	 * whenever the {@link Software#getParallelism()} is {@link ParallelismType#SERIAL} or null.
	 *
	 * @param job the job for which the submit script is being created
	 * @param software the app being run by the job
	 * @param executionSystem the system on which the app will be run
	 */
	public TorqueSubmitScript(Job job, Software software, ExecutionSystem executionSystem)
	{
		super(job, software, executionSystem);
	}

	/**
	 * Serializes the object into a PBS submit script. Assumption made are that
	 * for PTHREAD applications, the processor value is the number of cores per
	 * node. i.e. 1 node, N cores. For serial jobs, an entire node is requested.
	 * For parallel applications, half the processor value of nodes is requested
	 * with two cores per node.
	 */
	public String getScriptText() throws JobMacroResolutionException
	{
		// #!/bin/bash
		// #PBS -q batch
		// # the queue to be used.
		// #
		// #PBS -A your_allocation
		// # specify your project allocation
		// #
		// #PBS -l nodes=1:ppn=32
		// # number of nodes and number of processors on each node to be used.
		// # Do NOT use ppn = 1. Note that there are 32 procs on each Trestles node
		// # with 64GB memory/nodegoing 
		// #
		// #PBS -l cput=20:00:00
		// # requested CPU time.
		// #
		// #PBS -l walltime=20:00:00
		// # requested Wall-clock time hh:mm:ss
		// #
		// #PBS -o myoutput2
		// # name of the standard out file to be "output-file".
		// #
		// #PBS -j oe
		// # standard error output merge to the standard output file.
		// #
		// #PBS -N s_type
		// # name of the job (that will appear on executing the qstat command).
		// #
		// # Following are non PBS commands. PLEASE ADOPT THE SAME EXECUTION
		// SCHEME
		// # i.e. execute the job by copying the necessary files from your home
		// directpory
		// # to the scratch space, execute in the scratch space, and copy back
		// # the necessary files to your home directory.
		// #
		// export WORK_DIR=/work/$USER/your_code_directory
		// cd $WORK_DIR
		// # changing to your working directory (we recommend you to use work
		// volume for batch job run)
		// #
		// export NPROCS=`wc -l $PBS_NODEFILE |gawk '//{print $1}'`
		// #
		// date
		// #timing the time job starts
		// #
		
		String result = "#!/bin/bash\n" 
				+ DIRECTIVE_PREFIX + "-N " + name + "\n"
				+ DIRECTIVE_PREFIX + "-o " + standardOutputFile + "\n" 
				+ DIRECTIVE_PREFIX + "-e " + standardErrorFile + "\n" 
				+ DIRECTIVE_PREFIX + "-l cput=" + time + "\n"
				+ DIRECTIVE_PREFIX + "-l walltime=" + time + "\n"
				+ DIRECTIVE_PREFIX + "-q " + queue.getEffectiveMappedName() + "\n"
				+ DIRECTIVE_PREFIX + "-l nodes=" + nodes + ":ppn=" + processors + "\n";
				if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
					result += DIRECTIVE_PREFIX + queue.getCustomDirectives() + "\n";
				}
			
		return result;
	}

}
