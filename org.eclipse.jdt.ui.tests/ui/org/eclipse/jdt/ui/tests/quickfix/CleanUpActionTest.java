/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.actions.MultiSortMembersAction;

/**
 * @since 3.4
 */
public class CleanUpActionTest extends CleanUpTestCase {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	//205600 [clean up] 'Sort members' action uses workspace/project clean up options
	@Test
	public void testSortMembersAction() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("testSortMembersAction", false, null);
		String str= """
			package testSortMembersAction;
			public class E1 {
			    private void methodX() {}
			    private void methodA() {}
			    private int fieldX;
			    private int fieldA;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", str, false, null);

		IWorkbenchPartSite site= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
		MultiSortMembersAction action= new MultiSortMembersAction(site) {
			@Override
			protected Map<String, String> getSettings() {
				Hashtable<String, String> result= new Hashtable<>();
				result.put(CleanUpConstants.SORT_MEMBERS, CleanUpOptions.TRUE);
				result.put(CleanUpConstants.SORT_MEMBERS_ALL, CleanUpOptions.TRUE);
				return result;
			}
		};
		StructuredSelection selection= new StructuredSelection(cu1);
		action.selectionChanged(selection);
		assertTrue(action.isEnabled());

		action.run(selection);

		String expected1= """
			package testSortMembersAction;
			public class E1 {
			    private int fieldA;
			    private int fieldX;
			    private void methodA() {}
			    private void methodX() {}
			}
			""";

		assertEquals(expected1, cu1.getBuffer().getContents());
	}

}
