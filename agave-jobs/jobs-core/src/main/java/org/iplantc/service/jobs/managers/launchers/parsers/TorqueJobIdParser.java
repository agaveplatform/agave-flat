package org.iplantc.service.jobs.managers.launchers.parsers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the output from a qsub command into a local job id that can be used for querying later on.
 *
 * @author dooley
 */
public class TorqueJobIdParser implements RemoteJobIdParser {

    @Override
    public String getJobId(String output) throws RemoteJobIDParsingException, JobException, SchedulerException {
        String jobID = null;
        Pattern pattern = Pattern.compile("([0-9]+).*");

        String[] lines = output.replaceAll("\r", "\\n").split("\n");
		for (String s : lines) {
			String line = s.trim();
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				jobID = line.split(" ")[0];
				break;
			}
		}

        if (StringUtils.isEmpty(jobID)) {
            if (output.contains("qsub") || output.contains("submit error")) {
                throw new SchedulerException(output);
            } else {
                throw new RemoteJobIDParsingException(output);
            }
        } else {
            return jobID;
        }
    }

}
