/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.buildpath;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.jdt.internal.corext.buildpath.IBuildpathModifierListener;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.AddArchiveToBuildpathAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.BuildpathModifierAction;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class BuildpathModifierActionTest extends TestCase {

	private static final Class THIS= BuildpathModifierActionTest.class;
	
	private IJavaProject fJavaProject;
	
	public BuildpathModifierActionTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		TestSuite result= new TestSuite();
		result.addTest(new ProjectTestSetup(new TestSuite(THIS)));
		result.addTest(new TestSuite(BuildpathModifierActionEnablementTest.THIS));
		return result;
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			return setUpTest(new BuildpathModifierActionTest("testDefaultProject"));
		}	
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	protected void setUp() throws Exception {
		fJavaProject= ProjectTestSetup.getProject();
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJavaProject, ProjectTestSetup.getDefaultClasspath());
	}
	
	private static void assertActionEnabled(BuildpathModifierAction action) {
		assertTrue("Action " + action.getDescription() + " is not enabled", action.isEnabled());
    }
	
	private void assertIsOnBuildpath(IPath path) throws JavaModelException {
	    IClasspathEntry[] entries= fJavaProject.getRawClasspath();
		for (int i= 0; i < entries.length; i++) {
	        if (entries[i].getPath().equals(path))
	        	return;
        }
		assertTrue("Element with location " + path + " is not on buildpath", false);
    }

	private static void changeSelection(BuildpathModifierAction action, Object select) {
	    final StructuredSelection structuredSelection= new StructuredSelection(select);
		action.selectionChanged(new SelectionChangedEvent(new ISelectionProvider() {

			public void addSelectionChangedListener(ISelectionChangedListener listener) {
            }

			public ISelection getSelection() {
	            return structuredSelection;
            }

			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            }

			public void setSelection(ISelection selection) {
            }
			
		}, structuredSelection));
    }
	
	public void testBug132827() throws Exception {
		
		AddArchiveToBuildpathAction addArchiveAction= new AddArchiveToBuildpathAction(PlatformUI.getWorkbench().getProgressService(), new ISetSelectionTarget() {
			public void selectReveal(ISelection selection) {
            }
		});
		addArchiveAction.addBuildpathModifierListener(new IBuildpathModifierListener() {
			public void buildpathChanged(BuildpathDelta delta) {
            }
		});
		
		changeSelection(addArchiveAction, fJavaProject);
		assertActionEnabled(addArchiveAction);
		
		addArchiveAction.run(new IPath[] {JavaProjectHelper.MYLIB.makeAbsolute()}, true);
		
		assertIsOnBuildpath(JavaProjectHelper.MYLIB.makeAbsolute());
		
		changeSelection(addArchiveAction, fJavaProject);
		assertActionEnabled(addArchiveAction);
		
		addArchiveAction.run(new IPath[] {JavaProjectHelper.MYLIB.makeAbsolute()}, true);
		
		assertIsOnBuildpath(JavaProjectHelper.MYLIB.makeAbsolute());
	}
}