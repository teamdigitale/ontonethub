package it.cnr.istc.stlab.ontonethub;

/**
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
@SuppressWarnings("serial")
public class OntologyAlreadyExistingException extends Exception {

	private String ontologyName;
	
	public OntologyAlreadyExistingException(String ontologyName) {
		this.ontologyName = ontologyName;
	}
	
	@Override
	public String getMessage() {
		return "The ontology " + ontologyName + " is already managed by the OntoNetHub";
	}
	
}
