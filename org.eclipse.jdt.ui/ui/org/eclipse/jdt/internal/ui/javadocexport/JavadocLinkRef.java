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
/*
 * Created on 13.05.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.net.URL;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.JavaUI;


public class JavadocLinkRef {
	private Object fElement;
	
	public JavadocLinkRef(IPath libPath) {
		fElement= libPath;
	}
	
	public JavadocLinkRef(IJavaProject project) {
		fElement= project;
	}
	
	public boolean isProjectRef() {
		return (fElement instanceof IJavaProject);
	}
	
	public IPath getFullPath() {
		return fElement instanceof IPath ? (IPath) fElement : ((IJavaElement) fElement).getPath();
	}
	
	public URL getURL() {
		if (fElement instanceof IPath) {
			return JavaUI.getLibraryJavadocLocation((IPath) fElement);
		} else {
			return JavaUI.getProjectJavadocLocation((IJavaProject) fElement);
		}
	}
	
	public void setURL(URL url) {
		if (fElement instanceof IJavaProject) {
			JavaUI.setProjectJavadocLocation((IJavaProject) fElement, url);
		} else {
			JavaUI.setLibraryJavadocLocation((IPath) fElement, url);
		}
	}
	
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass().equals(getClass())) {
			return ((JavadocLinkRef) obj).fElement.equals(fElement);
		}
		return false;
	}
	
	public int hashCode() {
		return fElement.hashCode();
	}
}