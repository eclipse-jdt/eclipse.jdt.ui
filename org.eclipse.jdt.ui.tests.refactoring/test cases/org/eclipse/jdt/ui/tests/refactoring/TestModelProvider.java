/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Assert;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.ModelProvider;

public class TestModelProvider extends ModelProvider {
	public static IResourceDelta LAST_DELTA;
	
	public IStatus validateChange(IResourceDelta delta) {
		LAST_DELTA= delta;
		return super.validateChange(delta);
	}
	
	public static void assertTrue(IResourceDelta expected) {
		Assert.assertNotNull(LAST_DELTA);
		assertTrue(expected, LAST_DELTA);
		LAST_DELTA= null;
	}
	
	private static void assertTrue(IResourceDelta expected, IResourceDelta actual) {
		Assert.assertEquals("Same resource", expected.getResource(), actual.getResource());
		int actualKind= actual.getKind();
		Assert.assertEquals("Same kind", expected.getKind(), actualKind);
		Assert.assertEquals("Same flags", expected.getFlags(), actual.getFlags());
		IResourceDelta[] expectedChildren= expected.getAffectedChildren();
		IResourceDelta[] actualChildren= actual.getAffectedChildren();
		Assert.assertEquals("Same number of children", expectedChildren.length, actualChildren.length);
		for (int i= 0; i < expectedChildren.length; i++) {
			assertTrue(expectedChildren[i], actualChildren[i]);
		}
	}
}