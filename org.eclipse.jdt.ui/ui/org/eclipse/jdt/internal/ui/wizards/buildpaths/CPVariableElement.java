/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.util.Assert;


public class CPVariableElement {

	private String fName;
	private IPath fPath;
	
	private boolean fIsReserved;

	public CPVariableElement(String name, IPath path, boolean reserved) {
		Assert.isNotNull(name);
		Assert.isNotNull(path);
		fName= name;
		fPath= path;
		fIsReserved= reserved;
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
	
	/*
	 * @see Object#equals()
	 */	
	public boolean equals(Object other) {
		if (other.getClass().equals(getClass())) {
			CPVariableElement elem= (CPVariableElement)other;
			return fName.equals(elem.fName);
		}
		return false;
	}
	
	/*
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fName.hashCode();
	}	
	
	/**
	 * Returns true if variable is reserved
	 * @return Returns a boolean
	 */
	public boolean isReserved() {
		return fIsReserved;
	}

	/**
	 * Sets the isReserved
	 * @param isReserved The state to set
	 */
	public void setReserved(boolean isReserved) {
		fIsReserved= isReserved;
	}


}