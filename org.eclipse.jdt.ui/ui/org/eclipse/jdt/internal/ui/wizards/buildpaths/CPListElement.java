/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.JavaCore;

public class CPListElement {
			
	private int fEntryKind;
	private IPath fPath;
	private IResource fResource;
	
	private IPath fSourceAttachmentPath;
	private IPath fSourceAttachmentPrefix;
	
	private URL fJavaDocLocation;
					
	public CPListElement(int entryKind, IPath path, IResource res) {
		fEntryKind= entryKind;
		fPath= path;
		fSourceAttachmentPath= null;
		fSourceAttachmentPrefix= null;
		fResource= res;
	}
	
	public IClasspathEntry getClasspathEntry() {
		switch (fEntryKind) {
			case IClasspathEntry.CPE_SOURCE:
				return JavaCore.newSourceEntry(fPath);
			case IClasspathEntry.CPE_LIBRARY:
				return JavaCore.newLibraryEntry(fPath, fSourceAttachmentPath, fSourceAttachmentPrefix);
			case IClasspathEntry.CPE_PROJECT:
				return JavaCore.newProjectEntry(fPath);
			default: // IClasspathEntry.CPE_VARIABLE:
				return JavaCore.newVariableEntry(fPath, fSourceAttachmentPath, fSourceAttachmentPrefix);
		}
	}
	
	public IPath getPath() {
		return fPath;
	}
	
	public int getEntryKind() {
		return fEntryKind;
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
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
			CPListElement elem= (CPListElement)other;
			return elem.fEntryKind == fEntryKind && elem.fPath == fPath;
		}
		return false;
	}

}