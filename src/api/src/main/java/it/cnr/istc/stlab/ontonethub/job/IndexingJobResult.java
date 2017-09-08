package it.cnr.istc.stlab.ontonethub.job;

import org.apache.stanbol.commons.jobs.api.JobResult;

/**
 *
 * This class represents the output of an indexing job.
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */

public class IndexingJobResult implements JobResult {
	
	private String message;
	private boolean success;
	
	public IndexingJobResult(String message, boolean success) {
		this.message = message;
		this.success = success;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public boolean isSuccess() {
		return success;
	}

}
