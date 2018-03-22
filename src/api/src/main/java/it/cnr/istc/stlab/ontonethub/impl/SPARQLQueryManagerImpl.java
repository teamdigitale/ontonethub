package it.cnr.istc.stlab.ontonethub.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;

import it.cnr.istc.stlab.ontonethub.SPARQLQueryManager;

/**
 * Default implementation of the {@link SPARQLQueryManager}.<br>
 * Such an implementation provide an OSGi component for the SPARQLQueryManager.
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */

@Component(immediate = true)
@Service(SPARQLQueryManager.class)
public class SPARQLQueryManagerImpl implements SPARQLQueryManager {

	private String sparqlQuery;
	private static final String _USAGE_QUERY_DEFAULT_ = "";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private ComponentContext ctx;
	
	protected void activate(ComponentContext ctx) throws ConfigurationException, FileNotFoundException, IOException {
		this.ctx = ctx;
		
		Bundle bundle = ctx.getBundleContext().getBundle();
		
		URL usageEntryPath = bundle.getEntry("sparql_queries/ontology_indexing_query.sparql");
		if(usageEntryPath != null){
			InputStream is = usageEntryPath.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null){
				sb.append(line);
				sb.append('\n');
			}
			reader.close();
			is.close();
			
			sparqlQuery = sb.toString();
		}
		else sparqlQuery = _USAGE_QUERY_DEFAULT_;
		
		
		log.info("Activated " + getClass());
	}
	
	protected void deactivate(ComponentContext ctx) throws IOException {
		this.ctx = null;
		log.info("Deactivated " + getClass());
	}

	@Override
	public ResultSet executeSelect(Model ontology) {
		Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxSPARQL_11);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, ontology);
		return queryExecution.execSelect();
	}
	
}
