/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.wizards.buildpaths;import org.eclipse.core.runtime.IPath;


public class CPVariableElement {

	private String fName;
	private IPath fPath;

	public CPVariableElement(String name, IPath path) {
		fName= name;
		fPath= path;
	}
	
	/**
	 * Gets the path
	 * @return Returns a IPath
	 */
	public IPath getPath() {
		return fPath;
	}

	/**
	 * Sets the path
	 * @param path The path to set
	 */
	public void setPath(IPath path) {
		fPath= path;
	}

	/**
	 * Gets the name
	 * @return Returns a String
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Sets the name
	 * @param name The name to set
	 */
	public void setName(String name) {
		fName= name;
	}
	
	
	public boolean equals(Object other) {
		if (other.getClass().equals(getClass())) {
			CPVariableElement elem= (CPVariableElement)other;
			return fName.equals(elem.fName);
		}
		return false;
	}
}