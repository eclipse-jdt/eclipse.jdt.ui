/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * A template consiting of a name and a pattern.
 */
public class Template {

	private String fName;
	private String fDescription;
	private String fContext;
	private String fPattern;
	private boolean fEnabled= true;

	public Template() {
		this("", "", "", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * Creates a template.
	 * @param name        the name of the template.
	 * @param description the description of the template.
	 * @param context     the context in which the template can be applied.
	 * @param pattern     the template pattern.
	 */		
	public Template(String name, String description, String context, String pattern) {
		fName= name;
		fDescription= description;
		fContext= context;
		fPattern= pattern;
	}
	
	/*
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof Template))
			return false;
			
		Template template= (Template) object;

		if (template == this)
			return true;		

		return
			template.fPattern.equals(fPattern) &&
			template.fContext.equals(fContext);
	}
	
	/*
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fPattern.hashCode() ^ fContext.hashCode();
	}

	/**
	 * Sets the description of the template.
	 */
	public void setDescription(String description) {
		fDescription= new String(description);
	}
	
	/**
	 * Returns the description of the template.
	 */
	public String getDescription() {
		return new String(fDescription);
	}
	
	/**
	 * Sets the context of the template.
	 */
	public void setContext(String context) {
		fContext= new String(context);
	}
	
	/**
	 * Returns the context in which the cursor was.
	 */
	public String getContext() {
		return new String(fContext);
	}

	/**
	 * Sets the name of the template.
	 */
	public void setName(String name) {
		fName= new String(name);
	}
			
	/**
	 * Returns the name of the template.
	 */
	public String getName() {
		return new String(fName);
	}

	/**
	 * Sets the pattern of the template.
	 */
	public void setPattern(String pattern) {
		fPattern= new String(pattern);
	}
		
	/**
	 * Returns the template pattern.
	 */
	public String getPattern() {
		return new String(fPattern);
	}
	
	/**
	 * Sets the enable state of the template.
	 */
	public void setEnabled(boolean enable) {
		fEnabled= enable;	
	}
	
	/**
	 * Returns <code>true</code> if template is enabled, <code>false</code> otherwise.
	 */
	public boolean isEnabled() {
		return fEnabled;	
	}
	
	/**
	 * Returns <code>true</code> if template matches the prefix and context,
	 * <code>false</code> otherwise.
	 */
	public boolean matches(String prefix, String context) {
		return 
			fEnabled &&
			fContext.equals(context) &&
			(prefix.length() != 0) &&
			fName.toLowerCase().startsWith(prefix.toLowerCase());
	}

}
