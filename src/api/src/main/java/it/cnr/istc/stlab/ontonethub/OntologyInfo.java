package it.cnr.istc.stlab.ontonethub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
public class OntologyInfo {

	private String ontologyID;
	private String ontologySource;
	private String ontologyName;
	private String ontologyDescription;
	private String ontologyIRI;
	private int owlClasses;
	private int objectProperties;
	private int datatypeProperties;
	private int annotationProperties;
	private int individuals;
	private int importedOntologies;
	private Map<String, List<String>> rdfMetadata;
	
	public OntologyInfo() {
		rdfMetadata = new HashMap<String, List<String>>();
	}
	
	
	public String getOntologyID() {
		return ontologyID;
	}
	
	public void setOntologyID(String ontologyID) {
		this.ontologyID = ontologyID;
	}
	
	public String getOntologySource() {
		return ontologySource;
	}
	
	public void setOntologySource(String ontologySource) {
		this.ontologySource = ontologySource;
	}
	
	public String getOntologyName() {
		return ontologyName;
	}
	public void setOntologyName(String ontologyName) {
		this.ontologyName = ontologyName;
	}
	public String getOntologyDescription() {
		return ontologyDescription;
	}
	public void setOntologyDescription(String ontologyDescription) {
		this.ontologyDescription = ontologyDescription;
	}
	public String getOntologyIRI() {
		return ontologyIRI;
	}
	public void setOntologyIRI(String ontologyIRI) {
		this.ontologyIRI = ontologyIRI;
	}
	public int getOwlClasses() {
		return owlClasses;
	}
	public void setOwlClasses(int owlClasses) {
		this.owlClasses = owlClasses;
	}
	public int getObjectProperties() {
		return objectProperties;
	}
	public void setObjectProperties(int objectProperties) {
		this.objectProperties = objectProperties;
	}
	public int getDatatypeProperties() {
		return datatypeProperties;
	}
	public void setDatatypeProperties(int datatypeProperties) {
		this.datatypeProperties = datatypeProperties;
	}
	public int getAnnotationProperties() {
		return annotationProperties;
	}
	public void setAnnotationProperties(int annotationProperties) {
		this.annotationProperties = annotationProperties;
	}
	public int getIndividuals() {
		return individuals;
	}
	public void setIndividuals(int individuals) {
		this.individuals = individuals;
	}
	public int getImportedOntologies() {
		return importedOntologies;
	}
	public void setImportedOntologies(int importedOntologies) {
		this.importedOntologies = importedOntologies;
	}
	
	public Map<String, List<String>> getRdfMetadata() {
		return rdfMetadata;
	}
	
	public void setRdfMetadata(Map<String, List<String>> rdfMetadata) {
		this.rdfMetadata = rdfMetadata;
	}
	
}
