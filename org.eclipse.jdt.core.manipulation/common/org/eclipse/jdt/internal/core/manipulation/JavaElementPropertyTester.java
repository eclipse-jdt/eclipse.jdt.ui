/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.internal.core.manipulation;

import java.util.regex.Pattern;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A property tester for various properties of IJavaElements.
 * Might be moved down to jdt.core. See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=127085
 *
 * @since 1.1
 */
public class JavaElementPropertyTester extends PropertyTester {

	/**
	 * A property indicating the file name (value <code>"name"</code>). Regular expressions are supported.
	 */
	public static final String NAME = "name"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is in a open and existing Java project (value <code>"isInJavaProject"</code>).
	 */
	public static final String IS_IN_JAVA_PROJECT = "isInJavaProject"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is in a open and existing Java project that also implements the given nature (value <code>"isInJavaProjectWithNature"</code>).
	 */
	public static final String IS_IN_JAVA_PROJECT_WITH_NATURE = "isInJavaProjectWithNature"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is on the classpath (value <code>"isOnClasspath"</code>).
	 */
	public static final String IS_ON_CLASSPATH = "isOnClasspath"; //$NON-NLS-1$

	/**
	 * A property indicating if the a type of the given qualified name is on the classpath (value <code>"hasTypeOnClasspath"</code>).
	 */
	public static final String HAS_TYPE_ON_CLASSPATH = "hasTypeOnClasspath"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is a source folder or is inside a source folder. (value <code>"inSourceFolder"</code>).
	 * <code>false</code> is returned if the element does not exist.
	 */
	public static final String IN_SOURCE_FOLDER = "inSourceFolder"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is an archive or is inside an archive. (value <code>"inArchive"</code>).
	 * <code>false</code> is returned if the element does not exist.
	 */
	public static final String IN_ARCHIVE = "inArchive"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is an archive (value <code>"inExternalArchive"</code>).
	 * <code>false</code> is returned if the element does not exist.
	 */
	public static final String IN_EXTERNAL_ARCHIVE = "inExternalArchive"; //$NON-NLS-1$

	/**
	 * A property indicating a option in the Java project of the selected element
	 * (value <code>"projectOption"</code>). If two arguments are given,
	 * this treats the first as the option name, and the second as the option
	 * property value. If only one argument (or just the expected value) is
	 * given, this treats it as the property name, and simply tests if the option is
	 * avaiable in the project specific options.
	 */
	public static final String PROJECT_OPTION = "projectOption"; //$NON-NLS-1$


	@SuppressWarnings("boxing")
	@Override
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		return JavaCore.callReadOnly(() -> testCached(receiver, method, args, expectedValue));
	}

	private boolean testCached(Object receiver, String method, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IJavaElement)) {
			return false;
		}
		IJavaElement res = (IJavaElement) receiver;
		switch (method) {
		case NAME:
			return Pattern.matches(toString(expectedValue), res.getElementName());
		case IS_IN_JAVA_PROJECT:
		{
			IJavaProject javaProject= res.getJavaProject();
			return javaProject != null && javaProject.exists() && javaProject.getProject().isOpen();
		}
		case IS_IN_JAVA_PROJECT_WITH_NATURE:
		{
			IJavaProject javaProject= res.getJavaProject();
			if (javaProject != null && javaProject.exists() && javaProject.getProject().isOpen() ) {
				if (expectedValue != null) {
					try {
						return javaProject.getProject().hasNature(toString(expectedValue));
					} catch (CoreException e) {
						return false;
					}
				}
			}
			return false;
		}
		case IS_ON_CLASSPATH:
		{
			IJavaProject javaProject= res.getJavaProject();
			if (javaProject != null && javaProject.exists()) {
				return javaProject.isOnClasspath(res);
			}
			return false;
		}
		case IN_SOURCE_FOLDER:
		{
			IJavaElement root= res.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null) {
				try {
					return ((IPackageFragmentRoot) root).getKind() == IPackageFragmentRoot.K_SOURCE;
				} catch (JavaModelException e) {
					// ignore
				}
			}
			return false;
		}
		case IN_ARCHIVE:
		{
			IJavaElement root= res.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null) {
				return ((IPackageFragmentRoot) root).isArchive();
			}
			return false;
		}
		case IN_EXTERNAL_ARCHIVE:
		{
			IJavaElement root= res.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null) {
				return ((IPackageFragmentRoot) root).isExternal();
			}
			return false;
		}
		case PROJECT_OPTION:
		{
			IJavaProject project= res.getJavaProject();
			if (project != null) {
				if (args.length == 2) {
					String current= project.getOption(toString(args[0]), true);
					return current != null && current.equals(args[1]);
				} else if (args.length == 1) {
					return project.getOption(toString(args[0]), false) != null;
				}
			}
			return false;
		}
		case HAS_TYPE_ON_CLASSPATH:
		{
			IJavaProject javaProject= res.getJavaProject();
			if (javaProject != null && javaProject.exists()) {
				try {
					return javaProject.findType(toString(expectedValue)) != null;
				} catch (JavaModelException e) {
					return false;
				}
			}	break;
		}
		default:
			break;
		}
		return false;
	}

	private String toString(Object expectedValue) {
		return expectedValue == null ? "" : expectedValue.toString(); //$NON-NLS-1$
	}
}
