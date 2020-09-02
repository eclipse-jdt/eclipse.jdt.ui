/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.resources.IResource;

import org.eclipse.search.ui.text.IFileMatchAdapter;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.tests.core.rules.JUnitSourceSetup;

import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResult;

/**
 */
public class FileAdapterTest {

	@Rule
	public JUnitSourceSetup projectSetup = new JUnitSourceSetup();

	@Test
	public void testGetFile() throws Exception {
		JavaSearchQuery query= SearchTestHelper.runTypeRefQuery("junit.framework.Test");
		JavaSearchResult result= (JavaSearchResult) query.getSearchResult();
		IFileMatchAdapter adapter= result.getFileMatchAdapter();
		for (Object element : result.getElements()) {
			IJavaElement je= (IJavaElement) element;
			IResource underlying= je.getUnderlyingResource();
			if (underlying != null && underlying.getName().endsWith(".java")) {
				assertEquals(underlying, adapter.getFile(je));
			} else {
				assertNull(adapter.getFile(je));
			}
		}
	}

}
