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
	
	private boolean fIsMissing;
	
	private IPath fSourceAttachmentPath;
	private IPath fSourceAttachmentPrefix;
	
	private boolean fIsExported;
	
	private IClasspathEntry fCachedEntry;
	
	///private URL fJavaDocLocation;
	
	public CPListElement(int entryKind, IPath path, IResource res, IPath attachPath, IPath attachRoot) {
		fEntryKind= entryKind;
		fPath= path;
		fSourceAttachmentPath= attachPath;
		fSourceAttachmentPrefix= attachRoot;	
		fResource= res;
		fIsMissing= false;
		
		fCachedEntry= null;
	}

	public CPListElement(int entryKind, IPath path, IResource res) {
		this(entryKind, path, res, null, null);
	}

	
	public IClasspathEntry getClasspathEntry() {
		if (fCachedEntry == null) {
			fCachedEntry= newClasspathEntry();
		}
		return fCachedEntry;
	}
	

	private IClasspathEntry newClasspathEntry() {
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
	 * Entries without resource are either non existing or a variable entry
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
		
		fCachedEntry= null;
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

	/**
	 * Returns if a entry is missing.
	 * @return Returns a boolean
	 */
	public boolean isMissing() {
		return fIsMissing;
	}

	/**
	 * Sets the 'missing' state of the entry.
	 */
	public void setIsMissing(boolean isMissing) {
		fIsMissing= isMissing;
	}

	/**
	 * Returns if a entry is exported (only applies to libraries)
	 * @return Returns a boolean
	 */
	public boolean isExported() {
		return fIsExported;
	}

	/**
	 * Sets the export state of the entry.
	 */
	public void setExported(boolean isExported) {
		fIsExported = isExported;
	}

}