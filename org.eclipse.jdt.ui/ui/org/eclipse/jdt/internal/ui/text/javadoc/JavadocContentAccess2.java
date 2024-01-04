/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Hofmann, Google <eclipse@tom.eicher.name> - [hovering] NPE when hovering over @value reference within a type's javadoc - https://bugs.eclipse.org/bugs/show_bug.cgi?id=320084
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.manipulation.internal.javadoc.CoreJavadocAccess;


/**
 * Helper to get the content of a Javadoc comment as HTML.
 *
 * <p>
 * <strong>This is work in progress. Parts of this will later become
 * API through {@link CoreJavadocContentAccess}</strong>
 * </p>
 *
 * @since 3.4
 */
public class JavadocContentAccess2 {

	public static final String BlOCK_TAG_TITLE_START= "<dt>"; //$NON-NLS-1$
	public static final String BlOCK_TAG_TITLE_END= "</dt>"; //$NON-NLS-1$

	public static final String BlOCK_TAG_ENTRY_START= "<dd>"; //$NON-NLS-1$
	public static final String BlOCK_TAG_ENTRY_END= "</dd>"; //$NON-NLS-1$

	/**
	 * Gets an IJavaElement's Javadoc comment content from the source or Javadoc attachment
	 * and renders the tags and links in HTML.
	 * Returns <code>null</code> if the element does not have a Javadoc comment or if no source is available.
	 *
	 * @param element				the element to get the Javadoc of
	 * @param useAttachedJavadoc	if <code>true</code> Javadoc will be extracted from attached Javadoc
	 * 									if there's no source
	 * @return the Javadoc comment content in HTML or <code>null</code> if the element
	 * 			does not have a Javadoc comment or if no source is available
	 * @throws CoreException is thrown when the element's Javadoc cannot be accessed
	 */
	public static String getHTMLContent(IJavaElement element, boolean useAttachedJavadoc) throws CoreException {
		return new CoreJavadocAccess().getHTMLContent(element, useAttachedJavadoc);
	}

	/**
	 * @param content HTML content produced by <code>getHTMLContent(...)</code>
	 * @return the baseURL to use for the given content, or <code>null</code> if none
	 * @since 3.10
	 */
	public static String extractBaseURL(String content) {
		return new CoreJavadocAccess().extractBaseURL(content);
	}

	/**
	 * Returns the Javadoc for a PackageDeclaration.
	 *
	 * @param packageDeclaration the Java element whose Javadoc has to be retrieved
	 * @return the package documentation in HTML format or <code>null</code> if there is no
	 *         associated Javadoc
	 * @throws CoreException if the Java element does not exists or an exception occurs while
	 *             accessing the file containing the package Javadoc
	 * @since 3.9
	 */
	public static String getHTMLContent(IPackageDeclaration packageDeclaration) throws CoreException {
		return new CoreJavadocAccess().getHTMLContent(packageDeclaration);
	}


	/**
	 * Returns the Javadoc for a package which could be present in package.html, package-info.java
	 * or from an attached Javadoc.
	 *
	 * @param packageFragment the package which is requesting for the document
	 * @return the document content in HTML format or <code>null</code> if there is no associated
	 *         Javadoc
	 * @throws CoreException if the Java element does not exists or an exception occurs while
	 *             accessing the file containing the package Javadoc
	 * @since 3.9
	 */
	public static String getHTMLContent(IPackageFragment packageFragment) throws CoreException {
		return new CoreJavadocAccess().getHTMLContent(packageFragment);
	}
}
