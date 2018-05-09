package sandbox;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class ReadOntology {

	public static void main(String[] args) {
		
		Model data = FileManager.get().loadModel("https://w3id.org/italia/onto/CLV"); 
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		ontModel.add(data);
		
		ontModel.write(System.out);
	}
	
}
