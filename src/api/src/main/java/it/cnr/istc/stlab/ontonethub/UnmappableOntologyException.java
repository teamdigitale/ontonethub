package it.cnr.istc.stlab.ontonethub;

/**
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
@SuppressWarnings("serial")
public class UnmappableOntologyException extends Exception {

	private Exception e;
	private String ontologyID;
	
	public UnmappableOntologyException(String ontologyID, Exception e) {
		this.e = e;
		this.ontologyID = ontologyID;
	}
	
	@Override
	public String getMessage() {
		return "The ontology with ID " + ontologyID + " cannot be mapped to any bundle.";
	}
	
	public Exception getE() {
		return e;
	}
}
