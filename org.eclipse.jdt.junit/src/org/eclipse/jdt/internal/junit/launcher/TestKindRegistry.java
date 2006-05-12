/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

public class TestKindRegistry {
	
	public static final String JUNIT3_TEST_KIND_ID= "org.eclipse.jdt.junit.loader.junit3"; //$NON-NLS-1$
	public static final String JUNIT4_TEST_KIND_ID= "org.eclipse.jdt.junit.loader.junit4"; //$NON-NLS-1$

	public static TestKindRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;
		
		fgRegistry= new TestKindRegistry(Platform.getExtensionRegistry().getExtensionPoint(JUnitPlugin.ID_EXTENSION_POINT_TEST_KINDS));
		return fgRegistry;
	}

	private static TestKindRegistry fgRegistry;
	
	private final IExtensionPoint fPoint;
	private ArrayList/*<TestKind>*/ fTestKinds;

	private TestKindRegistry(IExtensionPoint point) {
		fPoint = point;
	}

	public ArrayList/*<TestKind>*/ getAllKinds() {
		loadKinds();
		return fTestKinds;
	}

	private void loadKinds() {
		if (fTestKinds != null)
			return;
		
		ArrayList items= new ArrayList();
		for (Iterator iter= getConfigurationElements().iterator(); iter.hasNext();) {
			IConfigurationElement element= (IConfigurationElement) iter.next();
			items.add(new TestKind(element));
		}
		
		Collections.sort(items, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				TestKind kind0 = (TestKind) arg0;
				TestKind kind1 = (TestKind) arg1;

				if (kind0.precedes(kind1))
					return -1;
				if (kind1.precedes(kind0))
					return 1;
				return 0;
			}
		});
		fTestKinds= items;
	}

	public ArrayList/*<String>*/ getDisplayNames() {
		ArrayList result = new ArrayList();
		ArrayList testTypes = getAllKinds();
		for (Iterator iter = testTypes.iterator(); iter.hasNext();) {
			ITestKind type = (ITestKind) iter.next();
			result.add(type.getDisplayName());
		}
		return result;
	}

	public TestKind getKind(ILaunchConfiguration launchConfiguration) {
		String loaderId = ""; //$NON-NLS-1$
		try {
			loaderId = launchConfiguration.getAttribute(JUnitBaseLaunchConfiguration.TEST_KIND_ATTR, (String) null);
		} catch (CoreException e) {
		}
		TestKind configuredKind= getKind(loaderId);
		if (configuredKind.isNull()) {
			try {
				IJavaProject javaProject= new JUnitLaunchConfiguration().getJavaProject(launchConfiguration);
				if (javaProject != null)
					return getKind(getContainerTestKindId(javaProject));
			} catch (CoreException e) {
				// fallback to default
			}
		}
		return configuredKind;
	}

	/**
	 * @param testKindId an id, can be <code>null</code>
	 * @return a TestKind, ITestKind.NULL if not available
	 */
	private TestKind getKind(String testKindId) {
		if (testKindId != null) {
			for (Iterator iter= getAllKinds().iterator(); iter.hasNext();) {
				TestKind kind= (TestKind) iter.next();
				if (testKindId.equals(kind.getId()))
					return kind;
			}
		}
		return ITestKind.NULL;
	}

	public ITestKind getKind(IType type) {
		TestKind projectKind= getKind(getContainerTestKindId(type));
		if (projectKind.isNull())
			return ITestKind.NULL;
		
		try {
			TestSearchResult result= projectKind.search(new SingleTypeTestSearchExtent(type));
			if (!result.isEmpty()) {
				return projectKind;
			} else {
				return ITestKind.NULL;
			}
		} catch (JavaModelException e) {
			return ITestKind.NULL;
		}
	}
	
	public static String getContainerTestKindId(IJavaElement element) {
		return element != null && TestSearchEngine.hasTestAnnotation(element.getJavaProject()) ? JUNIT4_TEST_KIND_ID : JUNIT3_TEST_KIND_ID;
	}

	private ArrayList getConfigurationElements() {
		ArrayList items= new ArrayList();
		IExtension[] extensions= fPoint.getExtensions();
		for (int i= 0; i < extensions.length; i++) {
			IExtension extension= extensions[i];
			IConfigurationElement[] elements= extension.getConfigurationElements();
			for (int j= 0; j < elements.length; j++) {
				IConfigurationElement element= elements[j];
				items.add(element);
			}
		}
		return items;
	}

	public String getAllKindIds() {
		ArrayList allKinds= getAllKinds();
		String returnThis= ""; //$NON-NLS-1$
		for (Iterator iter= allKinds.iterator(); iter.hasNext();) {
			ITestKind kind= (ITestKind) iter.next();
			returnThis+= "(" + kind.getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return returnThis;
	}

	public TestSearchResult getTestTypes(ILaunchConfiguration configuration, final ITestSearchExtent extent) throws JavaModelException {
		final TestKind configuredKind= getKind(configuration);
		if (!configuredKind.isNull())
			return configuredKind.search(extent);
		return new TestSearchResult(new IType[0], ITestKind.NULL);
	}
}
