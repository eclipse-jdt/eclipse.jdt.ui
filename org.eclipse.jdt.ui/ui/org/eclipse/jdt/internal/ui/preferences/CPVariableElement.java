package org.eclipse.jdt.internal.ui.preferences;import org.eclipse.core.runtime.IPath;


public class CPVariableElement {

	private String fName;
	private IPath fPath;
	private boolean fImmutable;

	public CPVariableElement(String name, IPath path) {
		fName= name;
		fPath= path;
		fImmutable= false;
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



}