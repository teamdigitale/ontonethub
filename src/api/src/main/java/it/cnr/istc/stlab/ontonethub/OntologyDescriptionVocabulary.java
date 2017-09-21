package it.cnr.istc.stlab.ontonethub;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */
public class OntologyDescriptionVocabulary {

	public static String NS = "http://dati.gov.it/onto/ann-voc/";
	
	public static String ONTOLOGY  = NS + "ontology/";
	public static String DAF_LABEL  = NS + "dafLabel";
	public static String DAF_ID  = NS + "dafId";
	public static String DEFINED_IN_ONTOLOGY  = NS + "definedInOntology";
	public static String DOMAIN_CLASS_LABEL  = NS + "domainClassLabel";
	public static String DOMAIN_CLASS_COMMENT = NS + "domainClassComment";
	public static String ONTOLOGY_LABEL  = NS + "ontologyLabel";
	public static String ONTOLOGY_COMMENT  = NS + "ontologyComment";
	public static String HAS_BUNDLE = NS + "hasBundle";
	public static String HAS_ONTOLOGY_IRI = NS + "hasOntologyIRI";
	public static String OWL_CLASSES = NS + "owlClasses";
	public static String OBJECT_PROPERTIES = NS + "objectProperties";
	public static String DATATYPE_PROPERTIES = NS + "datatypeProperties";
	public static String ANNOTATION_PROPERTIES = NS + "annotationProperties";
	public static String INDIVIDUALS = NS + "individuals";
	public static String IMPORTED_ONTOLOGIES = NS + "importedOntologies";
	
	public static Property domainClassLabel = ResourceFactory.createProperty(DOMAIN_CLASS_LABEL);
	public static Property domainClassComment = ResourceFactory.createProperty(DOMAIN_CLASS_COMMENT);
	public static Property ontologyLabel = ResourceFactory.createProperty(ONTOLOGY_LABEL);
	public static Property ontologyComment = ResourceFactory.createProperty(ONTOLOGY_COMMENT);
	
}
