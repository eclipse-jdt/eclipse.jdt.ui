/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;


public class TestKindRegistry {

	public static final String JUNIT3_TEST_KIND_ID= "org.eclipse.jdt.junit.loader.junit3"; //$NON-NLS-1$
	public static final String JUNIT4_TEST_KIND_ID= "org.eclipse.jdt.junit.loader.junit4"; //$NON-NLS-1$
	public static final String JUNIT5_TEST_KIND_ID= "org.eclipse.jdt.junit.loader.junit5"; //$NON-NLS-1$

	public static TestKindRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;

		fgRegistry= new TestKindRegistry(Platform.getExtensionRegistry().getExtensionPoint(JUnitCorePlugin.ID_EXTENSION_POINT_TEST_KINDS));
		return fgRegistry;
	}

	private static TestKindRegistry fgRegistry;

	private final IExtensionPoint fPoint;
	private ArrayList<TestKind> fTestKinds;

	private TestKindRegistry(IExtensionPoint point) {
		fPoint = point;
	}

	public ArrayList<TestKind> getAllKinds() {
		loadKinds();
		return fTestKinds;
	}

	private void loadKinds() {
		if (fTestKinds != null)
			return;

		ArrayList<TestKind> items= new ArrayList<>();
		for (IConfigurationElement configurationElement : getConfigurationElements()) {
			items.add(new TestKind(configurationElement));
		}

		Collections.sort(items, (kind0, kind1) -> {
			if (kind0.precedes(kind1))
				return -1;
			if (kind1.precedes(kind0))
				return 1;
			return 0;
		});
		fTestKinds= items;
	}

	public ArrayList<String> getDisplayNames() {
		ArrayList<String> result = new ArrayList<>();
		ArrayList<TestKind> testTypes = getAllKinds();
		for (ITestKind type : testTypes) {
			result.add(type.getDisplayName());
		}
		return result;
	}

	/**
	 * @param testKindId an id, can be <code>null</code>
	 * @return a TestKind, ITestKind.NULL if not available
	 */
	public ITestKind getKind(String testKindId) {
		if (testKindId != null) {
			for (TestKind kind : getAllKinds()) {
				if (testKindId.equals(kind.getId()))
					return kind;
			}
		}
		return ITestKind.NULL;
	}

	public static String getContainerTestKindId(IJavaElement element) {
		if (element != null) {
			IJavaProject project= element.getJavaProject();
			if (CoreTestSearchEngine.is50OrHigher(project)) {
				if (CoreTestSearchEngine.is18OrHigher(project)) {
					if (isRunWithJUnitPlatform(element)) {
						return JUNIT4_TEST_KIND_ID;
					}
					if (CoreTestSearchEngine.hasJUnit5TestAnnotation(project)) {
						return JUNIT5_TEST_KIND_ID;
					}
				}
				if (CoreTestSearchEngine.hasJUnit4TestAnnotation(project)) {
					return JUNIT4_TEST_KIND_ID;
				}
			}
		}
		return JUNIT3_TEST_KIND_ID;
	}

	/**
	 * @param element the element
	 * @return <code>true</code> if the element is a test class annotated with
	 *         <code>@RunWith(JUnitPlatform.class)</code>
	 */
	public static boolean isRunWithJUnitPlatform(IJavaElement element) {
		if (element instanceof ICompilationUnit) {
			element= ((ICompilationUnit) element).findPrimaryType();
		}
		if (element instanceof IType) {
			IType type= (IType) element;
			try {
				IAnnotation runWithAnnotation= type.getAnnotation("RunWith"); //$NON-NLS-1$
				if (!runWithAnnotation.exists()) {
					runWithAnnotation= type.getAnnotation("org.junit.runner.RunWith"); //$NON-NLS-1$
				}
				if (runWithAnnotation.exists()) {
					IMemberValuePair[] memberValuePairs= runWithAnnotation.getMemberValuePairs();
					for (IMemberValuePair memberValuePair : memberValuePairs) {
						if ("value".equals(memberValuePair.getMemberName()) && "JUnitPlatform".equals(memberValuePair.getValue())) { //$NON-NLS-1$ //$NON-NLS-2$
							return true;
						}
					}
				}
			} catch (JavaModelException e) {
				// ignore
			}
		}
		return false;
	}

	public static ITestKind getContainerTestKind(IJavaElement element) {
		return getDefault().getKind(getContainerTestKindId(element));
	}

	private ArrayList<IConfigurationElement> getConfigurationElements() {
		ArrayList<IConfigurationElement> items= new ArrayList<>();
		for (IExtension extension : fPoint.getExtensions()) {
			Collections.addAll(items, extension.getConfigurationElements());
		}
		return items;
	}

	public String getAllKindIds() {
		ArrayList<TestKind> allKinds= getAllKinds();
		StringBuilder returnThis= new StringBuilder();
		for (ITestKind kind : allKinds) {
			returnThis.append("(").append(kind.getId()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return returnThis.toString();
	}
}
