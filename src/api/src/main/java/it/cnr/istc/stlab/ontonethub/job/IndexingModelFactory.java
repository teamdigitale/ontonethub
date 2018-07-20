package it.cnr.istc.stlab.ontonethub.job;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary;

public class IndexingModelFactory {
	
	private static Logger log = LoggerFactory.getLogger(IndexingModelFactory.class);

	private static Property L0_CONTROLLED_VOCABULARY = ResourceFactory.createProperty("https://w3id.org/italia/onto/l0/controlledVocabulary");
	private Map<String, Set<String>> propContextMap;
	
	private Map<String, Onto> ontologyMapping = new HashMap<String, Onto>();
	

	
	public IndexingModelFactory(Map<String, Set<String>> propContextMap) {
		this.propContextMap = propContextMap;
		
		ontologyMapping.put("https://w3id.org/italia/onto/ACCO", 
				new Onto("https://w3id.org/italia/onto/ACCO", "Strutture ricettive", "Questa è l'ontologia del profilo italiano sulle Strutture Ricettive (Accomodation Facility - Italian Application Profile - ACCO-AP_IT)"));
		ontologyMapping.put("https://w3id.org/italia/onto/CLV", 
				new Onto("https://w3id.org/italia/onto/CLV", "Indirizzi e luoghi", "Questa è l'ontologia del profilo di italiano sugli indirizzi che hanno anche una connotazione geografica (Italian Core Location Vocabulary - Application Profile - CLV-AP_IT)"));
		ontologyMapping.put("https://w3id.org/italia/onto/COV", 
				new Onto("https://w3id.org/italia/onto/COV", "Organizzazioni (private e pubbliche)", "Questa è l'ontologia del profilo applicativo italiano sulle organizzazioni (pubbliche e private)"));
		ontologyMapping.put("https://w3id.org/italia/onto/CPEV", 
				new Onto("https://w3id.org/italia/onto/CPEV", "Eventi Pubblici", "Questa è l'ontologia del profilo applicativo italiano sugli eventi pubblici"));
		ontologyMapping.put("https://w3id.org/italia/onto/CPV", 
				new Onto("https://w3id.org/italia/onto/CPV", "Ontologia delle persone (individui)", "Questa è l'ontologia del profilo applicativo italiano sulle persone."));
		ontologyMapping.put("https://w3id.org/italia/onto/Cultural-ON", 
				new Onto("https://w3id.org/italia/onto/Cultural-ON", "Cultural-ON (Cultural ONtology): Ontologia dei Luoghi della Cultura e degli Eventi Culturali", "Cultural-ON (Cultural ONtology): Ontologia dei Luoghi della Cultura e degli Eventi Culturali."));
		ontologyMapping.put("https://w3id.org/italia/onto/IoT", 
				new Onto("https://w3id.org/italia/onto/IoT", "Ontologia per eventi IoT", "Questa è l'ontologia di eventi IoT. Essa può essere utilizzata per esempio per rappresentare serie temporali e misure di sensoristica di vario tipo. Attualmente l'ontologia include classi specializzate per la modellazione di flussi di traffico."));
		ontologyMapping.put("https://w3id.org/italia/onto/l0", 
				new Onto("https://w3id.org/italia/onto/l0", "Ontologia Level-0", "Questa ontologia fornisce il livello fondazionale allo stack ontologico di OntoPiA. Il nome L0 sta per Level-0 ontology poiché essa fornisce le fondamenta concettuali elementari all'intero stack. Questa ontologia è ispirata da DOLCE zero (http://www.ontologydesignpatterns.org/ont/d0.owl)."));
		ontologyMapping.put("https://w3id.org/italia/onto/MU", 
				new Onto("https://w3id.org/italia/onto/MU", "Ontologia per le unità di misura", "Questa è l'ontologia per la modellazione di valori e unità di misura."));
		ontologyMapping.put("https://w3id.org/italia/onto/PARK", 
				new Onto("https://w3id.org/italia/onto/PARK", "Ontologia dei Parcheggi", "Questa è l'ontologia del profilo applicativo italiano per i dati sui parcheggi."));
		ontologyMapping.put("https://w3id.org/italia/onto/POI", 
				new Onto("https://w3id.org/italia/onto/POI", "Punti di Interesse", "Questa è l'ontologia del profilo italiano sui Punti di Interesse (Point of Interest - Italian Application Profile - POI-AP_IT)."));
		ontologyMapping.put("https://w3id.org/italia/onto/POT", 
				new Onto("https://w3id.org/italia/onto/POT", "Prezzi/Offerte/Biglietti", "Questa è l'ontologia per Prezzi/Offerte/Biglietti."));
		ontologyMapping.put("https://w3id.org/italia/onto/RO", 
				new Onto("https://w3id.org/italia/onto/RO", "Ontologia dei Ruoli", "Questa è l'ontologia per la modellazione dei ruoli."));
		ontologyMapping.put("https://w3id.org/italia/onto/SM", 
				new Onto("https://w3id.org/italia/onto/SM", "Ontologia per i social media", "L'ontologia per i social media e i punti di contatto online. Questa ontologia permette di rappresentare social network, account, indirizzi email, loghi, ecc..."));
		ontologyMapping.put("https://w3id.org/italia/onto/TI", 
				new Onto("https://w3id.org/italia/onto/TI", "Ontologia del tempo - profilo applicativo italiano", "Questa è l'ontologia del profilo applicativo italiano sul tempo."));
	}
	
