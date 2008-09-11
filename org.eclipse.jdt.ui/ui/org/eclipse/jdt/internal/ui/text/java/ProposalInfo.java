/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;


import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
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
			if (javaElement instanceof IMember) {
				IMember member= (IMember) javaElement;
				return extractJavadoc(member, monitor);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Extracts the javadoc for the given <code>IMember</code> and returns it
	 * as HTML.
	 *
	 * @param member the member to get the documentation for
	 * @param monitor a progress monitor
	 * @return the javadoc for <code>member</code> or <code>null</code> if
	 *         it is not available
	 * @throws JavaModelException if accessing the javadoc fails
	 */
	private String extractJavadoc(IMember member, IProgressMonitor monitor) throws JavaModelException {
		if (member != null) {
			return JavadocContentAccess2.getHTMLContent(member, true, true);
		}
		return null;
	}

}
