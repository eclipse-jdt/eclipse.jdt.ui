/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.javadoc;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.manipulation.internal.javadoc.CoreJavaDocLocations;


public class JavaDocLocations {

	public static final String ARCHIVE_PREFIX= "jar:"; //$NON-NLS-1$


	/**
	 * Sets the Javadoc location for an archive with the given path.
	 * @param project the Java project
	 * @param url the Javadoc location
	 */
	public static void setProjectJavadocLocation(IJavaProject project, URL url) {
		CoreJavaDocLocations.setProjectJavadocLocation(project, url);
	}

	public static URL getProjectJavadocLocation(IJavaProject project) {
		return CoreJavaDocLocations.getProjectJavadocLocation(project);
	}

	public static URL getLibraryJavadocLocation(IClasspathEntry entry) {
		return CoreJavaDocLocations.getLibraryJavadocLocation(entry);
	}

	public static URL getJavadocBaseLocation(IJavaElement element) throws JavaModelException {
		return CoreJavaDocLocations.getJavadocBaseLocation(element);
	}

	public static URL getJavadocLocation(IJavaElement element, boolean includeMemberReference) throws JavaModelException {
		return CoreJavaDocLocations.getJavadocLocation(element, includeMemberReference);
	}

	/**
	 * Returns the location of the Javadoc.
	 *
	 * @param element whose Javadoc location has to be found
	 * @param isBinary <code>true</code> if the Java element is from a binary container
	 * @return the location URL of the Javadoc or <code>null</code> if the location cannot be found
	 * @throws JavaModelException thrown when the Java element cannot be accessed
	 * @since 3.9
	 */
	public static String getBaseURL(IJavaElement element, boolean isBinary) throws JavaModelException {
		return CoreJavaDocLocations.getBaseURL(element, isBinary, (s) -> PlatformUI.getWorkbench().getHelpSystem().resolve(s.toExternalForm(), true));
	}

	/**
	 * Returns the reason for why the Javadoc of the Java element could not be retrieved.
	 *
	 * @param element whose Javadoc could not be retrieved
	 * @param root the root of the Java element
	 * @return the String message for why the Javadoc could not be retrieved for the Java element or
	 *         <code>null</code> if the Java element is from a source container
	 * @since 3.9
	 */
	public static String getExplanationForMissingJavadoc(IJavaElement element, IPackageFragmentRoot root) {
		return CoreJavaDocLocations.getExplanationForMissingJavadoc(element, root);
	}

	/**
	 * Handles the exception thrown from JDT Core when the attached Javadoc
	 * cannot be retrieved due to accessibility issues or location URL issue. This exception is not
	 * logged but the exceptions occurred due to other reasons are logged.
	 *
	 * @param e the exception thrown when retrieving the Javadoc fails
	 * @return the String message for why the Javadoc could not be retrieved
	 * @since 3.9
	 */
	public static String handleFailedJavadocFetch(CoreException e) {
		return CoreJavaDocLocations.handleFailedJavadocFetch(e);
	}

	/**
	 * Parse a URL from a String. This method first tries to treat <code>url</code> as a valid, encoded URL.
	 * If that didn't work, it tries to recover from bad URLs, e.g. the unencoded form we used to use in persistent storage.
	 *
	 * @param url a URL
	 * @return the parsed URL or <code>null</code> if the URL couldn't be parsed
	 * @since 3.9
	 */
	public static URL parseURL(String url) {
		return CoreJavaDocLocations.parseURL(url);
	}

	/**
	 * Returns the {@link File} of a <code>file:</code> URL. This method tries to recover from bad URLs,
	 * e.g. the unencoded form we used to use in persistent storage.
	 *
	 * @param url a <code>file:</code> URL
	 * @return the file
	 * @since 3.9
	 */
	public static File toFile(URL url) {
		return CoreJavaDocLocations.toFile(url);
	}

	private JavaDocLocations() {
	}
}
