/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2;


public class ProposalInfo {

	private boolean fJavadocResolved= false;
	private String fJavadoc= null;

	protected IJavaElement fElement;

	public ProposalInfo(IMember member) {
		fElement= member;
	}

	protected ProposalInfo() {
		fElement= null;
	}

	/**
	 * Returns the Java element.
	 *
	 * @throws JavaModelException if accessing the java model fails
	 * @return the Java element
	 */
	public IJavaElement getJavaElement() throws JavaModelException {
		return fElement;
	}

	/**
	 * Gets the text for this proposal info formatted as HTML, or
	 * <code>null</code> if no text is available.
	 *
	 * @param monitor a progress monitor
	 * @return the additional info text
	 */
	public final String getInfo(IProgressMonitor monitor) {
		if (!fJavadocResolved) {
			fJavadocResolved= true;
			fJavadoc= computeInfo(monitor);
		}
		return fJavadoc;
	}

	/**
	 * Gets the text for this proposal info formatted as HTML, or
	 * <code>null</code> if no text is available.
	 *
	 * @param monitor a progress monitor
	 * @return the additional info text
	 */
	private String computeInfo(IProgressMonitor monitor) {
		try {
			final IJavaElement javaElement= getJavaElement();
			return extractJavadoc(javaElement);
		} catch (Exception e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Extracts the Javadoc for the given Java element and returns it as HTML.
	 * 
	 * @param element the Java element to get the documentation for
	 * @return the Javadoc for Java element or <code>null</code> if the Javadoc is not available
	 * @throws CoreException if the file containing the package Javadoc could not be successfully
	 *             connected
	 * @throws IOException if an I/O error occurs while accessing the file containing the package
	 *             Javadoc
	 */
	private String extractJavadoc(IJavaElement element) throws IOException, CoreException {
		if (element instanceof IMember) {
			return JavadocContentAccess2.getHTMLContent((IMember) element, true);
		} else if (element instanceof IPackageDeclaration) {
			return JavadocContentAccess2.getHTMLContent((IPackageDeclaration) element);
		} else if (element instanceof IPackageFragment) {
			return JavadocContentAccess2.getHTMLContent((IPackageFragment) element);
		}
		return null;
	}

}
