package org.eclipse.jdt.internal.ui.wizards.buildpaths;

/**
  */
public class CPListElementAttribute {
	
	public static final String K_SOURCEATTACHMENT= "sourcepath";
	public static final String K_SOURCEATTACHMENTROOT= "rootpath";
	public static final String K_JAVADOC= "javadoc";
	public static final String K_OUTPUT= "output";
	public static final String K_EXCLUSION= "exclusion";
	
	private CPListElement fParent;
	private String fKey;
	private Object fValue;
	
	public CPListElementAttribute(CPListElement parent, String key, Object value) {
		fKey= key;
		fValue= value;
		fParent= parent;
	}
	
	public CPListElement getParent() {
		return fParent;
	}

	/**
	 * Returns the key.
	 * @return String
	 */
	public String getKey() {
		return fKey;
	}

	/**
	 * Returns the value.
	 * @return Object
	 */
	public Object getValue() {
		return fValue;
	}
	
	/**
	 * Returns the value.
	 * @return Object
	 */
	public void setValue(Object value) {
		fValue= value;
	}	

}
