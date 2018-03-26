package it.cnr.istc.stlab.ontonethub.job;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public static Model create(String ontologyId, String ontologyName, Resource domain, Property property, Resource range){
		
		Model model = ModelFactory.createDefaultModel();
		Model sourceModel = domain.getModel();
		StmtIterator it = sourceModel.listStatements(null, RDF.type, OWL.Ontology);
		if(it.hasNext()){
			Statement stmt = it.next();
			Resource ontology = stmt.getSubject();
			
			List<Literal> ontologyLabels = listLiteralPropertyValues(ontology, RDFS.label);
			List<Literal> ontologyComments = listLiteralPropertyValues(ontology, RDFS.comment);
			
			
			
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
			String contextId = ontologyName + "." + domainLocalname + "." + propertyLocalname + "." + rangeLocalname;
			
			
			
			String contextIdHex = null;
			/*
			try {
				contextIdHex = (new HexBinaryAdapter()).marshal(MessageDigest.getInstance("MD5").digest((domainLocalname + "." + propertyLocalname + "." + rangeLocalname).getBytes()));
			} catch (NoSuchAlgorithmException e) {
				contextIdHex = domainLocalname + "." + propertyLocalname + "." + rangeLocalname;
			}
			*/
			contextIdHex = contextId;
			
			Resource context = ResourceFactory.createResource(property.getNameSpace() + contextIdHex);
			
			model.add(context, RDF.type, OntologyDescriptionVocabulary.context);
			model.add(context, OntologyDescriptionVocabulary.universeDomain, domain);
			model.add(domain, OntologyDescriptionVocabulary.isDomainOfUniverse, context);
			model.add(domain, RDF.type, OWL2.Class);
			model.add(context, OntologyDescriptionVocabulary.universeProperty, property);
			model.add(context, OntologyDescriptionVocabulary.universeRange, range);
			model.add(range, OntologyDescriptionVocabulary.isRangeOfUniverse, context);
			model.add(range, RDF.type, OWL2.Class);
			model.add(context, OntologyDescriptionVocabulary.definedInOntology, ontology);
			model.add(context, OntologyDescriptionVocabulary.universeSignature, ResourceFactory.createPlainLiteral(contextId));
			model.add(context, OntologyDescriptionVocabulary.universeFingerprint, ResourceFactory.createPlainLiteral(contextIdHex));
			
			model.add(context, OntologyDescriptionVocabulary.ontologyId, ResourceFactory.createPlainLiteral(ontologyId));
			
			/* LABELS */
			for(Literal literal : ontologyLabels)
				model.add(context, OntologyDescriptionVocabulary.ontologyLabel, literal);
			
			if(domainLabels.size() == 0)
				model.add(context, OntologyDescriptionVocabulary.domainLabel, domainLocalname);
			else{
				for(Literal literal : domainLabels)
					model.add(context, OntologyDescriptionVocabulary.domainLabel, literal);
			}
			if(propertyLabels.size() == 0)
				model.add(context, OntologyDescriptionVocabulary.propertyLabel, propertyLocalname);
			else{
				for(Literal literal : propertyLabels)
					model.add(context, OntologyDescriptionVocabulary.propertyLabel, literal);
			}
			if(rangeLabels.size() == 0)
				model.add(context, OntologyDescriptionVocabulary.rangeLabel, rangeLocalname);
			else{
				for(Literal literal : rangeLabels)
					model.add(context, OntologyDescriptionVocabulary.rangeLabel, literal);
			}
			
			/* COMMENTS */
			for(Literal literal : ontologyComments)
				model.add(context, OntologyDescriptionVocabulary.ontologyComment, literal);
			for(Literal literal : listLiteralPropertyValues(domain, RDFS.comment))
				model.add(context, OntologyDescriptionVocabulary.domainComment, literal);
			for(Literal literal : listLiteralPropertyValues(property, RDFS.comment))
				model.add(context, OntologyDescriptionVocabulary.propertyComment, literal);
			for(Literal literal : listLiteralPropertyValues(range, RDFS.comment))
				model.add(context, OntologyDescriptionVocabulary.rangeComment, literal);
			
			/* CONTROLLED VOCABULARIES */
			StmtIterator stmtIt = domain.listProperties(L0_CONTROLLED_VOCABULARY);
			while(stmtIt.hasNext()){
				Statement s = stmtIt.next();
				model.add(context, OntologyDescriptionVocabulary.domainControlledVocabulary, s.getObject());
			}
			stmtIt = property.listProperties(L0_CONTROLLED_VOCABULARY);
			while(stmtIt.hasNext()){
				Statement s = stmtIt.next();
				model.add(context, OntologyDescriptionVocabulary.propertyControlledVocabulary, s.getObject());
			}
			stmtIt = range.listProperties(L0_CONTROLLED_VOCABULARY);
			while(stmtIt.hasNext()){
				Statement s = stmtIt.next();
				model.add(context, OntologyDescriptionVocabulary.rangeControlledVocabulary, s.getObject());
			}
			
			/* SYNONYMS */
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
				
				for(Literal domainSyn : domainSynonyms){
					String domainSynLang = domainSyn.getLanguage();
					String domainSynValue = domainSyn.getLexicalForm();
					
					if(propSynLang.equals(domainSynLang)){
						foundDomainLabel = true;
						String propDomainValue = propSynValue + " " + domainSynValue;
						
						boolean foundRangeLabel = false;
						for(Literal rangeSyn : rangeSynonyms){
							String rangeSynLang = rangeSyn.getLanguage();
							String rangeSynValue = rangeSyn.getLexicalForm();
							if(domainSynLang.equals(rangeSynLang)){
								foundRangeLabel = true;
								
								values.add(propDomainValue + " " + rangeSynValue);
							}
						}
						if(!foundRangeLabel)
							values.add(propDomainValue);
					}
					
				}
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
					
					model.add(context, OntologyDescriptionVocabulary.synonym, literal);
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
	
}
