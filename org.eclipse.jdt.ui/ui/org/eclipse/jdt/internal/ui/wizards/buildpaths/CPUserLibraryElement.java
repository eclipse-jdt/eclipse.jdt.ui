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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.userlibrary.*;

public class CPUserLibraryElement {
	
	private String fName;
	private List fChildren;
	private boolean fIsSystemLibrary;

	public CPUserLibraryElement(String name) {
		fName= name;
		fChildren= new ArrayList();
		UserLibrary library= UserLibraryManager.getUserLibrary(name);
		if (library != null) {
			IClasspathEntry[] entries= library.getEntries();
			CPListElement[] res= new CPListElement[entries.length];
			for (int i= 0; i < res.length; i++) {
				IClasspathEntry curr= entries[i];
				CPListElement elem= new CPListElement(this, null, IClasspathEntry.CPE_LIBRARY, curr.getPath(), null);
				elem.setAttribute(CPListElement.SOURCEATTACHMENT, curr.getSourceAttachmentPath());
				elem.setAttribute(CPListElement.JAVADOC, JavaUI.getLibraryJavadocLocation(curr.getPath()));
				fChildren.add(elem);
			}
			fIsSystemLibrary= library.isSystemLibrary();
		} else {
			fIsSystemLibrary= false;
		}
	}
	
	public CPUserLibraryElement(String name, boolean isSystemLibrary, CPListElement[] children) {
		fName= name;
		fChildren= new ArrayList();
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				fChildren.add(children[i]);
			}
		}
		fIsSystemLibrary= isSystemLibrary;
	}
	
	public CPListElement[] getChildren() {
		return (CPListElement[]) fChildren.toArray(new CPListElement[fChildren.size()]);
	}

	public String getName() {
		return fName;
	}
	
	public IPath getPath() {
		return new Path(UserLibraryClasspathContainer.CONTAINER_ID).append(fName);
	}

	public boolean isSystemLibrary() {
		return fIsSystemLibrary;
	}
	
	public void add(CPListElement element) {
		if (!fChildren.contains(element)) {
			fChildren.add(element);
		}
	}
	
	public void remove(CPListElement element) {
		fChildren.remove(element);
	}
	
	public void replace(CPListElement existingElement, CPListElement element) {
		if (fChildren.contains(element)) {
			fChildren.remove(existingElement);
		} else {
			int index= fChildren.indexOf(existingElement);
			if (index != -1) {
				fChildren.set(index, element);
			} else {
				fChildren.add(element);
			}
		}
	}

	public UserLibrary getUserLibrary() {
		CPListElement[] children= getChildren();
		IClasspathEntry[] entries= new IClasspathEntry[children.length];
		for (int i= 0; i < entries.length; i++) {
			entries[i]= children[i].getClasspathEntry();
		}
		return new UserLibrary(entries, fIsSystemLibrary);
	}
	
	public void collectJavaDocLocations(ArrayList paths, ArrayList urls) {
		for (int i= 0; i < fChildren.size(); i++) {
			CPListElement curr= (CPListElement) fChildren.get(i);
			curr.collectJavaDocLocations(paths, urls);
		}
	}

	

}