package it.cnr.istc.stlab.ontonethub;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL2;

public class ReadOntology {
	
	private static int MAX_DEPTH = 4;
	private static List<String> serialize(int depth, OntClass ontClass, Map<OntClass,List<Statement>> graph, Set<OntClass> printed, StringBuilder sb, List<String> strings){
		
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

	public static void main(String[] args) {
		//OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		ontModel.read("https://w3id.org/italia/onto/Cultural-ON");
		
		Map<String, Set<String>> propIndexMap = new HashMap<String, Set<String>>();
		
		ExtendedIterator<OntClass> ontClasses = ontModel.listClasses();
		ontClasses.forEachRemaining(ontClass -> {
			if(ontClass.isAnon()){
				System.out.println("Is Anon " + ontClass.getClass() + ": " + ontClass.isUnionClass() + " _- " + ontClass.isIntersectionClass() + " -_ " + ontClass.isComplementClass() + " __ " + ontClass.isRestriction());
				
			}
			//else if(ontClass.getURI().equals("https://w3id.org/italia/onto/CPV/Person")){
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
		for(String prop : propIndexMap.keySet()){
			System.out.println(prop);
			for(String path : propIndexMap.get(prop)){
				
				StringBuilder ctxIdSb = new StringBuilder();
				for(String stPart : path.split(" \\. ")){
					stPart = stPart.trim();
					Resource stPartResource = ResourceFactory.createProperty(stPart);
					if(ctxIdSb.length() > 0) ctxIdSb.append(".");
					ctxIdSb.append(stPartResource.getLocalName());
				}
				System.out.println('\t' + ctxIdSb.toString());
				
				int index = path.indexOf(" .");
				String contextClass = path.substring(0, index).trim();
				System.out.println('\t' + '\t' + contextClass);
			}
			
		}
		*/
		
		
		/*
		while(!resources.isEmpty()){
			Resource c = resources.remove(0);
			if(!c.equals(OWL2.Thing) && !visited.contains(c)){
				System.out.println(c);	
				visited.add(c);
				resources.addAll(query(c, ontModel));
			}
			
		}
		*/
		
		
		
	}
	
	private static List<Resource> query(Resource concept, OntModel model, Set<Resource> visited){
		String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "SELECT DISTINCT ?concept ?prop "
				+ "WHERE{"
				+ "  {?prop rdfs:range <" + concept.getURI() + ">; "
				+ "    rdfs:domain ?concept"
				+ "  }"
				+ "  UNION "
				+ "  { "
				+ "    ?concept rdfs:subClassOf|owl:equivalentClass ?restriction . "
				+ "    ?restriction a owl:Restriction; "
				+ "      ?restr <" + concept.getURI() + ">;"
				+ "      owl:onProperty ?prop"
				+ "  } "
				+ "  FILTER(isIRI(?concept)) "
				+ "  FILTER(?prop != <http://www.w3.org/2002/07/owl#bottomObjectProperty>)"
				+ "}";
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		
		ResultSet resultSet = queryExecution.execSelect();
		List<Resource> resources = new ArrayList<Resource>();
		while(resultSet.hasNext()){
			QuerySolution querySolution = resultSet.next();
			Resource c = querySolution.getResource("concept");
			Resource p = querySolution.getResource("prop");
			
			
			//resources.add(c);
			
			if(!c.equals(OWL2.Thing) && !visited.contains(c)){
				System.out.println(c);
				System.out.println('\t' + p.getURI() + " " + concept.getURI());
				visited.add(c);
				query(c, model, visited);
			}
			
		}
		
		return resources;
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
	
	private static void query(OntModel model){
		
		Map<Property, Map<Resource, Model>> map = new HashMap<Property, Map<Resource, Model>>();
		
		ExtendedIterator<OntProperty> it = model.listAllOntProperties();
		while(it.hasNext()){
			OntProperty prop = it.next();
			if(!prop.equals(OWL2.bottomObjectProperty) && !prop.equals(OWL2.bottomDataProperty)){
				System.out.println(prop);
				String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
						+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
						+ "SELECT DISTINCT ?domain ?range "
						+ "WHERE{"
						+ "  {<" + prop.getURI() + "> rdfs:range/(<" + OWL2.unionOf + ">|<" + OWL2.intersectionOf + ">)* ?range; "
						+ "    rdfs:domain/(<" + OWL2.unionOf + ">|<" + OWL2.intersectionOf + ">)* ?domain"
						+ "  }"
						+ "  UNION "
						+ "  { "
						+ "    ?domain rdfs:subClassOf|owl:equivalentClass ?restriction . "
						+ "    ?restriction a owl:Restriction; "
						+ "      ?rest ?range;"
						+ "      owl:onProperty <" + prop.getURI() + ">"
						+ "  } "
						+ "  FILTER(isIRI(?domain)) "
						+ "  FILTER(isIRI(?range)) "
						+ "}"
						+ "ORDER BY ?domain ?range ";
				Query query = QueryFactory.create(sparql, Syntax.syntaxSPARQL_11);
				QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
				
				
				ResultSet resultSet = queryExecution.execSelect();
				
				while(resultSet.hasNext()){
					QuerySolution querySolution = resultSet.next();
					Resource domain = querySolution.getResource("domain");
					Resource range = querySolution.getResource("range");
					//Resource r = querySolution.getResource("range");
					
					
					//System.out.println('\t' + domain.getURI() + " - " + prop + " - " + range);
					
					Map<Resource,Model> universeMap = map.get(prop);
					if(universeMap == null){
						universeMap = new HashMap<Resource,Model>();
						map.put(prop, universeMap);
					}
					
					Model tmp = universeMap.get(domain);
					if(tmp == null){
						tmp = ModelFactory.createDefaultModel();
						universeMap.put(domain, tmp);
					}
					tmp.add(domain, prop, range);
					
					
				}
			}
			
			
		}
		
		Set<Property> props = map.keySet();
		for(Property prop : props){
			Map<Resource,Model> universeMap = map.get(prop);
			String sparql = "SELECT DISTINCT ?top "
					+ "WHERE{"
					+ " ?top ?p ?o "
					+ " OPTIONAL{ ?sup ?p1 ?top}"
					+ " FILTER(!BOUND(?sup))"
					+ "}";
			
			for(Model m : universeMap.values()){
				Query query = QueryFactory.create(sparql, Syntax.syntaxSPARQL_11);
				QueryExecution queryExecution = QueryExecutionFactory.create(query, m);
				ResultSetFormatter.out(System.out, queryExecution.execSelect());
			}
			
		}
		
	}
	
}
