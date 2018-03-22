package it.cnr.istc.stlab.ontonethub.web.adapter;

import java.util.HashMap;
import java.util.Map;

public class RepresentationAdapterFactory {
	
	private static Map<Class<?>, RepresentationAdapter> representationAdapterRegistry = new HashMap<Class<?>, RepresentationAdapter>();

	public static <T> RepresentationAdapter getAdapter(Class<T> c) throws MissingRepresentationAdapterException {
		
		
		RepresentationAdapter representationAdapter = representationAdapterRegistry.get(c);
		if(representationAdapter == null) throw new MissingRepresentationAdapterException(c);
		else return representationAdapter;
		
	}
	
	public static <T> void registerAdapter(RepresentationAdapter representationAdapter, Class<T> c){
		representationAdapterRegistry.put(c, representationAdapter);
	}
	
	public static <T> void unregisterAdapter(Class<T> c){
		representationAdapterRegistry.remove(c);
	}
	
}
