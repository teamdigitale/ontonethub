package it.cnr.istc.stlab.ontonethub;

import java.util.Collection;

import org.apache.stanbol.commons.jobs.api.Job;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSite;

import com.hp.hpl.jena.rdf.model.Model;

import it.cnr.istc.stlab.ontonethub.job.IndexingJobInput;

/**
 * Public interface for the OntoNetHub.<br>
 * It declares the core methods that implements CRUD operations on the ontology index.
 * 
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
public interface OntoNetHub {
	
	/**
	 * The method creates an index for the ontology provided as input inside the object instance of the class {@link IndexingJobInput}.
	 * The index is generated as an OSGi bundle and loaded to the Felix environment as a {@link ManagedSite} of the {@link Entityhub}.
	 * The index is created by running an indexing job, which is a instance of a {@link Job}.
	 * 
	 * @param input
	 * @return the String identifying the indexing job.
	 * @throws OntologyAlreadyExistingException
	 */
	String indexOntology(IndexingJobInput input) throws OntologyAlreadyExistingException;
	
	/**
	 * The method return the source of the ontology identified by the ID provided as input.
	 * 
	 * @param id
	 * @return the ontology as a Jena {@link Model}
	 * @throws NoSuchOntologyException
	 */
	Model getOntologySource(String id) throws NoSuchOntologyException;
	
	/**
	 * The method returns all the metadata associated with an ontology within the index.
	 * Namely, those metadata are:
	 * <ul>
	 * <li><b>ontologyID</b>: the identifier of the ontology in the OntoNetHub;</li>
	 * <li><b>ontologySource</b>: the URL of the service that serves the ontology source;</li>
	 * <li><b>ontologyName</b>: the name of the ontology;</li>
	 * <li><b>ontologyDescription</b>: a human-readable description of the ontology;</li>
	 * <li><b>ontologyIRI</b>: the IRI associated with the ontology (i.e. the official location in the Web where the ontology is available);</li>
	 * <li><b>owlClasses</b>: the number of the OWL classes available in the ontology;</li>
	 * <li><b>objectProperties</b>: the number of the OWL object properties available in the ontology;</li>
	 * <li><b>datatypeProperties</b>: the number of the OWL datatype properties available in the ontology;</li>
	 * <li><b>annotationProperties</b>: the number of the OWL annotation properties available in the ontology;</li>
	 * <li><b>annotationProperties</b>: the number of the OWL individuals available in the ontology;</li>
	 * <li><b>importedOntologies</b>: the number of the ontology imported by ontology by means of owl:import axioms;</li>
	 * </ul>
	 * 
	 * @param id
	 * @return
	 * @throws NoSuchOntologyException
	 */
	OntologyInfo getOntologyInfo(String id) throws NoSuchOntologyException;
	
	/**
	 * The method returns all the metadata associated with the ontologies available in the index.
	 * 
	 * @return
	 * @throws NoSuchOntologyException
	 */
	OntologyInfo[] getOntologiesInfo();
	
	/**
	 * The method allows developers to delete ontology from the OntoNetHub.
	 * 
	 * @param id
	 * @throws UnmappableOntologyException
	 * @throws NoSuchOntologyException
	 */
	void deleteOntologyIndex(String id) throws UnmappableOntologyException, NoSuchOntologyException;
	
	Collection<Representation> getOntologyEntityContext(String entityID, String lang);

}
