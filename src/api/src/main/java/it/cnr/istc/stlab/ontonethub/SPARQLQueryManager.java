package it.cnr.istc.stlab.ontonethub;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
/**
 * The SPARQLQueryManager allows to manage the SPARQL queries used by OntoNetHub to select particular aspects <br>
 * (e.g. property or class usage) for optimising ontology indexing.
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
public interface SPARQLQueryManager {

	/**
	 * Execute a SPARQL SELECT query over the input ontology provided as a Jena {@link Model}. <br>
	 * The method returns a {@link ResultSet} as output.  
	 * 
	 * @param ontology
	 * @return {@link ResultSet}
	 * 
	 */
	ResultSet executeSelect(Model ontology);
	
}
