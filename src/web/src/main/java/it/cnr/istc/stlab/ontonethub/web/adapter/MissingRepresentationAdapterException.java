package it.cnr.istc.stlab.ontonethub.web.adapter;

public class MissingRepresentationAdapterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6063597235751729594L;
	private Class<?> type;
	
	public MissingRepresentationAdapterException(Class<?> type) {
		this.type = type;
	}
	
	@Override
	public String getMessage() {
		return "No adapter found for the individual of class " + type;
	}

}
