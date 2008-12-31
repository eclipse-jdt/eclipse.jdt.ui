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
package org.eclipse.jdt.ui.tests.search;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IResource;

import org.eclipse.search.ui.text.IFileMatchAdapter;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResult;

/**
 */
public class FileAdapterTest extends TestCase {

	public static Test allTests() {
		return new JUnitSourceSetup(new TestSuite(FileAdapterTest.class));
	}

	public static Test suite() {
		return allTests();
	}

	public FileAdapterTest(String name) {
		super(name);
	}

	public void testGetFile() throws Exception {
		JavaSearchQuery query= SearchTestHelper.runTypeRefQuery("junit.framework.Test");
		JavaSearchResult result= (JavaSearchResult) query.getSearchResult();
		IFileMatchAdapter adapter= result.getFileMatchAdapter();
		Object[] elements= result.getElements();
		for (int i= 0; i < elements.length; i++) {
			IJavaElement je= (IJavaElement) elements[i];
			IResource underlying= je.getUnderlyingResource();
			if (underlying != null && underlying.getName().endsWith(".java")) {
				assertEquals(underlying, adapter.getFile(je));
			} else {
				assertNull(adapter.getFile(je));
			}
		}
	}

}
