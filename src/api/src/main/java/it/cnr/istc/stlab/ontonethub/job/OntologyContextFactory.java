package it.cnr.istc.stlab.ontonethub.job;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL2;

public class OntologyContextFactory {
	
	private static int MAX_DEPTH = 4;
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private List<String> serialize(int depth, OntClass ontClass, Map<OntClass,List<Statement>> graph, Set<OntClass> printed, StringBuilder sb, List<String> strings){
		
		printed.add(ontClass);
		
		StringBuilder sbCopy = new StringBuilder(sb.toString());
		List<Statement> stmts = graph.get(ontClass);
		if(stmts != null){
			for(Statement stmt : stmts){
				Property pred = stmt.getPredicate();
				RDFNode obj = stmt.getObject();
				sb.append(ontClass.getURI() + " . " + pred.getURI() + " . ");
				
		
				StringBuilder stateSb = new StringBuilder(sb.toString());
				OntClass rangeClass = ontClass.getOntModel().getOntClass(obj.asResource().getURI());
				if(!printed.contains(rangeClass) && (depth+1)<MAX_DEPTH)
					serialize(depth+1, rangeClass, graph, printed, stateSb, strings);
				 
				
				strings.add(sb.toString());
				sb = new StringBuilder(sbCopy.toString());
				
			}
		}
		
		return strings;
		
	}

	public Map<String,Set<String>> getContexts(OntModel ontModel) {
		Map<String, Set<String>> propIndexMap = new HashMap<String, Set<String>>();
		
		ExtendedIterator<OntClass> ontClasses = ontModel.listClasses();
		List<OntClass> classes = ontClasses.toList();
		
		int nClasses = classes.size();
		
		classes.parallelStream().forEach(ontClass -> {
			//counter++;
			log.info("Indexed {} classes out of {}.", ontClass, nClasses);
			if(ontClass.isAnon()){
				
			}
			else {
				
				
				Map<OntClass,List<Statement>> graph = new HashMap<OntClass,List<Statement>>();
				graph.put(ontClass, new ArrayList<Statement>());
				visit(ontClass, graph, new ArrayList<OntClass>());
				List<String> strings = new ArrayList<String>();
				serialize(0, ontClass, graph, new HashSet<OntClass>(), new StringBuilder(), strings);
				for(String string : strings){
					String path = string;
					int index = string.lastIndexOf(" . ");
					string = string.substring(0, index);
					index = string.lastIndexOf(" . ");
					String prop = string.substring(index+2).trim();
					
					Set<String> paths = propIndexMap.get(prop);
					if(paths == null){
						paths = new HashSet<String>();
						propIndexMap.put(prop, paths);
					}
					paths.add(path);
				}
			}
		});
		
		/*
		for(OntClass ontClass : classes) {
			counter++;
			log.info("Indexed {} classes out of {}.", counter, nClasses);
			if(ontClass.isAnon()){
				
			}
			else {
				
				
				Map<OntClass,List<Statement>> graph = new HashMap<OntClass,List<Statement>>();
				graph.put(ontClass, new ArrayList<Statement>());
				visit(ontClass, graph, new ArrayList<OntClass>());
				List<String> strings = new ArrayList<String>();
				serialize(0, ontClass, graph, new HashSet<OntClass>(), new StringBuilder(), strings);
				for(String string : strings){
					String path = string;
					int index = string.lastIndexOf(" . ");
					string = string.substring(0, index);
					index = string.lastIndexOf(" . ");
					String prop = string.substring(index+2).trim();
					
					Set<String> paths = propIndexMap.get(prop);
					if(paths == null){
						paths = new HashSet<String>();
						propIndexMap.put(prop, paths);
					}
					paths.add(path);
				}
			}
		}
	*/
		
		
		return propIndexMap;
		
	}
	
	private static void visit(OntClass ontClass, Map<OntClass,List<Statement>> graph, List<OntClass> visited){
		
		OntModel ontModel = ontClass.getOntModel();
		String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "SELECT DISTINCT ?prop ?range "
				+ "WHERE{"
				+ "  {?prop rdfs:range/(<" + OWL2.unionOf + ">|<" + OWL2.intersectionOf + ">)* ?range; "
				+ "    rdfs:domain/(<" + OWL2.unionOf + ">|<" + OWL2.intersectionOf + ">)* <" + ontClass.getURI() + "> "
				+ "  }"
				+ "  UNION "
				+ "  { "
				+ "    <" + ontClass.getURI() + "> rdfs:subClassOf|owl:equivalentClass ?restriction . "
				+ "    ?restriction a owl:Restriction; "
				+ "      ?rest ?range;"
				+ "      owl:onProperty ?prop "
				+ "  } "
				+ "  FILTER(isIRI(?range)) "
				+ "}"
				+ "ORDER BY ?domain ?range ";
		Query query = QueryFactory.create(sparql, Syntax.syntaxSPARQL_11);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, ontModel);
		
		
		List<Statement> stmts = graph.get(ontClass);
		ResultSet resultSet = queryExecution.execSelect();
		while(resultSet.hasNext()){
			QuerySolution querySolution = resultSet.next();
			Resource prop = querySolution.getResource("prop");
			Resource range = querySolution.getResource("range");
			Statement stmt = new StatementImpl(ontClass, ResourceFactory.createProperty(prop.getURI()), range);
			stmts.add(stmt);
			//stmtsNew.add(stmt);
			OntClass rangeClass = ontModel.getOntClass(range.getURI());
			
			OntProperty ontProperty = ontModel.getOntProperty(prop.getURI());
			if(ontProperty.isObjectProperty() && !visited.contains(rangeClass) && rangeClass != null){
				List<OntClass> visitedNew = new ArrayList<OntClass>();
				visitedNew.addAll(visited);
				visitedNew.add(rangeClass);
				graph.put(rangeClass, new ArrayList<Statement>());
				visit(rangeClass, graph, visitedNew);
			}
		}
		
		 
				
		
	}
	
}
