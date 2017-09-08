package it.cnr.istc.stlab.ontonethub;

/**
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
@SuppressWarnings("serial")
public class NoSuchOntologyException extends Exception {

	private String ontologyID;
	
	public NoSuchOntologyException(String ontologyID) {
		this.ontologyID = ontologyID;
	}
	
	@Override
	public String getMessage() {
		return "No ontology with ID " + ontologyID + " exists.";
	}
}
