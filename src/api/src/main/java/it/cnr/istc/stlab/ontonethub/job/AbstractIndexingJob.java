package it.cnr.istc.stlab.ontonethub.job;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.stanbol.commons.jobs.api.Job;

public abstract class AbstractIndexingJob implements Job {

	protected String lemmatize(String query) {
		ItalianAnalyzer analyzer = new ItalianAnalyzer();
		TokenStream tokenStream = analyzer.tokenStream("label", query);
		
		
		StringBuilder sb = new StringBuilder();
		CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);
		try {
	    	tokenStream.reset();
			while (tokenStream.incrementToken()) {
			    if (sb.length() > 0) {
			        sb.append(" ");
			    }
			    sb.append(token.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
}
