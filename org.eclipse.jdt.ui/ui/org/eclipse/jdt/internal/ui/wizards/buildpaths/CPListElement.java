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

import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

public class CPListElement {
	
	public static final String SOURCEATTACHMENT= "sourcepath"; //$NON-NLS-1$
	public static final String SOURCEATTACHMENTROOT= "rootpath"; //$NON-NLS-1$
	public static final String JAVADOC= "javadoc"; //$NON-NLS-1$
	public static final String OUTPUT= "output"; //$NON-NLS-1$
	public static final String EXCLUSION= "exclusion"; //$NON-NLS-1$
	public static final String INCLUSION= "inclusion"; //$NON-NLS-1$
	
	private IJavaProject fProject;
	
	private int fEntryKind;
	private IPath fPath;
	private IResource fResource;
	private boolean fIsExported;
	private boolean fIsMissing;
	
	private Object fParentContainer;
		
	private IClasspathEntry fCachedEntry;
	private ArrayList fChildren;
	
	public CPListElement(IJavaProject project, int entryKind, IPath path, IResource res) {
		this(null, project, entryKind, path, res);
	}
	
	
	public CPListElement(Object parent, IJavaProject project, int entryKind, IPath path, IResource res) {
		fProject= project;

		fEntryKind= entryKind;
		fPath= path;
		fChildren= new ArrayList();
		fResource= res;
		fIsExported= false;
		
		fIsMissing= false;
		fCachedEntry= null;
		fParentContainer= parent;
		
		switch (entryKind) {
			case IClasspathEntry.CPE_SOURCE:
				createAttributeElement(OUTPUT, null);
				createAttributeElement(INCLUSION, new Path[0]);
				createAttributeElement(EXCLUSION, new Path[0]);
				break;
			case IClasspathEntry.CPE_LIBRARY:
			case IClasspathEntry.CPE_VARIABLE:
				createAttributeElement(SOURCEATTACHMENT, null);
				createAttributeElement(JAVADOC, null);
				break;
			case IClasspathEntry.CPE_PROJECT:
				break;
			case IClasspathEntry.CPE_CONTAINER:
				try {
					IClasspathContainer container= JavaCore.getClasspathContainer(fPath, fProject);
					if (container != null) {
						IClasspathEntry[] entries= container.getClasspathEntries();
						for (int i= 0; i < entries.length; i++) {
							CPListElement curr= createFromExisting(entries[i], fProject);
							curr.setParentContainer(this);
							fChildren.add(curr);
						}						
					}
				} catch (JavaModelException e) {
				}			
				break;
			default:
		}
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
				IPath outputLocation= (IPath) getAttribute(OUTPUT);
				IPath[] inclusionPattern= (IPath[]) getAttribute(INCLUSION);
				IPath[] exclusionPattern= (IPath[]) getAttribute(EXCLUSION);
				return JavaCore.newSourceEntry(fPath, inclusionPattern, exclusionPattern, outputLocation);
			case IClasspathEntry.CPE_LIBRARY:
				IPath attach= (IPath) getAttribute(SOURCEATTACHMENT);
				return JavaCore.newLibraryEntry(fPath, attach, null, isExported());
			case IClasspathEntry.CPE_PROJECT:
				return JavaCore.newProjectEntry(fPath, isExported());
			case IClasspathEntry.CPE_CONTAINER:
				return JavaCore.newContainerEntry(fPath, isExported());
			case IClasspathEntry.CPE_VARIABLE:
				IPath varAttach= (IPath) getAttribute(SOURCEATTACHMENT);
				return JavaCore.newVariableEntry(fPath, varAttach, null, isExported());
			default:
				return null;
		}
	}
	
	/**
	 * Gets the class path entry path.
	 * @see IClasspathEntry#getPath()
	 */
	public IPath getPath() {
		return fPath;
	}

	/**
	 * Gets the class path entry kind.
	 * @see IClasspathEntry#getEntryKind()
	 */	
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
	
	public CPListElementAttribute setAttribute(String key, Object value) {
		CPListElementAttribute attribute= findAttributeElement(key);
		if (attribute == null) {
			return null;
		}
		attribute.setValue(value);
		attributeChanged(key);
		return attribute;
	}
	
	private CPListElementAttribute findAttributeElement(String key) {
		for (int i= 0; i < fChildren.size(); i++) {
			Object curr= fChildren.get(i);
			if (curr instanceof CPListElementAttribute) {
				CPListElementAttribute elem= (CPListElementAttribute) curr;
				if (key.equals(elem.getKey())) {
					return elem;
				}
			}
		}		
		return null;		
	}
	
	public Object getAttribute(String key) {
		CPListElementAttribute attrib= findAttributeElement(key);
		if (attrib != null) {
			return attrib.getValue();
		}
		return null;
	}
	
	private void createAttributeElement(String key, Object value) {
		fChildren.add(new CPListElementAttribute(this, key, value));
	}	
	
	
	public Object[] getChildren(boolean hideOutputFolder) {
		if (hideOutputFolder && fEntryKind == IClasspathEntry.CPE_SOURCE) {
			return new Object[] { findAttributeElement(INCLUSION), findAttributeElement(EXCLUSION) };
		}
		return fChildren.toArray();
	}
	
	private void setParentContainer(Object element) {
		fParentContainer= element;
	}
	
	public Object getParentContainer() {
		return fParentContainer;
	}	
	
	private void attributeChanged(String key) {
		fCachedEntry= null;
	}
	
	
	/*
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other != null && other.getClass().equals(getClass())) {
			CPListElement elem= (CPListElement)other;
			return elem.fEntryKind == fEntryKind && elem.fPath.equals(fPath);
		}
		return false;
	}
	
	/*
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fPath.hashCode() + fEntryKind;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getClasspathEntry().toString();
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
		if (isExported != fIsExported) {
			fIsExported = isExported;
			
			attributeChanged(null);
		}
	}

	/**
	 * Gets the project.
	 * @return Returns a IJavaProject
	 */
	public IJavaProject getJavaProject() {
		return fProject;
	}
	
	public static CPListElement createFromExisting(IClasspathEntry curr, IJavaProject project) {
		IPath path= curr.getPath();
		IWorkspaceRoot root= project.getProject().getWorkspace().getRoot();

		// get the resource
		IResource res= null;
		boolean isMissing= false;
		URL javaDocLocation= null;

		switch (curr.getEntryKind()) {
			case IClasspathEntry.CPE_CONTAINER:
				res= null;
				try {
					isMissing= (JavaCore.getClasspathContainer(path, project) == null);
				} catch (JavaModelException e) {
					isMissing= true;
				}
				break;
			case IClasspathEntry.CPE_VARIABLE:
				IPath resolvedPath= JavaCore.getResolvedVariablePath(path);
				res= null;
				isMissing=  root.findMember(resolvedPath) == null && !resolvedPath.toFile().isFile();
				javaDocLocation= JavaUI.getLibraryJavadocLocation(resolvedPath);
				break;
			case IClasspathEntry.CPE_LIBRARY:
				res= root.findMember(path);
				if (res == null) {
					if (!ArchiveFileFilter.isArchivePath(path)) {
						if (root.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()
								&& root.getProject(path.segment(0)).exists()) {
							res= root.getFolder(path);
						}
					}
					isMissing= !path.toFile().isFile(); // look for external JARs
				}
				javaDocLocation= JavaUI.getLibraryJavadocLocation(path);
				break;
			case IClasspathEntry.CPE_SOURCE:
				path= path.removeTrailingSeparator();
				res= root.findMember(path);
				if (res == null) {
					if (root.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()) {
						res= root.getFolder(path);
					}
					isMissing= true;
				}
				break;
			case IClasspathEntry.CPE_PROJECT:
				res= root.findMember(path);
				isMissing= (res == null);
				break;
		}
		CPListElement elem= new CPListElement(project, curr.getEntryKind(), path, res);
		elem.setExported(curr.isExported());
		elem.setAttribute(SOURCEATTACHMENT, curr.getSourceAttachmentPath());
		elem.setAttribute(JAVADOC, javaDocLocation);
		elem.setAttribute(OUTPUT, curr.getOutputLocation());
		elem.setAttribute(EXCLUSION, curr.getExclusionPatterns());
		elem.setAttribute(INCLUSION, curr.getInclusionPatterns()); 

		if (project.exists()) {
			elem.setIsMissing(isMissing);
		}
		return elem;
	}

	public static StringBuffer appendEncodePath(IPath path, StringBuffer buf) {
		if (path != null) {
			String str= path.toString();
			buf.append('[').append(str.length()).append(']').append(str);
		} else {
			buf.append('[').append(']');
		}
		return buf;
	}
	
	public static StringBuffer appendEncodedURL(URL url, StringBuffer buf) {
		if (url != null) {
			String str= url.toExternalForm();
			buf.append('[').append(str.length()).append(']').append(str);
		} else {
			buf.append('[').append(']');
		}
		return buf;
	}
	
	/**
	 * @return
	 */
	public StringBuffer appendEncodedSettings(StringBuffer buf) {
		buf.append(fEntryKind).append(';');
		appendEncodePath(fPath, buf).append(';');
		buf.append(Boolean.valueOf(fIsExported)).append(';');
		switch (fEntryKind) {
			case IClasspathEntry.CPE_SOURCE:
				IPath output= (IPath) getAttribute(OUTPUT);
				appendEncodePath(output, buf).append(';');
				IPath[] exclusion= (IPath[]) getAttribute(EXCLUSION);
				buf.append('[').append(exclusion.length).append(']');
				for (int i= 0; i < exclusion.length; i++) {
					appendEncodePath(exclusion[i], buf).append(';');
				}
				IPath[] inclusion= (IPath[]) getAttribute(INCLUSION);
				buf.append('[').append(inclusion.length).append(']');
				for (int i= 0; i < inclusion.length; i++) {
					appendEncodePath(inclusion[i], buf).append(';');
				}
				break;
			case IClasspathEntry.CPE_LIBRARY:
			case IClasspathEntry.CPE_VARIABLE:
				IPath sourceAttach= (IPath) getAttribute(SOURCEATTACHMENT);
				appendEncodePath(sourceAttach, buf).append(';');
				URL javadoc= (URL) getAttribute(JAVADOC);
				appendEncodedURL(javadoc, buf).append(';');				
				break;
			default:
			
		}
		return buf;
	}


	/**
	 * Adds the javadoc location to the given arrays.
	 */
	public void collectJavaDocLocations(ArrayList paths, ArrayList urls) {
		int entryKind= getEntryKind();
		if (entryKind == IClasspathEntry.CPE_LIBRARY || entryKind == IClasspathEntry.CPE_VARIABLE) {
			URL javadocLocation= (URL) getAttribute(CPListElement.JAVADOC);
			IPath path= getPath();
			if (entryKind == IClasspathEntry.CPE_VARIABLE) {
				path= JavaCore.getResolvedVariablePath(path);
			}
			if (path != null) {
				paths.add(path);
				urls.add(javadocLocation);
			}			
		} else if (entryKind == IClasspathEntry.CPE_CONTAINER) {
			Object[] children= getChildren(false);
			for (int i= 0; i < children.length; i++) {
				((CPListElement) children[i]).collectJavaDocLocations(paths, urls);
			}
		}
	}	

}
