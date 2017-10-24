package it.cnr.istc.stlab.ontonethub.job;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * The IndexingJobInput represents the input for an IndexingJob.
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
public class IndexingJobInput {

	private String name, description, baseURI, ontologyId;
	private Model data;
	
	public IndexingJobInput() {
		
	}
	
	public IndexingJobInput(String name, String description, String baseURI, Model data) {
		this.name = name;
		this.description = description;
		this.baseURI = baseURI;
		this.data = data;
	}
	
	public IndexingJobInput(String ontologyId, String name, String description, String baseURI, Model data) {
		this(name, description, baseURI, data);
		this.ontologyId = ontologyId;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getBaseURI() {
		return baseURI;
	}
	
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}
	
	public Model getData() {
		return data;
	}
	
	public void setData(Model data) {
		this.data = data;
	}
	
	public String getOntologyId() {
		return ontologyId;
	}
	
	public void setOntologyId(String ontologyId) {
		this.ontologyId = ontologyId;
	}
	
}
