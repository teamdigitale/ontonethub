package it.cnr.istc.stlab.ontonethub.job;

import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.ANNOTATION_PROPERTIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.DATATYPE_PROPERTIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.HAS_BUNDLE;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.HAS_ONTOLOGY_IRI;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.HAS_ONTOLOGY_SOURCE;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.IMPORTED_ONTOLOGIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.INDIVIDUALS;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.OBJECT_PROPERTIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.ONTOLOGY;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.OWL_CLASSES;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

public class IndexDeploymentManager {

	private Logger log = LoggerFactory.getLogger(getClass());
	private String stanbolHome, baseURI;;
	private BundleContext ctx;
	private TcManager tcManager;
	
	public IndexDeploymentManager(String baseURI, TcManager tcManager, BundleContext ctx) {
		this.ctx = ctx;
		this.tcManager = tcManager;
		this.stanbolHome = ctx.getProperty("stanbol.home");
		this.baseURI = baseURI;
	}
	
	public void deploy(String ontologyName, String ontologyID, String ontologyDescription, Model data, File ontologiesFolder, File tempFolder){
		String bundleFileName = IndexingJob.BUNDLE_NAME_PATTERN.replace("{$name}", ontologyName);
		File bundleFile = new File(tempFolder, "indexing" + File.separator + "dist" + File.separator +  bundleFileName);
		
		String zippedIndexFileName = IndexingJob.ZIPPED_INDEX_NAME_PATTERN.replace("{$name}", ontologyName);
		File zippedIndexFile = new File(tempFolder, "indexing" + File.separator + "dist" + File.separator +  zippedIndexFileName);
		
		log.debug("bundleFile {}", bundleFile.getPath());
		if(bundleFile.exists() && zippedIndexFile.exists()){
			
			File stanbolDatafiles = new File(stanbolHome + File.separator + "datafiles");
			File deployedIndex = new File(stanbolDatafiles, zippedIndexFileName);
			
			try{
				Files.copy(zippedIndexFile, deployedIndex);
				
				log.debug("Bundle URI: {} - URL: {}", bundleFile.toURI(), bundleFile.toURI().toURL());
				Bundle bundle = ctx.installBundle(bundleFile.toURI().toString());
				bundle.start();
				long bundleId = bundle.getBundleId();
				
				IRI jobIRI = new IRI(ONTOLOGY + ontologyID);
				
				Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
				g.add(new TripleImpl(
						jobIRI,
						new IRI(HAS_BUNDLE),
						new PlainLiteralImpl(String.valueOf(bundleId))));
				
				
				/*
				 * Ontology name
				 */
				g.add(new TripleImpl(
						jobIRI,
						org.apache.clerezza.rdf.ontologies.RDFS.label,
						new PlainLiteralImpl(ontologyName)));
				
				/*
				 * Ontology ID to use to identify the site withing the entityhub 
				 */
				g.add(new TripleImpl(
						jobIRI,
						DC.identifier,
						new PlainLiteralImpl(ontologyName)));
				
				/*
				 * Ontology description
				 */
				g.add(new TripleImpl(
						jobIRI,
						DC.description,
						new PlainLiteralImpl(ontologyDescription)));
				
				/*
				 * Store ontology file
				 */
				File ontologyFile = new File(ontologiesFolder, ontologyID + "."
						+ "rdf");
				data.write(new FileOutputStream(ontologyFile));
				g.add(new TripleImpl(
						jobIRI,
						new IRI(HAS_ONTOLOGY_SOURCE),
						new IRI(ONTOLOGY + ontologyID + "/source")));
				
				log.debug("Writing ontology {} - {}", ontologyName, ontologyID);
				/*
				 * Ontology IRI
				 */
				g.add(new TripleImpl(
						jobIRI,
						new IRI(HAS_ONTOLOGY_IRI),
						new IRI(baseURI)));
				
				OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
				log.info("Base URI is {}", baseURI);
				ontModel.read(baseURI);
				
				/*
				 * Number of OWL classes
				 */
				
				int classCounter = 0;
				ExtendedIterator<OntClass> classesIt = ontModel.listClasses();
				while(classesIt.hasNext()){
					classesIt.next();
					classCounter++;
				}
				
				g.add(new TripleImpl(
						jobIRI,
						new IRI(OWL_CLASSES),
						new TypedLiteralImpl(String.valueOf(classCounter), XSD.int_)));
				
				/*
				 * Object properties
				 */
				int objectProperties = 0;
				ExtendedIterator<ObjectProperty> objPropertiesIt = ontModel.listObjectProperties();
				while(objPropertiesIt.hasNext()) {
					objPropertiesIt.next();
					objectProperties++;
				}
				
				g.add(new TripleImpl(
						jobIRI,
						new IRI(OBJECT_PROPERTIES),
						new TypedLiteralImpl(String.valueOf(objectProperties), XSD.int_)));
				
				/*
				 * Datatype properties
				 */
				int dataProperties = 0;
				ExtendedIterator<DatatypeProperty> dataPropertiesIt = ontModel.listDatatypeProperties();
				while(dataPropertiesIt.hasNext()) {
					dataPropertiesIt.next();
					dataProperties++;
				}
				
				g.add(new TripleImpl(
						jobIRI,
						new IRI(DATATYPE_PROPERTIES),
						new TypedLiteralImpl(String.valueOf(dataProperties), XSD.int_)));
				
				
				/*
				 * Annotation properties
				 */
				int annotationProperties = 0;
				ExtendedIterator<AnnotationProperty> annotationPropertiesIt = ontModel.listAnnotationProperties();
				while(annotationPropertiesIt.hasNext()) {
					annotationPropertiesIt.next();
					annotationProperties++;
				}
				
				g.add(new TripleImpl(
						jobIRI,
						new IRI(ANNOTATION_PROPERTIES),
						new TypedLiteralImpl(String.valueOf(annotationProperties), XSD.int_)));
				
				
				/*
				 * OWL individuals
				 */
				int individuals = 0;
				ExtendedIterator<Individual> individualsIt = ontModel.listIndividuals();
				while(individualsIt.hasNext()) {
					individualsIt.next();
					individuals++;
				}
				
				g.add(new TripleImpl(
						jobIRI,
						new IRI(INDIVIDUALS),
						new TypedLiteralImpl(String.valueOf(individuals), XSD.int_)));
				
				
				/*
				 * Closure of imported ontologies
				 */
				Set<String> importedOntologies = ontModel.listImportedOntologyURIs(true);
				g.add(new TripleImpl(
						jobIRI,
						new IRI(IMPORTED_ONTOLOGIES),
						new TypedLiteralImpl(String.valueOf(importedOntologies.size()), XSD.int_)));
				
				log.debug("Indexing job shoub be completed~");
				
				ontModel.listOntologies().forEachRemaining(ont -> {
					ExtendedIterator<Statement> stmts = ont.listProperties().filterDrop(new Filter<Statement>() {
						
						@Override
						public boolean accept(Statement o) {
							return o.getPredicate().equals(RDF.type) ? true : false;
						}
					});
					
					stmts.forEachRemaining(stmt -> {
						try{
							RDFNode objectNode = stmt.getObject();
							RDFTerm objectTerm = null;
							if(objectNode.isResource()) objectTerm = new IRI(((Resource)objectNode).getURI());
							else{
								Literal objectLiteral = (Literal)objectNode;
								String lexicalForm = objectLiteral.getLexicalForm();
								String type = objectLiteral.getDatatypeURI();
								if(type != null) objectTerm = new TypedLiteralImpl(lexicalForm, new IRI(type));
								else{
									String language = objectLiteral.getLanguage();
									if(language != null && !language.isEmpty()) objectTerm = new PlainLiteralImpl(lexicalForm, new Language(language));
									else objectTerm = new PlainLiteralImpl(lexicalForm);
								}
							}
							
							g.add(new TripleImpl(
									jobIRI,
									new IRI(stmt.getPredicate().getURI()),
									objectTerm));
						} catch(Exception e){
							log.info(e.getMessage(), e);
						}
								
					});
				});
			} catch(Exception e){
				
			}
			
		}
	}
	
	
}
