package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobMacroResolutionException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class holds methods to dynamically generate a Condor Submit file
 */
public class CustomCondorSubmitScript extends AbstractSubmitScript {
	private final static Logger log = Logger.getLogger(CustomCondorSubmitScript.class);

    /**
     * Default constructor used by all {@link SubmitScript}. Note that node count will be forced to 1
     * whenever the {@link Software#getParallelism()} is {@link ParallelismType#SERIAL} or null.
     *
     * @param job the job for which the submit script is being created
     * @param software the app being run by the job
     * @param executionSystem the system on which the app will be run
     */
    public CustomCondorSubmitScript(Job job, Software software, ExecutionSystem executionSystem)
    {
        super(job, software, executionSystem);
    }

	public enum CondorUniverse {
		vanilla, standard, vm;
	}
	
    String executable 				= "transfer_wrapper.sh";
    
    CondorUniverse universe 		= CondorUniverse.vanilla;
    //String errorFilename      	 	= "job.err";                // these are all defaults for use with condor test job
    //String outputFilename     	 	= "job.out";
    String logFilename 				= "runtime.log";
    String initialDir 				= "";
    String shouldTransferFiles    	= "YES";
    String whenToTransferOutput 	= "ON_EXIT";
    String transferInputFiles     	= "transfer.tar.gz";
    String transferOutputFiles    	= "output.tar.gz";
    String arguments;
    String requirements 			= "OpSys == \"LINUX\" ";
    String notification				= "never";
    //String queue 					= "1";

    static final String Queue = "queue 1";

    /**
     * Creates a String representing a Condor Submit file
     *
     * @return String contents of a Condor Submit file
     */
    @Override
    public String getScriptText() throws JobException, JobMacroResolutionException
    {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
        	sb.append(resolveMacros(queue.getCustomDirectives()));
        }
        sb.append("\n\n");
        
        sb.append("executable   = " + executable + "\n");
        sb.append("input        = " + transferInputFiles + "\n");
        sb.append("error        = " + standardErrorFile + "\n");
        sb.append("output       = " + standardOutputFile + "\n");
        sb.append("log          = " + logFilename + "\n");
        sb.append("should_transfer_files  = " + shouldTransferFiles + "\n");
        sb.append("when_to_transfer_output = " + whenToTransferOutput+"\n");
        
        List<String> inputs = new ArrayList<>();
        try 
        {
        	// add the job inputs
        	Map<String,String[]> jobInputMap = JobManager.getJobInputMap(job);
        	
			for (String inputKey: jobInputMap.keySet()) 
			{
				for (String rawJobInputValue: jobInputMap.get(inputKey))
				{
					try 
					{
						URI rawJobInputUri = new URI(rawJobInputValue);
						String name = FilenameUtils.getName(rawJobInputUri.getPath());
						if (!StringUtils.isEmpty(name)) {
							inputs.add(name);
						}
					} 
					catch (Throwable e) 
					{
						throw new IOException("Failed to add " + rawJobInputValue + " for app input " + 
								inputKey + " to the list of inputs in the condor submit script for job " 
								+ job.getUuid(), e);
					}
				}
			}
		} 
        catch (IOException e) {
        	throw new JobException(e.getMessage());
        }
        catch (JobException e) {
			throw new JobException("Failed to add inputs to the condor submit script for job " + job.getUuid(), e);
		}
        
        if (inputs.isEmpty()) {
        	sb.append("transfer_input_files = " + transferInputFiles + "\n");
        } else {
        	sb.append("transfer_input_files = " + transferInputFiles + ", " + StringUtils.join(inputs, ", ") + "\n");
        }
        
        sb.append("notification = never\n");
        if ( !StringUtils.isEmpty(arguments) ) {
            sb.append("Arguments    = " + arguments + "\n\n");
        }
        sb.append(Queue);
        sb.append("\n\n");
        
        return sb.toString();
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public CondorUniverse getUniverse() {
        return universe;
    }

    public void setUniverse(CondorUniverse universe) {
    	this.universe = universe;
    }

//    public String getErrorFilename() {
//        return errorFilename;
//    }
//
//    public void setErrorFilename(String error) {
//    	errorFilename = error;
//    }
//
//    public String getOutputFilename() {
//        return outputFilename;
//    }
//
//    public void setOutputFilename(String output) {
//    	outputFilename = output;
//    }

    public String getLogFilename() {
        return logFilename;
    }

    public void setLogFilename(String log) {
        logFilename = log;
    }

    public String getInitialdir() {
        return initialDir;
    }

    public void setInitialDir(String initialdir) {
        this.initialDir = initialdir;
    }

    public String getShouldTransferFiles() {
        return shouldTransferFiles;
    }

    public void setShouldTransferFiles(String shouldTransferFiles) {
        this.shouldTransferFiles = shouldTransferFiles;
    }

    public String getWhenToTransferOutput() {
        return whenToTransferOutput;
    }

    public void setWhenToTransferOutput(String whenToTransferOutput) {
        this.whenToTransferOutput = whenToTransferOutput;
    }

    public String getTransferInputFiles() {
        return transferInputFiles;
    }

    public void setTransferInputFiles(String transferInputFiles) {
        this.transferInputFiles = transferInputFiles;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}