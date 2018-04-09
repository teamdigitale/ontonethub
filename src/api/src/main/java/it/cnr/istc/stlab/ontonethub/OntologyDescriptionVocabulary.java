package it.cnr.istc.stlab.ontonethub;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
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
	public static String CONTEXT = NS + "Context";
	public static String DEFINED_IN_ONTOLOGY  = NS + "definedInOntology";
	public static String DOMAIN_CLASS_LABEL  = NS + "domainClassLabel";
	public static String DOMAIN_CLASS_COMMENT = NS + "domainClassComment";
	
	public static String HAS_BUNDLE = NS + "hasBundle";
	public static String HAS_ONTOLOGY_IRI = NS + "hasOntologyIRI";
	public static String HAS_ONTOLOGY_SOURCE = NS + "hasOntologySource";
	public static String OWL_CLASSES = NS + "owlClasses";
	public static String OBJECT_PROPERTIES = NS + "objectProperties";
	public static String DATATYPE_PROPERTIES = NS + "datatypeProperties";
	public static String ANNOTATION_PROPERTIES = NS + "annotationProperties";
	public static String INDIVIDUALS = NS + "individuals";
	public static String IMPORTED_ONTOLOGIES = NS + "importedOntologies";
	public static String SYNONYM = NS + "synonym";
	public static String USAGE = NS + "usage";
	public static String DOMAIN_USAGE = NS + "domainUsage";
	public static String RANGE_USAGE = NS + "rangeUsage";
	public static String FULL_USAGE = NS + "fullUsage";
	public static String ONTOLOGY_ID = NS + "ontologyId";
	public static String ONTOLOGY_LABEL = NS + "ontologyLabel";
	public static String DOMAIN_LABEL = NS + "domainLabel";
	public static String PROPERTY_LABEL = NS + "propertyLabel";
	public static String RANGE_LABEL = NS + "rangeLabel";
	public static String ONTOLOGY_COMMENT  = NS + "ontologyComment";
	public static String DOMAIN_COMMENT = NS + "domainComment";
	public static String PROPERTY_COMMENT = NS + "propertyComment";
	public static String RANGE_COMMENT = NS + "rangeComment";
	public static String UNIVERSE_SIGNATURE = NS + "universeSignature";
	public static String UNIVERSE_FINGERPRINT = NS + "universeFingerprint";
	public static String DOMAIN_CONTROLLED_VOCABULARY = NS + "domainControlledVocabulary";
	public static String PROPERTY_CONTROLLED_VOCABULARY = NS + "propertyControlledVocabulary";
	public static String RANGE_CONTROLLED_VOCABULARY = NS + "rangeControlledVocabulary";
	public static String UNIVERSE_DOMAIN = NS + "universeDomain";
	public static String UNIVERSE_PROPERTY = NS + "universeProperty";
	public static String UNIVERSE_RANGE= NS + "universeRange";
	public static String IS_DOMAIN_OF_UNIVERSE = NS + "isDomainOfUniverse";
	public static String IS_RANGE_OF_UNIVERSE = NS + "isRangeOfUniverse";
	public static String UNIVERSE = NS + "Universe";
	public static String CONTEXT_ID = NS + "contextId";
	public static String HAS_CONTEXT = NS + "hasContext";
	
	public static Property domainClassLabel = ResourceFactory.createProperty(DOMAIN_CLASS_LABEL);
	public static Property domainClassComment = ResourceFactory.createProperty(DOMAIN_CLASS_COMMENT);
	public static Property synonym = ResourceFactory.createProperty(SYNONYM);
	public static Property usage = ResourceFactory.createProperty(USAGE);
	public static Property domainUsage = ResourceFactory.createProperty(DOMAIN_USAGE);
	public static Property rangeUsage = ResourceFactory.createProperty(RANGE_USAGE);
	public static Property fullUsage = ResourceFactory.createProperty(FULL_USAGE);
	public static Property ontologyId = ResourceFactory.createProperty(ONTOLOGY_ID);
	public static Property ontologyLabel = ResourceFactory.createProperty(ONTOLOGY_LABEL);
	public static Property domainLabel = ResourceFactory.createProperty(DOMAIN_LABEL);
	public static Property propertyLabel = ResourceFactory.createProperty(PROPERTY_LABEL);
	public static Property rangeLabel = ResourceFactory.createProperty(RANGE_LABEL);
	public static Property ontologyComment = ResourceFactory.createProperty(ONTOLOGY_COMMENT);
	public static Property domainComment = ResourceFactory.createProperty(DOMAIN_COMMENT);
	public static Property propertyComment = ResourceFactory.createProperty(PROPERTY_COMMENT);
	public static Property rangeComment = ResourceFactory.createProperty(RANGE_COMMENT);
	public static Resource context = ResourceFactory.createResource(CONTEXT);
	public static Resource universe = ResourceFactory.createResource(UNIVERSE);
	public static Property universeSignature = ResourceFactory.createProperty(UNIVERSE_SIGNATURE);
	public static Property universeFingerprint = ResourceFactory.createProperty(UNIVERSE_FINGERPRINT);
	public static Property definedInOntology = ResourceFactory.createProperty(DEFINED_IN_ONTOLOGY);
	public static Property domainControlledVocabulary = ResourceFactory.createProperty(DOMAIN_CONTROLLED_VOCABULARY);
	public static Property propertyControlledVocabulary = ResourceFactory.createProperty(PROPERTY_CONTROLLED_VOCABULARY);
	public static Property rangeControlledVocabulary = ResourceFactory.createProperty(RANGE_CONTROLLED_VOCABULARY);
	public static Property isDomainOfUniverse = ResourceFactory.createProperty(IS_DOMAIN_OF_UNIVERSE);
	public static Property isRangeOfUniverse = ResourceFactory.createProperty(IS_RANGE_OF_UNIVERSE);
	public static Property universeDomain = ResourceFactory.createProperty(UNIVERSE_DOMAIN);
	public static Property universeProperty = ResourceFactory.createProperty(UNIVERSE_PROPERTY);
	public static Property universeRange = ResourceFactory.createProperty(UNIVERSE_RANGE);
	public static Property contextID = ResourceFactory.createProperty(CONTEXT_ID);
	public static Property hasContext = ResourceFactory.createProperty(HAS_CONTEXT);
	
	
}
