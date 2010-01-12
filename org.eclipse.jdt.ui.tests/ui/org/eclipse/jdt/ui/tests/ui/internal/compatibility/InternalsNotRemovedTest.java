/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.ui.internal.compatibility;

import junit.framework.TestCase;

import org.eclipse.text.tests.Accessor;


/**
 * Tests that internal code which is used by a product doesn't get removed.
 * 
 * @since 3.6
 */
public class InternalsNotRemovedTest extends TestCase {


	public void testSourceRangeNotRemove() {
		testClassNotRemoved("org.eclipse.jdt.internal.corext.SourceRange");
	}

	public void testActionMessageNotRemoved() {

		// See https://bugs.eclipse.org/296836
		testFieldNotRemoved("org.eclipse.jdt.internal.ui.actions.ActionMessages", "OrganizeImportsAction_summary_added");
		testFieldNotRemoved("org.eclipse.jdt.internal.ui.actions.ActionMessages", "OrganizeImportsAction_summary_removed");
	}


	private void testFieldNotRemoved(String className, String fieldName) {
		Accessor classObject= new Accessor(className, getClass().getClassLoader());
		classObject.getField(fieldName);
	}

	private void testClassNotRemoved(String className) {
		new Accessor(className, getClass().getClassLoader());
	}
}