	public Model create(String ontologyId, String ontologyName, Resource domain, Property property, Resource range){
		
		Model model = ModelFactory.createDefaultModel();
		Model sourceModel = domain.getModel();
		StmtIterator it = sourceModel.listStatements(null, RDF.type, OWL.Ontology);
		if(it.hasNext()){
			Statement stmt = it.next();
			Resource ontology = stmt.getSubject();
			
			//List<Literal> ontologyLabels = listLiteralPropertyValues(ontology, RDFS.label);
			//List<Literal> ontologyComments = listLiteralPropertyValues(ontology, RDFS.comment);
			
			
			
			List<Literal> domainLabels = listLiteralPropertyValues(domain, RDFS.label);
			List<Literal> propertyLabels = listLiteralPropertyValues(property, RDFS.label);
			List<Literal> rangeLabels = listLiteralPropertyValues(range, RDFS.label);
			
			List<Literal> domainSynonyms = listLiteralPropertyValues(domain, OntologyDescriptionVocabulary.synonym);
			List<Literal> propertySynonyms = listLiteralPropertyValues(property, OntologyDescriptionVocabulary.synonym);
			List<Literal> rangeSynonyms = listLiteralPropertyValues(range, OntologyDescriptionVocabulary.synonym);
			
			String domainLocalname = domain.getLocalName();
			String propertyLocalname = property.getLocalName();
			String rangeLocalname = range.getLocalName();
			
			//String contextId = property.getNameSpace() + 
			//String universeId = ontologyName + "." + domainLocalname + "." + propertyLocalname + "." + rangeLocalname;
			String universeId = domainLocalname + "." + propertyLocalname + "." + rangeLocalname;
			
			
			
			String universeIdHex = null;
			/*
			try {
				contextIdHex = (new HexBinaryAdapter()).marshal(MessageDigest.getInstance("MD5").digest((domainLocalname + "." + propertyLocalname + "." + rangeLocalname).getBytes()));
			} catch (NoSuchAlgorithmException e) {
				contextIdHex = domainLocalname + "." + propertyLocalname + "." + rangeLocalname;
			}
			*/
			universeIdHex = universeId;
			
			/*
			 * TEST
			 * Resource universe = ResourceFactory.createResource(property.getNameSpace() + universeIdHex);
			 */
			Resource universe = ResourceFactory.createResource("https://w3id.org/italia/onto/" + universeIdHex);
			/* 
			 * End TEST
			 */
			
			model.add(universe, RDF.type, OntologyDescriptionVocabulary.universe);
			model.add(universe, OntologyDescriptionVocabulary.universeDomain, domain);
			model.add(domain, OntologyDescriptionVocabulary.isDomainOfUniverse, universe);
			model.add(domain, RDF.type, OWL2.Class);
			model.add(universe, OntologyDescriptionVocabulary.universeProperty, property);
			model.add(universe, OntologyDescriptionVocabulary.universeRange, range);
			model.add(range, OntologyDescriptionVocabulary.isRangeOfUniverse, universe);
			model.add(range, RDF.type, OWL2.Class);
			//model.add(universe, OntologyDescriptionVocabulary.definedInOntology, ontology);
			model.add(universe, OntologyDescriptionVocabulary.universeSignature, ResourceFactory.createPlainLiteral(universeId));
			model.add(universe, OntologyDescriptionVocabulary.universeFingerprint, ResourceFactory.createPlainLiteral(universeIdHex));
			
			model.add(universe, OntologyDescriptionVocabulary.ontologyId, ResourceFactory.createPlainLiteral(ontologyId));
			
			Set<String> contexts = propContextMap.get(property.getURI());
			if(contexts != null){
				for(String context : contexts){
					String contextIdHex;
					try {
						contextIdHex = (new HexBinaryAdapter()).marshal(MessageDigest.getInstance("MD5").digest(context.getBytes()));
					} catch (NoSuchAlgorithmException e) {
						contextIdHex = context;
					}
					Resource contextResource = ResourceFactory.createResource(property.getNameSpace() + contextIdHex);
					
					model.add(universe, OntologyDescriptionVocabulary.hasContext, contextResource);
					model.add(contextResource, RDF.type, OntologyDescriptionVocabulary.context);
					
					
					StringBuilder ctxIdSb = new StringBuilder();
					for(String stPart : context.split(" \\. ")){
						stPart = stPart.trim();
						Resource stPartResource = ResourceFactory.createProperty(stPart);
						if(ctxIdSb.length() > 0) ctxIdSb.append(".");
						ctxIdSb.append(stPartResource.getLocalName());
					}
					
					model.add(contextResource, OntologyDescriptionVocabulary.contextID, ctxIdSb.toString());
					
					int index = context.indexOf(" .");
					String contextClass = context.substring(0, index).trim();
					Resource contextClassResource = sourceModel.getResource(contextClass);
					StmtIterator propLbs = property.listProperties(RDFS.label);
					StmtIterator ctxLbs = contextClassResource.listProperties(RDFS.label);
					Set<Literal> probLabels = new HashSet<Literal>();
					Set<Literal> dmnLabels = new HashSet<Literal>();
					
					propLbs.forEachRemaining(propLb -> {
						Literal propLbLiteral = propLb.getObject().asLiteral();
						probLabels.add(propLbLiteral);
					});
					ctxLbs.forEachRemaining(ctxLb -> {
						Literal ctxLbLiteral = ctxLb.getObject().asLiteral();
						dmnLabels.add(ctxLbLiteral);
					});
					for(Literal probLabel : probLabels){
						for(Literal dmnLabel : dmnLabels){
							String probLabelLang = probLabel.getLanguage();
							String dmnLabelLang = dmnLabel.getLanguage();
							Literal contextLabel = null;
							if(probLabelLang.equals(dmnLabelLang)){
								String contextLabelString = null;
								if(probLabelLang.equals("it")){
									contextLabelString = "\"" + probLabel.getLexicalForm() + "\" riconducibile a \"" + dmnLabel.getLexicalForm() + "\"";
								}
								else {
									contextLabelString = "\"" + probLabel.getLexicalForm() + "\" associated with \"" + dmnLabel.getLexicalForm() + "\"";
								}
								contextLabel = ResourceFactory.createLangLiteral(contextLabelString, probLabelLang);
								
								model.add(contextResource, RDFS.label, contextLabel);
							}
							
						}
					}
				}
			}
			
			String ontologyURI = property.getNameSpace();
			ontologyURI = ontologyURI.substring(0, ontologyURI.length()-1);
			Onto onto = ontologyMapping.get(ontologyURI);
			
			List<Literal> ontologyLabels = new ArrayList<Literal>();
			List<Literal> ontologyComments = new ArrayList<Literal>();
			
			if(onto != null){
				Literal labelLiteral = ResourceFactory.createLangLiteral(onto.label, "it");
				Literal commentLiteral = ResourceFactory.createLangLiteral(onto.comment, "it");
				
				ontologyLabels.add(labelLiteral);
				ontologyComments.add(commentLiteral);
				model.add(universe, OntologyDescriptionVocabulary.definedInOntology, ResourceFactory.createResource(ontologyURI));
			}
			
			/* LABELS */
			for(Literal literal : ontologyLabels)
				model.add(universe, OntologyDescriptionVocabulary.ontologyLabel, literal);
			
			if(domainLabels.size() == 0)
				model.add(universe, OntologyDescriptionVocabulary.domainLabel, domainLocalname);
			else{
				for(Literal literal : domainLabels)
					model.add(universe, OntologyDescriptionVocabulary.domainLabel, literal);
			}
			if(propertyLabels.size() == 0)
				model.add(universe, OntologyDescriptionVocabulary.propertyLabel, propertyLocalname);
			else{
				for(Literal literal : propertyLabels)
					model.add(universe, OntologyDescriptionVocabulary.propertyLabel, literal);
			}
			if(rangeLabels.size() == 0)
				model.add(universe, OntologyDescriptionVocabulary.rangeLabel, rangeLocalname);
			else{
				for(Literal literal : rangeLabels)
					model.add(universe, OntologyDescriptionVocabulary.rangeLabel, literal);
			}
			
			/* COMMENTS */
			for(Literal literal : ontologyComments)
				model.add(universe, OntologyDescriptionVocabulary.ontologyComment, literal);
			for(Literal literal : listLiteralPropertyValues(domain, RDFS.comment))
				model.add(universe, OntologyDescriptionVocabulary.domainComment, literal);
			for(Literal literal : listLiteralPropertyValues(property, RDFS.comment))
				model.add(universe, OntologyDescriptionVocabulary.propertyComment, literal);
			for(Literal literal : listLiteralPropertyValues(range, RDFS.comment))
				model.add(universe, OntologyDescriptionVocabulary.rangeComment, literal);
			
			/* CONTROLLED VOCABULARIES */
			StmtIterator stmtIt = domain.listProperties(L0_CONTROLLED_VOCABULARY);
			while(stmtIt.hasNext()){
				Statement s = stmtIt.next();
				model.add(universe, OntologyDescriptionVocabulary.domainControlledVocabulary, s.getObject());
			}
			stmtIt = property.listProperties(L0_CONTROLLED_VOCABULARY);
			while(stmtIt.hasNext()){
				Statement s = stmtIt.next();
				model.add(universe, OntologyDescriptionVocabulary.propertyControlledVocabulary, s.getObject());
			}
			stmtIt = range.listProperties(L0_CONTROLLED_VOCABULARY);
			while(stmtIt.hasNext()){
				Statement s = stmtIt.next();
				model.add(universe, OntologyDescriptionVocabulary.rangeControlledVocabulary, s.getObject());
			}
			
			/* SYNONYMS */
			
			/*
			for(Literal propertySyn : propertySynonyms)
			model.add(universe, OntologyDescriptionVocabulary.synonym, propertySyn);
			*/
			
			
			Map<String, List<String>> synonymsMap = new HashMap<String, List<String>>();
			for(Literal propertySyn : propertySynonyms){
				String propSynLang = propertySyn.getLanguage();
				String propSynValue = propertySyn.getLexicalForm();
				
				String langKey = propSynLang != null ? propSynLang : "null";
				List<String> values = synonymsMap.get(langKey);
				if(values == null){
					values = new ArrayList<String>();
					synonymsMap.put(langKey, values);
				}
				
				boolean foundDomainLabel = false;
				
				/*
				for(Literal domainSyn : domainSynonyms){
					String domainSynLang = domainSyn.getLanguage();
					String domainSynValue = domainSyn.getLexicalForm();
					
					if(propSynLang.equals(domainSynLang)){
						foundDomainLabel = true;
						String propDomainValue = propSynValue + " " + domainSynValue;
						
						boolean foundRangeLabel = false;
						/*
						for(Literal rangeSyn : rangeSynonyms){
							String rangeSynLang = rangeSyn.getLanguage();
							String rangeSynValue = rangeSyn.getLexicalForm();
							if(domainSynLang.equals(rangeSynLang)){
								foundRangeLabel = true;
								
								values.add(propDomainValue + " " + rangeSynValue);
							}
						}
						*
						if(!foundRangeLabel)
							values.add(propDomainValue);
					}
					
				}
				*/
				if(!foundDomainLabel)
					values.add(propSynValue);
				
				
			}
			
			for(String lang : synonymsMap.keySet()){
				List<String> values = synonymsMap.get(lang);
				
				for(String value : values){
					Literal literal = null;
					if(lang == "null"){
						literal = ResourceFactory.createPlainLiteral(value);
					}
					else literal = ResourceFactory.createLangLiteral(value, lang);
					
					model.add(universe, OntologyDescriptionVocabulary.synonym, literal);
				}
			}
				
		}
		
		
		
		return model;
	}
	
	private static List<Literal> listLiteralPropertyValues(Resource obj, Property predicate){
		List<Literal> literals = new ArrayList<Literal>();
		StmtIterator stmtIterator = obj.listProperties(predicate);
		
		stmtIterator.forEachRemaining(stmt -> {
			RDFNode rdfNode = stmt.getObject();
			if(rdfNode.isLiteral()){
				literals.add(rdfNode.asLiteral());
			}
		});
		
		return literals;
	}
	
	private class Onto {
		
		private String id, label, comment;
		
		public Onto(String id, String label, String comment) {
			this.id = id;
			this.label = label;
			this.comment = comment;
		}
		
	}
	
}


