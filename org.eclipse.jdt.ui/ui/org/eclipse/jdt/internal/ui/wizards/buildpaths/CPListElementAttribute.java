/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

/**
  */
public class CPListElementAttribute {
	
	public static final String K_SOURCEATTACHMENT= "sourcepath"; //$NON-NLS-1$
	public static final String K_SOURCEATTACHMENTROOT= "rootpath"; //$NON-NLS-1$
	public static final String K_JAVADOC= "javadoc"; //$NON-NLS-1$
	public static final String K_OUTPUT= "output"; //$NON-NLS-1$
	public static final String K_EXCLUSION= "exclusion"; //$NON-NLS-1$
	
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
	 */
	public void setValue(Object value) {
		fValue= value;
	}	
	
    public boolean equals(Object obj) {
        if (!(obj instanceof CPListElementAttribute))
            return false;
        CPListElementAttribute attrib= (CPListElementAttribute)obj;
        return attrib.fKey== this.fKey && attrib.getParent().getPath().equals(fParent.getPath());
    }
}
