/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

public class CPListElement {
	
	public static final String SOURCEATTACHMENT= "sourcepath";
	public static final String SOURCEATTACHMENTROOT= "rootpath";
	public static final String JAVADOC= "javadoc";
	public static final String OUTPUT= "output";
	public static final String EXCLUSION= "exclusion";
	
	private IJavaProject fProject;
	
	private int fEntryKind;
	private IPath fPath;
	private IResource fResource;
	private boolean fIsExported;
	private boolean fIsMissing;
	
	private CPListElement fParentContainer;
		
	private IClasspathEntry fCachedEntry;
	private HashMap fAttributes;
	
	public CPListElement(IJavaProject project, int entryKind, IPath path, IResource res) {
		fProject= project;

		fEntryKind= entryKind;
		fPath= path;
		fAttributes= new HashMap();
		fResource= res;
		fIsExported= false;
		
		fIsMissing= false;
		fCachedEntry= null;
		fParentContainer= null;
		
		switch (entryKind) {
			case IClasspathEntry.CPE_SOURCE:
				createAttributeElement(OUTPUT);
				createAttributeElement(EXCLUSION);
				break;
			case IClasspathEntry.CPE_LIBRARY:
			case IClasspathEntry.CPE_VARIABLE:
				createAttributeElement(SOURCEATTACHMENT);
				createAttributeElement(JAVADOC);
				break;
			case IClasspathEntry.CPE_PROJECT:
			case IClasspathEntry.CPE_CONTAINER:
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
				IPath[] exclusionPattern= (IPath[]) getAttribute(EXCLUSION);
				return JavaCore.newSourceEntry(fPath, exclusionPattern, outputLocation);
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
	 * Gets the classpath entry path.
	 * @see IClasspathEntry#getPath()
	 */
	public IPath getPath() {
		return fPath;
	}

	/**
	 * Gets the classpath entry kind.
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
		CPListElementAttribute attribute= (CPListElementAttribute) fAttributes.get(key);
		if (attribute == null) {
			return null;
		}
		attribute.setValue(value);
		attributeChanged(key);
		return attribute;
	}
	

	
	public Object getAttribute(String key) {
		CPListElementAttribute attrib= (CPListElementAttribute) fAttributes.get(key);
		if (attrib != null) {
			return attrib.getValue();
		}
		return null;
	}
	
	private CPListElementAttribute createAttributeElement(String key) {
		CPListElementAttribute attribute= (CPListElementAttribute) fAttributes.get(key);
		if (attribute == null) {
			attribute= new CPListElementAttribute(this, key, null);
			fAttributes.put(key, attribute);
		}
		return attribute;
	}	
	
	
	public Object[] getChildren() {
		if (fEntryKind == IClasspathEntry.CPE_CONTAINER) {
			try {
				IClasspathContainer container= JavaCore.getClasspathContainer(fPath, fProject);
				IClasspathEntry[] entries= container.getClasspathEntries();
				CPListElement[] elements= new CPListElement[entries.length];
				for (int i= 0; i < elements.length; i++) {
					elements[i]= createFromExisting(entries[i], fProject);
					elements[i].setParentContainer(this);
				}
				return elements;
			} catch (JavaModelException e) {
				return new Object[0];
			}
		}
		return fAttributes.values().toArray();
	}
	
	private void setParentContainer(CPListElement element) {
		fParentContainer= element;
	}
	
	private CPListElement getParentContainer() {
		return fParentContainer;
	}	
	
	public void attributeChanged(String key) {
		fCachedEntry= null;
	}
	
	
	/**
	 * Sets the paths for source annotation
	 * @see org.eclipse.jdt.core.IPackageFragmentRoot#attachSource
	 * @deprecated 
	 */	
	public void setSourceAttachment(IPath path, IPath prefix) {
		setAttribute(SOURCEATTACHMENT, path);
		setAttribute(SOURCEATTACHMENTROOT, prefix);
	}
	
	/**
	 * Gets the current path prefix used when accessing the source attachment
	 * @see org.eclipse.jdt.core.IPackageFragmentRoot#getSourceAttachmentPath	 
	 * @return The source attachment prefix
	 * @deprecated 
	 */	
	public IPath getSourceAttachmentPath() {
		return (IPath) getAttribute(SOURCEATTACHMENT);
	}
	
	/**
	 * Returns the root path used for accessing source attchments
	 * @see org.eclipse.jdt.core.
	 * IPackageFragmentRoot#getSourceAttachmentRootPath
	 * 	  @deprecated
	 */
	public IPath getSourceAttachmentRootPath() {
		return (IPath) getAttribute(SOURCEATTACHMENTROOT);
	}

	/*
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
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
			
			fCachedEntry= null;
		}
	}

	/**
	 * Gets the Javadoc location.
	 * @return Returns a URL
	 * 	 	  @deprecated
	 */
	public URL getJavadocLocation() {
		return (URL) getAttribute(JAVADOC);
	}

	/**
	 * Sets the Javadoc location.
	 * @param javadocLocation The javadocLocation to set
	 * @deprecated
	 */
	public void setJavadocLocation(URL javadocLocation) {
		setAttribute(JAVADOC, javadocLocation);
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
						if (root.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()) {
							res= root.getFolder(path);
						}
					}
					isMissing= !path.toFile().isFile(); // look for external JARs
				}
				javaDocLocation= JavaUI.getLibraryJavadocLocation(path);
				break;
			case IClasspathEntry.CPE_SOURCE:
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

		if (project.exists()) {
			elem.setIsMissing(isMissing);
		}
		return elem;
	}	

}