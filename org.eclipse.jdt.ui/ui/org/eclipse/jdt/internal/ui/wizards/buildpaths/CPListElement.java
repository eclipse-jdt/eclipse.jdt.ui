/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;

public class CPListElement {
			
	private IClasspathEntry fEntry;
	private IResource fResource;
	
	private IPath fSourceAttachmentPath;
	private IPath fSourceAttachmentPrefix;
	
	private URL fJavaDocLocation;
					
	public CPListElement(IClasspathEntry entry, IResource res) {
		fEntry= entry;
		fSourceAttachmentPath= null;
		fSourceAttachmentPrefix= null;
		fResource= res;
	}
	
	public IClasspathEntry getClasspathEntry() {
		return fEntry;
	}
	
	public IPath getPath() {
		return fEntry.getPath();
	}
	
	public int getEntryKind() {
		return fEntry.getEntryKind();
	}

	/**
	 * The resources is used for LIBRARY entries to destinguish
	 * between folders, intrenal and external JARs
	 * External jars do not have a resource
	 */
	public IResource getResource() {
		return fResource;
	}


	/**
	 * Sets the paths for source annotation
	 * @see org.eclipse.jdt.core.IPackageFragmentRoot#attachSource	
	 * 
	 */	
	public void setSourceAttachment(IPath path, IPath prefix) {
		fSourceAttachmentPath= path;
		fSourceAttachmentPrefix= prefix;
	}
	
	/**
	 * Sets the JavaDoc documentation location
	 */
	public void setJavaDocLocation(URL jdocLocation) {
		fJavaDocLocation= jdocLocation;
	}

	/**
	 * Gets the current path prefix used when accessing the source attachment
	 * @see org.eclipse.jdt.core.IPackageFragmentRoot#getSourceAttachmentPath	 
	 * @return The source attachment prefix
	 */	
	public IPath getSourceAttachmentPath() {
		return fSourceAttachmentPath;
	}
	
	/**
	 * Returns the root path used for accessing source attchments
	 * @see org.eclipse.jdt.core.IPackageFragmentRoot#getSourceAttachmentRootPath
	 */
	public IPath getSourceAttachmentRootPath() {
		return fSourceAttachmentPrefix;
	}

	/**
	 * Returns the location of the JavaDoc documentation
	 */	
	public URL getJavaDocLocation() {
		return fJavaDocLocation;
	}

	/**
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
			return fEntry.equals(((CPListElement)other).getClasspathEntry());
		}
		return false;
	}

}