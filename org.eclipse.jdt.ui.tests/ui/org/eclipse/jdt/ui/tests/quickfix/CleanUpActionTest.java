/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.actions.MultiSortMembersAction;

/**
 * @since 3.4
 */
public class CleanUpActionTest extends CleanUpTestCase {

	private static final Class THIS= CleanUpActionTest.class;

	public CleanUpActionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	//205600 [clean up] 'Sort members' action uses workspace/project clean up options
	public void testSortMembersAction() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("testSortMembersAction", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package testSortMembersAction;\n");
		buf.append("public class E1 {\n");
		buf.append("    private void methodX() {}\n");
		buf.append("    private void methodA() {}\n");
		buf.append("    private int fieldX;\n");
		buf.append("    private int fieldA;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		IWorkbenchPartSite site= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
		MultiSortMembersAction action= new MultiSortMembersAction(site) {
			/**
			 * {@inheritDoc}
			 */
			protected Map getSettings() {
				Hashtable result= new Hashtable();
				result.put(CleanUpConstants.SORT_MEMBERS, CleanUpOptions.TRUE);
				result.put(CleanUpConstants.SORT_MEMBERS_ALL, CleanUpOptions.TRUE);
				return result;
			}
		};
		StructuredSelection selection= new StructuredSelection(cu1);
		action.selectionChanged(selection);
		assertTrue(action.isEnabled());

		action.run(selection);

		buf= new StringBuffer();
		buf.append("package testSortMembersAction;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int fieldA;\n");
		buf.append("    private int fieldX;\n");
		buf.append("    private void methodA() {}\n");
		buf.append("    private void methodX() {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEquals(expected1, cu1.getBuffer().getContents());
	}

}
